# Tasks: Voice Conversation

**Input**: Design documents from `/specs/004-voice-conversation/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ai-conversation.md ✅, contracts/conversation-ui.md ✅, quickstart.md ✅

**Tests**: Included — explicitly requested in the feature specification ("Planeje a criação de muitos testes unitários também") and per project convention (tests MUST pass before the feature is done).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Exact Android source paths used throughout

---

## Phase 1: Setup (Build Config & Dependencies)

**Purpose**: Wire the new build-time dependencies and Android permissions before any code is written.

- [ ] T001 Add OkHttp version entry to `gradle/libs.versions.toml` and add `implementation(libs.okhttp)` to `app/build.gradle.kts`
- [ ] T002 [P] Enable `buildFeatures { buildConfig = true }` and add `ANTHROPIC_API_KEY` buildConfigField reading from `local.properties` in `app/build.gradle.kts`
- [ ] T003 [P] Add `ANTHROPIC_API_KEY=sk-ant-<replace>` placeholder line to `local.properties` (developer one-time setup per quickstart §1)
- [ ] T004 [P] Add `RECORD_AUDIO` runtime and `INTERNET` normal permissions to `app/src/main/AndroidManifest.xml`

---

## Phase 2: Foundational (Domain Models, DB Layer, DI)

**Purpose**: Core entities, DAOs, Room migration, and domain types that MUST exist before any user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T005 [P] Create `SessionMode.kt` (enum: SINGLE | DUAL) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T006 [P] Create `SessionStatus.kt` (enum: ACTIVE | COMPLETED | PARTIAL) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T007 [P] Create `ConversationMessage.kt` (data class with `role: Role` (USER|ASSISTANT) and `text: String`) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T008 [P] Create `AiStreamEvent.kt` (sealed interface: TextDelta, SayEnded, Completed, Failed) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T009 [P] Create `AiResponseMeta.kt` and `DetectedCorrection.kt` (corrections list, vocabulary list, userSpokePortuguese flag) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T010 [P] Create `ConversationError.kt` (enum: MIC_PERMISSION_DENIED, MIC_UNAVAILABLE, NETWORK_UNAVAILABLE, AI_API_ERROR, STORAGE_FULL) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T011 [P] Create `ConversationConfig.kt` (data class: mode, participants, learningGoal, themes, voice, persona, cefrHint) and `ParticipantInfo.kt` (profileId, name) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T012 [P] Create `SessionResult.kt` (Outcome enum: SAVED | DISCARDED_TOO_SHORT; summaries: List<SessionSummary>) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T013 [P] Create `AiPersona.kt` (enum: MICHAEL/DAVID/MARY/PHOEBE with displayName/gender/accent fields and `fun of(gender, accent): AiPersona` total resolver) in `app/src/main/java/com/caiana/talks/domain/model/`
- [ ] T014 Extend `SessionEntity.kt` to add `status TEXT NOT NULL DEFAULT 'completed'`, `mode TEXT NOT NULL DEFAULT 'single'`, `vocabulary TEXT NOT NULL DEFAULT ''`, `co_practice_group_id TEXT` columns in `app/src/main/java/com/caiana/talks/data/local/db/`
- [ ] T015 Extend `SessionDao.kt` to add `updateSession`, `getSessionById`, `getActiveSessions` (status='active'), `getSessionsByGroup` (by co_practice_group_id), `deleteSession` in `app/src/main/java/com/caiana/talks/data/local/db/`
- [ ] T016 [P] Create `ConversationTurnEntity.kt` (id, session_id FK→sessions, speaker_profile_id, turn_index, user_text, ai_text, timestamp; Index on session_id) in `app/src/main/java/com/caiana/talks/data/local/db/`
- [ ] T017 Create `ConversationTurnDao.kt` (`insertTurn`, `getTurnsForSession`, `observeTurnsForSession` as Flow) in `app/src/main/java/com/caiana/talks/data/local/db/`
- [ ] T018 Update `AppDatabase.kt` to version 3: add `ConversationTurnEntity::class` to `@Database(entities=[...])`, define `MIGRATION_2_3` with ALTER TABLE and CREATE TABLE SQL, add `abstract fun conversationTurnDao(): ConversationTurnDao` in `app/src/main/java/com/caiana/talks/data/local/db/`
- [ ] T019 Update `DatabaseModule.kt` to provide `ConversationTurnDao` and pass `MIGRATION_2_3` alongside existing migrations in `app/src/main/java/com/caiana/talks/di/`

**Checkpoint**: Foundation ready — all domain types and DB infrastructure exist; user story implementation can begin.

---

## Phase 3: User Story 1 — Single-Speaker Voice Session (Priority: P1) 🎯 MVP

**Goal**: A user starts a conversation, speaks in English, receives an AI voice response (with corrections), ends the session, and sees their stats updated.

**Independent Test**: Start a session as a single user → speak in English → receive a voiced AI response → end the session after ≥60 s → confirm stats screen shows updated session count and minutes.

### Tests for User Story 1

> **Write these tests first — they MUST fail before implementation begins.**

- [ ] T020 [P] [US1] Create `SentenceChunkerTest.kt` (split on .!?/newline; no split on Mr./Mrs./e.g./i.e./decimals like 3.5; multi-sentence in one delta; sentence split across deltas; flush() remainder; empty input) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T021 [P] [US1] Create `AiResponseParserTest.kt` (well-formed `<say>+<meta>`; missing `<meta>` → empty AiResponseMeta; malformed JSON in meta; unknown `cat` value dropped; empty corrections/vocab arrays; `pt=true`; `<say>` tag not yet closed) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T022 [P] [US1] Create `RollingWindowTest.kt` (fewer than 6 turns kept as-is; exactly 6 kept; more than 6 evicts oldest; result ordered oldest-first; empty list returns empty) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T023 [P] [US1] Create `SystemPromptBuilderTest.kt` (static block byte-identical across different configs; goal/themes encoded in Block B; CEFR omitted when null; persona name present; dual mode includes both participant names; combined token estimate within SC-006 budget of 500) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T024 [P] [US1] Create `AiPersonaTest.kt` (masculine/american→Michael; masculine/british→David; feminine/american→Mary; feminine/british→Phoebe; `of()` is total over all four combinations, no fallback needed) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T025 [P] [US1] Create `VoiceSelectorTest.kt` (AMERICAN→Locale.US; BRITISH→Locale.UK; SLOW→0.8f; NORMAL→1.0f; FAST→1.4f; gender-matched voice picked from candidates; fallback to locale default when no gendered voice found) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T026 [P] [US1] Create `SessionDurationPolicyTest.kt` (59 s → discard; 60 s → keep; 61 s → keep; 0 ms → discard; negative → discard) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T027 [P] [US1] Create `SpectrumWaveformTest.kt` (speaking=false → all bars at constant floor height; speaking=true → every bar in [0f,1f]; bars are non-uniform/differ from each other; same timeMs+barCount → same result deterministically; barCount parameter respected) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T028 [P] [US1] Create `SseEventParserTest.kt` (valid `data:` line parsed; multiple events in a chunk; `[DONE]` sentinel recognised; blank lines ignored; malformed lines ignored without crash) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T029 [US1] Create `ConversationRepositoryTest.kt` — single-mode cases: `startSession` creates 1 active row; `appendTurn` writes `ConversationTurnEntity` + `CorrectionEntity` per correction + appends vocabulary; `finalizeSession` <60 s deletes session+turns and returns DISCARDED_TOO_SHORT; ≥60 s deliberate→status=completed+SAVED; ≥60 s interrupted→status=partial+SAVED; `recoverDanglingSessions` finalizes a dangling active row as partial or discards if <60 s in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T030 [US1] Create `ConversationViewModelTest.kt` — single-mode cases: phase machine IDLE→LISTENING→THINKING→SPEAKING→LISTENING; STT `Final` event triggers AI `streamReply`; TextDelta events feed SentenceChunker→TTS enqueue; completed turn persisted via repository `appendTurn`; `onEndSession` ≥60 s → navigates to sessionSummary; `onEndSession` <60 s → navigates to home; `onAppBackgrounded` → `finalizeSession(PARTIAL)`; `SttEvent.Silence` → re-prompt, stays LISTENING; MIC_PERMISSION_DENIED / NETWORK_UNAVAILABLE / AI_API_ERROR → sets `error` field, no crash; `onRetry` → clears error, resumes LISTENING; `meta.pt=true` → `portugueseNudgeVisible=true` in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T031 [P] [US1] Create `MigrationTest.kt` (v2 DB with a feature-003 session migrates to v3 without row loss; new `sessions` columns have correct defaults; `conversation_turns` table exists and accepts inserts) in `app/src/androidTest/java/com/caiana/talks/`

### Implementation for User Story 1

- [ ] T032 [P] [US1] Create `SentenceChunker.kt` (`accept(delta): List<String>` emits complete sentences; `flush(): List<String>` drains remainder; abbreviation guard for Mr./Mrs./e.g./i.e. and decimal points) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T033 [P] [US1] Create `AiResponseParser.kt` (object: `extractSayText(raw)` extracts `<say>…</say>` content; `parseMeta(raw)` parses `<meta>{…}</meta>` JSON tolerantly — missing/malformed → empty AiResponseMeta, unknown `cat` values dropped) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T034 [P] [US1] Create `RollingWindow.kt` (object: `MAX_TURNS = 6` constant; `take(allTurns): List<ConversationMessage>` returns last MAX_TURNS oldest-first) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T035 [US1] Create `SystemPromptBuilder.kt` (interface + impl: `build(config): SystemPrompt`; Block A static ~150 tokens — English-tutor role, correction directive, `<say>/<meta>` contract; Block B per-session ~60 tokens — persona name, goal, themes, CEFR hint when non-null, dual-mode participant names) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T036 [P] [US1] Create `VoiceSelector.kt` (object: `localeFor(accent): Locale`; `rateFor(rate): Float`; `pickVoice(candidates, gender, locale): VoiceDescriptor?` with locale-default fallback) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T037 [P] [US1] Create `SessionDurationPolicy.kt` (object or pure function: `shouldKeep(durationMs: Long): Boolean` — true iff durationMs ≥ 60_000) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T038 [P] [US1] Create `SpeechRecognizerService.kt` (interface `listen(languageTag): Flow<SttEvent>`/`stop()`; `SttEvent` sealed interface Partial/Final/Silence/Failed; `AndroidSpeechRecognizerService` impl wrapping `android.speech.SpeechRecognizer`) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T039 [P] [US1] Create `TextToSpeechService.kt` (interface `configure(voice)`/`enqueue(sentence)`/`stop()`/`isSpeaking: StateFlow<Boolean>`; `AndroidTextToSpeechService` impl using `VoiceSelector` for locale/voice/rate mapping and `QUEUE_ADD` progressive playback) in `app/src/main/java/com/caiana/talks/data/conversation/`
- [ ] T040 [P] [US1] Create `SseEventParser.kt` (pure parser: processes `data:`/`event:`/`id:` SSE lines into event objects; recognises `[DONE]`; ignores blank/malformed lines) in `app/src/main/java/com/caiana/talks/data/remote/`
- [ ] T041 [P] [US1] Create `ConversationAiClient.kt` (interface `streamReply(system, window, userInput): Flow<AiStreamEvent>`; `SystemPrompt` data class with `staticBlock` and `personalizationBlock`) in `app/src/main/java/com/caiana/talks/data/remote/`
- [ ] T042 [US1] Create `AnthropicConversationAiClient.kt` (OkHttp SSE impl: POST `/v1/messages` with `cache_control` on Block A; model `claude-haiku-4-5`; `max_tokens=400`; `stream=true`; parse SSE via `SseEventParser`; emit TextDelta→SayEnded→Completed or Failed; error mapping per contract) in `app/src/main/java/com/caiana/talks/data/remote/`
- [ ] T043 [US1] Create `ConversationRepository.kt` (interface) and `ConversationRepositoryImpl.kt` (`startSession`/`appendTurn` with turn+corrections+vocabulary writes/`finalizeSession` enforcing 60 s rule/`recoverDanglingSessions` using `SessionDao`, `ConversationTurnDao`, `CorrectionDao`) in `app/src/main/java/com/caiana/talks/data/repository/`
- [ ] T044 [P] [US1] Create `SpectrumWaveform.kt` (pure object: `barHeights(timeMs, barCount, speaking): List<Float>`; organic independent periods per bar; constant floor height when `!speaking`) and `AudioSpectrum.kt` (Canvas composable sampling `SpectrumWaveform` each animation frame) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T045 [US1] Create `ConversationViewModel.kt` (Hilt ViewModel: `ConversationUiState` with Phase enum IDLE/LISTENING/THINKING/SPEAKING/ENDED; orchestrates STT→AI→TTS via interfaces; incremental turn persistence via `ConversationRepository.appendTurn`; `onStart`/`onUserFinishedSpeaking`/`onEndSession`/`onPermissionDenied`/`onAppBackgrounded`/`onRetry`; sets `portugueseNudgeVisible` from `meta.userSpokePortuguese`; handles all `ConversationError` states without crashing) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T046 [US1] Create `ConversationScreen.kt` (Compose: displays phase, `aiPersonaName`, `liveTranscript`, completed `turns` list; renders `AudioSpectrum` while `phase==SPEAKING`; shows "Encerrar sessão" button; requests `RECORD_AUDIO` permission on entry; shows `ConversationError` messages in Brazilian Portuguese) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T047 [US1] Create `ConversationModule.kt` Hilt module (provide `OkHttpClient`; bind `AnthropicConversationAiClient` as `ConversationAiClient`; bind Android STT/TTS impls; bind `SystemPromptBuilderImpl` as `SystemPromptBuilder`) in `app/src/main/java/com/caiana/talks/di/`
- [ ] T048 [US1] Update `RepositoryModule.kt` to bind `ConversationRepositoryImpl` as `ConversationRepository` in `app/src/main/java/com/caiana/talks/di/`
- [ ] T049 [US1] Modify `HomeScreen.kt` to add "Iniciar conversa" button above the progress button, navigating to the `conversation` route in `app/src/main/java/com/caiana/talks/ui/home/`
- [ ] T050 [US1] Modify `AppNavGraph.kt` to add `conversation` composable route wired to `ConversationScreen` (single-speaker entry) in `app/src/main/java/com/caiana/talks/ui/navigation/`

**Checkpoint**: US1 fully functional — start conversation, speak, receive AI voice response, end after ≥60 s navigates to summary stub / <60 s returns to Home. All US1 unit tests pass (`.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.*"`).

