package com.spendsense.presentation.util

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
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
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.GlassSurface
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
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

val LocalBackdrop = compositionLocalOf<Backdrop?> { null }
/** Backward-compat alias for call-sites that still reference LocalGlassHazeState. */
val LocalGlassHazeState = LocalBackdrop
@Deprecated("Liquid refraction is deprecated in favor of Backdrop", ReplaceWith("LocalBackdrop"))
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

/**
 * Unified Design Tokens for Frosted Glass across the app.
 * Provides a single source of truth for cards, floating bars, dialogs, and controls.
 */
object FrostGlassDefaults {
    val containerColor: Color = Color.White.copy(alpha = 0.75f)
    val liveBlurContainerColor: Color = Color.White.copy(alpha = 0.75f)
    val borderWidth: Dp = 1.dp
    const val borderAlpha: Float = 0.22f
    const val sheenAlpha: Float = 0.10f
    const val prismAlpha: Float = 0.04f
}

// ═══════════════════════════════════════════════════════════════════════════════
// GLASSMORPHISM EFFECT UTILITIES
// Cyber-Premium style with prism edges and chromatic aberration
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Applies a premium frosted glass effect with:
 * - Real Liquid Glass backdrop sampling via [io.github.kyant0:backdrop]
 * - Vibrancy → Blur (14dp) → Lens refraction (on API 33+) pipeline for liveBlur surfaces
 * - Gradient sheen for depth
 * - Prism edge - subtle rainbow color bleeding on edges
 * - High corner radius support
 *
 * @param liveBlur When true (default), uses [drawBackdrop] for real-time blur sampling
 *   whenever a backdrop is available. When false, renders a lightweight static frosted surface.
 * @param backdrop Optional explicit [Backdrop] to sample (defaults to [LocalBackdrop.current]).
 * @param hazeState Kept for source compatibility; unused — pass null or omit.
 */
@Composable
fun Modifier.glassEffect(
    shape: Shape,
    containerColor: Color = FrostGlassDefaults.containerColor,
    borderWidth: Dp = FrostGlassDefaults.borderWidth,
    borderAlpha: Float = FrostGlassDefaults.borderAlpha,
    sheenAlpha: Float = FrostGlassDefaults.sheenAlpha,
    prismAlpha: Float = FrostGlassDefaults.prismAlpha,
    liveBlur: Boolean = true,
    useLens: Boolean = true,
    backdrop: Backdrop? = null,
    hazeState: Any? = null, // kept for call-site source-compat; ignored
    liquidState: Any? = null,
    contentModifier: Modifier = Modifier
): Modifier {
    val activeBackdrop = backdrop ?: LocalBackdrop.current
    val effectiveColor = if (liveBlur && activeBackdrop != null) {
        if (containerColor == FrostGlassDefaults.containerColor) {
            FrostGlassDefaults.liveBlurContainerColor
        } else {
            containerColor
        }
    } else {
        containerColor
    }

    return if (liveBlur && activeBackdrop != null) {
        // Live blur path: official Backdrop pipeline with enhanced 14dp blur and lens refraction
        this.drawBackdrop(
            backdrop = activeBackdrop,
            shape = { shape },
            effects = {
                vibrancy()
                // Increased blur for pure transparent glass without base color
                blur(14f.dp.toPx())
                // Lens refraction is API 33+ and strictly requires CornerBasedShape.
                if (useLens && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shape is CornerBasedShape) {
                    try {
                        lens(refractionHeight = 16f.dp.toPx(), refractionAmount = 16f.dp.toPx())
                    } catch (_: Throwable) {
                        // Fall back gracefully to vibrancy + blur without crashing
                    }
                }
            },
            onDrawSurface = {
                // Frosted tint (only drawn when a non-transparent color is specified)
                if (effectiveColor != Color.Transparent && effectiveColor.alpha > 0f) {
                    drawRect(effectiveColor)
                }
                // Specular sheen gradient
                if (sheenAlpha > 0f) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = sheenAlpha),
                            0.5f to Color.Transparent
                        )
                    )
                }
            }
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
    } else {
        // Static glass path: lightweight, zero-overhead for list items and cards
        this.clip(shape)
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
    containerColor: Color = FrostGlassDefaults.containerColor,
    borderAlpha: Float = FrostGlassDefaults.borderAlpha,
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

/**
 * Smoothly dissolves/fades content edges (top and/or bottom) using GPU-accelerated alpha blend masks.
 * [topFadeStart] allows keeping an area transparent before starting the fade into content (e.g. pinned headers).
 */
fun Modifier.fadingEdge(
    topFadeHeight: Dp = 0.dp,
    bottomFadeHeight: Dp = 0.dp,
    topFadeStart: Dp = 0.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val h = size.height
        val w = size.width
        if (h <= 0f || w <= 0f) return@drawWithContent

        val startPx = topFadeStart.toPx()
        val fadePx = topFadeHeight.toPx()
        if (fadePx > 0f) {
            if (startPx > 0f) {
                // Clear any content above startPx (e.g. scrolled behind pinned header)
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset.Zero,
                    size = Size(w, startPx),
                    blendMode = BlendMode.DstIn
                )
            }
            // Fade content smoothly between startPx and startPx + fadePx
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black,
                    startY = startPx,
                    endY = startPx + fadePx
                ),
                topLeft = Offset(0f, startPx),
                size = Size(w, fadePx),
                blendMode = BlendMode.DstIn
            )
        }

        val bottomFadePx = bottomFadeHeight.toPx()
        if (bottomFadePx > 0f && bottomFadePx <= h) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black,
                    1f to Color.Transparent,
                    startY = h - bottomFadePx,
                    endY = h
                ),
                topLeft = Offset(0f, h - bottomFadePx),
                size = Size(w, bottomFadePx),
                blendMode = BlendMode.DstIn
            )
        }
    }

