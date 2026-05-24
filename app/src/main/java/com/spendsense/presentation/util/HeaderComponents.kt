package com.spendsense.presentation.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.TextPrimary

@Composable
fun SpendSenseTopBar(
    title: String,
    onNavigationClick: (() -> Unit)? = null,
    navigationIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val barShape = RoundedCornerShape(28.dp)
    val brandShape = RoundedCornerShape(16.dp)
    val hazeState = LocalGlassHazeState.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                end = 16.dp
            )
            .shadow(
                elevation = 16.dp,
                shape = barShape,
                ambientColor = Color.Black.copy(alpha = 0.24f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            )
            .glassEffect(
                shape = barShape,
                containerColor = GlassSurface.copy(alpha = 0.9f),
                borderAlpha = 0.18f,
                sheenAlpha = 0.05f,
                hazeState = hazeState
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onNavigationClick != null && navigationIcon != null) {
                IconButton(
                    modifier = Modifier.size(44.dp),
                    onClick = onNavigationClick
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .glassEffect(
                            shape = brandShape,
                            containerColor = CyberBlue.copy(alpha = 0.14f),
                            borderAlpha = 0.22f,
                            sheenAlpha = 0.06f,
                            hazeState = hazeState
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyberBlue.copy(alpha = 0.95f)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(TextPrimary, TextPrimary.copy(alpha = 0.85f))
                    )
                ),
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}
