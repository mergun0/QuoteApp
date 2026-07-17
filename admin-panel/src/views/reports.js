const { escapeHtml } = require("../html");
const {
  STATUS_LABELS,
  formatDate,
  statusBadge,
  reasonBadge,
  emptyState,
  pagination,
  reportFilters,
} = require("./components");

function truncate(value, max = 96) {
  const text = String(value || "");
  return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}

function reportsTable(pageData) {
  if (!pageData.items.length) {
    return emptyState("Bu filtreye uygun rapor bulunmuyor.", "Arama veya filtreleri temizleyip tekrar deneyebilirsin.");
  }
  return `<div class="table-wrap"><table>
    <thead><tr>
      <th>Neden</th><th>Alıntı</th><th>Alıntı Sahibi</th><th>Raporlayan</th><th>Tarih</th><th>Durum</th><th></th>
    </tr></thead>
    <tbody>${pageData.items.map((report) => `<tr>
      <td>${reasonBadge(report.reason)}</td>
      <td><strong>${escapeHtml(truncate(report.quote?.text || report.quote?.title || report.quoteId))}</strong><small>${escapeHtml(report.quoteId || "")}</small></td>
      <td>${escapeHtml(report.owner?.username || "Bilinmeyen")}<small>${escapeHtml(report.reportedUserId || "")}</small></td>
      <td>${escapeHtml(report.reporter?.username || "Bilinmeyen")}<small>${escapeHtml(report.reporterUserId || "")}</small></td>
      <td>${formatDate(report.createdAt)}</td>
      <td>${statusBadge(report.status)}</td>
      <td><a class="button small" href="/report/${encodeURIComponent(report.id)}">Detay</a></td>
    </tr>`).join("")}</tbody>
  </table></div>`;
}

function reportsView(status, pageData, filters) {
  const title = STATUS_LABELS[status] || "Raporlar";
  return `<section class="card">
    <div class="section-head"><div><h2>${escapeHtml(title)} Raporlar</h2><p>${pageData.totalItems} kayıt bulundu.</p></div><a class="button ghost" href="/reports/${encodeURIComponent(status)}">Yenile</a></div>
    ${reportFilters(`/reports/${encodeURIComponent(status)}`, filters, false)}
  </section>
  <section class="card">
    ${reportsTable(pageData)}
    ${pagination(`/reports/${encodeURIComponent(status)}`, pageData, filters)}
  </section>`;
}

function detailView(detail, csrfToken) {
  const { report, quote, owner, reporter } = detail;
  const canHide = report.status === "APPROVED" && report.isValidReport === true;
  return `<div class="breadcrumb"><a href="/reports/${encodeURIComponent(report.status || "PENDING")}">Raporlar</a><span>/</span><strong>${escapeHtml(report.id)}</strong></div>
  <section class="detail-header card">
    <div>
      <h2>${escapeHtml(report.id)}</h2>
      <p>Oluşturulma: ${formatDate(report.createdAt)}</p>
    </div>
    ${statusBadge(report.status)}
  </section>
  <section class="detail-grid">
    <article class="card quote-card">
      <h2>Raporlanan Alıntı</h2>
      <blockquote>${escapeHtml(quote?.text || "Alıntı bulunamadı.")}</blockquote>
      <dl>
        <div><dt>Kaynak</dt><dd>${escapeHtml(quote?.title || "-")}</dd></div>
        <div><dt>Tür</dt><dd>${escapeHtml(quote?.type || "-")}</dd></div>
        <div><dt>Quote ID</dt><dd>${escapeHtml(report.quoteId || "-")}</dd></div>
        <div><dt>Sahip</dt><dd>${escapeHtml(owner?.username || "Bilinmeyen")} · ${escapeHtml(report.reportedUserId || "-")}</dd></div>
        <div><dt>Gizli mi?</dt><dd>${quote?.isHidden === true ? "Evet" : "Hayır"}</dd></div>
      </dl>
    </article>
    <article class="card">
      <h2>Rapor Bilgisi</h2>
      <p>${reasonBadge(report.reason)}</p>
      <p class="description">${escapeHtml(report.description || "Açıklama yok.")}</p>
      <dl>
        <div><dt>Raporlayan</dt><dd>${escapeHtml(reporter?.username || "Bilinmeyen")} · ${escapeHtml(report.reporterUserId || "-")}</dd></div>
        <div><dt>Tarih</dt><dd>${formatDate(report.createdAt)}</dd></div>
      </dl>
    </article>
    <article class="card">
      <h2>İnceleme Bilgisi</h2>
      <dl>
        <div><dt>İnceleyen</dt><dd>${escapeHtml(report.reviewedBy || "-")}</dd></div>
        <div><dt>İnceleme Tarihi</dt><dd>${formatDate(report.reviewedAt)}</dd></div>
        <div><dt>Geçerli Rapor</dt><dd>${report.isValidReport === true ? "Evet" : report.isValidReport === false ? "Hayır" : "-"}</dd></div>
        <div><dt>Durum</dt><dd>${statusBadge(report.status)}</dd></div>
      </dl>
    </article>
    <article class="card action-card">
      <h2>İşlem Alanı</h2>
      <form method="post" action="/report/${encodeURIComponent(report.id)}/approve" data-submit-lock>
        <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
        <textarea name="note" placeholder="Opsiyonel admin notu"></textarea>
        <button class="button success" type="submit" data-confirm="Rapor onaylansın mı?">Onayla</button>
      </form>
      <form method="post" action="/report/${encodeURIComponent(report.id)}/reject" data-submit-lock>
        <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
        <textarea name="note" placeholder="Opsiyonel admin notu"></textarea>
        <button class="button danger" type="submit" data-confirm="Rapor reddedilsin mi?">Reddet</button>
      </form>
      <form method="post" action="/report/${encodeURIComponent(report.id)}/delete-quote" data-submit-lock>
        <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
        <textarea name="note" placeholder="Gizleme notu"></textarea>
        <button class="button danger" type="submit" ${canHide ? "" : "disabled"} data-confirm="Onaylanan rapora bağlı alıntı görünümden kaldırılsın mı?">Alıntıyı Gizle</button>
        ${canHide ? "" : '<p class="muted">Alıntı yalnızca onaylanmış/geçerli rapor için gizlenebilir.</p>'}
      </form>
    </article>
  </section>`;
}

module.exports = {
  reportsView,
  detailView,
};
