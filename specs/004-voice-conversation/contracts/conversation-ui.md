# Contract: Conversation UI & Navigation

**Phase**: 1 — Design & Contracts
**Feature**: 004-voice-conversation

Defines the screens, navigation routes, and ViewModel UI-state contracts for the
voice-conversation feature. All screens are Jetpack Compose + Hilt, following the
existing `ui/` MVVM conventions.

---

## Navigation routes (added to `AppNavGraph`, `Home` graph)

| Route | Screen | Entry point |
|-------|--------|-------------|
| `conversation` | `ConversationScreen` | Home → "Iniciar conversa" (single-speaker) |
| `coPracticeSetup` | `CoPracticeSetupScreen` | Home → "Co-practice" |
| `conversation?group={id}` | `ConversationScreen` | `CoPracticeSetupScreen` after two profiles confirmed |
| `sessionSummary/{groupId}` | `SessionSummaryScreen` | shown when a session finalizes ≥ 60 s |

A session under 60 s navigates straight back to `home` with no summary
(FR-009 / spec Edge Case).

`HomeScreen` adds two buttons above the existing "Ver meu progresso":
- **"Iniciar conversa"** → `conversation`
- **"Co-practice"** → `coPracticeSetup`

---

## ConversationScreen + ConversationViewModel

The orchestration core: drives the STT → AI → TTS pipeline and the session state
machine (research R10).

```kotlin
data class ConversationUiState(
    val phase: Phase = Phase.IDLE,
    val mode: SessionMode = SessionMode.SINGLE,
    val aiPersonaName: String = "",               // Michael / David / Mary / Phoebe — from the voice config
    val activeSpeakerName: String? = null,        // dual mode: whose turn it is
    val liveTranscript: String = "",              // partial STT of the current utterance
    val turns: List<TurnUi> = emptyList(),         // completed turns, for on-screen history
    val elapsedSeconds: Int = 0,
    val portugueseNudgeVisible: Boolean = false,   // FR-017
    val error: ConversationError? = null
) {
    enum class Phase { IDLE, LISTENING, THINKING, SPEAKING, ENDED }
}

data class TurnUi(
    val speakerName: String,
    val userText: String,
    val aiText: String
)
```

**ViewModel surface**:

```kotlin
fun onStart()                       // mic permission already granted; begins LISTENING
fun onUserFinishedSpeaking()        // STT Final → THINKING
fun onEndSession()                  // user taps "Encerrar sessão"
fun onPermissionDenied()            // → error = MIC_PERMISSION_DENIED
fun onAppBackgrounded()             // lifecycle ON_STOP → finalize as partial
fun onRetry()                       // clears a recoverable error, resumes LISTENING
```

**Behavioural contract**:
- `Phase` transitions follow research R10's state machine exactly.
- While `phase == SPEAKING`, the screen renders the `AudioSpectrum` composable —
  animated equalizer bars (research R13). The bars rest at a floor height in every
  other phase. The visual is purely presentational; it adds no field to
  `ConversationUiState` (it is driven by `phase` alone).
- On `SttEvent.Silence`, the ViewModel shows a gentle re-prompt and stays
  `LISTENING` (no error).
- When `AiResponseMeta.userSpokePortuguese == true`, set
  `portugueseNudgeVisible = true` for that turn (FR-017) — the conversation
  continues, it is not an error.
- Each completed turn is persisted immediately via `ConversationRepository`
  (incremental persistence, research R10).
- On `onEndSession` / `onAppBackgrounded`, the ViewModel calls
  `ConversationRepository.finalizeSession(...)` and routes per `SessionResult`:
  `SAVED` → `sessionSummary/{groupId}`; `DISCARDED_TOO_SHORT` → `home`.
- Any `ConversationError` sets `error` and moves UI to a recoverable state — the
  app never crashes (FR-016, SC-007).

### AudioSpectrum (composable)

A presentational composable rendered inside `ConversationScreen` while the AI
speaks (research R13).

```kotlin
@Composable
fun AudioSpectrum(
    speaking: Boolean,            // true while phase == SPEAKING
    barCount: Int = 24,
    modifier: Modifier = Modifier
)

/** Pure, frame-sampled bar-height source — unit-tested. */
object SpectrumWaveform {
    /** Normalized bar heights in [0f, 1f]; all at the floor height when !speaking. */
    fun barHeights(timeMs: Long, barCount: Int, speaking: Boolean): List<Float>
}
```

