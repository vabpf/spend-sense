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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.DarkSurface
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.parseColor
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip data model
// ─────────────────────────────────────────────────────────────────────────────

internal data class TooltipContent(
    val title: String,
    val subtitle: String?,
    val amount: String
)

// ─────────────────────────────────────────────────────────────────────────────
// Hit-region types (one per chart shape)
// ─────────────────────────────────────────────────────────────────────────────

internal sealed interface TooltipHitRegion {
    val index: Int
    val content: TooltipContent
    fun hitTest(touch: Offset): Boolean

    data class Rectangular(
        override val index: Int,
        override val content: TooltipContent,
        val bound: Rect
    ) : TooltipHitRegion {
        override fun hitTest(touch: Offset) = bound.contains(touch)
    }

    data class Arc(
        override val index: Int,
        override val content: TooltipContent,
        val center: Offset,
        val innerRadius: Float,
        val outerRadius: Float,
        val startAngle: Float,
        val sweepAngle: Float
    ) : TooltipHitRegion {
        override fun hitTest(touch: Offset): Boolean {
            val dx = touch.x - center.x
            val dy = touch.y - center.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < innerRadius || dist > outerRadius) return false
            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val touchAngle = (angleDeg + 360f) % 360f
            val segStart = (startAngle + 360f) % 360f
            val segEnd = segStart + sweepAngle
            return if (segEnd > 360f) {
                touchAngle in segStart..360f || touchAngle in 0f..(segEnd - 360f)
            } else {
                touchAngle in segStart..segEnd
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tooltip overlay composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChartTooltipOverlay(
    content: TooltipContent,
    touchOffset: Offset,
    parentSize: IntSize,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.toPx() }
    val paddingPx = with(density) { 4.dp.toPx() }

    // Measure the tooltip once
    var measured by remember { mutableStateOf<IntSize?>(null) }

    val offsetInt = remember(content, touchOffset, parentSize, measured) {
        val tw = measured?.width?.toFloat() ?: with(density) { 160.dp.toPx() }
        val th = measured?.height?.toFloat() ?: with(density) { 56.dp.toPx() }

        // Prefer above the finger, flip below if not enough room
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

        // Center on touch horizontally, clamp to parent
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
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .widthIn(min = 80.dp, max = 200.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = content.amount,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (content.subtitle != null) {
                    Text(
                        text = content.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hit-region computation helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun computeBarHitRegions(
    bars: List<DailyBar>,
    maxAmount: Double,
    currency: String,
    size: IntSize
): List<TooltipHitRegion> {
    if (size == IntSize.Zero || bars.isEmpty()) return emptyList()
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    val count = bars.size
    val totalSpacing = w * 0.3f
    val barWidth = (w - totalSpacing) / count
    val gap = totalSpacing / (count + 1)

    return bars.mapIndexed { i, bar ->
        val left = gap + i * (barWidth + gap)
        val right = left + barWidth
        val content = TooltipContent(
            title = bar.dayLabel,
            subtitle = "${bar.transactionCount} transaction${if (bar.transactionCount != 1) "s" else ""}",
            amount = formatAmount(bar.amount, currency)
        )
        TooltipHitRegion.Rectangular(i, content, Rect(left, 0f, right, h))
    }
}

private fun computeLineHitRegions(
    points: List<MonthlyPoint>,
    maxAmount: Double,
    currency: String,
    size: IntSize
): List<TooltipHitRegion> {
    val n = points.size
    if (size == IntSize.Zero || n < 1) return emptyList()
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    val stepX = w / (n - 1).coerceAtLeast(1)

    return points.mapIndexed { i, point ->
        val left = if (i == 0) 0f else (i - 0.5f) * stepX
        val right = if (i == n - 1) w else (i + 0.5f) * stepX
        val content = TooltipContent(
            title = point.monthLabel,
            subtitle = null,
            amount = formatAmount(point.amount, currency)
        )
        TooltipHitRegion.Rectangular(i, content, Rect(left, 0f, right, h))
    }
}

private fun computeDonutHitRegions(
    slices: List<CategorySlice>,
    currency: String,
    size: IntSize
): List<TooltipHitRegion> {
    if (size == IntSize.Zero || slices.isEmpty()) return emptyList()
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    val minDim = minOf(w, h)
    val stroke = minDim * 0.18f
    val pathRadius = (minDim - stroke) / 2f
    val innerR = pathRadius - stroke / 2f
    val outerR = pathRadius + stroke / 2f
    val center = Offset(w / 2f, h / 2f)

    val regions = mutableListOf<TooltipHitRegion>()
    var startAngle = -90f
    val gap = 2f

    slices.forEachIndexed { i, slice ->
        val sweepAngle = slice.fraction * 360f - gap
        val endAngle = startAngle + sweepAngle
        val midAngle = startAngle + sweepAngle / 2f
        val content = TooltipContent(
            title = slice.category.name,
            subtitle = "${(slice.fraction * 100).toInt()}%",
            amount = formatAmount(slice.amount, currency)
        )
        regions.add(
            TooltipHitRegion.Arc(
                index = i,
                content = content,
                center = center,
                innerRadius = innerR,
                outerRadius = outerR,
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
        )
        startAngle = endAngle + gap
    }

    return regions
}

// ─────────────────────────────────────────────────────────────────────────────
// DONUT CHART — spending by category
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CategoryDonutChart(
    slices: List<CategorySlice>,
    currency: String,
    modifier: Modifier = Modifier
) {
    ChartCard(title = "Spending by Category", modifier = modifier) {
        if (slices.isEmpty()) {
            EmptyChart("No transactions this month")
            return@ChartCard
        }

        val sweep = remember { Animatable(0f) }
        LaunchedEffect(slices) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
        val progress = sweep.value

        var selectedIndex by remember { mutableStateOf(-1) }
        var tooltipOffset by remember { mutableStateOf(Offset.Zero) }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }
        var rowSize by remember { mutableStateOf(IntSize.Zero) }

        val hitRegions = remember(slices, currency, canvasSize) {
            computeDonutHitRegions(slices, currency, canvasSize)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(120.dp)) {
                Canvas(
                    modifier = Modifier
                        .size(120.dp)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(hitRegions) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val hit = hitRegions.find { it.hitTest(offset) }
                                    selectedIndex = hit?.index ?: -1
                                    tooltipOffset = offset
                                },
                                onDrag = { change, _ ->
                                    val hit = hitRegions.find { it.hitTest(change.position) }
                                    selectedIndex = hit?.index ?: -1
                                    tooltipOffset = change.position
                                },
                                onDragEnd = { selectedIndex = -1 },
                                onDragCancel = { selectedIndex = -1 }
                            )
                        }
                ) {
                    val stroke = size.minDimension * 0.18f
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val arcOffset = Offset(inset / 2f, inset / 2f)
                    var startAngle = -90f
                    val gap = 2f

                    slices.forEachIndexed { i, slice ->
                        val sweepAngle = (slice.fraction * 360f - gap) * progress
                        val isSelected = i == selectedIndex

                        val segColor = parseColor(slice.category.colorHex)
                        val segAlpha = if (isSelected) 1f else 0.75f
                        val segStrokeWidth = if (isSelected) stroke * 1.25f else stroke

                        drawArc(
                            color = segColor.copy(alpha = segAlpha),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle.coerceAtLeast(0f),
                            useCenter = false,
                            topLeft = Offset(
                                arcOffset.x - (segStrokeWidth - stroke) / 2f,
                                arcOffset.y - (segStrokeWidth - stroke) / 2f
                            ),
                            size = arcSize,
                            style = Stroke(
                                width = segStrokeWidth,
                                cap = StrokeCap.Butt
                            )
                        )

                        startAngle += slice.fraction * 360f
                    }

                    if (progress < 1f) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.05f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                    }
                }

                val selectedHit = hitRegions.find { it.index == selectedIndex }
                if (selectedHit != null) {
                    ChartTooltipOverlay(
                        content = selectedHit.content,
                        touchOffset = tooltipOffset,
                        parentSize = rowSize
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.take(5).forEachIndexed { i, slice ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = parseColor(slice.category.colorHex))
                        }
                        Text(
                            text = slice.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${(slice.fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (i == selectedIndex) {
                                parseColor(slice.category.colorHex)
                            } else {
                                parseColor(slice.category.colorHex).copy(alpha = 0.6f)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAILY BAR CHART — last 7 days
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DailySpendingBarChart(
    bars: List<DailyBar>,
    currency: String,
    modifier: Modifier = Modifier
) {
    ChartCard(title = "Last 7 Days", modifier = modifier) {
        if (bars.isEmpty() || bars.all { it.amount == 0.0 }) {
            EmptyChart("No spending in the last 7 days")
            return@ChartCard
        }

        val anim = remember { Animatable(0f) }
        LaunchedEffect(bars) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        val progress = anim.value

        val maxAmount = bars.maxOf { it.amount }.takeIf { it > 0 } ?: 1.0
        val barColor = CyberBlue
        val barColorDim = CyberBlue.copy(alpha = 0.3f)

        var selectedIndex by remember { mutableStateOf(-1) }
        var tooltipOffset by remember { mutableStateOf(Offset.Zero) }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        val hitRegions = remember(bars, maxAmount, currency, canvasSize) {
            computeBarHitRegions(bars, maxAmount, currency, canvasSize)
        }

        Column {
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
                                    selectedIndex = hit?.index ?: -1
                                    tooltipOffset = offset
                                },
                                onDrag = { change, _ ->
                                    val hit = hitRegions.find { it.hitTest(change.position) }
                                    selectedIndex = hit?.index ?: -1
                                    tooltipOffset = change.position
                                },
                                onDragEnd = { selectedIndex = -1 },
                                onDragCancel = { selectedIndex = -1 }
                            )
                        }
                ) {
                    val barCount = bars.size
                    val totalSpacing = size.width * 0.3f
                    val barWidth = (size.width - totalSpacing) / barCount
                    val gap = totalSpacing / (barCount + 1)

                    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                        val y = size.height * (1f - fraction)
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    bars.forEachIndexed { i, bar ->
                        val x = gap + i * (barWidth + gap)
                        val heightFraction = ((bar.amount / maxAmount) * progress).toFloat()
                        val barHeight = (size.height * heightFraction).coerceAtLeast(if (bar.amount > 0) 4f else 0f)
                        val top = size.height - barHeight

                        val isSelected = i == selectedIndex
                        val colors = if (isSelected) {
                            listOf(barColor, barColor)
                        } else {
                            listOf(barColor, barColorDim)
                        }

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = colors,
                                startY = top,
                                endY = size.height
                            ),
                            topLeft = Offset(x, top),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }

                val selectedHit = hitRegions.find { it.index == selectedIndex }
                if (selectedHit != null) {
                    ChartTooltipOverlay(
                        content = selectedHit.content,
                        touchOffset = tooltipOffset,
                        parentSize = canvasSize
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val barCount = bars.size
            val barGapWeight = 0.3f / (barCount + 1)
            val barLabelWeight = 0.7f / barCount
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.weight(barGapWeight))
                bars.forEachIndexed { i, bar ->
                    Text(
                        text = bar.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == selectedIndex) CyberBlue else TextSecondary,
                        modifier = Modifier.weight(barLabelWeight),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    if (i < barCount - 1) {
                        Spacer(Modifier.weight(barGapWeight))
                    }
                }
                Spacer(Modifier.weight(barGapWeight))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MONTHLY TREND LINE CHART — last 6 months
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonthlyTrendLineChart(
    points: List<MonthlyPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    ChartCard(title = "6-Month Trend", modifier = modifier) {
        if (points.isEmpty() || points.all { it.amount == 0.0 }) {
            EmptyChart("No monthly data yet")
            return@ChartCard
        }

        val anim = remember { Animatable(0f) }
        LaunchedEffect(points) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
        val progress = anim.value

        val textMeasurer = rememberTextMeasurer()

        val maxAmount = points.maxOf { it.amount }.takeIf { it > 0 } ?: 1.0
        val lineColor = CyberBlue

        var selectedIndex by remember { mutableStateOf(-1) }
        var tooltipOffset by remember { mutableStateOf(Offset.Zero) }
        var canvasSize by remember { mutableStateOf(IntSize.Zero) }

        val hitRegions = remember(points, maxAmount, currency, canvasSize) {
            computeLineHitRegions(points, maxAmount, currency, canvasSize)
        }

        Column {
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
                                    selectedIndex = hit?.index ?: -1
                                    tooltipOffset = offset
                                },
                                onDrag = { change, _ ->
                                    val hit = hitRegions.find { it.hitTest(change.position) }
                                    selectedIndex = hit?.index ?: -1
                                    tooltipOffset = change.position
                                },
                                onDragEnd = { selectedIndex = -1 },
                                onDragCancel = { selectedIndex = -1 }
                            )
                        }
                ) {
                    val n = points.size
                    if (n < 2) return@Canvas

                    val stepX = size.width / (n - 1).toFloat()

                    fun xAt(i: Int) = i * stepX
                    fun yAt(i: Int): Float {
                        val raw = (points[i].amount / maxAmount).toFloat()
                        val clamped = (raw * progress).coerceIn(0f, 1f)
                        val topPadding = 24f
                        val bottomPadding = 8f
                        val availableHeight = size.height - topPadding - bottomPadding
                        return availableHeight * (1f - clamped) + topPadding
                    }

                    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                        val y = size.height * (1f - fraction)
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                    }

                    val fillPath = Path().apply {
                        moveTo(xAt(0), size.height)
                        lineTo(xAt(0), yAt(0))
                        for (i in 1 until n) {
                            val cx = (xAt(i - 1) + xAt(i)) / 2f
                            cubicTo(cx, yAt(i - 1), cx, yAt(i), xAt(i), yAt(i))
                        }
                        lineTo(xAt(n - 1), size.height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height
                        )
                    )

                    val linePath = Path().apply {
                        moveTo(xAt(0), yAt(0))
                        for (i in 1 until n) {
                            val cx = (xAt(i - 1) + xAt(i)) / 2f
                            cubicTo(cx, yAt(i - 1), cx, yAt(i), xAt(i), yAt(i))
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    for (i in 0 until n) {
                        val isSelected = i == selectedIndex
                        val dotRadius = if (isSelected) 8f else 4f

                        if (isSelected) {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.2f),
                                radius = 14f,
                                center = Offset(xAt(i), yAt(i))
                            )
                        }
                        drawCircle(
                            color = lineColor,
                            radius = dotRadius,
                            center = Offset(xAt(i), yAt(i))
                        )
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.6f),
                            radius = if (isSelected) 4f else 2f,
                            center = Offset(xAt(i), yAt(i))
                        )

                        // Draw compact amount text annotation above the line point
                        val text = formatCompactAmount(points[i].amount)
                        if (text.isNotEmpty() && progress >= 0.9f) {
                            val textLayoutResult = textMeasurer.measure(
                                text = text,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            val textWidth = textLayoutResult.size.width
                            val textHeight = textLayoutResult.size.height
                            
                            val xOffset = when (i) {
                                0 -> 4f
                                n - 1 -> size.width - textWidth - 4f
                                else -> xAt(i) - textWidth / 2f
                            }
                            
                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(
                                    x = xOffset,
                                    y = yAt(i) - textHeight - 8f
                                )
                            )
                        }
                    }
                }

                val selectedHit = hitRegions.find { it.index == selectedIndex }
                if (selectedHit != null) {
                    ChartTooltipOverlay(
                        content = selectedHit.content,
                        touchOffset = tooltipOffset,
                        parentSize = canvasSize
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { i, point ->
                    Text(
                        text = point.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == selectedIndex) CyberBlue else TextSecondary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared card wrapper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChartCard(
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

@Composable
private fun EmptyChart(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

internal fun formatAmount(amount: Double, currencyCode: String): String {
    return try {
        val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
        fmt.currency = Currency.getInstance(currencyCode)
        fmt.maximumFractionDigits = 0
        fmt.format(amount)
    } catch (_: Exception) {
        "$currencyCode ${String.format("%.0f", amount)}"
    }
}
