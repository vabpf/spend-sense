package com.spendsense.presentation.util

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.GlassSurface

/**
 * Applies a glassmorphism effect with optional blur (Android 12+), 
 * semi-transparent background, and a subtle gradient border.
 */
fun Modifier.glassEffect(
    shape: Shape,
    containerColor: Color = GlassSurface.copy(alpha = 0.65f),
    blurRadius: Dp = 12.dp,
    borderWidth: Dp = 1.dp,
    borderAlpha: Float = 0.2f,
    contentModifier: Modifier = Modifier
): Modifier = this
    .clip(shape)
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.blur(blurRadius)
        } else {
            Modifier
        }
    )
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                containerColor,
                containerColor.copy(alpha = containerColor.alpha * 0.8f)
            )
        )
    )
    .then(contentModifier)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = borderAlpha * 0.5f),
                Color.Transparent,
                Color.White.copy(alpha = borderAlpha * 0.3f)
            )
        ),
        shape = shape
    )
