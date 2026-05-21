# Data Model: LCARS Theme UI/UX Refactor

**Branch**: `005-lcars-theme-refactor` | **Date**: 2026-05-20

No new domain entities, database tables, or data layer changes. This document defines the UI design token model and reusable component API contracts that implement the LCARS theme.

---

## Color Tokens — `ui/theme/LcarsColors.kt`

```kotlin
object LcarsColors {
    val Black    = Color(0xFF000000)  // background — all screens
    val Orange   = Color(0xFFFF7700)  // primary accent
    val Blue     = Color(0xFF6688CC)  // secondary accent
    val Purple   = Color(0xFFCC99CC)  // tertiary accent
    val Beige    = Color(0xFFFFCC88)  // warm highlight
    val Maroon   = Color(0xFFCC2200)  // alert / active session
    val Text     = Color(0xFFFFEECC)  // primary text on black
    val TextDim  = Color(0xFF997755)  // secondary / dimmed text
    val Surface  = Color(0xFF111111)  // slightly elevated surface
}
```

### Material3 ColorScheme mapping

| Material3 role | LCARS token |
|----------------|-------------|
| `background` | `Black` |
| `surface` | `Surface` |
| `primary` | `Orange` |
| `secondary` | `Blue` |
| `tertiary` | `Purple` |
| `error` | `Maroon` |
| `onBackground` | `Text` |
| `onSurface` | `Text` |
| `onPrimary` | `Black` |
| `onSecondary` | `Black` |
| `onTertiary` | `Black` |
| `onError` | `Text` |
| `surfaceVariant` | `Surface` |
| `onSurfaceVariant` | `TextDim` |

---

## Typography Tokens — `ui/theme/LcarsTypography.kt`

Font: `antonio_regular.ttf` (Antonio Regular, SIL OFL), loaded via `FontFamily(Font(R.font.antonio_regular))`.

**Rule**: Antonio is used for all text slots ≥ 16 sp (`titleMedium` and above). Roboto (system default) is used for all slots below 16 sp.

| Scale slot | Size | Font | Letter Spacing | Usage |
|------------|------|------|----------------|-------|
| `displayLarge` | 32 sp | Antonio | +2 sp | Screen section headers |
| `displayMedium` | 28 sp | Antonio | +2 sp | Panel / screen titles |
| `headlineLarge` | 24 sp | Antonio | +1 sp | Data panel headers |
| `headlineMedium` | 20 sp | Antonio | +1 sp | Sub-headers |
| `titleLarge` | 18 sp | Antonio | +0.5 sp | Section labels, button text |
| `titleMedium` | 16 sp | Antonio | +0.5 sp | Minimum LCARS font size |
| `bodyLarge` | 16 sp | Roboto | 0 | Body text (readable) |
| `bodyMedium` | 14 sp | Roboto | 0 | Secondary body |
| `bodySmall` | 12 sp | Roboto | 0 | Small body text |
| `labelLarge` | 14 sp | Roboto Medium | 0 | Form labels, metadata |
| `labelMedium` | 12 sp | Roboto Medium | 0 | Tags, chips |
| `labelSmall` | 11 sp | Roboto Medium | 0 | Fine print |

---

## Shape Tokens — `ui/theme/LcarsShapes.kt`

| Name | Value | Usage |
|------|-------|-------|
| `pill` | `CircleShape` | All buttons, option pills |
| `panelElbow` | `RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)` | Left framing strip |
| `dataPanel` | `RoundedCornerShape(4.dp)` | Data panel containers |

---

## Component API Contracts

### `LcarsButton` — `ui/theme/components/LcarsButton.kt`

```kotlin
@Composable
fun LcarsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LcarsColors.Orange,
    textColor: Color = LcarsColors.Black,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
    content: @Composable RowScope.() -> Unit
)
```

- Shape: `CircleShape` (pill, rounded both ends)
- Disabled: `color.copy(alpha = 0.3f)` background, `textColor.copy(alpha = 0.3f)` text
- `fullWidth = true` → `Modifier.fillMaxWidth()` applied; `false` → `wrapContentWidth()` with min padding 24 dp horizontal
- Press feedback: inherited from `LocalIndication` (`LcarsIndication`)

---

### `LcarsFrame` — `ui/theme/components/LcarsFrame.kt`

