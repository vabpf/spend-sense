package com.spendsense.presentation.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.DarkSurface
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.NeonMint
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.theme.NeonViolet
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.theme.WarningAmber
import com.spendsense.presentation.util.glassEffect

internal fun paymentSourceTypeColor(type: String): Color = when {
    type.equals("Credit Card", ignoreCase = true) -> NeonRose
    type.equals("Debit Card", ignoreCase = true) -> CyberBlue
    type.equals("Bank Account", ignoreCase = true) -> NeonViolet
    type.equals("Wallet", ignoreCase = true) -> NeonMint
    type.equals("Manual", ignoreCase = true) -> WarningAmber
    else -> TextSecondary
}

// ─────────────────────────────────────────────────────────────────────────────
// Stacked bar chart composable with tap-to-inspect month breakdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun MonthlyPaymentSourceStackedBar(
    monthlyData: List<MonthlyPaymentSourceData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    val anim = remember { Animatable(0f) }
    LaunchedEffect(monthlyData) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    val progress = anim.value

    var selectedMonth by remember { mutableStateOf(-1) }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(monthlyData) {
                    detectTapGestures { offset ->
                        val count = monthlyData.size
                        if (count == 0) return@detectTapGestures
                        val w = size.width.toFloat()
                        val totalSpacing = w * 0.3f
                        val barWidth = (w - totalSpacing) / count
                        val gap = totalSpacing / (count + 1)

                        val hitMonth = monthlyData.indices.find { mi ->
                            val barStart = gap + mi * (barWidth + gap)
                            offset.x in (barStart - gap / 2f)..(barStart + barWidth + gap / 2f)
                        }
                        selectedMonth = if (selectedMonth == hitMonth) -1 else (hitMonth ?: -1)
                    }
                }
        ) {
            val count = monthlyData.size
            if (count == 0) return@Canvas
            val w = size.width
            val h = size.height
            val totalSpacing = w * 0.3f
            val barWidth = (w - totalSpacing) / count
            val gap = totalSpacing / (count + 1)

            // Grid lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = h * (1f - fraction)
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            val isAnyMonthSelected = selectedMonth != -1

            monthlyData.forEachIndexed { mi, month ->
                val x = gap + mi * (barWidth + gap)
                var accumulatedTop = h
                val isThisMonthSelected = selectedMonth == mi
                val barAlpha = when {
                    !isAnyMonthSelected -> 1f
                    isThisMonthSelected -> 1f
                    else -> 0.35f
                }

                month.slices.forEachIndexed { si, slice ->
                    val segH = (slice.fraction * h * progress).toFloat().coerceAtLeast(if (slice.amount > 0) 2f else 0f)
                    val top = accumulatedTop - segH
                    val isGrounding = si == 0
                    val color = paymentSourceTypeColor(slice.type)
                    val colorDim = color.copy(alpha = 0.3f)

                    if (isGrounding) {
                        val segBottom = top + segH
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = if (isThisMonthSelected) {
                                    listOf(color, color)
                                } else {
                                    listOf(color.copy(alpha = barAlpha), colorDim.copy(alpha = barAlpha))
                                },
                                startY = top,
                                endY = segBottom
                            ),
                            topLeft = Offset(x, top),
                            size = Size(barWidth, segH)
                        )
                    } else {
                        drawRect(
                            color = color.copy(alpha = (if (isThisMonthSelected) 1f else 0.75f) * barAlpha),
                            topLeft = Offset(x, top),
                            size = Size(barWidth, segH)
                        )
                    }

                    accumulatedTop = top
                }

                if (isThisMonthSelected) {
                    val totalHeight = (h - accumulatedTop).coerceAtLeast(4f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = Offset(x - 2f, (accumulatedTop - 2f).coerceAtLeast(0f)),
                        size = Size(barWidth + 4f, totalHeight + 4f),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Month labels — aligned to exact bar centers, clickable to select month
        val count = monthlyData.size
        val gapWeight = 0.3f / (count + 1)
        val barWeight = 0.7f / count
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.weight(gapWeight))
            monthlyData.forEachIndexed { mi, month ->
                val isSelected = mi == selectedMonth
                Text(
                    text = month.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        CyberBlue
                    } else {
                        TextSecondary
                    },
                    modifier = Modifier
                        .weight(barWeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            selectedMonth = if (selectedMonth == mi) -1 else mi
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                if (mi < count - 1) {
                    Spacer(Modifier.weight(gapWeight))
                }
            }
            Spacer(Modifier.weight(gapWeight))
        }

        // Tapped month breakdown
        AnimatedVisibility(
            visible = selectedMonth in monthlyData.indices,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (selectedMonth in monthlyData.indices) {
                val selectedData = monthlyData[selectedMonth]
                val allSources = selectedData.slices
                    .flatMap { it.sources }
                    .sortedByDescending { it.amount }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .glassEffect(
                            shape = RoundedCornerShape(12.dp),
                            containerColor = DarkSurface.copy(alpha = 0.6f),
                            borderAlpha = 0.2f
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedData.monthLabel} Breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatAmount(selectedData.total, currency),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonMint
                        )
                    }

                    if (allSources.isNotEmpty()) {
                        PaymentSourceDetailTable(
                            sources = allSources,
                            currency = currency
                        )
                    } else {
                        Text(
                            text = "No payment source details for ${selectedData.monthLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Detail table: payment source breakdown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun PaymentSourceDetailTable(
    sources: List<PaymentSourceBreakdown>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Source",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Type",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.width(100.dp),
                textAlign = TextAlign.Start
            )
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.End
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        sources.forEach { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = source.identifier,
                    style = MaterialTheme.typography.bodySmall,
                    color = paymentSourceTypeColor(source.type),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = source.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.width(100.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatAmount(source.amount, currency),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wallet Card wrapper (mirrors ChartCard from SpendingCharts)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WalletCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.24f
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Payment Sources card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PaymentSourcesCard(
    currentMonthSources: List<PaymentSourceBreakdown>,
    monthlyData: List<MonthlyPaymentSourceData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    WalletCard(title = "Payment Sources", modifier = modifier) {
        if (currentMonthSources.isEmpty() && monthlyData.all { it.slices.isEmpty() }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No payment source data",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            return@WalletCard
        }

        // Current month detail table
        if (currentMonthSources.isNotEmpty()) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            PaymentSourceDetailTable(
                sources = currentMonthSources,
                currency = currency
            )
        }

        // Stacked bar chart
        if (monthlyData.any { it.slices.isNotEmpty() }) {
            Spacer(Modifier.height(16.dp))
            MonthlyPaymentSourceStackedBar(
                monthlyData = monthlyData,
                currency = currency
            )
        }
    }
}
