# Feature Specification: User Stats & Progress Insights

**Feature Branch**: `003-user-stats`

**Created**: 2026-05-19

**Status**: Draft

**Input**: User description: "Agora quero criar a feature em que o usuario irá ver o seu progresso e insights sobre o seu nivel de ingles, nomeie-a como user-stats"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View English Level Overview (Priority: P1)

After completing one or more conversation sessions, the user opens the Stats screen and sees their estimated overall English level (CEFR scale: A1–C2) along with a brief description of what that level means in practice.

**Why this priority**: The CEFR level estimate is the single most motivating data point — it gives the user a concrete, internationally recognized benchmark for where they stand and drives continued engagement.

**Independent Test**: Can be fully tested by navigating to the Stats screen after at least one session exists, verifying the CEFR label and description are displayed.

**Acceptance Scenarios**:

1. **Given** the user has completed at least one session, **When** they open the Stats screen, **Then** they see their estimated CEFR level (e.g., "B1 — Intermediate") and a one-sentence description of that level.
2. **Given** the user has never completed a session, **When** they open the Stats screen, **Then** they see a placeholder message indicating no data is available yet.

---

### User Story 2 - View Error Trend by Category (Priority: P2)

The user views a breakdown of their most frequent error types across sessions — grammar, vocabulary, and fluency — so they know where to focus their practice.

**Why this priority**: Categorical error trends turn raw corrections into actionable direction; without them, users cannot prioritize improvement areas.

**Independent Test**: Can be fully tested by completing two or more sessions with recorded corrections and verifying that the category breakdown reflects the accumulated correction data.

**Acceptance Scenarios**:

1. **Given** the user has sessions with recorded corrections, **When** they view the Stats screen, **Then** they see three categories (Grammar, Vocabulary, Fluency) each displaying the number or percentage of errors recorded.
2. **Given** one category has zero errors, **When** viewing the breakdown, **Then** that category shows "0" or "No errors" rather than being hidden.

---

### User Story 3 - View Session History with Corrections (Priority: P3)

The user scrolls through a list of past sessions, each showing its date, duration, and the key corrections highlighted during that session.

**Why this priority**: Session history provides context for progress over time and allows users to review specific feedback they received, reinforcing learning.

**Independent Test**: Can be fully tested by completing two or more sessions and verifying the session list displays correct dates, durations, and corrections per session.

**Acceptance Scenarios**:

1. **Given** the user has completed multiple sessions, **When** they scroll through session history, **Then** each entry shows the session date, duration, and up to 5 highlighted corrections.
2. **Given** a session had no corrections, **When** viewing that session's entry, **Then** it shows "No corrections recorded" rather than an empty card.

---

### User Story 4 - View Actionable Insights (Priority: P4)

The user reads automatically generated textual insights that identify recurring patterns in their mistakes (e.g., "You frequently confuse present perfect and simple past") so they have specific areas to work on.

**Why this priority**: Raw numbers alone are not enough — natural-language insights translate data into concrete guidance, increasing the perceived value of the app.

**Independent Test**: Can be fully tested by having enough sessions with repeated error patterns and confirming at least one insight message appears on the Stats screen.

**Acceptance Scenarios**:

1. **Given** the user has a recurring error pattern across at least 2 sessions, **When** they view the Stats screen, **Then** at least one actionable insight message is displayed describing the pattern.
2. **Given** insufficient data for pattern detection, **When** viewing insights, **Then** a message reads "Complete more sessions to unlock insights."

---

### Edge Cases

- What happens when the user has exactly one session? All sections must display available data without crashing; sections requiring multiple sessions show appropriate "need more data" messages.
- What happens when stored session data is corrupted or missing? The screen must degrade gracefully, showing only the sections for which data is intact.
- What if the user switches profiles? Stats must be scoped strictly to the active profile; switching profiles immediately refreshes all data.
- What happens when a session had very long duration (e.g., 2+ hours)? Duration must display correctly without overflow (e.g., "2h 15min").

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST display the active user's estimated CEFR level (A1–C2) on the Stats screen.
- **FR-002**: The system MUST display a breakdown of error frequency across three categories: Grammar, Vocabulary, and Fluency.
- **FR-003**: The system MUST display a chronological list of past sessions, each with date, duration, and highlighted corrections.
- **FR-004**: The system MUST generate and display at least one actionable insight when a recurring error pattern is detected across two or more sessions.
- **FR-005**: All progress data MUST be read exclusively from local on-device storage; no network call may be made to retrieve or compute stats.
- **FR-006**: The Stats screen MUST be scoped to the currently active user profile; switching profiles MUST refresh all displayed data.
- **FR-007**: The system MUST display empty-state messages for each section when insufficient data is available, rather than hiding the section.
- **FR-008**: Users MUST be able to reach the Stats screen from the Home screen without more than two taps.

### Key Entities *(include if feature involves data)*

- **Session**: A single completed conversation session, containing start time, end time, and a list of corrections.
- **Correction**: An individual error recorded during a session, tagged with a category (Grammar, Vocabulary, or Fluency) and a description.
- **ProgressSnapshot**: A computed aggregate derived from all sessions for a profile — includes estimated CEFR level, per-category error counts, and generated insight strings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can open the Stats screen and see their overall level within 2 seconds of navigation.
- **SC-002**: 100% of users with at least one completed session see a non-empty Stats screen.
- **SC-003**: Users with 2 or more sessions containing corrections see at least one actionable insight.
- **SC-004**: All four sections (Level, Error Breakdown, Session History, Insights) are visible without scrolling on a standard mobile screen, or are reachable with a single scroll gesture.
- **SC-005**: Switching profiles updates all stats within 1 second, with no stale data from the previous profile visible.

## Assumptions

- Sessions and corrections are already being recorded by a prior or concurrent feature; this spec covers only the display layer for existing stored data.
- The CEFR level estimate is computed locally using a heuristic based on error frequency and session count — no external AI call is required for this computation.
- "Actionable insights" are generated from deterministic rules applied to local correction data, not from an AI API call, keeping this feature cost-free per Principle II.
- The Stats screen is accessible from the Home screen via a dedicated navigation entry (e.g., bottom navigation bar or a stats button).
- Dual-speaker mode sessions (Principle V) will have corrections attributed per speaker; this spec covers single-speaker stats only — dual-speaker stats are a future enhancement.
- Data retention: all session history is retained indefinitely on-device unless the user explicitly deletes a profile.
