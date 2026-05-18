# Implementation Plan: MVP User Profile Selection

**Branch**: `001-user-profile-select` | **Date**: 2026-05-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-user-profile-select/spec.md`

## Summary

Allow two fixed users (Caio and Ana) to identify themselves on first app launch via a tap-based selection screen, persisting the choice locally so subsequent launches skip directly to the main screen. A profile switch option in Settings allows reassigning the active user. Implemented as an Android feature using Jetpack Compose, DataStore for the active-user preference, and Room for the full user profile record.

## Technical Context

**Language/Version**: Kotlin 1.9+

**Primary Dependencies**: Jetpack Compose (UI), Jetpack Navigation Compose (routing), Jetpack DataStore Preferences (active user persistence), Room (profile and session storage), Hilt (dependency injection), Kotlin Coroutines + Flow

**Storage**: Room database (UserProfile and Session entities) + DataStore Preferences (active user identity key)

**Testing**: JUnit4, Compose UI Testing, Robolectric (ViewModel unit tests)

**Target Platform**: Android (minimum SDK per project build config; targeting modern devices per constitution)

**Project Type**: Mobile app (Android, Kotlin + Jetpack Compose)

**Performance Goals**: Active-user DataStore read completes before first frame (splash/startup window); profile selection screen renders in under 300ms

**Constraints**: No network calls; all persistence is on-device; UI in Brazilian Portuguese

**Scale/Scope**: 2 fixed users, 1 new screen (ProfileSelection) + Settings entry point

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Voice-First Interface | PASS | Profile selection uses touch input, explicitly permitted for configuration/settings screens. |
| II. Token-Efficient AI | PASS | Zero AI API calls in this feature. |
| III. Pedagogical Effectiveness | PASS | Not applicable — no conversation in this feature. |
| IV. Brazilian Portuguese → English Only | PASS | Selection screen labels, headings, and prompts MUST be in Brazilian Portuguese. |
| V. Dual-Speaker Mode | PASS | Profile selection identifies the device owner for solo sessions; dual-speaker attribution is a session-level concern handled separately. |
| VI. Personalization & Progress Tracking | PASS | This feature is the foundation for Principle VI — it creates the persistent Room-backed UserProfile that all personalization builds on. |

**Post-Phase-1 re-check**: All gates remain green. No violations introduced by design.

## Project Structure

### Documentation (this feature)

```text
specs/001-user-profile-select/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
app/
└── src/
    ├── main/
    │   └── java/com/caiana/talks/
    │       ├── data/
    │       │   ├── local/
    │       │   │   ├── db/
    │       │   │   │   ├── AppDatabase.kt
    │       │   │   │   ├── UserProfileDao.kt
    │       │   │   │   └── UserProfileEntity.kt
    │       │   │   └── preferences/
    │       │   │       └── UserPreferencesDataStore.kt
    │       │   └── repository/
    │       │       └── UserRepository.kt
    │       ├── ui/
    │       │   ├── profileselection/
    │       │   │   ├── ProfileSelectionScreen.kt
    │       │   │   └── ProfileSelectionViewModel.kt
    │       │   └── navigation/
    │       │       └── AppNavGraph.kt
    │       └── MainActivity.kt
    └── test/
        └── java/com/caiana/talks/
            ├── data/
            │   └── UserRepositoryTest.kt
            └── ui/
                └── ProfileSelectionViewModelTest.kt
```

**Structure Decision**: Single Android application module (`app/`). Standard MVVM layering with `data/` (Room + DataStore + Repository) and `ui/` (Compose screens + ViewModels). No separate API or backend module — this feature is entirely on-device per the Constitution's no-cloud constraint.
