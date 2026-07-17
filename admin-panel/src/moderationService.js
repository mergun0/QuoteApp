const REPORT_STATUS = {
  pending: "PENDING",
  approved: "APPROVED",
  rejected: "REJECTED",
};

const ACTION_TYPES = {
  reportApproved: "REPORT_APPROVED",
  reportRejected: "REPORT_REJECTED",
  quoteDeleted: "QUOTE_DELETED",
};

const PAGE_SIZE = 20;

function toMillis(value) {
  if (!value) {
    return 0;
  }
  if (typeof value.toMillis === "function") {
    return value.toMillis();
  }
  if (value instanceof Date) {
    return value.getTime();
  }
  return 0;
}

function normalize(value) {
  return String(value || "").trim().toLocaleLowerCase("tr-TR");
}

function matchesSearch(text, query) {
  if (!query) {
    return true;
  }
  return normalize(text).includes(normalize(query));
}

function paginate(items, page = 1, pageSize = PAGE_SIZE) {
  const safePage = Math.max(1, Number(page) || 1);
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const currentPage = Math.min(safePage, totalPages);
  const start = (currentPage - 1) * pageSize;
  return {
    items: items.slice(start, start + pageSize),
    page: currentPage,
    totalPages,
    totalItems: items.length,
    hasPrevious: currentPage > 1,
    hasNext: currentPage < totalPages,
  };
}

async function enrichReport(db, doc) {
  const report = { id: doc.id, ...doc.data() };
  const [quoteSnap, ownerSnap, reporterSnap] = await Promise.all([
    report.quoteId ? db.collection("quotes").doc(report.quoteId).get() : Promise.resolve(null),
    report.reportedUserId ? db.collection("users").doc(report.reportedUserId).get() : Promise.resolve(null),
    report.reporterUserId ? db.collection("users").doc(report.reporterUserId).get() : Promise.resolve(null),
  ]);
  return {
    ...report,
    quote: quoteSnap && quoteSnap.exists ? { id: quoteSnap.id, ...quoteSnap.data() } : null,
    owner: ownerSnap && ownerSnap.exists ? { id: ownerSnap.id, ...ownerSnap.data() } : null,
    reporter: reporterSnap && reporterSnap.exists ? { id: reporterSnap.id, ...reporterSnap.data() } : null,
  };
}

async function listReports(db, options = {}) {
  const status = options.status;
  let query = db.collection("reports");
  if (status && status !== "ALL") {
    query = query.where("status", "==", status);
  }
  query = query.orderBy("createdAt", options.sort === "asc" ? "asc" : "desc").limit(120);
  const snapshot = await query.get();
  const enriched = await Promise.all(snapshot.docs.map((doc) => enrichReport(db, doc)));

  const filtered = enriched.filter((report) => {
    const queryText = [
      report.quote?.text,
      report.quote?.title,
      report.owner?.username,
      report.reporter?.username,
      report.reportedUserId,
      report.reporterUserId,
      report.quoteId,
      report.id,
    ].join(" ");
    return matchesSearch(queryText, options.search)
      && (!options.reason || options.reason === "ALL" || report.reason === options.reason)
      && (!options.statusFilter || options.statusFilter === "ALL" || report.status === options.statusFilter);
  });

  return paginate(filtered, options.page);
}

async function latestReports(db, status, limit = 5) {
  const snapshot = await db.collection("reports")
    .where("status", "==", status)
    .orderBy("createdAt", "desc")
    .limit(limit)
    .get();
  return Promise.all(snapshot.docs.map((doc) => enrichReport(db, doc)));
}

async function countReports(db, status) {
  const snapshot = await db.collection("reports").where("status", "==", status).get();
  return snapshot.size;
}

async function countHiddenQuotes(db) {
  const snapshot = await db.collection("quotes").where("isHidden", "==", true).limit(1000).get();
  return snapshot.size;
}

async function countTodayActions(db) {
  const start = new Date();
  start.setHours(0, 0, 0, 0);
  const snapshot = await db.collection("moderationActions")
    .where("createdAt", ">=", start)
    .get();
  return snapshot.size;
}

async function loadDashboard(db) {
  const [pending, approved, rejected, hiddenQuotes, todayActions, latestPending, latestActions] = await Promise.all([
    countReports(db, REPORT_STATUS.pending),
    countReports(db, REPORT_STATUS.approved),
    countReports(db, REPORT_STATUS.rejected),
    countHiddenQuotes(db),
    countTodayActions(db),
    latestReports(db, REPORT_STATUS.pending, 5),
    listActions(db, { page: 1, limit: 5 }),
  ]);
  return {
    counts: { pending, approved, rejected, hiddenQuotes, todayActions },
    latestPending,
    latestActions: latestActions.items,
  };
}

