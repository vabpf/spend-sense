package com.spendsense.presentation.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.parseColor
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Donut
            Canvas(modifier = Modifier.size(120.dp)) {
                val stroke = size.minDimension * 0.18f
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val arcOffset = Offset(inset / 2f, inset / 2f)
                var startAngle = -90f
                val gap = 2f

                slices.forEach { slice ->
                    val sweepAngle = (slice.fraction * 360f - gap) * progress
                    drawArc(
                        color = parseColor(slice.category.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle.coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = arcOffset,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    startAngle += slice.fraction * 360f
                }

                // Thin track ring underneath
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

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                slices.take(5).forEach { slice ->
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
                            color = parseColor(slice.category.colorHex)
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

        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val barCount = bars.size
                val totalSpacing = size.width * 0.3f
                val barWidth = (size.width - totalSpacing) / barCount
                val gap = totalSpacing / (barCount + 1)

                // Horizontal grid lines
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

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, barColorDim),
                            startY = top,
                            endY = size.height
                        ),
                        topLeft = Offset(x, top),
                        size = Size(barWidth, barHeight)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                bars.forEach { bar ->
                    Text(
                        text = bar.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
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

        val maxAmount = points.maxOf { it.amount }.takeIf { it > 0 } ?: 1.0
        val lineColor = CyberBlue

        Column {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val n = points.size
                if (n < 2) return@Canvas

                val stepX = size.width / (n - 1).toFloat()

                fun xAt(i: Int) = i * stepX
                fun yAt(i: Int): Float {
                    val raw = (points[i].amount / maxAmount).toFloat()
                    val clamped = (raw * progress).coerceIn(0f, 1f)
                    return size.height * (1f - clamped) + 4f
                }

                // Horizontal grid
                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                    val y = size.height * (1f - fraction)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                // Fill path under the line
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

                // Line path
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

                // Dots at each data point
                for (i in 0 until n) {
                    drawCircle(
                        color = lineColor,
                        radius = 4f,
                        center = Offset(xAt(i), yAt(i))
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.6f),
                        radius = 2f,
                        center = Offset(xAt(i), yAt(i))
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Month labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { point ->
                    Text(
                        text = point.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
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
