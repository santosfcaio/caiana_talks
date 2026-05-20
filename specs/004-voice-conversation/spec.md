# Feature Specification: Voice Conversation

**Feature Branch**: `004-voice-conversation`

**Created**: 2026-05-20

**Status**: Draft

**Input**: User description: "feature de nome 'conversation', nela iremos de fato criar a conversação por voz com o modelo de IA, lembre-se de consultar o constitution onde temos varias regras a cerca dessa feature, lembre-se tambem que será preciso atualizar os stats do usuario a cada sessão de conversa e tambem deverá sempre levar em conta a personalização do perfil do usuario para a forma como a conversa será feita."

## Clarifications

### Session 2026-05-20

- Q: When the app goes to the background or crashes mid-session, is the partial session data saved and stats updated, or is the session discarded entirely? → A: Save partial — interruption saves whatever was captured; stats and session history are updated with partial data.
- Q: What level of session transcript data should be stored locally on-device? → A: Full text transcripts — store the complete STT transcription of user speech and AI text responses for each turn; no audio recording.
- Q: How does a user initiate a dual-speaker session? → A: Separate mode entry — a dedicated "Co-practice" button on the home screen launches its own flow for selecting two profiles before starting the session.
- Q: How should the AI response be delivered to the user (streaming vs. wait-for-complete)? → A: Stream audio — TTS begins playing as soon as the first sentence of the AI response is ready; audio plays progressively.
- Q: What is the minimum session duration for it to count toward the user's stats? → A: 60 seconds — sessions shorter than 60 seconds are discarded and do not update stats or session history.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Single-Speaker Voice Session (Priority: P1)

A user selects their profile, taps "Start Conversation," and begins a spoken English practice session. The AI responds via voice using the configured gender, accent, and speech rate. The AI incorporates the user's selected learning goals and conversation themes into the dialogue. After the session ends, the user's stats (sessions completed, minutes practiced, corrections received) are updated.

**Why this priority**: This is the core product loop. Without a working solo voice session, the app delivers no value.

**Independent Test**: Can be fully tested by starting a session as a single user, speaking in English, receiving a voiced AI response with corrections, ending the session, and confirming that the stats screen reflects the new session data.

**Acceptance Scenarios**:

1. **Given** the user is on the home screen with a configured profile, **When** they tap "Start Conversation," **Then** the microphone activates and the session screen appears.
2. **Given** the session is active, **When** the user stops speaking, **Then** the transcribed text is displayed and the AI responds in English via the configured voice (gender, accent, speech rate).
3. **Given** the user makes a grammatical or vocabulary error, **When** the AI detects it, **Then** the AI provides a concise, encouraging correction within the conversation flow without interrupting the dialogue disruptively.
4. **Given** the user taps "End Session," **Then** the session is closed and the user's stats are updated to reflect the new session (duration, number of corrections, etc.).
5. **Given** the user has previously configured learning goals and conversation themes, **Then** the AI incorporates those preferences into the conversation from the first turn.

---

### User Story 2 — AI Correction and Vocabulary Enrichment (Priority: P2)

During a conversation session, the AI identifies errors in grammar, vocabulary, and fluency patterns (common for Brazilian Portuguese speakers), offers natural inline corrections, and suggests alternative phrasings without breaking the conversational flow.

**Why this priority**: Corrections are the primary educational differentiator. A session without structured feedback is just chatting.

**Independent Test**: Can be tested by deliberately speaking with grammatical errors; the AI should correct them inline with encouraging language and continue the conversation.

**Acceptance Scenarios**:

1. **Given** the user makes a grammar error (e.g., wrong verb tense), **When** the AI responds, **Then** it subtly corrects the error, offers the correct form, and continues the conversation without dwelling on the mistake.
2. **Given** the user uses a vocabulary word incorrectly, **When** the AI responds, **Then** it acknowledges the intent, introduces the correct word or phrase naturally, and moves on.
3. **Given** the correction is non-critical, **When** the AI delivers it, **Then** the tone is encouraging and non-punitive.
4. **Given** the user's profile includes a specific learning goal (e.g., business English), **Then** vocabulary enrichment suggestions align with that goal.

