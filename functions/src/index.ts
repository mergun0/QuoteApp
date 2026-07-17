import { HttpsError, onCall } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";
import { ACTION_TYPES, addHours, MODERATION_POLICY, REGION, REPORT_STATUS, turkeyDateKey } from "./config";
import { auth, db } from "./firebase";
import { requireAdmin, requireModerator, requireUid } from "./auth";
import { clampNonNegative, decrement, isHiddenQuote, stringField, validateReason } from "./validation";

setGlobalOptions({ region: REGION });

type Decision = "APPROVED" | "REJECTED";
type UserRole = "user" | "moderator" | "admin";

const callableOptions = {
  region: REGION,
  enforceAppCheck: false,
};

function moderationActionRef() {
  return db.collection("moderationActions").doc();
}

function nowDate(): Date {
  return new Date();
}

function millisFromFirestoreDate(value: unknown): number {
  if (value instanceof Date) {
    return value.getTime();
  }
  if (value && typeof value === "object" && "toMillis" in value) {
    const toMillis = (value as { toMillis?: () => number }).toMillis;
    return typeof toMillis === "function" ? toMillis.call(value) : 0;
  }
  return 0;
}

function actionPayload(params: {
  actionId: string;
  actorUserId: string;
  actorRole: string;
  actionType: string;
  reportId?: string | null;
  quoteId?: string | null;
  targetUserId?: string | null;
  previousStatus?: string | null;
  newStatus?: string | null;
  reason?: string | null;
  note?: string | null;
  metadata?: Record<string, unknown>;
}) {
  return {
    actionId: params.actionId,
    actorUserId: params.actorUserId,
    actorRole: params.actorRole,
    actionType: params.actionType,
    reportId: params.reportId ?? null,
    quoteId: params.quoteId ?? null,
    targetUserId: params.targetUserId ?? null,
    previousStatus: params.previousStatus ?? null,
    newStatus: params.newStatus ?? null,
    reason: params.reason ?? null,
    note: params.note ?? null,
    metadata: params.metadata ?? {},
    createdAt: nowDate(),
  };
}

function safeTargetCounts(value: unknown): Record<string, number> {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return {};
  }
  const result: Record<string, number> = {};
  for (const [key, raw] of Object.entries(value as Record<string, unknown>)) {
    if (typeof key === "string") {
      result[key] = clampNonNegative(raw);
    }
  }
  return result;
}

async function accountDailyLimit(uid: string): Promise<number> {
  try {
    const user = await auth.getUser(uid);
    const createdAt = user.metadata.creationTime ? new Date(user.metadata.creationTime).getTime() : 0;
    const ageHours = createdAt > 0 ? (Date.now() - createdAt) / (60 * 60 * 1000) : Number.MAX_SAFE_INTEGER;
    return ageHours < MODERATION_POLICY.newAccountAgeHours ?
      MODERATION_POLICY.newAccountDailyLimit :
      MODERATION_POLICY.establishedDailyLimit;
  } catch {
    return MODERATION_POLICY.newAccountDailyLimit;
  }
}