---

## Phase 4: User Story 2 — AI Correction and Vocabulary Enrichment (Priority: P2)

**Goal**: Corrections and vocabulary enrichment are displayed in the session and the Portuguese nudge appears when the user speaks Portuguese.

**Independent Test**: Deliberately speak with grammar/vocabulary errors during a session; the AI corrects them inline with encouraging language; speak a sentence in Portuguese and confirm the gentle nudge appears.

### Tests for User Story 2

- [ ] T051 [P] [US2] Create `PortugueseNudgeTest.kt` (verify `ConversationUiState.portugueseNudgeVisible=true` when `AiResponseMeta.userSpokePortuguese=true`; `=false` when `pt=false`; nudge clears on next turn where `pt=false`) in `app/src/test/java/com/caiana/talks/conversation/`

### Implementation for User Story 2

- [ ] T052 [US2] Add Portuguese nudge UI to `ConversationScreen.kt` — show a dismissible banner "Continue em inglês 😊" when `state.portugueseNudgeVisible == true`; banner disappears automatically on the next turn in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T053 [US2] Verify vocabulary highlights from `AiResponseMeta.vocabulary` are surfaced in each `TurnUi` entry and rendered in `ConversationScreen.kt`; add vocabulary display row under each completed AI turn if not already present in `app/src/main/java/com/caiana/talks/ui/conversation/`

