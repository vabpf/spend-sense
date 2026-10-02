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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
// Stacked bar chart composable with synchronized month selection and Option 1 highlight
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun MonthlyPaymentSourceStackedBar(
    monthlyData: List<MonthlyPaymentSourceData>,
    currency: String,
    selectedYear: Int,
    selectedMonth: Int,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    val anim = remember { Animatable(0f) }
    LaunchedEffect(monthlyData) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    val progress = anim.value

    val selectedIndex = monthlyData.indexOfFirst { it.year == selectedYear && it.month == selectedMonth }
    val isAnyMonthSelected = selectedIndex != -1

    val density = LocalDensity.current
    val colCornerPx = with(density) { 8.dp.toPx() }
    val barStrokePx = with(density) { 2.dp.toPx() }
    val colBorderPx = with(density) { 1.dp.toPx() }

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

                        val hit = monthlyData.indices.find { mi ->
                            val barStart = gap + mi * (barWidth + gap)
                            offset.x in (barStart - gap / 2f)..(barStart + barWidth + gap / 2f)
                        }
                        if (hit != null) {
                            val target = monthlyData[hit]
                            onMonthSelected(target.year, target.month)
                        }
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

            // Option 1: Glass Column Backdrop behind selected month
            if (selectedIndex != -1) {
                val selX = gap + selectedIndex * (barWidth + gap)
                drawRoundRect(
                    color = CyberBlue.copy(alpha = 0.08f),
                    topLeft = Offset(selX - gap * 0.35f, 0f),
                    size = Size(barWidth + gap * 0.7f, h),
                    cornerRadius = CornerRadius(colCornerPx, colCornerPx)
                )
                drawRoundRect(
                    color = CyberBlue.copy(alpha = 0.22f),
                    topLeft = Offset(selX - gap * 0.35f, 0f),
                    size = Size(barWidth + gap * 0.7f, h),
                    cornerRadius = CornerRadius(colCornerPx, colCornerPx),
                    style = Stroke(width = colBorderPx)
                )
            }

            monthlyData.forEachIndexed { mi, month ->
                val x = gap + mi * (barWidth + gap)
                var accumulatedTop = h
                val isThisMonthSelected = mi == selectedIndex
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
                        color = CyberBlue.copy(alpha = 0.9f),
                        topLeft = Offset(x - 2f, (accumulatedTop - 2f).coerceAtLeast(0f)),
                        size = Size(barWidth + 4f, totalHeight + 4f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = barStrokePx)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Month labels — aligned to exact bar centers, Option 1 pill highlight
        val count = monthlyData.size
        val gapWeight = 0.3f / (count + 1)
        val barWeight = 0.7f / count
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(gapWeight))
            monthlyData.forEachIndexed { mi, month ->
                val isSelected = mi == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(barWeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onMonthSelected(month.year, month.month)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberBlue.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = month.monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlue,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(CyberBlue)
                            )
                        }
                    } else {
                        Text(
                            text = month.monthLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
                if (mi < count - 1) {
                    Spacer(Modifier.weight(gapWeight))
                }
            }
            Spacer(Modifier.weight(gapWeight))
        }

        // Selected month breakdown
        AnimatedVisibility(
            visible = selectedIndex != -1,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (selectedIndex != -1) {
                val selectedData = monthlyData[selectedIndex]
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
    selectedYear: Int = 0,
    selectedMonth: Int = 0,
    onMonthSelected: (year: Int, month: Int) -> Unit = { _, _ -> },
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

        // Stacked bar chart with interactive month selection
        if (monthlyData.any { it.slices.isNotEmpty() }) {
            MonthlyPaymentSourceStackedBar(
                monthlyData = monthlyData,
                currency = currency,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onMonthSelected = onMonthSelected
            )
        } else if (currentMonthSources.isNotEmpty()) {
            PaymentSourceDetailTable(
                sources = currentMonthSources,
                currency = currency
            )
        }
    }
}