export const submitReport = onCall(callableOptions, async (request) => {
  const reporterUid = requireUid(request);
  const quoteId = stringField(request.data, "quoteId", true, MODERATION_POLICY.quoteIdMaxLength);
  const reason = stringField(request.data, "reason", true, MODERATION_POLICY.reasonMaxLength);
  const description = stringField(request.data, "description", false, MODERATION_POLICY.descriptionMaxLength);
  validateReason(reason);

  const dailyLimit = await accountDailyLimit(reporterUid);
  const dateKey = turkeyDateKey();
  const reportId = `${quoteId}_${reporterUid}`;
  const now = nowDate();

  return db.runTransaction(async (transaction) => {
    const quoteRef = db.collection("quotes").doc(quoteId);
    const reportRef = db.collection("reports").doc(reportId);
    const restrictionRef = db.collection("userRestrictions").doc(reporterUid);
    const rateLimitRef = db.collection("reportRateLimits").doc(`${reporterUid}_${dateKey}`);

    const [quoteSnap, reportSnap, restrictionSnap, rateLimitSnap] = await Promise.all([
      transaction.get(quoteRef),
      transaction.get(reportRef),
      transaction.get(restrictionRef),
      transaction.get(rateLimitRef),
    ]);

    if (!quoteSnap.exists) {
      throw new HttpsError("not-found", "Alıntı bulunamadı.");
    }
    const quote = quoteSnap.data() ?? {};
    if (isHiddenQuote(quote)) {
      throw new HttpsError("failed-precondition", "Bu alıntı raporlanamaz.");
    }
    const reportedUserId = typeof quote.userId === "string" ? quote.userId : "";
    if (!reportedUserId) {
      throw new HttpsError("failed-precondition", "Alıntı sahibi doğrulanamadı.");
    }
    if (reportedUserId === reporterUid) {
      throw new HttpsError("failed-precondition", "Kendi alıntını raporlayamazsın.");
    }
    if (reportSnap.exists) {
      throw new HttpsError("already-exists", "Bu alıntı daha önce raporlandı.");
    }

    const restriction = restrictionSnap.data() ?? {};
    const restrictedUntil = restriction.reportingRestrictedUntil;
    if (millisFromFirestoreDate(restrictedUntil) > now.getTime()) {
      throw new HttpsError("resource-exhausted", "Rapor gönderme sınırına ulaşıldı.");
    }

    const rateLimit = rateLimitSnap.data() ?? {};
    const reportCount = clampNonNegative(rateLimit.reportCount);
    const targetCounts = safeTargetCounts(rateLimit.targetCounts);
    const targetCount = clampNonNegative(targetCounts[reportedUserId]);
    if (reportCount >= dailyLimit || targetCount >= MODERATION_POLICY.sameTargetDailyLimit) {
      throw new HttpsError("resource-exhausted", "Rapor gönderme sınırına ulaşıldı.");
    }

    const moderationStatsRef = db.collection("moderationStats").doc(reportedUserId);
    const reporterStatsRef = db.collection("reporterStats").doc(reporterUid);
    const [moderationStatsSnap, reporterStatsSnap] = await Promise.all([
      transaction.get(moderationStatsRef),
      transaction.get(reporterStatsRef),
    ]);
    const moderationStats = moderationStatsSnap.data() ?? {};
    const reporterStats = reporterStatsSnap.data() ?? {};

    transaction.create(reportRef, {
      reportId,
      quoteId,
      reportedUserId,
      reporterUserId: reporterUid,
      reason,
      description,
      status: REPORT_STATUS.pending,
      createdAt: nowDate(),
      reviewedAt: null,
      reviewedBy: null,
      isValidReport: null,
    });

    targetCounts[reportedUserId] = targetCount + 1;
    transaction.set(rateLimitRef, {
      userId: reporterUid,
      dateKey,
      reportCount: reportCount + 1,
      targetCounts,
      lastReportAt: nowDate(),
      createdAt: rateLimit.createdAt ?? nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });

    transaction.set(reporterStatsRef, {
      reporterUserId: reporterUid,
      submittedReports: clampNonNegative(reporterStats.submittedReports) + 1,
      approvedReports: clampNonNegative(reporterStats.approvedReports),
      rejectedReports: clampNonNegative(reporterStats.rejectedReports),
      pendingReports: clampNonNegative(reporterStats.pendingReports) + 1,
      currentInvalidStreak: clampNonNegative(reporterStats.currentInvalidStreak),
      recentInvalidReports: clampNonNegative(reporterStats.recentInvalidReports),
      reportRestrictionUntil: reporterStats.reportRestrictionUntil ?? null,
      lastReportAt: nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });

    transaction.set(moderationStatsRef, {
      userId: reportedUserId,
      totalReportsReceived: clampNonNegative(moderationStats.totalReportsReceived) + 1,
      pendingReports: clampNonNegative(moderationStats.pendingReports) + 1,
      validReportsReceived: clampNonNegative(moderationStats.validReportsReceived),
      invalidReportsReceived: clampNonNegative(moderationStats.invalidReportsReceived),
      removedQuotesCount: clampNonNegative(moderationStats.removedQuotesCount),
      warningCount: clampNonNegative(moderationStats.warningCount),
      temporarySuspensionCount: clampNonNegative(moderationStats.temporarySuspensionCount),
      lastModerationAt: moderationStats.lastModerationAt ?? null,
      updatedAt: nowDate(),
    }, { merge: true });

    return {
      reportId,
      status: REPORT_STATUS.pending,
      remainingDailyReports: Math.max(0, dailyLimit - reportCount - 1),
    };
  });
});