**Checkpoint**: US2 verified — grammar and vocabulary corrections appear inline in the conversation; Portuguese nudge shows when Portuguese is detected; vocabulary enrichments visible per turn.

---

## Phase 5: User Story 3 — Session Summary and Stats Update (Priority: P2)

**Goal**: After every session ≥60 s, a summary screen shows duration, correction count, and vocabulary highlights; the stats screen reflects updated totals immediately.

**Independent Test**: Complete a session ≥60 s → confirm summary screen shows correct duration/corrections/vocab → open stats screen → confirm session count and minutes increased.

### Tests for User Story 3

- [ ] T054 [P] [US3] Create `SessionSummaryViewModelTest.kt` (single mode → `perParticipant` has 1 entry; dual mode → 2 entries loaded by `coPracticeGroupId`; duration <60 min → "Xmin" label; ≥60 min → "Xh Ymin" label; vocabulary highlights surfaced per participant) in `app/src/test/java/com/caiana/talks/conversation/`

### Implementation for User Story 3

- [ ] T055 [US3] Create `SessionSummaryViewModel.kt` (Hilt ViewModel: loads session(s) by sessionId or `coPracticeGroupId` via `ConversationRepository`; builds `SessionSummaryUiState` with `perParticipant: List<ParticipantSummaryUi>`; duration label follows "Xmin"/"Xh Ymin" rule from feature 003) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T056 [US3] Create `SessionSummaryScreen.kt` (Compose: list `perParticipant` summaries each showing profile name, duration label, correction count, vocabulary highlights; "Voltar ao início" button → home) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T057 [US3] Modify `AppNavGraph.kt` to add `sessionSummary/{groupId}` composable route wired to `SessionSummaryScreen` in `app/src/main/java/com/caiana/talks/ui/navigation/`

