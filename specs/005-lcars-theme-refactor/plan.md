# Implementation Plan: LCARS Theme UI/UX Refactor

**Branch**: `005-lcars-theme-refactor` | **Date**: 2026-05-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-lcars-theme-refactor/spec.md`

## Summary

Replace every screen's Material3 styling with a custom LCARS theme inspired by Star Trek: The Next Generation. The change is purely visual: black background, canonical LCARS color palette, pill-shaped buttons, structural framing bars, Antonio typeface, LCARS-style loading indicators, and custom press feedback. No ViewModel, repository, domain model, or database code is modified.

## Technical Context

**Language/Version**: Kotlin 2.0.21 | Android compileSdk 34, minSdk 26

**Primary Dependencies**: Jetpack Compose BOM 2024.05.00 (Compose 1.6.x), Material3, Navigation Compose 2.7.7, Hilt 2.51.1

**Storage**: N/A — no new storage

**Testing**: JUnit 4 + MockK + Turbine (existing unit test suite); new unit tests for color token correctness, `LcarsProgressBar` segment animation logic, and `LcarsIndication` press-state transitions

**Target Platform**: Android SDK 26–34, portrait-only, phone form factors (360–393 dp width)

**Project Type**: Android mobile app (Jetpack Compose)

**Performance Goals**: No new performance targets; all new composables must be stateless or use `remember` correctly to avoid unnecessary recomposition

**Constraints**: Portrait-only layout; left strip 16–24 dp (20 dp default); LCARS typeface (Antonio) only for text ≥ 16 sp; max 3 accent colors per screen; no sound effects or complex animations

**Scale/Scope**: 8 screens + 1 audio spectrum component + 1 nav graph + 1 activity; 7 new reusable LCARS components; 1 font asset

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Voice-First Interface | ✅ Pass | No changes to conversation flow or voice APIs |
| II. Token-Efficient AI | ✅ Pass | No AI calls added or modified |
| III. Pedagogical Effectiveness | ✅ Pass | No educational logic modified |
| IV. Brazilian PT → English | ✅ Pass | UI language (pt-BR strings) unchanged |
| V. Dual-Speaker Mode | ✅ Pass | Co-practice UI restyled, logic untouched |
| VI. Personalization & Progress | ✅ Pass | Stats and profile UI restyled, logic untouched |
| VII. LCARS Visual Identity | ✅ Implementing | This feature is the primary implementation of this principle |

**Gate result**: All principles pass. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/005-lcars-theme-refactor/
├── plan.md              # This file
├── research.md          # Phase 0 — font, indication, navigation, color palette
├── data-model.md        # Phase 1 — color/type/shape tokens + component API contracts
├── contracts/
│   └── screens.md       # Phase 1 — per-screen structural and color assignments
└── tasks.md             # Phase 2 — from /speckit-tasks (not yet generated)
```

### Source Code — new files

```text
app/src/main/java/com/caiana/talks/ui/theme/
├── LcarsColors.kt              — canonical color token object
├── LcarsTypography.kt          — Material3 Typography with Antonio (≥16sp) and Roboto (<16sp)
├── LcarsShapes.kt              — shape tokens: pill, panelElbow, dataPanel
├── LcarsTheme.kt               — MaterialTheme wrapper; sets LcarsColors, LcarsTypography, LcarsShapes;
│                                 provides LcarsIndication globally via LocalIndication
├── LcarsIndication.kt          — custom Indication: instant 30% black tint on press, no ripple
└── components/
    ├── LcarsButton.kt           — pill-shaped button; full-width or auto-sized
    ├── LcarsFrame.kt            — structural framing: 20dp left strip + top/bottom bars
    ├── LcarsDataPanel.kt        — content panel with left accent border (replaces Card)
    ├── LcarsProgressBar.kt      — segmented animated horizontal bar (replaces CircularProgressIndicator)
    ├── LcarsStatusIndicator.kt  — blinking dot + label (replaces phase label Text in ConversationScreen)
    ├── LcarsTopBar.kt           — custom title bar with accentColor background (replaces TopAppBar)
    └── LcarsOptionPills.kt      — FlowRow of pills for radio/checkbox groups (LcarsOptionPills + LcarsCheckRow)

app/src/main/res/font/
└── antonio_regular.ttf          — bundled Antonio typeface (SIL Open Font License)
```

