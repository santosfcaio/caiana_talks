# Research: Voice Conversation

**Phase**: 0 — Outline & Research
**Feature**: 004-voice-conversation
**Date**: 2026-05-20

This document resolves every `NEEDS CLARIFICATION` item from the Technical Context
and records the technology decisions for the voice-conversation feature.

---

## R1 — AI conversation provider

**Decision**: Anthropic Claude API, model `claude-haiku-4-5`, called over streaming
SSE. The app talks to the Messages API through a thin OkHttp + SSE client wrapped
behind a `ConversationAiClient` interface.

**Rationale**:
- Constitution Principle II makes cost-per-token a first-class selection criterion.
  Haiku 4.5 is the cheapest current Claude tier and is more than capable of
  conversational tutoring + inline correction.
- The Messages API supports token-level streaming, which FR-004 requires
  (TTS must start on the first finished sentence).
- Anthropic prompt caching lets the static system-prompt block be cached
  (`cache_control: {"type": "ephemeral"}`), directly satisfying FR-015 and
  Principle II's "system prompts MUST be cached where the provider supports it".
- A hand-written OkHttp/SSE client (vs. the official `anthropic-java` SDK) keeps
  the dependency surface tiny on Android, gives full control of caching headers,
  and — most importantly for this feature's test goal — is trivially mockable: the
  whole AI surface collapses to one interface with one streaming function.

**Alternatives considered**:
- *OpenAI Realtime API (voice-to-voice)*: bypasses the Android STT/TTS pipeline,
  costs significantly more per minute, and removes per-voice control (gender/accent
  come from the Android TTS engine). Rejected on cost (Principle II).
- *Google Gemini Flash*: competitive token cost, but no prompt-caching parity with
  the split-system-prompt strategy below. Rejected to keep the caching design.
- *Official `anthropic-java` SDK*: pulls a larger dependency tree, needs care for
  Android desugaring, and is harder to fake cleanly in unit tests. Rejected in
  favor of the thin client.

---

## R2 — API key management (v1)

**Decision**: The Anthropic API key is stored in `local.properties` (untracked) and
surfaced to code via a generated `BuildConfig.ANTHROPIC_API_KEY` field. The app
calls the Anthropic API directly; no proxy server.

**Rationale**:
- Constitution: "No cloud dependency at launch beyond the AI conversation API."
  A proxy would add exactly the cloud infrastructure the constitution defers.
- `local.properties` keeps the key out of version control and out of the APK
  source tree; it is injected at build time only.

**Accepted risk**: A client-side key can be extracted from a distributed APK. For
v1 (individual-user, pre-distribution) this is an accepted tradeoff, documented
here and in the plan's Complexity Tracking. A backend proxy is the planned
post-v1 hardening step.

**Files touched**: `local.properties` (new key line), `app/build.gradle.kts`
(`buildConfigField` + `buildFeatures { buildConfig = true }`).

---

## R3 — Speech-to-Text (STT)

**Decision**: Android's native `android.speech.SpeechRecognizer` with
`RecognizerIntent.EXTRA_LANGUAGE = "en-US"`, wrapped behind a
`SpeechRecognizerService` interface.

**Rationale**:
- Constitution names Android `SpeechRecognizer` as the default STT engine.
- It is free (no per-minute STT cost) and on-device-capable on modern devices.
- The interface wrapper makes the orchestration layer fully unit-testable with a
  fake recognizer — the Android class itself cannot run on the JVM test runtime.

**Notes / known limits**:
- `SpeechRecognizer` returns partial + final results; the service exposes both as
  a callback/`Flow`. End-of-speech is signalled by `onEndOfSpeech` / final result.
- Silence handling (Edge Case): a no-speech timeout > 10 s emits a
  `SttResult.Silence` event the ViewModel turns into a gentle re-prompt.
- Accuracy for Brazilian-accented English is "good enough" for v1; a third-party
  STT swap is explicitly out of scope unless field testing proves it necessary
  (spec Assumptions).

---

## R4 — Text-to-Speech (TTS) and voice configuration

**Decision**: Android's native `android.speech.tts.TextToSpeech`, wrapped behind a
`TextToSpeechService` interface. Profile voice preferences map to TTS parameters:

