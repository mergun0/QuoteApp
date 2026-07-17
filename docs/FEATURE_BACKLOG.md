# Feature Backlog

This is the single source of truth for future Satır Arası product features.

Do not create a separate `PRODUCT_BACKLOG.md`.

## Backlog governance

Every significant feature should use this structure:

- feature name
- target version
- status
- user value
- complexity
- dependencies
- security considerations
- moderation impact
- monetization impact
- analytics required
- acceptance criteria
- implementation notes
- open questions

Allowed statuses:

- `CURRENT`
- `NEXT`
- `PLANNED`
- `EXPERIMENTAL`
- `DEFERRED`
- `NOT_PLANNED`

Implementation details belong in `docs/FUTURE_ARCHITECTURE.md` or a feature-specific technical document. This backlog should describe product value and requirements.

## Product principle

Core loop:

```text
Discover quotes → like or save quotes → publish quotes → grow a profile and personal library → return for progress, collections and discovery
```

The first release must validate this loop. Do not overload v1.0 with restrictive publishing limits, quote tickets, complex coin economies, mandatory ads, premium systems or complicated social features.

Real user behavior must be measured before introducing content limits or virtual economies.

---

## V1.0 — Core Release

### Core app experience

- Target version: v1.0
- Status: `CURRENT`
- User value: users can register, publish quotes, discover quotes, save favorites, like quotes and build a visible profile.
- Complexity: high
- Dependencies: Firebase Auth, Firestore, Java/XML Android app, local admin moderation panel.
- Security considerations: hardened Firestore Rules, deterministic IDs for sensitive interaction documents, public user data review.
- Moderation impact: users can submit reports; local admin panel reviews reports.
- Monetization impact: none.
- Analytics required: baseline activation, publishing and retention metrics should be added in v1.1.
- Acceptance criteria:
  - authentication works
  - unique usernames work
  - quote CRUD works
  - Home, Discover and Favorites work
  - search and filters work
  - spoiler handling works
  - likes work
  - profiles work
  - achievements, XP and levels display safely
  - report submission works
  - local admin panel can approve/reject reports
  - Play Store preparation checklist is reviewed
- Implementation notes:
  - do not add mandatory quote limits or quote tickets to v1.0
  - do not add mandatory ads to v1.0
- Open questions:
  - final public app name
  - launch country/language scope

### Firestore security hardening

- Target version: v1.0
- Status: `CURRENT`
- User value: users can trust that private and moderation-sensitive data is protected.
- Complexity: high
- Dependencies: Firestore Rules tests, schema audit, migrations.
- Security considerations: highest priority.
- Moderation impact: protects reports and moderation collections.
- Monetization impact: none.
- Analytics required: rule-denied operation monitoring later.
- Acceptance criteria:
  - Rules tests pass
  - public user documents do not expose authentication email
  - username reservations are deterministic
  - moderation collections are not readable by normal clients
  - service account files are excluded from Git
- Implementation notes:
  - moderation currently uses no-billing direct pending report creation
  - trusted callable moderation remains future architecture
- Open questions:
  - exact timing for Cloud Functions deployment

---

## V1.1 — Stability and Measurement

### Crash, error and performance monitoring

- Target version: v1.1
- Status: `NEXT`
- User value: a more stable app after the first public release.
- Complexity: medium
- Dependencies: Crashlytics or equivalent monitoring decision.
- Security considerations: do not log sensitive user content unnecessarily.
- Moderation impact: helps detect report or feed crashes.
- Monetization impact: none.
- Analytics required:
  - crash-free users
  - non-fatal errors
  - slow screen loads
  - failed Firestore operations
- Acceptance criteria:
  - crash reports are visible
  - major error states are tracked
  - performance bottlenecks are documented
- Implementation notes:
  - avoid over-instrumenting quote text content
- Open questions:
  - Firebase Crashlytics rollout timing

### Analytics and retention measurement

