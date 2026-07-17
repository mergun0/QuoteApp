const express = require("express");
const {
  createSession,
  destroySession,
  sessionFromRequest,
  requireSession,
  requireCsrf,
  setSessionCookies,
  clearSessionCookies,
  verifyPassword,
  isLoginThrottled,
  recordLoginFailure,
  clearLoginFailures,
} = require("../security");
const { page, loginPage } = require("../views/layout");
const { dashboardView } = require("../views/dashboard");
const { reportsView, detailView } = require("../views/reports");
const { actionsView } = require("../views/actions");
const {
  REPORT_STATUS,
  loadDashboard,
  listReports,
  loadReportDetail,
  listActions,
  reviewReport,
  deleteReportedQuote,
} = require("../moderationService");

function safeErrorMessage() {
  return "İşlem tamamlanamadı. Lütfen sunucu loglarını kontrol edin.";
}

function createAdminRoutes({ db, FieldValue, adminPassword, actor }) {
  const router = express.Router();

  router.get("/login", (req, res) => {
    res.send(loginPage());
  });

  router.post("/login", (req, res) => {
    try {
      if (isLoginThrottled(req)) {
        res.status(429).send(loginPage("Çok fazla deneme yapıldı. Lütfen daha sonra tekrar deneyin."));
        return;
      }
      if (!verifyPassword(req.body.password, adminPassword)) {
        recordLoginFailure(req);
        res.status(401).send(loginPage("Giriş bilgileri geçersiz."));
        return;
      }
      clearLoginFailures(req);
      const session = createSession();
      setSessionCookies(res, session.sessionId, session.csrfToken);
      res.redirect("/");
    } catch (error) {
      console.error("Login failed", error);
      res.status(500).send(loginPage("Giriş yapılamadı."));
    }
  });

  router.post("/logout", (req, res) => {
    const current = sessionFromRequest(req);
    if (current) {
      destroySession(current.sessionId);
    }
    clearSessionCookies(res);
    res.redirect("/login");
  });

  router.get("/", requireSession, async (req, res, next) => {
    try {
      const data = await loadDashboard(db);
      res.send(page({
        title: "Dashboard",
        subtitle: "Moderasyon durumunu hızlıca takip et.",
        active: "dashboard",
        body: dashboardView(data),
        flash: req.query.message ? { type: "success", message: req.query.message } : null,
      }));
    } catch (error) {
      next(error);
    }
  });

  router.get("/reports/:status", requireSession, async (req, res, next) => {
    try {
      const status = String(req.params.status || "").toUpperCase();
      if (!Object.values(REPORT_STATUS).includes(status)) {
        res.status(404).send("Geçersiz rapor durumu.");
        return;
      }
      const filters = {
        search: req.query.search || "",
        reason: req.query.reason || "ALL",
        sort: req.query.sort || "desc",
        page: Number(req.query.page || 1),
      };
      const pageData = await listReports(db, { ...filters, status });
      const active = status === REPORT_STATUS.pending ? "pending" : status === REPORT_STATUS.approved ? "approved" : "rejected";
      res.send(page({
        title: `${status === "PENDING" ? "Bekleyen" : status === "APPROVED" ? "Onaylanan" : "Reddedilen"} Raporlar`,
        subtitle: "Raporları filtrele, sırala ve detayına git.",
        active,
        body: reportsView(status, pageData, filters),
        flash: req.query.message ? { type: "success", message: req.query.message } : null,
      }));
    } catch (error) {
      next(error);
    }
  });

  router.get("/report/:reportId", requireSession, async (req, res, next) => {
    try {
      const detail = await loadReportDetail(db, req.params.reportId);
      if (!detail) {
        res.status(404).send("Rapor bulunamadı.");
        return;
      }
      res.send(page({
        title: "Rapor Detayı",
        subtitle: "Alıntı, raporlayan ve inceleme bilgileri.",
        active: "pending",
        body: detailView(detail, req.adminSession.csrfToken),
        flash: req.query.message ? { type: "success", message: req.query.message } : null,
      }));
    } catch (error) {
      next(error);
    }
  });

  router.get("/actions", requireSession, async (req, res, next) => {
    try {
      const filters = {
        search: req.query.search || "",
        actionType: req.query.actionType || "ALL",
        page: Number(req.query.page || 1),
      };
      const pageData = await listActions(db, filters);
      res.send(page({
        title: "İşlem Geçmişi",
        subtitle: "Yerel admin panelinden yapılan aksiyonlar.",
        active: "actions",
        body: actionsView(pageData, filters),
      }));
    } catch (error) {
      next(error);
    }
  });

  router.post("/report/:reportId/approve", requireCsrf, async (req, res, next) => {
    try {
      await reviewReport(db, FieldValue, req.params.reportId, REPORT_STATUS.approved, actor, req.body.note);
      res.redirect(`/report/${encodeURIComponent(req.params.reportId)}?message=${encodeURIComponent("Rapor onaylandı.")}`);
    } catch (error) {
      next(error);
    }
  });

  router.post("/report/:reportId/reject", requireCsrf, async (req, res, next) => {
    try {
      await reviewReport(db, FieldValue, req.params.reportId, REPORT_STATUS.rejected, actor, req.body.note);
      res.redirect(`/report/${encodeURIComponent(req.params.reportId)}?message=${encodeURIComponent("Rapor reddedildi.")}`);
    } catch (error) {
      next(error);
    }
  });

  router.post("/report/:reportId/delete-quote", requireCsrf, async (req, res, next) => {
    try {
      await deleteReportedQuote(db, FieldValue, req.params.reportId, actor, req.body.note);
      res.redirect(`/report/${encodeURIComponent(req.params.reportId)}?message=${encodeURIComponent("Alıntı görünümden kaldırıldı.")}`);
    } catch (error) {
      next(error);
    }
  });

  router.use((error, req, res, next) => {
    console.error("Admin panel error", error);
    res.status(500).send(page({
      title: "Hata",
      subtitle: "Beklenmeyen bir sorun oluştu.",
      active: "dashboard",
      body: `<section class="card"><h2>İşlem tamamlanamadı.</h2><p class="muted">${safeErrorMessage()}</p></section>`,
      flash: { type: "error", message: safeErrorMessage() },
    }));
  });

  return router;
}

module.exports = {
  createAdminRoutes,
};
