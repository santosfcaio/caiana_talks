# Tasks: LCARS Theme UI/UX Refactor

**Input**: Design documents from `specs/005-lcars-theme-refactor/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/screens.md ✅

**Tests**: Unit tests are included per project policy (tests must be written and passing before implementation is considered done).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Foundational phase tasks have no story label — they unblock all user stories

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add the font asset and fix the base XML theme so the Compose theme takes full control.

- [X] T001 Add `antonio_regular.ttf` to `app/src/main/res/font/antonio_regular.ttf` (download from Google Fonts: Antonio Regular, SIL OFL)
- [X] T002 Update `app/src/main/res/values/themes.xml` — replace `android:Theme.Material.Light.NoActionBar` parent with `android:Theme.Material.NoActionBar` to force a dark-compatible window

**Checkpoint**: Font asset available; base XML theme is dark-compatible.

---

## Phase 2: Foundational — LCARS Component Library

**Purpose**: Build the complete LCARS design token set and reusable component library. Every user story screen depends on this phase.

**⚠️ CRITICAL**: No screen implementation (US1/US2/US3) can begin until this phase is complete.

### 2A — Design Tokens (fully parallel)

- [X] T003 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/LcarsColors.kt` — `object LcarsColors` with all 9 color tokens as per `data-model.md` Color Tokens section
- [X] T004 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/LcarsTypography.kt` — Material3 `Typography` using `FontFamily(Font(R.font.antonio_regular))` for all slots ≥ 16 sp (displayLarge through titleMedium) and Roboto for slots below 16 sp, with letter spacing per `data-model.md`
- [X] T005 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/LcarsShapes.kt` — `object LcarsShapes` with `pill = CircleShape`, `panelElbow = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)`, `dataPanel = RoundedCornerShape(4.dp)`
- [X] T006 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/LcarsIndication.kt` — `object LcarsIndication : Indication` whose `IndicationInstance` draws a `Color.Black.copy(alpha = 0.3f)` tint over the composable bounds on `PressInteraction.Press` and removes it instantly on `Release`/`Cancel` (no animation; collect from `interactionSource.interactions` in a coroutine launched in `rememberUpdatedInstance`)

### 2B — Theme Wrapper (depends on T003–T006)

- [X] T007 Create `app/src/main/java/com/caiana/talks/ui/theme/LcarsTheme.kt` — `@Composable fun LcarsTheme(content: @Composable () -> Unit)` that calls `MaterialTheme` with the `ColorScheme` built from `LcarsColors` (see `data-model.md` Material3 mapping table), `LcarsTypography`, and `Shapes(extraSmall = LcarsShapes.dataPanel, small = LcarsShapes.dataPanel, medium = LcarsShapes.dataPanel)`; wraps content in `CompositionLocalProvider(LocalIndication provides LcarsIndication)`
- [X] T008 Update `app/src/main/java/com/caiana/talks/MainActivity.kt` — replace `MaterialTheme { Surface { AppNavGraph() } }` with `LcarsTheme { Surface(color = LcarsColors.Black) { AppNavGraph() } }`

### 2C — Reusable Components (parallel; each depends on T003–T005)

- [X] T009 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsButton.kt` — `@Composable fun LcarsButton(onClick, modifier, color, textColor, enabled, fullWidth, content)` using `Button` with `shape = CircleShape`, `colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor, disabledContainerColor = color.copy(alpha = 0.3f))`, and `modifier.fillMaxWidth()` when `fullWidth = true` or `wrapContentWidth()` with `padding(horizontal = 24.dp)` when false
- [X] T010 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsFrame.kt` — `@Composable fun LcarsFrame(accentColor, modifier, topBarHeight, bottomBarHeight, leftStripWidth, content)` using a `Box` with top bar (`Box(Modifier.fillMaxWidth().height(topBarHeight).background(accentColor, RectangleShape))`), bottom bar (`Box(Modifier.fillMaxWidth().height(bottomBarHeight).background(accentColor.copy(alpha = 0.6f)))`), and left strip (`Box(Modifier.width(leftStripWidth).fillMaxHeight().background(accentColor, LcarsShapes.panelElbow))`) arranged via `Column`/`Row` so content area is to the right of the strip and between the bars
- [X] T011 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsTopBar.kt` — `@Composable fun LcarsTopBar(title, accentColor, modifier, navigationIcon, actions)` as a `Row` of height 56.dp filled with `accentColor`, containing optional `navigationIcon`, `Text(title.uppercase(), style = MaterialTheme.typography.displayMedium, color = LcarsColors.Black, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))`, and optional `actions`
- [X] T012 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsDataPanel.kt` — `@Composable fun LcarsDataPanel(accentColor, modifier, content)` using a `Column` with `modifier.background(LcarsColors.Surface, LcarsShapes.dataPanel).border(3.dp, accentColor, LcarsShapes.dataPanel).padding(12.dp)` then calling `content()`
- [X] T013 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsProgressBar.kt` — `@Composable fun LcarsProgressBar(modifier, segmentCount, color)` using `rememberInfiniteTransition` to animate an `activeIndex` from 0 to `segmentCount` at 250 ms per segment; draw `segmentCount` pill-shaped `Box` composables in a `Row` separated by 4 dp gaps, each 16 dp tall; active segment (index ≤ `activeIndex`) uses `color`; inactive uses `color.copy(alpha = 0.2f)`
- [X] T014 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsStatusIndicator.kt` — `@Composable fun LcarsStatusIndicator(label, color, blinking, modifier)` as a `Row` with a `Box` of size 8.dp × 8.dp with `CircleShape` background in `color` (alpha animated 1f → 0.3f on 600 ms loop when `blinking = true`) and a `Text(label, style = MaterialTheme.typography.titleMedium, color = color)` with `Modifier.padding(start = 6.dp)`
- [X] T015 [P] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsOptionPills.kt` — two composables in one file: (1) `@Composable fun <T> LcarsOptionPills(options, selected, onSelect, accentColor, modifier)` using `FlowRow` (or `Row`) with `LcarsButton(fullWidth = false, color = if it == selected accentColor else Color.Transparent, textColor = if it == selected LcarsColors.Black else accentColor)` with `border(1.dp, accentColor, CircleShape)` on unselected; (2) `@Composable fun LcarsCheckRow(label, checked, onCheckedChange, accentColor, modifier)` as a `Row` with min 48.dp × 48.dp touch target containing a square 16.dp box indicator (filled = checked, outlined = unchecked) and the label text

