const { escapeHtml } = require("../html");
const { formatDate, badge, emptyState, pagination } = require("./components");
const { PHASES } = require("../accountDeletionService");

function deletionStatusBadge(status) {
  const type = status === "COMPLETED" ? "success" : status === "FAILED" ? "danger" : status === "PROCESSING" ? "warning" : "neutral";
  return badge(status || "-", type);
}

function accountDeletionListView(status, pageData) {
  const tabs = ["PENDING", "COMPLETED", "FAILED"].map((item) =>
    `<a class="button ${status === item ? "primary" : "ghost"}" href="/account-deletions/${item}">${escapeHtml(item)}</a>`).join("");
  if (!pageData.items.length) {
    return `<section class="card"><div class="toolbar-actions">${tabs}</div></section>${emptyState("Talep yok", "Bu durumda hesap silme talebi bulunmuyor.")}`;
  }
  const rows = pageData.items.map((request) => `<tr>
    <td><a href="/account-deletion/${encodeURIComponent(request.id)}">${escapeHtml(request.username || request.id)}</a></td>
    <td><code>${escapeHtml(request.userId || request.id)}</code></td>
    <td>${formatDate(request.requestedAt)}</td>
    <td>${deletionStatusBadge(request.status)}</td>
    <td>${escapeHtml(request.currentPhase || "-")}</td>
  </tr>`).join("");
  return `<section class="card">
    <div class="toolbar-actions">${tabs}</div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Kullanıcı</th><th>UID</th><th>Talep zamanı</th><th>Durum</th><th>Faz</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    </div>
    ${pagination(`/account-deletions/${encodeURIComponent(status)}`, pageData)}
  </section>`;
}

function accountDeletionDetailView(detail, csrfToken) {
  const { request, user, counts, actions } = detail;
  const executable = request.status === "PENDING" || request.status === "FAILED";
  const progress = PHASES.map((phase) => {
    const done = Array.isArray(request.completedPhases) && request.completedPhases.includes(phase);
    return `<li>${done ? "✓" : "·"} ${escapeHtml(phase)}</li>`;
  }).join("");
  const actionRows = actions.length
    ? actions.map((action) => `<tr>
        <td>${formatDate(action.createdAt)}</td>
        <td>${escapeHtml(action.phase || "-")}</td>
        <td>${escapeHtml(action.status || "-")}</td>
        <td>${escapeHtml(action.note || "")}</td>
      </tr>`).join("")
    : `<tr><td colspan="4">Henüz audit kaydı yok.</td></tr>`;
  return `<section class="grid two">
    <article class="card">
      <h2>Talep Detayı</h2>
      <dl class="detail-list">
        <dt>Kullanıcı adı</dt><dd>${escapeHtml(request.username || "Kullanıcı")}</dd>
        <dt>UID</dt><dd><code>${escapeHtml(request.userId || request.id)}</code></dd>
        <dt>Durum</dt><dd>${deletionStatusBadge(request.status)}</dd>
        <dt>Talep zamanı</dt><dd>${formatDate(request.requestedAt)}</dd>
        <dt>Neden</dt><dd>${escapeHtml(request.reason || "-")}</dd>
        <dt>Profil gizli</dt><dd>${request.profileHidden === true ? "Evet" : "Hayır"}</dd>
      </dl>
    </article>
    <article class="card">
      <h2>Veri Matrisi</h2>
      <dl class="detail-list">
        <dt>Profil</dt><dd>${user ? "Var" : "Yok"}</dd>
        <dt>Alıntılar</dt><dd>${counts.quotes}</dd>
        <dt>Beğeniler</dt><dd>${counts.likes}</dd>
        <dt>Favoriler</dt><dd>${counts.favorites}</dd>
        <dt>Başarımlar</dt><dd>${counts.achievements}</dd>
        <dt>İstatistik</dt><dd>${counts.stats}</dd>
        <dt>Moderasyon referansı</dt><dd>${counts.moderationReferences}</dd>
      </dl>
    </article>
  </section>
  <section class="card">
    <h2>İlerleme</h2>
    <ul class="phase-list">${progress}</ul>
  </section>
  <section class="card">
    <h2>Silme İşlemi</h2>
    <p class="muted">Firebase Auth kullanıcısı en son silinir. Bu form yalnızca localhost admin oturumunda POST + CSRF ile çalışır.</p>
    ${executable ? `<form method="post" action="/account-deletion/${encodeURIComponent(request.id)}/execute" data-submit-lock>
      <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
      <label class="field">
        <span>Onay için kullanıcı UID değerini yaz</span>
        <input name="confirmation" autocomplete="off" required>
      </label>
      <button class="button danger" type="submit" data-confirm="Bu hesabın silme işlemini başlatmak istediğine emin misin?">${request.status === "FAILED" ? "Tekrar Dene" : "Silme İşlemini Başlat"}</button>
    </form>` : `<p class="muted">Bu talep mevcut durumda çalıştırılamaz.</p>`}
  </section>
  <section class="card">
    <h2>Audit Log</h2>
    <div class="table-wrap">
      <table>
        <thead><tr><th>Zaman</th><th>Faz</th><th>Durum</th><th>Not</th></tr></thead>
        <tbody>${actionRows}</tbody>
      </table>
    </div>
  </section>`;
}

module.exports = {
  accountDeletionListView,
  accountDeletionDetailView,
};
