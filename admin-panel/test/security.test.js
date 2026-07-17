const assert = require("assert");
const fs = require("fs");
const path = require("path");
const {
  parseCookies,
  verifyPassword,
  createSession,
  sessionFromRequest,
  isLoginThrottled,
  recordLoginFailure,
} = require("../src/security");
const { escapeHtml } = require("../src/html");
const { loginPage, page } = require("../src/views/layout");
const { dashboardView } = require("../src/views/dashboard");
const { reportsView, detailView } = require("../src/views/reports");
const { actionsView } = require("../src/views/actions");
const {
  accountDeletionListView,
  accountDeletionDetailView,
} = require("../src/views/accountDeletions");
const {
  anonymizedUserRef,
  PHASES,
} = require("../src/accountDeletionService");
const {
  parseArgs,
  runBackfillQuoteVisibility,
} = require("../scripts/backfillQuoteVisibility");

function req(cookie = "", ip = "127.0.0.1") {
  return { headers: { cookie }, ip, socket: { remoteAddress: ip } };
}

assert.deepStrictEqual(parseCookies("a=1; b=two%20words"), {
  a: "1",
  b: "two words",
});

assert.strictEqual(verifyPassword("super-secret-password", "super-secret-password"), true);
assert.strictEqual(verifyPassword("wrong-secret-password", "super-secret-password"), false);
assert.throws(() => verifyPassword("short", "too-short"), /at least 16/);

for (let index = 0; index < 5; index += 1) {
  recordLoginFailure(req("", "10.0.0.1"));
}
assert.strictEqual(isLoginThrottled(req("", "10.0.0.1")), true);

const session = createSession();
assert.ok(session.sessionId.length >= 32);
assert.ok(session.csrfToken.length >= 32);
assert.ok(sessionFromRequest(req(`qa_admin_session=${session.sessionId}`)));

assert.strictEqual(escapeHtml("<script>alert('x')</script>"), "&lt;script&gt;alert(&#039;x&#039;)&lt;/script&gt;");

const loginHtml = loginPage("Giriş bilgileri geçersiz.");
assert.ok(loginHtml.includes("Satır Arası Admin"));
assert.ok(loginHtml.includes("data-toggle-password"));
assert.ok(loginHtml.includes("Giriş bilgileri geçersiz."));

const shell = page({
  title: "Dashboard",
  subtitle: "Test",
  active: "dashboard",
  body: "<section>Body</section>",
});
assert.ok(shell.includes("Dashboard"));
assert.ok(shell.includes("Bekleyen Raporlar"));
assert.ok(shell.includes("Hesap Silme Talepleri"));
assert.ok(shell.includes("/css/admin.css"));
assert.ok(shell.includes("/js/admin.js"));

const dashboardHtml = dashboardView({
  counts: { pending: 2, approved: 1, rejected: 0, hiddenQuotes: 3, todayActions: 4 },
  latestPending: [],
  latestActions: [],
});
assert.ok(dashboardHtml.includes("Bekleyen Raporlar"));
assert.ok(dashboardHtml.includes("Cloud Functions: inactive"));

const pageData = {
  items: [{
    id: "quote1_user1",
    quoteId: "quote1",
    reportedUserId: "owner1",
    reporterUserId: "user1",
    reason: "SPAM",
    status: "PENDING",
    createdAt: new Date(),
    quote: { text: "Uzun olmayan alıntı", title: "Kitap" },
    owner: { username: "Owner" },
    reporter: { username: "Reporter" },
  }],
  page: 1,
  totalPages: 1,
  totalItems: 1,
  hasPrevious: false,
  hasNext: false,
};
assert.ok(reportsView("PENDING", pageData, { reason: "ALL", sort: "desc" }).includes("Uzun olmayan alıntı"));

