# Feature Specification: OpenRouter AI Connector

**Feature Branch**: `006-openrouter-connector`

**Created**: 2026-05-20

**Status**: Draft

**Input**: User description: "Mude o conector com a IA, nao irei usar diretamente os modelos Claude, irei usar uma api key do openrouter.ai"

## Clarifications

### Session 2026-05-20

- Q: Who controls the API key and model settings? → A: Hybrid — developer sets defaults at build time; user can optionally override both in the Settings screen.
- Q: Can the user also change the AI model? → A: Yes — free-text field in the Settings screen.
- Q: How should the user-entered API key be stored on-device? → A: Plain DataStore, stored in the existing preferences DataStore alongside other user settings.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Use Developer-Configured OpenRouter Key (Priority: P1)

As the app owner, I want to configure a default OpenRouter.ai API key at build time so that the app works out of the box without any user configuration.

**Why this priority**: This is the core migration. Without a working OpenRouter connection, the conversation feature is non-functional. The default key allows the app to work immediately on first launch.

**Independent Test**: Can be fully tested by setting a valid OpenRouter API key in the build configuration and completing a full voice conversation session that receives AI replies, without touching the Settings screen.

**Acceptance Scenarios**:

1. **Given** a valid OpenRouter API key is set in build configuration and no user override exists, **When** the user starts a conversation, **Then** the app uses the build-time default key and successfully receives streaming AI replies.
2. **Given** no API key is configured at build time and no user override exists, **When** the app attempts an AI call, **Then** the app surfaces an appropriate error to the user (connection failed / invalid credentials).

---

### User Story 2 - Override API Key and Model in Settings (Priority: P2)

As the app owner, I want to enter my own OpenRouter API key and model identifier in the Settings screen so that I can change credentials or try a different model without rebuilding the app.

**Why this priority**: Build-time changes require a full rebuild and reinstall. A Settings override makes it easy to switch keys or experiment with models at runtime.

**Independent Test**: Can be fully tested by leaving the build-time key blank, entering a valid key and model identifier in Settings, starting a conversation, and confirming the AI replies correctly.

**Acceptance Scenarios**:

1. **Given** the user has entered a custom API key in Settings, **When** the app makes an AI request, **Then** the user-provided key takes precedence over the build-time default.
2. **Given** the user has entered a custom model identifier in Settings, **When** the app makes an AI request, **Then** the user-provided model takes precedence over the build-time default.
3. **Given** the user clears the API key field in Settings, **When** the app makes an AI request, **Then** the app falls back to the build-time default key.
4. **Given** the user clears the model field in Settings, **When** the app makes an AI request, **Then** the app falls back to the build-time default model.

---

### User Story 3 - Preserve All Existing Conversation Behaviors (Priority: P1)

As a user, I want voice conversation sessions to behave identically to before so that the provider change is invisible to me.

**Why this priority**: Functional regression is the greatest risk of this migration. All session behavior — streaming replies, `<say>` block parsing, corrections, session summaries — must be preserved exactly.

**Independent Test**: A full voice session can be completed from start to finish with corrections appearing at the end, identical to pre-migration behavior.

**Acceptance Scenarios**:

1. **Given** an active voice session, **When** the AI replies, **Then** text is streamed in real time and spoken aloud exactly as before.
2. **Given** the AI reply contains a `<say>` block and a `<correction>` block, **When** the session ends, **Then** the summary screen shows the correction data correctly.
3. **Given** two users in co-practice mode, **When** both speak, **Then** speaker attribution and corrections remain independent and correct.

---

### Edge Cases

