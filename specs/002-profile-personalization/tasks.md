# Tasks: Profile Personalization

**Input**: Design documents from `/specs/002-profile-personalization/`

**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, quickstart.md ✓

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Android module root: `app/src/main/java/com/caiana/talks/`
- Test root: `app/src/test/java/com/caiana/talks/`

---

## Phase 1: Setup

**Purpose**: Establish the `domain/model/` package structure (new in this feature)

- [ ] T001 Create `ProfilePreferences` and `VoicePreference` data classes in `app/src/main/java/com/caiana/talks/domain/model/ProfilePreferences.kt` to establish the `domain/model/` package

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain enumerations, entity-domain mapping, and data layer write path — required by all user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T002 [P] Create `LearningGoal` enum with values `TRAVEL`, `BUSINESS`, `CASUAL` and PT-BR display labels in `app/src/main/java/com/caiana/talks/domain/model/LearningGoal.kt`
- [ ] T003 [P] Create `ConversationTheme` enum with 10 predefined values and PT-BR display labels in `app/src/main/java/com/caiana/talks/domain/model/ConversationTheme.kt`
- [ ] T004 [P] Create `VoiceGender` enum with values `FEMININE`, `MASCULINE` and PT-BR display labels in `app/src/main/java/com/caiana/talks/domain/model/VoiceGender.kt`
- [ ] T005 [P] Create `VoiceAccent` enum with values `AMERICAN`, `BRITISH` and PT-BR display labels in `app/src/main/java/com/caiana/talks/domain/model/VoiceAccent.kt`
- [ ] T006 [P] Create `SpeechRate` enum with values `SLOW`, `NORMAL`, `FAST` and PT-BR display labels in `app/src/main/java/com/caiana/talks/domain/model/SpeechRate.kt`
- [ ] T007 [P] Add `@Update suspend fun update(profile: UserProfileEntity)` to `app/src/main/java/com/caiana/talks/data/local/db/UserProfileDao.kt`
- [ ] T008 Add entity-domain extension functions mapping `UserProfileEntity` fields to/from `ProfilePreferences`, `VoicePreference`, and all enums — return safe defaults (`FEMININE`/`AMERICAN`/`NORMAL`/`null` goal/empty themes) for unrecognized DB values — in `app/src/main/java/com/caiana/talks/data/local/db/UserProfileEntityExt.kt`
- [ ] T009 Add `suspend fun updateProfile(profile: UserProfileEntity)` to `UserRepository` interface and implement in `UserRepositoryImpl` calling `dao.update(profile)` in `app/src/main/java/com/caiana/talks/data/repository/UserRepository.kt`

**Checkpoint**: Foundation ready — domain enumerations, entity mapper, DAO write path, and repository write method are complete. User story implementation can begin.

---

## Phase 3: User Story 1 — Definir Metas de Aprendizado (Priority: P1) 🎯 MVP

**Goal**: User can select a learning goal (travel/business/casual) on the profile screen, have it persisted in Room, and the app routes new users through onboarding before showing the home screen.

**Independent Test**: Clear app data → launch → tap profile → verify automatic navigation to profile edit screen (no back button) → select "Viagem" → save → verify navigation to HomeScreen → relaunch app → verify HomeScreen shown directly.

### Implementation for User Story 1

- [ ] T010 [US1] Create `ProfileEditUiState` data class and `ProfileEditViewModel` with `setLearningGoal()`, `savePreferences()`, and `StateFlow<ProfileEditUiState>` loaded from `UserRepository` via `savedStateHandle` profile ID in `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditViewModel.kt`
- [ ] T011 [US1] Add `StartDestination.ProfileSetup(userName: String)` variant to the `StartDestination` sealed class and update `MainViewModel` to emit `ProfileSetup` when active profile has blank `learningGoals` in `app/src/main/java/com/caiana/talks/ui/main/MainViewModel.kt`
- [ ] T012 [US1] Add `profileEdit` composable route and `ProfileSetup` navigation handling (back button hidden when no home in back stack) to `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt`
- [ ] T013 [US1] Create `ProfileEditScreen` composable with learning goal radio-button section (Viagem / Negócios / Conversa casual in PT-BR) and Save button; hide system back button via `BackHandler` when navigating from `ProfileSetup` flow in `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditScreen.kt`

**Checkpoint**: User Story 1 fully functional — onboarding flow works end-to-end; learning goal persists and is correctly recovered after app restart.

---

## Phase 4: User Story 2 — Configurar Temas de Conversa Preferidos (Priority: P2)

**Goal**: User can select zero or more conversation themes from a 10-item list; selections are saved as CSV and displayed correctly on next open.

**Independent Test**: Open profile edit screen → select "Restaurantes" and "Compras" → save → reopen screen → verify both themes appear selected; deselect "Compras" → save → reopen → verify only "Restaurantes" is selected.

### Implementation for User Story 2

- [ ] T014 [US2] Expand `ProfileEditUiState` with `selectedThemes: Set<ConversationTheme>` and add `toggleTheme(theme: ConversationTheme)` to `ProfileEditViewModel` in `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditViewModel.kt`
- [ ] T015 [US2] Add conversation themes multi-select section (10 checkboxes or chips with PT-BR labels, no minimum selection required) to `ProfileEditScreen` in `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditScreen.kt`

**Checkpoint**: User Story 2 functional — themes can be selected, deselected, and persisted; empty selection is accepted.

---

## Phase 5: User Story 3 — Configurar Voz da IA (Priority: P3)

**Goal**: User can configure AI voice gender, accent, and speech rate; first-time users see defaults (FEMININE / AMERICAN / NORMAL) pre-selected; subsequent edits update only changed fields.