export const reviewReport = onCall(callableOptions, async (request) => {
  const moderator = requireModerator(request);
  const reportId = stringField(request.data, "reportId", true, 256);
  const decision = stringField(request.data, "decision", true, 20) as Decision;
  const note = stringField(request.data, "note", false, MODERATION_POLICY.moderatorNoteMaxLength);
  if (decision !== REPORT_STATUS.approved && decision !== REPORT_STATUS.rejected) {
    throw new HttpsError("invalid-argument", "Geçersiz karar.");
  }

  return db.runTransaction(async (transaction) => {
    const reportRef = db.collection("reports").doc(reportId);
    const reportSnap = await transaction.get(reportRef);
    if (!reportSnap.exists) {
      throw new HttpsError("not-found", "Rapor bulunamadı.");
    }
    const report = reportSnap.data() ?? {};
    if (report.status !== REPORT_STATUS.pending || report.reviewedAt || report.reviewedBy) {
      throw new HttpsError("failed-precondition", "Rapor zaten incelenmiş.");
    }

    const targetUserId = String(report.reportedUserId || "");
    const reporterUserId = String(report.reporterUserId || "");
    const moderationStatsRef = db.collection("moderationStats").doc(targetUserId);
    const reporterStatsRef = db.collection("reporterStats").doc(reporterUserId);
    const moderatorStatsRef = db.collection("moderatorStats").doc(moderator.uid);
    const restrictionRef = db.collection("userRestrictions").doc(reporterUserId);

    const [moderationStatsSnap, reporterStatsSnap, moderatorStatsSnap] = await Promise.all([
      transaction.get(moderationStatsRef),
      transaction.get(reporterStatsRef),
      transaction.get(moderatorStatsRef),
    ]);
    const moderationStats = moderationStatsSnap.data() ?? {};
    const reporterStats = reporterStatsSnap.data() ?? {};
    const moderatorStats = moderatorStatsSnap.data() ?? {};
    const approved = decision === REPORT_STATUS.approved;
    const rejectedReports = clampNonNegative(reporterStats.rejectedReports) + (approved ? 0 : 1);
    const invalidStreak = approved ? 0 : clampNonNegative(reporterStats.currentInvalidStreak) + 1;
    const recentInvalid = approved ? clampNonNegative(reporterStats.recentInvalidReports) :
      clampNonNegative(reporterStats.recentInvalidReports) + 1;
    const restrictionUntil = restrictionForReporter(invalidStreak, recentInvalid);

    transaction.update(reportRef, {
      status: decision,
      isValidReport: approved,
      reviewedBy: moderator.uid,
      reviewedAt: nowDate(),
    });

    transaction.set(moderationStatsRef, {
      userId: targetUserId,
      pendingReports: decrement(moderationStats.pendingReports),
      validReportsReceived: clampNonNegative(moderationStats.validReportsReceived) + (approved ? 1 : 0),
      invalidReportsReceived: clampNonNegative(moderationStats.invalidReportsReceived) + (approved ? 0 : 1),
      lastModerationAt: nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });

    transaction.set(reporterStatsRef, {
      reporterUserId,
      pendingReports: decrement(reporterStats.pendingReports),
      approvedReports: clampNonNegative(reporterStats.approvedReports) + (approved ? 1 : 0),
      rejectedReports,
      currentInvalidStreak: invalidStreak,
      recentInvalidReports: recentInvalid,
      reportRestrictionUntil: restrictionUntil,
      updatedAt: nowDate(),
    }, { merge: true });

    if (restrictionUntil) {
      transaction.set(restrictionRef, {
        userId: reporterUserId,
        reportingRestrictedUntil: restrictionUntil,
        accountSuspendedUntil: null,
        reason: "INVALID_REPORT_PATTERN",
        setBy: "system",
        createdAt: nowDate(),
        updatedAt: nowDate(),
      }, { merge: true });
    }

    transaction.set(moderatorStatsRef, {
      moderatorUserId: moderator.uid,
      totalDecisions: clampNonNegative(moderatorStats.totalDecisions) + 1,
      approvedReports: clampNonNegative(moderatorStats.approvedReports) + (approved ? 1 : 0),
      rejectedReports: clampNonNegative(moderatorStats.rejectedReports) + (approved ? 0 : 1),
      deletedQuotes: clampNonNegative(moderatorStats.deletedQuotes),
      reversedDecisions: clampNonNegative(moderatorStats.reversedDecisions),
      warningsIssued: clampNonNegative(moderatorStats.warningsIssued),
      usersSuspended: clampNonNegative(moderatorStats.usersSuspended),
      lastActionAt: nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });

    const actionRef = moderationActionRef();
    transaction.create(actionRef, actionPayload({
      actionId: actionRef.id,
      actorUserId: moderator.uid,
      actorRole: moderator.role,
      actionType: approved ? ACTION_TYPES.reportApproved : ACTION_TYPES.reportRejected,
      reportId,
      quoteId: String(report.quoteId || ""),
      targetUserId,
      previousStatus: REPORT_STATUS.pending,
      newStatus: decision,
      reason: String(report.reason || ""),
      note,
    }));

    return {
      reportId,
      status: decision,
      reviewedAt: new Date().toISOString(),
    };
  });
});

