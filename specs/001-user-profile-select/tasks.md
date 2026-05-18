# Tasks: MVP User Profile Selection

**Input**: Design documents from `specs/001-user-profile-select/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | quickstart.md ✅

**Tests**: Written before implementation (TDD) — AAA pattern — must pass before feature is considered done.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[Story]**: User story this task belongs to (US1, US2, US3)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Android project initialization and Gradle configuration

- [x] T001 Create Android project with package `com.caiana.talks` and configure `app/build.gradle.kts` with Room, DataStore, Hilt, Navigation Compose, and Coroutines dependencies per quickstart.md
- [x] T002 [P] Create Hilt `CaianaTalksApp.kt` application class annotated with `@HiltAndroidApp` in `app/src/main/java/com/caiana/talks/CaianaTalksApp.kt`
- [x] T003 [P] Create package directory structure per plan.md: `data/local/db/`, `data/local/preferences/`, `data/repository/`, `ui/profileselection/`, `ui/navigation/`, `ui/home/`, `ui/settings/`, `ui/main/`, `di/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Data layer — Room entities, DAO, AppDatabase with seed, DataStore, Repository, and Hilt modules. No user story can begin until this phase is complete.

**⚠️ CRITICAL**: All phases 3–5 depend on this phase being fully complete.

- [x] T004 Create `UserProfileEntity.kt` with all columns from data-model.md (`id`, `name`, `learning_goals`, `preferred_themes`, `ai_voice_gender`, `ai_voice_accent`, `ai_speech_rate`) in `app/src/main/java/com/caiana/talks/data/local/db/UserProfileEntity.kt`
- [x] T005 [P] Create `SessionEntity.kt` placeholder with `id`, `user_profile_id` (FK), `started_at`, `ended_at`, `transcript` columns per data-model.md in `app/src/main/java/com/caiana/talks/data/local/db/SessionEntity.kt`
- [x] T006 Create `UserProfileDao.kt` with `getAll(): Flow<List<UserProfileEntity>>` and `getById(id: Int): Flow<UserProfileEntity?>` queries in `app/src/main/java/com/caiana/talks/data/local/db/UserProfileDao.kt`
- [x] T007 Create `AppDatabase.kt` as a Room database including both entities, version 1, and a `Callback.onCreate()` that seeds two rows (`id=1 Caio`, `id=2 Ana`) with default values per data-model.md in `app/src/main/java/com/caiana/talks/data/local/db/AppDatabase.kt`
- [x] T008 [P] Create `UserPreferencesDataStore.kt` exposing `activeUserId: Flow<Int?>` and `suspend fun setActiveUserId(id: Int)` and `suspend fun clearActiveUserId()` using DataStore Preferences with key `active_user_id` in `app/src/main/java/com/caiana/talks/data/local/preferences/UserPreferencesDataStore.kt`
- [x] T009 Create `UserRepository.kt` combining DataStore and Room: `getActiveUserProfile(): Flow<UserProfileEntity?>`, `selectUser(id: Int)`, `clearActiveUser()`, and corruption-safe fallback (catch read exceptions → treat as null) in `app/src/main/java/com/caiana/talks/data/repository/UserRepository.kt`
- [x] T010 Create Hilt `DatabaseModule.kt` providing singleton `AppDatabase` and `UserProfileDao` in `app/src/main/java/com/caiana/talks/di/DatabaseModule.kt`
- [x] T011 [P] Create Hilt `DataStoreModule.kt` providing singleton `UserPreferencesDataStore` in `app/src/main/java/com/caiana/talks/di/DataStoreModule.kt`

**Checkpoint**: Data layer is fully functional. User story phases can now begin.

---

## Phase 3: User Story 1 — First Launch Profile Selection (Priority: P1) 🎯 MVP

**Goal**: On first launch, show a selection screen with two buttons (Caio, Ana). Tapping one saves the user and navigates to the main screen.

**Independent Test**: Fresh install → open app → verify ProfileSelection screen appears with two name buttons → tap one → verify main screen loads with that user's identity.

- [x] T012 Create `ProfileSelectionViewModel.kt` with `uiState: StateFlow<ProfileSelectionUiState>` (loading / showSelection) and `fun onUserSelected(id: Int)` that calls `UserRepository.selectUser()` in `app/src/main/java/com/caiana/talks/ui/profileselection/ProfileSelectionViewModel.kt`
- [x] T013 Create `ProfileSelectionScreen.kt` Compose screen showing a heading in Brazilian Portuguese ("Quem está usando o app?") and two `Button` composables ("Caio" and "Ana") that call `onUserSelected` from the ViewModel in `app/src/main/java/com/caiana/talks/ui/profileselection/ProfileSelectionScreen.kt`
- [x] T014 Create `AppNavGraph.kt` with `NavHost`, routes `"profileSelection"` and `"home"`, and navigation from ProfileSelection to Home on user selection in `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt`
- [x] T015 Update `MainActivity.kt` to be a `@AndroidEntryPoint` Activity that calls `setContent { AppNavGraph() }` in `app/src/main/java/com/caiana/talks/MainActivity.kt`

**Checkpoint**: US1 fully functional. Fresh-install flow works end to end.

