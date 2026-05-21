# Tasks: OpenRouter AI Connector

**Input**: Design documents from `/specs/006-openrouter-connector/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)

---

## Phase 1: Setup (Build Configuration)

**Purpose**: Replace Anthropic build config fields with OpenRouter equivalents. Blocks everything.

- [x] T001 Update `local.properties` — remove `ANTHROPIC_API_KEY`; add `OPENROUTER_API_KEY=sk-or-v1-your-openrouter-api-key-here` and `OPENROUTER_MODEL=qwen/qwen3.5-9b`
- [x] T002 Update `app/build.gradle.kts` `defaultConfig` — replace `ANTHROPIC_API_KEY` `buildConfigField` with two new fields: `OPENROUTER_API_KEY` and `OPENROUTER_MODEL`, both loaded from `localProps` with empty-string fallbacks

**Checkpoint**: Project compiles with new BuildConfig fields; `ANTHROPIC_API_KEY` no longer referenced in build files

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infrastructure that both US1 (client reads DataStore) and US2 (Settings writes DataStore) depend on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Extend `app/src/main/java/com/caiana/talks/data/local/preferences/UserPreferencesDataStore.kt` — add `openrouterApiKey: Flow<String>` and `aiModel: Flow<String>` to the interface and impl using `stringPreferencesKey("openrouter_api_key")` / `stringPreferencesKey("ai_model")`; add `suspend fun setOpenrouterApiKey(key: String)` and `suspend fun setAiModel(model: String)` to both interface and impl; both flows default to `""`
- [x] T004 Update `SseEventParser.extractDeltaText` in `app/src/main/java/com/caiana/talks/data/remote/SseEventParser.kt` — replace Anthropic `content_block_delta` parsing with OpenAI/OpenRouter format: parse `choices[0].delta.content` (return `null` if `choices` is absent, `delta.content` is absent, or content is empty); `parseLine` and `isDone` are unchanged

**Checkpoint**: DataStore compiles with new keys; `SseEventParser` extracts text from OpenRouter SSE format

---

## Phase 3: User Stories 1 & 3 — Core Migration (Priority: P1) 🎯 MVP

**Goal (US1)**: App uses build-time OpenRouter key by default and streams AI replies through the new client.
**Goal (US3)**: All existing conversation behaviors (streaming, `<say>` parsing, corrections, co-practice) are preserved exactly.

**Independent Test**: Set a valid `OPENROUTER_API_KEY` in `local.properties`, rebuild, start a full voice session — AI replies stream in real time with corrections appearing at session end. All 198+ unit tests pass.

### Tests (write first — must fail before implementation)

- [x] T005 [P] [US1] Create `app/src/test/java/com/caiana/talks/conversation/OpenRouterConversationAiClientTest.kt` — mock `OkHttpClient` and `UserPreferencesDataStore`; write failing tests for: (a) successful SSE stream emits `TextDelta` events then `SayEnded` then `Completed`, (b) non-2xx response emits `Failed(AI_API_ERROR)`, (c) `IOException` emits `Failed(NETWORK_UNAVAILABLE)`, (d) non-empty user key from DataStore takes precedence over BuildConfig, (e) empty user key falls back to BuildConfig `OPENROUTER_API_KEY`, (f) non-empty user model takes precedence, (g) empty user model falls back to BuildConfig `OPENROUTER_MODEL`
- [x] T006 [P] [US3] Update `app/src/test/java/com/caiana/talks/conversation/SseEventParserTest.kt` — rename `extractDeltaText extracts text delta from Anthropic SSE JSON` to `extractDeltaText extracts text delta from OpenRouter SSE JSON`; replace fixture with `{"id":"gen-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}`; add test `extractDeltaText returns null when choices delta content is absent` using `{"id":"gen-2","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}`

### Implementation

- [x] T007 [US1] Create `app/src/main/java/com/caiana/talks/data/remote/OpenRouterConversationAiClient.kt` — `@Inject constructor(httpClient: OkHttpClient, userPrefs: UserPreferencesDataStore)`; per-call resolution: `userPrefs.openrouterApiKey.first()` → `takeIf { it.isNotEmpty() } ?: BuildConfig.OPENROUTER_API_KEY` (same for model); request URL `https://openrouter.ai/api/v1/chat/completions`; auth header `Authorization: Bearer $apiKey`; no `anthropic-version` or `anthropic-beta` headers; OpenAI-format body: system role message = `"${system.staticBlock}\n\n${system.personalizationBlock}"` followed by window + userInput as user/assistant messages; max_tokens=400, stream=true; SSE streaming loop identical to old client but uses updated `SseEventParser.extractDeltaText`; error mapping unchanged (`!isSuccessful` → `AI_API_ERROR`, `IOException` → `NETWORK_UNAVAILABLE`)
- [x] T008 [US1] Update `app/src/main/java/com/caiana/talks/di/ConversationModule.kt` — change `@Binds` in `ConversationBindModule` from `AnthropicConversationAiClient` to `OpenRouterConversationAiClient`; update import; remove `AnthropicConversationAiClient` import
- [x] T009 [US1] Delete `app/src/main/java/com/caiana/talks/data/remote/AnthropicConversationAiClient.kt` — file is fully replaced by `OpenRouterConversationAiClient.kt`
- [x] T010 [US1] [US3] Run `.\gradlew testDebugUnitTest` — all tests (198 existing + new T005/T006 tests) must pass; zero compilation errors; zero test failures

