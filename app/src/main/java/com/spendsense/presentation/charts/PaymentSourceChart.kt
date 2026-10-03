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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.ui.unit.dp
import java.util.Currency

import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.CyberBlueLight
import com.spendsense.presentation.theme.DarkSurface
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.NeonMint
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.theme.NeonViolet
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.theme.WarningAmber
import com.spendsense.presentation.util.glassEffect

enum class SpendingChartMode {
    BAR, LINE
}

internal fun formatAxisAmount(amount: Double, currencyCode: String): String {
    val clean = currencyCode.trim().uppercase()
    val isVnd = clean == "VND"
    val symbol = if (isVnd) "₫" else try {
        Currency.getInstance(clean).symbol
    } catch (_: Exception) {
        clean
    }

    val (numStr, suffix) = when {
        amount >= 1_000_000_000.0 -> {
            val v = amount / 1_000_000_000.0
            (if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)) to "B"
        }
        amount >= 1_000_000.0 -> {
            val v = amount / 1_000_000.0
            (if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)) to "M"
        }
        amount >= 1_000.0 -> {
            val v = amount / 1_000.0
            (if (v % 1.0 == 0.0) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)) to "K"
        }
        else -> String.format(Locale.US, "%.0f", amount) to ""
    }

    return if (isVnd) {
        "$numStr$suffix₫"
    } else {
        "$symbol$numStr$suffix"
    }
}

internal fun paymentSourceTypeColor(type: String): Color = when {
    type.equals("Credit Card", ignoreCase = true) -> NeonRose
    type.equals("Debit Card", ignoreCase = true) -> CyberBlue
    type.equals("Bank Account", ignoreCase = true) -> NeonViolet
    type.equals("Wallet", ignoreCase = true) -> NeonMint
    type.equals("Manual", ignoreCase = true) -> WarningAmber
    else -> TextSecondary
}