### 2D — Unit Tests (parallel; test components created in 2A–2C)

- [X] T016 [P] Create `app/src/test/java/com/caiana/talks/ui/theme/LcarsColorsTest.kt` — JUnit tests: verify `LcarsColors.Black` == `Color(0xFF000000)`, each accent color matches its expected hex per `data-model.md`, and that no two tokens share the same value
- [X] T017 [P] Create `app/src/test/java/com/caiana/talks/ui/theme/LcarsProgressBarTest.kt` — JUnit tests for the segment animation step logic (extract segment index calculation to a pure function and test it): verify segment 0 is active at time 0, segment `n` active at time `n * 250 ms`, all segments active at `segmentCount * 250 ms`, loop restarts correctly
- [X] T018 [P] Create `app/src/test/java/com/caiana/talks/ui/theme/LcarsIndicationTest.kt` — JUnit tests for `LcarsIndicationInstance`: create a `MutableInteractionSource`, emit `PressInteraction.Press` → verify `pressed == true`, emit `PressInteraction.Release` → verify `pressed == false`, emit `PressInteraction.Cancel` → verify `pressed == false`; run in `TestCoroutineScope`

**Checkpoint**: Component library complete; all 3 unit tests pass (`.\gradlew testDebugUnitTest`). Screen implementation can begin.

---

## Phase 3: User Story 1 — LCARS Visual Identity on Core Screens (Priority: P1) 🎯 MVP

**Goal**: ProfileSelectionScreen and HomeScreen fully reskinned with LCARS theme.

**Independent Test**: Launch app on emulator; verify ProfileSelectionScreen shows black background + orange LcarsFrame + Beige pill profile buttons; navigate to HomeScreen and verify orange LcarsTopBar + three color-coded LcarsButtons. No backend changes required.