**Checkpoint**: Core migration complete. App uses OpenRouter. All existing behaviors preserved. SC-001, SC-002, SC-003, FR-001 through FR-009 satisfied.

---

## Phase 4: User Story 2 — Settings Screen Overrides (Priority: P2)

**Goal**: User can enter a custom OpenRouter API key and model identifier in the Settings screen, overriding the build-time defaults without rebuilding the app.

**Independent Test**: Leave `OPENROUTER_API_KEY` blank in `local.properties`, enter a valid key and model in Settings, start a conversation — AI replies correctly using the user-provided credentials.

### Tests (write first — must fail before implementation)

- [x] T011 [P] [US2] Create `app/src/test/java/com/caiana/talks/ui/SettingsViewModelTest.kt` — mock `UserPreferencesDataStore`; write failing tests for: (a) `apiKeyOverride` StateFlow emits value from `userPrefs.openrouterApiKey`, (b) `modelOverride` StateFlow emits value from `userPrefs.aiModel`, (c) `setApiKey("abc")` calls `userPrefs.setOpenrouterApiKey("abc")`, (d) `setModel("qwen/x")` calls `userPrefs.setAiModel("qwen/x")`
- [x] T012 [P] [US2] Create `app/src/test/java/com/caiana/talks/data/UserPreferencesDataStoreTest.kt` — use in-memory `DataStore` test fixture; write tests for: (a) `openrouterApiKey` emits `""` by default, (b) `setOpenrouterApiKey("key")` → `openrouterApiKey` emits `"key"`, (c) `aiModel` emits `""` by default, (d) `setAiModel("model")` → `aiModel` emits `"model"`

### Implementation

- [x] T013 [US2] Create `app/src/main/java/com/caiana/talks/ui/theme/components/LcarsTextField.kt` — `@Composable fun LcarsTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, placeholder: String = "")` wrapping `OutlinedTextField`; colors: container = `LcarsColors.Black`, focused/unfocused border = `LcarsColors.Orange`, label = `LcarsColors.Orange`, text = `Color.White`, placeholder = `Color.White.copy(alpha = 0.5f)`; follows existing LCARS component conventions in `ui/theme/components/`
- [x] T014 [US2] Create `app/src/main/java/com/caiana/talks/ui/settings/SettingsViewModel.kt` — `@HiltViewModel class SettingsViewModel @Inject constructor(private val userPrefs: UserPreferencesDataStore) : ViewModel()`; `val apiKeyOverride: StateFlow<String> = userPrefs.openrouterApiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")`; same for `modelOverride` from `userPrefs.aiModel`; `fun setApiKey(key: String)` launches coroutine calling `userPrefs.setOpenrouterApiKey(key)`; `fun setModel(model: String)` same for `aiModel`
- [x] T015 [US2] Update `app/src/main/java/com/caiana/talks/ui/settings/SettingsScreen.kt` — add `viewModel: SettingsViewModel = hiltViewModel()` parameter; collect `apiKeyOverride` and `modelOverride` via `collectAsStateWithLifecycle()`; add a new `LcarsDataPanel(accentColor = LcarsColors.Blue)` section labeled "API / Modelo" below the existing "Conta" section; inside add two `LcarsTextField` instances: first with `label = "Chave OpenRouter"` and `placeholder = "Usar padrão do app"` bound to `apiKeyOverride`/`setApiKey`, second with `label = "Modelo de IA"` and `placeholder = "Usar padrão do app"` bound to `modelOverride`/`setModel`; update Preview to supply a mock ViewModel or use default parameter
- [x] T016 [US2] Run `.\gradlew testDebugUnitTest` — all tests including T011 and T012 must pass

