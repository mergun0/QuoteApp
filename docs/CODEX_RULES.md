# CODEX_RULES.md

## Project Overview

QuoteApp is a Java + XML Android application for sharing quotes from movies, TV series, and books.

The project is intended to become:

* A Google Play-ready mobile app
* A portfolio-quality Android project
* A clean MVVM + Firebase-based social quote platform

## Tech Stack

* Java
* XML layouts
* Firebase Authentication
* Cloud Firestore
* MVVM Architecture
* Repository Pattern
* LiveData / ViewModel
* RecyclerView
* Material Components
* Bottom Navigation
* Git / GitHub

## Architecture Rules

### General

* Do not add Jetpack Compose.
* Do not replace Java with Kotlin.
* Keep the existing Java + XML structure.
* Do not rewrite working screens unless explicitly requested.
* Make small, focused changes.
* Do not introduce large refactors without asking.

### MVVM

* Activity and Fragment classes should only manage UI behavior.
* ViewModel classes should manage UI state.
* Repository classes should handle Firebase / Firestore operations.
* Business logic should not be placed directly inside Activity or Fragment when avoidable.

## Screen Responsibilities

### SplashActivity

* Checks Firebase session.
* If user is logged in, opens MainActivity.
* If user is not logged in, opens LoginActivity.

### Auth Screens

* LoginActivity handles login.
* RegisterActivity handles account creation.
* ForgotPasswordActivity handles password reset.
* Firebase Authentication must remain the auth provider.

### MainActivity

* Only manages BottomNavigationView and fragment navigation.
* Must load HomeFragment by default.
* Must not contain quote listing business logic.

### HomeFragment

* Personal quote management area.
* Shows only current user's quotes.
* Allows adding, editing, deleting, searching, filtering, and sharing quotes.
* FAB should stay here.

### DiscoverFragment

* Social discovery feed.
* Shows quotes from all users.
* Must visually differ from HomeFragment.
* Should not show a main “add quote” FAB.
* Other users’ quotes must not show edit/delete actions.

### ProfileFragment

* Current user profile screen.
* Shows current user information and personal stats.
* Handles logout.

### UserProfileActivity

* Public profile screen for any user.
* Opens when clicking a username in Discover or Quote Detail.
* Receives only userId through Intent.
* Loads user data from Firestore.
* Shows that user's quotes.

### QuoteDetailActivity

* Opens when clicking a quote card.
* Receives only quoteId through Intent.
* Loads quote data from Firestore.
* Shows full quote details.
* Edit/delete only visible for the owner.

## Firestore Structure

Current collections:

* users
* quotes

Expected future collections:

* likes
* favorites
* comments
* follows
* notifications

## Firestore Rules Logic

* Authenticated users can create their own content.
* Users can update/delete only their own quotes.
* Discover requires authenticated users to read public quotes.
* Never allow unauthenticated writes.

## Quote Model Guidelines

Quote fields may include:

* quoteId
* userId
* username
* type
* text
* title
* author
* characterName
* season
* episode
* tags
* spoiler
* createdAt
* updatedAt

Do not remove existing fields without checking all usages.

## UI / UX Rules

* Home should feel personal.
* Discover should feel social.
* Empty states must be visible and user-friendly.
* Loading states must not block the UI forever.
* Error states must be shown clearly.
* No permanent debug TextView should appear in UI.
* Debug information should use Log.d only.
* No invisible overlay should block touches.
* Avoid clickable/focusable root overlays unless necessary.

## RecyclerView Rules

* RecyclerView item click should open QuoteDetailActivity.
* Username click should open UserProfileActivity.
* Edit/delete actions should only appear for quote owner.
* Spoiler state must not break because of RecyclerView recycling.
* If using pagination, loading indicators must not block the whole screen.

## Resource Rules

Before finishing any task:

* Check all new string resources exist in strings.xml.
* Check all drawable references exist.
* Check all color references exist.
* Check all layout IDs match Java references.
* Check AndroidManifest entries for new activities.
* Do not leave broken XML references.

## Build Rules

Every task must end with:

* assembleDebug
* XML resource validation
* Clear summary of changed files

Do not claim the task is complete if assembleDebug fails.

## Git Rules

After every stable feature:

* Test manually.
* Run assembleDebug.
* Commit.
* Push.

Commit style:

* feat: add new feature
* fix: resolve bug
* refactor: improve code structure
* docs: update documentation
* style: update UI spacing or visual style
* perf: improve performance

Examples:

* feat: add public user profile screen
* fix: resolve BottomNavigationView theme crash
* feat: implement discover feed with search and filtering
* docs: add project rules for Codex

## Safety Rules for Codex

Before making changes:

1. Read this CODEX_RULES.md file.
2. Inspect relevant existing files.
3. Do not modify unrelated screens.
4. Avoid broad rewrites.
5. Preserve working behavior.
6. Add missing resources immediately.
7. Run assembleDebug.
8. Summarize exactly what changed.

## Current Product Direction

QuoteApp should evolve from a personal quote app into a social quote platform.

Priority roadmap:

1. Quote Detail
2. Public User Profiles
3. Like System
4. Favorites
5. Comments
6. Follow System
7. Trend / Popular Feed
8. Profile Photo with Firebase Storage
9. Dark Mode
10. Firebase Analytics / Crashlytics
11. Google Play Release
