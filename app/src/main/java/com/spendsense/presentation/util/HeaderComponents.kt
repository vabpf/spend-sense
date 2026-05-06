package com.spendsense.presentation.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.DeepCharcoal
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.NeonViolet
import com.spendsense.presentation.theme.TextPrimary

@Composable
fun SpendSenseTopBar(
    title: String,
    onNavigationClick: (() -> Unit)? = null,
    navigationIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassSurface.copy(alpha = 0.88f),
                        GlassSurface.copy(alpha = 0.72f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onNavigationClick != null && navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyberBlue.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SS",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyberBlue.copy(alpha = 0.95f)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    brush = Brush.linearGradient(
                        colors = listOf(TextPrimary, TextPrimary.copy(alpha = 0.85f))
                    )
                ),
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.5f)
                .height(1.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CyberBlue.copy(alpha = 0.5f),
                            NeonViolet.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