- Target version: v1.1
- Status: `NEXT`
- User value: product decisions are based on real behavior instead of assumptions.
- Complexity: medium
- Dependencies: analytics provider and event naming plan.
- Security considerations: no sensitive text payloads in analytics.
- Moderation impact: report abuse patterns can be measured at a high level.
- Monetization impact: informs future ads/cosmetics decisions.
- Analytics required:
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
- Acceptance criteria:
  - core events are implemented
  - dashboards can answer activation, engagement and retention questions
- Implementation notes:
  - analytics should validate whether publishing limits are needed
- Open questions:
  - final event taxonomy

### Hidden-content filtering

- Target version: v1.1
- Status: `NEXT`
- User value: moderated content disappears consistently.
- Complexity: medium
- Dependencies: moderation soft-hide fields.
- Security considerations: hidden content should not appear in feeds or detail views for normal users.
- Moderation impact: high.
- Monetization impact: none.
- Analytics required:
  - hidden quote count
  - attempts to open hidden content
- Acceptance criteria:
  - hidden quotes do not appear in Home
  - hidden quotes do not appear in Discover
  - hidden quotes do not appear in Favorites
  - hidden quotes do not appear in public profiles
  - detail screen handles hidden quote safely
- Implementation notes:
  - may require Firestore index review
- Open questions:
  - whether owners can still see their hidden quotes

### Notification foundation

- Target version: v1.1
- Status: `NEXT`
- User value: prepares future user re-engagement.
- Complexity: medium
- Dependencies: notification architecture decision.
- Security considerations: notifications must not expose private content.
- Moderation impact: report/review notifications may be useful later.
- Monetization impact: none initially.
- Analytics required:
  - notification opt-in
  - notification open rate
- Acceptance criteria:
  - architecture is documented
  - no spammy push behavior
- Implementation notes:
  - do not implement broad push notifications before privacy review
- Open questions:
  - local-only vs FCM timing

### Accessibility review

- Target version: v1.1
- Status: `NEXT`
- User value: app is usable with larger fonts, TalkBack and narrow screens.
- Complexity: medium
- Dependencies: UI audit.
- Security considerations: none.
- Moderation impact: report flow must remain accessible.
- Monetization impact: none.
- Analytics required: none required.
- Acceptance criteria:
  - critical screens support larger font sizes
  - touch targets are adequate
  - content descriptions exist where needed
- Implementation notes:
  - use existing design system
- Open questions:
  - target accessibility standard

---

## V1.2 — Avatars and Profile Customization

### Avatar system

- Target version: v1.2
- Status: `PLANNED`
- User value: users can express identity beyond username and stats.
- Complexity: high
- Dependencies: avatar catalog, user unlock records, equipped avatar field.
- Security considerations: unlock ownership must be server-verifiable before paid/rewarded flows.
- Moderation impact: avatar assets must be safe and non-offensive.
- Monetization impact: future cosmetic monetization possible.
- Analytics required:
  - avatar equip rate
  - avatar unlock source
  - rewarded avatar completion rate
- Acceptance criteria:
  - users always have free starter avatars
  - equipped avatar persists across reinstall/login
  - locked avatars cannot be equipped
  - catalog can distinguish free, level, achievement, rewarded and event avatars
- Implementation notes:
  - no external asset marketplace required for initial version
  - reward claims must be idempotent
- Open questions:
  - static drawable assets vs remote catalog

Avatar categories:

- free starter avatars
- level-unlocked avatars
- achievement-unlocked avatars
- rewarded-ad avatars
- limited-event avatars
- optional coin-unlocked avatars

Rewarded avatar behavior:

- each rewarded avatar may require one voluntarily started rewarded ad
- ads must never start automatically
- users must always have free avatar choices
- UI must clearly state the reward before the ad starts
- avatar unlock must happen only after a successful reward callback
- unlocked avatars must be stored under the user account
- reward claims must be idempotent
- retries must not grant duplicate unlocks
- deleting and reinstalling the app must not remove unlocked avatars