| Profile preference | TTS mapping |
|--------------------|-------------|
| `VoiceAccent.AMERICAN` | `Locale.US` (`en-US`) |
| `VoiceAccent.BRITISH`  | `Locale.UK` (`en-GB`) |
| `VoiceGender.FEMININE` / `MASCULINE` | pick a `Voice` from `tts.voices` matching the locale whose name/metadata indicates the gender; deterministic fallback to the locale default if none found |
| `SpeechRate.SLOW` / `NORMAL` / `FAST` | `tts.setSpeechRate(0.8f / 1.0f / 1.4f)` |

**Rationale**:
- Constitution names Android `TextToSpeech` as an accepted TTS path and requires
  gender/accent/rate to be configurable — all three map cleanly to the API.
- Free, offline-capable for playback, and avoids any per-character TTS billing
  (Principle II).
- The gender→`Voice` resolution is non-trivial and lives in a pure
  `VoiceSelector` function so it can be unit-tested exhaustively without a device.

**Streaming playback**: AI text is fed to TTS sentence-by-sentence with
`TextToSpeech.QUEUE_ADD` so audio begins on the first finished sentence and
continues progressively (FR-004).

---

## R5 — Progressive streaming pipeline (STT → AI → TTS)

**Decision**: A staged pipeline:

1. `SpeechRecognizerService` produces the final user transcript.
2. `ConversationAiClient` streams the AI response as text deltas over SSE.
3. `SentenceChunker` buffers deltas and emits complete sentences as soon as a
   sentence-terminating boundary (`.`, `!`, `?`, plus newline) is detected,
   guarding against abbreviations (`Mr.`, `e.g.`, decimals).
4. Each emitted sentence is enqueued to `TextToSpeechService` with `QUEUE_ADD`.

**Rationale**: Satisfies FR-004 (TTS starts on first finished sentence) and
SC-002 (audio within 3 s of the user finishing). `SentenceChunker` is a pure,
stateful function — a prime unit-test target (see plan's test strategy).

**Alternatives considered**: Waiting for the full AI response before any TTS —
rejected, violates FR-004 and SC-002. Fixed-size chunking — rejected, produces
unnatural mid-word/mid-clause audio breaks.

---

## R6 — AI response format: spoken text + structured metadata

**Decision**: The AI returns a single streamed message split into two delimited
sections:

```
<say>Natural spoken English here, with corrections woven in inline.</say>
<meta>{"corrections":[{"cat":"GRAMMAR","note":"..."}],"vocab":["..."],"pt":false}</meta>
```

- Only the text inside `<say>…</say>` is forwarded to TTS. The chunker stops
  feeding TTS at `</say>`.
- `<meta>` is a compact JSON tail parsed by `AiResponseParser` for stats
  (corrections by category, vocabulary introduced) and the `pt` flag (user spoke
  Portuguese — drives FR-017).

**Rationale**:
- Keeps corrections *inline and spoken* (Principle III, FR-006) while still giving
  the app machine-readable data for stats (FR-008/FR-009) — without a second AI
  call (Principle II).
- Compact keys (`cat`, `note`, `pt`) minimise tokens (Principle II, SC-006).
- `AiResponseParser` handles a missing/malformed `<meta>` block gracefully
  (treat as zero corrections) — another pure unit-test target.

**Alternatives considered**: A separate AI call for structured correction data —
rejected, doubles token cost (Principle II). Tool/function calling — rejected,
adds tokens and streaming complexity for a fixed-shape payload.

---

## R7 — Token-efficient system prompt and prompt caching

**Decision**: Two-block system prompt:

- **Block A — static tutor instructions** (identical for every user and every
  call): role, Brazilian-PT-speaker focus, correction behaviour, the `<say>/<meta>`
  output contract. Marked with `cache_control: {"type": "ephemeral"}`.
- **Block B — per-session personalization**: the AI's persona name (Michael /
  David / Mary / Phoebe, derived from the voice config — see R12), learning goal,
  selected themes, current CEFR level hint (read from feature 003's
  `StatsRepository`), and the speaker setup (single vs. dual). Small; rebuilt per
  session, constant within it.

The rolling conversation window (R8) is sent as `messages`, never in the system
prompt.

**Rationale**:
- Block A is byte-identical across all sessions and users → high cache-hit rate,
  satisfying FR-015 and Principle II.