---

### User Story 3 — Session Summary and Stats Update (Priority: P2)

At the end of a conversation session, the user sees a brief summary of the session (duration, number of corrections, key vocabulary introduced) and the app updates the persistent user stats accordingly.

**Why this priority**: Without visible progress updates, learners lose motivation. Stats must be recorded every session.

**Independent Test**: Can be tested by completing a session and verifying that the stats screen reflects updated total session count, total minutes, and at least one new correction entry.

**Acceptance Scenarios**:

1. **Given** a session has just ended, **When** the summary screen appears, **Then** it shows session duration, number of corrections made, and key vocabulary highlights.
2. **Given** the session summary is displayed, **When** the user views their stats, **Then** session count, total practice time, and correction history reflect the completed session.
3. **Given** the user completes multiple sessions, **Then** the stats aggregate across all sessions correctly.

---

### User Story 4 — Dual-Speaker Co-Practice Mode (Priority: P3)

Two users share one physical device and practice English conversation together. The app distinguishes between the two speakers, tracks each person's contributions and errors separately, and records stats independently for each profile.

**Why this priority**: Co-practice increases engagement significantly, but it requires single-speaker mode to be solid first. Speaker diarization adds complexity; it is the second release priority.

**Independent Test**: Can be tested by two speakers alternating turns; the app must attribute each utterance to the correct speaker and display corrections addressed to the right person.

**Acceptance Scenarios**:

1. **Given** the user is on the home screen, **When** they tap "Co-practice," **Then** a profile-selection screen appears where the user selects two distinct profiles before starting.
2. **Given** two profiles are selected and the session starts, **When** audio is captured, **Then** the app activates speaker diarization to distinguish between the two voices.
3. **Given** Speaker A makes an error, **When** the AI corrects it, **Then** the correction is addressed to Speaker A and recorded only in Speaker A's stats.
4. **Given** both speakers participate throughout the session, **Then** each person's session stats (duration attributed, corrections received) are recorded independently in their respective profiles.
5. **Given** a dual-speaker session is active, **When** either speaker attempts to switch to single-speaker mode mid-session, **Then** the app requires ending the current session before allowing a mode change.

---

### Edge Cases

- What happens when the device microphone is not available or access is denied?
- How does the app handle silence (no speech detected) for more than 10 seconds mid-session?
- What happens if the AI API is unreachable during a session?
- Sessions shorter than 60 seconds are discarded and do not update stats or session history; the user sees no summary screen and is returned to the home screen.
- What happens when two speakers speak simultaneously in dual-speaker mode?
- How does the app behave when the user speaks in Portuguese instead of English?
- What happens if the device runs out of storage space while recording session data?
- If the app is sent to the background or crashes mid-session, the session is saved as partial with all data captured up to that point, and stats are updated accordingly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST allow users to start a single-speaker voice conversation session from the home screen.
- **FR-002**: The app MUST capture the user's spoken English via the device microphone and convert it to text (speech-to-text).
- **FR-003**: The app MUST send the transcribed user input to the AI model along with a token-efficient system prompt that encodes the user's learning goals, conversation theme, AI voice preferences, and a rolling conversation window (not full history).
- **FR-004**: The AI MUST respond in spoken English using the voice configuration from the user's profile (gender, accent, speech rate). The response MUST be delivered via progressive streaming — TTS playback begins as soon as the first sentence of the AI response is available, without waiting for the full response to be generated.
- **FR-005**: The AI MUST identify and correct grammatical errors, vocabulary misuse, and fluency patterns common to Brazilian Portuguese speakers within every session.
- **FR-006**: Corrections MUST be delivered inline within the conversational response — concise, encouraging, and non-punitive.
- **FR-007**: The AI MUST offer vocabulary enrichment and alternative phrasings naturally within the conversation, aligned with the user's configured learning goals and conversation theme.
- **FR-008**: The app MUST record each session's data locally: start time, end time (or interruption time), session status (completed/partial), number of AI corrections, vocabulary introduced, and the full text transcript of every turn (complete STT transcription of user speech and AI text response). No audio recordings are stored. Session data MUST be persisted even when the session ends via interruption (app backgrounded or crash).
- **FR-009**: The app MUST update the user's persistent stats whenever a session ends — whether via a deliberate "End Session" action or an interruption — provided the session lasted at least 60 seconds. Sessions under 60 seconds MUST be discarded without updating any stats. Stats updated include: total sessions completed, total practice time, corrections received (by category when detectable), and session history. Partial sessions of 60 seconds or more MUST be included in the session count and time totals.
- **FR-010**: A post-session summary screen MUST be shown after each session, displaying duration, correction count, and key vocabulary highlights.
- **FR-011**: The home screen MUST expose a dedicated "Co-practice" entry point that launches a profile-selection flow allowing the user to choose two distinct profiles before starting a dual-speaker session. Once both profiles are confirmed, the session begins with speaker diarization active to attribute speech to the correct user.
- **FR-012**: In dual-speaker mode, stats and corrections MUST be tracked and stored independently for each of the two participants.
- **FR-013**: Switching between single-speaker and dual-speaker modes MUST require ending the current session first; mid-session mode switching is not permitted.
- **FR-014**: The conversation session MUST use a rolling context window for AI calls — full transcript history MUST NOT be sent to the AI model.
- **FR-015**: The system prompt sent to the AI MUST be short, reusable, and structured for prompt caching where the provider supports it.
- **FR-016**: The app MUST handle microphone unavailability and API errors gracefully, displaying user-friendly messages without crashing.
- **FR-017**: When the user speaks in Portuguese (detected), the app MUST gently prompt the user to continue in English rather than silently failing or switching language.

