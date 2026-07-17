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

- Android report submission still needs to be migrated from direct Firestore writes to the new `submitReport` callable before report-denying Firestore Rules are deployed.
- Functions emulator tests use local Node 22 while the deploy runtime target is Node 18; this is acceptable for local validation but production deploy should use a compatible Firebase CLI/runtime setup.

Priority:
High