- Folding the CEFR level into Block B gives the AI cross-session difficulty
  adaptation (Principle III) at near-zero token cost.
- `SystemPromptBuilder` is a pure function: given `ProfilePreferences` (+ optional
  CEFR + mode) it returns the prompt blocks — fully unit-testable, including a
  token-budget assertion.

---

## R8 — Rolling conversation context window

**Decision**: Send only the **last 6 turns** (≈3 user/AI exchange pairs) plus the
current user input as `messages`. Older turns are dropped from the AI payload but
remain persisted in full in the `conversation_turns` table.

**Rationale**:
- FR-014 forbids sending full history; Principle II requires windowing.
- Budget check against SC-006 (< 500 tokens for system + context per turn):
  Block A ~150 tokens (cached → effectively free on hits), Block B ~60 tokens,
  6 windowed turns ~240 tokens, current input ~30 tokens → ~330 non-cached tokens.
  Comfortably inside budget with headroom.
- 6 turns preserves enough conversational coherence for natural follow-ups while
  staying cheap.

**Implementation**: `RollingWindow` — a pure helper that, given the full turn list,
returns the last N as API `messages`. `N = 6` is a named constant so it can be
tuned. Pure → unit-tested for eviction, ordering, and the under-N case.

---

## R9 — Dual-speaker attribution (Co-practice, P3)

**Decision**: v1 dual-speaker mode uses **deterministic turn-based attribution**,
not acoustic diarization. The Co-practice session UI shows whose turn it is and
alternates the active speaker each turn; every recognized utterance is attributed
to the speaker whose turn is active. The AI is told it is conversing with two
named participants and addresses corrections to the active speaker.

**Rationale**:
- Android exposes **no** on-device speaker-diarization API. The only true
  diarization path is a cloud STT service (e.g. Google Cloud Speech-to-Text),
  which would add the very cloud dependency the constitution defers at launch.
- Spec clarifications + Assumptions explicitly scope diarization as "best-effort
  for v1" and "may be imperfect".
- With explicit turn-taking, attribution is effectively deterministic when users
  follow the on-screen prompt — comfortably meeting SC-005's 90% target.
- This is a **documented deviation** from Principle V's "MUST perform speaker
  diarization"; see the plan's Complexity Tracking. Acoustic diarization is the
  planned post-v1 enhancement.

**Persistence impact**: see data-model.md — a dual session writes one
`SessionEntity` row per participant, linked by a shared `co_practice_group_id`, so
feature 003's per-profile stats keep working unchanged.

---

## R10 — Session lifecycle, persistence, and the 60-second rule

**Decision**: The session is a state machine —
`Idle → Listening → Thinking → Speaking → Listening … → Ended` — owned by
`ConversationViewModel`. `ConversationRepository` persists session, turns, and
corrections.

- A session ends via deliberate "End Session" **or** interruption (app backgrounded
  via `ON_STOP` lifecycle, or process death recovery on next launch).
- On end, duration = `endedAt − startedAt`:
  - **< 60 s** → the session and its turns are **discarded** (deleted / never
    committed); no stats update; no summary screen; return to Home (FR-009,
    spec Edge Case).
  - **≥ 60 s** → persisted with `status = completed` (deliberate end) or
    `status = partial` (interruption); stats updated; summary screen shown.
- Turns are written to the DB **as they complete** (not only at session end), so an
  interruption naturally leaves a correct partial record (FR-008).

**Rationale**: Writing turns incrementally is the simplest design that makes
"interruption saves partial data" fall out for free, and it makes process-death
recovery a plain "find the dangling `active` session" query. The 60-second gate is
a single pure predicate (`SessionDurationPolicy`) — unit-tested at the boundary
(59 s, 60 s, 61 s).

---

## R11 — Permissions and error handling

**Decision**:
- `RECORD_AUDIO` — runtime permission, requested when the user first starts a
  session. Denial → friendly Portuguese message, no crash, return to Home (FR-016).
- `INTERNET` — normal manifest permission for the AI call.
- AI/network errors and STT engine errors surface as typed
  `ConversationError` UI states (mic unavailable, network down, API error),
  each with a Portuguese user-facing message (FR-016, SC-007).
- Out-of-storage during turn persistence → caught, session downgraded to whatever
  was already saved, user informed.

