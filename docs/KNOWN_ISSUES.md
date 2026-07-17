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
- Hidden quote v1.0 migration is prepared in code, tests and documentation, but production is not complete until `docs/QUOTE_VISIBILITY_MIGRATION.md` is executed: dry-run backfill, apply backfill, verification, index deployment, Rules deployment and Android release verification.
- Local admin panel is single-password and localhost-only; it is not a hosted multi-moderator product.
- Firestore indexes in `firestore.indexes.json` must be reviewed and deployed manually before production-scale panel use.
- Before switching back to callable-only moderation, deploy Functions first, migrate Android report submission to `submitReport`, then deploy stricter report-denying Rules.
- Functions emulator tests use local Node 22 while the deploy runtime target is Node 18; this is acceptable for local validation but production deploy should use a compatible Firebase CLI/runtime setup.

Priority:
High
