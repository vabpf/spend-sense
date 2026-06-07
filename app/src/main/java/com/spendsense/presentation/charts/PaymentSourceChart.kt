package com.spendsense.presentation.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
// Tooltip data & hit regions (reuses TooltipHitRegion from SpendingCharts)
// ─────────────────────────────────────────────────────────────────────────────

private data class StackedSegmentData(
    val typeLabel: String,
    val amount: Double,
    val sources: List<PaymentSourceBreakdown>,
    val color: Color
)

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip overlay (self-contained, same style as ChartTooltipOverlay)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SegmentTooltipOverlay(
    content: TooltipContent,
    subLines: List<Pair<String, String>>,
    touchOffset: Offset,
    parentSize: IntSize,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.toPx() }
    val paddingPx = with(density) { 4.dp.toPx() }

    var measured by remember { mutableStateOf<IntSize?>(null) }

    val offsetInt = remember(content, subLines, touchOffset, parentSize, measured) {
        val tw = measured?.width?.toFloat() ?: with(density) { 160.dp.toPx() }
        val th = measured?.height?.toFloat() ?: with(density) { 56.dp.toPx() }

        val y = if (touchOffset.y - th - gapPx >= 0) {
            touchOffset.y - th - gapPx
        } else {
            val maxY = parentSize.height - th - paddingPx
            if (maxY >= paddingPx) {
                (touchOffset.y + gapPx).coerceAtMost(maxY)
            } else {
                touchOffset.y + gapPx
            }
        }

        val maxX = parentSize.width - tw - paddingPx
        val x = if (maxX >= paddingPx) {
            (touchOffset.x - tw / 2f).coerceIn(paddingPx, maxX)
        } else {
            (touchOffset.x - tw / 2f).coerceAtLeast(paddingPx)
        }

        IntOffset(x.roundToInt(), y.roundToInt())
    }

    Box(
        modifier = modifier
            .offset { offsetInt }
            .onSizeChanged { measured = it }
            .glassEffect(
                shape = RoundedCornerShape(8.dp),
                containerColor = DarkSurface.copy(alpha = 0.92f),
                borderAlpha = 0.3f
            )
            .widthIn(min = 80.dp, max = 220.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = content.amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subLines.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                subLines.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hit region for stacked bar segments
// ─────────────────────────────────────────────────────────────────────────────

private data class StackedBarHitRegion(
    val monthIndex: Int,
    val sliceIndex: Int,
    val data: StackedSegmentData,
    val bound: Rect
) {
    fun hitTest(touch: Offset) = bound.contains(touch)
}

// ─────────────────────────────────────────────────────────────────────────────
// Stacked bar chart composable
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
    var selectedSlice by remember { mutableStateOf(-1) }
    var tooltipOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Pre-compute segment layouts for hit-testing
    val hitRegions = remember(monthlyData, canvasSize) {
        if (canvasSize == IntSize.Zero) emptyList()
        else computeStackedHitRegions(monthlyData, canvasSize)
    }

    Column(modifier = modifier) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(hitRegions) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val hit = hitRegions.find { it.hitTest(offset) }
                                selectedMonth = hit?.monthIndex ?: -1
                                selectedSlice = hit?.sliceIndex ?: -1
                                tooltipOffset = offset
                            },
                            onDrag = { change, _ ->
                                val hit = hitRegions.find { it.hitTest(change.position) }
                                selectedMonth = hit?.monthIndex ?: -1
                                selectedSlice = hit?.sliceIndex ?: -1
                                tooltipOffset = change.position
                            },
                            onDragEnd = { selectedMonth = -1; selectedSlice = -1 },
                            onDragCancel = { selectedMonth = -1; selectedSlice = -1 }
                        )
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

                monthlyData.forEachIndexed { mi, month ->
                    val x = gap + mi * (barWidth + gap)
                    var accumulatedTop = h

                    month.slices.forEachIndexed { si, slice ->
                        val segH = (slice.fraction * h * progress).toFloat().coerceAtLeast(if (slice.amount > 0) 2f else 0f)
                        val top = accumulatedTop - segH
                        val isSelected = mi == selectedMonth && si == selectedSlice
                        val isGrounding = si == 0
                        val color = paymentSourceTypeColor(slice.type)
                        val colorDim = color.copy(alpha = 0.3f)

                        if (isGrounding) {
                            val segBottom = top + segH
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isSelected) listOf(color, color) else listOf(color, colorDim),
                                    startY = top,
                                    endY = segBottom
                                ),
                                topLeft = Offset(x, top),
                                size = Size(barWidth, segH)
                            )
                        } else {
                            drawRect(
                                color = color.copy(alpha = if (isSelected) 1f else 0.75f),
                                topLeft = Offset(x, top),
                                size = Size(barWidth, segH)
                            )
                        }

                        if (isSelected) {
                            drawRect(
                                color = color,
                                topLeft = Offset(x, top),
                                size = Size(barWidth, segH),
                                style = Stroke(width = 2f)
                            )
                        }

                        accumulatedTop = top
                    }
                }
            }

            // Tooltip
            val hit = hitRegions.find { it.monthIndex == selectedMonth && it.sliceIndex == selectedSlice }
            if (hit != null) {
                val total = monthlyData[selectedMonth].total
                val content = TooltipContent(
                    title = "${monthlyData[selectedMonth].monthLabel} — ${hit.data.typeLabel}",
                    subtitle = "${(hit.data.amount / total * 100).toInt()}%",
                    amount = formatAmount(hit.data.amount, currency)
                )
                val subLines = hit.data.sources.take(5).map { src ->
                    src.identifier to formatAmount(src.amount, currency)
                }

                SegmentTooltipOverlay(
                    content = content,
                    subLines = subLines,
                    touchOffset = tooltipOffset,
                    parentSize = canvasSize
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Month labels — aligned to exact bar centers
        val count = monthlyData.size
        val gapWeight = 0.3f / (count + 1)
        val barWeight = 0.7f / count
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.weight(gapWeight))
            monthlyData.forEachIndexed { mi, month ->
                Text(
                    text = month.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mi == selectedMonth) {
                        paymentSourceTypeColor(month.slices.firstOrNull()?.type ?: "")
                    } else {
                        TextSecondary
                    },
                    modifier = Modifier.weight(barWeight),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                if (mi < count - 1) {
                    Spacer(Modifier.weight(gapWeight))
                }
            }
            Spacer(Modifier.weight(gapWeight))
        }
    }
}

