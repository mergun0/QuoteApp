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
- Soft-hidden quotes are filtered in Android repositories and direct hidden quote `get` reads are denied for normal users. Full Firestore `list` denial for hidden quotes still requires a production backfill that writes `isHidden = false` to legacy visible quote documents, then client queries can safely require `whereEqualTo("isHidden", false)`.
- Local admin panel is single-password and localhost-only; it is not a hosted multi-moderator product.
- Firestore indexes in `firestore.indexes.json` must be reviewed and deployed manually before production-scale panel use.
- Before switching back to callable-only moderation, deploy Functions first, migrate Android report submission to `submitReport`, then deploy stricter report-denying Rules.
- Functions emulator tests use local Node 22 while the deploy runtime target is Node 18; this is acceptable for local validation but production deploy should use a compatible Firebase CLI/runtime setup.

Priority:
High
