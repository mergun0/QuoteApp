const crypto = require("crypto");
const { PAGE_SIZE, toMillis } = require("./moderationService");

const REQUEST_STATUS = {
  pending: "PENDING",
  processing: "PROCESSING",
  completed: "COMPLETED",
  failed: "FAILED",
};

const PHASES = [
  "PROFILE",
  "QUOTES",
  "LIKES",
  "FAVORITES",
  "ACHIEVEMENTS",
  "STATS",
  "SOCIAL_RELATIONS",
  "REPORTS_ANONYMIZED",
  "USERNAME_RELEASED",
  "AUTH_DELETED",
  "COMPLETED",
];

const BATCH_LIMIT = 400;

function anonymizedUserRef(uid) {
  return `deleted_${crypto.createHash("sha256")
    .update(`account_deletion:${uid}`)
    .digest("hex")
    .slice(0, 24)}`;
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

async function listDeletionRequests(db, options = {}) {
  const status = options.status || REQUEST_STATUS.pending;
  const snapshot = await db.collection("accountDeletionRequests")
    .where("status", "==", status)
    .orderBy("requestedAt", "desc")
    .limit(120)
    .get();
  const items = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }))
    .sort((a, b) => toMillis(b.requestedAt) - toMillis(a.requestedAt));
  return paginate(items, options.page);
}

async function countQuery(query) {
  const snapshot = await query.get();
  return snapshot.size;
}

async function loadDeletionRequestDetail(db, userId) {
  const requestSnap = await db.collection("accountDeletionRequests").doc(userId).get();
  if (!requestSnap.exists) {
    return null;
  }
  const [
    userSnap,
    quotes,
    likes,
    favorites,
    achievements,
    statsSnap,
    reportsBy,
    reportsAbout,
    actions,
  ] = await Promise.all([
    db.collection("users").doc(userId).get(),
    countQuery(db.collection("quotes").where("userId", "==", userId)),
    countQuery(db.collection("likes").where("userId", "==", userId)),
    countQuery(db.collection("favorites").where("userId", "==", userId)),
    countQuery(db.collection("userAchievements").where("userId", "==", userId)),
    db.collection("userStats").doc(userId).get(),
    countQuery(db.collection("reports").where("reporterUserId", "==", userId)),
    countQuery(db.collection("reports").where("reportedUserId", "==", userId)),
    db.collection("accountDeletionActions")
      .where("requestId", "==", userId)
      .orderBy("createdAt", "desc")
      .limit(50)
      .get(),
  ]);
  return {
    request: { id: requestSnap.id, ...requestSnap.data() },
    user: userSnap.exists ? { id: userSnap.id, ...userSnap.data() } : null,
    counts: {
      quotes,
      likes,
      favorites,
      achievements,
      stats: statsSnap.exists ? 1 : 0,
      moderationReferences: reportsBy + reportsAbout,
    },
    actions: actions.docs.map((doc) => ({ id: doc.id, ...doc.data() })),
  };
}

