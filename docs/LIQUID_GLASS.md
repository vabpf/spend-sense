# Liquid Glass Implementation Guide

This document details how to apply the "Liquid Glass" effect in the SpendSense application using the `io.github.fletchmckee.liquid` library.

## 1. Architectural Overview

The Liquid Glass effect uses GPU-accelerated AGSL shaders (Android 13+) to sample background pixels and apply real-time refraction and dispersion.

### Core Components
- **`LiquidState`**: Shared state that links background sources to foreground effects.
- **`Modifier.liquefiable(liquidState)`**: Tags a composable as a source for the glass to sample.
- **`Modifier.liquid(liquidState)`**: Applies the actual glass effect.

## 2. Global Setup (MainActivity)

The root background is marked as `liquefiable` to allow all child components to "see through" to the app-wide gradients.

```kotlin
val liquidState = rememberLiquidState()

CompositionLocalProvider(LocalLiquidState provides liquidState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .liquefiable(liquidState = liquidState) // MUST come before background
            .background(Brush.verticalGradient(...))
    ) {
        // App Content
    }
}
```

## 3. Applying the Effect (glassEffect)

Use the optimized `.glassEffect()` or `.glassCard()` modifiers defined in `ModifierExtensions.kt`. These handles the API 33+ detection and fallback to the `Haze` library for older devices.

### Recommended "Cyber-Premium" Parameters
Our design system standard for high-end glass is:

```kotlin
modifier.liquid(liquidState = liquidState) {
    frost = 4.dp           // Low frost keeps neon shapes sharp
    refraction = 0.6f      // Strong lens distortion
    curve = 0.3f           // Subtle spherical curvature
    saturation = 1.8f      // Boosts the glow of background colors
    dispersion = 0.6f      // RGB chromatic aberration (glitchy edge feel)
    edge = 0.3f            // Bright edge lighting
    tint = Color.Black.copy(alpha = 0.35f) // Keeps it dark-mode friendly
}
```

## 4. Critical Rules

1.  **Modifier Order (Source)**: `Modifier.liquefiable()` must be applied **BEFORE** any `.background()`, `.border()`, or `.shadow()` modifiers on the background element.
2.  **Modifier Order (Effect)**: `Modifier.liquid()` should be applied **BEFORE** `.clip()` to ensure the refracted pixels aren't cut off prematurely.
3.  **No Recursive Sampling**: A `liquid` node cannot have an ancestor `liquefiable` node outside of its own chain if they share the same state. Doing so will cause a fatal `SIGSEGV` crash.
4.  **Fallback**: The system automatically falls back to `Modifier.hazeEffect()` on API < 33.

## 5. Design Tokens (DESIGN.md)

Refer to `DESIGN.md` for the latest `liquid-*` tokens to maintain consistency across the app.