### Profile cosmetics

- Target version: v1.2+
- Status: `PLANNED`
- User value: profiles feel collectible and personal.
- Complexity: high
- Dependencies: avatar system, cosmetic catalog.
- Security considerations: paid/rewarded cosmetics must be server-authoritative.
- Moderation impact: cosmetic assets must be reviewed.
- Monetization impact: high future potential if kept cosmetic-only.
- Analytics required:
  - equip rate
  - cosmetic unlock source
  - profile view engagement
- Acceptance criteria:
  - users can preview owned cosmetics
  - locked cosmetics are clearly marked
  - cosmetics do not affect XP, ranking or moderation
- Implementation notes:
  - Plus/cosmetic rewards must not accelerate XP
- Open questions:
  - which cosmetics ship first

Future cosmetics:

- avatar frames
- profile backgrounds
- badge showcase
- profile title
- pinned quote
- quote-card themes

---

## V1.3 — Personal Library: Kitaplığım

### Kitaplığım

- Target version: v1.3
- Status: `PLANNED`
- User value: users can track books, movies and TV series connected to their quote life.
- Complexity: high
- Dependencies: new library models, profile UI, Home modules.
- Security considerations: privacy controls for public/private library items.
- Moderation impact: public notes/covers may require reporting later.
- Monetization impact: future cosmetic/library customization possible.
- Analytics required:
  - library item creation
  - status changes
  - quote-to-library association
  - public/private visibility usage
- Acceptance criteria:
  - users can add books, movies and TV series
  - users can set status per content type
  - users can rate and add personal notes
  - profile can show library sections
  - Home can show currently reading/watching
  - quotes can eventually be associated with library items
- Implementation notes:
  - do not integrate an external content API yet
  - external provider research is a separate technical task
- Open questions:
  - manual entry only vs provider lookup
  - public visibility defaults

Content types:

- books
- movies
- TV series

Book states:

- `WANT_TO_READ`
- `READING`
- `COMPLETED`
- `DROPPED`

Movie states:

- `WANT_TO_WATCH`
- `WATCHED`

TV series states:

- `WANT_TO_WATCH`
- `WATCHING`
- `COMPLETED`
- `DROPPED`

Potential fields:

- title
- content type
- creator
- author
- director
- cover reference
- user rating
- personal note
- start date
- completion date
- status
- privacy
- related quote IDs
- createdAt
- updatedAt

Profile experience:

- Kitaplar
- Filmler
- Diziler
- currently reading
- currently watching
- completed totals
- favorites
- public or private visibility

Home experience:

- “Şu An Okuyorum”
- “Şu An İzliyorum”
- recent personal library activity

---

## V1.4 — Quests and Coin Economy

### Quests

- Target version: v1.4
- Status: `PLANNED — REQUIRES PRODUCT VALIDATION`
- User value: gives users light reasons to return without forcing behavior.
- Complexity: high
- Dependencies: analytics, achievement system, trusted backend for rewards.
- Security considerations: quest completion and rewards must be server-authoritative.
- Moderation impact: quests must not incentivize spam.
- Monetization impact: may connect to cosmetic coins later.
- Analytics required:
  - quest starts
  - quest completions
  - retention effect
  - spam/report correlation
- Acceptance criteria:
  - quests encourage meaningful activity
  - quests do not require spammy publishing
  - rewards are idempotent
- Implementation notes:
  - use real v1.1 metrics before selecting quest types
- Open questions:
  - daily vs weekly first

### Virtual coins

- Target version: v1.4
- Status: `PLANNED — REQUIRES PRODUCT VALIDATION`
- User value: optional cosmetic progression.
- Complexity: very high
- Dependencies: trusted backend, immutable ledger, fraud monitoring.
- Security considerations: highest priority; balance must be server-authoritative.
- Moderation impact: coins must not manipulate visibility or moderation.
- Monetization impact: high future potential, but not purchasable in initial coin release.
- Analytics required:
  - coin earnings
  - coin spending
  - reward claims
  - fraud signals