const detailHtml = detailView({
  report: {
    id: "quote1_user1",
    quoteId: "quote1",
    reportedUserId: "owner1",
    reporterUserId: "user1",
    reason: "SPAM",
    status: "APPROVED",
    isValidReport: true,
    createdAt: new Date(),
  },
  quote: { text: "Tam alıntı", title: "Kaynak", type: "Kitap", isHidden: false },
  owner: { username: "Owner" },
  reporter: { username: "Reporter" },
}, "csrf-token");
assert.ok(detailHtml.includes("data-confirm"));
assert.ok(detailHtml.includes("Alıntıyı Gizle"));
assert.ok(detailHtml.includes("_csrf"));

const actionsHtml = actionsView({
  items: [{
    actionType: "REPORT_APPROVED",
    reportId: "quote1_user1",
    quoteId: "quote1",
    targetUserId: "owner1",
    previousStatus: "PENDING",
    newStatus: "APPROVED",
    actor: "LOCAL_ADMIN",
    createdAt: new Date(),
    note: "<private>",
  }],
  page: 1,
  totalPages: 1,
  totalItems: 1,
  hasPrevious: false,
  hasNext: false,
}, { actionType: "ALL" });
assert.ok(actionsHtml.includes("REPORT_APPROVED"));
assert.ok(!actionsHtml.includes("<private>"));
assert.ok(actionsHtml.includes("&lt;private&gt;"));

const deletionListHtml = accountDeletionListView("PENDING", {
  items: [{
    id: "deleteUser",
    userId: "deleteUser",
    username: "Delete Me",
    status: "PENDING",
    requestedAt: new Date(),
    currentPhase: "REQUESTED",
  }],
  page: 1,
  totalPages: 1,
  totalItems: 1,
  hasPrevious: false,
  hasNext: false,
});
assert.ok(deletionListHtml.includes("Delete Me"));
assert.ok(deletionListHtml.includes("/account-deletion/deleteUser"));

const deletionDetailHtml = accountDeletionDetailView({
  request: {
    id: "deleteUser",
    userId: "deleteUser",
    username: "Delete Me",
    status: "PENDING",
    requestedAt: new Date(),
    reason: "<private>",
    profileHidden: true,
    completedPhases: ["PROFILE"],
  },
  user: { id: "deleteUser" },
  counts: {
    quotes: 2,
    likes: 3,
    favorites: 4,
    achievements: 1,
    stats: 1,
    moderationReferences: 5,
  },
  actions: [],
}, "csrf-token");
assert.ok(deletionDetailHtml.includes("_csrf"));
assert.ok(deletionDetailHtml.includes("confirmation"));
assert.ok(deletionDetailHtml.includes("Firebase Auth kullanıcısı en son silinir"));
assert.ok(!deletionDetailHtml.includes("<private>"));
assert.ok(deletionDetailHtml.includes("&lt;private&gt;"));
assert.strictEqual(PHASES[PHASES.length - 1], "COMPLETED");
assert.strictEqual(anonymizedUserRef("deleteUser"), anonymizedUserRef("deleteUser"));
assert.notStrictEqual(anonymizedUserRef("deleteUser"), anonymizedUserRef("otherUser"));
assert.ok(anonymizedUserRef("deleteUser").startsWith("deleted_"));

assert.deepStrictEqual(parseArgs([], {}), { apply: false, verify: false });
assert.deepStrictEqual(parseArgs(["--", "--apply"], {}), { apply: true, verify: false });
assert.deepStrictEqual(parseArgs(["--", "--verify"], {}), { apply: false, verify: true });
assert.deepStrictEqual(parseArgs([], { npm_config_apply: "true" }), { apply: true, verify: false });
assert.deepStrictEqual(parseArgs([], { npm_config_verify: "true" }), { apply: false, verify: true });
assert.deepStrictEqual(parseArgs([], {
  npm_config_argv: JSON.stringify({ original: ["run", "backfill:quote-visibility", "--", "--verify"] }),
}), { apply: false, verify: true });
assert.throws(() => parseArgs(["--wat"], {}), /Unknown argument/);
assert.throws(() => parseArgs(["--apply", "--verify"], {}), /either --verify or --apply/);