- What happens when the OpenRouter API rate limit is reached mid-session?
- What happens if OpenRouter returns a non-streaming response when streaming was requested?
- How does the app handle an OpenRouter outage (network timeout vs. HTTP error)?
- What happens if a model requested via OpenRouter does not support the request format?
- What happens if the user saves an invalid model identifier in Settings and starts a session?
- What happens if both the build-time default key and the user override key are blank?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST send AI conversation requests to the OpenRouter.ai API endpoint instead of the Anthropic API endpoint.
- **FR-002**: The app MUST authenticate with OpenRouter using a bearer token resolved in this order: (1) user-provided key from DataStore if non-empty, (2) developer default from build configuration.
- **FR-003**: The app MUST send the target AI model identifier resolved in this order: (1) user-provided model from DataStore if non-empty, (2) developer default model from build configuration.
- **FR-004**: The app MUST support streaming responses from OpenRouter so that text is delivered incrementally during a session.
- **FR-005**: The ANTHROPIC_API_KEY build configuration field MUST be replaced by an OPENROUTER_API_KEY field and a default OPENROUTER_MODEL field; the old field MUST be removed.
- **FR-006**: All existing streaming parsing logic (SSE events, `<say>` block detection, correction block extraction) MUST continue to function correctly with the OpenRouter response format.
- **FR-007**: Error handling MUST map OpenRouter-specific HTTP error codes to the existing ConversationError domain model without exposing provider-specific details to the UI.
- **FR-008**: The `ConversationAiClient` interface contract MUST remain unchanged so that no ViewModel, Repository, or UI code requires modification.
- **FR-009**: All existing unit tests for the AI client and related conversation components MUST pass after the migration.
- **FR-010**: The Settings screen MUST include a free-text field for the user to enter a custom OpenRouter API key (optional override).
- **FR-011**: The Settings screen MUST include a free-text field for the user to enter a custom AI model identifier (optional override).
- **FR-012**: Both user-provided values MUST be persisted in the existing user preferences DataStore and survive app restarts.
- **FR-013**: When either user override field is empty or cleared, the app MUST fall back to the corresponding build-time default value.

### Key Entities

- **Default API Key**: The developer-supplied OpenRouter bearer token baked into the build configuration; used when no user override is present.
- **User API Key Override**: An optional OpenRouter bearer token entered by the user in Settings; stored in the existing preferences DataStore; takes precedence over the default when non-empty.
- **Default Model Identifier**: The developer-supplied OpenRouter model string baked into the build configuration; used when no user override is present.
- **User Model Override**: An optional free-text model identifier entered by the user in Settings; stored in the existing preferences DataStore; takes precedence over the default when non-empty.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A complete voice conversation session can be started, conducted, and ended successfully using the OpenRouter connector, with no user-visible difference in conversation behavior from the previous Anthropic connector.
- **SC-002**: All 198 existing unit tests pass after the migration (zero regressions).
- **SC-003**: The ANTHROPIC_API_KEY configuration field is fully removed; no reference to it remains in build files or source code.
- **SC-004**: Network errors and API authentication failures surface the same user-facing error states as before (no new crash paths introduced).
- **SC-005**: The model used for conversation can be changed either by updating the build configuration default or by entering a new value in the Settings screen, without any code modification.
- **SC-006**: A user-entered API key override persists correctly across app restarts and takes precedence over the build-time default.

## Assumptions

- OpenRouter.ai's streaming API is compatible with the OpenAI chat completions SSE format, which the existing SSE parser can handle with targeted adjustments.
- The AI model chosen via OpenRouter will support the same `<say>` / `<correction>` response format enforced by the existing system prompt.
- Prompt caching (currently an Anthropic-specific feature) is not available via OpenRouter for the chosen model tier; the feature is dropped for now and may be revisited if OpenRouter adds support.
- The app owner has a valid OpenRouter.ai account and API key before this feature is deployed.
- UI changes are limited to the Settings screen (adding two optional text fields); all other screens are unaffected.
- User override values are stored in plain (unencrypted) DataStore alongside existing profile preferences; this is an acceptable tradeoff given the personal-use nature of the app.
- Token budget and cost considerations remain relevant (per Constitution Principle II); model selection via OpenRouter should target a cost-efficient model comparable to `claude-haiku-4-5`.
