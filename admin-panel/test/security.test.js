const assert = require("assert");
const {
  parseCookies,
  verifyPassword,
} = require("../src/security");
const { escapeHtml } = require("../src/html");

assert.deepStrictEqual(parseCookies("a=1; b=two%20words"), {
  a: "1",
  b: "two words",
});

assert.strictEqual(verifyPassword("super-secret-password", "super-secret-password"), true);
assert.strictEqual(verifyPassword("wrong-secret-password", "super-secret-password"), false);
assert.throws(() => verifyPassword("short", "too-short"), /at least 16/);

assert.strictEqual(escapeHtml("<script>alert('x')</script>"), "&lt;script&gt;alert(&#039;x&#039;)&lt;/script&gt;");

console.log("admin-panel tests passed.");