// ─────────────────────────────────────────────────────────────────────────────
// Monthly Spending Chart (Stacked Bar / Line Area) with cyber glow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun MonthlyPaymentSourceStackedBar(
    monthlyData: List<MonthlyPaymentSourceData>,
    currency: String,
    selectedYear: Int,
    selectedMonth: Int,
    chartMode: SpendingChartMode = SpendingChartMode.BAR,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (monthlyData.isEmpty()) return

    val anim = remember { Animatable(0f) }
    LaunchedEffect(monthlyData, chartMode) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }
    val progress = anim.value

    val selectedIndex = monthlyData.indexOfFirst { it.year == selectedYear && it.month == selectedMonth }
    val isAnyMonthSelected = selectedIndex != -1

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val colCornerPx = with(density) { 8.dp.toPx() }
    val barStrokePx = with(density) { 2.dp.toPx() }
    val colBorderPx = with(density) { 1.dp.toPx() }

    val maxAmount = monthlyData.maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .pointerInput(monthlyData) {
                    detectTapGestures { offset ->
                        val count = monthlyData.size
                        if (count == 0) return@detectTapGestures
                        val w = size.width.toFloat()
                        val totalSpacing = w * 0.28f
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
            val totalSpacing = w * 0.28f
            val barWidth = (w - totalSpacing) / count
            val gap = totalSpacing / (count + 1)

            val topPadding = 22.dp.toPx()
            val bottomPadding = 6.dp.toPx()
            val availH = (h - topPadding - bottomPadding).coerceAtLeast(1f)
            val baseY = h - bottomPadding

            // Grid lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = baseY - availH * fraction
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Compact Y-axis amount labels for top and mid gridlines
            if (progress >= 0.75f && maxAmount > 1.0) {
                listOf(0.5f, 1.0f).forEach { fraction ->
                    val y = baseY - availH * fraction
                    val label = formatAxisAmount(maxAmount * fraction, currency)
                    val textLayout = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary.copy(alpha = 0.55f)
                        )
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(
                            x = w - textLayout.size.width - 2.dp.toPx(),
                            y = y - textLayout.size.height - 2.dp.toPx()
                        )
                    )
                }
            }

            if (chartMode == SpendingChartMode.BAR) {
                // Glowing Column Backdrop behind selected month
                if (selectedIndex != -1) {
                    val selX = gap + selectedIndex * (barWidth + gap)
                    val colLeft = selX - gap * 0.35f
                    val colWidth = barWidth + gap * 0.7f

                    // Layer 1: Ambient outer vertical glow bloom
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberBlue.copy(alpha = 0.20f),
                                CyberBlue.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h
                        ),
                        topLeft = Offset(colLeft - 3.dp.toPx(), 0f),
                        size = Size(colWidth + 6.dp.toPx(), h),
                        cornerRadius = CornerRadius(colCornerPx + 3.dp.toPx(), colCornerPx + 3.dp.toPx())
                    )

                    // Layer 2: Main vertical neon light-beam pillar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberBlue.copy(alpha = 0.25f),
                                CyberBlue.copy(alpha = 0.12f),
                                CyberBlue.copy(alpha = 0.03f)
                            ),
                            startY = 0f,
                            endY = h
                        ),
                        topLeft = Offset(colLeft, 0f),
                        size = Size(colWidth, h),
                        cornerRadius = CornerRadius(colCornerPx, colCornerPx)
                    )

                    // Layer 3: Diffused outer border aura
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberBlue.copy(alpha = 0.40f),
                                CyberBlue.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(colLeft, 0f),
                        size = Size(colWidth, h),
                        cornerRadius = CornerRadius(colCornerPx, colCornerPx),
                        style = Stroke(width = colBorderPx * 2.5f)
                    )

                    // Layer 4: Sharp neon inner border
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberBlueLight.copy(alpha = 0.85f),
                                CyberBlue.copy(alpha = 0.45f),
                                CyberBlue.copy(alpha = 0.12f)
                            )
                        ),
                        topLeft = Offset(colLeft, 0f),
                        size = Size(colWidth, h),
                        cornerRadius = CornerRadius(colCornerPx, colCornerPx),
                        style = Stroke(width = colBorderPx)
                    )
                }

                monthlyData.forEachIndexed { mi, month ->
                    val x = gap + mi * (barWidth + gap)
                    var accumulatedTop = baseY
                    val isThisMonthSelected = mi == selectedIndex
                    val barAlpha = when {
                        !isAnyMonthSelected -> 1f
                        isThisMonthSelected -> 1f
                        else -> 0.35f
                    }

                    month.slices.forEachIndexed { si, slice ->
                        val segH = ((slice.amount / maxAmount) * availH * progress).toFloat()
                            .coerceAtLeast(if (slice.amount > 0) 2f else 0f)
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

                    if (isThisMonthSelected && month.total > 0) {
                        val barTop = (accumulatedTop - 2.dp.toPx()).coerceAtLeast(0f)
                        val barTotalH = (baseY - barTop).coerceAtLeast(4f)
                        val barCenterX = x + barWidth / 2f
                        val barCenterY = (barTop + baseY) / 2f
                        val glowRadius = (barWidth + barTotalH) * 0.55f

                        // 1. Ambient radial bloom
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyberBlue.copy(alpha = 0.38f),
                                    CyberBlue.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(barCenterX, barCenterY),
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = Offset(barCenterX, barCenterY)
                        )

                        // 2. Wide soft outer glow aura
                        drawRoundRect(
                            color = CyberBlue.copy(alpha = 0.22f),
                            topLeft = Offset(x - 5.dp.toPx(), (barTop - 3.dp.toPx()).coerceAtLeast(0f)),
                            size = Size(barWidth + 10.dp.toPx(), barTotalH + 3.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // 3. Mid-range neon halo
                        drawRoundRect(
                            color = CyberBlue.copy(alpha = 0.50f),
                            topLeft = Offset(x - 2.5.dp.toPx(), (barTop - 1.5.dp.toPx()).coerceAtLeast(0f)),
                            size = Size(barWidth + 5.dp.toPx(), barTotalH + 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 4. Sharp luminous core outline with gradient
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White,
                                    CyberBlueLight,
                                    CyberBlue
                                ),
                                startY = barTop,
                                endY = baseY
                            ),
                            topLeft = Offset(x - 1.dp.toPx(), barTop),
                            size = Size(barWidth + 2.dp.toPx(), barTotalH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // 5. Bright top beacon flare
                        drawLine(
                            color = Color.White.copy(alpha = 0.95f),
                            start = Offset(x + 1.dp.toPx(), barTop),
                            end = Offset(x + barWidth - 1.dp.toPx(), barTop),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                }
            } else {
                // Line area mode (6-month spending trend)
                val n = monthlyData.size
                fun xAt(i: Int) = gap + i * (barWidth + gap) + barWidth / 2f
                fun yAt(i: Int): Float {
                    val raw = (monthlyData[i].total / maxAmount).toFloat()
                    val clamped = (raw * progress).coerceIn(0f, 1f)
                    return baseY - availH * clamped
                }

                if (n >= 2) {
                    val fillPath = Path().apply {
                        moveTo(xAt(0), baseY)
                        lineTo(xAt(0), yAt(0))
                        for (i in 1 until n) {
                            val cx = (xAt(i - 1) + xAt(i)) / 2f
                            cubicTo(cx, yAt(i - 1), cx, yAt(i), xAt(i), yAt(i))
                        }
                        lineTo(xAt(n - 1), baseY)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberBlue.copy(alpha = 0.32f),
                                CyberBlue.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            startY = topPadding,
                            endY = baseY
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
                        color = CyberBlue,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                for (i in 0 until n) {
                    val px = xAt(i)
                    val py = yAt(i)
                    val isSelected = i == selectedIndex

                    if (isSelected) {
                        // Vertical guideline pillar from point down to base
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CyberBlueLight.copy(alpha = 0.70f),
                                    CyberBlue.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                startY = py,
                                endY = baseY
                            ),
                            start = Offset(px, py),
                            end = Offset(px, baseY),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // Ambient radial bloom around beacon dot
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CyberBlue.copy(alpha = 0.45f),
                                    CyberBlue.copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(px, py),
                                radius = 16.dp.toPx()
                            ),
                            radius = 16.dp.toPx(),
                            center = Offset(px, py)
                        )

                        // Outer beacon ring
                        drawCircle(
                            color = CyberBlueLight,
                            radius = 6.dp.toPx(),
                            center = Offset(px, py),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Center white core
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = Offset(px, py)
                        )

                        // Amount badge above beacon dot
                        if (progress >= 0.85f) {
                            val text = formatAmount(monthlyData[i].total, currency)
                            val textLayout = textMeasurer.measure(
                                text = text,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberBlueLight
                                )
                            )
                            val tw = textLayout.size.width
                            val th = textLayout.size.height
                            val tx = (px - tw / 2f).coerceIn(4f, w - tw - 4f)
                            val ty = (py - th - 8.dp.toPx()).coerceAtLeast(2.dp.toPx())

                            drawRoundRect(
                                color = DarkSurface.copy(alpha = 0.85f),
                                topLeft = Offset(tx - 4.dp.toPx(), ty - 2.dp.toPx()),
                                size = Size(tw + 8.dp.toPx(), th + 4.dp.toPx()),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                            drawRoundRect(
                                color = CyberBlue.copy(alpha = 0.5f),
                                topLeft = Offset(tx - 4.dp.toPx(), ty - 2.dp.toPx()),
                                size = Size(tw + 8.dp.toPx(), th + 4.dp.toPx()),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(tx, ty)
                            )
                        }
                    } else {
                        drawCircle(
                            color = CyberBlue.copy(alpha = 0.7f),
                            radius = 3.5.dp.toPx(),
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = DarkSurface,
                            radius = 1.8.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Month labels — aligned to exact bar centers, Option 1 pill highlight
        val count = monthlyData.size
        val gapWeight = 0.28f / (count + 1)
        val barWeight = 0.72f / count
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
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            CyberBlue.copy(alpha = 0.30f),
                                            CyberBlue.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            CyberBlueLight.copy(alpha = 0.85f),
                                            CyberBlue.copy(alpha = 0.35f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = month.monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlueLight,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(CyberBlueLight)
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
                            shape = RoundedCornerShape(12.dp)
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
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                headerAction?.invoke()
            }
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chart Mode Selector (Bar / Line Area segmented switch)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChartModeSelector(
    selectedMode: SpendingChartMode,
    onModeSelected: (SpendingChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpendingChartMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) {
                            Brush.verticalGradient(
                                listOf(
                                    CyberBlue.copy(alpha = 0.35f),
                                    CyberBlue.copy(alpha = 0.18f)
                                )
                            )
                        } else {
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.dp,
                                color = CyberBlueLight.copy(alpha = 0.60f),
                                shape = RoundedCornerShape(6.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onModeSelected(mode)
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (mode == SpendingChartMode.BAR) "Bar" else "Line",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) CyberBlueLight else TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Monthly Spending card (merged 6-month trend & payment sources)
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
    var chartMode by rememberSaveable { mutableStateOf(SpendingChartMode.BAR) }

    WalletCard(
        title = "Monthly Spending",
        modifier = modifier,
        headerAction = {
            if (monthlyData.any { it.slices.isNotEmpty() || it.total > 0 }) {
                ChartModeSelector(
                    selectedMode = chartMode,
                    onModeSelected = { chartMode = it }
                )
            }
        }
    ) {
        if (currentMonthSources.isEmpty() && monthlyData.all { it.slices.isEmpty() && it.total == 0.0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No spending data",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            return@WalletCard
        }

        // Stacked bar or line area chart with interactive month selection
        if (monthlyData.any { it.slices.isNotEmpty() || it.total > 0 }) {
            MonthlyPaymentSourceStackedBar(
                monthlyData = monthlyData,
                currency = currency,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                chartMode = chartMode,
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