async function executeAccountDeletion({ db, auth, FieldValue, userId, actor, confirmation }) {
  if (!userId || confirmation !== userId) {
    throw new Error("Confirmation mismatch.");
  }
  const requestRef = db.collection("accountDeletionRequests").doc(userId);
  const requestSnap = await requestRef.get();
  if (!requestSnap.exists) {
    throw new Error("Deletion request not found.");
  }
  const request = requestSnap.data() || {};
  if (request.status === REQUEST_STATUS.completed) {
    return { alreadyCompleted: true };
  }
  if (request.status === REQUEST_STATUS.processing) {
    throw new Error("Deletion is already processing.");
  }
  if (![REQUEST_STATUS.pending, REQUEST_STATUS.failed].includes(request.status)) {
    throw new Error("Deletion request status is not executable.");
  }

  const anonymizedRef = anonymizedUserRef(userId);
  await requestRef.update({
    status: REQUEST_STATUS.processing,
    currentPhase: "STARTED",
    failureCode: null,
    failureMessage: null,
  });
  await writeAudit(db, FieldValue, userId, anonymizedRef, actor, "ACCOUNT_DELETION_STARTED", "STARTED", "SUCCESS");

  try {
    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "PROFILE", async () => {
      await db.collection("users").doc(userId).set({
        deletionPending: true,
        profileHidden: true,
        deletionProcessingAt: FieldValue.serverTimestamp(),
      }, { merge: true });
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "QUOTES", async () => {
      const quoteSnap = await db.collection("quotes").where("userId", "==", userId).get();
      for (const quoteDoc of quoteSnap.docs) {
        await deleteQueryDocs(db, db.collection("likes").where("quoteId", "==", quoteDoc.id));
        await deleteQueryDocs(db, db.collection("favorites").where("quoteId", "==", quoteDoc.id));
      }
      await deleteDocs(db, quoteSnap.docs);
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "LIKES", async () => {
      await deleteQueryDocs(db, db.collection("likes").where("userId", "==", userId));
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "FAVORITES", async () => {
      await deleteQueryDocs(db, db.collection("favorites").where("userId", "==", userId));
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "ACHIEVEMENTS", async () => {
      await deleteQueryDocs(db, db.collection("userAchievements").where("userId", "==", userId));
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "STATS", async () => {
      await deleteKnownDocs(db, [
        db.collection("userStats").doc(userId),
        db.collection("reporterStats").doc(userId),
        db.collection("moderationStats").doc(userId),
        db.collection("moderatorStats").doc(userId),
        db.collection("userRestrictions").doc(userId),
      ]);
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "SOCIAL_RELATIONS", async () => {
      await deleteQueryDocs(db, db.collection("follows").where("followerUserId", "==", userId));
      await deleteQueryDocs(db, db.collection("follows").where("followedUserId", "==", userId));
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "REPORTS_ANONYMIZED", async () => {
      await anonymizeQueryDocs(db, db.collection("reports").where("reporterUserId", "==", userId), {
        reporterUserId: anonymizedRef,
        reporterAnonymized: true,
        anonymizedAt: FieldValue.serverTimestamp(),
      });
      await anonymizeQueryDocs(db, db.collection("reports").where("reportedUserId", "==", userId), {
        reportedUserId: anonymizedRef,
        reportedUserAnonymized: true,
        anonymizedAt: FieldValue.serverTimestamp(),
      });
      await anonymizeQueryDocs(db, db.collection("moderationActions").where("targetUserId", "==", userId), {
        targetUserId: anonymizedRef,
        targetUserAnonymized: true,
        anonymizedAt: FieldValue.serverTimestamp(),
      });
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "USERNAME_RELEASED", async () => {
      if (request.normalizedUsername) {
        await deleteKnownDocs(db, [db.collection("usernames").doc(request.normalizedUsername)]);
      }
      await deleteKnownDocs(db, [db.collection("users").doc(userId)]);
    });

    await runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, "AUTH_DELETED", async () => {
      try {
        await auth.deleteUser(userId);
      } catch (error) {
        if (!error || error.code !== "auth/user-not-found") {
          throw error;
        }
      }
    });

    await requestRef.update({
      status: REQUEST_STATUS.completed,
      currentPhase: "COMPLETED",
      completedAt: FieldValue.serverTimestamp(),
      completedBy: actor,
      anonymizedUserRef: anonymizedRef,
      username: "[silindi]",
      normalizedUsername: null,
      failureCode: null,
      failureMessage: null,
      completedPhases: FieldValue.arrayUnion("COMPLETED"),
    });
    await writeAudit(db, FieldValue, userId, anonymizedRef, actor, "ACCOUNT_DELETION_COMPLETED", "COMPLETED", "SUCCESS");
    return { completed: true };
  } catch (error) {
    console.error("Account deletion failed", { userId, phase: error.phase, error });
    await requestRef.update({
      status: REQUEST_STATUS.failed,
      currentPhase: error.phase || "UNKNOWN",
      failureCode: error.code || "UNKNOWN",
      failureMessage: safeFailureMessage(error),
    });
    await writeAudit(db, FieldValue, userId, anonymizedRef, actor, "ACCOUNT_DELETION_FAILED", error.phase || "UNKNOWN", "FAILED", safeFailureMessage(error));
    throw error;
  }
}

async function runPhase(db, FieldValue, requestRef, userId, anonymizedRef, actor, phase, action) {
  try {
    await requestRef.update({ currentPhase: phase });
    await action();
    await requestRef.update({
      completedPhases: FieldValue.arrayUnion(phase),
      currentPhase: phase,
    });
    await writeAudit(db, FieldValue, userId, anonymizedRef, actor, "ACCOUNT_DELETION_PHASE", phase, "SUCCESS");
  } catch (error) {
    error.phase = phase;
    throw error;
  }
}

async function writeAudit(db, FieldValue, requestId, anonymizedUserRefValue, actor, actionType, phase, status, note = "") {
  await db.collection("accountDeletionActions").doc().create({
    requestId,
    userIdHash: anonymizedUserRefValue,
    actor,
    actionType,
    phase,
    status,
    createdAt: FieldValue.serverTimestamp(),
    note: note || null,
    errorCode: status === "FAILED" ? actionType : null,
  });
}

async function deleteQueryDocs(db, query) {
  const snapshot = await query.get();
  await deleteDocs(db, snapshot.docs);
}

async function deleteDocs(db, docs) {
  for (let index = 0; index < docs.length; index += BATCH_LIMIT) {
    const batch = db.batch();
    docs.slice(index, index + BATCH_LIMIT).forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }
}

async function deleteKnownDocs(db, refs) {
  for (let index = 0; index < refs.length; index += BATCH_LIMIT) {
    const batch = db.batch();
    refs.slice(index, index + BATCH_LIMIT).forEach((ref) => batch.delete(ref));
    await batch.commit();
  }
}

async function anonymizeQueryDocs(db, query, data) {
  const snapshot = await query.get();
  for (let index = 0; index < snapshot.docs.length; index += BATCH_LIMIT) {
    const batch = db.batch();
    snapshot.docs.slice(index, index + BATCH_LIMIT).forEach((doc) => batch.set(doc.ref, data, { merge: true }));
    await batch.commit();
  }
}

function safeFailureMessage(error) {
  if (error && error.code === "auth/user-not-found") {
    return "Auth kullanıcısı zaten bulunamadı.";
  }
  return "Hesap silme işlemi tamamlanamadı. Sunucu loglarını kontrol edin.";
}

module.exports = {
  REQUEST_STATUS,
  PHASES,
  anonymizedUserRef,
  listDeletionRequests,
  loadDeletionRequestDetail,
  executeAccountDeletion,
};
