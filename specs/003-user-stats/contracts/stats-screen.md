# UI Contract: StatsScreen

**Phase**: 1 — Design & Contracts
**Feature**: 003-user-stats

---

## Composable Signature

```kotlin
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
)
```

---

## ViewModel State

```kotlin
data class StatsUiState(
    val isLoading: Boolean = true,
    val cefrLevel: CefrLevel? = null,
    val grammarErrors: Int = 0,
    val vocabularyErrors: Int = 0,
    val fluencyErrors: Int = 0,
    val sessions: List<SessionSummary> = emptyList(),
    val insights: List<String> = emptyList()
)
```

`StatsViewModel` exposes `uiState: StateFlow<StatsUiState>` collected from
`StatsRepository.getProgressSnapshot(profileId)`. The `profileId` is obtained
from `UserRepository.getActiveUserProfile()` in `init {}`.

```kotlin
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val uiState: StateFlow<StatsUiState>
}
```

---

## Screen Layout

```
┌────────────────────────────────────┐
│ ←  Meu Progresso                   │  TopAppBar — back button + title
├────────────────────────────────────┤
│                                    │
│  ┌──────────────────────────────┐  │
│  │  Nível de Inglês             │  │  Section 1: CEFR Level Card
│  │  ─────────────────────────  │  │
│  │  B1 — Intermediário          │  │
│  │  "Você lida com a maioria…"  │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  Erros por Categoria         │  │  Section 2: Error Breakdown Card
│  │  ─────────────────────────  │  │
│  │  Gramática:     12 erros     │  │
│  │  Vocabulário:    8 erros     │  │
│  │  Fluência:       3 erros     │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  Histórico de Sessões        │  │  Section 3: Session History (lazy list)
│  │  ─────────────────────────  │  │
│  │  22 mai 2026 · 15min         │  │
│  │  • Gramática: "used 'make'"  │  │
│  │  ─────────────────────────  │  │
│  │  20 mai 2026 · 8min          │  │
│  │  • Nenhuma correção          │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │  Insights                    │  │  Section 4: Insights Card
│  │  ─────────────────────────  │  │
│  │  "Você comete mais erros…"   │  │
│  └──────────────────────────────┘  │
│                                    │
└────────────────────────────────────┘
```

The four sections are wrapped in a `LazyColumn` so all are reachable with a single scroll gesture (SC-004).

---

## Empty States

Each section shows a message rather than hiding itself when data is absent (FR-007):

| Section | Condition | Empty-state message |
|---------|-----------|---------------------|
| Nível de Inglês | `cefrLevel == null` | `"Conclua uma sessão para ver seu nível estimado."` |
| Erros por Categoria | All counts == 0 | Each row shows `"0 erros"` (never hidden) |
| Histórico de Sessões | `sessions.isEmpty()` | `"Nenhuma sessão concluída ainda."` |
| Session corrections | Session had 0 corrections | `"Nenhuma correção registrada."` |
| Insights | `insights.isEmpty()` | `"Conclua mais sessões para desbloquear insights."` |

---

## Loading State

While `isLoading == true`, a centered `CircularProgressIndicator` is shown in place of all sections. Transitions to content once the first `ProgressSnapshot` emission arrives.

---

## Navigation

| Property | Value |
|----------|-------|
| Route | `"stats"` |
| Entry point | `HomeScreen` — "Ver meu progresso" button |
| Back action | `onNavigateBack()` → pops back stack to `"home"` |
| Taps from Home | 1 (satisfies FR-008: ≤ 2 taps) |

---

## Constraints & Rules

- Corrections per session capped at **5** for display (spec US3); extras are silently omitted in `SessionSummary`.
- Duration display:
  - `durationMinutes < 60` → `"Xmin"` (e.g., `"15min"`)
  - `durationMinutes >= 60` → `"Xh Ymin"` (e.g., `"2h 15min"`)
- Profile scope: `StatsViewModel` reacts to `UserRepository.getActiveUserProfile()` — switching profiles triggers a new `profileId` emission, which causes `StatsRepository` to recompute `ProgressSnapshot` for the new profile (FR-006, SC-005).
- All UI text in Brazilian Portuguese; CEFR codes (A1–C2) displayed in English shorthand (Constitution Principle IV).

---

## HomeScreen Change

```kotlin
// HomeScreen.kt — add below the greeting text
Button(onClick = onNavigateToStats) {
    Text("Ver meu progresso")
}
```

```kotlin
// AppNavGraph.kt — add stats composable
composable("stats") {
    StatsScreen(onNavigateBack = { navController.popBackStack() })
}
```

```kotlin
// HomeScreen signature change
@Composable
fun HomeScreen(
    userName: String,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit    // NEW
)
```
