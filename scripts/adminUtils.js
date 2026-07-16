const { initializeApp, cert, getApps } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");
const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const serviceAccountPath = path.resolve(rootDir, "serviceAccountKey.json");

function readServiceAccount() {
  if (!fs.existsSync(serviceAccountPath)) {
    throw new Error("serviceAccountKey.json not found in project root.");
  }

  let serviceAccount;
  try {
    serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
  } catch (error) {
    throw new Error(`serviceAccountKey.json could not be parsed: ${error.message}`);
  }

  const requiredFields = ["private_key", "client_email", "project_id"];
  const missingFields = requiredFields.filter((field) => !serviceAccount[field]);
  if (missingFields.length > 0) {
    throw new Error(
      `serviceAccountKey.json is missing required field(s): ${missingFields.join(", ")}.`
    );
  }

  serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, "\n");
  return serviceAccount;
}

function initializeAdmin() {
  if (!getApps().length) {
    initializeApp({
      credential: cert(readServiceAccount()),
    });
  }

  return {
    auth: getAuth(),
    db: getFirestore(),
  };
}

function hasApplyFlag() {
  return process.argv.includes("--apply");
}

function chunk(items, size) {
  const chunks = [];
  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }
  return chunks;
}

module.exports = {
  initializeAdmin,
  hasApplyFlag,
  chunk,
};
