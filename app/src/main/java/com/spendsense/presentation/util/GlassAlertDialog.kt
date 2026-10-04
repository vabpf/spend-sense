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
import androidx.compose.ui.unit.Dp
import android.graphics.drawable.ColorDrawable
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.Dialog
import com.spendsense.presentation.theme.GlassSurface
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider

private val DialogLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0F172A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF334155),
    background = Color.White,
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFEF4444),
    onError = Color.White
)

/**
 * A Material3-style dialog with solid elevated surface styling and clean shadows.
 * Replaces glass backdrop sampling to prevent multi-window wallpaper redraw artifacts.
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable RowScope.() -> Unit = {},
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = Color.White,
    borderAlpha: Float = FrostGlassDefaults.borderAlpha
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        SideEffect {
            var parent = view.parent
            while (parent != null && parent !is DialogWindowProvider) {
                parent = parent.parent
            }
            (parent as? DialogWindowProvider)?.window?.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        }

        val configuration = LocalConfiguration.current
        val maxContentHeight = (configuration.screenHeightDp.dp * 0.72f).coerceAtMost(620.dp)

        MaterialTheme(
            colorScheme = DialogLightColorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides Color(0xFF0F172A)
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth(0.92f)
                        .widthIn(max = 560.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = 0.18f),
                            spotColor = Color.Black.copy(alpha = 0.12f)
                        )
                        .background(
                            color = containerColor,
                            shape = shape
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE2E8F0),
                            shape = shape
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
