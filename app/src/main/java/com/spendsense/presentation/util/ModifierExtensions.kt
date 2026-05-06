package com.spendsense.presentation.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.BorderSubtle
import com.spendsense.presentation.theme.BorderMedium
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.GlassSurface

// ═══════════════════════════════════════════════════════════════════════════════
// GLASSMORPHISM EFFECT UTILITIES
// Cyber-Premium style with prism edges and chromatic aberration
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Applies a premium glassmorphism effect with:
 * - Frosted glass translucency (semi-transparent background)
 * - Gradient sheen for depth
 * - Prism edge - subtle rainbow color bleeding on edges (chromatic aberration)
 * - High corner radius support
 *
 * This creates the "frosted glass" look where elements appear as 
 * semi-transparent layers over the background with subtle rainbow highlights.
 */
fun Modifier.glassEffect(
    shape: Shape,
    containerColor: Color = GlassSurface.copy(alpha = 0.75f),
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 0.15f,
    sheenAlpha: Float = 0.08f,
    prismAlpha: Float = 0.04f,
    contentModifier: Modifier = Modifier
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                containerColor.copy(alpha = containerColor.alpha * 0.88f),
                containerColor,
                containerColor.copy(alpha = containerColor.alpha * 0.72f)
            )
        )
    )
    .background(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = sheenAlpha),
                Color.Transparent
            )
        )
    )
    .then(contentModifier)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = borderAlpha * 0.6f),
                Color.Transparent,
                Color.White.copy(alpha = borderAlpha * 0.4f)
            )
        ),
        shape = shape
    )

/**
 * Applies a PRISM EDGE effect - subtle chromatic aberration on edges
 * Creates that "rainbow-like color bleeding" high-tech premium feel
 * 
 * @param accentColor The primary accent color to tint the prism effect
 * @param intensity Strength of the effect (0.0 to 1.0)
 */
fun Modifier.prismEdge(
    shape: Shape,
    accentColor: Color = CyberBlue,
    intensity: Float = 0.5f,
    borderWidth: Dp = 1.dp
): Modifier = this.border(
    width = borderWidth,
    brush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.3f * intensity),
            accentColor.copy(alpha = 0.1f * intensity),
            Color.Transparent,
            accentColor.copy(alpha = 0.15f * intensity),
            accentColor.copy(alpha = 0.05f * intensity)
        )
    ),
    shape = shape
)

/**
 * GLOSSY OVERLAY - for interactive elements that need extra shine
 * Adds a subtle top-light reflection
 */
fun Modifier.glossyOverlay(
    shape: Shape,
    alpha: Float = 0.12f
): Modifier = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = alpha),
            Color.Transparent,
            Color.Black.copy(alpha = alpha * 0.3f)
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    ),
    shape = shape
)

/**
 * NEON GLOW - for action buttons and critical elements
 * Creates a subtle outer glow effect
 */
fun Modifier.neonGlow(
    color: Color = CyberBlue,
    intensity: Float = 0.4f
): Modifier = this
    .background(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = intensity),
                color.copy(alpha = intensity * 0.5f),
                Color.Transparent
            )
        )
    )

/**
 * GLASS CARD - full glassmorphism with optional border
 * Convenience modifier combining glass + prism edge
 */
fun Modifier.glassCard(
    shape: Shape,
    containerColor: Color = GlassSurface.copy(alpha = 0.82f),
    borderAlpha: Float = 0.2f,
    hasPrism: Boolean = true,
    prismColor: Color = CyberBlue
): Modifier = glassEffect(
    shape = shape,
    containerColor = containerColor,
    borderAlpha = borderAlpha
).let { mod ->
    if (hasPrism) {
        mod.prismEdge(shape = shape, accentColor = prismColor, intensity = 0.3f)
    } else {
        mod
    }
}