- Acceptance criteria:
  - no negative balance
  - immutable transaction ledger
  - idempotent reward claims
  - anti-replay protection
  - clear earning/spending history
  - no pay-to-win behavior
- Implementation notes:
  - any future real-money sale requires separate Google Play Billing and policy review
- Open questions:
  - whether coins are needed at all after v1.1 metrics

Coin earning candidates:

- level-up rewards
- achievements
- daily quests
- weekly quests
- meaningful app activity
- rewarded ads
- limited events

Coin spending candidates:

- avatars
- avatar frames
- profile backgrounds
- badge styles
- profile titles
- quote-card themes
- collection covers

Prohibited coin uses:

- buying likes
- buying followers
- artificially boosting quotes
- manipulating Discover ranking
- bypassing moderation
- increasing fake engagement

---

## Experimental — Quote Ticket System

### Quote tickets

- Target version: not scheduled
- Status: `EXPERIMENTAL — REQUIRES REAL USER DATA`
- User value: may reduce spam only if excessive publishing becomes real.
- Complexity: high
- Dependencies: analytics, trusted backend, anti-abuse policy.
- Security considerations: ticket balances and consumption must be server-authoritative.
- Moderation impact: can reduce spam, but can also harm activation.
- Monetization impact: risky; must not feel like paywalling core creation.
- Analytics required:
  - quotes published per active user
  - spam reports per active user
  - publishing frequency distribution
  - activation and retention impact
- Acceptance criteria:
  - real spam or excessive posting is observed
  - publishing metrics justify a limit
  - free allowance covers ordinary users
  - users understand the system
  - abuse cannot be solved with simpler server-side rate limits
  - legitimate new users are not blocked
- Implementation notes:
  - do not recommend a strict ticket-only publication system
  - prefer high invisible anti-spam rate limits first
- Open questions:
  - whether visible tickets are ever worth the UX cost

Potential behavior:

- users receive a generous number of free daily quote publications
- tickets are consumed only after the free allowance
- level-ups may award quote tickets
- achievements may award tickets
- daily or weekly quests may award tickets
- optional rewarded ads may award tickets
- unused tickets may have a balance limit

Primary risk:

Restricting the application’s central content-creation action may reduce activation, publishing and retention.

---

## V1.5 — Collections and Social Features

### Quote collections

- Target version: v1.5
- Status: `PLANNED`
- User value: lets users organize favorite quotes into meaningful sets.
- Complexity: high
- Dependencies: Favorites, profile UI, sharing flow.
- Security considerations: private collections must stay private.
- Moderation impact: public collections may need report/review later.
- Monetization impact: future collection cover cosmetics.
- Analytics required:
  - collection creation
  - quotes added to collections
  - public/private usage
  - collection sharing
- Acceptance criteria:
  - users can create custom collections
  - collections can be private or public
  - favorites can be saved into collections
  - collections can have covers
  - public collections can be shared
- Implementation notes:
  - do not replace Favorites; collections extend Favorites
- Open questions:
  - collection limits

Examples:

- Hayata Dair
- Motivasyon
- Aşk
- Film Replikleri
- Daha Sonra Oku

### Social expansion

- Target version: v1.5+
- Status: `PLANNED`
- User value: makes discovery more personal and shareable.
- Complexity: high
- Dependencies: profiles, collections, moderation improvements.
- Security considerations: abuse and privacy review required.
- Moderation impact: high.
- Monetization impact: indirect.
- Analytics required:
  - follow interactions
  - profile card shares
  - achievement card shares
- Acceptance criteria:
  - following experience is improved
  - activity summaries are understandable
  - shareable cards do not leak private data
- Implementation notes:
  - avoid heavy social graph features before moderation is stronger
- Open questions:
  - whether following should ship before comments

Included ideas:

- improved following experience
- activity summaries
- shareable profile cards
- shareable achievement cards
- pinned profile collections

---

## ACCOUNT UPGRADES — GO / PLUS / PRO

### Account upgrade system

- Target version: v1.6 or later
- Status: `PLANNED — REQUIRES PRODUCT AND PRICING VALIDATION`
- User value: gives highly engaged users optional convenience, personalization and insight upgrades while keeping the core quote experience accessible.
- Complexity: very high
- Dependencies: Google Play Billing, trusted entitlement verification, plan catalog, cosmetic catalog, user entitlement storage, analytics, support process.
- Security considerations: subscription state and entitlement grants must be server-verified; paid plans must not affect ranking, moderation or fake engagement.
- Moderation impact: paid users must remain subject to the same moderation and reporting systems as free users.
- Monetization impact: high future potential, but pricing and packaging must wait for real usage and retention data.
- Analytics required:
  - daily active users
  - monthly active users
  - retention
  - average sessions
  - advertisement engagement
  - avatar usage
  - collection usage
  - personal library usage
  - percentage of users reaching higher levels
  - cosmetic unlock interest
  - rewarded-ad completion rate
  - willingness-to-pay feedback
  - expected support and billing workload
- Acceptance criteria:
  - free plan remains useful and complete enough to retain users
  - paid benefits are clearly explained before purchase
  - paid plans do not manipulate social ranking
  - paid plans do not allow buying likes, followers or Discover placement
  - subscription restoration works after reinstall and across devices
  - cancellation, expiration, grace period and account hold states are handled safely
  - plan downgrade does not delete user content
- Implementation notes:
  - all benefits below are provisional
  - do not invent final prices before validation
  - permanent purchases and subscription-only access must be distinguished
  - unlocked avatars must not disappear immediately after plan expiration unless explicitly documented
  - collections over a downgraded limit should become read-only or follow another safe downgrade policy, not be deleted
  - coin rewards must be idempotent
- Open questions:
  - exact Go, Plus and Pro benefits
  - whether Go is subscription-based or a low-cost one-time tier
  - monthly versus annual billing
  - regional pricing
  - introductory pricing
  - free trial
  - family or shared plan support
  - coin rewards per plan
  - ad behavior per plan
  - which cosmetics are subscription-only
  - what happens to plan-specific cosmetics after cancellation
  - upgrade and downgrade behavior
  - whether Pro early access is appropriate

Product principles:

- Core quote publishing, discovery, liking, saving and profile usage must remain usable on the free plan.
- Paid plans must not manipulate social ranking.
- Paid users must not be able to buy likes, followers or Discover placement.
- Paid plans should focus on convenience, cosmetics, personalization, storage limits and advanced insights.
- The free experience must remain complete enough to retain users.
- Subscription benefits must be clearly explained before purchase.
- Pricing must not be finalized before real usage and retention data exists.

#### Free plan

Permanent free plan. Do not intentionally make it unusable.

Potential Free features:

- standard quote publishing
- Discover
- likes
- favorites
- basic profile
- starter avatars
- standard collections
- basic personal library
- achievements and levels
- standard moderation and reporting
- limited advertising where appropriate

#### Go plan

Purpose: an affordable entry plan for users who want fewer ads and additional customization.

Provisional Go benefits:

- reduced advertising
- additional avatar choices
- additional profile customization
- additional collection allowance
- optional monthly coin reward
- Go profile badge
- additional quote-card themes
- limited premium cosmetic catalog access

#### Plus plan

Purpose: a plan for active users who want an ad-free and more customizable experience.

Provisional Plus benefits:

- no standard advertising
- all Go benefits
- additional avatar frames
- additional profile backgrounds
- expanded collection allowance
- expanded library customization
- advanced personal activity statistics
- larger monthly cosmetic coin reward
- Plus profile badge
- additional annual recap options
- premium quote-card themes

#### Pro plan

