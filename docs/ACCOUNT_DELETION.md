# Account Deletion

Satır Arası v1.0 account deletion uses a temporary no-billing architecture:

```text
Android client -> Firestore accountDeletionRequests/{uid}
Local admin panel -> Firebase Admin SDK -> Firestore cleanup -> Firebase Auth delete last
```

No Cloud Functions are deployed for this flow yet. Do not delete production data until the request has been manually reviewed and tested.

## User flow

Android path:

```text
Profile -> Settings -> Hesabımı Sil
```

The screen warns users that the account will be permanently deleted, the profile will become inaccessible, published quotes will be deleted, likes/favorites/achievements/statistics will be removed, the action cannot be undone, and moderation records may be retained only in anonymized form where required for abuse prevention and audit history.

Email/password accounts must re-enter the current password. The user must type:

```text
HESABIMI SİL
```

After a successful request, the app routes the user to a pending deletion state and disables normal app usage. The app must not claim deletion is complete until the local admin workflow finishes.

## Firestore request document

Collection:

```text
accountDeletionRequests/{uid}
```

Fields:

```text
userId
username
normalizedUsername
status = PENDING
requestedAt
requestedBy
reason
profileHidden = true
deletionVersion
completedAt = null
completedBy = null
failureCode = null
failureMessage = null
currentPhase
completedPhases
```

The client also marks:

```text
users/{uid}.deletionPending = true
users/{uid}.profileHidden = true
users/{uid}.deletionRequestedAt = request time
```

This makes pending-account write restrictions enforceable in Firestore Rules.

## Data deletion matrix

| Collection / data | Action | Notes |
| --- | --- | --- |
| Firebase Auth account | Delete | Last phase only. If already missing, treat as idempotent success. |
| `users/{uid}` | Delete | Marked hidden/pending first, then deleted by Admin SDK. |
| `usernames/{normalizedUsername}` | Delete | Releases the username reservation after profile cleanup. |
| `quotes` where `userId == uid` | Delete | Related quote likes/favorites are deleted first. |
| `likes` where `userId == uid` | Delete | Likes created by the user are removed. |
| `favorites` where `userId == uid` | Delete | Private saved collection is removed. |
| `userAchievements` where `userId == uid` | Delete | Achievement unlock records are removed. |
| `userStats/{uid}` | Delete | XP, level and stats are removed. |
| `follows` / social relations if present | Delete | Both follower and followed references are removed if collection exists. |
| `reports` | Anonymize | `reporterUserId` / `reportedUserId` replaced with a non-reversible scoped reference when matching deleted user. |
| `moderationActions` | Anonymize | `targetUserId` replaced with the scoped anonymized reference when matching deleted user. |
| `reporterStats`, `moderationStats`, `moderatorStats`, `userRestrictions` | Delete known user doc | Abuse-prevention data tied to the raw UID is removed or represented only through retained anonymized records. |
| `accountDeletionRequests/{uid}` | Retain sanitized | Completion record remains; username is replaced after completion. |
| `accountDeletionActions` | Retain sanitized | Immutable local audit log. No email, password, token or service-account data. |

Anonymization uses:

```text
deleted_<sha256 scoped hash prefix>
```

Do not use a shared literal like `DELETED_USER`.

## Local admin flow

Admin panel page:

```text
Hesap Silme Talepleri
```

The detail page shows request metadata, collection counts, phase progress and audit records. Execution requires a local admin session, POST request, CSRF token and explicit UID confirmation.

## Deletion phases

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

Firebase Auth deletion must happen only after Firestore cleanup succeeds.

If a phase fails, request status becomes `FAILED`, progress is retained, retry is possible, and detailed technical errors stay in server logs.

## Firestore Rules

Normal clients can create only their own valid request and exact-get only their own request.

Normal clients cannot list, update or delete deletion requests; access deletion audit logs; or continue writing quotes, likes, favorites, reports or stats after pending status.

Admin SDK remains unrestricted because it bypasses Firestore Rules.

## Future public `/account-deletion` page

A future public web page should include app name, developer/company identification, request steps, deleted data categories, anonymized/retained categories, retention explanation, contact method and processing expectations.

Do not publish a public deletion website until the legal/privacy text is reviewed.

## Safe rollout order

1. Run `npm --prefix admin-panel test`.
2. Run `npm run test:rules`.
3. Run `.\gradlew.bat assembleDebug`.
4. Manually review Firestore Rules diff.
5. Deploy Firestore Rules manually when ready.
6. Release Android only after QA confirms pending-routing and sign-out behavior.
7. Run the admin panel only on a trusted local machine.
8. Test with a dedicated non-production account before any real user account.

This document does not approve production deletion by itself.
