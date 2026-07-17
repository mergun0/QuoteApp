const { escapeHtml } = require("../html");

const REASON_LABELS = {
  SPAM: "Spam",
  HARASSMENT: "Nefret/Taciz",
  INAPPROPRIATE: "Uygunsuz",
  WRONG_CATEGORY: "Yanlış Kategori",
  COPYRIGHT: "Telif",
  OTHER: "Diğer",
};

const STATUS_LABELS = {
  PENDING: "Bekliyor",
  APPROVED: "Onaylandı",
  REJECTED: "Reddedildi",
};

function formatDate(value) {
  if (!value) {
    return "-";
  }
  if (typeof value.toDate === "function") {
    return value.toDate().toLocaleString("tr-TR");
  }
  if (value instanceof Date) {
    return value.toLocaleString("tr-TR");
  }
  return escapeHtml(value);
}

function badge(value, type = "neutral") {
  return `<span class="badge badge-${escapeHtml(type)}">${escapeHtml(value || "-")}</span>`;
}

function statusBadge(status) {
  const type = status === "APPROVED" ? "success" : status === "REJECTED" ? "danger" : "warning";
  return badge(STATUS_LABELS[status] || status, type);
}

function reasonBadge(reason) {
  return badge(REASON_LABELS[reason] || reason, "reason");
}

function alert(message, type = "info") {
  if (!message) {
    return "";
  }
  return `<div class="alert alert-${escapeHtml(type)}">${escapeHtml(message)}</div>`;
}

function emptyState(title, body) {
  return `<section class="empty-state">
    <div class="empty-icon">∅</div>
    <h3>${escapeHtml(title)}</h3>
    <p>${escapeHtml(body)}</p>
  </section>`;
}

function pagination(basePath, pageData, query = {}) {
  if (!pageData || pageData.totalPages <= 1) {
    return "";
  }
  const makeUrl = (page) => {
    const params = new URLSearchParams({ ...query, page: String(page) });
    return `${basePath}?${params.toString()}`;
  };
  return `<nav class="pagination" aria-label="Sayfalama">
    <a class="button ghost ${pageData.hasPrevious ? "" : "disabled"}" href="${pageData.hasPrevious ? makeUrl(pageData.page - 1) : "#"}">Önceki</a>
    <span>${pageData.page} / ${pageData.totalPages}</span>
    <a class="button ghost ${pageData.hasNext ? "" : "disabled"}" href="${pageData.hasNext ? makeUrl(pageData.page + 1) : "#"}">Sonraki</a>
  </nav>`;
}

function reportFilters(action, filters = {}, includeStatus = false) {
  const reasonOptions = ["ALL", "SPAM", "HARASSMENT", "INAPPROPRIATE", "WRONG_CATEGORY", "COPYRIGHT", "OTHER"]
    .map((reason) => `<option value="${reason}" ${filters.reason === reason ? "selected" : ""}>${reason === "ALL" ? "Tüm nedenler" : escapeHtml(REASON_LABELS[reason])}</option>`)
    .join("");
  const statusOptions = ["ALL", "PENDING", "APPROVED", "REJECTED"]
    .map((status) => `<option value="${status}" ${filters.statusFilter === status ? "selected" : ""}>${status === "ALL" ? "Tüm durumlar" : escapeHtml(STATUS_LABELS[status])}</option>`)
    .join("");
  return `<form class="filters" method="get" action="${escapeHtml(action)}">
    <label class="field">
      <span>Arama</span>
      <input type="search" name="search" value="${escapeHtml(filters.search || "")}" placeholder="Alıntı, kullanıcı veya UID ara">
    </label>
    <label class="field">
      <span>Neden</span>
      <select name="reason">${reasonOptions}</select>
    </label>
    ${includeStatus ? `<label class="field"><span>Durum</span><select name="statusFilter">${statusOptions}</select></label>` : ""}
    <label class="field">
      <span>Sıralama</span>
      <select name="sort">
        <option value="desc" ${filters.sort !== "asc" ? "selected" : ""}>Yeni → Eski</option>
        <option value="asc" ${filters.sort === "asc" ? "selected" : ""}>Eski → Yeni</option>
      </select>
    </label>
    <button class="button primary" type="submit">Filtrele</button>
    <a class="button ghost" href="${escapeHtml(action)}">Temizle</a>
  </form>`;
}

function actionFilters(action, filters = {}) {
  const actionTypes = ["ALL", "REPORT_APPROVED", "REPORT_REJECTED", "QUOTE_DELETED"]
    .map((type) => `<option value="${type}" ${filters.actionType === type ? "selected" : ""}>${type === "ALL" ? "Tüm işlemler" : escapeHtml(type)}</option>`)
    .join("");
  return `<form class="filters" method="get" action="${escapeHtml(action)}">
    <label class="field">
      <span>Arama</span>
      <input type="search" name="search" value="${escapeHtml(filters.search || "")}" placeholder="Rapor, alıntı, kullanıcı veya not ara">
    </label>
    <label class="field">
      <span>İşlem tipi</span>
      <select name="actionType">${actionTypes}</select>
    </label>
    <button class="button primary" type="submit">Filtrele</button>
    <a class="button ghost" href="${escapeHtml(action)}">Temizle</a>
  </form>`;
}

module.exports = {
  REASON_LABELS,
  STATUS_LABELS,
  formatDate,
  badge,
  statusBadge,
  reasonBadge,
  alert,
  emptyState,
  pagination,
  reportFilters,
  actionFilters,
};
