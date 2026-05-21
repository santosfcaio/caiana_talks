# Feature Specification: LCARS Theme UI/UX Refactor

**Feature Branch**: `005-lcars-theme-refactor`

**Created**: 2026-05-20

**Status**: Draft

**Input**: User description: "Refatore o UI e UX (front end) do projeto utilizando um tema inspirado nos computadores de Star Trek The Next Generation."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - LCARS Visual Identity on Core Screens (Priority: P1)

A user launches the app and immediately encounters the iconic LCARS aesthetic: black background, colorful rounded panels framing the content, futuristic typography, and pill-shaped buttons in the characteristic orange, blue, and purple palette. The profile selection screen and home screen are fully reskinned.

**Why this priority**: The home and profile selection screens are the entry point of every session. Delivering the LCARS look here establishes the full visual identity and provides immediate wow-factor.

**Independent Test**: Can be tested by launching the app on the emulator and verifying the profile selection and home screens match LCARS visual references. No backend changes required.

**Acceptance Scenarios**:

1. **Given** the app is launched, **When** the profile selection screen appears, **Then** the screen displays a black background, colored LCARS-style framing panels, and all interactive buttons in the characteristic pill/capsule shape with LCARS color accents.
2. **Given** a profile is selected and the home screen loads, **When** the user views the screen, **Then** all buttons (Start Conversation, Co-practice) render as LCARS-style pill buttons, the layout includes decorative framing bars, and futuristic typography is used for all labels.
3. **Given** any screen in this scope, **When** the user taps any interactive element, **Then** the touch feedback is an instant color shift (element lightens to ~70% opacity on press, reverts immediately on release) with no ripple animation — the default Android ripple `Indication` MUST be replaced entirely.

---

### User Story 2 - LCARS Theme on Profile Configuration Screens (Priority: P2)

A user navigating to profile edit or settings screens experiences the same LCARS visual language. Input fields, toggles, dropdowns, and section headers all adopt LCARS-style presentation, while retaining full interaction capability.

**Why this priority**: Configuration screens are visited less frequently than the home screen, but they must be visually consistent to avoid breaking the immersive theme.

**Independent Test**: Can be tested by navigating to the Profile Edit and Settings screens and verifying the LCARS styling without testing conversation functionality.

**Acceptance Scenarios**:

1. **Given** the user enters the profile edit screen, **When** the screen renders, **Then** all section headers use LCARS-style horizontal bars, all option selectors (learning goal, accent, speech rate) appear as LCARS-styled components, and the background is black.
2. **Given** the user opens the settings screen, **When** they interact with any option, **Then** the interaction is fully functional and all UI elements maintain the LCARS visual language.
3. **Given** any configuration screen, **When** the user saves changes and navigates away, **Then** the transition animation, if any, matches the LCARS aesthetic.

---

### User Story 3 - LCARS Theme on Conversation and Stats Screens (Priority: P3)

A user entering a voice conversation session or viewing their progress statistics sees the full LCARS treatment: the conversation screen's voice waveform, AI status indicators, and session summary all styled as LCARS data panels; the stats screen surfaces progress data within LCARS-style chart containers and data readouts.

**Why this priority**: These screens are deeply functional, so styling must not interfere with real-time voice interaction. They are lower priority as the core LCARS identity is established by P1 and P2.

**Independent Test**: Can be tested by starting a voice session and verifying visual presentation without evaluating AI response quality. Stats screen can be independently tested by navigating to it.

**Acceptance Scenarios**:

1. **Given** the user starts a voice conversation, **When** the conversation screen loads, **Then** the voice waveform/recording indicator is styled as an LCARS display element, status information appears in LCARS-style data panels, and the stop/end session control is an LCARS-style button.
2. **Given** a session ends and the summary screen appears, **When** the user views session results, **Then** corrections and metrics are presented in LCARS-style readout panels with LCARS typography.
3. **Given** the user navigates to the statistics screen, **When** the screen renders, **Then** all charts, metrics, and labels are framed within LCARS-style panels using the LCARS color palette.

---

### Edge Cases

