import { strict as assert } from "assert";
import * as admin from "firebase-admin";
import { initializeApp, deleteApp, FirebaseApp } from "firebase/app";
import { getAuth, connectAuthEmulator, signInWithCustomToken, Auth } from "firebase/auth";
import { getFunctions, connectFunctionsEmulator, httpsCallable, Functions } from "firebase/functions";

process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8090";
process.env.FIREBASE_AUTH_EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST || "127.0.0.1:9099";
process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT || "demo-quoteapp";

if (admin.apps.length === 0) {
  admin.initializeApp({ projectId: "demo-quoteapp" });
}

const db = admin.firestore();
const authAdmin = admin.auth();

interface ClientContext {
  app: FirebaseApp;
  auth: Auth;
  functions: Functions;
}

async function client(uid?: string, claims: Record<string, unknown> = {}): Promise<ClientContext> {
  const app = initializeApp({
    projectId: "demo-quoteapp",
    apiKey: "demo-key",
    authDomain: "demo-quoteapp.firebaseapp.com",
  }, `app-${uid || "anon"}-${Math.random()}`);
  const auth = getAuth(app);
  connectAuthEmulator(auth, "http://127.0.0.1:9099", { disableWarnings: true });
  const functions = getFunctions(app, "europe-west1");
  connectFunctionsEmulator(functions, "127.0.0.1", 5001);
  if (uid) {
    try {
      await authAdmin.createUser({ uid, email: `${uid}@example.com` });
    } catch {
      // User may already exist in the emulator.
    }
    await authAdmin.setCustomUserClaims(uid, claims);
    const token = await authAdmin.createCustomToken(uid, claims);
    await signInWithCustomToken(auth, token);
  }
  return { app, auth, functions };
}

async function cleanupClient(context: ClientContext): Promise<void> {
  await context.auth.signOut().catch(() => undefined);
  await deleteApp(context.app);
}

async function clearFirestore(): Promise<void> {
  const collections = [
    "quotes",
    "reports",
    "moderationActions",
    "moderationStats",
    "moderatorStats",
    "reporterStats",
    "reportRateLimits",
    "userRestrictions",
    "users",
  ];
  for (const collectionName of collections) {
    const snapshot = await db.collection(collectionName).get();
    const batch = db.batch();
    snapshot.docs.forEach((document) => batch.delete(document.ref));
    await batch.commit();
  }
}

async function seedEstablishedAuthUser(uid: string, claims: Record<string, unknown> = {}): Promise<void> {
  await authAdmin.deleteUser(uid).catch(() => undefined);
  await authAdmin.importUsers([{
    uid,
    email: `${uid}@example.com`,
    metadata: {
      creationTime: new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString(),
    },
  }]);
  await authAdmin.setCustomUserClaims(uid, claims);
}

