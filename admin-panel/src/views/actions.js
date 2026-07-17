const { escapeHtml } = require("../html");
const { formatDate, emptyState, pagination, actionFilters } = require("./components");

function actionsView(pageData, filters) {
  const rows = pageData.items.map((action) => `<tr>
    <td><span class="badge badge-neutral">${escapeHtml(action.actionType || "-")}</span></td>
    <td>${escapeHtml(action.reportId || "-")}</td>
    <td>${escapeHtml(action.quoteId || "-")}</td>
    <td>${escapeHtml(action.targetUserId || "-")}</td>
    <td>${escapeHtml(action.previousStatus || "-")}</td>
    <td>${escapeHtml(action.newStatus || "-")}</td>
    <td>${escapeHtml(action.actor || "LOCAL_ADMIN")}</td>
    <td>${formatDate(action.createdAt)}</td>
    <td>${escapeHtml(action.note || "-")}</td>
  </tr>`).join("");

  return `<section class="card">
    <div class="section-head"><div><h2>İşlem Geçmişi</h2><p>${pageData.totalItems} işlem bulundu.</p></div><a class="button ghost" href="/actions">Yenile</a></div>
    ${actionFilters("/actions", filters)}
  </section>
  <section class="card">
    ${rows ? `<div class="table-wrap"><table>
      <thead><tr><th>İşlem</th><th>Rapor ID</th><th>Quote ID</th><th>Hedef</th><th>Önceki</th><th>Yeni</th><th>Actor</th><th>Tarih</th><th>Not</th></tr></thead>
      <tbody>${rows}</tbody>
    </table></div>` : emptyState("Henüz işlem geçmişi yok.", "Admin aksiyonları tamamlandıkça burada görünecek.")}
    ${pagination("/actions", pageData, filters)}
  </section>`;
}

module.exports = {
  actionsView,
};
