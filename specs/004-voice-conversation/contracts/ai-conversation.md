# Contract: AI Conversation Interface

**Phase**: 1 — Design & Contracts
**Feature**: 004-voice-conversation

This contract defines the boundary between the app and the Anthropic Claude API,
plus the internal service interfaces that make the conversation pipeline
testable. Every cross-component boundary is an interface so the orchestration
layer (`ConversationViewModel`) can be unit-tested with fakes.

---

## 1. ConversationAiClient

The single seam to the AI provider. The OkHttp/SSE implementation is the only
class that knows about HTTP; everything above it is mockable.

```kotlin
interface ConversationAiClient {
    /**
     * Streams one AI turn. [system] is the cached static block + personalization
     * block (research R7); [window] is the rolling context (research R8);
     * [userInput] is the latest STT transcript.
     * Emits TextDelta* → SayEnded → Completed, or Failed on error.
     */
    fun streamReply(
        system: SystemPrompt,
        window: List<ConversationMessage>,
        userInput: String
    ): Flow<AiStreamEvent>
}
```

### Wire request (Anthropic Messages API, streaming)

`POST https://api.anthropic.com/v1/messages`

Headers: `x-api-key: <BuildConfig.ANTHROPIC_API_KEY>`,
`anthropic-version: 2023-06-01`, `content-type: application/json`.

```json
{
  "model": "claude-haiku-4-5",
  "max_tokens": 400,
  "stream": true,
  "system": [
    { "type": "text", "text": "<STATIC BLOCK A>", "cache_control": { "type": "ephemeral" } },
    { "type": "text", "text": "<PERSONALIZATION BLOCK B>" }
  ],
  "messages": [
    { "role": "user", "content": "<windowed turn>" },
    { "role": "assistant", "content": "<windowed turn>" },
    { "role": "user", "content": "<latest userInput>" }
  ]
}
```

- `system` block A carries `cache_control` → prompt caching (FR-015, Principle II).
- `messages` carries only the last 6 turns + current input (FR-014, R8).
- `max_tokens = 400` caps response cost; the `<say>+<meta>` payload fits well
  inside it.

### Response contract

The streamed assistant message MUST follow this exact shape:

```
<say>Spoken English. Corrections woven in naturally and encouragingly.</say>
<meta>{"corrections":[{"cat":"GRAMMAR","note":"..."}],"vocab":["word"],"pt":false}</meta>
```

- `<say>…</say>` — spoken to the user via TTS. Inline, concise corrections only
  (FR-006, Principle III).
- `<meta>{…}</meta>` — compact JSON, never spoken. Keys:
  - `corrections`: array of `{ "cat": GRAMMAR|VOCABULARY|FLUENCY, "note": string }`.
  - `vocab`: array of strings — vocabulary the AI introduced this turn.
  - `pt`: boolean — `true` if the user's input was (partly) Portuguese (FR-017).

### Error mapping

| Condition | Emitted event |
|-----------|---------------|
| No connectivity | `Failed(NETWORK_UNAVAILABLE)` |
| HTTP 4xx/5xx, or malformed SSE | `Failed(AI_API_ERROR)` |
| Stream ends with no `<meta>` block | `Completed(AiResponseMeta(emptyList(), emptyList(), false))` — graceful |

---

## 2. SystemPromptBuilder

Pure function — no I/O. Prime unit-test target.

```kotlin
interface SystemPromptBuilder {
    fun build(config: ConversationConfig): SystemPrompt
}

data class SystemPrompt(
    val staticBlock: String,        // Block A — identical for every user/call → cached
    val personalizationBlock: String // Block B — per-session
)
```

**Block A** (static, ~150 tokens) MUST contain: the English-tutor role for
Brazilian-Portuguese speakers; the encouraging-correction directive; the
`<say>/<meta>` output contract.