- [X] T019 [P] [US1] Update `app/src/main/java/com/caiana/talks/ui/profileselection/ProfileSelectionScreen.kt` — wrap content in `LcarsFrame(accentColor = LcarsColors.Orange)`; replace `CircularProgressIndicator()` with `LcarsProgressBar(color = LcarsColors.Orange)`; replace `Button` with `LcarsButton(color = LcarsColors.Beige, textColor = LcarsColors.Black)`; update headline `Text` to use `style = MaterialTheme.typography.displayLarge`; remove `Scaffold`
- [X] T020 [P] [US1] Update `app/src/main/java/com/caiana/talks/ui/home/HomeScreen.kt` — remove `Scaffold` + `TopAppBar`; wrap content in `LcarsFrame(accentColor = LcarsColors.Orange)`; add `LcarsTopBar(title = "Caiana Talks", accentColor = LcarsColors.Orange, actions = { settings icon })` at top; replace "Iniciar conversa" `Button` with `LcarsButton(color = LcarsColors.Orange)`; replace "Co-practice" `Button` with `LcarsButton(color = LcarsColors.Blue)`; replace "Ver meu progresso" `Button` with `LcarsButton(color = LcarsColors.Beige, textColor = LcarsColors.Black)`

**Checkpoint**: User Story 1 fully functional. ProfileSelection and Home screens display the LCARS aesthetic without any functional regression.

---

## Phase 4: User Story 2 — LCARS Theme on Profile Configuration Screens (Priority: P2)

**Goal**: ProfileEditScreen and SettingsScreen reskinned with LCARS theme.

**Independent Test**: Navigate to ProfileEdit and Settings screens; verify blue LcarsFrame + option pills for all radio groups + LcarsCheckRow for theme checkboxes; verify all save/navigation interactions remain functional.

- [X] T021 [P] [US2] Update `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditScreen.kt` — remove `Scaffold` + `TopAppBar`; add `LcarsTopBar(title = "Editar perfil", accentColor = LcarsColors.Blue, navigationIcon = { back icon if !hideBack })`; wrap content in `LcarsFrame(accentColor = LcarsColors.Blue)`; replace each `Text` section header (meta, temas, voz) with `LcarsDataPanel(accentColor = LcarsColors.Orange) { Text(header, style = titleLarge, color = LcarsColors.Orange) }`; replace `LearningGoal` `RadioButton` rows with `LcarsOptionPills(options = LearningGoal.entries.map { it to it.displayLabel }, selected = uiState.learningGoal, onSelect = viewModel::setLearningGoal, accentColor = LcarsColors.Blue)`; replace `VoiceGender` + `VoiceAccent` `RadioButton` rows with `LcarsOptionPills(accentColor = LcarsColors.Blue)`; replace `SpeechRate` `RadioButton` rows with `LcarsOptionPills(accentColor = LcarsColors.Purple)`; replace `ConversationTheme` `Checkbox` rows with `LcarsCheckRow` per entry; replace save `Button` with `LcarsButton(color = LcarsColors.Orange)`
- [X] T022 [P] [US2] Update `app/src/main/java/com/caiana/talks/ui/settings/SettingsScreen.kt` — remove `Scaffold` + `TopAppBar`; add `LcarsTopBar(title = "Configurações", accentColor = LcarsColors.Purple, navigationIcon = { back icon })`; wrap content in `LcarsFrame(accentColor = LcarsColors.Purple)`; replace "Conta" `Text` header with `LcarsDataPanel(accentColor = LcarsColors.Orange) { Text("CONTA", ...) }`; replace "Editar preferências" `Button` with `LcarsButton(color = LcarsColors.Orange)`; replace "Trocar perfil" `Button` with `LcarsButton(color = LcarsColors.Blue)`

**Checkpoint**: User Stories 1 AND 2 independently functional. All configuration screens display LCARS aesthetic; all save/navigation flows work.

---

## Phase 5: User Story 3 — LCARS Theme on Conversation and Stats Screens (Priority: P3)

**Goal**: ConversationScreen, CoPracticeSetupScreen, SessionSummaryScreen, AudioSpectrum, and StatsScreen fully reskinned.

**Independent Test**: Start a voice session; verify maroon LcarsFrame + blinking LcarsStatusIndicator for LISTENING/THINKING phases + orange waveform bars in AudioSpectrum. Navigate to stats; verify blue LcarsFrame + segmented progress bar + all LcarsDataPanel cards. No need to evaluate AI response quality.

