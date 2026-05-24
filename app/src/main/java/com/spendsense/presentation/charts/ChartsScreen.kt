package com.spendsense.presentation.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.NeonMint
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.parseColor
import kotlin.math.abs

@Composable
fun ChartsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val summary = state.summary

    Scaffold(containerColor = Color.Transparent) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(40.dp)) }

                // ── Row 1: This Month + Daily Average ────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonthTotalCard(
                            modifier = Modifier.weight(1f),
                            currency = summary.currency,
                            thisMonth = summary.thisMonthTotal,
                            lastMonth = summary.lastMonthTotal
                        )
                        DailyAverageCard(
                            modifier = Modifier.weight(1f),
                            currency = summary.currency,
                            dailyAverage = summary.dailyAverage,
                            lastMonthDailyAverage = summary.lastMonthDailyAverage
                        )
                    }
                }

                // ── Row 2: Top Category + Biggest Transaction ─────────────────────
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

                // ── Donut chart ───────────────────────────────────────────────────
                item {
                    CategoryDonutChart(
                        slices = state.categorySlices,
                        currency = summary.currency
                    )
                }

                // ── Daily bar chart ───────────────────────────────────────────────
                item {
                    DailySpendingBarChart(
                        bars = state.dailyBars,
                        currency = summary.currency
                    )
                }

                // ── Monthly trend line ────────────────────────────────────────────
                item {
                    MonthlyTrendLineChart(
                        points = state.monthlyPoints,
                        currency = summary.currency
                    )
                }

                item { Spacer(modifier = Modifier.height(120.dp)) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background,
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            0.75f to MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                            1.0f to Color.Transparent
                        )
                    )
                    .align(Alignment.TopCenter)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthTotalCard(
    modifier: Modifier = Modifier,
    currency: String,
    thisMonth: Double,
    lastMonth: Double
) {
    val delta = thisMonth - lastMonth
    val deltaPositive = delta >= 0
    val deltaColor = if (deltaPositive) NeonRose else NeonMint
    val deltaIcon = if (deltaPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val deltaLabel = if (lastMonth > 0) {
        val pct = (abs(delta) / lastMonth * 100).toInt()
        "${if (deltaPositive) "+" else "-"}$pct% vs last month"
    } else {
        "No data last month"
    }

    GlassSummaryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "This Month",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatAmount(thisMonth, currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lastMonth > 0) {
                    Icon(deltaIcon, contentDescription = null, tint = deltaColor, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = deltaLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lastMonth > 0) deltaColor else MaterialTheme.colorScheme.onSurfaceVariant
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
    val deltaColor = if (deltaPositive) NeonRose else NeonMint
    val deltaIcon = if (deltaPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val deltaLabel = if (lastMonthDailyAverage > 0) {
        val pct = (abs(delta) / lastMonthDailyAverage * 100).toInt()
        "${if (deltaPositive) "+" else "-"}$pct% vs last month"
    } else {
        "No data last month"
    }

    GlassSummaryCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Daily Average",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatAmount(dailyAverage, currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (lastMonthDailyAverage > 0) {
                    Icon(deltaIcon, contentDescription = null, tint = deltaColor, modifier = Modifier.size(12.dp))
                }
                Text(
                    text = deltaLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lastMonthDailyAverage > 0) deltaColor else MaterialTheme.colorScheme.onSurfaceVariant
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Top Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (category != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.iconName),
                        contentDescription = null,
                        tint = parseColor(category.colorHex),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatAmount(amount, currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Biggest Spend",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (transaction != null) {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatAmount(transaction.amount, transaction.currencyCode),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (category != null) {
                        Icon(
                            imageVector = getCategoryIcon(category.iconName),
                            contentDescription = null,
                            tint = parseColor(category.colorHex),
                            modifier = Modifier.size(14.dp)
                        )
                    }
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
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.24f
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun EmptyStateText() {
    Text(text = "No data yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
}
