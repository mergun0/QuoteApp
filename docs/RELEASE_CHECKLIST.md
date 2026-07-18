# V1.0 Play Store Release Checklist

This checklist is only for the first Play Store release. Do not add distant future features here.

Do not mark an item complete unless repository evidence, emulator tests, manual QA or release artifacts prove it.

## Core functionality

- [ ] Authentication works.
- [ ] Registration works.
- [ ] Login works with email and password.
- [ ] Password reset uses privacy-safe messaging.
- [ ] Unique username reservation works.
- [ ] Quote creation works.
- [ ] Quote editing works.
- [ ] Quote deletion works.
- [ ] Home screen loads and filters quotes.
- [ ] Discover screen loads and filters community quotes.
- [ ] Favorites screen loads saved quotes.
- [ ] Profile screen loads user data, stats and achievements safely.
- [ ] User public profile loads public data safely.
- [ ] Like and unlike work.
- [ ] Save and unsave work.
- [ ] Report submission works.
- [ ] Achievements and levels display safely.
- [ ] Empty, loading and error states are visible.

## Security

- [ ] Firestore Rules reviewed.
- [ ] Firestore emulator rules tests passed.
- [ ] `usernameLogins` or other email-exposing lookup documents are not accessible.
- [ ] Username uniqueness uses secure reservation rules.
- [ ] Deterministic likes are protected.
- [ ] Deterministic favorites are protected.
- [ ] Public user data does not expose authentication email unnecessarily.
- [ ] Service account files are excluded from Git.
- [ ] Moderation collections are protected from normal users.
- [ ] Report reads are limited to allowed owner/moderator flows.
- [ ] Admin/moderator actions are not available to normal clients.

## Moderation

- [ ] Report submission creates expected Firestore documents.
- [ ] Reports appear in the local admin panel.
- [ ] Approve flow works.
- [ ] Reject flow works.
- [x] Hidden quote flow works.
- [x] Production quote visibility backfill completed and verified.
- [ ] Quote visibility Firestore indexes deployed and enabled.
- [ ] Tightened quote visibility Rules deployed after Android visible-only query release.
- [ ] Moderation audit logs work.
- [ ] Hidden quotes do not appear in Home after production visibility migration.
- [ ] Hidden quotes do not appear in Discover after production visibility migration.
- [ ] Hidden quotes do not appear in Favorites after production visibility migration.
- [ ] Hidden quotes do not appear in UserProfile quote lists after production visibility migration.
- [x] Hidden quote detail behavior is reviewed.
- [ ] Local admin panel credentials and localhost assumptions are documented.

## Legal and account management

- [x] Privacy policy draft is written.
- [ ] Privacy policy is legally reviewed and published at a stable public URL.
- [x] Terms of use draft is written.
- [ ] Terms of use is legally reviewed and published at a stable public URL.
- [x] Community guidelines draft is written.
- [ ] Community guidelines are legally reviewed and published at a stable public URL.
- [x] KVKK information notice draft is written.
- [ ] KVKK legal bases, transfer language and controller details are reviewed.
- [x] Google Play Data Safety worksheet is drafted from repository evidence.
- [ ] Google Play Data Safety form is submitted and matches the final production SDK set.
- [ ] Report and moderation explanation is written.
- [ ] Account deletion flow is defined.
- [ ] Account deletion request flow passes Android QA.
- [ ] Pending account cannot navigate back to Home/Profile/Settings/AddQuote.
- [ ] Pending account logout clears the task and returns to Login.
- [ ] Account deletion Firestore indexes are deployed and enabled.
- [ ] Local admin account deletion completion flow passes dry-run/manual QA.
- [ ] User-data deletion flow is defined.
- [x] Public account deletion page draft is written.
- [ ] Public account deletion page content is legally reviewed and published.
- [ ] Store listing links do not point to broken URLs.
- [ ] Placeholder legal text is replaced before public production release.

## Release preparation

- [ ] Final app name confirmed.
- [ ] Package name confirmed.
- [ ] App icon ready.
- [ ] Adaptive icon ready.
- [ ] Splash branding reviewed.
- [ ] Screenshots prepared.
- [ ] Store description prepared.
- [ ] Short description prepared.
- [ ] Feature graphic prepared.
- [ ] Content rating questionnaire completed.
- [ ] Data safety form completed.
- [ ] Signed release build created.
- [ ] `versionCode` set.
- [ ] `versionName` set.
- [ ] Internal testing completed.
- [ ] Closed testing completed if required.
- [ ] Known issues reviewed.
- [ ] Rollback plan documented.

## Performance and quality

- [ ] `assembleDebug` passes.
- [ ] Release build passes.
- [ ] App opens from a clean install.
- [ ] Existing session opens to Main.
- [ ] Logout returns to Login and clears back stack.
- [ ] No permanent debug UI remains.
- [ ] No blank white screens in main flows.
- [ ] Network failures show friendly errors.
- [ ] Pagination does not block UI.
- [ ] Search fields are focusable and readable.
- [ ] Accessibility pass completed for main screens.
