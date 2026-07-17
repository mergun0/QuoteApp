const { initializeFirebaseAdmin } = require("../src/firebaseAdmin");

const BATCH_LIMIT = 450;
const READ_PAGE_SIZE = 500;
const MAX_RETRIES = 3;

function hasOwn(object, key) {
  return Object.prototype.hasOwnProperty.call(object || {}, key);
}

function npmConfigEnabled(value) {
  return value === true || value === "true" || value === "1";
}

function parseNpmOriginalArgs(env = process.env) {
  if (!env.npm_config_argv) {
    return [];
  }
  try {
    const parsed = JSON.parse(env.npm_config_argv);
    const original = Array.isArray(parsed.original) ? parsed.original : [];
    return original.filter((arg) => typeof arg === "string" && arg.startsWith("--"));
  } catch {
    return [];
  }
}

function parseArgs(argv = process.argv.slice(2), env = process.env) {
  const validFlags = new Set(["--", "--dry-run", "--apply", "--verify"]);
  const args = [
    ...argv,
    ...parseNpmOriginalArgs(env),
  ].filter((arg) => typeof arg === "string" && arg.length > 0);

  const unknown = args.filter((arg) => arg.startsWith("-") && !validFlags.has(arg));
  if (unknown.length > 0) {
    throw new Error(`Unknown argument: ${unknown.join(", ")}`);
  }

  const apply = args.includes("--apply") || npmConfigEnabled(env.npm_config_apply);
  const verify = args.includes("--verify") || npmConfigEnabled(env.npm_config_verify);
  if (apply && verify) {
    throw new Error("Use either --verify or --apply, not both.");
  }
  return { apply, verify };
}

function createTotals() {
  return {
    scanned: 0,
    missingField: 0,
    alreadyVisible: 0,
    alreadyHidden: 0,
    updated: 0,
    failed: 0,
  };
}

async function commitWithRetry(batch, logger, attempt = 1) {
  try {
    await batch.commit();
    return true;
  } catch (error) {
    if (attempt < MAX_RETRIES && isTransientError(error)) {
      logger.warn(`Batch write failed transiently. Retrying ${attempt}/${MAX_RETRIES}.`);
      return commitWithRetry(batch, logger, attempt + 1);
    }
    logger.error("Batch write failed.", error);
    return false;
  }
}

function isTransientError(error) {
  const code = error && (error.code || error.status);
  return code === 10
    || code === 14
    || code === "ABORTED"
    || code === "UNAVAILABLE"
    || code === "deadline-exceeded"
    || code === "resource-exhausted";
}

function logTotals(totals, { apply, verify }, logger) {
  logger.log("Quote visibility backfill completed.");
  logger.log(`Mode: ${verify ? "verify" : apply ? "apply" : "dry-run"}`);
  logger.log(`Quotes scanned: ${totals.scanned}`);
  logger.log(`Missing isHidden: ${totals.missingField}`);
  logger.log(`Already visible: ${totals.alreadyVisible}`);
  logger.log(`Already hidden: ${totals.alreadyHidden}`);
  logger.log(`Updated: ${totals.updated}`);
  logger.log(`Failed: ${totals.failed}`);
}

async function runBackfillQuoteVisibility({ db, apply = false, verify = false, logger = console } = {}) {
  if (!db) {
    throw new Error("Firestore db instance is required.");
  }

  const totals = createTotals();
  let pendingBatch = db.batch();
  let pendingWrites = 0;

  const flush = async () => {
    if (!apply || pendingWrites === 0) {
      return;
    }
    const success = await commitWithRetry(pendingBatch, logger);
    if (!success) {
      totals.failed += pendingWrites;
      totals.updated -= pendingWrites;
    }
    pendingBatch = db.batch();
    pendingWrites = 0;
  };

  let lastDocument = null;
  let hasMore = true;

  while (hasMore) {
    let query = db.collection("quotes")
      .orderBy("__name__")
      .limit(READ_PAGE_SIZE);
    if (lastDocument) {
      query = query.startAfter(lastDocument);
    }

    const snapshot = await query.get();
    hasMore = snapshot.docs.length === READ_PAGE_SIZE;
    if (snapshot.docs.length > 0) {
      lastDocument = snapshot.docs[snapshot.docs.length - 1];
    }

    for (const quoteDoc of snapshot.docs) {
      totals.scanned += 1;
      const data = quoteDoc.data() || {};
      if (!hasOwn(data, "isHidden")) {
        totals.missingField += 1;
        if (apply && !verify) {
          pendingBatch.update(quoteDoc.ref, { isHidden: false });
          pendingWrites += 1;
          totals.updated += 1;
          if (pendingWrites >= BATCH_LIMIT) {
            await flush();
          }
        }
        continue;
      }

      if (data.isHidden === true) {
        totals.alreadyHidden += 1;
      } else {
        totals.alreadyVisible += 1;
      }
    }
  }

  await flush();
  logTotals(totals, { apply, verify }, logger);

  if (verify && totals.missingField > 0) {
    throw new Error(`Verification failed: ${totals.missingField} quote documents are missing isHidden.`);
  }

  return totals;
}

async function main() {
  const options = parseArgs();
  const { db } = initializeFirebaseAdmin();
  await runBackfillQuoteVisibility({ db, ...options });
}

if (require.main === module) {
  main().catch((error) => {
    console.error(error.message || error);
    process.exit(1);
  });
}

module.exports = {
  parseArgs,
  runBackfillQuoteVisibility,
};