async function loadReportDetail(db, reportId) {
  const reportSnap = await db.collection("reports").doc(reportId).get();
  if (!reportSnap.exists) {
    return null;
  }
  const report = await enrichReport(db, reportSnap);
  return {
    report,
    quote: report.quote,
    owner: report.owner,
    reporter: report.reporter,
  };
}

async function listActions(db, options = {}) {
  let query = db.collection("moderationActions");
  if (options.actionType && options.actionType !== "ALL") {
    query = query.where("actionType", "==", options.actionType);
  }
  query = query.orderBy("createdAt", "desc").limit(options.limit || 120);
  const snapshot = await query.get();
  const all = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
  const filtered = all.filter((action) => {
    const text = [
      action.actionType,
      action.reportId,
      action.quoteId,
      action.targetUserId,
      action.actor,
      action.note,
    ].join(" ");
    return matchesSearch(text, options.search);
  });
  return paginate(filtered, options.page, options.limit || PAGE_SIZE);
}

async function reviewReport(db, FieldValue, reportId, decision, actor = "LOCAL_ADMIN", note = "") {
  if (decision !== REPORT_STATUS.approved && decision !== REPORT_STATUS.rejected) {
    throw new Error("Invalid report decision.");
  }
  const reportRef = db.collection("reports").doc(reportId);
  const actionRef = db.collection("moderationActions").doc();

  await db.runTransaction(async (transaction) => {
    const reportSnap = await transaction.get(reportRef);
    if (!reportSnap.exists) {
      throw new Error("Report not found.");
    }
    const report = reportSnap.data();
    const previousStatus = report.status || null;
    if (previousStatus !== REPORT_STATUS.pending) {
      throw new Error("Only pending reports can be reviewed.");
    }

    transaction.update(reportRef, {
      status: decision,
      isValidReport: decision === REPORT_STATUS.approved,
      reviewedBy: actor,
      reviewedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(actionRef, {
      actionType: decision === REPORT_STATUS.approved
        ? ACTION_TYPES.reportApproved
        : ACTION_TYPES.reportRejected,
      reportId,
      quoteId: report.quoteId || null,
      targetUserId: report.reportedUserId || null,
      actor,
      previousStatus,
      newStatus: decision,
      createdAt: FieldValue.serverTimestamp(),
      note: note || null,
    });
  });
}

async function deleteReportedQuote(db, FieldValue, reportId, actor = "LOCAL_ADMIN", note = "") {
  const reportRef = db.collection("reports").doc(reportId);
  const actionRef = db.collection("moderationActions").doc();

  await db.runTransaction(async (transaction) => {
    const reportSnap = await transaction.get(reportRef);
    if (!reportSnap.exists) {
      throw new Error("Report not found.");
    }
    const report = reportSnap.data();
    if (report.status !== REPORT_STATUS.approved || report.isValidReport !== true) {
      throw new Error("Only approved valid reports can remove a quote.");
    }
    if (!report.quoteId) {
      throw new Error("Report has no quoteId.");
    }
    const quoteRef = db.collection("quotes").doc(report.quoteId);
    const quoteSnap = await transaction.get(quoteRef);
    if (!quoteSnap.exists) {
      throw new Error("Quote not found.");
    }
    const quote = quoteSnap.data() || {};

    if (quote.isHidden !== true) {
      transaction.update(quoteRef, {
        isHidden: true,
        hiddenAt: FieldValue.serverTimestamp(),
        hiddenBy: actor,
        hiddenReason: `Approved report ${reportId}`,
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    transaction.create(actionRef, {
      actionType: ACTION_TYPES.quoteDeleted,
      reportId,
      quoteId: report.quoteId,
      targetUserId: report.reportedUserId || null,
      actor,
      previousStatus: report.status || null,
      newStatus: report.status || null,
      createdAt: FieldValue.serverTimestamp(),
      note: note || null,
    });
  });
}

module.exports = {
  ACTION_TYPES,
  REPORT_STATUS,
  PAGE_SIZE,
  toMillis,
  listReports,
  latestReports,
  loadDashboard,
  loadReportDetail,
  listActions,
  reviewReport,
  deleteReportedQuote,
};
