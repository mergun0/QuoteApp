# Moderation Backend

QuoteApp moderation has a trusted Cloud Functions backend prepared for future activation.

For the temporary no-billing v1.0 architecture, these functions remain checked in but inactive/not deployed. Android submits narrowly scoped pending reports directly to Firestore, and review happens only through the local Node.js admin panel described in `docs/LOCAL_ADMIN_PANEL.md`.

## Runtime

- Cloud Functions for Firebase v2
- TypeScript
- Node.js 18 runtime target
- Firebase Admin SDK
- Callable HTTPS functions
- Region: `europe-west1`

`europe-west1` was selected as a stable EU region close to the app's current expected user base. Before production deployment, verify that this region matches the Firestore database location strategy.

## Local commands

```bash
npm --prefix functions install
npm --prefix functions run lint
npm --prefix functions run build
npm --prefix functions run emulators:test
npm run test:rules
```

Do not deploy functions or rules automatically from local development tasks.

## Callable functions

### submitReport

Authenticated users submit reports through the backend only.

Input:

```json
{
  "quoteId": "quoteId",
  "reason": "Spam",
  "description": "optional text"
}
```

Server-owned behavior:

- derives `reporterUserId` from Firebase Auth;
- derives `reportedUserId` from `quotes/{quoteId}.userId`;
- rejects self-reporting;
- rejects duplicate `reports/{quoteId}_{reporterUserId}`;
- enforces daily and same-target limits;
- creates a trusted pending report;
- updates `reportRateLimits`, `reporterStats`, and `moderationStats`.

### reviewReport

Requires custom claim:

```text
role == moderator OR role == admin
```

Input:

```json
{
  "reportId": "quoteId_reporterUid",
  "decision": "APPROVED",
  "note": "optional moderator note"
}
```

Server-owned behavior:

- updates only pending reports;
- writes `reviewedAt`, `reviewedBy`, `status`, `isValidReport`;
- updates reporter, moderator, and target moderation counters;
- creates a `moderationActions` audit record;
- may create a reporting restriction when invalid-report patterns exceed policy thresholds.

### deleteReportedQuote

Requires moderator/admin custom claim and an approved report.

Server-owned behavior:

- soft-hides the quote with `isHidden`, `hiddenAt`, `hiddenBy`, and `hiddenReason`;
- does not physically delete the quote document;
- increments moderation counters once;
- creates a moderation audit action.

### setUserRole

Requires admin custom claim.

Server-owned behavior:

- sets Firebase Auth custom claims;
- preserves unrelated existing custom claims;
- writes a public `users/{uid}.role` cache for UI display only;
- creates an audit action.

### setReportingRestriction

Requires admin custom claim.

Server-owned behavior:

- writes or clears `userRestrictions/{uid}`;
- creates an audit action.

## Firestore structure

```text
reports/{quoteId}_{reporterUserId}
moderationActions/{autoId}
moderationStats/{reportedUserId}
moderatorStats/{moderatorUserId}
reporterStats/{reporterUserId}
reportRateLimits/{reporterUserId}_{yyyy-MM-dd}
userRestrictions/{userId}
```

## Security model

Normal clients:

- cannot create, update, or delete reports directly;
- cannot write moderation counters;
- cannot write moderation actions;
- cannot write report rate limits;
- cannot write user restrictions.

Moderators/admins:

- are authorized by Firebase Auth custom claims, not by public `users.role`;
- can read moderation documents as allowed by `firestore.rules`;
- perform writes through Cloud Functions only.

Admin SDK bypasses Firestore Rules, so all validation for moderation writes is implemented in the callable functions.

## App Check

The callables are App Check-ready. Enforcement is currently disabled in code for emulator/development compatibility:

```ts
enforceAppCheck: false
```

Before production enforcement:

1. verify Android App Check debug and Play Integrity rollout;
2. monitor callable traffic;
3. flip callable options to enforce App Check;
4. test release builds before deployment.

## Android migration note

Temporary v1.0 behavior:

```text
Android -> reports/{quoteId}_{reporterUid}
```

Android may create only pending report documents with a strict field allowlist. It cannot update/delete reports or review content.

Future backend behavior:

```text
Android -> callable submitReport -> Admin SDK transaction -> Firestore
```

Do not deploy the callable-only report-denying Firestore Rules until Android is migrated back to `submitReport` and the Functions backend is deployed.

## Audit and privacy notes

- Users cannot forge `reportedUserId`, `reporterUserId`, `status`, or review fields.
- Moderator actions are logged in `moderationActions`.
- Public user documents must not contain authentication email.
- `users.role` is not trusted for privileged access.
