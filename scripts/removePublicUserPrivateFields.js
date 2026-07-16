const { FieldValue } = require("firebase-admin/firestore");
const { initializeAdmin, hasApplyFlag, chunk } = require("./adminUtils");

const PRIVATE_FIELDS = [
  "email",
  "password",
  "authToken",
  "authTokens",
  "deviceToken",
  "deviceTokens",
  "fcmToken",
  "resetToken",
  "resetCode",
  "isAdmin",
  "isModerator",
  "admin",
  "moderator",
  "moderationStatus",
];

async function main() {
  const apply = hasApplyFlag();
  const { db } = initializeAdmin();
  const usersSnapshot = await db.collection("users").get();
  const updates = [];

  usersSnapshot.forEach((document) => {
    const data = document.data();
    const presentFields = PRIVATE_FIELDS.filter((field) => Object.prototype.hasOwnProperty.call(data, field));
    if (presentFields.length === 0) {
      return;
    }

    updates.push({
      ref: document.ref,
      fields: presentFields,
    });
  });

  if (apply) {
    for (const group of chunk(updates, 450)) {
      const batch = db.batch();
      group.forEach((update) => {
        const deletePayload = {};
        update.fields.forEach((field) => {
          deletePayload[field] = FieldValue.delete();
        });
        batch.set(update.ref, deletePayload, { merge: true });
      });
      await batch.commit();
    }
  }

  console.log(`Mode: ${apply ? "APPLY" : "DRY_RUN"}`);
  console.log(`Users scanned: ${usersSnapshot.size}`);
  console.log(`Users with private public-profile fields: ${updates.length}`);
  console.log(`Fields checked: ${PRIVATE_FIELDS.join(", ")}`);
  if (!apply) {
    updates.slice(0, 20).forEach((update) => {
      console.log(`Would clean users/${update.ref.id}: ${update.fields.join(", ")}`);
    });
    if (updates.length > 20) {
      console.log(`...and ${updates.length - 20} more user document(s).`);
    }
    console.log("No writes were made. Re-run with --apply after reviewing the dry-run output.");
  }
}

main().catch((error) => {
  console.error(`removePublicUserPrivateFields failed: ${error.message}`);
  process.exit(1);
});
