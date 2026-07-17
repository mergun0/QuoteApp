const { escapeHtml } = require("../html");
const { alert } = require("./components");

const NAV_ITEMS = [
  { href: "/", key: "dashboard", label: "Dashboard", icon: "⌂" },
  { href: "/reports/PENDING", key: "pending", label: "Bekleyen Raporlar", icon: "!" },
  { href: "/reports/APPROVED", key: "approved", label: "Onaylanan Raporlar", icon: "✓" },
  { href: "/reports/REJECTED", key: "rejected", label: "Reddedilen Raporlar", icon: "×" },
  { href: "/actions", key: "actions", label: "İşlem Geçmişi", icon: "↻" },
];

function sidebar(active) {
  const links = NAV_ITEMS.map((item) => `<a class="nav-link ${active === item.key ? "active" : ""}" href="${item.href}">
    <span class="nav-icon">${item.icon}</span><span>${item.label}</span>
  </a>`).join("");
  return `<aside class="sidebar" id="sidebar">
    <div class="brand">
      <div class="brand-mark">SA</div>
      <div><strong>Satır Arası</strong><span>Admin</span></div>
    </div>
    <nav>${links}</nav>
    <form method="post" action="/logout" class="logout-form">
      <button class="nav-link logout" type="submit"><span class="nav-icon">↩</span><span>Çıkış</span></button>
    </form>
  </aside>`;
}

function topbar(title, subtitle = "") {
  return `<header class="topbar">
    <button class="mobile-menu" type="button" data-sidebar-toggle aria-label="Menüyü aç">☰</button>
    <div>
      <h1>${escapeHtml(title)}</h1>
      ${subtitle ? `<p>${escapeHtml(subtitle)}</p>` : ""}
    </div>
    <div class="topbar-meta">
      <span>${new Date().toLocaleDateString("tr-TR", { weekday: "long", day: "2-digit", month: "long" })}</span>
      <span class="admin-chip">LOCAL_ADMIN</span>
      <button class="button ghost" type="button" onclick="location.reload()">Yenile</button>
    </div>
  </header>`;
}

function page({ title, subtitle, active, body, flash, scripts = "" }) {
  return `<!doctype html>
<html lang="tr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)} · Satır Arası Admin</title>
  <link rel="stylesheet" href="/css/admin.css">
</head>
<body>
  <div class="app-shell">
    ${sidebar(active)}
    <main class="main">
      ${topbar(title, subtitle)}
      ${alert(flash && flash.message, flash && flash.type)}
      ${body}
    </main>
  </div>
  <script src="/js/admin.js"></script>
  ${scripts}
</body>
</html>`;
}

function loginPage(error = "") {
  return `<!doctype html>
<html lang="tr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Giriş · Satır Arası Admin</title>
  <link rel="stylesheet" href="/css/admin.css">
</head>
<body class="login-body">
  <section class="login-card">
    <div class="brand center">
      <div class="brand-mark">SA</div>
      <div><strong>Satır Arası Admin</strong><span>Local moderation</span></div>
    </div>
    <h1>Giriş Yap</h1>
    <p class="muted">Bu panel yalnızca localhost üzerinde kullanılmalıdır.</p>
    ${error ? `<div class="alert alert-error">${escapeHtml(error)}</div>` : ""}
    <form method="post" action="/login" data-submit-lock>
      <label class="field">
        <span>Admin şifresi</span>
        <div class="password-row">
          <input id="password" name="password" type="password" autocomplete="current-password" required>
          <button class="button ghost" type="button" data-toggle-password>Göster</button>
        </div>
      </label>
      <button class="button primary full" type="submit">Giriş Yap</button>
    </form>
    <p class="security-note">LOCAL_ADMIN_PASSWORD en az 16 karakter olmalıdır. Kimlik bilgileri tarayıcıya gönderilmez.</p>
  </section>
  <script src="/js/admin.js"></script>
</body>
</html>`;
}

module.exports = {
  page,
  loginPage,
};
