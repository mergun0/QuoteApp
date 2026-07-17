# Quote Visibility Migration

Status: production backfill has been reported complete. Do not mark the full migration complete until index deployment, Rules deployment and Android release verification are finished.

## Why this migration is required

The moderation admin panel soft-hides approved reported quotes by writing:

```text
quotes/{quoteId}.isHidden = true
quotes/{quoteId}.hiddenAt
quotes/{quoteId}.hiddenBy
quotes/{quoteId}.hiddenReason
```

Earlier Android code treated a missing `isHidden` field as legacy visible. That preserved old quotes, but broad quote list reads could still include hidden documents if a modified client ignored the official app's client-side filter.

The v1.0 target is stricter:

- every visible quote has `isHidden = false`
- every hidden quote has `isHidden = true`
- normal users can read and list only `isHidden == false` quotes
- Android list queries include `whereEqualTo("isHidden", false)`

## Backfill script

The script is local-only and uses Firebase Admin SDK through the admin panel's existing service-account configuration:

```text
GOOGLE_APPLICATION_CREDENTIALS
FIREBASE_SERVICE_ACCOUNT_PATH
admin-panel/serviceAccountKey.json for local-only fallback
```

It never logs credentials.

Dry run, no writes:

```bash
npm --prefix admin-panel run backfill:quote-visibility
```

Apply, writes only missing fields:

```bash
npm --prefix admin-panel run backfill:quote-visibility -- --apply
```

Verify, fails if any quote is still missing `isHidden`:

```bash
npm --prefix admin-panel run backfill:quote-visibility -- --verify
```

The script is idempotent:

- `isHidden` missing -> optionally updates to `false`
- `isHidden == false` -> leaves unchanged
- `isHidden == true` -> leaves unchanged

It prints:

- scanned
- missing field
- already visible
- already hidden
- updated
- failed

## Required Firestore indexes

Deploy indexes manually after review:

```bash
firebase deploy --project quoteapp-a92e4 --only firestore:indexes
```

If Firebase CLI asks to delete remote indexes that are absent locally, choose `No` unless the local `firestore.indexes.json` has been reviewed and confirmed as the complete source of truth.

New or restored indexes may remain in `Building` before becoming `Enabled`. Android can continue to show a missing-index error until the required index is enabled.

Required composite indexes:

```text
favorites:
  userId ASC
  createdAt DESC

quotes:
  userId ASC
  createdAt DESC

quotes:
  isHidden ASC
  createdAt DESC

quotes:
  userId ASC
  isHidden ASC
  createdAt DESC

achievements:
  isActive ASC
  sortOrder ASC

userAchievements:
  userId ASC
  unlockedAt DESC

reports:
  status ASC
  createdAt DESC

reports:
  reporterUserId ASC
  createdAt ASC

moderationActions:
  actionType ASC
  createdAt DESC
```

Existing category, spoiler and search filters are local filters after a visible quote query, so they do not require additional Firestore indexes right now.

Home depends on:

```text
quotes:
  userId ASC
  isHidden ASC
  createdAt DESC
```

Source query:

```text
QuoteRepository.getCurrentUserQuotes()
quotes
  where userId == currentUser.uid
  where isHidden == false
  orderBy createdAt DESC
```

## Safe production rollout order

1. Commit and back up the current repository state.
2. Run local tests:
   ```bash
   npm run test:rules
   npm --prefix admin-panel test
   .\gradlew.bat assembleDebug
   ```
3. Run dry-run backfill:
   ```bash
   npm --prefix admin-panel run backfill:quote-visibility
   ```
4. Review totals, especially missing, hidden and failed counts.
5. Run apply backfill:
   ```bash
   npm --prefix admin-panel run backfill:quote-visibility -- --apply
   ```
6. Run verification:
   ```bash
   npm --prefix admin-panel run backfill:quote-visibility -- --verify
   ```
7. Deploy indexes:
   ```bash
   firebase deploy --project quoteapp-a92e4 --only firestore:indexes
   ```
8. Wait until the Firebase Console shows every index as enabled.
9. Test the updated Android build against production-compatible data.
10. Deploy tightened Firestore Rules:
    ```bash
    firebase deploy --project <real-project-id> --only firestore:rules
    ```
11. Release the updated Android build.
12. Verify:
    - visible quotes load in Home, Discover, Favorites and profiles
    - hidden quote direct links show `Bu alıntı artık görüntülenemiyor.`
    - hidden quotes cannot be liked, favorited or reported

## Android compatibility risk

The tightened Rules require quote list queries to prove `isHidden == false`. Older installed Android builds that still use broad quote list queries may receive permission-denied errors after Rules deployment.

Recommended staged strategy:

1. Backfill production first.
2. Deploy indexes.
3. Release Android build with visible-only queries.
4. Monitor adoption and critical errors.
5. Deploy tightened Rules after the updated build is available and verified.

This avoids legacy visible quotes disappearing and reduces old-client breakage risk.

## Accidental index deletion incident

A previous `firebase deploy --only firestore:indexes` reported remote indexes missing from the local file. Selecting `Yes` deleted required production indexes for favorites, quotes, achievements, userAchievements and reports.

Those indexes have been restored in `firestore.indexes.json`. Future index deploys must treat the file as the complete source of truth and must not approve remote index deletion prompts without reviewing the diff.

## Rollback considerations

- Backfill is intentionally narrow and writes only `isHidden = false` to documents missing the field.
- Existing `isHidden = true` hidden moderation decisions are never changed by the script.
- If Rules cause unexpected client errors, roll back Firestore Rules to the previous compatible version while investigating.
- Do not delete `isHidden` fields as a rollback.

## Current implementation notes

- New Android quote creation writes `isHidden = false`.
- Normal Android clients cannot create `isHidden = true`.
- Normal Android clients cannot update `isHidden`, `hiddenAt`, `hiddenBy` or `hiddenReason`.
- Admin SDK moderation remains unaffected by Firestore Rules.
