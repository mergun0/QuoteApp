const { initializeAdmin, hasApplyFlag, chunk } = require("./adminUtils");

function normalizeUsername(value) {
  return typeof value === "string" ? value.trim().toLowerCase() : "";
}

async function resolveEmail(auth, userId, userData) {
  if (typeof userData.email === "string" && userData.email.trim()) {
    return userData.email.trim();
  }

  try {
    const authUser = await auth.getUser(userId);
    return authUser.email || "";
  } catch (error) {
    return "";
  }
}

async function main() {
  const apply = hasApplyFlag();
  const { auth, db } = initializeAdmin();
  const usersSnapshot = await db.collection("users").get();
  const candidatesByUsername = new Map();
  const totals = {
    scanned: usersSnapshot.size,
    missingUsername: 0,
    missingEmail: 0,
    collisions: 0,
    existingReservations: 0,
    conflictingReservations: 0,
    usernameDocsToWrite: 0,
    usernameLoginDocsToWrite: 0,
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

    const email = await resolveEmail(auth, document.id, userData);
    if (!email) {
      totals.missingEmail += 1;
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
      email,
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
    const loginRef = db.collection("usernameLogins").doc(candidate.normalizedUsername);
    const [usernameDoc, loginDoc] = await Promise.all([usernameRef.get(), loginRef.get()]);

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

    if (!loginDoc.exists || loginDoc.get("uid") !== candidate.uid || loginDoc.get("email") !== candidate.email) {
      totals.usernameLoginDocsToWrite += 1;
      operations.push({
        type: "set",
        ref: loginRef,
        data: {
          uid: candidate.uid,
          email: candidate.email,
          createdAt: candidate.createdAt || new Date(),
        },
      });
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
  console.log(`Users missing email/Auth email: ${totals.missingEmail}`);
  console.log(`Username collisions: ${totals.collisions}`);
  console.log(`Existing reservations preserved: ${totals.existingReservations}`);
  console.log(`Conflicting reservations skipped: ${totals.conflictingReservations}`);
  console.log(`Username documents to write: ${totals.usernameDocsToWrite}`);
  console.log(`Username login documents to write: ${totals.usernameLoginDocsToWrite}`);
  console.log(`Users skipped: ${totals.usersSkipped}`);
  if (!apply) {
    console.log("No writes were made. Re-run with --apply after reviewing the dry-run output.");
  }
}

main().catch((error) => {
  console.error(`backfillUsernameReservations failed: ${error.message}`);
  process.exit(1);
});
