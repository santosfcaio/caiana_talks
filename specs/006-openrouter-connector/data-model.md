# Data Model: OpenRouter AI Connector

**Feature**: 006-openrouter-connector | **Phase**: 1 | **Date**: 2026-05-20

---

## Entities

### 1. OpenRouter Configuration (Build-Time)

Stored in `BuildConfig` via `app/build.gradle.kts` + `local.properties`.

| Field | Type | Default | Description |
|---|---|---|---|
| `OPENROUTER_API_KEY` | String | `sk-or-v1-...` | Developer-supplied bearer token; used when no user override exists |
| `OPENROUTER_MODEL` | String | `qwen/qwen3.5-9b` | Developer-supplied model identifier; used when no user override exists |

Validation rules:
- Both fields MUST be present in `local.properties` for a functional build; empty strings are valid (will surface `AI_API_ERROR` at call time)
- `ANTHROPIC_API_KEY` MUST be fully removed (SC-003)

---

### 2. User API Override (Runtime, DataStore)

Stored in the existing `DataStore<Preferences>` alongside `activeUserId`.

| Preference Key | DataStore Key | Type | Default | Description |
|---|---|---|---|---|
| `openrouter_api_key` | `stringPreferencesKey("openrouter_api_key")` | String | `""` | User-entered API key; empty = fall back to BuildConfig default |
| `ai_model` | `stringPreferencesKey("ai_model")` | String | `""` | User-entered model identifier; empty = fall back to BuildConfig default |

Validation rules:
- Empty string means "use default" — no trimming or format validation is applied on write
- Values persist across app restarts via DataStore
- No encryption applied (plain DataStore, per spec assumption)

State transitions:
- `""` → user enters value → non-empty override stored → takes precedence at call time
- non-empty → user clears field → `""` stored → fallback to BuildConfig

---

### 3. Resolved API Credentials (Call-Time Computation)

Computed inside `OpenRouterConversationAiClient.streamReply()` on each invocation.

| Field | Resolution rule |
|---|---|
| `effectiveApiKey` | `userKey.takeIf { it.isNotEmpty() } ?: BuildConfig.OPENROUTER_API_KEY` |
| `effectiveModel` | `userModel.takeIf { it.isNotEmpty() } ?: BuildConfig.OPENROUTER_MODEL` |

No persistence. No caching between calls.

---

## Interface Changes

### `UserPreferencesDataStore` interface (extended)

```kotlin
interface UserPreferencesDataStore {
    val activeUserId: Flow<Int?>                    // existing
    val openrouterApiKey: Flow<String>              // new
    val aiModel: Flow<String>                       // new

    suspend fun setActiveUserId(id: Int)            // existing
    suspend fun clearActiveUserId()                 // existing
    suspend fun setOpenrouterApiKey(key: String)    // new
    suspend fun setAiModel(model: String)           // new
}
```

### `OpenRouterConversationAiClient` constructor

```kotlin
class OpenRouterConversationAiClient @Inject constructor(
    private val httpClient: OkHttpClient,
    private val userPrefs: UserPreferencesDataStore
) : ConversationAiClient
```

Replaces `AnthropicConversationAiClient`. The `ConversationAiClient` interface is unchanged (FR-008).

### `SseEventParser.extractDeltaText` — updated signature (same, new body)

```kotlin
// Before (Anthropic format):
// {"type":"content_block_delta","delta":{"type":"text_delta","text":"Hello"}}

// After (OpenAI/OpenRouter format):
// {"choices":[{"delta":{"content":"Hello"},"finish_reason":null}]}
fun extractDeltaText(json: String): String?
```

---

## Unchanged Interfaces

- `ConversationAiClient` — interface contract unchanged (FR-008)
- `SystemPrompt` data class — unchanged; `OpenRouterConversationAiClient` concatenates `staticBlock + "\n\n" + personalizationBlock` as the system role message
- `AiStreamEvent` sealed interface — unchanged
- `ConversationError` enum — unchanged (no new values)
- `AiResponseParser` — unchanged (parses `<say>` / `<meta>` blocks from full accumulated text)
- All ViewModels, Repositories, and other UI code — unchanged (FR-008 guarantee)
