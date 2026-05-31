package com.spendsense.presentation.util

import androidx.compose.animation.core.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent

fun Modifier.shimmer(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = -2.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    drawWithContent {
        drawContent()
        val width = size.width
        val xOffset = width * translateAnim.value
        
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(x = xOffset, y = 0f),
            end = Offset(x = xOffset + width, y = size.height)
        )
        drawRect(brush = brush)
    }
}
