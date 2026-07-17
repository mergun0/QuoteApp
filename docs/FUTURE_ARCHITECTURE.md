# Future Architecture

This document captures provisional technical architecture ideas for future Satır Arası releases.

Nothing in this file is approved as a final schema or implementation plan. Do not implement these systems without a dedicated product and security review.

## Principles

- Keep product ideas in `docs/FEATURE_BACKLOG.md`.
- Keep release sequencing in `docs/ROADMAP.md`.
- Treat every collection and model below as provisional.
- Prefer trusted backend verification for rewards, moderation, coins, quests and abuse-sensitive features.
- Do not make client-only writes authoritative for money-like, reward-like or moderation-sensitive state.

## Trusted backend direction

Future systems that affect rewards, restrictions, moderation state or virtual currency should move through Cloud Functions or another trusted backend.

Backend responsibilities may include:

- validating rewarded-ad reward callbacks
- granting avatar unlocks
- granting coins
- writing immutable coin transactions
- evaluating quests
- granting quote tickets if the experimental ticket system is approved
- processing moderation reviews
- updating moderation counters
- sending notifications
- protecting admin-only workflows with moderator custom claims

## Moderation migration

Current v1.0 direction allows direct Firestore report submission with a local Node.js admin panel.

Future migration path:

1. keep Android report submission stable for v1.0
2. deploy callable moderation backend when ready
3. move report creation and daily-limit enforcement behind trusted functions
4. update Android to call the trusted backend
5. tighten Firestore Rules so clients cannot directly write moderation-sensitive data
6. keep local admin tooling or replace it with a hosted admin panel only after role and audit requirements are clear

## Provisional future collections

### Avatar catalog

Possible collections:

- `avatarCatalog/{avatarId}`
- `userAvatarUnlocks/{userId}_{avatarId}`
- `rewardTransactions/{transactionId}`

Possible responsibilities:

- list free, level-unlocked, achievement-unlocked, rewarded and event avatars
- store which avatars a user has unlocked
- store equipped avatar on the public user profile
- record idempotent reward transactions
- prevent duplicate unlocks after retry or reinstall

Security concerns:

- users must not unlock paid/rewarded avatars directly from the client
- reward callbacks must be verified by a trusted backend
- equipped avatar must reference an unlocked or free avatar

### Profile cosmetics

Possible collections:

- `cosmeticCatalog/{cosmeticId}`
- `userCosmeticUnlocks/{userId}_{cosmeticId}`

Potential cosmetic types:

- avatar frame
- profile background
- badge showcase slot
- profile title
- pinned quote slot
- quote-card theme
- collection cover

Security concerns:

- clients should not grant cosmetic ownership directly
- public profile rendering must tolerate missing or retired assets

### Coin wallet

Possible collections:

- `coinWallets/{userId}`
- `coinTransactions/{transactionId}`

Architecture notes:

- wallet balance should be server-authoritative
- coin transactions should be append-only
- every earning and spending event should have an idempotency key
- direct client balance writes should be denied

Security concerns:

- never use client-only writes for coin balances
- do not allow negative balances
- do not let coins affect moderation, visibility or trust

### Quests

Possible collections:

- `questCatalog/{questId}`
- `userQuestProgress/{userId}_{questId}`
- `questCompletions/{completionId}`

Architecture notes:

- quest definitions should be versioned
- progress should be derived from trusted events where possible
- quest rewards should be granted through an idempotent backend path

Security concerns:

- quests must not reward spammy quote publishing
- completion should not rely only on client assertions

### Experimental quote tickets

Possible collections:

- `quoteTicketWallets/{userId}`
- `quoteTicketTransactions/{transactionId}`

Decision state:

- not part of v1.0
- experimental only after real publishing metrics are reviewed
- prefer invisible anti-spam controls before visible publishing restrictions

Security concerns:

- tickets behave like a quota or virtual currency and must be trusted-backend controlled
- ticket consumption must be idempotent with quote creation

### Personal library

Possible collections:

- `libraryItems/{libraryItemId}`
- `userLibraryStats/{userId}`

Possible item fields:

- userId
- contentType
- title
- creator
- state
- rating
- notes
- startedAt
- completedAt
- favorite
- createdAt
- updatedAt

Security concerns:

- private notes and reading/watching states should be readable only by the owner unless a public sharing model is explicitly added
- external API identifiers must not be trusted as ownership or moderation signals

### Collections

Possible collections:

- `quoteCollections/{collectionId}`
- `collectionItems/{collectionId}_{quoteId}`

Architecture notes:

- collections can extend Favorites without replacing them
- collections may be private or public
- public collections may need reporting and moderation later

Security concerns:

- private collections must remain private
- public collection names, descriptions and covers need moderation review paths

### Notifications

Possible collections:

- `notifications/{notificationId}`
- `userNotificationSettings/{userId}`

Architecture notes:

- notification creation should happen on trusted backend events
- Android can read only the current user's notifications
- notification preferences should be owner-only

Security concerns:

- clients should not create arbitrary notifications for other users
- notification payloads should not expose private data

### Moderator roles

Possible structures:

- Firebase custom claims for moderator/admin roles
- `moderationAuditLogs/{auditId}`
- optional `moderatorProfiles/{uid}` for non-sensitive display metadata

Security concerns:

- roles should not be editable by normal users
- moderation actions should be audit logged
- public user data and private auth data must remain separated

### Billing and account upgrades

All billing and account-upgrade notes are provisional. Do not implement a final schema without product, pricing, security and policy review.

Possible concepts:

- `planCatalog/{planId}`
- `subscriptions/{subscriptionId}`
- `userEntitlements/{userId}`
- `purchaseTransactions/{transactionId}`
- `entitlementAuditLogs/{auditId}`

Architecture notes:

- Google Play Billing should be the Android purchase surface.
- Subscription products may support monthly and annual options.
- Purchase tokens must be validated server-side before granting entitlements.
- Entitlements should restore after reinstall and work across devices for the same account.
- Processing must be idempotent so retries do not grant duplicate benefits or coin rewards.
- Renewal state, grace period, account hold, cancellation and expiration must be represented safely.
- Plan upgrades and downgrades must preserve user content.
- Collections above a downgraded limit should become read-only or use another safe policy; they should not be deleted automatically.
- Permanent unlocks and subscription-only access must be modeled separately.
- Entitlement changes should be audit logged.

Security concerns:

- clients should not grant or extend their own paid entitlements
- billing state must not be trusted from the client alone
- paid plans must not grant moderation immunity
- paid plans must not buy likes, followers, fake engagement, Discover boosts or guaranteed visibility
- fraud prevention must cover replayed purchase tokens, duplicate reward processing and entitlement tampering

## Migration notes

- Introduce new systems behind feature flags or versioned rollout paths.
- Backfill scripts should be idempotent and documented.
- Avoid replacing existing documents with broad `set()` calls when field-level updates are safer.
- Keep deterministic document IDs for like, favorite, report and future idempotent reward records.
- Keep old clients in mind when changing public profile or quote document fields.