**Checkpoint**: US3 verified — summary screen appears after each ≥60 s session with correct data; stats screen aggregates correctly across multiple sessions.

---

## Phase 6: User Story 4 — Dual-Speaker Co-Practice Mode (Priority: P3)

**Goal**: Two users share a device, alternate turns, see corrections addressed to the right speaker, and each profile's stats update independently.

**Independent Test**: Tap "Co-practice" → select two distinct profiles → start session → alternate speakers → confirm turn indicator switches → end session → verify each profile's stats updated independently.

### Tests for User Story 4

- [ ] T058 [P] [US4] Create `CoPracticeSetupViewModelTest.kt` (`canStart=false` until two profiles selected; `canStart=false` when same profile selected twice; `canStart=true` for two distinct profiles; profile list loaded from `UserRepository`) in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T059 [US4] Extend `ConversationViewModelTest.kt` with dual-mode cases: `activeSpeakerName` alternates each turn; corrections attributed to the active speaker's profile; `onEndSession` in dual mode navigates to `sessionSummary/{groupId}` in `app/src/test/java/com/caiana/talks/conversation/`
- [ ] T060 [US4] Extend `ConversationRepositoryTest.kt` with dual-mode cases: `startSession` dual → 2 active rows sharing one UUID `coPracticeGroupId`; `appendTurn` writes to the correct participant row; dual `finalizeSession` → 2 `SessionSummary` entries in the `SessionResult` in `app/src/test/java/com/caiana/talks/conversation/`

