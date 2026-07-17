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

async function listReports(db, status) {
  const snapshot = await db.collection("reports")
    .where("status", "==", status)
    .orderBy("createdAt", "desc")
    .limit(100)
    .get();
  return snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
}

async function loadReportDetail(db, reportId) {
  const reportSnap = await db.collection("reports").doc(reportId).get();
  if (!reportSnap.exists) {
    return null;
  }
  const report = { id: reportSnap.id, ...reportSnap.data() };
  const [quoteSnap, ownerSnap, reporterSnap] = await Promise.all([
    report.quoteId ? db.collection("quotes").doc(report.quoteId).get() : Promise.resolve(null),
    report.reportedUserId ? db.collection("users").doc(report.reportedUserId).get() : Promise.resolve(null),
    report.reporterUserId ? db.collection("users").doc(report.reporterUserId).get() : Promise.resolve(null),
  ]);

  return {
    report,
    quote: quoteSnap && quoteSnap.exists ? { id: quoteSnap.id, ...quoteSnap.data() } : null,
    owner: ownerSnap && ownerSnap.exists ? { id: ownerSnap.id, ...ownerSnap.data() } : null,
    reporter: reporterSnap && reporterSnap.exists ? { id: reporterSnap.id, ...reporterSnap.data() } : null,
  };
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

    transaction.update(quoteRef, {
      isHidden: true,
      hiddenAt: FieldValue.serverTimestamp(),
      hiddenBy: actor,
      hiddenReason: `Approved report ${reportId}`,
      updatedAt: FieldValue.serverTimestamp(),
    });
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
  REPORT_STATUS,
  listReports,
  loadReportDetail,
  reviewReport,
  deleteReportedQuote,
};