**Independent Test**: Open profile edit for a new user → verify defaults pre-selected → change to MASCULINE / BRITISH / FAST → save → reopen → verify all three changes are displayed; change only speech rate to SLOW → save → reopen → verify gender and accent unchanged.

### Implementation for User Story 3

- [ ] T016 [US3] Expand `ProfileEditUiState` with `voiceGender: VoiceGender`, `voiceAccent: VoiceAccent`, `speechRate: SpeechRate` (defaulting to FEMININE/AMERICAN/NORMAL) and add `setVoiceGender()`, `setVoiceAccent()`, `setSpeechRate()` to `ProfileEditViewModel` in `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditViewModel.kt`
- [ ] T017 [US3] Add voice settings section (gender toggle, accent selector, speech rate selector — all labels in PT-BR) to `ProfileEditScreen` in `app/src/main/java/com/caiana/talks/ui/profileedit/ProfileEditScreen.kt`
- [ ] T018 [P] [US3] Add "Editar preferências" button navigating to the `profileEdit` route in `app/src/main/java/com/caiana/talks/ui/settings/SettingsScreen.kt`

**Checkpoint**: All three user stories functional — full profile personalization screen works end-to-end with correct persistence and profile isolation.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: ViewModel unit tests, edge-case hardening, and manual validation

- [ ] T019 [P] Write `ProfileEditViewModelTest` covering: initial state load from entity, `setLearningGoal`, `toggleTheme` (add and remove), `setVoiceGender` / `setVoiceAccent` / `setSpeechRate`, and `savePreferences` setting `isSaved = true` in `app/src/test/java/com/caiana/talks/ui/ProfileEditViewModelTest.kt`
- [ ] T020 Verify corrupted DB recovery: confirm entity-to-domain mapping in `app/src/main/java/com/caiana/talks/data/local/db/UserProfileEntityExt.kt` returns safe defaults when DB strings are unrecognized — update mapping functions if any unsafe `.valueOf()` or unchecked enum lookup is found
- [ ] T021 Run manual validation per `specs/002-profile-personalization/quickstart.md`: onboarding flow, post-onboarding edit via Settings, persistence after full app restart, and preference isolation between two user profiles

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **User Story Phases (3 → 4 → 5)**: All depend on Phase 2; must be completed sequentially since US2 and US3 expand the same `ProfileEditViewModel.kt` and `ProfileEditScreen.kt` files created in US1
- **Polish (Phase 6)**: Depends on all user story phases being complete

### User Story Dependencies

- **User Story 1 (P1)**: Depends on Phase 2. Creates `ProfileEditViewModel.kt` and `ProfileEditScreen.kt` — prerequisite for US2 and US3.
- **User Story 2 (P2)**: Depends on US1 (expands same ViewModel and Screen files)
- **User Story 3 (P3)**: Depends on US2 (expands same ViewModel and Screen files); T018 (`SettingsScreen.kt`) is independent [P]

### Parallel Opportunities

- **Phase 2**: T002, T003, T004, T005, T006, T007 can all run in parallel (separate new files); T008 runs after T002-T006; T009 runs after T007
- **Phase 5**: T018 (`SettingsScreen.kt`) can run in parallel with T016 and T017 (different file)
- **Phase 6**: T019 (new test file) can run in parallel with T020 (different file)

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Step 1 — Run all enum file creations and DAO update in parallel:
Task T002: "Create LearningGoal enum in domain/model/LearningGoal.kt"
Task T003: "Create ConversationTheme enum in domain/model/ConversationTheme.kt"
Task T004: "Create VoiceGender enum in domain/model/VoiceGender.kt"
Task T005: "Create VoiceAccent enum in domain/model/VoiceAccent.kt"
Task T006: "Create SpeechRate enum in domain/model/SpeechRate.kt"
Task T007: "Add @Update to UserProfileDao.kt"

# Step 2 — After T002-T006 complete:
Task T008: "Add entity-domain extension functions in UserProfileEntityExt.kt"

# Step 3 — After T007 completes:
Task T009: "Add updateProfile to UserRepository.kt"
```

## Parallel Example: Phase 5 (User Story 3)

```bash
# T016 and T017 are sequential (T017 depends on T016 adding ViewModel methods)
# T018 is independent and can run alongside either:
Task T018: "Add 'Editar preferências' button to SettingsScreen.kt"
Task T016: "Expand ProfileEditViewModel with voice preference fields and setters"
# Then:
Task T017: "Add voice settings section to ProfileEditScreen.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run quickstart.md onboarding flow
5. Demo — user can configure their learning goal and app routes correctly

### Incremental Delivery

1. Setup + Foundational → domain layer and data write path ready
2. User Story 1 → onboarding + learning goal → Demo (MVP!)
3. User Story 2 → conversation themes → Demo
4. User Story 3 → voice preferences → Demo (full feature complete)
5. Polish → ViewModel tests + edge-case hardening → Ship

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps each task to a specific user story for traceability
- US1 creates `ProfileEditViewModel.kt` and `ProfileEditScreen.kt`; US2 and US3 expand them sequentially
- No Room migration needed — all columns (`learning_goals`, `preferred_themes`, `ai_voice_gender`, `ai_voice_accent`, `ai_speech_rate`) exist from feature 001
- All UI labels and section titles must be in Portuguese Brazilian (FR-008)
- The learning goal field is required before starting a conversation session but is NOT required at profile save time (spec assumption, line 108)
- Unsaved changes are discarded automatically on back navigation (ViewModel destroyed by Navigation Compose) — no extra implementation needed
- Commit after each phase checkpoint to keep the branch clean