async function seedQuote(quoteId: string, userId: string, hidden = false): Promise<void> {
  await db.collection("quotes").doc(quoteId).set({
    quoteId,
    userId,
    username: userId,
    type: "Kitap",
    text: "Quote text",
    title: "Book",
    author: "Author",
    characterName: "",
    season: "",
    episode: "",
    tags: [],
    spoiler: false,
    favoriteCount: 0,
    isHidden: hidden,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

async function seedApprovedReport(reportId: string, quoteId: string, reporterUserId: string, reportedUserId: string) {
  await db.collection("reports").doc(reportId).set({
    reportId,
    quoteId,
    reporterUserId,
    reportedUserId,
    reason: "Spam",
    description: "",
    status: "APPROVED",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    reviewedAt: admin.firestore.FieldValue.serverTimestamp(),
    reviewedBy: "mod1",
    isValidReport: true,
  });
}

async function expectHttpsError(promise: Promise<unknown>, code: string): Promise<void> {
  try {
    await promise;
    assert.fail(`Expected ${code}`);
  } catch (error: any) {
    assert.equal(error.code, `functions/${code}`);
  }
}

describe("moderation callable functions", () => {
  beforeEach(async () => {
    await clearFirestore();
  });

  it("submitReport rejects unauthenticated callers", async () => {
    await seedQuote("quote1", "owner1");
    const anon = await client();
    try {
      const submitReport = httpsCallable(anon.functions, "submitReport");
      await expectHttpsError(
        submitReport({ quoteId: "quote1", reason: "Spam" }),
        "unauthenticated",
      );
    } finally {
      await cleanupClient(anon);
    }
  });

  it("submitReport creates a trusted pending report and counters", async () => {
    await seedQuote("quote2", "owner2");
    const reporter = await client("reporter1", { role: "user" });
    try {
      const submitReport = httpsCallable(reporter.functions, "submitReport");
      const result = await submitReport({
        quoteId: "quote2",
        reason: "Spam",
        reportedUserId: "forged",
        status: "APPROVED",
      });
      const data = result.data as Record<string, unknown>;
      assert.equal(data.reportId, "quote2_reporter1");
      assert.equal(data.status, "PENDING");
      const report = (await db.collection("reports").doc("quote2_reporter1").get()).data();
      assert.equal(report?.reportedUserId, "owner2");
      assert.equal(report?.reporterUserId, "reporter1");
      assert.equal(report?.status, "PENDING");
      assert.equal(report?.isValidReport, null);
      const reporterStats = (await db.collection("reporterStats").doc("reporter1").get()).data();
      assert.equal(reporterStats?.submittedReports, 1);
      assert.equal(reporterStats?.pendingReports, 1);
      const moderationStats = (await db.collection("moderationStats").doc("owner2").get()).data();
      assert.equal(moderationStats?.totalReportsReceived, 1);
      assert.equal(moderationStats?.pendingReports, 1);
    } finally {
      await cleanupClient(reporter);
    }
  });

  it("submitReport rejects own quote, nonexistent quote and duplicate report", async () => {
    await seedQuote("quote3", "reporter2");
    const reporter = await client("reporter2", { role: "user" });
    try {
      const submitReport = httpsCallable(reporter.functions, "submitReport");
      await expectHttpsError(submitReport({ quoteId: "quote3", reason: "Spam" }), "failed-precondition");
      await expectHttpsError(submitReport({ quoteId: "missing", reason: "Spam" }), "not-found");
    } finally {
      await cleanupClient(reporter);
    }

    await seedQuote("quote4", "owner4");
    const other = await client("reporter3", { role: "user" });
    try {
      const submitReport = httpsCallable(other.functions, "submitReport");
      await submitReport({ quoteId: "quote4", reason: "Spam" });
      await expectHttpsError(submitReport({ quoteId: "quote4", reason: "Spam" }), "already-exists");
    } finally {
      await cleanupClient(other);
    }
  });

  it("submitReport enforces same-target and active restriction limits", async () => {
    for (let index = 1; index <= 4; index += 1) {
      await seedQuote(`targetQuote${index}`, "targetA");
    }
    await seedEstablishedAuthUser("reporterLimit", { role: "user" });
    const reporter = await client("reporterLimit", { role: "user" });
    try {
      const submitReport = httpsCallable(reporter.functions, "submitReport");
      await submitReport({ quoteId: "targetQuote1", reason: "Spam" });
      await submitReport({ quoteId: "targetQuote2", reason: "Spam" });
      await submitReport({ quoteId: "targetQuote3", reason: "Spam" });
      await expectHttpsError(submitReport({ quoteId: "targetQuote4", reason: "Spam" }), "resource-exhausted");
    } finally {
      await cleanupClient(reporter);
    }

    await seedQuote("restrictedQuote", "ownerRestricted");
    await db.collection("userRestrictions").doc("restrictedReporter").set({
      userId: "restrictedReporter",
      reportingRestrictedUntil: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 60 * 60 * 1000)),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    const restricted = await client("restrictedReporter", { role: "user" });
    try {
      const submitReport = httpsCallable(restricted.functions, "submitReport");
      await expectHttpsError(submitReport({ quoteId: "restrictedQuote", reason: "Spam" }), "resource-exhausted");
    } finally {
      await cleanupClient(restricted);
    }
  });

  it("reviewReport requires custom claims and updates counters once", async () => {
    await seedQuote("quoteReview", "reviewTarget");
    await db.collection("reports").doc("quoteReview_reporterReview").set({
      reportId: "quoteReview_reporterReview",
      quoteId: "quoteReview",
      reportedUserId: "reviewTarget",
      reporterUserId: "reporterReview",
      reason: "Spam",
      description: "",
      status: "PENDING",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      reviewedAt: null,
      reviewedBy: null,
      isValidReport: null,
    });
    await db.collection("reporterStats").doc("reporterReview").set({
      reporterUserId: "reporterReview",
      pendingReports: 1,
    });
    await db.collection("moderationStats").doc("reviewTarget").set({
      userId: "reviewTarget",
      pendingReports: 1,
    });
    await db.collection("users").doc("fakeMod").set({ uid: "fakeMod", role: "moderator" });

    const fakeMod = await client("fakeMod", { role: "user" });
    try {
      const reviewReport = httpsCallable(fakeMod.functions, "reviewReport");
      await expectHttpsError(
        reviewReport({ reportId: "quoteReview_reporterReview", decision: "APPROVED" }),
        "permission-denied",
      );
    } finally {
      await cleanupClient(fakeMod);
    }

    const moderator = await client("mod1", { role: "moderator" });
    try {
      const reviewReport = httpsCallable(moderator.functions, "reviewReport");
      const result = await reviewReport({
        reportId: "quoteReview_reporterReview",
        decision: "APPROVED",
        note: "valid",
      });
      assert.equal((result.data as Record<string, unknown>).status, "APPROVED");
      const report = (await db.collection("reports").doc("quoteReview_reporterReview").get()).data();
      assert.equal(report?.status, "APPROVED");
      assert.equal(report?.isValidReport, true);
      const modStats = (await db.collection("moderatorStats").doc("mod1").get()).data();
      assert.equal(modStats?.totalDecisions, 1);
      assert.equal(modStats?.approvedReports, 1);
      await expectHttpsError(
        reviewReport({ reportId: "quoteReview_reporterReview", decision: "REJECTED" }),
        "failed-precondition",
      );
      const actions = await db.collection("moderationActions").where("actionType", "==", "REPORT_APPROVED").get();
      assert.equal(actions.size, 1);
    } finally {
      await cleanupClient(moderator);
    }
  });

  it("deleteReportedQuote soft hides approved quote once", async () => {
    await seedQuote("quoteDelete", "deleteTarget");
    await seedApprovedReport("quoteDelete_reporter", "quoteDelete", "reporter", "deleteTarget");
    const moderator = await client("modDelete", { role: "moderator" });
    try {
      const deleteReportedQuote = httpsCallable(moderator.functions, "deleteReportedQuote");
      const result = await deleteReportedQuote({ reportId: "quoteDelete_reporter", note: "remove" });
      assert.equal((result.data as Record<string, unknown>).hidden, true);
      const quote = (await db.collection("quotes").doc("quoteDelete").get()).data();
      assert.equal(quote?.isHidden, true);
      const stats = (await db.collection("moderatorStats").doc("modDelete").get()).data();
      assert.equal(stats?.deletedQuotes, 1);
      await expectHttpsError(deleteReportedQuote({ reportId: "quoteDelete_reporter" }), "already-exists");
    } finally {
      await cleanupClient(moderator);
    }
  });

  it("setUserRole is admin-only and preserves unrelated claims", async () => {
    await authAdmin.createUser({ uid: "targetRole", email: "targetRole@example.com" }).catch(() => undefined);
    await authAdmin.setCustomUserClaims("targetRole", { beta: true });
    const moderator = await client("roleMod", { role: "moderator" });
    try {
      const setUserRole = httpsCallable(moderator.functions, "setUserRole");
      await expectHttpsError(setUserRole({ uid: "targetRole", role: "moderator" }), "permission-denied");
    } finally {
      await cleanupClient(moderator);
    }

    const adminUser = await client("admin1", { role: "admin" });
    try {
      const setUserRole = httpsCallable(adminUser.functions, "setUserRole");
      await setUserRole({ uid: "targetRole", role: "moderator" });
      const user = await authAdmin.getUser("targetRole");
      assert.equal(user.customClaims?.role, "moderator");
      assert.equal(user.customClaims?.beta, true);
      const action = await db.collection("moderationActions").where("actionType", "==", "ROLE_CHANGED").get();
      assert.equal(action.size, 1);
    } finally {
      await cleanupClient(adminUser);
    }
  });
});
