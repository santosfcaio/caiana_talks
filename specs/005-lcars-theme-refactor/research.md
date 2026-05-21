# Research: LCARS Theme UI/UX Refactor

**Branch**: `005-lcars-theme-refactor` | **Date**: 2026-05-20

## Decision 1: LCARS Typeface

**Decision**: Bundle `antonio_regular.ttf` (Antonio, Google Fonts) in `app/src/main/res/font/`.

**Rationale**: Antonio is a narrow, geometric sans-serif on Google Fonts (SIL Open Font License — no commercial restrictions). Its tall, condensed letterforms closely match the official LCARS display font (Swiss911 UCm BT), while being freely bundleable without a network dependency. The constitution limits cloud dependencies to the AI API; using the `ui-text-google-fonts` downloadable font library would add a second cloud call on first launch.

**Alternatives considered**:
- `Orbitron` (Google Fonts): Too wide and round; lacks the narrow condensed silhouette of LCARS readouts.
- `ui-text-google-fonts` downloadable: Network dependency on first launch; unreliable in offline edge cases.
- `Swiss911 UCm BT` (original LCARS font): Commercial license — excluded per spec assumption.

---

## Decision 2: Custom Press Indication (replacing Android ripple)

**Decision**: Implement `LcarsIndication` as a Compose `Indication` + `IndicationInstance` pair. On `PressInteraction.Press`, overlay a 30% black tint on the composable (equivalent to reducing content to ~70% apparent brightness) with no animation duration. On `PressInteraction.Release` or `PressInteraction.Cancel`, remove the tint instantly.

**Rationale**: LCARS computer interactions in TNG are instantaneous state changes with no wave animation. A Compose `Indication` receiving `InteractionSource` events is the correct extension point and lets `LcarsTheme` set it as `LocalIndication` globally, so all `Modifier.clickable` and `Button` interactions inherit it without per-component changes.

**Implementation sketch**:
```kotlin
class LcarsIndicationInstance(
    private val interactionSource: InteractionSource,
    private val coroutineScope: CoroutineScope
) : IndicationInstance {
    private var pressed by mutableStateOf(false)

    init {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                pressed = when (interaction) {
                    is PressInteraction.Press -> true
                    else -> false
                }
            }
        }
    }

    override fun ContentDrawScope.drawIndication() {
        drawContent()
        if (pressed) drawRect(color = Color.Black.copy(alpha = 0.3f))
    }
}
```

**Alternatives considered**:
- `graphicsLayer { alpha }` per component: Requires per-component wiring; doesn't scale.
- `rememberRipple(color = LcarsOrange)`: Retains the expanding ripple wave — inconsistent with LCARS aesthetic.
- No indication at all: Fails FR-009 (accessibility); users cannot confirm a tap registered.

---

## Decision 3: Navigation Transitions

**Decision**: Use the `enterTransition`, `exitTransition`, `popEnterTransition`, `popExitTransition` lambdas on `NavHost` (Navigation Compose 2.7.7, already in project). Apply horizontal slide + fade (300 ms, `FastOutSlowInEasing`) for forward/back navigation. Use fade-only (400 ms) for root-to-root transitions (profile selection → home screen tree replacement).

**Rationale**: Navigation Compose 2.7+ exposes transition lambdas natively — no `AnimatedNavHost` import or additional dependency required. Horizontal slide matches LCARS scanline horizontal motion. 300 ms with FastOutSlowIn stays within standard Android motion guidance and avoids sluggish feel. Root transitions use fade-only because they replace the entire screen tree (not a push/pop), so a directional slide would be spatially confusing.

**Implementation sketch**:
```kotlin
NavHost(
    navController = navController,
    startDestination = "home",
    enterTransition = {
        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(300))
    },
    exitTransition = {
        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } +
        fadeOut(tween(300))
    },
    popEnterTransition = {
        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } +
        fadeIn(tween(300))
    },
    popExitTransition = {
        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } +
        fadeOut(tween(300))
    }
)
```

**Alternatives considered**:
- No transitions: Default Android back-stack animation remains, violating FR-010.
- `AnimatedNavHost` (accompanist): Library is deprecated; Navigation Compose has native support.
- Scale/expand transition: Material-style; does not fit LCARS horizontal scanline aesthetic.

---

## Decision 4: LCARS Canonical Color Tokens

**Decision**: The following palette is the canonical LCARS TNG color set, derived from production reference screenshots and the `lcars.com` color specification.

| Token | Hex | Role |
|-------|-----|------|
| `LcarsBlack` | `#000000` | Screen background (all screens) |
| `LcarsOrange` | `#FF7700` | Primary accent (framing, primary buttons) |
| `LcarsBlue` | `#6688CC` | Secondary accent (data panels, config screens) |
| `LcarsPurple` | `#CC99CC` | Tertiary accent (settings, grouped options) |
| `LcarsBeige` | `#FFCC88` | Warm highlight (results, profile panels) |
| `LcarsMaroon` | `#CC2200` | Alert / active session state |
| `LcarsText` | `#FFEECC` | Primary text on black |
| `LcarsTextDim` | `#997755` | Secondary / dimmed text |
| `LcarsSurface` | `#111111` | Slightly elevated surface (data panels) |

**Rationale**: `#FFEECC` for text matches the warm amber glow of LCARS display readouts; pure white (#FFFFFF) would feel clinical and inconsistent with the reference aesthetic. Orange `#FF7700` is the canonical reddish-orange used for the most prominent LCARS framing bars.

**Alternatives considered**:
- `#FFFFFF` text: Too harsh on the black LCARS background; loses the warm tint.
- `#FF9900` orange: Slightly too yellow; `#FF7700` is the more faithful reddish-orange.