- [X] T023 [P] [US3] Update `app/src/main/java/com/caiana/talks/ui/conversation/ConversationScreen.kt` — remove `Scaffold` + `TopAppBar`; add `LcarsTopBar(title = state.aiPersonaName, accentColor = LcarsColors.Maroon, actions = { close icon in Orange })`; wrap in `LcarsFrame(accentColor = LcarsColors.Maroon)`; replace `TurnCard` (Card) with `LcarsDataPanel(accentColor = LcarsColors.Orange)` containing speaker name in `titleMedium` Antonio + user/AI text in `bodyMedium`/`bodySmall` Roboto; replace phase label `Text` with `LcarsStatusIndicator(label = phaseLabel, color = LcarsColors.Orange, blinking = phase in {LISTENING, THINKING})`; replace Portuguese nudge `Surface` with `LcarsDataPanel(accentColor = LcarsColors.Beige)`; replace error `Text` + retry `Button` with `LcarsDataPanel(accentColor = LcarsColors.Maroon)` + `LcarsButton(color = LcarsColors.Maroon)`; replace end session `Button` with `LcarsButton(color = LcarsColors.Maroon)`
- [X] T024 [P] [US3] Update `app/src/main/java/com/caiana/talks/ui/conversation/AudioSpectrum.kt` — replace hardcoded `Color(0xFF6200EE)` in `drawRect` with `LcarsColors.Orange`
- [X] T025 [P] [US3] Update `app/src/main/java/com/caiana/talks/ui/conversation/CoPracticeSetupScreen.kt` — remove `Scaffold` + `TopAppBar`; add `LcarsTopBar(title = "Co-practice", accentColor = LcarsColors.Blue)`; wrap in `LcarsFrame(accentColor = LcarsColors.Blue)`; replace participant `Card` selection rows with `LcarsButton(fullWidth = true, color = if selected LcarsColors.Purple else Color.Transparent, textColor = if selected LcarsColors.Black else LcarsColors.Blue)` with `border(1.dp, LcarsColors.Blue, CircleShape)` when unselected; replace start `Button` with `LcarsButton(color = LcarsColors.Orange)`; wrap each participant section in `LcarsDataPanel(accentColor = LcarsColors.Blue)`
- [X] T026 [P] [US3] Update `app/src/main/java/com/caiana/talks/ui/conversation/SessionSummaryScreen.kt` — remove `Scaffold` + `TopAppBar`; add `LcarsTopBar(title = "Resumo da sessão", accentColor = LcarsColors.Orange)`; wrap in `LcarsFrame(accentColor = LcarsColors.Orange)`; replace `CircularProgressIndicator()` with `LcarsProgressBar(color = LcarsColors.Orange)` centered; replace `ParticipantSummaryCard` (Card) with `LcarsDataPanel(accentColor = LcarsColors.Beige)` with participant name in `headlineLarge` Antonio; render vocabulary items as `LcarsDataPanel(accentColor = LcarsColors.Blue)` inline; replace "Voltar ao início" `Button` with `LcarsButton(color = LcarsColors.Orange)`
- [X] T027 [P] [US3] Update `app/src/main/java/com/caiana/talks/ui/stats/StatsScreen.kt` — remove `Scaffold` + `TopAppBar`; add `LcarsTopBar(title = "Meu Progresso", accentColor = LcarsColors.Blue, navigationIcon = { back icon })`; wrap in `LcarsFrame(accentColor = LcarsColors.Blue)`; replace `CircularProgressIndicator()` with `LcarsProgressBar(color = LcarsColors.Blue)` centered; replace all four `Card` composables (`CefrLevelCard`, `ErrorBreakdownCard`, `SessionHistoryCard`, `InsightsCard`) with `LcarsDataPanel(accentColor = LcarsColors.Blue)`; render CEFR level in `displayLarge` Antonio colored `LcarsColors.Orange`; render error counts right-aligned in `titleMedium` Antonio; render session date/duration in `titleMedium` Antonio and correction bullets in `bodyMedium` Roboto

**Checkpoint**: All user stories functional. Every screen in the app displays the LCARS aesthetic. Verify no `CircularProgressIndicator`, `TopAppBar`, `Card`, or `RadioButton` remains in any screen file.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Navigation transitions and final clean-up that spans all screens.

- [X] T028 Update `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt` — (a) replace the root `Loading` branch `CircularProgressIndicator()` with `LcarsProgressBar(color = LcarsColors.Orange)` centered on a black `Box`; (b) add `enterTransition`/`exitTransition`/`popEnterTransition`/`popExitTransition` lambdas to the `ProfileSelection` and `ProfileSetup` NavHost instances using `fadeIn(tween(400))` / `fadeOut(tween(400))` (root transitions); (c) add horizontal slide + fade transitions to the `Home` NavHost instance: `enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) }`, `exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) }`, `popEnterTransition = { slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) }`, `popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }`
- [X] T029 Run `.\gradlew testDebugUnitTest` and verify all tests pass (LcarsColorsTest, LcarsProgressBarTest, LcarsIndicationTest, plus all 177 pre-existing tests)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — BLOCKS all user story phases
  - Internal order: 2A (tokens) → 2B (theme + MainActivity) and 2C (components) in parallel → 2D (unit tests)
