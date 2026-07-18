# Known Issues

## UI

- Quote card action spacing can be improved.

Priority:
Medium

-------------------------

## UX

- Search keyboard animation can be smoother.

Priority:
Low

-------------------------

## Performance

- Profile loading has slight delay.

Priority:
Medium

-------------------------

## Future Polish

- Add icon animations.
- Future product ideas should not be duplicated here. Keep them in `docs/FEATURE_BACKLOG.md`; keep this file focused on known defects, rollout risks and polish issues.

Priority:
Low

-------------------------

## Security Rollout

- Temporary no-billing v1.0 uses direct Firestore pending report creation instead of deployed Cloud Functions.
- Reliable daily report limits, same-target daily limits, invalid-report streak restrictions and moderation counters require the trusted Functions backend and are deferred.
- Hidden quote backfill has been reported complete, but the v1.0 visibility migration is not fully complete until required Firestore indexes are deployed/enabled, tightened Rules are deployed, and Android QA confirms visible-only quote lists.
- Android Home can show a missing-index error until the `quotes: userId ASC, isHidden ASC, createdAt DESC` composite index is deployed and enabled.
- A previous index deploy prompt deleted required remote indexes that were missing locally. Future `firestore:indexes` deploys must choose `No` on deletion prompts unless `firestore.indexes.json` has been reviewed as complete.
- Local admin panel is single-password and localhost-only; it is not a hosted multi-moderator product.
- Firestore indexes in `firestore.indexes.json` must be reviewed and deployed manually before production-scale panel use.
- Before switching back to callable-only moderation, deploy Functions first, migrate Android report submission to `submitReport`, then deploy stricter report-denying Rules.
- Functions emulator tests use local Node 22 while the deploy runtime target is Node 18; this is acceptable for local validation but production deploy should use a compatible Firebase CLI/runtime setup.
- Account deletion is prepared through Android request creation plus a local Admin SDK completion panel, but it must not be considered production-complete until Rules are deployed manually and a dedicated non-production account deletion test has passed.
- Account deletion admin pages require `accountDeletionRequests(status, requestedAt desc)` and `accountDeletionActions(requestId, createdAt desc)` Firestore composite indexes. Missing indexes are logged server-side; deploy indexes manually and answer `No` to remote index deletion prompts.

Priority:
High

-------------------------

## Legal / Store Readiness

- Legal, KVKK, Terms, Community Guidelines, account deletion page and Play Data Safety worksheet drafts exist under `docs/legal/`, but they are not final legal approval.
- Developer identity, address, privacy/support emails, country, public website domain, account deletion URL, policy effective date and policy version are still placeholders.
- Public legal pages are not deployed yet; Google Play URLs must not point to broken or private pages.
- Google Play Data Safety answers must be reviewed again if analytics, Crashlytics, ads, billing, push notifications, uploads/media or other SDKs are added.

Priority:
High
