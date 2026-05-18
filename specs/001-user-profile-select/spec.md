# Feature Specification: MVP User Profile Selection

**Feature Branch**: `001-user-profile-select`

**Created**: 2026-05-18

**Status**: Draft

**Input**: User description: "feature de autenticação, para esse MVP terei apenas dois usuarios, então ao acessar o app o usuario irá selecionar se é o caio ou a ana, o app deve guardar essa escolha para as proximas vezes que for aberto"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - First Launch Profile Selection (Priority: P1)

On the very first time the app is opened, the user sees a selection screen offering two identities — Caio and Ana. The user taps their name, and the app remembers this choice for all future launches.

**Why this priority**: This is the entry point to the entire app. Without a selected profile, no personalization, progress tracking, or dual-speaker attribution can function. All other features depend on knowing which user is active.

**Independent Test**: Install the app fresh, open it, select one of the two names, and confirm the home screen loads with that user's identity visible. Delivers a working identity foundation even before any other feature exists.

**Acceptance Scenarios**:

1. **Given** the app is launched for the first time with no saved profile, **When** the selection screen is displayed, **Then** exactly two options are shown: "Caio" and "Ana"
2. **Given** the selection screen is displayed, **When** the user taps "Caio", **Then** the app saves "Caio" as the active user and navigates to the main screen
3. **Given** the selection screen is displayed, **When** the user taps "Ana", **Then** the app saves "Ana" as the active user and navigates to the main screen

---

### User Story 2 - Returning User Bypasses Selection (Priority: P2)

On every subsequent launch after a profile has been chosen, the app opens directly to the main screen without showing the selection screen again.

**Why this priority**: Forcing users to re-identify themselves on every launch degrades the experience and slows access to the core voice practice feature.

**Independent Test**: Select a profile in Story 1, close and reopen the app, and confirm the selection screen is never shown again and the correct user is still active.

**Acceptance Scenarios**:

1. **Given** a user previously selected "Caio" and the app was closed, **When** the app is reopened, **Then** the selection screen is skipped and the app loads directly with "Caio" as the active user
2. **Given** a user previously selected "Ana" and the device was restarted, **When** the app is reopened, **Then** the app still loads with "Ana" as the active user without asking for re-selection

---

### User Story 3 - Switch Active User (Priority: P3)

A user who previously selected one profile can switch to the other (e.g., Caio hands the device to Ana). This is accessible from the app settings, not the main conversation flow.

**Why this priority**: Two people share one physical device per the Dual-Speaker Mode principle. There must be a way to reassign the device's active solo user without reinstalling the app.

**Independent Test**: Open settings, trigger a profile switch, select the other user, confirm the main screen now reflects the new user and the change persists across a relaunch.

**Acceptance Scenarios**:

1. **Given** "Caio" is the active user, **When** the user navigates to Settings and selects "Switch Profile", **Then** the profile selection screen is displayed again
2. **Given** the profile selection screen is shown from Settings, **When** the user selects "Ana", **Then** "Ana" becomes the active user and the change is persisted locally
3. **Given** a profile switch just occurred, **When** the app is closed and reopened, **Then** the newly selected user is still active

---

### Edge Cases

- What happens if the locally stored profile data becomes corrupted or unreadable? The app MUST fall back to the selection screen as if it were a first launch.
- What if the user closes the app mid-selection before tapping a name? No profile is saved; the selection screen reappears on next launch.
- What if both users are using the app in Dual-Speaker Mode? Profile selection determines the device owner; dual-speaker attribution happens within the session, not at the profile selection level.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: App MUST display a profile selection screen containing exactly two options ("Caio" and "Ana") when no profile is saved locally
- **FR-002**: App MUST persist the selected user profile to local on-device storage immediately upon selection
- **FR-003**: App MUST skip the profile selection screen on subsequent launches when a valid saved profile exists
- **FR-004**: App MUST load and apply the saved user's profile data (preferences, learning goals, voice settings, session history) on each launch
- **FR-005**: App MUST provide a "Switch Profile" option accessible from the Settings screen that re-displays the selection screen
- **FR-006**: App MUST treat corrupted or missing profile storage as a first-launch state and show the selection screen

### Key Entities

- **UserProfile**: Represents one of the two fixed users (Caio or Ana). Attributes: identifier (name), learning goals, preferred conversation themes, AI voice preferences (gender, accent, speech rate), and a reference to session history. Relates to session records and progress data.
- **SavedSession**: Local record of a past conversation session, attributed to a specific UserProfile.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users complete profile selection in under 15 seconds on first launch (two taps: name + confirm, or single tap)
- **SC-002**: 100% of return launches for a user with a saved profile skip the selection screen and load the correct user identity without any interaction
- **SC-003**: Profile selection survives device restarts — the chosen user is correctly restored in 100% of cases after a full device power cycle
- **SC-004**: A profile switch initiated from Settings completes and persists in under 5 seconds

## Assumptions

- No passwords, PINs, or biometric authentication are required; selection is trust-based (MVP scope)
- The two users are permanently fixed as "Caio" and "Ana" — no user creation, deletion, or renaming in this version
- Profile data is stored exclusively on-device (no cloud sync), consistent with Constitution Principle VI
- The selection screen is shown in Brazilian Portuguese (UI language per Constitution Principle IV)
- Single-tap selection with immediate confirmation is sufficient UX for MVP; no intermediate confirmation dialog is needed
- Profile switching is a deliberate, low-frequency action and therefore placed in Settings rather than the main navigation
