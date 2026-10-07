package com.spendsense.presentation.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.util.fadingEdge
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.parseColor
import kotlin.math.abs

@Composable
fun ChartsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChartsViewModel = hiltViewModel(),
    onNavigateToHomeWithFilter: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val summary = state.summary
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val topFadeHeight by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                statusBarPadding + 16.dp
            } else {
                val offsetDp = with(density) { listState.firstVisibleItemScrollOffset.toDp() }
                offsetDp.coerceAtMost(statusBarPadding + 16.dp)
            }
        }
    }

    Scaffold(containerColor = Color.Transparent) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient scrim: smoothly transitions wallpaper to #F8FAFC
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x00F8FAFC),
                                0.35f to Color(0x26F8FAFC),
                                0.70f to Color(0x8CF8FAFC),
                                0.90f to Color(0xDEF8FAFC),
                                1.0f to Color(0xFFF8FAFC)
                            ),
                            startY = 0f,
                            endY = with(LocalDensity.current) { (statusBarPadding + 260.dp).toPx() }
                        )
                    )
            )

            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxSize()
                    .fadingEdge(topFadeHeight = topFadeHeight),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarPadding + 12.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Row 1: This Month + Daily Average (Glass Effect) ─────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonthTotalCard(
                            modifier = Modifier.weight(1f),
                            currency = summary.currency,
                            thisMonth = summary.thisMonthTotal,
                            lastMonth = summary.lastMonthTotal,
                            monthLabel = if (state.isCurrentMonth) "This Month" else state.selectedMonthLabel
                        )
                        DailyAverageCard(
                            modifier = Modifier.weight(1f),
                            currency = summary.currency,
                            dailyAverage = summary.dailyAverage,
                            lastMonthDailyAverage = summary.lastMonthDailyAverage
                        )
                    }
                }

                // ── Row 2: Top Category + Biggest Transaction (Glass Effect) ──────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TopCategoryCard(
                            modifier = Modifier.weight(1f),
                            currency = summary.currency,
                            category = summary.topCategory,
                            amount = summary.topCategoryAmount
                        )
                        BiggestTransactionCard(
                            modifier = Modifier.weight(1f),
                            currency = summary.currency,
                            transaction = summary.biggestTransaction,
                            category = summary.biggestTransactionCategory
                        )
                    }
                }

                // ── Donut chart: Spending by Category (White Card) ─────────────────
                item {
                    CategoryDonutChart(
                        slices = state.categorySlices,
                        currency = summary.currency,
                        monthLabel = if (state.isCurrentMonth) null else state.selectedMonthLabel
                    )
                }

                // ── Monthly Spending & Payment sources (White Card) ───────────────
                item {
                    PaymentSourcesCard(
                        currentMonthSources = state.currentMonthPaymentSources,
                        monthlyData = state.monthlyPaymentSources,
                        currency = summary.currency,
                        selectedYear = state.selectedYear,
                        selectedMonth = state.selectedMonth,
                        onMonthSelected = { y, m -> viewModel.selectMonth(y, m) }
                    )
                }

                // ── Daily calendar chart (White Card) ─────────────────────────────
                item {
                    CalendarSpendingChart(
                        allTransactions = state.allTransactions,
                        categories = summary.categories,
                        currency = summary.currency,
                        selectedYear = state.selectedYear,
                        selectedMonth = state.selectedMonth,
                        onMonthChanged = { y, m -> viewModel.setMonth(y, m) },
                        onFilterDay = onNavigateToHomeWithFilter
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary cards (Frosted Glass)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthTotalCard(
    modifier: Modifier = Modifier,
    currency: String,
    thisMonth: Double,
    lastMonth: Double,
    monthLabel: String = "This Month"
) {
    val delta = thisMonth - lastMonth
    val deltaPositive = delta >= 0
    val deltaColor = if (deltaPositive) Color(0xFFDC2626) else Color(0xFF16A34A)
    val deltaIcon = if (deltaPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val deltaLabel = if (lastMonth > 0) {
        val pct = (abs(delta) / lastMonth * 100).toInt()
        "$pct% vs last month"
    } else {
        "No data last month"
    }

    GlassSummaryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDCFCE7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Text(
                text = formatAmount(thisMonth, currency),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lastMonth > 0) {
                    Icon(deltaIcon, contentDescription = null, tint = deltaColor, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = deltaLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (lastMonth > 0) deltaColor else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun DailyAverageCard(
    modifier: Modifier = Modifier,
    currency: String,
    dailyAverage: Double,
    lastMonthDailyAverage: Double
) {
    val delta = dailyAverage - lastMonthDailyAverage
    val deltaPositive = delta >= 0
    val deltaColor = if (deltaPositive) Color(0xFFDC2626) else Color(0xFF16A34A)
    val deltaIcon = if (deltaPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val deltaLabel = if (lastMonthDailyAverage > 0) {
        val pct = (abs(delta) / lastMonthDailyAverage * 100).toInt()
        "$pct% vs last month"
    } else {
        "No data last month"
    }

    GlassSummaryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Average",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3E8FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF9333EA),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = formatAmount(dailyAverage, currency),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lastMonthDailyAverage > 0) {
                    Icon(deltaIcon, contentDescription = null, tint = deltaColor, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = deltaLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (lastMonthDailyAverage > 0) deltaColor else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun TopCategoryCard(
    modifier: Modifier = Modifier,
    currency: String,
    category: Category?,
    amount: Double
) {
    GlassSummaryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Top Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0284C7)
            )
            if (category != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val catColor = parseColor(category.colorHex)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.iconName),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatAmount(amount, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                EmptyStateText()
            }
        }
    }
}

@Composable
private fun BiggestTransactionCard(
    modifier: Modifier = Modifier,
    currency: String,
    transaction: Transaction?,
    category: Category?
) {
    GlassSummaryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Biggest Spend",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0284C7)
            )
            if (transaction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val iconColor = category?.let { parseColor(it.colorHex) } ?: Color(0xFF2563EB)
                    val iconVector = category?.let { getCategoryIcon(it.iconName) } ?: Icons.Rounded.LocalGasStation
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = transaction.merchant,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatAmount(transaction.amount, transaction.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                EmptyStateText()
            }
        }
    }
}

@Composable
private fun GlassSummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassEffect(
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
private fun EmptyStateText() {
    Text(text = "No data yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}
