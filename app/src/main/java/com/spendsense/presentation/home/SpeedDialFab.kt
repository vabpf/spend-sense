package com.spendsense.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.util.softDropShadow

@Composable
fun SpeedDialFab(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onManualClick: () -> Unit,
    onScanReceiptClick: () -> Unit,
    onQuickTextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_rotation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Floating options stack
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(spring(stiffness = Spring.StiffnessHigh)) + scaleIn(spring(stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) + scaleOut(spring(stiffness = Spring.StiffnessHigh))
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpeedDialItem(
                    label = "Quick Free Text",
                    icon = Icons.Rounded.AutoAwesome,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF7C3AED),
                    onClick = onQuickTextClick
                )

                SpeedDialItem(
                    label = "Scan Receipt",
                    icon = Icons.Rounded.CameraAlt,
                    iconBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    onClick = onScanReceiptClick
                )

                SpeedDialItem(
                    label = "Manual Entry",
                    icon = Icons.Rounded.Edit,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    onClick = onManualClick
                )
            }
        }

        // Primary FAB
        Box(
            modifier = Modifier
                .size(54.dp)
                .softDropShadow(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.12f),
                    blur = 14.dp,
                    offsetY = 4.dp
                )
                .softDropShadow(
                    shape = CircleShape,
                    color = CyberBlue.copy(alpha = 0.30f),
                    blur = 12.dp,
                    offsetY = 2.dp
                )
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .background(
                    Brush.linearGradient(listOf(CyberBlue, Color(0xFF00C6FF))),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onExpandedChange(!isExpanded)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (isExpanded) "Close" else "Add Transaction Options",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 5.dp,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(5.dp, CircleShape)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
