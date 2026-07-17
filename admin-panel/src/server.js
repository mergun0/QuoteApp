const path = require("path");
const express = require("express");
const { initializeFirebaseAdmin } = require("./firebaseAdmin");
const { securityHeaders } = require("./middleware/securityHeaders");
const { createAdminRoutes } = require("./routes/adminRoutes");

const HOST = process.env.ADMIN_PANEL_HOST || "127.0.0.1";
const PORT = Number(process.env.ADMIN_PANEL_PORT || 4173);
const LOCAL_ADMIN_PASSWORD = process.env.LOCAL_ADMIN_PASSWORD || "";
const LOCAL_ADMIN_ACTOR = process.env.LOCAL_ADMIN_ACTOR || "LOCAL_ADMIN";

function createApp(dependencies = null) {
  const firebase = dependencies || initializeFirebaseAdmin();
  const app = express();
  app.disable("x-powered-by");
  app.use(securityHeaders);
  app.use(express.urlencoded({ extended: false }));
  app.use(express.static(path.resolve(__dirname, "..", "public"), {
    etag: true,
    maxAge: "1h",
  }));
  app.use(createAdminRoutes({
    db: firebase.db,
    FieldValue: firebase.FieldValue,
    adminPassword: LOCAL_ADMIN_PASSWORD,
    actor: LOCAL_ADMIN_ACTOR,
  }));
  return app;
}

if (require.main === module) {
  const app = createApp();
  app.listen(PORT, HOST, () => {
    console.log(`Satır Arası admin panel running at http://${HOST}:${PORT}`);
  });
}

module.exports = {
  createApp,
};