function restrictionForReporter(invalidStreak: number, recentInvalid: number) {
  const now = new Date();
  if (recentInvalid >= MODERATION_POLICY.rejectedWindowForLongRestriction) {
    return addHours(now, MODERATION_POLICY.longRestrictionHours);
  }
  if (invalidStreak >= MODERATION_POLICY.rejectedStreakForShortRestriction) {
    return addHours(now, MODERATION_POLICY.shortRestrictionHours);
  }
  return null;
}

export const deleteReportedQuote = onCall(callableOptions, async (request) => {
  const moderator = requireModerator(request);
  const reportId = stringField(request.data, "reportId", true, 256);
  const note = stringField(request.data, "note", false, MODERATION_POLICY.moderatorNoteMaxLength);

  return db.runTransaction(async (transaction) => {
    const reportRef = db.collection("reports").doc(reportId);
    const reportSnap = await transaction.get(reportRef);
    if (!reportSnap.exists) {
      throw new HttpsError("not-found", "Rapor bulunamadı.");
    }
    const report = reportSnap.data() ?? {};
    if (report.status !== REPORT_STATUS.approved) {
      throw new HttpsError("failed-precondition", "Yalnızca onaylı raporlar için işlem yapılabilir.");
    }
    const quoteId = String(report.quoteId || "");
    const targetUserId = String(report.reportedUserId || "");
    const quoteRef = db.collection("quotes").doc(quoteId);
    const moderationStatsRef = db.collection("moderationStats").doc(targetUserId);
    const moderatorStatsRef = db.collection("moderatorStats").doc(moderator.uid);
    const [quoteSnap, moderationStatsSnap, moderatorStatsSnap] = await Promise.all([
      transaction.get(quoteRef),
      transaction.get(moderationStatsRef),
      transaction.get(moderatorStatsRef),
    ]);
    if (!quoteSnap.exists) {
      throw new HttpsError("not-found", "Alıntı bulunamadı.");
    }
    const quote = quoteSnap.data() ?? {};
    if (quote.userId !== targetUserId) {
      throw new HttpsError("failed-precondition", "Alıntı sahibi raporla eşleşmiyor.");
    }
    if (quote.isHidden === true) {
      throw new HttpsError("already-exists", "Alıntı zaten gizlenmiş.");
    }

    const moderationStats = moderationStatsSnap.data() ?? {};
    const moderatorStats = moderatorStatsSnap.data() ?? {};
    transaction.update(quoteRef, {
      isHidden: true,
      hiddenReason: "MODERATION",
      hiddenAt: nowDate(),
      hiddenBy: moderator.uid,
    });
    transaction.set(moderationStatsRef, {
      userId: targetUserId,
      removedQuotesCount: clampNonNegative(moderationStats.removedQuotesCount) + 1,
      lastModerationAt: nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });
    transaction.set(moderatorStatsRef, {
      moderatorUserId: moderator.uid,
      deletedQuotes: clampNonNegative(moderatorStats.deletedQuotes) + 1,
      lastActionAt: nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });

    const actionRef = moderationActionRef();
    transaction.create(actionRef, actionPayload({
      actionId: actionRef.id,
      actorUserId: moderator.uid,
      actorRole: moderator.role,
      actionType: ACTION_TYPES.quoteRemoved,
      reportId,
      quoteId,
      targetUserId,
      previousStatus: REPORT_STATUS.approved,
      newStatus: "QUOTE_HIDDEN",
      reason: "MODERATION",
      note,
    }));

    return { reportId, quoteId, hidden: true };
  });
});

