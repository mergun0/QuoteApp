const fs = require("fs");
const path = require("path");
const { initializeApp, cert, getApps } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

function loadServiceAccount() {
  const configuredPath = process.env.GOOGLE_APPLICATION_CREDENTIALS
    || process.env.FIREBASE_SERVICE_ACCOUNT_PATH
    || path.resolve(process.cwd(), "admin-panel", "serviceAccountKey.json");

  if (!fs.existsSync(configuredPath)) {
    throw new Error(
      "Firebase service account not found. Set GOOGLE_APPLICATION_CREDENTIALS or FIREBASE_SERVICE_ACCOUNT_PATH."
    );
  }

  const raw = fs.readFileSync(configuredPath, "utf8");
  const serviceAccount = JSON.parse(raw);
  if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
    throw new Error("Firebase service account is missing project_id, client_email or private_key.");
  }
  serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, "\n");
  return serviceAccount;
}

function initializeFirebaseAdmin() {
  if (!getApps().length) {
    initializeApp({
      credential: cert(loadServiceAccount()),
    });
  }
  return {
    db: getFirestore(),
    FieldValue,
  };
}

module.exports = {
  initializeFirebaseAdmin,
};
