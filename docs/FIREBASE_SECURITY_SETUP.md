# Firebase Security Setup

Do not deploy rules or apply migrations until the dry-run output has been reviewed.

## Role model

Normal users are registered with:

```text
users/{uid}.role = "user"
```

Privileged authorization uses Firebase Auth custom claims, not the public user document:

```text
request.auth.token.role == "moderator"
request.auth.token.role == "admin"
```

Set roles from a trusted machine only:

```bash
node scripts/setUserRole.js <uid> user
node scripts/setUserRole.js <uid> moderator
node scripts/setUserRole.js <uid> admin
```

Backfill missing public role cache values:

```bash
node scripts/backfillUserRoles.js
node scripts/backfillUserRoles.js --apply
```

Dry-run is the default. `--apply` is required for writes.

## Username reservation rollout

Registration now writes:

```text
usernames/{normalizedUsername}
users/{uid}
```

`users/{uid}` must not contain email. v1.0 uses email/password login only; username remains the public in-app identity and is reserved only through `usernames/{normalizedUsername}`.

For existing users, create username reservation documents with an Admin SDK migration before removing legacy `users.email` data or deploying strict rules.

Review dry-run output first:

```bash
node scripts/backfillUsernameReservations.js
node scripts/removePublicUserPrivateFields.js
```

Apply only after confirming there are no username collisions:

```bash
node scripts/backfillUsernameReservations.js --apply
node scripts/removePublicUserPrivateFields.js --apply
```

Run the username reservation backfill before deleting public `users.email`.

Username editing is disabled for v1.0. Replacing username reservations safely requires a backend-owned rename flow and should not be re-enabled with direct client writes.

## Like and favorite migration

New deterministic ids:

```text
likes/{userId}_{quoteId}
favorites/{userId}_{quoteId}
```

Review dry-runs:

```bash
node scripts/backfillUsernameReservations.js
node scripts/removePublicUserPrivateFields.js
node scripts/migrateDeterministicLikes.js
node scripts/migrateDeterministicFavorites.js
```

Apply only after review:

```bash
node scripts/backfillUsernameReservations.js --apply
node scripts/removePublicUserPrivateFields.js --apply
node scripts/migrateDeterministicLikes.js --apply
node scripts/migrateDeterministicFavorites.js --apply
```

Favorite migration also recalculates `quotes/{quoteId}.favoriteCount`.

## Firestore Rules

Local test:

```bash
npm run test:rules
```

Deploy manually only after migrations and test pass:

```bash
firebase deploy --project <real-firebase-project-id> --only firestore:rules
```

This task intentionally does not deploy rules.
The checked-in `.firebaserc` uses `demo-quoteapp` for local emulator tests only.

## Quote visibility migration

Before deploying tightened quote visibility Rules, run the local Admin SDK backfill described in `docs/QUOTE_VISIBILITY_MIGRATION.md`.

Dry-run:

```bash
npm --prefix admin-panel run backfill:quote-visibility
```

Apply:

```bash
npm --prefix admin-panel run backfill:quote-visibility -- --apply
```

Verify:

```bash
npm --prefix admin-panel run backfill:quote-visibility -- --verify
```

Then deploy and wait for required indexes:

```bash
firebase deploy --project quoteapp-a92e4 --only firestore:indexes
```

If Firebase CLI asks whether to delete indexes that exist remotely but are absent locally, choose `No` until `firestore.indexes.json` is reviewed. Required production indexes were previously deleted by accepting this prompt accidentally.

Indexes can be visible as `Building` before they become `Enabled`. Android Home depends on the `quotes: userId ASC, isHidden ASC, createdAt DESC` index and may continue to show a missing-index error until it is enabled.

Only after the updated Android build uses `whereEqualTo("isHidden", false)` and production data is verified should tightened Rules be deployed:

```bash
firebase deploy --project <real-firebase-project-id> --only firestore:rules
```

Older Android versions that still issue broad quote list queries can receive permission-denied errors after the tightened Rules deployment.

## Moderation rollout

The long-term target architecture remains:

```text
Android client -> callable Cloud Function -> Admin SDK transaction -> Firestore
```

For the temporary no-billing v1.0 release, Cloud Functions are not deployed. Android can create only a narrow pending report document:

```text
reports/{quoteId}_{reporterUid}
```

Firestore Rules validate:

- signed-in user
- deterministic report id
- reporter uid equals `request.auth.uid`
- reported user matches the quote owner
- no self-report
- `status == "PENDING"`
- review fields are null
- exact field allowlist

Normal clients still must not write these moderation collections directly:

```text
moderationActions
moderationStats
moderatorStats
reporterStats
reportRateLimits
userRestrictions
```

Moderator/admin authorization for the future Cloud Functions backend uses Firebase Auth custom claims:

```text
role == "moderator"
role == "admin"
```

The public `users/{uid}.role` field is only a display/cache field and must not be treated as privileged authorization.

During the temporary local-admin v1.0 architecture, moderation collections are not exposed to normal, moderator or admin clients through Firestore Rules. The local panel uses Firebase Admin SDK instead.

Local Functions checks:

```bash
npm --prefix functions run lint
npm --prefix functions run build
npm --prefix functions run emulators:test
```

Important rollout order:

Temporary v1.0 no-billing order:

1. Run Android build and Firestore Rules tests.
2. Deploy Firestore Rules that allow only valid pending report creates.
3. Keep Cloud Functions undeployed.
4. Run `admin-panel/` locally with Firebase Admin SDK for review.

Future callable order:

1. Deploy callable functions after review.
2. Migrate Android report submission to `submitReport`.
3. Run `npm run test:rules`.
4. Deploy stricter Firestore Rules that deny normal client report creates.

See `docs/MODERATION_BACKEND.md` for callable contracts and collection details.
See `docs/LOCAL_ADMIN_PANEL.md` for the temporary local review panel.

## App Check rollout

Android is prepared for:

- debug builds: Debug App Check provider
- release builds: Play Integrity provider

Manual Firebase Console steps:

1. Add the release signing SHA-256 fingerprint.
2. Register debug App Check tokens from development devices.
3. Enable App Check monitoring first.
4. Watch Firebase metrics and crash reports.
5. Enforce App Check only after a monitoring period confirms healthy traffic.

Do not commit App Check debug tokens.

## Known remaining limitation

Achievements, XP and levels are bounded by Security Rules but still written by the client. A modified APK or direct Firestore request could still attempt to award progress within allowed bounds. These values must remain non-authoritative and must not unlock paid access, security privileges or money-related features until moved to trusted backend code.
