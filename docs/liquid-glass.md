# Frosted Glass Implementation & 120 FPS Architecture

This document details the high-performance **Frosted Glass** system implemented in SpendSense using `dev.chrisbanes.haze`, the dual-tier rendering strategy for 120 FPS performance, and the theme integration engine.

---

## 1. Architectural Overview

SpendSense uses a dual-tier Frosted Glass architecture designed for maximum visual fidelity and zero scrolling latency:

1. **Active Frosted Backdrop Blur (`liveBlur = true`):**
   - Applied to persistent floating chrome (Bottom Navigation Bar, Top Bar, Floating Action Button, Floating Selection Toolbar, Dialogs, and Action Overlay).
   - Uses `dev.chrisbanes.haze:haze` with `HazeMaterials.thin()` for hardware-accelerated GPU backdrop blur (`RenderEffect.createBlurEffect` on Android 12+).
   - Because only 1–2 blur nodes exist on screen at once, the GPU maintains a locked 120 FPS.

2. **Frosted Surface Glass (`liveBlur = false`):**
   - Applied to all repeating items in `LazyColumn` and grid layouts (`TransactionItem`, `CategoryItem`, settings cards, charts).
   - Uses translucent tinted glass (`GlassSurface.copy(alpha = 0.65f)`), specular gradient border (`borderAlpha = 0.20f`), and a subtle top-to-bottom specular sheen (`sheenAlpha = 0.08f`).
   - **Zero GPU offscreen buffer allocations, zero shader overhead, 100% 120 FPS buttery-smooth scrolling.**

---

## 2. Global Setup (MainActivity)

The single root background renders `AppBackground()` once and is tagged as the Haze blur source:

```kotlin
val hazeState = rememberHazeState()

CompositionLocalProvider(
    LocalGlassHazeState provides hazeState,
    LocalAppBackgroundTheme provides backgroundTheme,
    LocalCustomBackgroundPath provides customBackgroundPath
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Single root background providing pixels for Haze blur
        AppBackground(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        )

        // Screen content
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(containerColor = Color.Transparent) { innerPadding ->
                NavHost(navController = navController, startDestination = "home") {
                    // Composables...
                }
            }
        }

        // Floating Bottom Navigation Bar with live blur
        SpendSenseBottomNav(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .glassEffect(
                    shape = RoundedCornerShape(999.dp),
                    containerColor = GlassSurface.copy(alpha = 0.70f),
                    borderAlpha = 0.20f,
                    sheenAlpha = 0.08f,
                    liveBlur = true,
                    hazeState = hazeState
                )
        )
    }
}
```

---

## 3. High-End Design Parameters

In [`ModifierExtensions.kt`](file:///home/vab/apps/spend-sense/app/src/main/java/com/spendsense/presentation/util/ModifierExtensions.kt), `Modifier.glassEffect(...)` automatically coordinates parameters:

```kotlin
@Composable
fun Modifier.glassEffect(
    shape: Shape,
    containerColor: Color = GlassSurface.copy(alpha = 0.65f),
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 0.20f,
    sheenAlpha: Float = 0.08f,
    prismAlpha: Float = 0.04f,
    liveBlur: Boolean = false,
    hazeState: HazeState? = LocalGlassHazeState.current,
    contentModifier: Modifier = Modifier
): Modifier
```

- When `liveBlur = true`: applies `modifier.hazeEffect(...)` and caps alpha at `0.35f` so the blur shines through clearly.
- When `liveBlur = false`: uses the full translucent `containerColor.alpha` (e.g. `0.65f - 0.75f`) with gradient border and top sheen for pure 120 FPS performance.

---

## 4. Theme & Wallpaper Integration

All screens float over the single root `AppBackground()`, which responds to:
- `LocalAppBackgroundTheme`: 5 preset themes (`Cyberpunk`, `Deep Space`, `Neon Sunset`, `Matrix`, `Minimalist Dark`).
- `LocalCustomBackgroundPath`: Custom background photo chosen from user storage, automatically blurred by floating chrome and visible through translucent list cards.