private fun computeStackedHitRegions(
    monthlyData: List<MonthlyPaymentSourceData>,
    size: IntSize
): List<StackedBarHitRegion> {
    if (size == IntSize.Zero || monthlyData.isEmpty()) return emptyList()
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    val count = monthlyData.size
    val totalSpacing = w * 0.3f
    val barWidth = (w - totalSpacing) / count
    val gap = totalSpacing / (count + 1)

    val regions = mutableListOf<StackedBarHitRegion>()

    monthlyData.forEachIndexed { mi, month ->
        val x = gap + mi * (barWidth + gap)
        var accumulatedTop = h

        if (month.slices.isEmpty()) {
            regions.add(
                StackedBarHitRegion(
                    monthIndex = mi,
                    sliceIndex = 0,
                    data = StackedSegmentData(
                        typeLabel = "No data",
                        amount = 0.0,
                        sources = emptyList(),
                        color = Color.White.copy(alpha = 0.15f)
                    ),
                    bound = Rect(x, 0f, x + barWidth, h)
                )
            )
        } else {
            month.slices.forEachIndexed { si, slice ->
                val segH = (slice.fraction * h).coerceAtLeast(if (slice.amount > 0) 2f else 0f)
                val top = accumulatedTop - segH

                regions.add(
                    StackedBarHitRegion(
                        monthIndex = mi,
                        sliceIndex = si,
                        data = StackedSegmentData(
                            typeLabel = slice.type,
                            amount = slice.amount,
                            sources = slice.sources,
                            color = paymentSourceTypeColor(slice.type)
                        ),
                        bound = Rect(x, top, x + barWidth, accumulatedTop)
                    )
                )
                accumulatedTop = top
            }
        }
    }

    return regions
}

// ─────────────────────────────────────────────────────────────────────────────
// Detail table: current month payment source breakdown
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
