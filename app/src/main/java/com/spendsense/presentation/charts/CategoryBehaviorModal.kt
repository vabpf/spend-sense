package com.spendsense.presentation.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendsense.domain.calculation.CategoryBehaviorProfile
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBehaviorModal(
    profile: CategoryBehaviorProfile,
    currency: String,
    periodLabel: String? = null,
    onDismiss: () -> Unit
) {
    val category = profile.category
    val catColor = parseColor(category.colorHex)
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        windowInsets = WindowInsets.statusBars
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header: Category Icon, Name, Period & Spend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sub = if (!periodLabel.isNullOrBlank()) {
                                "${profile.transactionCount} transactions • $periodLabel"
                            } else {
                                "${profile.transactionCount} transactions"
                            }
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = formatAmount(profile.totalAmount, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Section 1: Time-of-Day Context
            SectionCard(
                    title = "Time-of-Day Context",
                    icon = Icons.Rounded.Schedule,
                    accentColor = Color(0xFF0284C7)
                ) {
                    if (profile.timeBuckets.isNotEmpty()) {
                        // Multi-color distribution bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            val bucketColors = listOf(
                                Color(0xFF6366F1), // Night - indigo
                                Color(0xFF0EA5E9), // Midday - sky
                                Color(0xFFF59E0B), // Morning - amber
                                Color(0xFF94A3B8)  // Other - slate
                            )
                            profile.timeBuckets.forEachIndexed { idx, bucket ->
                                val color = bucketColors.getOrElse(idx) { Color(0xFF94A3B8) }
                                Box(
                                    modifier = Modifier
                                        .weight(bucket.fraction.coerceAtLeast(0.01f))
                                        .fillMaxHeight()
                                        .background(color)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val bucketColors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF0EA5E9),
                                Color(0xFFF59E0B),
                                Color(0xFF94A3B8)
                            )
                            profile.timeBuckets.forEachIndexed { idx, bucket ->
                                val dotColor = bucketColors.getOrElse(idx) { Color(0xFF94A3B8) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Canvas(modifier = Modifier.size(8.dp)) {
                                            drawCircle(color = dotColor)
                                        }
                                        Text(
                                            text = bucket.bucketName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF334155)
                                        )
                                        Text(
                                            text = "(${bucket.count})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = formatAmount(bucket.totalAmount, currency),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${(bucket.fraction * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = dotColor
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No time patterns recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

            // Section 2: Spending Sizes
            SectionCard(
                    title = "Spending Sizes",
                    icon = Icons.Rounded.Receipt,
                    accentColor = Color(0xFF10B981)
                ) {
                    if (profile.ticketTiers.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            profile.ticketTiers.forEach { tier ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = tier.tierName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = tier.thresholdLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Text(
                                            text = "${tier.count} txns • Avg ${formatAmount(tier.averageTicket, currency)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatAmount(tier.totalAmount, currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${(tier.fraction * 100).toInt()}% of total",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No tier distribution available",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

            // Section 3: Top Frequent Venues & Spots
            SectionCard(
                    title = "Top Frequent Venues & Merchants",
                    icon = Icons.Rounded.Store,
                    accentColor = Color(0xFF8B5CF6)
                ) {
                    if (profile.topMerchants.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            profile.topMerchants.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = m.merchantName,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${m.visitCount} visits • Avg ${formatAmount(m.averageTicket, currency)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = formatAmount(m.totalAmount, currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No merchants recorded",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }
        content()
    }
}
