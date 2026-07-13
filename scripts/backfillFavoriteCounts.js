const { initializeApp, cert, getApps } = require("firebase-admin/app");
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

function initializeFirebase() {
  if (!getApps().length) {
    initializeApp({
      credential: cert(readServiceAccount()),
    });
  }
}

async function main() {
  initializeFirebase();
  const db = getFirestore();

  const favoritesSnapshot = await db.collection("favorites").get();
  const countsByQuoteId = new Map();

  favoritesSnapshot.forEach((document) => {
    const quoteId = document.get("quoteId");
    if (!quoteId || typeof quoteId !== "string") {
      return;
    }
    countsByQuoteId.set(quoteId, (countsByQuoteId.get(quoteId) || 0) + 1);
  });

  const quotesSnapshot = await db.collection("quotes").get();
  const quoteIds = new Set(quotesSnapshot.docs.map((document) => document.id));
  const missingQuoteIds = [...countsByQuoteId.keys()].filter((quoteId) => !quoteIds.has(quoteId));
  let updatedQuotes = 0;
  const batchSize = 450;
  let batch = db.batch();
  let pendingWrites = 0;

  async function commitIfNeeded(force = false) {
    if (pendingWrites === 0 || (!force && pendingWrites < batchSize)) {
      return;
    }
    await batch.commit();
    batch = db.batch();
    pendingWrites = 0;
  }

  for (const quoteDocument of quotesSnapshot.docs) {
    const count = countsByQuoteId.get(quoteDocument.id) || 0;
    batch.set(quoteDocument.ref, { favoriteCount: count }, { merge: true });
    updatedQuotes += 1;
    pendingWrites += 1;
    await commitIfNeeded();
  }

  await commitIfNeeded(true);

  console.log("Favorite count backfill completed.");
  console.log(`Favorites scanned: ${favoritesSnapshot.size}`);
  console.log(`Quotes updated: ${updatedQuotes}`);
  console.log(`Missing quote documents: ${missingQuoteIds.length}`);
  if (missingQuoteIds.length > 0) {
    console.log(`Missing quote IDs: ${missingQuoteIds.join(", ")}`);
  }
  console.log("Final status: completed");
}

main().catch((error) => {
  console.error(`Backfill failed: ${error.message}`);
  process.exit(1);
});
