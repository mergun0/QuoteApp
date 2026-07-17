const express = require("express");
const { initializeFirebaseAdmin } = require("./firebaseAdmin");
const {
  createSession,
  destroySession,
  sessionFromRequest,
  requireSession,
  requireCsrf,
  setSessionCookies,
  clearSessionCookies,
  verifyPassword,
} = require("./security");
const {
  REPORT_STATUS,
  listReports,
  loadReportDetail,
  reviewReport,
  deleteReportedQuote,
} = require("./moderationService");
const { escapeHtml } = require("./html");

const HOST = process.env.ADMIN_PANEL_HOST || "127.0.0.1";
const PORT = Number(process.env.ADMIN_PANEL_PORT || 4173);
const LOCAL_ADMIN_PASSWORD = process.env.LOCAL_ADMIN_PASSWORD || "";
const LOCAL_ADMIN_ACTOR = process.env.LOCAL_ADMIN_ACTOR || "LOCAL_ADMIN";

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

function layout(title, body) {
  return `<!doctype html>
<html lang="tr">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)} · QuoteApp Admin</title>
  <style>
    :root { color-scheme: dark; --bg:#0F1117; --surface:#171A23; --card:#1E2330; --primary:#6C63FF; --text:#FFFFFF; --muted:#B5BAC8; --error:#EF4444; --ok:#00C896; --accent:#FFB800; }
    * { box-sizing: border-box; }
    body { margin:0; font-family: Arial, sans-serif; background:var(--bg); color:var(--text); }
    a { color:var(--text); text-decoration:none; }
    .wrap { max-width: 1120px; margin: 0 auto; padding: 24px; }
    .nav { display:flex; gap:12px; align-items:center; justify-content:space-between; margin-bottom:24px; }
    .tabs { display:flex; gap:8px; flex-wrap:wrap; }
    .btn, button { border:0; border-radius:16px; padding:12px 16px; background:var(--primary); color:var(--text); font-weight:700; cursor:pointer; min-height:44px; }
    .btn.secondary { background:var(--card); color:var(--muted); }
    .btn.danger, button.danger { background:var(--error); }
    .btn.ok, button.ok { background:var(--ok); color:#07110d; }
    .card { background:var(--card); border:1px solid rgba(255,255,255,.06); border-radius:20px; padding:20px; margin-bottom:16px; }
    .muted { color:var(--muted); }
    .grid { display:grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap:16px; }
    input, textarea { width:100%; border-radius:16px; border:1px solid rgba(255,255,255,.08); background:var(--surface); color:var(--text); padding:12px; min-height:44px; }
    textarea { min-height:96px; }
    .row { display:flex; gap:12px; align-items:center; justify-content:space-between; flex-wrap:wrap; }
    .pill { display:inline-block; border-radius:999px; padding:6px 10px; background:var(--surface); color:var(--muted); font-size:12px; }
    .danger-text { color:var(--error); }
    pre { white-space:pre-wrap; word-break:break-word; color:var(--muted); }
  </style>
</head>
<body>
  <main class="wrap">${body}</main>
</body>
</html>`;
}

function nav() {
  return `<div class="nav">
    <div>
      <h1>QuoteApp Local Admin</h1>
      <p class="muted">Sadece localhost üzerinde geçici v1.0 moderasyon paneli.</p>
    </div>
    <div class="tabs">
      <a class="btn secondary" href="/">Dashboard</a>
      <a class="btn secondary" href="/reports/PENDING">Bekleyen</a>
      <a class="btn secondary" href="/reports/APPROVED">Onaylanan</a>
      <a class="btn secondary" href="/reports/REJECTED">Reddedilen</a>
      <form method="post" action="/logout"><button class="danger" type="submit">Çıkış</button></form>
    </div>
  </div>`;
}

