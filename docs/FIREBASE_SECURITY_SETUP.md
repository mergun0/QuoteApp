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
usernameLogins/{normalizedUsername}
users/{uid}
```

`users/{uid}` must not contain email. Username login still requires an exact username-to-email mapping because Firebase Auth email/password sign-in needs an email. The `usernameLogins` collection is not listable; it supports only exact document lookups for login.

For existing users, create username reservation/login documents with an Admin SDK migration before removing legacy `users.email` data or deploying strict rules.

Review dry-run output first:

```bash
node scripts/backfillUsernameReservations.js
node scripts/removePublicUserPrivateFields.js
```

Apply only after confirming there are no username collisions or missing Auth emails:

```bash
node scripts/backfillUsernameReservations.js --apply
node scripts/removePublicUserPrivateFields.js --apply
```

Run the username reservation backfill before deleting public `users.email`, because legacy username login may need the email value to create `usernameLogins/{normalizedUsername}`.

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