```kotlin
@Composable
fun LcarsFrame(
    accentColor: Color,
    modifier: Modifier = Modifier,
    topBarHeight: Dp = 48.dp,
    bottomBarHeight: Dp = 24.dp,
    leftStripWidth: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
)
```

- Top bar: `accentColor` fill, `RectangleShape`, full width, `topBarHeight` tall
- Bottom bar: `accentColor.copy(alpha = 0.6f)` fill, full width, `bottomBarHeight` tall
- Left strip: `accentColor` fill, `panelElbow` shape (rounded top-left + bottom-left), full height (minus top/bottom bars), `leftStripWidth` wide
- Content area: starts at `leftStripWidth + 8.dp` from left edge, between top and bottom bars
- Does NOT replace `LcarsTopBar`; both coexist — `LcarsFrame` handles structural framing, `LcarsTopBar` handles the title bar

---

### `LcarsTopBar` — `ui/theme/components/LcarsTopBar.kt`

```kotlin
@Composable
fun LcarsTopBar(
    title: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
)
```

- Height: 56 dp
- Background: `accentColor`
- Title text: `LcarsColors.Black`, `displayMedium` (28 sp Antonio), uppercase
- Navigation and action icons: tinted `LcarsColors.Black`
- Replaces `TopAppBar` (Material3) on every screen

---

### `LcarsDataPanel` — `ui/theme/components/LcarsDataPanel.kt`

```kotlin
@Composable
fun LcarsDataPanel(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

- Background: `LcarsColors.Surface`
- Left border: 3 dp solid `accentColor`
- Corner shape: `dataPanel` (4 dp rounded)
- Replaces `Card` everywhere

---

### `LcarsProgressBar` — `ui/theme/components/LcarsProgressBar.kt`

```kotlin
@Composable
fun LcarsProgressBar(
    modifier: Modifier = Modifier,
    segmentCount: Int = 12,
    color: Color = LcarsColors.Orange
)
```

- Horizontal row of `segmentCount` pill-shaped segments
- Animated: segments fill left-to-right, one per 250 ms, looping
- Segment active: `color` fill; segment inactive: `color.copy(alpha = 0.2f)`
- Height: 16 dp; each segment separated by 4 dp gap
- Replaces `CircularProgressIndicator` everywhere

---

### `LcarsStatusIndicator` — `ui/theme/components/LcarsStatusIndicator.kt`

```kotlin
@Composable
fun LcarsStatusIndicator(
    label: String,
    color: Color = LcarsColors.Orange,
    blinking: Boolean = false,
    modifier: Modifier = Modifier
)
```

- Small circle dot (8 dp diameter) + `titleMedium` label text, in a Row
- `blinking = true`: dot pulses 100% → 30% alpha on 600 ms `InfiniteTransition`
- Replaces phase label `Text` in `ConversationScreen`

---

### `LcarsOptionPills` — `ui/theme/components/LcarsOptionPills.kt`

```kotlin
@Composable
fun <T> LcarsOptionPills(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    accentColor: Color = LcarsColors.Blue,
    modifier: Modifier = Modifier
)
```

- `FlowRow` (or `Row` if options fit) of `LcarsButton(fullWidth = false)` pills
- Selected: `accentColor` background, `Black` text
- Unselected: transparent background, `accentColor` border + `accentColor` text
- Replaces `RadioButton` rows in `ProfileEditScreen`

---

### `LcarsCheckRow` — `ui/theme/components/LcarsOptionPills.kt` (same file)

```kotlin
@Composable
fun LcarsCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = LcarsColors.Blue,
    modifier: Modifier = Modifier
)
```

- Row: small square with `accentColor` fill (checked) or `accentColor` border only (unchecked) + label
- Min touch target: 48 × 48 dp (FR-009)
- Replaces `Checkbox` rows in `ProfileEditScreen`

---

### `LcarsIndication` — `ui/theme/LcarsIndication.kt`

```kotlin
object LcarsIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance
}
```

- On `PressInteraction.Press`: overlay `Color.Black.copy(alpha = 0.3f)` tint over full composable bounds
- On `PressInteraction.Release` / `Cancel`: remove tint instantly (no animation)
- Applied globally in `LcarsTheme` via `CompositionLocalProvider(LocalIndication provides LcarsIndication)`
