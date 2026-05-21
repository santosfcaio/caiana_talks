# Implementation Plan: Voice Conversation

**Branch**: `004-voice-conversation` | **Date**: 2026-05-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-voice-conversation/spec.md`

## Summary

Build the core product loop: a voice-driven English practice conversation with an
AI tutor. The user speaks (Android `SpeechRecognizer` STT); the transcript plus a
token-efficient, prompt-cached system prompt and a 6-turn rolling window go to the
Anthropic Claude API (`claude-haiku-4-5`, streaming); the AI's reply streams back
and is spoken progressively (Android `TextToSpeech`, first sentence first). The AI
delivers inline grammar/vocabulary/fluency corrections and returns a compact
`<meta>` tail the app parses for stats. Every session ≥ 60 s is persisted (turns
written incrementally so interruptions save partial data) and updates feature
003's stats with **zero changes to 003's code** — a finished conversation is just
a `sessions` row plus `corrections` rows. The feature also adds a P3 dual-speaker
"Co-practice" mode with turn-based speaker attribution. The conversation is driven
entirely by the user's profile personalization (learning goal, themes, voice
config, CEFR hint). The AI tutor takes a fixed persona name from the voice config —
Michael (masculine/American), David (masculine/British), Mary (feminine/American),
Phoebe (feminine/British) — so the user always converses with a named tutor. While
the tutor speaks, the screen shows an animated audio-spectrum visual (synthesized
equalizer bars).

Per the project's testing convention, this feature carries an extensive unit-test
suite — see **Unit Testing Strategy** below — and tests MUST pass before the
feature is considered done.

## Technical Context

**Language/Version**: Kotlin 2.0.21 (JVM target 11), Android minSdk 26 / compileSdk 34

**Primary Dependencies**: Jetpack Compose, Room 2.6.1, Hilt 2.51.1, Navigation
Compose, kotlinx.coroutines 1.8.0; **new**: OkHttp (SSE streaming to the Anthropic
Messages API), `org.json` (bundled with Android, for `<meta>` parsing). Testing:
JUnit4, MockK 1.13.10, Turbine 1.1.0, kotlinx-coroutines-test.

**Storage**: Room database (`AppDatabase`, local on-device). Migration **v2→v3**:
extend `sessions` with conversation columns and add the `conversation_turns` table.

**Testing**: JUnit4 + MockK + Turbine + `runTest` for all pure logic, repositories,
and ViewModels; one Room `MigrationTest` (`androidTest`); Android framework classes
(`SpeechRecognizer`, `TextToSpeech`) are behind interfaces and verified manually on
the emulator.

**Target Platform**: Android API 26+, Pixel 6 emulator (`CaianaTalks_Pixel6`) /
physical device. Requires `RECORD_AUDIO` (runtime) and `INTERNET` permissions.

**Project Type**: Android mobile app (Jetpack Compose + MVVM + Hilt).

**Performance Goals**:
- AI voice output begins within 3 s of the user finishing a turn (SC-002).
- Stats updated and visible within 2 s of session end (SC-004).
- System prompt + rolling context stays under 500 tokens per turn (SC-006).
- ≥ 95% of sessions complete without an unrecoverable error (SC-007).

**Constraints**:
- AI calls MUST be token-efficient: cached static system prompt, 6-turn rolling
  window, inline concise corrections, compact `<meta>` payload (Constitution II).
- Full transcript history MUST NOT be sent to the AI (FR-014).
- No audio is stored — text transcripts only (spec clarification).
- Sessions < 60 s are discarded entirely (FR-009).
- No cloud dependency beyond the Anthropic API; API key is client-side via
  `BuildConfig` for v1 (accepted risk — see Complexity Tracking).
- Internet connectivity is required (offline conversation is out of scope).

**Scale/Scope**: ~4 new screens, ~3 new ViewModels, ~6 new service/helper classes,
1 new repository, 1 new Room entity + DAO, 1 migration. Single active profile for
single mode; exactly two profiles for dual mode. Indefinite on-device retention.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design — still passes.*

| Principle | Status | Justification |
|-----------|--------|---------------|
| I. Voice-First Interface | ✅ PASS | The conversation loop is voice-only (STT in, TTS out). Touch is used solely for Start/End controls, the mic-permission prompt, co-practice profile selection, and the summary screen — all non-conversation surfaces, which is constitutional. No text/silent conversation mode is introduced. |
| II. Token-Efficient AI | ✅ PASS (active design) | Static system block is prompt-cached (`cache_control: ephemeral`); only a 6-turn rolling window is sent (FR-014); corrections are inline and concise; structured data rides a compact `<meta>` tail (no second AI call); model tier is `claude-haiku-4-5`, the lowest-cost Claude tier. Token budget is quantified below and in contracts/ai-conversation.md against SC-006. |
| III. Pedagogical Effectiveness | ✅ PASS | The AI corrects grammar/vocabulary/fluency every turn, inline and encouragingly (FR-005/006), and enriches vocabulary aligned to the profile's goal/themes (FR-007). Cross-session difficulty adaptation is achieved by feeding feature 003's CEFR estimate into the personalization block (research R7). |
| IV. Brazilian Portuguese → English | ✅ PASS | All UI labels, nudges, and error messages are in Brazilian Portuguese; the AI converses in English and may use Portuguese only for clarity in corrections. FR-017 gently nudges the user back to English when Portuguese is detected. |
| V. Dual-Speaker Mode | ⚠️ PASS WITH DEVIATION | Co-practice mode, independent per-profile stats, and a two-person-aware AI are all implemented. v1 uses **deterministic turn-based attribution** instead of acoustic diarization — Android exposes no on-device diarization API, and cloud diarization would add the cloud dependency the constitution defers at launch. Documented in Complexity Tracking; meets SC-005's 90% target. |
| VI. Personalization & Progress Tracking | ✅ PASS | The conversation is fully driven by the profile (goal, themes, voice config, CEFR hint). Every session ≥ 60 s updates persistent on-device stats; feature 003's Progress screen surfaces them with no code change. |

**Development Workflow gate** — "no new AI call without a token-budget
justification": satisfied by the token-budget table below and in
contracts/ai-conversation.md.

**Gate result: PASS** — five principles fully satisfied; Principle V satisfied with
one documented, justified deviation (turn-based attribution for v1).

### Token budget (Constitution II / SC-006)

| Component | Est. tokens | Cached |
|-----------|-------------|--------|
| System block A (static tutor instructions + output contract) | ~150 | yes — effectively free on cache hits |
| System block B (goal, themes, CEFR hint, speaker setup) | ~60 | no |
| Rolling window — last 6 turns | ~240 | no |
| Current user input | ~30 | no |
| **Non-cached per-turn total** | **~330** | under the 500-token SC-006 budget |

Response capped at `max_tokens = 400`.

## Project Structure

### Documentation (this feature)

```text
specs/004-voice-conversation/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output — 11 resolved decisions
├── data-model.md        # Phase 1 output — entities, DAOs, migration v2→v3
├── quickstart.md        # Phase 1 output — setup, tests, manual walkthrough
├── contracts/           # Phase 1 output
│   ├── ai-conversation.md   # AI client + STT/TTS + pure-helper interfaces
│   └── conversation-ui.md   # screens, routes, ViewModel UI-state contracts
└── tasks.md             # Phase 2 output (/speckit-tasks — NOT created here)
```

### Source Code (repository root)

```text
app/src/main/java/com/caiana/talks/
├── data/local/db/
│   ├── SessionEntity.kt                 # MODIFY: + status, mode, vocabulary, co_practice_group_id
│   ├── SessionDao.kt                    # MODIFY: + update/getById/getActive/getByGroup/delete
│   ├── ConversationTurnEntity.kt        # NEW: conversation_turns table
│   ├── ConversationTurnDao.kt           # NEW: turn queries
│   └── AppDatabase.kt                   # MODIFY: v3, ConversationTurnEntity, MIGRATION_2_3
├── data/remote/
│   └── ConversationAiClient.kt          # NEW: interface + OkHttp/SSE Anthropic impl
├── data/conversation/
│   ├── SpeechRecognizerService.kt       # NEW: interface + Android SpeechRecognizer impl
│   ├── TextToSpeechService.kt           # NEW: interface + Android TextToSpeech impl
│   ├── VoiceSelector.kt                 # NEW: pure VoicePreference → locale/voice/rate
│   ├── SentenceChunker.kt               # NEW: pure streamed-text → sentences
│   ├── AiResponseParser.kt              # NEW: pure <say>/<meta> extraction
│   ├── RollingWindow.kt                 # NEW: pure 6-turn windowing
│   ├── SystemPromptBuilder.kt           # NEW: pure ConversationConfig → SystemPrompt
│   └── SessionDurationPolicy.kt         # NEW: pure 60 s gate predicate
├── data/repository/
│   └── ConversationRepository.kt        # NEW: interface + impl — session/turn/correction persistence
├── domain/model/
│   ├── SessionMode.kt                   # NEW: SINGLE | DUAL
│   ├── SessionStatus.kt                 # NEW: ACTIVE | COMPLETED | PARTIAL
│   ├── AiPersona.kt                     # NEW: persona name per (gender × accent)
│   ├── ConversationConfig.kt            # NEW: ConversationConfig, ParticipantInfo
│   ├── ConversationMessage.kt           # NEW: rolling-window message
│   ├── AiStreamEvent.kt                 # NEW: TextDelta/SayEnded/Completed/Failed
│   ├── AiResponseMeta.kt                # NEW: corrections, vocabulary, pt flag
│   └── ConversationError.kt             # NEW: typed error enum
├── ui/conversation/
│   ├── ConversationScreen.kt            # NEW: active-session voice UI
│   ├── ConversationViewModel.kt         # NEW: STT→AI→TTS orchestration + state machine
│   ├── AudioSpectrum.kt                 # NEW: animated equalizer bars (shown while AI speaks) + pure SpectrumWaveform helper
│   ├── CoPracticeSetupScreen.kt         # NEW: dual-speaker profile selection
│   ├── CoPracticeSetupViewModel.kt      # NEW
│   ├── SessionSummaryScreen.kt          # NEW: post-session summary
│   └── SessionSummaryViewModel.kt       # NEW
├── ui/home/
│   └── HomeScreen.kt                    # MODIFY: + "Iniciar conversa" + "Co-practice" buttons
├── ui/navigation/
│   └── AppNavGraph.kt                   # MODIFY: + conversation/coPracticeSetup/sessionSummary routes
└── di/
    ├── DatabaseModule.kt                # MODIFY: provide ConversationTurnDao, add MIGRATION_2_3
    ├── RepositoryModule.kt              # MODIFY: bind ConversationRepository
    └── ConversationModule.kt            # NEW: provide AI client, STT/TTS services, OkHttpClient

