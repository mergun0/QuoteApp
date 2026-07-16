const { initializeAdmin, hasApplyFlag, chunk } = require("./adminUtils");

function normalizeUsername(value) {
  return typeof value === "string" ? value.trim().toLowerCase() : "";
}

async function main() {
  const apply = hasApplyFlag();
  const { db } = initializeAdmin();
  const usersSnapshot = await db.collection("users").get();
  const candidatesByUsername = new Map();
  const totals = {
    scanned: usersSnapshot.size,
    missingUsername: 0,
    collisions: 0,
    existingReservations: 0,
    conflictingReservations: 0,
    usernameDocsToWrite: 0,
    usersSkipped: 0,
  };

  for (const document of usersSnapshot.docs) {
    const userData = document.data();
    const normalizedUsername = normalizeUsername(userData.usernameLowercase || userData.username);
    if (!normalizedUsername) {
      totals.missingUsername += 1;
      totals.usersSkipped += 1;
      continue;
    }

    const existing = candidatesByUsername.get(normalizedUsername);
    if (existing && existing.uid !== document.id) {
      existing.collision = true;
      totals.collisions += 1;
      totals.usersSkipped += 1;
      continue;
    }

    candidatesByUsername.set(normalizedUsername, {
      uid: document.id,
      normalizedUsername,
      createdAt: userData.createdAt || null,
      collision: false,
    });
  }

  const operations = [];
  for (const candidate of candidatesByUsername.values()) {
    if (candidate.collision) {
      continue;
    }

    const usernameRef = db.collection("usernames").doc(candidate.normalizedUsername);
    const usernameDoc = await usernameRef.get();

    if (usernameDoc.exists && usernameDoc.get("uid") && usernameDoc.get("uid") !== candidate.uid) {
      totals.conflictingReservations += 1;
      totals.usersSkipped += 1;
      continue;
    }

    if (!usernameDoc.exists) {
      totals.usernameDocsToWrite += 1;
      operations.push({
        type: "set",
        ref: usernameRef,
        data: {
          uid: candidate.uid,
          createdAt: candidate.createdAt || new Date(),
        },
      });
    } else {
      totals.existingReservations += 1;
    }
  }

  if (apply) {
    for (const group of chunk(operations, 450)) {
      const batch = db.batch();
      group.forEach((operation) => batch.set(operation.ref, operation.data, { merge: true }));
      await batch.commit();
    }
  }

  console.log(`Mode: ${apply ? "APPLY" : "DRY_RUN"}`);
  console.log(`Users scanned: ${totals.scanned}`);
  console.log(`Users missing username: ${totals.missingUsername}`);
  console.log(`Username collisions: ${totals.collisions}`);
  console.log(`Existing reservations preserved: ${totals.existingReservations}`);
  console.log(`Conflicting reservations skipped: ${totals.conflictingReservations}`);
  console.log(`Username documents to write: ${totals.usernameDocsToWrite}`);
  console.log(`Users skipped: ${totals.usersSkipped}`);
  if (!apply) {
    console.log("No writes were made. Re-run with --apply after reviewing the dry-run output.");
  }
}

main().catch((error) => {
  console.error(`backfillUsernameReservations failed: ${error.message}`);
  process.exit(1);
});