**Rationale**: SC-007 requires 95% of sessions to end without an unrecoverable
error. Modelling errors as explicit ViewModel states (not exceptions thrown to the
UI) makes every branch unit-testable and keeps the UI declarative.

---

## R12 — AI tutor persona names

**Decision**: The AI tutor has a fixed persona name determined by the profile's
voice configuration — the `(VoiceGender × VoiceAccent)` pair maps to exactly one
named persona:

| Gender | Accent | Persona |
|--------|--------|---------|
| Masculine | American | Michael |
| Masculine | British | David |
| Feminine | American | Mary |
| Feminine | British | Phoebe |

This mapping lives in an `AiPersona` enum (`domain/model/AiPersona.kt`) with a
total `of(gender, accent)` resolver. The persona name is injected into system
prompt Block B (R7) so the AI introduces and refers to itself by that name, and it
is surfaced on the conversation screen (`ConversationUiState.aiPersonaName`).

**Rationale**:
- A named tutor makes the practice feel like a relationship with a consistent
  conversation partner rather than a faceless model — reinforcing engagement
  (Principle VI personalization).
- Tying the name to the voice config keeps it deterministic and free: no extra
  user setting, no extra storage column, no AI cost. The name is derived, not
  persisted.
- `AiPersona` is a pure mapping → exhaustively unit-testable across all four
  combinations.

---

## R13 — Audio spectrum visualization while the AI speaks

**Decision**: While the conversation is in the `SPEAKING` phase, the conversation
screen renders an animated audio-spectrum visual — a row of vertical equalizer
bars (Canvas-drawn) that pulse organically, in the stylized Dribbble look the user
referenced. The bars are a **synthesized decorative animation**, not driven by real
TTS audio.

Bar heights come from a pure helper `SpectrumWaveform.barHeights(timeMs, barCount,
speaking)` returning normalized heights in `[0f, 1f]`. The `AudioSpectrum`
composable samples this helper on each animation frame and draws the bars; when
`speaking == false` every bar rests at a minimal floor height. Each bar follows its
own period/phase so the row looks organic rather than uniform; liveliness is keyed
to the streaming-sentence cadence (a fresh sentence enqueued to TTS gives the bars
a brief amplitude boost).

**Rationale**:
- Capturing real TTS output requires Android's `Visualizer` on the session-0
  output mix — a global capture whose reliability varies by device, that needs an
  extra `MODIFY_AUDIO_SETTINGS` permission, and that cannot be unit-tested. The
  decorative approach avoids all three, matching the constitution's simplicity
  bias and this feature's heavy-testing goal.
- The referenced Dribbble shot is itself a stylized motion design, not a real FFT
  readout — a synthesized animation faithfully reproduces that look.
- Extracting bar heights into a pure function keeps the visual fully
  unit-testable; the composable is a thin Canvas renderer verified on the emulator.

**Alternatives considered**: Audio-reactive bars via the `Visualizer` API —
rejected for the device-reliability, extra-permission, and untestability reasons
above. A pre-rendered Lottie/GIF animation — rejected; it can't rest/resume with
the `SPEAKING` phase or react to sentence cadence, and adds a dependency.

---

## Resolved unknowns summary

| Unknown (from Technical Context) | Resolution |
|----------------------------------|------------|
| AI provider | Claude API, `claude-haiku-4-5`, streaming SSE (R1) |
| API key handling | `local.properties` → `BuildConfig`, direct calls (R2) |
| STT engine | Android `SpeechRecognizer`, `en-US` (R3) |
| TTS engine + voice mapping | Android `TextToSpeech`; locale/voice/rate mapping (R4) |
| Streaming approach | Sentence-chunked STT→AI→TTS pipeline (R5) |
| Correction data extraction | `<say>`/`<meta>` delimited response (R6) |
| System prompt / caching | Two-block prompt, static block cached (R7) |
| Rolling window size | Last 6 turns (R8) |
| Speaker diarization | Turn-based attribution for v1 (R9) |
| Session lifecycle / 60 s rule | State machine + incremental persist + 60 s gate (R10) |
| Permissions / errors | `RECORD_AUDIO` runtime, typed error states (R11) |
| AI persona naming | Michael / David / Mary / Phoebe per voice config (R12) |
| Audio spectrum visual | Synthesized animated equalizer bars during SPEAKING (R13) |
