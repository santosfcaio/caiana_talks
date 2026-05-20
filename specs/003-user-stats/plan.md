# Implementation Plan: User Stats & Progress Insights

**Branch**: `003-user-stats` | **Date**: 2026-05-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-user-stats/spec.md`

## Summary

Build a read-only Stats screen that displays the active user's estimated CEFR English level, a per-category error breakdown (Grammar / Vocabulary / Fluency), a scrollable session history with highlighted corrections, and auto-generated actionable insights — all computed locally from Room data with zero AI or network calls. The feature requires a Room migration (v1→v2) to add a new `CorrectionEntity` table and new DAOs, a `StatsRepository` for reactive `ProgressSnapshot` computation, and a `StatsScreen` composable reachable from the Home screen in one tap.

## Technical Context

**Language/Version**: Kotlin (JVM target 11), Android minSdk 26 / compileSdk 34

**Primary Dependencies**: Jetpack Compose, Room 2.x, Hilt, Navigation Compose, kotlinx.coroutines, kotlinx-coroutines-test, MockK 1.13.10, Turbine 1.1.0

**Storage**: Room database (`AppDatabase`, local on-device); migration v1→v2 required to introduce `CorrectionEntity`

**Testing**: JUnit4, MockK, Turbine (Flow testing), kotlinx-coroutines-test (`runTest`)

**Target Platform**: Android API 26+, physical device / Pixel 6 emulator (`CaianaTalks_Pixel6`)

**Project Type**: Android mobile app (Jetpack Compose + MVVM + Hilt)

**Performance Goals**:
- Stats screen renders within 2 seconds of navigation (SC-001)
- Profile switch refreshes all stats data within 1 second (SC-005)

**Constraints**:
- Zero network calls for stats (FR-005) — all data sourced from Room
- Room migration required (v1→v2) to add `corrections` table
- No AI calls introduced — CEFR and insights are fully deterministic and local (Constitution Principle II)
- `CorrectionEntity` does not yet exist in the codebase; `SessionDao` does not yet exist (gap confirmed by codebase exploration)

**Scale/Scope**: Single active profile at a time; all four stat sections visible on standard mobile screen or reachable with one scroll gesture (SC-004); indefinite on-device data retention

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design — no changes.*

| Principle | Status | Justification |
|-----------|--------|---------------|
| I. Voice-First Interface | ✅ PASS | Stats is a read-only display screen; no conversation flow is introduced. Touch navigation is constitutional for non-conversation screens. |
| II. Token-Efficient AI | ✅ PASS | Zero AI/API calls. CEFR estimation and insight generation are fully local, deterministic, and cost-free. |
| III. Pedagogical Effectiveness | ✅ PASS | Error breakdowns and actionable insights directly support targeted practice and reinforce learning improvement. |
| IV. Brazilian Portuguese → English | ✅ PASS | All UI labels and empty-state messages are in Brazilian Portuguese; CEFR shorthand (A1–C2) is displayed as internationally recognized notation. |
| V. Dual-Speaker Mode | ⚠️ DEFERRED | Spec explicitly limits this feature to single-speaker stats. Dual-speaker attribution is acknowledged as a future enhancement. Deferral is documented in spec assumptions — no violation. |
| VI. Personalization & Progress Tracking | ✅ PASS | This feature is the direct implementation of Principle VI's Progress section (CEFR level, error trends, session history, insights, profile-scoped data). |

**Gate result: PASS** — all principles satisfied or explicitly deferred with justification.

## Project Structure

### Documentation (this feature)

```text
specs/003-user-stats/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── stats-screen.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/src/main/java/com/caiana/talks/
├── data/local/db/
│   ├── CorrectionEntity.kt          # NEW: corrections table entity (FK → sessions)
│   ├── SessionDao.kt                # NEW: queries sessions by profile
│   ├── CorrectionDao.kt             # NEW: queries corrections by session and by category
│   └── AppDatabase.kt               # MODIFY: add CorrectionEntity, SessionDao, CorrectionDao, MIGRATION_1_2
├── data/repository/
│   └── StatsRepository.kt           # NEW: interface + impl — computes ProgressSnapshot from Flows
├── domain/model/
│   ├── CorrectionCategory.kt        # NEW: GRAMMAR / VOCABULARY / FLUENCY enum
│   ├── CefrLevel.kt                 # NEW: A1–C2 enum with Portuguese labels and descriptions
│   └── ProgressSnapshot.kt          # NEW: computed aggregate (level, counts, sessions, insights)
├── ui/stats/
│   ├── StatsScreen.kt               # NEW: four-section composable (Level, Errors, History, Insights)
│   └── StatsViewModel.kt            # NEW: Hilt ViewModel — collects ProgressSnapshot StateFlow
├── ui/navigation/
│   └── AppNavGraph.kt               # MODIFY: register "stats" route and composable
├── ui/home/
│   └── HomeScreen.kt                # MODIFY: add "Ver meu progresso" button → stats navigation
└── di/
    └── AppModule.kt                 # MODIFY: bind StatsRepositoryImpl to StatsRepository interface
```

**Structure Decision**: Android single-project layout following existing conventions. The stats feature is a new `ui/stats/` package, parallel to `ui/home/` and `ui/settings/`. Data-layer additions extend existing `data/local/db/` and `data/repository/` packages without restructuring them.

## Complexity Tracking

No Constitution Check violations. No complexity justification required.
