# QuoteApp Design System

> Version: 1.0
> Status: Active

This document defines the visual identity, UI principles and reusable design tokens of QuoteApp.

Every new UI implementation must follow this document before writing code.

---

# Design Philosophy

QuoteApp should feel:

* Modern
* Premium
* Clean
* Minimal
* Social
* Gamified
* Comfortable to read

The interface should never feel crowded.

Whitespace is part of the design.

---

# Visual Identity

Current style:

* Dark-first design
* Soft rounded corners
* Large cards
* High readability
* Subtle animations
* Material 3 inspired
* Premium social experience

---

# Color Palette

## Brand

Primary

* #6C63FF

Primary Dark

* #564FD8

Secondary

* #00C896

Accent

* #FFB800

Success

* #22C55E

Warning

* #F59E0B

Error

* #EF4444

---

## Background

Background

* #0F1117

Surface

* #171A23

Card

* #1E2330

Divider

* #2A3042

---

## Text

Primary

* #FFFFFF

Secondary

* #B5BAC8

Hint

* #8C92A4

Disabled

* #6D7385

---

## Achievement Colors

Bronze

* #CD7F32

Silver

* #C0C0C0

Gold

* #FFD700

Diamond

* #55DDE0

Legendary

* #A855F7

---

# Typography

## Titles

Font

Poppins

Weights

* Bold
* SemiBold

---

## Body

Font

Inter

Weights

* Regular
* Medium

---

## Quote Text

Font

Merriweather

Weight

* Regular

Quote text should always have the highest visual priority.

---

# Corner Radius

Small

8dp

Medium

12dp

Large

20dp

Dialogs

24dp

---

# Spacing System

Only these spacing values should be used:

4dp

8dp

12dp

16dp

24dp

32dp

48dp

---

# Elevation

Cards

4dp

Dialogs

8dp

Floating Button

12dp

---

# Icon Sizes

Small

18dp

Normal

24dp

Large

32dp

---

# Avatar Sizes

Quote Card

40dp

Profile

96dp

Large Profile

128dp

---

# Buttons

Height

48dp

Corner Radius

16dp

Horizontal Padding

20dp

Primary buttons always use the Primary color.

Secondary buttons use Surface colors.

---

# Quote Cards

Every quote card should contain:

* Category badge
* Quote text
* Source
* Username
* Social actions

Card Style

Corner Radius

20dp

Padding

20dp

Elevation

4dp

Cards should always feel spacious.

---

# Dialogs

Dialogs should be:

* Compact
* Simple
* Easy to understand

Corner Radius

24dp

---

# Bottom Sheets

Use BottomSheet instead of Dialog whenever appropriate.

Examples:

* Report Quote
* Filters
* Share Options

---

# Navigation

Primary navigation belongs to BottomNavigation.

Secondary navigation belongs inside screens.

Navigation should always be predictable.

---

# Lists

Lists should always support:

* Loading state
* Empty state
* Error state
* Pagination

Never leave blank screens.

---

# Empty States

Every empty state must explain:

* Why it is empty
* What the user can do next

---

# Error States

Errors should never expose technical information.

Use friendly language.

---

# Animations

Preferred animations:

* Fade
* Scale
* Slide

Animation duration

200ms – 300ms

Avoid excessive motion.

---

# Achievement UX

Achievements should feel rewarding.

Future visual feedback includes:

* Achievement popup
* XP animation
* Level up animation
* Confetti
* Badge glow

---

# Accessibility

Support:

* High contrast
* Large touch targets
* Readable typography

---

# Performance

UI improvements must never reduce performance.

Smooth scrolling is always more important than complex animations.

---

# Screen Order

Whenever a redesign is performed, update screens in this order:

1. Home
2. Discover
3. Quote Detail
4. Favorites
5. Profile
6. User Profile
7. Achievements
8. Notifications
9. Settings

---

# Rules for Codex

Before modifying any UI:

1. Read this document.
2. Keep visual consistency.
3. Reuse components whenever possible.
4. Never introduce a different design language.
5. Prefer reusable layouts over duplicated UI.
6. Follow the defined colors, typography, spacing and radius values.
7. Do not invent new colors or dimensions unless this document is updated first.

The application should always look as if it was designed by a single designer.
