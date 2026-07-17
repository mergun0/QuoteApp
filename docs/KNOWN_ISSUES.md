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

Priority:
High