- **Phase 3 (US1)**: Depends on Phase 2 — T019 and T020 are fully parallel
- **Phase 4 (US2)**: Depends on Phase 2 — T021 and T022 are fully parallel; can run in parallel with Phase 3
- **Phase 5 (US3)**: Depends on Phase 2 — T023–T027 are fully parallel; can run in parallel with Phases 3 and 4
- **Phase 6 (Polish)**: Depends on Phases 3, 4, 5 complete

### User Story Dependencies

- **US1 (P1)**: Depends on Phase 2. Independent — no dependency on US2 or US3.
- **US2 (P2)**: Depends on Phase 2. Independent — no dependency on US1 or US3.
- **US3 (P3)**: Depends on Phase 2. Independent — no dependency on US1 or US2.

### Within Phase 2 (Internal Order)

```
T003 [P] ─┐
T004 [P] ─┤→ T007 → T008
T005 [P] ─┤
T006 [P] ─┘

T003 [P] ─┐
T004 [P] ─┤→ T009–T015 [all parallel with each other]
T005 [P] ─┘

T016, T017, T018 [P] — after their respective implementations complete
```

---

## Parallel Example: Phase 2 (Component Library)

```
# Batch 1 — start all four simultaneously:
T003: LcarsColors.kt
T004: LcarsTypography.kt
T005: LcarsShapes.kt
T006: LcarsIndication.kt

# Batch 2 — start after Batch 1 completes:
T007: LcarsTheme.kt  (sequential — needs T003–T006)

# Batch 3 — start in parallel after Batch 1 (don't wait for T007):
T009: LcarsButton.kt
T010: LcarsFrame.kt
T011: LcarsTopBar.kt
T012: LcarsDataPanel.kt
T013: LcarsProgressBar.kt
T014: LcarsStatusIndicator.kt
T015: LcarsOptionPills.kt

# T008 — after T007 completes:
T008: Update MainActivity.kt

# Batch 4 — after their implementations:
T016: LcarsColorsTest.kt
T017: LcarsProgressBarTest.kt
T018: LcarsIndicationTest.kt
```

## Parallel Example: User Stories (after Phase 2)

```
# US1, US2, US3 can all start in parallel after Phase 2:
T019: ProfileSelectionScreen.kt   (US1)
T020: HomeScreen.kt               (US1)

T021: ProfileEditScreen.kt        (US2)
T022: SettingsScreen.kt           (US2)

T023: ConversationScreen.kt       (US3)
T024: AudioSpectrum.kt            (US3)
T025: CoPracticeSetupScreen.kt    (US3)
T026: SessionSummaryScreen.kt     (US3)
T027: StatsScreen.kt              (US3)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001–T002)
2. Complete Phase 2: Foundational (T003–T018) — **critical blocker**
3. Complete Phase 3: User Story 1 (T019–T020)
4. **STOP and VALIDATE**: Launch emulator, verify ProfileSelection + Home screens match LCARS aesthetic
5. Continue to Phase 4–6 when ready

### Incremental Delivery

1. Phase 1 + Phase 2 → Component library ready
2. Phase 3 (US1) → Core screens done → **Demo: entry point looks like LCARS**
3. Phase 4 (US2) → Config screens done → **Demo: full configuration flow in LCARS**
4. Phase 5 (US3) → Conversation + stats done → **Demo: full app in LCARS**
5. Phase 6 → Navigation transitions wired → **Ship-ready**

---

## Notes

- `[P]` tasks touch different files — safe to implement concurrently
- `[USX]` label maps each task to its user story for traceability
- Run `.\gradlew testDebugUnitTest` after Phase 2 (T029-equivalent) to verify unit tests pass before starting screen work
- Verify on emulator after each user story phase: functional regression is the primary risk
- No ViewModel, repository, domain model, or database file is modified — if a change bleeds outside `ui/`, stop and re-check scope
- After Phase 5, grep for `CircularProgressIndicator`, `TopAppBar`, `Card` (Material3), and `RadioButton` across all screen files to confirm complete replacement
