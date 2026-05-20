# Tasks: User Stats & Progress Insights

**Input**: Design documents from `/specs/003-user-stats/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/stats-screen.md ✅

**Tests**: Not included — not explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- All paths relative to `app/src/main/java/com/caiana/talks/`

---

## Phase 1: Setup (Domain Models)

**Purpose**: Create the domain model files that the data layer and UI both depend on. All three tasks are independent and can run in parallel.

- [ ] T001 [P] Create `CorrectionCategory` enum (`GRAMMAR`, `VOCABULARY`, `FLUENCY`) with Portuguese `displayLabel` values in `domain/model/CorrectionCategory.kt`
- [ ] T002 [P] Create `CefrLevel` enum (`A1`–`C2`) with `label` and Portuguese `description` per level in `domain/model/CefrLevel.kt`
- [ ] T003 [P] Create `ProgressSnapshot`, `SessionSummary`, and `CorrectionSummary` data classes (all computed, not persisted) in `domain/model/ProgressSnapshot.kt`

**Checkpoint**: Domain model files exist — data layer can reference them without circular dependencies.

---

## Phase 2: Foundational (Data Layer — Blocking Prerequisites)

**Purpose**: Build the complete data layer that ALL user stories depend on. Must be completed before any UI work begins.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T004 [P] Create `CorrectionEntity` with `tableName = "corrections"`, FK to `SessionEntity` (`CASCADE`), and index on `sessionId` in `data/local/db/CorrectionEntity.kt`
- [ ] T005 [P] Create `SessionDao` with `getSessionsForProfile(profileId): Flow<List<SessionEntity>>`, `insertSession`, and `getSessionCount` in `data/local/db/SessionDao.kt`
- [ ] T006 [P] Create `CorrectionDao` with `CategoryCount` data class, `getCorrectionsForSession`, `getCategoryCountsForProfile` (JOIN query grouped by category), and `insertCorrection` in `data/local/db/CorrectionDao.kt`
- [ ] T007 Update `AppDatabase` to version 2 — add `CorrectionEntity::class` to `@Database(entities)`, add `abstract fun sessionDao()` and `abstract fun correctionDao()`, and add `MIGRATION_1_2` (SQL in `data-model.md §Database Migration`) in `data/local/db/AppDatabase.kt`
- [ ] T008 Implement `StatsRepository` interface and `StatsRepositoryImpl` — reactive `combine()` on Sessions + Corrections Flows, CEFR heuristic from `research.md §3`, 7-rule insights engine from `research.md §4`, session list capped at 5 corrections per `SessionSummary` in `data/repository/StatsRepository.kt`
- [ ] T009 Bind `StatsRepositoryImpl` to `StatsRepository` as a Hilt `@Singleton` in `di/AppModule.kt`

**Task order within phase**: T004 + T005 + T006 run in parallel → T007 (requires all three) → T008 (requires T007 + T001–T003) → T009 (requires T008).

**Checkpoint**: Foundation ready — `StatsRepository.getProgressSnapshot(profileId)` returns a live `Flow<ProgressSnapshot>`. User story implementation can now begin.

---

## Phase 3: User Story 1 — View English Level Overview (Priority: P1) 🎯 MVP

**Goal**: The Stats screen exists and shows the active user's estimated CEFR level (or a placeholder when no sessions exist). Navigation from Home works.

**Independent Test**: Navigate Home → "Ver meu progresso" → Stats screen. With no sessions, see the CEFR placeholder. After seeding one session via `quickstart.md §Option 2`, see "A1 — Iniciante" and its description.

- [ ] T010 [P] [US1] Create `StatsViewModel` — inject `StatsRepository` + `UserRepository`, collect `getActiveUserProfile()`, flatMap to `getProgressSnapshot(profileId)`, expose `StateFlow<StatsUiState>` in `ui/stats/StatsViewModel.kt`
- [ ] T011 [P] [US1] Create `StatsScreen` composable — `TopAppBar` with back button, `LazyColumn` body, and CEFR Level card (shows `CefrLevel.label` + `description`, or empty-state `"Conclua uma sessão para ver seu nível estimado."` when null) in `ui/stats/StatsScreen.kt`
- [ ] T012 [US1] Register `"stats"` composable route in `AppNavGraph.kt` — compose `StatsScreen(onNavigateBack = { navController.popBackStack() })` and pass `onNavigateToStats = { navController.navigate("stats") }` to `HomeScreen` in `ui/navigation/AppNavGraph.kt`
- [ ] T013 [US1] Add `onNavigateToStats: () -> Unit` parameter to `HomeScreen` and add a `Button(onClick = onNavigateToStats) { Text("Ver meu progresso") }` below the greeting in `ui/home/HomeScreen.kt`

**Task order within phase**: T010 + T011 run in parallel → T012 (requires T011) + T013 (independent of T012, different file).

**Checkpoint**: User Story 1 fully functional. Stats screen accessible from Home; CEFR level displays correctly.

---

## Phase 4: User Story 2 — View Error Trend by Category (Priority: P2)

**Goal**: The Stats screen shows a breakdown card with Grammar, Vocabulary, and Fluency error counts — including "0 erros" for empty categories (never hidden, per FR-007).

**Independent Test**: Seed two sessions with corrections tagged by category via `quickstart.md §Option 2`. Verify all three category rows show correct counts; remove all corrections and verify all rows still show "0 erros".

- [ ] T014 [US2] Add error breakdown section card to `StatsScreen` — three rows (Gramática, Vocabulário, Fluência) each displaying `${count} erro(s)` from `uiState.grammarErrors`, `vocabularyErrors`, `fluencyErrors`; always visible regardless of counts in `ui/stats/StatsScreen.kt`

**Checkpoint**: User Story 2 functional — error breakdown visible below the CEFR card.

---

## Phase 5: User Story 3 — View Session History with Corrections (Priority: P3)

**Goal**: The Stats screen shows a scrollable session history — each entry with date, duration in `Xmin` / `Xh Ymin` format, and up to 5 highlighted corrections (or "Nenhuma correção registrada." when none). Empty list shows "Nenhuma sessão concluída ainda."

**Independent Test**: Seed two sessions (one with corrections, one without) via `quickstart.md §Option 2`. Verify dates, durations, correction descriptions, and the "no corrections" empty state on the second session.

- [ ] T015 [US3] Add session history section to `StatsScreen` — `LazyColumn` (or sub-list) of session cards from `uiState.sessions`, each showing formatted date (`dd MMM yyyy`), duration (`durationMinutes < 60 → "Xmin"`, `≥ 60 → "Xh Ymin"`), corrections list (max 5), correction empty-state, and overall empty-state when `sessions.isEmpty()` in `ui/stats/StatsScreen.kt`

**Checkpoint**: User Story 3 functional — session history scrollable below the error breakdown card.

---

## Phase 6: User Story 4 — View Actionable Insights (Priority: P4)

**Goal**: The Stats screen shows an insights card with up to 3 auto-generated Portuguese-language insight strings based on correction patterns, or "Conclua mais sessões para desbloquear insights." when insufficient data.

**Independent Test**: Seed two sessions with Grammar corrections > 50% of total. Verify the grammar-focused insight appears. Seed only one session and verify the "more sessions needed" placeholder shows instead.

- [ ] T016 [US4] Add insights section card to `StatsScreen` — iterate `uiState.insights` list, display each string as a bullet item; show `"Conclua mais sessões para desbloquear insights."` when list is empty in `ui/stats/StatsScreen.kt`

**Checkpoint**: All four user stories functional. Stats screen is complete end-to-end.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Loading state, edge-case validation, and emulator sign-off.

- [ ] T017 Add `CircularProgressIndicator` loading state to `StatsScreen` — when `uiState.isLoading == true`, render the indicator centered in the `LazyColumn` body instead of the section cards in `ui/stats/StatsScreen.kt`
- [ ] T018 [P] Validate Stats screen on emulator using `specs/003-user-stats/quickstart.md` — run Options 2 + 3 to confirm: empty state, A1 with 1 session, B1 with 7 sessions / 8 avg errors, long-duration display (≥ 60 min), profile switch refreshes all data, no stale data visible after switch

**Checkpoint**: Feature complete and validated on device.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately. All three tasks parallelizable.
- **Foundational (Phase 2)**: Requires Phase 1 complete. T004/T005/T006 parallel → T007 → T008 → T009.
- **US1 (Phase 3)**: Requires Phase 2 complete. T010/T011 parallel → T012 + T013 (parallel, different files).
- **US2 (Phase 4)**: Requires Phase 3 complete (StatsScreen scaffold and ViewModel must exist).
- **US3 (Phase 5)**: Requires Phase 4 complete (sequential addition to same StatsScreen.kt file).
- **US4 (Phase 6)**: Requires Phase 5 complete (sequential addition to same StatsScreen.kt file).
- **Polish (Phase 7)**: Requires Phase 6 complete.

### User Story Dependencies

- **US1 (P1)**: Depends only on Foundational (Phase 2). MVP scope — stop here for initial release.
- **US2 (P2)**: Depends on US1 (StatsScreen.kt and ViewModel already exist). Single task adding one card.
- **US3 (P3)**: Depends on US2 (sequential edit to same file).
- **US4 (P4)**: Depends on US3 (sequential edit to same file).

### Within Phase 2 (Foundational)

```
T004 ──┐
T005 ──┤──► T007 ──► T008 ──► T009
T006 ──┘              ▲
T001 ─────────────────┘ (domain models needed by StatsRepositoryImpl)
T002 ─────────────────┘
T003 ─────────────────┘
```

### Within Phase 3 (US1)

```
T010 ──┐
       ├──► T012 (nav graph)