### Implementation for User Story 4

- [ ] T061 [P] [US4] Create `CoPracticeSetupViewModel.kt` (Hilt ViewModel: loads all profiles; `onSelectFirst`/`onSelectSecond`; `canStart` = first and second are both non-null and distinct; `onStart` → emit `coPracticeGroupId` and navigate to `conversation?group={id}`) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T062 [P] [US4] Create `CoPracticeSetupScreen.kt` (Compose: scrollable profile list for first and second slot; "Iniciar" button enabled only when `state.canStart`; slots show selected profile name; user cannot select the same profile in both slots) in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T063 [US4] Extend `ConversationViewModel.kt` to support `SessionMode.DUAL`: track `activeSpeakerName` alternating each turn, pass `speakerProfileId` of the active participant to `ConversationRepository.appendTurn`, update `ConversationUiState.activeSpeakerName` in `app/src/main/java/com/caiana/talks/ui/conversation/`
- [ ] T064 [US4] Extend `ConversationRepositoryImpl.kt` dual-session support: `startSession` for DUAL creates two `SessionEntity` rows sharing one UUID `coPracticeGroupId`; `finalizeSession` handles both rows and returns two `SessionSummary` objects in `app/src/main/java/com/caiana/talks/data/repository/`
- [ ] T065 [US4] Modify `HomeScreen.kt` to add "Co-practice" button navigating to `coPracticeSetup` route in `app/src/main/java/com/caiana/talks/ui/home/`
- [ ] T066 [US4] Modify `AppNavGraph.kt` to add `coPracticeSetup` route wired to `CoPracticeSetupScreen` and `conversation?group={id}` route wired to `ConversationScreen` in dual mode in `app/src/main/java/com/caiana/talks/ui/navigation/`

**Checkpoint**: US4 verified — two profiles selectable; turn indicator alternates; corrections attributed correctly; each profile's stats updated independently after session.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Recovery path, error hardening, and full test validation across all stories.