---

## Phase 4: User Story 2 — Returning User Bypasses Selection (Priority: P2)

**Goal**: On subsequent launches, the app reads the saved active user from DataStore and navigates directly to the main screen without showing the selection screen.

**Independent Test**: After completing US1 flow, close and reopen the app — verify ProfileSelection screen is never shown and the correct user identity is present on the main screen.

- [x] T016 Create `MainViewModel.kt` with `startDestination: StateFlow<StartDestination>` (Loading / ProfileSelection / Home) based on DataStore read in `app/src/main/java/com/caiana/talks/ui/main/MainViewModel.kt`
- [x] T017 [P] Create placeholder `HomeScreen.kt` Compose screen displaying the active user's name (e.g., "Olá, Caio!") received as a parameter in `app/src/main/java/com/caiana/talks/ui/home/HomeScreen.kt`
- [x] T018 Update `AppNavGraph.kt` to collect `MainViewModel.startDestination` and render the `NavHost` only after the destination is resolved (no loading flash) in `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt`
- [x] T019 Update `MainActivity.kt` to inject `MainViewModel` via `@HiltViewModel` and pass it to `AppNavGraph` in `app/src/main/java/com/caiana/talks/MainActivity.kt`

**Checkpoint**: US2 fully functional. Returning users skip the selection screen; active user identity persists across restarts.

---

## Phase 5: User Story 3 — Switch Active User (Priority: P3)

**Goal**: Users can switch the active profile from the Settings screen. The switch is persisted and survives app restarts.

**Independent Test**: Open Settings → tap "Trocar perfil" → verify ProfileSelection screen appears → select the other user → close and reopen → verify new user is active.

- [x] T020 Create `SettingsScreen.kt` Compose screen (in Brazilian Portuguese) with a "Trocar perfil" `Button` that invokes a passed-in `onSwitchProfile` callback in `app/src/main/java/com/caiana/talks/ui/settings/SettingsScreen.kt`
- [x] T021 Add `"settings"` route to `AppNavGraph.kt` and a Settings navigation entry point (icon button) on `HomeScreen` that navigates to `"settings"` in `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt`
- [x] T022 Update `ProfileSelectionViewModel.kt` to expose `fun clearAndReselect()` that calls `UserRepository.clearActiveUser()` in `app/src/main/java/com/caiana/talks/ui/profileselection/ProfileSelectionViewModel.kt`
- [x] T023 Wire `SettingsScreen` "Trocar perfil" button in `AppNavGraph.kt`: on tap, call `mainViewModel.clearActiveUser()` which triggers `StartDestination.ProfileSelection` and replaces the NavHost in `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt`

**Checkpoint**: US3 fully functional. All three user stories work independently and together.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Hardening and UX finishing touches that apply across all stories.

- [x] T024 [P] Add Compose `@Preview` annotations to `ProfileSelectionScreen.kt` and `SettingsScreen.kt` in their respective files
- [x] T025 [P] Add a loading state to `AppNavGraph.kt` (`CircularProgressIndicator`) shown while `MainViewModel.startDestination` is Loading in `app/src/main/java/com/caiana/talks/ui/navigation/AppNavGraph.kt`
- [ ] T026 Run all manual test flows from quickstart.md on a physical device or emulator: first-launch, returning-user, profile-switch, and storage-corruption-fallback scenarios

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — **blocks all user story phases**
- **US1 (Phase 3)**: Depends on Phase 2 — can start as soon as Foundational is done
- **US2 (Phase 4)**: Depends on Phase 2 + Phase 3 (reuses `AppNavGraph`, `HomeScreen` stub needed)
- **US3 (Phase 5)**: Depends on Phase 3 + Phase 4 (reuses `ProfileSelectionViewModel`, `AppNavGraph`)
- **Polish (Phase 6)**: Depends on Phases 3–5

### Within Each Phase

- Tasks without `[P]` must be done in listed order
- Tasks marked `[P]` can be done in any order relative to each other (different files)

### Parallel Opportunities

```
Phase 1:  T001 → T002 [P], T003 [P]
Phase 2:  T004 → T005 [P], T006 → T007 → T008 [P], T009 → T010, T011 [P]
Phase 3:  T012 → T013 → T014 → T015
Phase 4:  T016 → T017 [P], T018 → T019
Phase 5:  T020 → T021 → T022 → T023
Phase 6:  T024 [P], T025 [P] → T026
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (**CRITICAL — blocks everything**)
3. Complete Phase 3: US1
4. **STOP and VALIDATE**: Fresh-install → selection screen → tap name → main screen ✅
5. Demo or ship this increment

### Incremental Delivery

1. Setup + Foundational → data layer ready
2. Add US1 → first-launch flow works → **Demo**
3. Add US2 → returning-user flow works → **Demo**
4. Add US3 → profile switch works → **Demo**
5. Polish → production-ready

---

## Notes

- `[P]` = different files, no shared dependencies — safe to parallelize
- `[Story]` label maps each task to a specific user story for traceability
- UI strings must be in Brazilian Portuguese (Constitution Principle IV)
- No network calls anywhere in this feature (Constitution Principle VI)
- Commit after each phase checkpoint at minimum
