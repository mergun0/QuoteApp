const { initializeAdmin, hasApplyFlag, chunk } = require("./adminUtils");

async function main() {
  const apply = hasApplyFlag();
  const { db } = initializeAdmin();
  const snapshot = await db.collection("users").get();
  const writes = [];
  const totals = {
    scanned: snapshot.size,
    missingRole: 0,
    preservedPrivileged: 0,
    updated: 0,
  };

  snapshot.forEach((document) => {
    const role = document.get("role");
    if (role === "moderator" || role === "admin") {
      totals.preservedPrivileged += 1;
      return;
    }
    if (!role) {
      totals.missingRole += 1;
      writes.push(document.ref);
    }
  });

  if (apply) {
    for (const refs of chunk(writes, 450)) {
      const batch = db.batch();
      refs.forEach((ref) => batch.set(ref, { role: "user" }, { merge: true }));
      await batch.commit();
      totals.updated += refs.length;
    }
  }

  console.log(`Mode: ${apply ? "APPLY" : "DRY_RUN"}`);
  console.log(`Users scanned: ${totals.scanned}`);
  console.log(`Users missing role: ${totals.missingRole}`);
  console.log(`Privileged roles preserved: ${totals.preservedPrivileged}`);
  console.log(`Users updated: ${totals.updated}`);
  if (!apply) {
    console.log("No writes were made. Re-run with --apply to update missing roles.");
  }
}

main().catch((error) => {
  console.error(`backfillUserRoles failed: ${error.message}`);
  process.exit(1);
});
