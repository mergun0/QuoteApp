const { initializeAdmin, hasApplyFlag, chunk } = require("./adminUtils");

async function main() {
  const apply = hasApplyFlag();
  const { db } = initializeAdmin();
  const favoritesSnapshot = await db.collection("favorites").get();
  const byTargetId = new Map();
  let invalid = 0;

  favoritesSnapshot.forEach((document) => {
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
  const countsByQuoteId = new Map();
  for (const [deterministicId, entry] of byTargetId.entries()) {
    const data = { ...entry.data, favoriteId: deterministicId };
    writes.push({ id: deterministicId, data });
    countsByQuoteId.set(data.quoteId, (countsByQuoteId.get(data.quoteId) || 0) + 1);
  }
  favoritesSnapshot.forEach((document) => {
    const data = document.data();
    if (!data.userId || !data.quoteId) {
      return;
    }
    const deterministicId = `${data.userId}_${data.quoteId}`;
    if (document.id !== deterministicId || byTargetId.get(deterministicId).document.id !== document.id) {
      deletes.push(document.ref);
    }
  });

  const quotesSnapshot = await db.collection("quotes").get();
  const quoteUpdates = [];
  quotesSnapshot.forEach((document) => {
    quoteUpdates.push({
      ref: document.ref,
      count: countsByQuoteId.get(document.id) || 0,
    });
  });

  if (apply) {
    const operations = [
      ...writes.map((write) => ({ type: "set", ...write })),
      ...deletes.map((ref) => ({ type: "delete", ref })),
      ...quoteUpdates.map((update) => ({ type: "quoteCount", ...update })),
    ];
    for (const group of chunk(operations, 400)) {
      const batch = db.batch();
      group.forEach((operation) => {
        if (operation.type === "set") {
          batch.set(db.collection("favorites").doc(operation.id), operation.data, { merge: true });
        } else if (operation.type === "delete") {
          batch.delete(operation.ref);
        } else {
          batch.set(operation.ref, { favoriteCount: operation.count }, { merge: true });
        }
      });
      await batch.commit();
    }
  }

  console.log(`Mode: ${apply ? "APPLY" : "DRY_RUN"}`);
  console.log(`Favorites scanned: ${favoritesSnapshot.size}`);
  console.log(`Invalid favorites skipped: ${invalid}`);
  console.log(`Deterministic favorite documents to create/update: ${writes.length}`);
  console.log(`Duplicate/legacy favorite documents to delete: ${deletes.length}`);
  console.log(`Quote favoriteCount documents to recalculate: ${quoteUpdates.length}`);
  if (!apply) {
    console.log("No writes were made. Re-run with --apply after reviewing the dry-run output.");
  }
}

main().catch((error) => {
  console.error(`migrateDeterministicFavorites failed: ${error.message}`);
  process.exit(1);
});
