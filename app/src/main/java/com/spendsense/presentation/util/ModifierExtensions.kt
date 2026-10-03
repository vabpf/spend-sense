package com.spendsense.presentation.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.BorderSubtle
import com.spendsense.presentation.theme.BorderMedium
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.GlassSurface
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

val LocalGlassHazeState = compositionLocalOf<HazeState?> { null }
@Deprecated("Liquid refraction is deprecated in favor of Haze Frosted Glass", ReplaceWith("LocalGlassHazeState"))
val LocalLiquidState = compositionLocalOf<Any?> { null }

/**
 * Design tokens for the Liquid Glass effect.
 * Values sourced from docs/design.md §"Liquid Glass (API 33+)".
 */
object LiquidTokens {
    val frost = 5.dp
    const val edge = 0.1f
    val tint = Color.Black.copy(alpha = 0.2f)
    const val curve = 0.5f
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLASSMORPHISM EFFECT UTILITIES
// Cyber-Premium style with prism edges and chromatic aberration
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Applies a premium frosted glass effect with:
 * - Frosted glass translucency via Haze
 * - Gradient sheen for depth
 * - Prism edge - subtle rainbow color bleeding on edges
 * - High corner radius support
 */
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
    liquidState: Any? = null,
    contentModifier: Modifier = Modifier
): Modifier {
    var modifier: Modifier = this
    if (liveBlur && hazeState != null) {
        modifier = modifier.hazeEffect(
            state = hazeState,
            style = HazeMaterials.thin()
        )
    }
    modifier = modifier.clip(shape)
    val effectiveColor = if (liveBlur && hazeState != null) {
        containerColor.copy(alpha = containerColor.alpha.coerceAtMost(0.35f))
    } else {
        containerColor
    }
    return modifier
        .background(color = effectiveColor)
        .then(
            if (sheenAlpha > 0f) {
                Modifier.drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = sheenAlpha),
                            0.5f to Color.Transparent
                        )
                    )
                }
            } else Modifier
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
}

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
 *
 * @see [Liquid Glass Guide](docs/LIQUID_GLASS.md)
 */
@Composable
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

/**
 * Interactive bounce clickable effect.
 * Compresses the element scale on press via a low-stiffness spring animation.
 */
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "press_scale_spring"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Interactive bounce clickable effect supporting click and long click.
 * Compresses the element scale on press via a low-stiffness spring animation.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedBounceClickable(
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "press_scale_spring"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