Purpose: the highest personalization and insight tier for highly engaged users.

Provisional Pro benefits:

- all Plus benefits
- Pro profile badge
- premium avatar collections
- premium profile frames and backgrounds
- advanced monthly and yearly summaries
- advanced personal library insights
- expanded customization limits
- early access to selected non-critical features
- exclusive cosmetic collections
- increased cosmetic and collection capacity

Pro must not provide moderation immunity, ranking manipulation or social influence advantages.

#### Features that must not be fully paywalled

- account creation
- reading public quotes
- standard quote publishing
- standard likes
- standard favorites
- basic profiles
- reporting harmful content
- account deletion
- data deletion
- community safety features

#### Prohibited paid advantages

- purchased likes
- purchased followers
- artificial Discover boosts
- moderation bypass
- report suppression
- fake engagement
- guaranteed visibility
- unfair XP manipulation
- unlimited spam publishing

#### Interaction with other systems

- Avatars: plans may unlock additional choices or catalog access, but free starter avatars must remain available.
- Profile cosmetics: plans may expand personalization, frames, backgrounds and themes.
- Coins: plans may include idempotent monthly cosmetic coin rewards only after wallet architecture exists.
- Collections: plans may expand collection allowance, but downgrade handling must preserve user content safely.
- Personal library: plans may expand customization or insights, not basic tracking.
- Annual recap: plans may offer additional recap layouts or insights.
- Ads: Go may reduce ads; Plus/Pro may remove standard ads.
- Rewarded ads: may remain optional for Free users; paid plans should not force users to watch rewarded ads.
- Quote tickets: if the experimental ticket system is ever approved, subscriptions must not create unlimited spam publishing.

---

## Gamification and statistics

### Daily streak

- Target version: future
- Status: `PLANNED`
- User value: encourages healthy return habits.
- Complexity: medium
- Dependencies: analytics and trusted progress calculation.
- Security considerations: streaks should not grant sensitive privileges.
- Moderation impact: must not encourage spam quotes.
- Monetization impact: possible future streak-freeze cosmetic/Plus idea.
- Analytics required:
  - streak starts
  - streak length
  - churn after streak loss
- Acceptance criteria:
  - 7/30/100/365 day streaks can be represented
  - streak recovery does not become manipulative
- Implementation notes:
  - meaningful activity should be defined carefully
- Open questions:
  - quote publish vs app open vs save/like as streak activity

### Daily missions

- Target version: future
- Status: `PLANNED`
- User value: light engagement goals.
- Complexity: medium
- Dependencies: quest system.
- Security considerations: rewards must be idempotent.
- Moderation impact: avoid spam incentives.
- Monetization impact: may connect to cosmetics.
- Analytics required:
  - mission completion
  - retention impact
- Acceptance criteria:
  - examples such as share 1 quote, like 5 quotes, save 1 quote can be tested
- Implementation notes:
  - do not ship before v1.1 measurement
- Open questions:
  - XP-only vs cosmetic rewards

### Quote calendar and advanced statistics

- Target version: future
- Status: `PLANNED`
- User value: users see their quote-sharing history and personal patterns.
- Complexity: medium
- Dependencies: historical activity data.
- Security considerations: private activity must not be exposed publicly by default.
- Moderation impact: none direct.
- Monetization impact: possible Plus advanced stats later.
- Analytics required:
  - stats screen visits
  - heatmap engagement
- Acceptance criteria:
  - quote calendar can show active days
  - advanced stats can show likes, averages, favorite category and most popular quote
- Implementation notes:
  - imported from old backlog
- Open questions:
  - which stats should be public

---

## Long-term ideas

### Daily personalized quote

- Target version: long-term
- Status: `PLANNED`
- User value: daily lightweight return reason.
- Complexity: medium
- Dependencies: recommendation logic.
- Security considerations: avoid exposing hidden or inappropriate content.
- Moderation impact: must respect hidden/moderated content.
- Monetization impact: none initially.
- Analytics required: open rate and save/like rate.
- Acceptance criteria: daily recommendation respects category preferences.
- Implementation notes: can start simple before AI.
- Open questions: local algorithm vs backend.