export const setUserRole = onCall(callableOptions, async (request) => {
  const adminUser = requireAdmin(request);
  const uid = stringField(request.data, "uid", true, 128);
  const role = stringField(request.data, "role", true, 20) as UserRole;
  if (!["user", "moderator", "admin"].includes(role)) {
    throw new HttpsError("invalid-argument", "Geçersiz rol.");
  }
  const user = await auth.getUser(uid);
  const previousRole = user.customClaims?.role || "user";
  await auth.setCustomUserClaims(uid, { ...(user.customClaims ?? {}), role });
  const actionRef = moderationActionRef();
  await db.runTransaction(async (transaction) => {
    transaction.set(db.collection("users").doc(uid), { role }, { merge: true });
    transaction.create(actionRef, actionPayload({
      actionId: actionRef.id,
      actorUserId: adminUser.uid,
      actorRole: adminUser.role,
      actionType: ACTION_TYPES.roleChanged,
      targetUserId: uid,
      previousStatus: String(previousRole),
      newStatus: role,
    }));
  });
  return { uid, role };
});

export const setReportingRestriction = onCall(callableOptions, async (request) => {
  const adminUser = requireAdmin(request);
  const uid = stringField(request.data, "uid", true, 128);
  const reason = stringField(request.data, "reason", false, 500);
  const untilIso = stringField(request.data, "reportingRestrictedUntil", false, 80);
  const clear = Boolean((request.data as Record<string, unknown> | undefined)?.clear);
  const until = clear || !untilIso ? null : new Date(untilIso);
  const actionRef = moderationActionRef();

  await db.runTransaction(async (transaction) => {
    transaction.set(db.collection("userRestrictions").doc(uid), {
      userId: uid,
      reportingRestrictedUntil: until,
      accountSuspendedUntil: null,
      reason: clear ? null : reason,
      setBy: adminUser.uid,
      createdAt: nowDate(),
      updatedAt: nowDate(),
    }, { merge: true });
    transaction.create(actionRef, actionPayload({
      actionId: actionRef.id,
      actorUserId: adminUser.uid,
      actorRole: adminUser.role,
      actionType: ACTION_TYPES.reportingRestrictionSet,
      targetUserId: uid,
      previousStatus: null,
      newStatus: clear ? "CLEARED" : "RESTRICTED",
      reason,
    }));
  });

  return { uid, reportingRestrictedUntil: until ? until.toISOString() : null };
});