function loginPage(error = "") {
  return layout("Giriş", `<div class="card" style="max-width:480px;margin:64px auto;">
    <h1>Local Admin Girişi</h1>
    <p class="muted">Bu panel yalnızca 127.0.0.1 üzerinde çalışacak şekilde tasarlanmıştır.</p>
    ${error ? `<p class="danger-text">${escapeHtml(error)}</p>` : ""}
    <form method="post" action="/login">
      <label>Admin şifresi</label>
      <input name="password" type="password" autocomplete="current-password" required>
      <div style="height:16px"></div>
      <button type="submit">Giriş Yap</button>
    </form>
  </div>`);
}

function reportListPage(status, reports) {
  const items = reports.map((report) => `<a class="card" href="/report/${encodeURIComponent(report.id)}">
    <div class="row">
      <strong>${escapeHtml(report.reason)}</strong>
      <span class="pill">${escapeHtml(report.status)}</span>
    </div>
    <p class="muted">${escapeHtml(report.quoteId)} · ${escapeHtml(report.reportedUserId)}</p>
    <p>${escapeHtml(report.description || "Açıklama yok")}</p>
    <small class="muted">${formatDate(report.createdAt)}</small>
  </a>`).join("");
  return layout(`${status} raporlar`, `${nav()}
    <div class="card"><h2>${escapeHtml(status)} raporlar</h2><p class="muted">Son 100 kayıt gösterilir.</p></div>
    ${items || '<div class="card"><p class="muted">Bu durumda rapor yok.</p></div>'}`);
}

function detailPage(detail, csrfToken, message = "") {
  const { report, quote, owner, reporter } = detail;
  const canDelete = report.status === REPORT_STATUS.approved && report.isValidReport === true;
  return layout("Rapor Detayı", `${nav()}
    ${message ? `<div class="card"><strong>${escapeHtml(message)}</strong></div>` : ""}
    <div class="grid">
      <section class="card">
        <h2>Rapor</h2>
        <p><span class="pill">${escapeHtml(report.status)}</span></p>
        <p><strong>Neden:</strong> ${escapeHtml(report.reason)}</p>
        <p><strong>Açıklama:</strong></p><pre>${escapeHtml(report.description || "Yok")}</pre>
        <p class="muted">Oluşturulma: ${formatDate(report.createdAt)}</p>
      </section>
      <section class="card">
        <h2>Alıntı</h2>
        <p><strong>${escapeHtml(quote && quote.title || "Başlık yok")}</strong></p>
        <pre>${escapeHtml(quote && quote.text || "Alıntı bulunamadı")}</pre>
        <p class="muted">Quote ID: ${escapeHtml(report.quoteId)}</p>
      </section>
      <section class="card">
        <h2>Hedef Kullanıcı</h2>
        <p>${escapeHtml(owner && owner.username || "Bilinmeyen kullanıcı")}</p>
        <p class="muted">${escapeHtml(report.reportedUserId)}</p>
      </section>
      <section class="card">
        <h2>Raporlayan</h2>
        <p>${escapeHtml(reporter && reporter.username || "Bilinmeyen kullanıcı")}</p>
        <p class="muted">${escapeHtml(report.reporterUserId)}</p>
      </section>
    </div>
    <section class="card">
      <h2>İşlemler</h2>
      <form method="post" action="/report/${encodeURIComponent(report.id)}/approve">
        <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
        <textarea name="note" placeholder="Opsiyonel not"></textarea>
        <button class="ok" type="submit">Onayla</button>
      </form>
      <hr>
      <form method="post" action="/report/${encodeURIComponent(report.id)}/reject">
        <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
        <textarea name="note" placeholder="Opsiyonel not"></textarea>
        <button class="danger" type="submit">Reddet</button>
      </form>
      <hr>
      <form method="post" action="/report/${encodeURIComponent(report.id)}/delete-quote" onsubmit="return confirm('Onaylanan rapora bağlı alıntı gizlenecek. Devam edilsin mi?');">
        <input type="hidden" name="_csrf" value="${escapeHtml(csrfToken)}">
        <textarea name="note" placeholder="Silme/gizleme notu"></textarea>
        <button class="danger" type="submit" ${canDelete ? "" : "disabled"}>Onaylı Rapor İçin Alıntıyı Gizle</button>
      </form>
    </section>`);
}

