# Screen UI Contracts: LCARS Theme

**Branch**: `005-lcars-theme-refactor` | **Date**: 2026-05-20

Each screen specifies its accent palette (max 3), frame color, top bar presence, and structural notes. These contracts are the source of truth for per-screen color and layout decisions during implementation.

---

## ProfileSelectionScreen

| Property | Value |
|----------|-------|
| Accent palette | Orange, Beige, Purple |
| `LcarsFrame` color | Orange |
| `LcarsTopBar` | Absent (full-screen entry point, no back navigation) |

**Layout**: Black background, `LcarsFrame(Orange)` surrounds centered column. Headline "QUEM ESTÁ USANDO O APP?" in `displayLarge` Antonio. Profile name buttons as full-width `LcarsButton(color = Beige, textColor = Black)`. Loading state: `LcarsProgressBar(color = Orange)` centered on black screen.

---

## HomeScreen

| Property | Value |
|----------|-------|
| Accent palette | Orange, Blue, Beige |
| `LcarsFrame` color | Orange |
| `LcarsTopBar` | Present — color: Orange, title: "CAIANA TALKS", settings icon |

**Layout**: Greeting "Olá, {name}!" in `displayMedium` Antonio below top bar. Three primary actions stacked vertically: `LcarsButton(color = Orange)` for Iniciar Conversa, `LcarsButton(color = Blue)` for Co-practice, `LcarsButton(color = Beige, textColor = Black)` for Ver meu progresso.

---

## ProfileEditScreen

| Property | Value |
|----------|-------|
| Accent palette | Blue, Orange, Purple |
| `LcarsFrame` color | Blue |
| `LcarsTopBar` | Present — color: Blue, title: "EDITAR PERFIL", back icon (hidden during onboarding) |

**Layout**: Section headers ("META DE APRENDIZADO", "TEMAS PREFERIDOS", "CONFIGURAÇÕES DE VOZ") rendered as `LcarsDataPanel(accentColor = Orange)` label rows using `titleLarge` Antonio in Orange. `LcarsOptionPills(accentColor = Blue)` for `LearningGoal`, `VoiceGender`, `VoiceAccent`. `LcarsOptionPills(accentColor = Purple)` for `SpeechRate`. `LcarsCheckRow(accentColor = Blue)` for `ConversationTheme`. Save button: `LcarsButton(color = Orange)` full-width at bottom.

---

## SettingsScreen

| Property | Value |
|----------|-------|
| Accent palette | Purple, Orange, Blue |
| `LcarsFrame` color | Purple |
| `LcarsTopBar` | Present — color: Purple, title: "CONFIGURAÇÕES", back icon |

**Layout**: Section header "CONTA" as `LcarsDataPanel(accentColor = Orange)` label. "Editar preferências" button: `LcarsButton(color = Orange)`. "Trocar perfil" button: `LcarsButton(color = Blue)`.

---

## CoPracticeSetupScreen

| Property | Value |
|----------|-------|
| Accent palette | Blue, Purple, Orange |
| `LcarsFrame` color | Blue |
| `LcarsTopBar` | Present — color: Blue, title: "CO-PRACTICE" |

**Layout**: Two participant selection panels, each a `LcarsDataPanel(accentColor = Blue)` with header in `titleLarge` Antonio. Profile rows: full-width `LcarsButton(color = if selected Purple else transparent)` with `accentColor` border when unselected. Start button: `LcarsButton(color = Orange)` at bottom, disabled until both selections made.

---

## ConversationScreen

| Property | Value |
|----------|-------|
| Accent palette | Orange, Maroon, Beige |
| `LcarsFrame` color | Maroon |
| `LcarsTopBar` | Present — color: Maroon, title: AI persona name, close icon in Orange |

**Layout**: Turn history: each turn as `LcarsDataPanel(accentColor = Orange)` — speaker name in `titleMedium` Antonio, user text in `bodyMedium` Roboto, AI correction in `bodySmall` Roboto. Phase indicator: `LcarsStatusIndicator(blinking = phase in {LISTENING, THINKING}, color = Orange)`. Audio spectrum: bars recolored to `LcarsColors.Orange` (replace hardcoded `0xFF6200EE`). Portuguese nudge: `LcarsDataPanel(accentColor = Beige)`. Error state: `LcarsDataPanel(accentColor = Maroon)` containing error text + `LcarsButton(color = Maroon)` retry. End session: `LcarsButton(color = Maroon)` full-width at bottom.

---

## SessionSummaryScreen

| Property | Value |
|----------|-------|
| Accent palette | Orange, Beige, Blue |
| `LcarsFrame` color | Orange |
| `LcarsTopBar` | Present — color: Orange, title: "RESUMO DA SESSÃO" |

**Layout**: Loading: `LcarsProgressBar(color = Orange)` centered. Per-participant: `LcarsDataPanel(accentColor = Beige)` with participant name in `headlineLarge` Antonio. Duration/correction count: `titleMedium` Antonio labels aligned right. Vocabulary highlights: each as `LcarsDataPanel(accentColor = Blue)` inline item. "Voltar ao início" button: `LcarsButton(color = Orange)`.

---

## StatsScreen

| Property | Value |
|----------|-------|
| Accent palette | Blue, Beige, Orange |
| `LcarsFrame` color | Blue |
| `LcarsTopBar` | Present — color: Blue, title: "MEU PROGRESSO", back icon |

**Layout**: Loading: `LcarsProgressBar(color = Blue)`. All four data cards become `LcarsDataPanel(accentColor = Blue)`. CEFR level label: `displayLarge` Antonio in Orange. Error count rows: category in `bodyMedium` Roboto, count in `titleMedium` Antonio right-aligned. Session items: date/duration in `titleMedium` Antonio, correction bullets in `bodyMedium` Roboto. Insights: `LcarsDataPanel(accentColor = Beige)` with `bodyMedium` Roboto text.

---

## AppNavGraph (Navigation Transitions)

All `NavHost` instances receive these default transitions applied at the `NavHost` level:

| Event | Transition |
|-------|-----------|
| Forward enter | `slideInHorizontally { it } + fadeIn(tween(300))` |
| Forward exit | `slideOutHorizontally { -it } + fadeOut(tween(300))` |
| Back (pop) enter | `slideInHorizontally { -it } + fadeIn(tween(300))` |
| Back (pop) exit | `slideOutHorizontally { it } + fadeOut(tween(300))` |

**Root transitions** (profile-selection → home, profile-setup → home): `fadeIn(tween(400))` / `fadeOut(tween(400))` only — no slide, since these replace the entire screen tree rather than pushing to a back stack.