const indexesPath = path.resolve(__dirname, "..", "..", "firestore.indexes.json");
const firestoreIndexes = JSON.parse(fs.readFileSync(indexesPath, "utf8"));
assert.ok(Array.isArray(firestoreIndexes.indexes));
assert.ok(Array.isArray(firestoreIndexes.fieldOverrides));
const indexKeys = new Set();
for (const index of firestoreIndexes.indexes) {
  assert.ok(index.collectionGroup);
  assert.strictEqual(index.queryScope, "COLLECTION");
  assert.ok(Array.isArray(index.fields));
  assert.ok(index.fields.length > 0);
  const key = JSON.stringify({
    collectionGroup: index.collectionGroup,
    queryScope: index.queryScope,
    fields: index.fields,
  });
  assert.ok(!indexKeys.has(key), `Duplicate Firestore index: ${key}`);
  indexKeys.add(key);
}

function createFakeQuoteDb(seedQuotes) {
  const quotes = new Map(Object.entries(seedQuotes).map(([id, data]) => [id, { ...data }]));
  const updates = [];
  return {
    quotes,
    updates,
    batch() {
      const operations = [];
      return {
        update(ref, data) {
          operations.push({ ref, data });
        },
        async commit() {
          for (const operation of operations) {
            const existing = quotes.get(operation.ref.id);
            quotes.set(operation.ref.id, { ...existing, ...operation.data });
            updates.push({ id: operation.ref.id, data: operation.data });
          }
        },
      };
    },
    collection(name) {
      assert.strictEqual(name, "quotes");
      return {
        orderBy() {
          return this;
        },
        limit() {
          return this;
        },
        startAfter() {
          return this;
        },
        async get() {
          return {
            docs: Array.from(quotes.entries()).map(([id, data]) => ({
              id,
              data: () => ({ ...data }),
              ref: { id },
            })),
          };
        },
      };
    },
  };
}

function silentLogger() {
  return {
    log() {},
    warn() {},
    error() {},
  };
}

(async () => {
  const dryRunDb = createFakeQuoteDb({
    missing: { text: "legacy" },
    visible: { text: "visible", isHidden: false },
    hidden: { text: "hidden", isHidden: true },
  });
  const dryRunTotals = await runBackfillQuoteVisibility({
    db: dryRunDb,
    logger: silentLogger(),
  });
  assert.strictEqual(dryRunTotals.scanned, 3);
  assert.strictEqual(dryRunTotals.missingField, 1);
  assert.strictEqual(dryRunTotals.alreadyVisible, 1);
  assert.strictEqual(dryRunTotals.alreadyHidden, 1);
  assert.strictEqual(dryRunTotals.updated, 0);
  assert.strictEqual(dryRunDb.quotes.get("missing").isHidden, undefined);

  const applyDb = createFakeQuoteDb({
    missing: { text: "legacy" },
    visible: { text: "visible", isHidden: false },
    hidden: { text: "hidden", isHidden: true },
  });
  const applyTotals = await runBackfillQuoteVisibility({
    db: applyDb,
    apply: true,
    logger: silentLogger(),
  });
  assert.strictEqual(applyTotals.updated, 1);
  assert.strictEqual(applyDb.quotes.get("missing").isHidden, false);
  assert.strictEqual(applyDb.quotes.get("visible").isHidden, false);
  assert.strictEqual(applyDb.quotes.get("hidden").isHidden, true);

  const secondApplyTotals = await runBackfillQuoteVisibility({
    db: applyDb,
    apply: true,
    logger: silentLogger(),
  });
  assert.strictEqual(secondApplyTotals.missingField, 0);
  assert.strictEqual(secondApplyTotals.updated, 0);

  const verifyDb = createFakeQuoteDb({
    missing: { text: "legacy" },
  });
  await assert.rejects(
    runBackfillQuoteVisibility({ db: verifyDb, verify: true, logger: silentLogger() }),
    /Verification failed/
  );

  console.log("admin-panel tests passed.");
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
