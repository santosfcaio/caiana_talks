# Implementation Plan: OpenRouter AI Connector

**Branch**: `006-openrouter-connector` | **Date**: 2026-05-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/006-openrouter-connector/spec.md`

## Summary

Migrate the AI conversation backend from Anthropic's proprietary API to OpenRouter.ai's OpenAI-compatible endpoint, replacing `AnthropicConversationAiClient` with a new `OpenRouterConversationAiClient`. The migration also adds user-configurable API key and model overrides in the Settings screen, backed by the existing DataStore, so the app owner can switch credentials or models at runtime without rebuilding.

## Technical Context

**Language/Version**: Kotlin 1.9 / JVM 11

**Primary Dependencies**: Jetpack Compose, Hilt, OkHttp, DataStore Preferences, Room v3

**Storage**: Room (session/turn/correction DB — unchanged); DataStore Preferences (adds 2 new string keys)

**Testing**: JUnit 4 + MockK + Turbine + kotlinx-coroutines-test (`.\gradlew testDebugUnitTest`)

**Target Platform**: Android SDK 26+ (minSdk 26, compileSdk 34)

**Project Type**: Mobile app (Android)

**Performance Goals**: Streaming first-token latency comparable to previous Anthropic integration; max_tokens=400 preserved

**Constraints**: DataStore writes must be non-blocking (suspend functions on IO); key resolution must be per-call (not cached at injection time)

**Scale/Scope**: Single-user and co-practice modes unchanged; Settings screen adds 2 text fields

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Voice-First Interface | PASS | No changes to STT/TTS or conversation flow; Settings text fields are configuration-only |
| II. Token-Efficient AI Conversations | PASS | max_tokens=400 preserved; `qwen/qwen3.5-9b` is a cost-efficient model comparable to claude-haiku-4-5; prompt caching dropped (not available on this tier) |
| III. Pedagogical Effectiveness | PASS | System prompt and `<say>`/`<meta>` format enforced via existing `SystemPromptBuilder` and `AiResponseParser` — no changes |
| IV. Brazilian Portuguese → English Only | PASS | UI additions are in Brazilian Portuguese; no language scope changes |
| V. Dual-Speaker Mode | PASS | `ConversationAiClient` interface unchanged; co-practice path is unaffected |
| VI. Personalization & Progress Tracking | PASS | Room DB and stats unchanged; 2 new DataStore keys added alongside existing ones |
| VII. LCARS Visual Identity | PASS | New `LcarsTextField` component required for Settings fields; uses LCARS color palette and styling — no default Material text fields |

**Post-Design Re-check**: PASS — design introduces `LcarsTextField` as a native LCARS component rather than an unstyled widget. No new screens added; Settings screen extended only.

## Project Structure

### Documentation (this feature)

```text
specs/006-openrouter-connector/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (files touched by this feature)

```text
app/build.gradle.kts
local.properties
app/src/main/java/com/caiana/talks/
├── data/
│   ├── local/preferences/
│   │   └── UserPreferencesDataStore.kt     ← extend: 2 new string keys
│   └── remote/
│       ├── AnthropicConversationAiClient.kt ← delete (replaced)
│       ├── ConversationAiClient.kt          ← unchanged
│       ├── OpenRouterConversationAiClient.kt ← new
│       └── SseEventParser.kt               ← update extractDeltaText
├── di/
│   └── ConversationModule.kt               ← rebind to OpenRouterConversationAiClient
└── ui/
    ├── settings/
    │   ├── SettingsScreen.kt               ← add 2 LcarsTextField + ViewModel wiring
    │   └── SettingsViewModel.kt            ← new HiltViewModel
    └── theme/components/
        └── LcarsTextField.kt               ← new LCARS text field component

app/src/test/java/com/caiana/talks/
├── conversation/
│   └── SseEventParserTest.kt              ← update OpenAI SSE fixture
└── data/
    ├── OpenRouterConversationAiClientTest.kt ← new
    ├── SettingsViewModelTest.kt              ← new
    └── UserPreferencesDataStoreTest.kt       ← extend: new key tests
```

**Structure Decision**: Single Android project (Option 3 mobile-only); existing module layout preserved; no new Gradle modules introduced.

## Implementation Phases

### Phase A — Build Configuration

1. **`local.properties`**: Remove `ANTHROPIC_API_KEY`; add:
   ```
   OPENROUTER_API_KEY=sk-or-v1-your-openrouter-api-key-here
   OPENROUTER_MODEL=qwen/qwen3.5-9b
   ```

2. **`app/build.gradle.kts`**: In `defaultConfig`, replace the `ANTHROPIC_API_KEY` `buildConfigField` with:
   ```kotlin
   buildConfigField("String", "OPENROUTER_API_KEY",
       "\"${localProps.getProperty("OPENROUTER_API_KEY", "")}\"")
   buildConfigField("String", "OPENROUTER_MODEL",
       "\"${localProps.getProperty("OPENROUTER_MODEL", "")}\"")
   ```

### Phase B — DataStore Extensions

3. **`UserPreferencesDataStore.kt`**: Extend interface and implementation:
   - New preference keys: `stringPreferencesKey("openrouter_api_key")`, `stringPreferencesKey("ai_model")`
   - New `Flow<String>` properties: `openrouterApiKey`, `aiModel` (both default to `""`)
   - New suspend setters: `setOpenrouterApiKey(key: String)`, `setAiModel(model: String)`

### Phase C — SSE Parser Update

