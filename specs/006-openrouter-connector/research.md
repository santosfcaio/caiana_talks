# Research: OpenRouter AI Connector

**Feature**: 006-openrouter-connector | **Phase**: 0 | **Date**: 2026-05-20

---

## 1. OpenRouter API vs. Anthropic API — Protocol Differences

**Decision**: Replace `AnthropicConversationAiClient` with `OpenRouterConversationAiClient`, targeting the OpenRouter OpenAI-compatible endpoint.

**Rationale**: OpenRouter exposes a standard OpenAI chat-completions interface, making most of the HTTP plumbing transferable. The differences are limited to four areas: URL, auth header, request body schema, and SSE response schema.

**Alternatives considered**: Using OpenRouter's native Anthropic-pass-through mode was rejected because it still locks in Anthropic-specific formatting and does not generalize to other providers.

### Endpoint

| | Anthropic | OpenRouter |
|---|---|---|
| URL | `https://api.anthropic.com/v1/messages` | `https://openrouter.ai/api/v1/chat/completions` |
| Auth header | `x-api-key: <key>` | `Authorization: Bearer <key>` |
| Extra headers | `anthropic-version: 2023-06-01`, `anthropic-beta: prompt-caching-2024-07-31` | None required |

### Request body schema

**Anthropic format** (current):
```json
{
  "model": "claude-haiku-4-5",
  "max_tokens": 400,
  "stream": true,
  "system": [
    {"type": "text", "text": "<static block>", "cache_control": {"type": "ephemeral"}},
    {"type": "text", "text": "<personalization block>"}
  ],
  "messages": [{"role": "user", "content": "..."}, ...]
}
```

**OpenRouter/OpenAI format** (target):
```json
{
  "model": "qwen/qwen3.5-9b",
  "max_tokens": 400,
  "stream": true,
  "messages": [
    {"role": "system", "content": "<static block>\n\n<personalization block>"},
    {"role": "user", "content": "..."},
    {"role": "assistant", "content": "..."}
  ]
}
```

System content is a plain string concatenation of the two blocks separated by `\n\n`. No cache-control objects.

### SSE response delta schema

**Anthropic format** (current — `SseEventParser.extractDeltaText`):
```json
{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}
```
Extraction path: `$.delta.text` where `$.type == "content_block_delta"` and `$.delta.type == "text_delta"`.

**OpenRouter/OpenAI format** (target):
```json
{"id":"gen-...","object":"chat.completion.chunk","choices":[{"index":0,"delta":{"content":"Hello"},"finish_reason":null}]}
```
Extraction path: `$.choices[0].delta.content` (when non-empty; may be null/absent on the final chunk).

Stream termination sentinel: `data: [DONE]` — identical in both formats. `parseLine` and `isDone` in `SseEventParser` are already correct and require no change.

---

## 2. Prompt Caching

**Decision**: Drop prompt caching entirely for this migration.

**Rationale**: Prompt caching is an Anthropic-specific feature tied to `anthropic-beta: prompt-caching-2024-07-31`. OpenRouter does not expose this for the `qwen/qwen3.5-9b` model tier. The current `system` array structure with `cache_control` objects cannot be sent to OpenRouter and will cause a 400 error. The spec explicitly acknowledges this tradeoff.

**Alternatives considered**: OpenRouter advertises provider-level caching for certain premium providers (OpenAI, Anthropic pass-through), but none apply here.

---

## 3. API Key and Model Resolution

**Decision**: Resolve the effective API key and model at call time inside `OpenRouterConversationAiClient` using a two-level fallback:
1. User-provided value from `UserPreferencesDataStore` (via `Flow.first()` on the IO dispatcher)
2. Build-time default from `BuildConfig.OPENROUTER_API_KEY` / `BuildConfig.OPENROUTER_MODEL`

**Rationale**: The client is a `@Singleton` but the user override can change at any time from the Settings screen. Reading from DataStore on each call (not at injection) ensures the freshest value is always used without requiring re-injection.

**Alternatives considered**: Providing the resolved key as a `@Singleton` string at DI time was rejected because it would bake in the key at first injection and not respond to user changes until the process restarts.

---

## 4. Build Configuration Changes

**Decision**: Replace `ANTHROPIC_API_KEY` with two new fields:
- `OPENROUTER_API_KEY` — default: `sk-or-v1-your-openrouter-api-key-here`
- `OPENROUTER_MODEL` — default: `qwen/qwen3.5-9b`

**Rationale**: Two separate fields map cleanly to the two override dimensions the spec requires (FR-005, FR-013).

**Where set**: `local.properties` (gitignored) and `app/build.gradle.kts` `defaultConfig` block. The existing `localProps.getProperty(...)` pattern is reused.

---

## 5. Settings Screen Architecture

**Decision**: Add `SettingsViewModel` (new, `@HiltViewModel`) backed by `UserPreferencesDataStore`. The screen becomes stateful via `viewModel()`.

**Rationale**: The current `SettingsScreen` is purely stateless. Adding DataStore reads/writes requires a ViewModel to lift state out of the composable and survive recomposition.

**New UI elements** (LCARS-compliant):
- New `LcarsTextField` component in `ui/theme/components/` — wraps `OutlinedTextField` with LCARS styling (black background, orange border, white text, monospace hint).
- Two `LcarsTextField` instances in `SettingsScreen`: one for API key override, one for model override.
- Changes are persisted on each keystroke (DataStore write) for simplicity; no explicit "Save" button needed given DataStore's low write latency.

---

## 6. Error Handling

**Decision**: Map OpenRouter HTTP error codes to the existing `ConversationError` enum with no new values added.

| HTTP Status | OpenRouter meaning | Mapped to |
|---|---|---|
| 401 | Invalid / missing API key | `AI_API_ERROR` |
| 404 | Model not found | `AI_API_ERROR` |
| 429 | Rate limit exceeded | `AI_API_ERROR` |
| 5xx | Provider / OpenRouter outage | `AI_API_ERROR` |
| IOException | Network timeout / no connectivity | `NETWORK_UNAVAILABLE` |

**Rationale**: The spec requires FR-007 (no provider details exposed to UI) and SC-004 (same user-facing error states). The existing two-error-bucket approach (`AI_API_ERROR` / `NETWORK_UNAVAILABLE`) is sufficient.

---

## 7. Unit Test Impact

**Decision**: Update `SseEventParserTest` to use OpenRouter/OpenAI SSE format; all other tests remain unchanged.

**Tests affected**:
- `SseEventParserTest.extractDeltaText extracts text delta from Anthropic SSE JSON` — rename and update JSON fixture to OpenAI format.
- No other existing tests reference Anthropic-specific behavior.

**New tests required**:
- `OpenRouterConversationAiClientTest` — mock OkHttpClient, verify streaming behavior, error mapping, key/model resolution.
- `SettingsViewModelTest` — verify StateFlow emissions, DataStore write calls.
- `UserPreferencesDataStoreTest` additions — verify new key/model fields read/write correctly.

**Existing 198 tests**: All pass unchanged except the one SSE format test above, which is updated (not removed).