T011 ──┘
T013          (independent, different file — update HomeScreen)
```

---

## Parallel Opportunities

### Phase 1 (run all at once)

```
Task: "Create CorrectionCategory enum in domain/model/CorrectionCategory.kt"      [T001]
Task: "Create CefrLevel enum in domain/model/CefrLevel.kt"                        [T002]
Task: "Create ProgressSnapshot models in domain/model/ProgressSnapshot.kt"         [T003]
```

### Phase 2 (first batch)

```
Task: "Create CorrectionEntity in data/local/db/CorrectionEntity.kt"              [T004]
Task: "Create SessionDao in data/local/db/SessionDao.kt"                           [T005]
Task: "Create CorrectionDao in data/local/db/CorrectionDao.kt"                    [T006]
```

### Phase 3 (first batch)

```
Task: "Create StatsViewModel in ui/stats/StatsViewModel.kt"                        [T010]
Task: "Create StatsScreen scaffold + Level card in ui/stats/StatsScreen.kt"        [T011]
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Domain models (T001–T003)
2. Complete Phase 2: Data layer (T004–T009)
3. Complete Phase 3: US1 — CEFR level screen + navigation (T010–T013)
4. **STOP and VALIDATE**: Open Stats screen, verify CEFR card with seed data
5. Ship or demo this increment

### Incremental Delivery

1. Phase 1 + Phase 2 → Data layer ready
2. Phase 3 (US1) → Stats screen reachable from Home with CEFR level ✓
3. Phase 4 (US2) → Error breakdown card added ✓
4. Phase 5 (US3) → Session history scrollable ✓
5. Phase 6 (US4) → Insights card added → full feature ✓
6. Phase 7 → Polish + emulator sign-off ✓

---

## Notes

- **[P]** tasks touch different files with no shared in-flight dependencies — safe to parallelize.
- US2–US4 each modify the same `StatsScreen.kt` file and must be sequential.
- `StatsRepositoryImpl` contains all the algorithmic logic (CEFR heuristic + insights rules). Reference `research.md §3` and `§4` for exact thresholds and rule ordering.
- The `AppDatabase` migration is non-destructive — existing `UserProfileEntity` and `SessionEntity` data is preserved.
- Do not add `fallbackToDestructiveMigration()` to `AppDatabase` — it would wipe user profile data.
- Corrections per `SessionSummary` are capped at 5 in `StatsRepositoryImpl`, not in the DAO.
- All UI empty-state text and section labels must be in Brazilian Portuguese (Constitution Principle IV).