- What happens when the screen is rotated (if landscape is supported)? The LCARS layout must adapt without breaking proportions.
- How does the theme render on very small or very large screen densities? All LCARS decorative panels must scale appropriately.
- What happens during loading states (e.g., waiting for the AI to respond)? Loading indicators must be styled consistently with LCARS (no default Android spinners).
- How does the theme handle dark-mode system settings? LCARS is inherently dark (black background), so no adaptation is needed — the theme is always dark by design.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All screens MUST use the LCARS core color palette: black (#000000) background; accent panels in orange, blue, purple, beige/peach, and red-maroon tones consistent with TNG LCARS references. Each individual screen MUST use a maximum of 3 distinct accent colors (in addition to black); different screens may draw from different subsets of the palette to create variety across the app without individual screens feeling cluttered at phone scale.
- **FR-002**: All interactive buttons MUST use LCARS-style pill/capsule shapes — rounded on both ends — replacing all default Android button styles. Primary or solo-action buttons (e.g., "Iniciar Conversa", "Salvar") MUST span the full content width. Grouped option buttons (e.g., accent selectors, speech rate selectors) MUST be fixed auto-sized pills arranged in horizontal rows, mirroring LCARS console grouping patterns.
- **FR-003**: Each screen layout MUST incorporate LCARS-style structural framing consisting of: a narrow left accent strip (16–24 dp wide) plus top and bottom horizontal bars. The left strip uses a rounded corner on one end (the LCARS "elbow") to form the characteristic bracket shape. A full-width sidebar is explicitly excluded to preserve usable content area on portrait phone widths (360–393 dp).
- **FR-004**: All text at or above 16 sp MUST use the LCARS-style futuristic sans-serif typeface (headers, section labels, button text, prominent data readouts). Text below 16 sp (small labels, helper text, fine print) MUST use a standard readable sans-serif typeface to preserve legibility on phone-density screens — particularly for accented Portuguese characters.
- **FR-005**: Every screen that currently exists in the app MUST receive the LCARS theme — no screen may be left with default or prior styling after this feature is complete.
- **FR-006**: All existing user interactions, navigation flows, and data displays MUST remain fully functional after the theme is applied — this is a purely visual refactor.
- **FR-007**: The voice recording/waveform visualization on the conversation screen MUST be styled as an LCARS data display element (e.g., angular waveform bars in the LCARS palette).
- **FR-008**: Loading and progress indicators MUST be replaced with LCARS-style equivalents (e.g., segmented progress bars, blinking status indicators in LCARS colors); default Android spinners MUST NOT appear.
- **FR-009**: All touch targets MUST meet the minimum accessibility size (at least 48×48 dp) within the LCARS layout, even where decorative panels constrain available space.
- **FR-010**: Screen navigation transitions MUST be styled to reinforce the LCARS aesthetic (e.g., horizontal slide or fade) rather than default Android back-stack animations.

### Key Entities

- **LCARS Color Palette**: The defined set of colors used across the theme — primary (orange, blue, purple, beige/peach, maroon) and background (black). All screens reference this palette; no ad-hoc colors may be introduced.
- **LCARS Component Library**: The collection of reusable themed UI components (pill button, framing bar, data panel, status indicator, LCARS typography styles) that all screens consume to ensure visual consistency.
- **Screen Theme Contract**: A per-screen definition of which LCARS structural elements appear (e.g., left framing panel, top bar, color accent selection) to ensure coherent variety within the LCARS language without being identical across screens.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every screen in the app consistently applies the LCARS visual language — zero screens render with default Android Material styling after the refactor.
- **SC-002**: All existing user workflows (profile selection, profile editing, voice conversation, co-practice, session summary, settings, stats) complete without functional regression after the theme is applied.
- **SC-003**: A Star Trek: TNG fan, shown screenshots of the app, recognizes the LCARS aesthetic without any prompting.
- **SC-004**: Interactive elements (buttons, inputs) remain visually distinct from decorative LCARS panels — users never mistake a decorative bar for a tappable button.
- **SC-005**: No default Android loading spinners, default button styles, or default Material color tokens are visible to the user anywhere in the app after this feature ships.

## Clarifications

### Session 2026-05-20

- Q: How should LCARS structural framing adapt to portrait phone layout? → A: Narrow left accent strip (16–24 dp) + top/bottom horizontal bars on every screen; full sidebar explicitly excluded.
- Q: At what text size does the LCARS typeface switch to a standard readable font? → A: LCARS typeface for text ≥ 16 sp; standard readable sans-serif for text < 16 sp.
- Q: How many distinct accent colors may appear on a single screen simultaneously? → A: Maximum 3 accent colors per screen (+ black background); different screens may use different subsets of the full palette.
- Q: How should LCARS pill button width behave on mobile? → A: Full content-width for primary/solo actions; fixed auto-sized pills in horizontal rows for grouped option selectors.
- Q: What visual feedback replaces the Android ripple for press states? → A: Instant color shift to ~70% opacity on press, reverts on release; no animation duration, no ripple.

## Assumptions

- Sound effects (button clicks, alert tones, computer beeps) are out of scope — this refactor addresses only visual presentation.
- Complex animations (e.g., scanning sweeps, flickering screens, animated starfields) are out of scope; only subtle transitions and press-state feedback are included.
- The app continues to run in portrait orientation only, consistent with its current configuration — LCARS layout adaptation for landscape is not required.
- All UI text remains in Brazilian Portuguese (UI language), consistent with the constitution — the LCARS theme applies to visual styling only, not language.
- A freely licensed LCARS-style typeface available without commercial restrictions will be used; if none meets quality standards, the closest freely available futuristic sans-serif is acceptable.
- Co-practice mode setup screen is included in the scope of this refactor alongside the conversation screens.
