<!--
SYNC IMPACT REPORT
Version change: [TEMPLATE] → 1.0.0
Modified principles: All template placeholders replaced — 6 principles defined from scratch
Added sections: Core Principles (6), Platform & Technology Constraints, Development Workflow, Governance
Removed sections: N/A (initial constitution fill)
Templates requiring updates:
  ✅ .specify/templates/plan-template.md — Constitution Check section is generic; compatible
  ✅ .specify/templates/spec-template.md — Structure is generic; compatible
  ✅ .specify/templates/tasks-template.md — Structure is generic; compatible
Deferred TODOs: None
-->

# Caiana Talks Constitution

## Core Principles

### I. Voice-First Interface

All primary user interaction MUST happen via voice: speech-to-text (STT) for input and
text-to-speech (TTS) for AI output. Touch/text input is permitted only for configuration
and settings screens, never for conversation flow. The AI persona voice MUST be
configurable: gender (feminine/masculine), accent (Brazilian-friendly American or British
English), and speech rate (slow/normal/fast). Silent or text-only conversation modes are
out of scope and MUST NOT be introduced.

**Rationale**: The core value proposition is spoken English practice. A voice-only
conversation loop maximizes immersion and mirrors real conversational contexts.

### II. Token-Efficient AI Conversations

Every AI call MUST be designed to minimize token usage without sacrificing educational
quality. Mandatory cost controls:
- System prompts MUST be short, reusable, and cached where the provider supports it.
- Conversation history sent to the AI MUST be windowed (rolling context, not full history).
- Corrections and feedback MUST be inline and concise — no lengthy explanations unless
  the user explicitly requests a deeper breakdown.
- Batch or summarization passes MUST be used for progress insights rather than streaming
  raw transcripts to the model.
- AI provider selection and model tier MUST favor cost per token; switching to a smaller
  model for simpler tasks (e.g., grammar correction) is encouraged.

**Rationale**: The product must remain economically viable for individual users. High API
costs would price out the target audience.

### III. Pedagogical Effectiveness

The AI MUST actively teach, not just converse. Required behaviors:
- Identify and correct grammatical errors, pronunciation patterns (inferred from common
  Brazilian Portuguese interference), and vocabulary misuse in every session.
- Offer alternative phrasings and vocabulary enrichment naturally within the conversation
  flow — never interrupting disruptively.
- Corrections MUST be encouraging and non-punitive in tone.
- The AI MUST adapt difficulty based on the learner's demonstrated level over sessions.
- Users MUST be able to configure conversation themes and learning goals that the AI
  incorporates into subsequent sessions.

**Rationale**: An app that only chats provides no more value than a generic chatbot.
Structured, adaptive correction is the differentiator.

### IV. Brazilian Portuguese → English Only

The app is exclusively designed for native Brazilian Portuguese speakers learning English.
No additional language pairs MUST be added. The UI, onboarding, instructions, and
settings MUST remain in Brazilian Portuguese. The AI MUST respond in English during
conversation sessions, switching to Portuguese only when providing corrections or
explanations that require it for clarity.

**Rationale**: A narrow scope allows deeper optimization of the learning model, prompt
design, and UX for a single, well-understood audience. Generalization would dilute focus.

### V. Dual-Speaker Mode

The app MUST support a co-practice mode where two users share one physical Android device
simultaneously. In this mode:
- The app MUST perform speaker diarization to distinguish between the two speakers.
- Each speaker's contributions MUST be tracked, attributed, and corrected independently.
- Both speakers' progress metrics MUST be recorded separately.
- The AI MUST address both participants and be aware it is conversing with two people.
- Single-speaker and dual-speaker sessions MUST NOT share state mid-session; switching
  modes requires starting a new session.

**Rationale**: Co-practice significantly increases engagement and mirrors real conversation
contexts (e.g., two colleagues practicing together). Speaker attribution is non-negotiable
for fair progress tracking.

### VI. Personalization & Progress Tracking

Every user MUST have a persistent profile containing:
- Learning goals (e.g., travel, business, casual conversation).
- Preferred conversation themes for upcoming sessions.
- AI voice preferences (gender, accent, speech rate).
- Historical session data used to compute progress insights.

The Progress section MUST surface:
- Overall English level estimate (CEFR or equivalent).
- Trends in error frequency by category (grammar, vocabulary, fluency).
- Session history with key corrections highlighted.
- Actionable insights ("You frequently confuse present perfect and simple past").

Progress data MUST be stored locally on-device; cloud sync is a future enhancement and
MUST NOT block the initial release.

**Rationale**: Without visible progress, learners lose motivation. Personalization ensures
the app feels tailored rather than generic.

## Platform & Technology Constraints

- **Target platform**: Android (minimum SDK TBD during planning, targeting modern devices).
- **Language**: Kotlin with Jetpack Compose for UI.
- **AI provider**: To be finalized in planning — MUST support streaming TTS or real-time
  voice response. Cost per token is a first-class selection criterion.
- **STT**: Android's native SpeechRecognizer API is the default; a third-party alternative
  may be evaluated if accuracy for Brazilian-accented English is insufficient.
- **TTS**: Android's TextToSpeech API or a provider-supplied voice synthesis endpoint with
  accent/gender control.
- **Local storage**: Room database for profiles, session history, and progress data.
- **No cloud dependency** at launch beyond the AI conversation API.
- **Offline mode**: Out of scope for v1 (AI API requires connectivity).

## Development Workflow

- Features MUST be developed against a spec (`spec.md`) before implementation begins.
- Each user story MUST be independently testable and deployable as an MVP increment.
- UI changes MUST be tested on a physical device or emulator before a feature is marked
  complete.
- API cost estimates MUST be included in the plan for any feature that introduces new or
  changed AI calls.
- No feature may introduce a new AI call without a corresponding token-budget justification
  reviewed against Principle II.

## Governance

This constitution supersedes all other development practices in this repository. Amendments
require:
1. A written rationale documenting what changed and why.
2. A version bump following semantic versioning (MAJOR for principle removal/redefinition,
   MINOR for new principle or section, PATCH for clarifications).
3. Propagation of changes to all dependent templates (plan, spec, tasks).
4. Update of `LAST_AMENDED_DATE` to the date of the amendment.

All implementation plans and specs MUST include a Constitution Check gate verifying
compliance with the six core principles before work begins.

**Version**: 1.0.0 | **Ratified**: 2026-05-18 | **Last Amended**: 2026-05-18
