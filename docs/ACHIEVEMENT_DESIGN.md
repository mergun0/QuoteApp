# ACHIEVEMENT_DESIGN.md

## Goal

QuoteApp will use achievements, XP, and levels to increase user motivation and long-term engagement.

Core idea:

* Achievements = visible accomplishments
* XP = continuous progress
* Level = long-term user status

This system should be flexible, Firebase-driven, and expandable without major code changes.

---

## Main Collections

### achievements

Stores all achievement definitions.

Fields:

```text
achievementId
title
description
category
ruleType
targetScope
metric
operator
targetValue
achievementGroup
tier
xpReward
iconName
level
isActive
sortOrder
createdAt
```

Example:

```text
achievementId: likes_received_10
title: İlk Alkış
description: Toplam 10 beğeniye ulaş.
category: SOCIAL
ruleType: USER_STAT
metric: totalLikesReceived
operator: GREATER_OR_EQUAL
targetValue: 10
achievementGroup: likes_received
tier: 1
xpReward: 50
iconName: ic_badge_like
level: BRONZE
isActive: true
```

---

### userAchievements

Stores achievements unlocked by users.

Fields:

```text
userAchievementId
userId
achievementId
achievementGroup
tier
unlockedAt
progressAtUnlock
xpRewardGranted
```

Suggested document id:

```text
userId_achievementId
```

---

### userStats

Stores calculated user progress.

Fields:

```text
userId
totalXp
level
totalQuotes
totalLikesReceived
maxSingleQuoteLikes
totalMovieQuotes
totalSeriesQuotes
totalBookQuotes
validReports
invalidReports
unlockedAchievementCount
lastUpdatedAt
```

---

### levels

Stores level requirements and future unlocks.

Document id:

```text
level_1
level_2
...
level_100
```

Fields:

```text
level
requiredTotalXp
title
badgeName
unlockedFeatures
createdAt
```

Example:

```text
level: 1
requiredTotalXp: 0
title: Yeni Üye
badgeName: Başlangıç
unlockedFeatures: []
```

```text
level: 50
requiredTotalXp: 250000
title: Usta Alıntıcı
badgeName: Platin
unlockedFeatures: ["advanced_profile_theme"]
```

```text
level: 100
requiredTotalXp: 5000000
title: Efsane
badgeName: Efsanevi
unlockedFeatures: ["legend_profile_frame"]
```

---

## Achievement Categories

```text
SOCIAL
QUOTE
MODERATION
TYPE_MASTER
SPECIAL
```

---

## Rule Types

Initial supported rule types:

```text
USER_STAT
SINGLE_QUOTE_STAT
EVENT_BASED
```

### USER_STAT

For achievements based on user totals.

Examples:

```text
totalLikesReceived >= 100
totalQuotes >= 50
validReports >= 25
```

### SINGLE_QUOTE_STAT

For achievements based on one quote.

Examples:

```text
single quote reaches 100 likes
single quote reaches 1000 likes
```

### EVENT_BASED

For special one-time events.

Examples:

```text
first quote shared
first valid report
first spoiler quote
```

---

## Suggested Achievement Groups

### Likes Received

```text
10 likes: İlk Alkış
50 likes: Dikkat Çeken
100 likes: Sevilen Yazar
500 likes: Topluluk Favorisi
1000 likes: Efsane Alıntıcı
5000 likes: Fenomen Alıntıcı
10000 likes: Topluluğun Sesi
```

### Quotes Shared

```text
1 quote: İlk Satır
10 quotes: Düzenli Paylaşımcı
50 quotes: Arşivci
100 quotes: Üretken Yazar
500 quotes: Alıntı Ustası
1000 quotes: Yaşayan Kütüphane
```

### Single Quote Likes

```text
100 likes on one quote: Parlayan Alıntı
500 likes on one quote: Viral Alıntı
1000 likes on one quote: Efsane Replik
5000 likes on one quote: Kült Alıntı
```

### Moderation

```text
1 valid report: İlk İhbar
5 valid reports: Gözlemci
25 valid reports: Dedektif
50 valid reports: Keskin Göz
100 valid reports: Topluluk Koruyucusu
```

### Type Mastery

```text
10 movie quotes: Sinema Meraklısı
50 movie quotes: Film Arşivcisi

10 series quotes: Bölüm Bölüm
50 series quotes: Dizi Ustası

10 book quotes: Kitap Kurdu
50 book quotes: Sayfa Avcısı
```

---

## XP System

XP should reward meaningful contribution, not spam.

Suggested XP sources:

```text
Share quote: +10 XP
First quote: +25 XP
Receive like: +2 XP
Unlock bronze achievement: +50 XP
Unlock silver achievement: +100 XP
Unlock gold achievement: +250 XP
Valid report: +20 XP
Invalid report: 0 XP or -10 XP
```

---

## XP Anti-Abuse Limits

To prevent spam farming:

```text
Daily quote XP limit: 50 XP
Daily received like XP limit: 100 XP
Daily valid report XP limit: 100 XP
```

Example:

A user may share 100 quotes in one day, but can only earn quote-sharing XP up to the daily limit.

---

## Level System

There will be 100 levels.

Rules:

* Early levels should be easy.
* Mid levels should require consistent activity.
* High levels should be prestigious.
* Level requirements must be stored in Firebase, not hardcoded.

Level should be calculated from `totalXp`.

Example curve:

```text
Level 1: 0 XP
Level 2: 100 XP
Level 3: 250 XP
Level 4: 475 XP
Level 5: 815 XP
Level 10: 5000 XP
Level 25: 50000 XP
Level 50: 250000 XP
Level 75: 1250000 XP
Level 100: 5000000 XP
```

---

## Future Plus Model

Plus should not provide faster XP gain.

Avoid pay-to-win feeling.

Plus may include:

```text
No ads
Extra daily quote sharing limit
Special profile themes
Special profile frames
Advanced statistics
Premium avatar styles
Collection customization
```

XP and levels should represent contribution, not payment.

---

## Moderation Relationship

Achievements and XP can connect to moderation later.

Examples:

```text
Valid report increases validReports
Invalid report increases invalidReports
Valid report may grant XP
Invalid reports may reduce trust
Detective badge depends on validReports
```

Future moderation restrictions:

```text
3 invalid reports: warning
5 invalid reports: 24-hour report restriction
10 invalid reports: 7-day report restriction
20 invalid reports: indefinite restriction pending admin review
```

---

## Implementation Roadmap

### Sprint 7A — Design & Data Models

* Add this design document
* Create Achievement model
* Create UserAchievement model
* Create UserStats model
* Create Level model

### Sprint 7B — Firestore Infrastructure

* AchievementRepository
* UserStatsRepository
* LevelRepository
* AchievementViewModel

### Sprint 7C — Profile UI

* Show current level
* Show XP progress
* Show unlocked achievements
* Show locked achievements

### Sprint 7D — Unlock Engine

* Check achievements after:

  * quote creation
  * like received
  * valid report
* Grant XP
* Update level

### Sprint 7E — Future Expansion

* Notifications for unlocked achievements
* Admin-managed achievements
* Special seasonal achievements
* Plus cosmetic rewards
