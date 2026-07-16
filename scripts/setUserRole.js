const { initializeAdmin } = require("./adminUtils");

const VALID_ROLES = new Set(["user", "moderator", "admin"]);

async function main() {
  const uid = process.argv[2];
  const role = process.argv[3];

  if (!uid || !role || !VALID_ROLES.has(role)) {
    throw new Error("Usage: node scripts/setUserRole.js <uid> <user|moderator|admin>");
  }

  const { auth, db } = initializeAdmin();
  const user = await auth.getUser(uid);
  const existingClaims = user.customClaims || {};
  const nextClaims = { ...existingClaims, role };

  await auth.setCustomUserClaims(uid, nextClaims);
  await db.collection("users").doc(uid).set({ role }, { merge: true });

  console.log("User role updated.");
  console.log(`uid: ${uid}`);
  console.log(`role: ${role}`);
}

main().catch((error) => {
  console.error(`setUserRole failed: ${error.message}`);
  process.exit(1);
});