4. **`SseEventParser.kt`**: Update `extractDeltaText` to parse OpenAI/OpenRouter format:
   - Old: check `type == "content_block_delta"` → `delta.type == "text_delta"` → `delta.text`
   - New: parse `choices[0].delta.content` (return `null` if absent or empty)
   - `parseLine` and `isDone` are unchanged.

### Phase D — New AI Client

5. **`OpenRouterConversationAiClient.kt`** (new file, replaces `AnthropicConversationAiClient`):
   - Constructor: `OkHttpClient`, `UserPreferencesDataStore`
   - Per-call key/model resolution:
     ```kotlin
     val userKey = userPrefs.openrouterApiKey.first()
     val userModel = userPrefs.aiModel.first()
     val apiKey = userKey.takeIf { it.isNotEmpty() } ?: BuildConfig.OPENROUTER_API_KEY
     val model = userModel.takeIf { it.isNotEmpty() } ?: BuildConfig.OPENROUTER_MODEL
     ```
   - Request URL: `https://openrouter.ai/api/v1/chat/completions`
   - Auth: `Authorization: Bearer $apiKey`
   - No `anthropic-version` or `anthropic-beta` headers
   - Request body (OpenAI format): system role message (staticBlock + `\n\n` + personalizationBlock), then conversation window + userInput as user/assistant messages
   - SSE streaming loop: identical to current implementation (uses updated `SseEventParser.extractDeltaText`)
   - Error mapping: unchanged (`!response.isSuccessful` → `AI_API_ERROR`, `IOException` → `NETWORK_UNAVAILABLE`)

6. **Delete `AnthropicConversationAiClient.kt`** after step 5 is complete and tests pass.

### Phase E — DI Rebinding

7. **`ConversationModule.kt`**: In `ConversationBindModule`:
   - Change `@Binds` from `AnthropicConversationAiClient` to `OpenRouterConversationAiClient`
   - Update import

### Phase F — LCARS Text Field Component

8. **`LcarsTextField.kt`** (new, in `ui/theme/components/`):
   - Wraps `OutlinedTextField` with LCARS palette: black background, orange label/border, white text, monospace hint
   - Parameters: `value`, `onValueChange`, `label`, `modifier`, optionally `placeholder`
   - Follows existing LCARS component patterns (see `LcarsButton`, `LcarsDataPanel` for style reference)

### Phase G — Settings Screen and ViewModel

9. **`SettingsViewModel.kt`** (new):
   - `@HiltViewModel` + `@Inject constructor(private val userPrefs: UserPreferencesDataStore)`
   - `val apiKeyOverride: StateFlow<String>` — from `userPrefs.openrouterApiKey.stateIn(...)`
   - `val modelOverride: StateFlow<String>` — from `userPrefs.aiModel.stateIn(...)`
   - `fun setApiKey(key: String)` — calls `userPrefs.setOpenrouterApiKey(key)` in `viewModelScope`
   - `fun setModel(model: String)` — calls `userPrefs.setAiModel(model)` in `viewModelScope`

10. **`SettingsScreen.kt`** (extended):
    - Add `viewModel: SettingsViewModel = hiltViewModel()` parameter
    - Collect `apiKeyOverride` and `modelOverride` via `collectAsStateWithLifecycle()`
    - Add a new `LcarsDataPanel` section (e.g., labeled "API / Modelo") below the existing "Conta" section
    - Add two `LcarsTextField` instances:
      - API Key: label "Chave OpenRouter", placeholder "Usar padrão do app", persists on `onValueChange`
      - Model: label "Modelo de IA", placeholder "Usar padrão do app", persists on `onValueChange`

### Phase H — Tests

11. **`SseEventParserTest.kt`**: Update `extractDeltaText` fixture:
    - Old JSON: `{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}`
    - New JSON: `{"id":"gen-1","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}`
    - Add test: `extractDeltaText returns null when choices delta content is absent` (final stop chunk)

12. **`OpenRouterConversationAiClientTest.kt`** (new):
    - Test: successful streaming emits `TextDelta` + `SayEnded` + `Completed`
    - Test: `!response.isSuccessful` emits `Failed(AI_API_ERROR)`
    - Test: `IOException` emits `Failed(NETWORK_UNAVAILABLE)`
    - Test: user key override takes precedence over BuildConfig default
    - Test: empty user key falls back to BuildConfig default
    - Test: user model override takes precedence over BuildConfig default

13. **`SettingsViewModelTest.kt`** (new):
    - Test: `apiKeyOverride` emits DataStore value
    - Test: `setApiKey` calls `userPrefs.setOpenrouterApiKey`
    - Test: `setModel` calls `userPrefs.setAiModel`

14. **Run full test suite**: `.\gradlew testDebugUnitTest` — all 198 existing + new tests must pass.

## API Cost Estimate (Constitution Principle II)

| Metric | Value |
|---|---|
| Model | `qwen/qwen3.5-9b` via OpenRouter |
| Estimated input tokens/turn | ~600 (system prompt ~300 + 5-turn window ~300) |
| Estimated output tokens/turn | ~200 (max_tokens=400, typical reply shorter) |
| OpenRouter pricing (qwen3.5-9b approximate) | ~$0.06/M input, ~$0.24/M output |
| Estimated cost/session (10 turns) | < $0.001 |

Significantly below the previous Anthropic Haiku cost (~$0.008/10-turn session). Principle II satisfied.

## Complexity Tracking

No Constitution Check violations. No justification table required.
