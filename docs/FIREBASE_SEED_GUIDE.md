# Firebase Seed Guide — Levels and Achievements

This guide explains how to import `firebase_seed_levels_achievements.json` into Cloud Firestore.

The seed file contains two root objects:

- `levels`
- `achievements`

Each key inside these objects is used as the Firestore document ID.

## Target Firestore collections

```text
levels/{levelId}
achievements/{achievementId}
```

Examples:

```text
levels/level_1
achievements/likes_received_10
```

## One-time setup

From the project root:

```bash
npm init -y
npm install firebase-admin
```

## Service account

1. Open Firebase Console.
2. Go to Project Settings.
3. Open Service accounts.
4. Generate a new private key.
5. Save the downloaded file in the project root as:

```text
serviceAccountKey.json
```

This file is intentionally ignored by Git. Do not commit service account files.

The script also supports Application Default Credentials if `serviceAccountKey.json` is not present.

## Run the seed script

From the project root:

```bash
node scripts/seedFirestore.js
```

The script will:

1. Read `firebase_seed_levels_achievements.json`.
2. Write each `levels` item to `levels/{documentId}`.
3. Write each `achievements` item to `achievements/{documentId}`.
4. Use `{ merge: true }` so re-running the script safely updates existing documents.

## Backfill favorite counts

If legacy favorite documents exist but quote documents have missing or stale `favoriteCount`
values, run this one-time Admin SDK script from the project root:

```bash
node scripts/backfillFavoriteCounts.js
```

The script will:

1. Read every document in `favorites`.
2. Group favorites by `quoteId`.
3. Update every `quotes/{quoteId}.favoriteCount` with the matching favorite count.
4. Set quotes with no matching favorites to `0`.
5. Log scanned favorites, updated quotes, missing quote documents and final status.

## Notes

- Run this script from a trusted local machine or CI environment.
- Do not run Admin SDK scripts from the Android app.
- Keep `achievementId` equal to the achievement document ID.
- Keep `level_{number}` as the level document ID.
- The app can safely run before this data exists; it falls back to Level 1 and no achievements.
- Do not seed `userAchievements` manually. That collection is created by the unlock engine.
- Do not seed `userStats` for all users manually unless needed; the app creates default stats when missing.
