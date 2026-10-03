# Liquid Glass Implementation & 120 FPS Sibling Architecture

This document details the GPU-accelerated **Liquid Glass** shader pipeline implemented in SpendSense using the `io.github.fletchmckee.liquid` library, the **Sibling Background Architecture** for achieving 120 FPS performance, and the theme integration system.

---

## 1. Architectural Overview

SpendSense utilizes Android 13+ (API 33+) **AGSL (Android Graphics Shading Language)** runtime shaders to simulate real-time optical glass physics:
- **Refraction:** Dynamic bending of background pixels based on normal maps.
- **Chromatic Dispersion:** Separation of RGB channels at edges (prism chromatic aberration).
- **Depth & Lighting:** Specular edge illumination and luminance tinting.

### Core Primitives
- **`LiquidState`**: A state holder coordinating between pixel capture nodes and shader render nodes.
- **`Modifier.liquefiable(liquidState)`**: Flags a Composable subtree as the texture source captured into GPU offscreen buffers.
- **`Modifier.liquid(liquidState)`**: Invokes the AGSL shader pipeline over the captured texture.
- **`Modifier.glassEffect(...)`**: The high-level SpendSense modifier that applies curvature, dispersion, borders, and fallbacks.

---

## 2. The 120 FPS Sibling Background Architecture

### The Problem with Direct Nesting
In naive glassmorphism implementations, developers often wrap the entire screen or individual list items in a single hierarchy where the background and foreground share the same Composable chain. 

When a scrollable list (`LazyColumn`) or animated top bar is placed inside or ancestor to a `.liquefiable` node:
1. Every micro-scroll or recomposition invalidates the background texture buffer.
2. The glass cards attempt to sample the very buffer they are rendering into (recursive sampling).
3. Result: Severe UI jank, frame drops below 30 FPS, and catastrophic `SIGSEGV` or infinite redraw loops on some GPUs.

### The SpendSense Solution: Sibling Decoupling
To achieve rock-solid **120 FPS** on high-refresh displays, every screen in SpendSense uses a strict 2-sibling layout pattern:

```kotlin
@Composable
fun FeatureScreen() {
    val screenLiquidState = rememberLiquidState()

    CompositionLocalProvider(LocalLiquidState provides screenLiquidState) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0)
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // ==========================================
                // Sibling 1: Pure Background Capture Source
                // ==========================================
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquefiable(screenLiquidState)
                ) {
                    AppBackground() // Renders active gradient theme or custom wallpaper
                }

                // ==========================================
                // Sibling 2: Interactive Content (OUTSIDE liquefiable)
                // ==========================================
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(...) {
                        items(...) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassEffect(shape = MaterialTheme.shapes.large)
                            ) {
                                // Content
                            }
                        }
                    }

                    SpendSenseTopBar(...)
                }
            }
        }
    }
}
```

### Why This Works:
1. **Zero Redraw Feedback:** Scrolling the `LazyColumn` in Sibling 2 never invalidates the background texture in Sibling 1.
2. **Instant Sampling:** The AGSL shader reads the static or theme-rendered background texture buffer at full GPU speed.
3. **120 FPS Smoothness:** Flinging lists or dragging items executes with zero dropped frames.

---

## 3. High-End "Cyber-Premium" Shader Parameters

The SpendSense design aesthetic specifies custom optical constants tuned for deep dark backgrounds with neon accents:

```kotlin
modifier.liquid(liquidState = liquidState) {
    frost = 4.dp           // Low frost keeps background shapes readable
    refraction = 0.6f      // Lens distortion level
    curve = 0.3f           // Subtle spherical curvature across the card
    saturation = 1.8f      // Enhances background neon radiance
    dispersion = 0.6f      // RGB chromatic aberration on refractive boundaries
    edge = 0.3f            // Bright specular border illumination
    tint = Color.Black.copy(alpha = 0.35f) // Keeps text readable in dark mode
}
```

### Alpha Clamping Rule
When the AGSL shader is active, setting a card's container alpha too high (e.g. `0.8f` or higher) occludes the refracted pixels, making the card look like flat grey plastic instead of glass.

In [`ModifierExtensions.kt`](file:///home/vab/apps/spend-sense/app/src/main/java/com/spendsense/presentation/util/ModifierExtensions.kt), SpendSense automatically enforces:
```kotlin
val effectiveAlpha = if (hasShader) containerColor.alpha.coerceAtMost(0.35f) else containerColor.alpha
```
This guarantees that glass surfaces are transparent enough for dynamic refraction while retaining enough contrast for typography.

---

## 4. Theme & Wallpaper Integration

All screens render `AppBackground()` in Sibling 1, which listens to composition locals:
- `LocalAppBackgroundTheme`: Subscribes to the theme preset selected in Settings (`Cyberpunk`, `Deep Space`, `Neon Sunset`, `Matrix`, `Minimalist Dark`).
- `LocalCustomBackgroundPath`: If the user has picked a personal photo from device storage, `AppBackground()` loads and scales the bitmap with a subtle dark scrim, and the Liquid Glass shader instantly refracts their custom wallpaper across all cards.

---

## 5. Backward Compatibility & Fallback (API < 33)

For devices running Android 8.0 through Android 12 (APIs 26–32), AGSL runtime shaders are unavailable at the OS level.

`Modifier.glassEffect()` automatically detects the OS version:
- **API >= 33:** Executes the full AGSL `Modifier.liquid()` shader pipeline.
- **API < 33:** Gracefully falls back to GPU blur using the `Haze` library (`Modifier.hazeEffect()`), preserving a premium frosted glass look across older devices without crashing.
