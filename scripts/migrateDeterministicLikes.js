const { initializeAdmin, hasApplyFlag, chunk } = require("./adminUtils");

async function main() {
  const apply = hasApplyFlag();
  const { db } = initializeAdmin();
  const snapshot = await db.collection("likes").get();
  const byTargetId = new Map();
  let invalid = 0;

  snapshot.forEach((document) => {
    const data = document.data();
    const userId = data.userId;
    const quoteId = data.quoteId;
    if (!userId || !quoteId) {
      invalid += 1;
      return;
    }
    const deterministicId = `${userId}_${quoteId}`;
    const current = byTargetId.get(deterministicId);
    const createdAt = data.createdAt && data.createdAt.toMillis ? data.createdAt.toMillis() : Number.MAX_SAFE_INTEGER;
    const currentCreatedAt = current && current.data.createdAt && current.data.createdAt.toMillis
      ? current.data.createdAt.toMillis()
      : Number.MAX_SAFE_INTEGER;
    if (!current || createdAt < currentCreatedAt) {
      byTargetId.set(deterministicId, { document, data });
    }
  });

  const writes = [];
  const deletes = [];
  snapshot.forEach((document) => {
    const data = document.data();
    if (!data.userId || !data.quoteId) {
      return;
    }
    const deterministicId = `${data.userId}_${data.quoteId}`;
    const keeper = byTargetId.get(deterministicId);
    if (document.id !== deterministicId && keeper && keeper.document.id === document.id) {
      writes.push({ id: deterministicId, data: { ...data, likeId: deterministicId } });
      deletes.push(document.ref);
    } else if (keeper && keeper.document.id !== document.id) {
      deletes.push(document.ref);
    }
  });

  if (apply) {
    for (const group of chunk([...writes.map((write) => ({ type: "set", ...write })), ...deletes.map((ref) => ({ type: "delete", ref }))], 400)) {
      const batch = db.batch();
      group.forEach((operation) => {
        if (operation.type === "set") {
          batch.set(db.collection("likes").doc(operation.id), operation.data, { merge: true });
        } else {
          batch.delete(operation.ref);
        }
      });
      await batch.commit();
    }
  }

  console.log(`Mode: ${apply ? "APPLY" : "DRY_RUN"}`);
  console.log(`Likes scanned: ${snapshot.size}`);
  console.log(`Invalid likes skipped: ${invalid}`);
  console.log(`Deterministic like documents to create/update: ${writes.length}`);
  console.log(`Duplicate/legacy like documents to delete: ${deletes.length}`);
  if (!apply) {
    console.log("No writes were made. Re-run with --apply after reviewing the dry-run output.");
  }
}

main().catch((error) => {
  console.error(`migrateDeterministicLikes failed: ${error.message}`);
  process.exit(1);
});
