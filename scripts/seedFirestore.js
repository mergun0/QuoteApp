const { initializeApp, cert, getApps } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const seedPath = path.resolve(rootDir, "firebase_seed_levels_achievements.json");
const serviceAccountPath = path.resolve(rootDir, "serviceAccountKey.json");

function readJsonFile(filePath, missingMessage, parseMessage) {
  if (!fs.existsSync(filePath)) {
    throw new Error(missingMessage);
  }

  try {
    return JSON.parse(fs.readFileSync(filePath, "utf8"));
  } catch (error) {
    throw new Error(`${parseMessage} ${error.message}`);
  }
}

function loadSeedData() {
  const seedData = readJsonFile(
    seedPath,
    "firebase_seed_levels_achievements.json not found in project root.",
    "firebase_seed_levels_achievements.json could not be parsed:"
  );

  if (!seedData.levels || typeof seedData.levels !== "object") {
    throw new Error("Seed JSON is missing a valid 'levels' object.");
  }
  if (!seedData.achievements || typeof seedData.achievements !== "object") {
    throw new Error("Seed JSON is missing a valid 'achievements' object.");
  }

  return seedData;
}

function loadServiceAccount() {
  const serviceAccount = readJsonFile(
    serviceAccountPath,
    "serviceAccountKey.json not found in project root.",
    "serviceAccountKey.json could not be parsed:"
  );

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
    const serviceAccount = loadServiceAccount();
    initializeApp({
      credential: cert(serviceAccount),
    });
  }
}

async function writeCollection(db, collectionName, documents) {
  const entries = Object.entries(documents);

  for (const [documentId, data] of entries) {
    await db.collection(collectionName).doc(documentId).set(data, { merge: true });
  }

  return entries.length;
}

async function main() {
  const seedData = loadSeedData();
  initializeFirebase();
  const db = getFirestore();

  const levelCount = await writeCollection(db, "levels", seedData.levels);
  const achievementCount = await writeCollection(
    db,
    "achievements",
    seedData.achievements
  );

  console.log("Seed completed.");
  console.log(`Levels written: ${levelCount}`);
  console.log(`Achievements written: ${achievementCount}`);
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(`Seed failed: ${error.message}`);
    process.exit(1);
  });
