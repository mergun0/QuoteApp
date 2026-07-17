const assert = require("assert");
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

console.log("admin-panel tests passed.");
