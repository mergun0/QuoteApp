# Local Admin Panel

Satır Arası / QuoteApp v1.0 uses a temporary no-billing moderation architecture.

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

## Navigation structure

The redesigned server-rendered panel uses a SaaS-style admin layout:

- left dark navy sidebar
- compact top bar
- light content background
- white rounded cards
- responsive tables and filters

Sidebar pages:

- Dashboard
- Bekleyen Raporlar
- Onaylanan Raporlar
- Reddedilen Raporlar
- İşlem Geçmişi
- Çıkış

On mobile, the sidebar collapses behind a touch-friendly menu button.

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
- moderation action history

Dashboard shows:

- Bekleyen Raporlar
- Onaylanan Raporlar
- Reddedilen Raporlar
- Gizlenen Alıntılar
- Bugünkü İşlemler
- latest pending reports
- latest moderation actions
- quick actions
- system status

Report list pages include:

- search by quote text, username or UID
- reason filter
- date sorting
- pagination
- empty state
- status/reason badges

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

The v1.0 visibility migration requires every visible quote to have `isHidden = false`. Missing `isHidden` is no longer accepted by tightened Rules after the production backfill.

Current Android behavior:

- Home, Discover, Favorites and public profile lists suppress hidden quote documents.
- Quote Detail shows `Bu alıntı artık görüntülenemiyor.` for hidden or inaccessible quote IDs.
- Like and save actions reject hidden quote documents.
- Existing favorite/like documents may remain, but they must not expose hidden quote content.

Prepared Rules behavior after the visibility migration:

- direct normal-user `get` reads for `quotes/{quoteId}` are allowed only when `isHidden == false`
- normal-user list queries must include a visible-quote constraint compatible with `isHidden == false`
- hidden or legacy-unbackfilled quotes are not readable by normal users

Visibility migration commands:

```bash
npm --prefix admin-panel run backfill:quote-visibility
npm --prefix admin-panel run backfill:quote-visibility -- --apply
npm --prefix admin-panel run backfill:quote-visibility -- --verify
```

See `docs/QUOTE_VISIBILITY_MIGRATION.md` for the exact rollout order. Do not deploy tightened Rules before production backfill and index readiness.

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

## Account deletion requests

The panel includes a `Hesap Silme Talepleri` page for the v1.0 account deletion completion workflow.

Pages:

- pending deletion requests
- completed deletion requests
- failed deletion requests
- request detail

The detail view shows the request, affected collection counts, progress phases and deletion audit records. Execution is POST-only, CSRF-protected and requires typing the target UID as explicit confirmation.

Deletion phases:

```text
PROFILE
QUOTES
LIKES
FAVORITES
ACHIEVEMENTS
STATS
SOCIAL_RELATIONS
REPORTS_ANONYMIZED
USERNAME_RELEASED
AUTH_DELETED
COMPLETED
```

Firebase Auth deletion happens last. If Auth deletion fails, the request remains `FAILED` and can be retried. If Auth is already missing, the Auth phase is treated as idempotently complete.

Deletion audit records are written to:

```text
accountDeletionActions/{autoId}
```

They must not include email, password, raw tokens or service-account data.

See `docs/ACCOUNT_DELETION.md` for the full data deletion matrix and safe rollout order.

## Required Firestore indexes

Defined in `firestore.indexes.json`:

```text
reports:
  status ASC
  createdAt DESC

quotes:
  isHidden ASC
  createdAt DESC

quotes:
  userId ASC
  isHidden ASC
  createdAt DESC

moderationActions:
  actionType ASC
  createdAt DESC
```

Deploy indexes manually only after review:

```bash
firebase deploy --project <real-project-id> --only firestore:indexes
```

Do not deploy indexes automatically from Codex tasks.

## Panel security

The panel preserves and adds:

- Firebase Admin SDK server-side only
- no service account values in HTML, JS or logs
- `LOCAL_ADMIN_PASSWORD` from environment
- minimum 16-character local admin password
- login attempt throttling
- 8-hour in-memory session timeout
- HttpOnly session cookie
- SameSite=Strict cookies
- CSRF validation on POST actions
- POST-only moderation actions
- confirmation dialogs for destructive actions
- local security headers / CSP
- localhost bind by default

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
- hosted multi-user admin UI
- production-grade audit analytics

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