function createApp() {
  const { db, FieldValue } = initializeFirebaseAdmin();
  const app = express();
  app.disable("x-powered-by");
  app.use(express.urlencoded({ extended: false }));

  app.get("/login", (req, res) => {
    res.send(loginPage());
  });

  app.post("/login", (req, res) => {
    try {
      if (!verifyPassword(req.body.password, LOCAL_ADMIN_PASSWORD)) {
        res.status(401).send(loginPage("Şifre geçersiz."));
        return;
      }
      const session = createSession();
      setSessionCookies(res, session.sessionId, session.csrfToken);
      res.redirect("/");
    } catch (error) {
      res.status(500).send(loginPage(error.message));
    }
  });

  app.post("/logout", (req, res) => {
    const current = sessionFromRequest(req);
    if (current) {
      destroySession(current.sessionId);
    }
    clearSessionCookies(res);
    res.redirect("/login");
  });

  app.get("/", requireSession, async (req, res, next) => {
    try {
      const [pending, approved, rejected] = await Promise.all([
        listReports(db, REPORT_STATUS.pending),
        listReports(db, REPORT_STATUS.approved),
        listReports(db, REPORT_STATUS.rejected),
      ]);
      res.send(layout("Dashboard", `${nav()}<div class="grid">
        <a class="card" href="/reports/PENDING"><h2>${pending.length}</h2><p>Bekleyen rapor</p></a>
        <a class="card" href="/reports/APPROVED"><h2>${approved.length}</h2><p>Onaylanan rapor</p></a>
        <a class="card" href="/reports/REJECTED"><h2>${rejected.length}</h2><p>Reddedilen rapor</p></a>
      </div>`));
    } catch (error) {
      next(error);
    }
  });

  app.get("/reports/:status", requireSession, async (req, res, next) => {
    try {
      const status = String(req.params.status || "").toUpperCase();
      if (!Object.values(REPORT_STATUS).includes(status)) {
        res.status(404).send("Geçersiz rapor durumu.");
        return;
      }
      res.send(reportListPage(status, await listReports(db, status)));
    } catch (error) {
      next(error);
    }
  });

  app.get("/report/:reportId", requireSession, async (req, res, next) => {
    try {
      const detail = await loadReportDetail(db, req.params.reportId);
      if (!detail) {
        res.status(404).send("Rapor bulunamadı.");
        return;
      }
      res.send(detailPage(detail, req.adminSession.csrfToken));
    } catch (error) {
      next(error);
    }
  });

  app.post("/report/:reportId/approve", requireCsrf, async (req, res, next) => {
    try {
      await reviewReport(db, FieldValue, req.params.reportId, REPORT_STATUS.approved, LOCAL_ADMIN_ACTOR, req.body.note);
      res.redirect(`/report/${encodeURIComponent(req.params.reportId)}`);
    } catch (error) {
      next(error);
    }
  });

  app.post("/report/:reportId/reject", requireCsrf, async (req, res, next) => {
    try {
      await reviewReport(db, FieldValue, req.params.reportId, REPORT_STATUS.rejected, LOCAL_ADMIN_ACTOR, req.body.note);
      res.redirect(`/report/${encodeURIComponent(req.params.reportId)}`);
    } catch (error) {
      next(error);
    }
  });

  app.post("/report/:reportId/delete-quote", requireCsrf, async (req, res, next) => {
    try {
      await deleteReportedQuote(db, FieldValue, req.params.reportId, LOCAL_ADMIN_ACTOR, req.body.note);
      res.redirect(`/report/${encodeURIComponent(req.params.reportId)}`);
    } catch (error) {
      next(error);
    }
  });

  app.use((error, req, res, next) => {
    res.status(500).send(layout("Hata", `${nav()}<div class="card"><h2>İşlem tamamlanamadı</h2><p class="muted">${escapeHtml(error.message)}</p></div>`));
  });

  return app;
}

if (require.main === module) {
  const app = createApp();
  app.listen(PORT, HOST, () => {
    console.log(`QuoteApp admin panel running at http://${HOST}:${PORT}`);
  });
}

module.exports = {
  createApp,
};