### Key Entities

- **ConversationSession**: Represents one practice session. Key attributes: session ID, profile ID(s), start time, end time (or interruption time), status (active | completed | partial), mode (single/dual), list of turns, corrections count, vocabulary introduced.
- **ConversationTurn**: A single exchange within a session. Attributes: speaker ID, user speech transcription (full STT text), AI response text (full), corrections flagged. No audio data is stored.
- **SessionSummary**: Aggregated view of a completed session surfaced to the user. Attributes: duration, correction count, vocabulary highlights, session date.
- **UserStats** (existing, updated): Accumulates data across sessions — total sessions, total minutes, correction history by category.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can start, conduct, and end a complete voice conversation session within 60 seconds of opening the app from the home screen.
- **SC-002**: The AI response (voice output) begins playing within 3 seconds of the user finishing their spoken turn, under normal network conditions.
- **SC-003**: At least one correction or vocabulary suggestion is delivered per session when errors are present, without the user needing to request it.
- **SC-004**: User stats are updated within 2 seconds of the session ending, and the updated values are immediately visible on the stats screen.
- **SC-005**: In dual-speaker mode, speaker attribution accuracy allows each participant's corrections to be recorded under their own profile with no cross-contamination in at least 90% of turns.
- **SC-006**: The AI system prompt token count stays within a budget that keeps per-session AI cost viable for individual users (target: under 500 tokens for system prompt + recent context window per turn).
- **SC-007**: 95% of sessions complete without a crash or unrecoverable error, even under poor network conditions.

## Assumptions

- Users have an active internet connection during sessions (offline conversation is out of scope for v1).
- The AI provider supports streaming text-to-speech or near-real-time voice response; exact provider to be finalized during planning.
- Android's native SpeechRecognizer is used as the default STT engine; a third-party alternative may be evaluated during implementation if accuracy for Brazilian-accented English is insufficient.
- Speaker diarization in dual-speaker mode relies on the device microphone and may be imperfect; the feature is best-effort for v1.
- The user's profile preferences (learning goals, conversation theme, voice config) are already stored in the local database from the profile personalization feature (002).
- User stats persistence layer already exists from feature 003; this feature extends it with session-level data.
- The rolling context window size (number of recent turns sent to AI) will be determined during planning based on token budget constraints.
- Portuguese-language detection in user speech is best-effort via the STT output text; dedicated language identification is not required.
