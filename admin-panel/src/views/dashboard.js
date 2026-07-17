const { escapeHtml } = require("../html");
const { formatDate, reasonBadge, statusBadge, emptyState } = require("./components");

function statCard(title, value, label, href, icon, tone = "") {
  return `<a class="stat-card ${tone}" href="${href}">
    <div class="stat-icon">${icon}</div>
    <div><strong>${value}</strong><span>${escapeHtml(title)}</span><small>${escapeHtml(label)}</small></div>
  </a>`;
}

function reportMiniList(reports) {
  if (!reports.length) {
    return emptyState("Henüz bekleyen rapor bulunmuyor.", "Yeni raporlar geldiğinde burada görünecek.");
  }
  return `<div class="mini-list">${reports.map((report) => `<a href="/report/${encodeURIComponent(report.id)}" class="mini-item">
    <div>${reasonBadge(report.reason)} ${statusBadge(report.status)}</div>
    <strong>${escapeHtml(report.quote?.text || report.quoteId || "Alıntı bulunamadı")}</strong>
    <span>${escapeHtml(report.owner?.username || report.reportedUserId || "-")} · ${formatDate(report.createdAt)}</span>
  </a>`).join("")}</div>`;
}

function actionMiniList(actions) {
  if (!actions.length) {
    return emptyState("Henüz işlem bulunmuyor.", "Admin aksiyonları burada listelenecek.");
  }
  return `<div class="mini-list">${actions.map((action) => `<a href="/actions?search=${encodeURIComponent(action.reportId || action.quoteId || "")}" class="mini-item">
    <div><span class="badge badge-neutral">${escapeHtml(action.actionType)}</span></div>
    <strong>${escapeHtml(action.reportId || "-")}</strong>
    <span>${escapeHtml(action.actor || "LOCAL_ADMIN")} · ${formatDate(action.createdAt)}</span>
  </a>`).join("")}</div>`;
}

function dashboardView(data) {
  const { counts } = data;
  return `<section class="stat-grid">
    ${statCard("Bekleyen Raporlar", counts.pending, "İnceleme bekliyor", "/reports/PENDING", "!", "warning")}
    ${statCard("Onaylanan Raporlar", counts.approved, "Geçerli bulundu", "/reports/APPROVED", "✓", "success")}
    ${statCard("Reddedilen Raporlar", counts.rejected, "Geçersiz bulundu", "/reports/REJECTED", "×", "danger")}
    ${statCard("Gizlenen Alıntılar", counts.hiddenQuotes, "Görünümden kaldırıldı", "/actions?search=QUOTE_DELETED", "◌")}
    ${statCard("Bugünkü İşlemler", counts.todayActions, "Yerel admin aksiyonu", "/actions", "↻")}
  </section>
  <section class="dashboard-grid">
    <div class="card"><div class="section-head"><h2>Son Bekleyen Raporlar</h2><a href="/reports/PENDING">Tümünü gör</a></div>${reportMiniList(data.latestPending)}</div>
    <div class="card"><div class="section-head"><h2>Son İşlemler</h2><a href="/actions">Geçmiş</a></div>${actionMiniList(data.latestActions)}</div>
    <div class="card">
      <h2>Hızlı İşlemler</h2>
      <div class="quick-actions">
        <a class="button primary" href="/reports/PENDING">Bekleyenleri İncele</a>
        <a class="button ghost" href="/actions">İşlem Geçmişi</a>
      </div>
    </div>
    <div class="card system-status">
      <h2>Sistem Durumu</h2>
      <p><span class="status-dot success"></span> Firebase bağlantısı: Admin SDK</p>
      <p><span class="status-dot success"></span> Çalışma modu: localhost only</p>
      <p><span class="status-dot warning"></span> Cloud Functions: inactive</p>
      <p><span class="status-dot success"></span> Admin actor: LOCAL_ADMIN</p>
    </div>
  </section>`;
}

module.exports = {
  dashboardView,
};