app/src/test/java/com/caiana/talks/conversation/    # NEW unit-test package (see Unit Testing Strategy)
app/src/androidTest/java/com/caiana/talks/          # NEW: MigrationTest (v2→v3)
app/src/main/AndroidManifest.xml                    # MODIFY: RECORD_AUDIO + INTERNET permissions
app/build.gradle.kts                                # MODIFY: OkHttp dep, BuildConfig API-key field
gradle/libs.versions.toml                           # MODIFY: okhttp version + library entries
```

**Structure Decision**: Android single-project layout, extending the existing
conventions. A new `ui/conversation/` package sits parallel to `ui/home/` and
`ui/stats/`. The pure conversation helpers and the Android service wrappers live
in a new `data/conversation/` package; the AI network client lives in a new
`data/remote/` package. The data layer extends `data/local/db/` and
`data/repository/` without restructuring. This isolation — every Android-framework
and network dependency behind an interface — is what makes the heavy unit-test
suite below possible.

## Unit Testing Strategy

*The user explicitly requested an extensive unit-test suite ("Planeje a criação de
muitos testes unitários também"). Per project convention, tests are written and
passing before the feature is considered done. This section is a first-class part
of the plan and feeds directly into `/speckit-tasks`.*

### Principle: isolate the untestable, then test everything else exhaustively

Every Android-framework dependency (`SpeechRecognizer`, `TextToSpeech`) and the
network (`ConversationAiClient`) sits behind an interface. As a result the entire
conversation logic — pure helpers, the repository, and all ViewModels — runs on
the plain JVM test runtime with MockK fakes. Android-framework wrappers themselves
are thin and verified manually on the emulator (quickstart §3–4).

### Suite 1 — Pure logic (no mocks, exhaustive; the "many tests" core)

| Test class | Under test | Representative cases |
|------------|-----------|----------------------|
| `SentenceChunkerTest` | `SentenceChunker` | split on `.`/`!`/`?`/newline; **no** split on `Mr.`, `e.g.`, `i.e.`, decimals `3.5`; multi-sentence delta; sentence split across deltas; `flush()` remainder; empty input |
| `AiResponseParserTest` | `AiResponseParser` | well-formed `<say>`+`<meta>`; missing `<meta>`; malformed JSON; unknown `cat` dropped; empty arrays; `pt=true`; `<say>` not yet closed |
| `RollingWindowTest` | `RollingWindow` | fewer than 6 turns; exactly 6; more than 6 (eviction); ordering oldest-first; empty list |
| `SystemPromptBuilderTest` | `SystemPromptBuilder` | static block byte-identical across configs; goal/themes encoded; CEFR omitted when null; persona name present in Block B; dual-mode includes both names; token-estimate within budget |
| `AiPersonaTest` | `AiPersona` | all four (gender × accent) combinations resolve — masculine/american→Michael, masculine/british→David, feminine/american→Mary, feminine/british→Phoebe; `of()` resolver exhaustive |
| `VoiceSelectorTest` | `VoiceSelector` | accent→locale (US/UK); rate→float (slow/normal/fast); gender→voice pick; fallback when no gendered voice |
| `SessionDurationPolicyTest` | `SessionDurationPolicy` | 59 s discard, 60 s keep, 61 s keep (boundary); negative/zero duration guard |
| `PortugueseNudgeTest` | FR-017 nudge mapping | `meta.pt=true` → nudge visible; `pt=false` → not |
| `SpectrumWaveformTest` | `SpectrumWaveform` | `speaking=false` → all bars at the rest/floor height; `speaking=true` → every height within `[0f,1f]` and bars differ from each other (organic); deterministic for a given `timeMs`; respects `barCount` |

### Suite 2 — Repository (MockK fakes for DAOs)

| Test class | Under test | Representative cases |
|------------|-----------|----------------------|
| `ConversationRepositoryTest` | `ConversationRepositoryImpl` | `startSession` single → 1 row, `active`; `startSession` dual → 2 rows + shared `coPracticeGroupId`; `appendTurn` writes turn + corrections + vocabulary to the right profile row; `finalizeSession` < 60 s → deletes session+turns, `DISCARDED_TOO_SHORT`; ≥ 60 s deliberate → `completed`; ≥ 60 s interrupted → `partial`; dual finalize → one `SessionSummary` per participant; `recoverDanglingSessions` finalizes a stray `active` row as partial (or discards < 60 s) |

### Suite 3 — ViewModels (MockK fakes for repository + services; Turbine for state)

| Test class | Under test | Representative cases |
|------------|-----------|----------------------|
| `ConversationViewModelTest` | `ConversationViewModel` | phase machine `IDLE→LISTENING→THINKING→SPEAKING→LISTENING`; STT `Final` triggers AI call; AI stream feeds chunker→TTS; completed turn persisted via repository; `onEndSession` ≥ 60 s → summary route; `onEndSession` < 60 s → home route; `onAppBackgrounded` → finalize partial; `Silence` event → re-prompt, stays `LISTENING`; `MIC_PERMISSION_DENIED`/`NETWORK_UNAVAILABLE`/`AI_API_ERROR` → error state, no crash; `onRetry` resumes; `meta.pt=true` → nudge; dual mode alternates `activeSpeakerName` |
| `CoPracticeSetupViewModelTest` | `CoPracticeSetupViewModel` | `canStart=false` until two profiles; `canStart=false` when same profile twice; `canStart=true` for two distinct; profile list loaded |
| `SessionSummaryViewModelTest` | `SessionSummaryViewModel` | single → 1 participant summary; dual → 2 loaded by group id; duration label `"Xmin"` vs `"Xh Ymin"`; vocabulary highlights surfaced |

### Suite 4 — Migration (instrumented, `androidTest`)

| Test class | Under test | Cases |
|------------|-----------|-------|
| `MigrationTest` | `MIGRATION_2_3` | a v2 DB with a feature-003 session migrates to v3 without loss; new columns get defaults; `conversation_turns` table exists and is writable |

### Out of unit-test scope (manual / emulator only — quickstart §3–4)

The Android impls `AndroidSpeechRecognizerService` and `AndroidTextToSpeechService`
(thin wrappers over framework classes) and `AnthropicConversationAiClient`'s real
HTTP path. The client's **SSE-parsing** logic is extracted into a pure
`SseEventParser` and unit-tested with canned event strings; only the live socket
is left to manual verification. The `AudioSpectrum` composable's Canvas rendering
is emulator-verified — its bar-height logic (`SpectrumWaveform`) is pure and unit-
tested above.

### Tooling

JUnit4, MockK 1.13.10, Turbine 1.1.0, kotlinx-coroutines-test (`runTest`) — all
already in the project. No new test dependency is required. Run with
`.\gradlew testDebugUnitTest` (see quickstart §2).

## Complexity Tracking

| Violation / deviation | Why needed | Simpler alternative rejected because |
|-----------------------|------------|--------------------------------------|
| **Principle V — turn-based attribution instead of acoustic speaker diarization** (v1) | Android exposes no on-device speaker-diarization API. Dual-speaker mode (FR-011/012, P3) still needs deterministic per-speaker attribution to keep each profile's stats fair. | True acoustic diarization requires a cloud STT service (e.g. Google Cloud Speech-to-Text), which would introduce exactly the cloud dependency the constitution defers at launch ("no cloud dependency beyond the AI conversation API"). Turn-based attribution with an on-screen turn indicator is deterministic when users follow the prompt, meets SC-005's 90% target, and matches the spec's "best-effort for v1" scoping. Acoustic diarization is the planned post-v1 enhancement. |
| **Client-side API key via `BuildConfig`** (v1) | The app must call the Anthropic API and the constitution forbids extra cloud infrastructure at launch. | A backend proxy is the secure design but adds a server to operate — the cloud dependency the constitution defers. The key is kept in untracked `local.properties`; APK-extraction risk is accepted for the pre-distribution v1 and flagged for post-v1 hardening (research R2). |

## Progress

- [x] Phase 0 — research.md (11 decisions, all NEEDS CLARIFICATION resolved)
- [x] Phase 1 — data-model.md, contracts/ (ai-conversation, conversation-ui), quickstart.md
- [x] Phase 1 — agent context (CLAUDE.md plan reference) updated
- [x] Constitution re-check after design — PASS (one documented deviation)
- [ ] Phase 2 — tasks.md (generated by `/speckit-tasks`, not this command)