### Reading and watching goals

- Target version: long-term
- Status: `PLANNED`
- User value: supports personal library motivation.
- Complexity: medium
- Dependencies: Kitaplığım.
- Security considerations: privacy controls.
- Moderation impact: low.
- Monetization impact: possible cosmetic rewards.
- Analytics required: goal creation/completion.
- Acceptance criteria: users can set and update goals.
- Implementation notes: do not ship before library system.
- Open questions: annual vs monthly goals.

### Weekly activity summary and annual recap

- Target version: long-term
- Status: `PLANNED`
- User value: makes progress shareable.
- Complexity: high
- Dependencies: analytics, personal library, collections.
- Security considerations: recaps must not leak private content.
- Moderation impact: public cards must avoid hidden content.
- Monetization impact: shareable growth loop.
- Analytics required: recap views and shares.
- Acceptance criteria: weekly summary and annual “Satır Arası Özeti” can produce shareable cards.
- Implementation notes: include yearly recap cards.
- Open questions: server-generated images vs client rendering.

### Comments, quote chains and discussion threads

- Target version: long-term
- Status: `DEFERRED — HIGH MODERATION COST`
- User value: deeper discussion around quotes.
- Complexity: very high
- Dependencies: stronger moderation, notification system, report comment flow.
- Security considerations: abuse, harassment and spam handling.
- Moderation impact: very high.
- Monetization impact: none initially.
- Analytics required:
  - comments per quote
  - reports per comment
  - moderation load
- Acceptance criteria:
  - comment reporting exists
  - moderation queue supports comments
  - notification preferences exist
- Implementation notes:
  - existing Quote Detail placeholder remains intentionally future-facing
- Open questions:
  - whether comments are worth moderation cost before community size grows

### AI recommendations and smart search

- Target version: long-term
- Status: `DEFERRED`
- User value: better discovery and organization.
- Complexity: high
- Dependencies: data volume, privacy review, recommendation architecture.
- Security considerations: avoid leaking private favorites or hidden content.
- Moderation impact: recommendations must respect moderation.
- Monetization impact: none initially.
- Analytics required: recommendation CTR and save/like conversion.
- Acceptance criteria: recommendations are useful and safe.
- Implementation notes: imported from old backlog.
- Open questions: local vs backend vs external AI provider.

### Communities

- Target version: long-term
- Status: `DEFERRED`
- User value: group discovery and shared identity.
- Complexity: very high
- Dependencies: moderation, roles, community rules.
- Security considerations: role and abuse handling.
- Moderation impact: very high.
- Monetization impact: possible community cosmetics.
- Analytics required: community joins, posts, reports.
- Acceptance criteria: communities have safe moderation model.
- Implementation notes: imported from old backlog.
- Open questions: whether communities fit the core loop.

### Other long-term ideas

- Target version: long-term
- Status: `PLANNED`
- User value: future expansion after core loop validation.
- Complexity: varies
- Dependencies: varies
- Security considerations: review per feature.
- Moderation impact: review per feature.
- Monetization impact: review per feature.
- Analytics required: define before implementation.
- Acceptance criteria:
  - each idea must be promoted into its own governed backlog item before build
- Implementation notes:
  - limited-time avatar collections
  - seasonal quests
  - quote image generator
  - multi-language support
  - collection customization
  - quote-card themes
  - badge styles
  - profile titles
  - profile backgrounds
  - collection covers
- Open questions:
  - prioritization after v1.1 metrics

---

## Not planned for v1.0

- mandatory quote tickets
- strict ticket-only publishing
- mandatory rewarded ads
- purchasable coins
- Plus subscription
- comments
- complex follow/social graph
- hosted multi-moderator admin panel
- external book/movie/TV API integration