### Source Code — modified files

```text
app/src/main/res/values/themes.xml
    — remove Material.Light.NoActionBar parent; set android:Theme.Material.NoActionBar
      (no-op: Compose ignores the XML theme for colors; change ensures edge-to-edge
       and status bar remain black)

app/src/main/java/com/caiana/talks/MainActivity.kt
    — replace MaterialTheme { Surface { ... } } with LcarsTheme { Surface(color = LcarsColors.Black) { ... } }

app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt
    — add enterTransition/exitTransition/popEnterTransition/popExitTransition to all NavHost instances
    — root-level transitions (profile-selection, profile-setup NavHosts): fade-only (400ms)
    — home NavHost: horizontal slide + fade (300ms)

app/src/main/java/com/caiana/talks/ui/profileselection/ProfileSelectionScreen.kt
    — replace CircularProgressIndicator → LcarsProgressBar(Orange)
    — replace Button → LcarsButton(Beige)
    — wrap content in LcarsFrame(Orange)
    — replace MaterialTheme.typography.headlineMedium with displayLarge Antonio

app/src/main/java/com/caiana/talks/ui/home/HomeScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Orange) + LcarsTopBar(Orange)
    — replace Button × 3 → LcarsButton(Orange/Blue/Beige)
    — greeting text updated to displayMedium Antonio

app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Blue) + LcarsTopBar(Blue)
    — replace RadioButton rows → LcarsOptionPills (Blue / Purple per section)
    — replace Checkbox rows → LcarsCheckRow(Blue)
    — replace section header Text → LcarsDataPanel label rows with Orange accent
    — replace Button → LcarsButton(Orange)

app/src/main/java/com/caiana/talks/ui/settings/SettingsScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Purple) + LcarsTopBar(Purple)
    — replace Button × 2 → LcarsButton(Orange) and LcarsButton(Blue)

app/src/main/java/com/caiana/talks/ui/conversation/CoPracticeSetupScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Blue) + LcarsTopBar(Blue)
    — replace profile selection Cards → LcarsButton pills (Purple when selected, outlined when not)
    — replace start Button → LcarsButton(Orange)

app/src/main/java/com/caiana/talks/ui/conversation/ConversationScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Maroon) + LcarsTopBar(Maroon)
    — replace TurnCard (Card) → LcarsDataPanel(Orange)
    — replace phase Text → LcarsStatusIndicator(blinking for LISTENING/THINKING)
    — replace Surface (Portuguese nudge) → LcarsDataPanel(Beige)
    — replace error Text + Button → LcarsDataPanel(Maroon) + LcarsButton(Maroon)
    — replace end session Button → LcarsButton(Maroon)

app/src/main/java/com/caiana/talks/ui/conversation/AudioSpectrum.kt
    — replace hardcoded Color(0xFF6200EE) with LcarsColors.Orange

app/src/main/java/com/caiana/talks/ui/conversation/SessionSummaryScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Orange) + LcarsTopBar(Orange)
    — replace CircularProgressIndicator → LcarsProgressBar(Orange)
    — replace ParticipantSummaryCard (Card) → LcarsDataPanel(Beige)
    — replace vocabulary item Text → LcarsDataPanel(Blue) items
    — replace Button → LcarsButton(Orange)

app/src/main/java/com/caiana/talks/ui/stats/StatsScreen.kt
    — replace Scaffold + TopAppBar → LcarsFrame(Blue) + LcarsTopBar(Blue)
    — replace CircularProgressIndicator → LcarsProgressBar(Blue)
    — replace all Card composables → LcarsDataPanel(Blue)
    — CEFR level display: displayLarge Antonio in Orange
    — error counts: right-aligned titleMedium Antonio
```

### Unit Tests — new files

```text
app/src/test/java/com/caiana/talks/ui/theme/
├── LcarsColorsTest.kt           — verify all token hex values; verify background is #000000
└── LcarsProgressBarTest.kt      — verify segment count, animation step logic (pure logic, no Compose)

app/src/test/java/com/caiana/talks/ui/theme/
└── LcarsIndicationTest.kt       — verify press/release state transitions of LcarsIndicationInstance
```

## Complexity Tracking

No Constitution Check violations. Table omitted.