**Contract**:
- `AudioSpectrum` draws `barCount` vertical bars on a `Canvas`, sampling
  `SpectrumWaveform.barHeights` each animation frame.
- `speaking == false` → every bar sits at the constant floor height (idle look).
- `speaking == true` → bars animate organically; each bar has an independent
  period/phase so the row is non-uniform.
- The composable holds no business logic and adds no `ConversationUiState` field;
  it is driven solely by the `speaking` flag derived from `phase`.
- `SpectrumWaveform` is pure → unit-tested; the Canvas rendering is
  emulator-verified.

---

## CoPracticeSetupScreen + CoPracticeSetupViewModel

Profile selection for dual-speaker mode (FR-011).

```kotlin
data class CoPracticeSetupUiState(
    val profiles: List<UserProfileEntity> = emptyList(),
    val firstSelectedId: Int? = null,
    val secondSelectedId: Int? = null,
    val canStart: Boolean = false                 // true iff two DISTINCT profiles chosen
)
```

```kotlin
fun onSelectFirst(profileId: Int)
fun onSelectSecond(profileId: Int)
fun onStart()                                      // emits the new coPracticeGroupId
```

**Contract**: `canStart` is `true` only when `firstSelectedId` and
`secondSelectedId` are both non-null **and distinct**. Selecting the same profile
twice keeps `canStart = false`. Pure validation → unit-tested.

---

## SessionSummaryScreen + SessionSummaryViewModel

Post-session summary (FR-010, US3).

```kotlin
data class SessionSummaryUiState(
    val isLoading: Boolean = true,
    val perParticipant: List<ParticipantSummaryUi> = emptyList()
)

data class ParticipantSummaryUi(
    val profileName: String,
    val durationLabel: String,                    // "Xmin" or "Xh Ymin" — reuses 003's rule
    val correctionCount: Int,
    val vocabularyHighlights: List<String>
)
```

**Contract**:
- Single mode → `perParticipant` has 1 entry; dual mode → 2 entries, loaded by
  `coPracticeGroupId`.
- Duration label reuses feature 003's display rule (`< 60 min` → `"Xmin"`,
  `≥ 60 min` → `"Xh Ymin"`).
- A "Voltar ao início" action returns to `home`.

---

## ConversationRepository

Persistence + lifecycle boundary used by the ViewModels.

```kotlin
interface ConversationRepository {
    /** Creates the active session row(s); dual mode creates two rows + a groupId. */
    suspend fun startSession(config: ConversationConfig): SessionHandle

    /** Persists one completed turn (incremental persistence). */
    suspend fun appendTurn(
        handle: SessionHandle,
        speakerProfileId: Int,
        userText: String,
        aiText: String,
        meta: AiResponseMeta
    )

    /** Finalizes: applies the 60 s rule, sets status, updates stats, returns the result. */
    suspend fun finalizeSession(handle: SessionHandle, status: SessionStatus): SessionResult

    /** On app launch: finalizes any dangling `active` session as partial (or discards < 60 s). */
    suspend fun recoverDanglingSessions()
}
```

**Contract**:
- `appendTurn` writes a `ConversationTurnEntity` and inserts a `CorrectionEntity`
  per `meta.corrections` entry, attributed to `speakerProfileId`'s session row.
  It also appends `meta.vocabulary` to that row's `vocabulary` column.
- `finalizeSession` enforces the 60 s rule (research R10): `< 60 s` deletes the
  session + cascaded turns/corrections and returns
  `Outcome.DISCARDED_TOO_SHORT`; `≥ 60 s` sets `status`, returns
  `Outcome.SAVED` with one `SessionSummary` per participant.
- Because finalized sessions are ordinary `sessions` + `corrections` rows, feature
  003's `StatsRepository` reflects them with no code change (SC-004).

---

## UI principles compliance

- **Voice-First (Principle I)**: `ConversationScreen` has no text input for the
  conversation; touch is used only for Start/End controls and the permission
  prompt. `CoPracticeSetupScreen` / `SessionSummaryScreen` are configuration and
  review screens, where touch is constitutional.
- **Brazilian Portuguese (Principle IV)**: every label, button, nudge, and error
  message on these screens is in Brazilian Portuguese. The conversation audio is
  English.