- [X] T067 [P] Run full unit test suite and fix any failures: `.\gradlew testDebugUnitTest` in `app/src/test/`
- [ ] T068 [P] Run instrumented migration test and fix failures: `.\gradlew connectedDebugAndroidTest --tests "com.caiana.talks.MigrationTest"` in `app/src/androidTest/`
- [X] T069 Wire `ConversationRepository.recoverDanglingSessions()` call on app launch (in `MainActivity.kt` or `AppNavGraph.kt` init) to handle dangling `active` sessions from process death
- [X] T070 [P] Add storage-full error handling in `ConversationRepositoryImpl.kt`: catch `IOException` during `appendTurn`, downgrade session to partial, set `ConversationError.STORAGE_FULL` in ViewModel
- [ ] T071 Perform emulator walkthrough per quickstart §3–4: US1–US4 golden paths and all edge cases (mic denied, airplane mode mid-session, <60 s session, Portuguese speech, app backgrounded)
- [X] T072 [P] Update `CLAUDE.md` to reference the completed feature 004 plan and remove the plan pointer that is no longer the current work

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Phase 2 — this is the MVP; no dependency on US2/US3/US4
- **US2 (Phase 4)**: Depends on US1 (the `portugueseNudgeVisible` state is set by the ViewModel built in US1)
- **US3 (Phase 5)**: Depends on US1 (repository `finalizeSession` is implemented in US1); can run in parallel with US2
- **US4 (Phase 6)**: Depends on US1, US3 (extends both ViewModel and repository; reuses `sessionSummary` route)
- **Polish (Phase 7)**: Depends on all desired stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Phase 2 — independent of US2/US3/US4
- **US2 (P2)**: Can start after US1 ViewModel is done (T045); independent of US3/US4
- **US3 (P2)**: Can start after US1 repository is done (T043); can run in parallel with US2
- **US4 (P3)**: Requires US1 and US3 to be complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation begins
- Domain models → DAOs → Repository → Services → ViewModel → Screen → Navigation
- Core implementation before integration with navigation/DI
- Story fully passing tests before moving to next priority

### Parallel Opportunities

- All Phase 1 tasks marked [P] can run in parallel
- All Phase 2 domain model tasks (T005–T013) marked [P] can run in parallel
- T014/T016 (DB entities) can run in parallel with each other
- All pure-logic tests within US1 (T020–T028) can run in parallel
- All pure-logic implementations within US1 (T032–T041) can run in parallel
- US2 and US3 can be worked in parallel once US1 is complete
- All Polish tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1 Pure Logic

```powershell
# Run all pure-logic tests in parallel:
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.SentenceChunkerTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.AiResponseParserTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.RollingWindowTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.AiPersonaTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.VoiceSelectorTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.SessionDurationPolicyTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.SpectrumWaveformTest"
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.SseEventParserTest"

# Run all feature tests together:
.\gradlew testDebugUnitTest --tests "com.caiana.talks.conversation.*"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (build config, deps, manifest)
2. Complete Phase 2: Foundational — CRITICAL, blocks everything
3. Complete Phase 3: User Story 1 (tests first, then implementation)
4. **STOP AND VALIDATE**: Run all US1 tests → install on emulator → follow quickstart §4 US1 walkthrough
5. US1 delivers the entire core product loop — ship-ready as an MVP

### Incremental Delivery

1. Setup + Foundational → infrastructure ready
2. US1 → solo voice session complete → **MVP deliverable**
3. US2 → corrections and nudge polished → better pedagogical experience
4. US3 → summary screen + stats confirmed → full feedback loop
5. US4 → co-practice mode → engagement feature
6. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks operate on different files or have no cross-dependencies — safe to parallelize
- [Story] label maps each task to a specific user story for independent traceability
- Test classes live in `app/src/test/java/com/caiana/talks/conversation/` (unit) and `app/src/androidTest/java/com/caiana/talks/` (instrumented)
- Source classes follow the plan's `app/src/main/java/com/caiana/talks/` package layout exactly
- Android framework classes (`SpeechRecognizer`, `TextToSpeech`) and the live HTTP path are **not** unit-tested — exercised via the emulator per quickstart §3–4
- Run `.\gradlew testDebugUnitTest` after each phase to catch regressions early
