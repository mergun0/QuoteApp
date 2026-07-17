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

Priority:
Low

-------------------------

## Security Rollout

- Temporary no-billing v1.0 uses direct Firestore pending report creation instead of deployed Cloud Functions.
- Reliable daily report limits, same-target daily limits, invalid-report streak restrictions and moderation counters require the trusted Functions backend and are deferred.
- Soft-hidden quotes require Android feed/query filtering before content removal is fully production-safe.
- Before switching back to callable-only moderation, deploy Functions first, migrate Android report submission to `submitReport`, then deploy stricter report-denying Rules.
- Functions emulator tests use local Node 22 while the deploy runtime target is Node 18; this is acceptable for local validation but production deploy should use a compatible Firebase CLI/runtime setup.

Priority:
High
