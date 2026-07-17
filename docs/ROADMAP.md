# Satır Arası Roadmap

Satır Arası is a social quote app for discovering, saving, publishing and collecting memorable lines from books, movies and TV series.

## Product loop

```text
Discover quotes → like or save quotes → publish quotes → grow a profile and personal library → return for progress, collections and discovery
```

The first public release must validate this loop before adding restrictive publishing systems, complex economies, mandatory ads or heavy social mechanics.

## Current release: v1.0 — Core Release

Goal: ship a stable Play Store-ready core experience.

Scope:

- authentication
- unique usernames
- quote CRUD
- Home
- Discover
- Favorites
- search
- category filters
- spoiler handling
- likes
- user profiles
- achievements
- XP
- levels
- report submission
- local Node.js admin moderation panel
- Firestore security hardening
- Play Store preparation
- stability
- error handling

Release principle:

Real user behavior must be measured before introducing content limits, quote tickets or virtual economies.

## Next release: v1.1 — Stability and Measurement

Goal: understand real usage and harden the app after the first release.

Planned:

- crash and error monitoring
- analytics events
- performance improvements
- Firestore query and index review
- hidden-content filtering across every feed and detail screen
- moderation workflow improvements
- notification foundation
- accessibility review
- user feedback collection
- retention measurement
- quote-publishing behavior measurement

Metrics to evaluate:

- daily active users
- weekly active users
- quotes published per active user
- percentage of active users publishing quotes
- favorites per user
- likes per quote
- reports per active user
- duplicate or spam report rate
- day-1 retention
- day-7 retention
- day-30 retention
- average sessions per user

## Planned releases

### v1.2 — Avatars and Profile Customization

- free starter avatars
- level-unlocked avatars
- achievement-unlocked avatars
- rewarded-ad avatars
- limited-event avatars
- optional cosmetic unlock paths
- avatar frames
- profile backgrounds
- badge showcase
- profile titles
- pinned quote
- quote-card themes

### v1.3 — Personal Library: Kitaplığım

- books
- movies
- TV series
- reading/watching states
- ratings and notes
- profile library sections
- Home library highlights
- quote-to-library association
- external content-provider research

### v1.4 — Quests and Coin Economy

Status: planned, requires product validation.

- daily quests
- weekly quests
- level-up rewards
- achievement rewards
- voluntary rewarded-ad rewards
- cosmetic-only coin spending
- server-authoritative wallet design

### Experimental — Quote Ticket System

Status: experimental, requires real user data.

Quote tickets are not part of v1.0. Prefer generous invisible anti-spam limits before showing publishing restrictions to normal users.

### v1.5 — Collections and Social Features

- custom quote collections
- private and public collections
- collection covers
- saving favorites into collections
- collection sharing
- pinned profile collections
- improved following experience
- activity summaries
- shareable profile cards
- shareable achievement cards

### v1.6+ — Account Upgrades

- Free
- Go
- Plus
- Pro
- Google Play Billing
- entitlement verification
- subscription management
- cosmetic and convenience benefits

## Long-term direction

Long-term ideas live in `docs/FEATURE_BACKLOG.md`.

Technical future architecture notes live in `docs/FUTURE_ARCHITECTURE.md`.

The first-release checklist lives in `docs/RELEASE_CHECKLIST.md`.
