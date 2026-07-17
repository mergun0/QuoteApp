# Local Admin Panel

QuoteApp v1.0 uses a temporary no-billing moderation architecture.

## Architecture

```text
Android client -> Firestore reports/{quoteId}_{reporterUid}
Local admin panel -> Firebase Admin SDK -> report review and audit log
```

Cloud Functions moderation code remains in `functions/` for future activation, but it is not deployed for this temporary release plan.

## Local startup

Install dependencies once:

```bash
npm --prefix admin-panel install
```

Run locally:

```bash
set LOCAL_ADMIN_PASSWORD=change-this-to-a-long-local-password
set GOOGLE_APPLICATION_CREDENTIALS=C:\secure\serviceAccountKey.json
npm --prefix admin-panel start
```

Optional environment variables:

```text
ADMIN_PANEL_HOST=127.0.0.1
ADMIN_PANEL_PORT=4173
LOCAL_ADMIN_ACTOR=LOCAL_ADMIN
FIREBASE_SERVICE_ACCOUNT_PATH=C:\secure\serviceAccountKey.json
```

Default bind host is `127.0.0.1`. Do not expose this panel publicly.

## Service account handling

- Never commit service account files.
- Prefer `GOOGLE_APPLICATION_CREDENTIALS` or `FIREBASE_SERVICE_ACCOUNT_PATH`.
- `admin-panel/serviceAccountKey.json` is supported for local use only and is ignored by the repository service-account ignore rules.
- The service account is loaded only by Node.js server code.
- Credentials are never sent to browser JavaScript, HTML responses or logs.

## Review flow

Pages:

- login gate
- dashboard
- pending reports
- approved reports
- rejected reports
- report detail

Report detail shows:

- quote text/title
- quote owner username/UID
- reporter username/UID
- reason
- description
- createdAt
- current status

Actions:

- approve report
- reject report
- hide reported quote after an approved report

`hide reported quote` uses a soft-hide update:

```text
quotes/{quoteId}.isHidden = true
quotes/{quoteId}.hiddenAt = server timestamp
quotes/{quoteId}.hiddenBy = LOCAL_ADMIN
quotes/{quoteId}.hiddenReason = approved report id
```

Before relying on soft hidden quotes in production feeds, Android queries should be updated to exclude `isHidden == true`.

## Audit log

Local admin actions create:

```text
moderationActions/{autoId}
```

Fields:

```text
actionType
reportId
quoteId
targetUserId
actor = LOCAL_ADMIN
previousStatus
newStatus
createdAt
note
```

Normal clients cannot read or write this collection. The local panel uses Firebase Admin SDK, which bypasses Firestore Rules.

## Current anti-abuse protections

Firestore Rules enforce:

- authenticated report creation only
- deterministic report id: `quoteId_reporterUid`
- one report per reporter per quote
- no self-report
- immutable reports from Android
- bounded reason and description
- exact field allowlist
- no global report list access for normal users

Deferred until trusted backend activation:

- reliable daily report limits
- same-target daily limits
- invalid-report streak restrictions
- reporter/moderator/moderation counters
- automatic reporting restrictions

## Future migration to Cloud Functions

When billing/backend deployment is approved:

1. Deploy callable Functions from `functions/`.
2. Switch Android `ReportRepository` back to callable `submitReport`.
3. Run Functions emulator tests.
4. Run Firestore Rules tests.
5. Deploy stricter report-denying Firestore Rules.
6. Keep or replace the local panel with a trusted hosted admin surface.

## Safe rollout order for v1.0 temporary architecture

1. Run Android `assembleDebug`.
2. Run `npm run test:rules`.
3. Run `npm --prefix functions run lint`.
4. Run `npm --prefix functions run build`.
5. Run `npm --prefix functions run emulators:test`.
6. Run `npm --prefix admin-panel test`.
7. Manually review Firestore Rules diff.
8. Deploy Firestore Rules only when ready.
9. Do not deploy Functions for this no-billing release.
10. Run the admin panel only on a trusted local machine.