**Block B** (per-session, ~60 tokens) MUST encode: the AI's persona name
(`config.persona.displayName` — Michael / David / Mary / Phoebe, derived from the
voice config), an instruction for the AI to introduce and refer to itself by that
name, the learning goal, selected themes, CEFR hint (omitted when
`cefrHint == null`), and — in dual mode — both participant names with the
turn-based-attribution instruction.

**Contract guarantee**: `staticBlock` is byte-identical for every `config` (high
cache-hit rate). `staticBlock + personalizationBlock` token estimate stays within
the SC-006 budget.

---

## 3. SpeechRecognizerService (STT)

```kotlin
interface SpeechRecognizerService {
    fun listen(languageTag: String = "en-US"): Flow<SttEvent>
    fun stop()
}

sealed interface SttEvent {
    data class Partial(val text: String) : SttEvent
    data class Final(val text: String) : SttEvent
    data object Silence : SttEvent                       // > 10 s no speech
    data class Failed(val error: ConversationError) : SttEvent  // MIC_UNAVAILABLE
}
```

Android implementation wraps `android.speech.SpeechRecognizer`. Not unit-tested
on the JVM — exercised via the emulator (quickstart). The orchestration layer is
tested against a fake `SpeechRecognizerService`.

---

## 4. TextToSpeechService (TTS)

```kotlin
interface TextToSpeechService {
    suspend fun configure(voice: VoicePreference)        // applies locale/voice/rate
    fun enqueue(sentence: String)                        // QUEUE_ADD — progressive playback
    fun stop()
    val isSpeaking: StateFlow<Boolean>
}
```

Android implementation wraps `android.speech.tts.TextToSpeech`. The
`VoicePreference → (Locale, Voice, rate)` mapping is extracted into a pure
`VoiceSelector` (see below) so it is fully unit-testable.

```kotlin
object VoiceSelector {
    fun localeFor(accent: VoiceAccent): Locale
    fun rateFor(rate: SpeechRate): Float
    fun pickVoice(candidates: List<VoiceDescriptor>, gender: VoiceGender, locale: Locale): VoiceDescriptor?
}
```

---

## 5. SentenceChunker

Pure, stateful helper turning streamed text deltas into TTS-ready sentences
(research R5).

```kotlin
class SentenceChunker {
    /** Feed a delta; returns any newly-completed sentences. */
    fun accept(delta: String): List<String>
    /** Flush any buffered remainder at stream end. */
    fun flush(): List<String>
}
```

**Contract**: a sentence is emitted on `.`, `!`, `?`, or newline; common
abbreviations (`Mr.`, `Mrs.`, `e.g.`, `i.e.`) and decimal points (`3.5`) MUST NOT
trigger a premature split. Pure → exhaustively unit-tested.

---

## 6. AiResponseParser

Pure helper extracting structured data from the streamed response (research R6).

```kotlin
object AiResponseParser {
    fun extractSayText(raw: String): String
    fun parseMeta(raw: String): AiResponseMeta   // tolerant of missing/malformed <meta>
}
```

**Contract**: a missing or malformed `<meta>` block yields
`AiResponseMeta(emptyList(), emptyList(), false)` — never an exception. Unknown
`cat` values are dropped, not crashed on. Pure → unit-tested for the happy path
and every malformed shape.

---

## 7. RollingWindow

Pure helper enforcing FR-014 / research R8.

```kotlin
object RollingWindow {
    const val MAX_TURNS = 6
    /** Returns the last MAX_TURNS messages, oldest-first, as API messages. */
    fun take(allTurns: List<ConversationMessage>): List<ConversationMessage>
}
```

---

## Token budget (SC-006 / Constitution Principle II)

| Component | Est. tokens | Cached? |
|-----------|-------------|---------|
| System block A (static) | ~150 | yes — free on cache hit |
| System block B (personalization) | ~60 | no |
| Rolling window (6 turns) | ~240 | no |
| Current user input | ~30 | no |
| **Non-cached total per turn** | **~330** | within the <500 SC-006 budget |

Response is capped at `max_tokens = 400`. Model tier `claude-haiku-4-5` is the
lowest-cost Claude tier (Principle II: favour cost per token).
