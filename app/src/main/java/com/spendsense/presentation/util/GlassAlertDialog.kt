package com.spendsense.presentation.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.spendsense.presentation.theme.GlassSurface
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp

/**
 * A Material3-style dialog with the liquid glass effect applied to its surface.
 *
 * Replaces [androidx.compose.material3.AlertDialog] with glass styling —
 * uses real Liquid Glass sampling on API 33+ and falls back to Haze on older devices.
 *
 * @see [Liquid Glass Guide](docs/LIQUID_GLASS.md)
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable RowScope.() -> Unit = {},
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = GlassSurface.copy(alpha = 0.9f),
    borderAlpha: Float = 0f
) {
    Dialog(onDismissRequest = onDismissRequest) {
        val configuration = LocalConfiguration.current
        val maxContentHeight = configuration.screenHeightDp.dp * 0.55f

        Box(
            modifier = modifier
                .glassEffect(
                    shape = shape,
                    containerColor = containerColor,
                    borderAlpha = borderAlpha
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (title != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        title()
                    }
                }
                if (text != null) {
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxContentHeight)
                            .fadingScrollEdges(scrollState, 20.dp)
                            .verticalScroll(scrollState)
                    ) {
                        text()
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

private fun Modifier.fadingScrollEdges(
    scrollState: androidx.compose.foundation.ScrollState,
    fadeHeight: Dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        
        val fadePx = fadeHeight.toPx()
        if (fadePx > 0f) {
            // Top fade
            if (scrollState.value > 0) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            1f to Color.Black
                        ),
                        startY = 0f,
                        endY = fadePx
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            // Bottom fade
            if (scrollState.value < scrollState.maxValue) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black,
                            1f to Color.Transparent
                        ),
                        startY = size.height - fadePx,
                        endY = size.height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
    }
