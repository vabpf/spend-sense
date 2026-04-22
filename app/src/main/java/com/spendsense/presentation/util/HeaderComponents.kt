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
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.HoloCyan
import com.spendsense.presentation.theme.HoloRose

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
            .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 0.dp)
            .glassEffect(
                shape = MaterialTheme.shapes.medium,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.3f
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                // Cyberpunk Logo-ish icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .glassEffect(
                            shape = MaterialTheme.shapes.small,
                            containerColor = HoloCyan.copy(alpha = 0.15f),
                            borderAlpha = 0.4f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "S",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = HoloCyan
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.linearGradient(
                        colors = listOf(HoloCyan, HoloRose)
                    )
                ),
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        
        // A subtle holographic line at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.6f)
                .height(2.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HoloCyan.copy(alpha = 0.6f),
                            HoloRose.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