**Checkpoint**: User overrides work. Entering key+model in Settings and starting a session uses the user values. SC-005, SC-006, FR-010 through FR-013 satisfied.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup, verification, and documentation consistency

- [x] T017 [P] Search entire codebase for remaining references to `ANTHROPIC_API_KEY` and `AnthropicConversationAiClient` — remove or update any stray references in source, build files, comments, or string literals (SC-003)
- [x] T018 [P] Update `CLAUDE.md` source structure comment — change `data/remote/` description from "Anthropic claude-haiku-4-5 via OkHttp SSE" to "OpenRouter.ai via OkHttp SSE (`OpenRouterConversationAiClient`)"
- [ ] T019 Install on emulator and run manual acceptance test — `.\gradlew installDebug`; launch app; start a voice conversation; confirm streaming AI replies appear and are spoken aloud; end session; confirm corrections summary appears (SC-001 manual validation)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US1 + US3 (Phase 3)**: Depends on Phase 2 — core migration
- **US2 (Phase 4)**: Depends on Phase 2 (DataStore ready) — can start in parallel with Phase 3 if desired, except T015 which needs T007 to compile
- **Polish (Phase 5)**: Depends on Phase 3 and Phase 4 complete

### User Story Dependencies

- **US1 + US3 (P1)**: Start after Phase 2 — no dependency on US2
- **US2 (P2)**: Start after Phase 2 — T011–T013 can run in parallel with Phase 3; T014–T016 need T007 compiled

### Within Each Phase

- Tests (T005, T006, T011, T012) should be written first and seen to fail before implementation
- T007 must compile before T008 (DI rebinding)
- T009 (delete old file) must come after T008 (rebinding verified)
- T015 (Settings UI) must come after T013 (LcarsTextField) and T014 (ViewModel)

### Parallel Opportunities

- T005 and T006 can run in parallel (different test files)
- T011 and T012 can run in parallel (different test files)
- T013 (LcarsTextField) can run in parallel with T011/T012 (different files)
- T017 and T018 can run in parallel (Phase 5)

---

## Parallel Example: Phase 3

```text
# These test tasks can be written simultaneously:
Task T005: OpenRouterConversationAiClientTest.kt
Task T006: SseEventParserTest.kt (update fixture)

# Then implement:
Task T007: OpenRouterConversationAiClient.kt
Task T008: ConversationModule.kt (after T007)
Task T009: delete AnthropicConversationAiClient.kt (after T008)
Task T010: run full test suite
```

## Parallel Example: Phase 4

```text
# These can run simultaneously after Phase 2:
Task T011: SettingsViewModelTest.kt
Task T012: UserPreferencesDataStoreTest.kt
Task T013: LcarsTextField.kt

# Then implement sequentially:
Task T014: SettingsViewModel.kt (after T011 fails as expected)
Task T015: SettingsScreen.kt (after T013, T014)
Task T016: run full test suite
```

---

## Implementation Strategy

### MVP First (Phase 1 + 2 + 3 only)

1. Complete Phase 1: Build config
2. Complete Phase 2: DataStore extension + SSE parser update
3. Complete Phase 3: Core migration (new client, rebind, delete old, all tests green)
4. **STOP and VALIDATE**: Full voice session works with OpenRouter. 198+ tests pass.
5. Ship or demo the OpenRouter migration

### Incremental Delivery

1. Phase 1 + 2 → Foundation ready
2. Phase 3 → OpenRouter migration works (MVP)
3. Phase 4 → User can override key/model from Settings
4. Phase 5 → Cleanup and manual validation

---

## Notes

- `[P]` tasks target different files — safe to work in parallel
- `[Story]` label maps each task to its user story for traceability
- Run test suite after each phase checkpoint; do not skip
- T009 (delete old file) is irreversible — only run after T008 compiles and tests pass
- `local.properties` is gitignored; the `OPENROUTER_API_KEY` value in T001 is a developer secret and must not be committed to source control
