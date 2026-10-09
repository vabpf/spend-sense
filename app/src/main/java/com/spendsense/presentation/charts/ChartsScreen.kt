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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.spendsense.domain.calculation.CategoryBehaviorProfiler
import com.spendsense.domain.calculation.MonthForecastResult
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

    val detectedCreditCardSources = remember(state.allTransactions) {
        state.allTransactions
            .filter { it.paymentSourceType.equals("Credit Card", ignoreCase = true) && it.paymentSource.isNotBlank() }
            .map { it.paymentSource.trim() }
            .distinct()
    }
    val configuredCardNames = remember(state.creditCardConfigs) {
        state.creditCardConfigs.map { it.cardName.trim().lowercase() }.toSet()
    }
    val unsetCreditCards = remember(detectedCreditCardSources, configuredCardNames) {
        detectedCreditCardSources.filter { it.lowercase() !in configuredCardNames }
    }

    var editingCardName by remember { mutableStateOf<String?>(null) }
    var showCardConfigDialog by remember { mutableStateOf(false) }
    var selectedCategoryForBehavior by remember { mutableStateOf<Category?>(null) }

    if (showCardConfigDialog) {
        val existingConfig = editingCardName?.let { name ->
            state.creditCardConfigs.find { it.cardName.trim().equals(name.trim(), ignoreCase = true) }
        }
        CreditCardConfigDialog(
            initialCardName = editingCardName ?: "",
            existingConfig = existingConfig,
            availableCardNames = detectedCreditCardSources,
            onDismiss = { showCardConfigDialog = false },
            onSave = { config ->
                viewModel.saveCreditCardConfig(config)
            },
            onDelete = { cardName ->
                viewModel.deleteCreditCardConfig(cardName)
            }
        )
    }

    val targetBehaviorCategory = selectedCategoryForBehavior
    if (targetBehaviorCategory != null) {
        val catTxns = remember(targetBehaviorCategory, state.selectedMonthTransactions, state.allTransactions) {
            state.selectedMonthTransactions.filter { it.categoryId == targetBehaviorCategory.id }.ifEmpty {
                state.allTransactions.filter { it.categoryId == targetBehaviorCategory.id }
            }
        }
        val behaviorProfile = remember(targetBehaviorCategory, catTxns, summary.currency) {
            CategoryBehaviorProfiler.profileCategory(
                category = targetBehaviorCategory,
                categoryTransactions = catTxns,
                currency = summary.currency
            )
        }
        CategoryBehaviorModal(
            profile = behaviorProfile,
            currency = summary.currency,
            periodLabel = if (state.isCurrentMonth) "This Month" else state.selectedMonthLabel,
            onDismiss = { selectedCategoryForBehavior = null }
        )
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
                // ── Month-End Predictive Forecast (REBD Glass Hero Card) ────────
                val forecast = state.monthForecast
                if (state.isCurrentMonth && forecast != null) {
                    item(key = "forecast_hero") {
                        MonthForecastHeroCard(
                            forecast = forecast,
                            currency = summary.currency
                        )
                    }
                }

                // ── Credit & Cash Liquidity Card ─────────────────────────────────
                val liquiditySummary = state.creditLiquiditySummary
                if (state.isCurrentMonth && liquiditySummary != null && (liquiditySummary.cardCycles.isNotEmpty() || liquiditySummary.totalCreditSpendThisMonth > 0 || unsetCreditCards.isNotEmpty())) {
                    item(key = "credit_liquidity") {
                        CreditLiquidityCard(
                            summary = liquiditySummary,
                            currency = summary.currency,
                            unsetCreditCards = unsetCreditCards,
                            onConfigureCard = { cardName ->
                                editingCardName = cardName
                                showCardConfigDialog = true
                            }
                        )
                    }
                }

                // ── Row 1: This Month + Daily Average (Glass Effect) ─────────────
                item(key = "month_summary_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MonthTotalCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            currency = summary.currency,
                            thisMonth = summary.thisMonthTotal,
                            lastMonth = summary.lastMonthTotal,
                            monthLabel = if (state.isCurrentMonth) "This Month" else state.selectedMonthLabel,
                            isSamePeriod = summary.isSamePeriodComparison
                        )
                        DailyAverageCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            currency = summary.currency,
                            dailyAverage = summary.dailyAverage,
                            lastMonthDailyAverage = summary.lastMonthDailyAverage
                        )
                    }
                }

                // ── Row 2: Top Category + Biggest Transaction (Glass Effect) ──────
                item(key = "top_category_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TopCategoryCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            currency = summary.currency,
                            category = summary.topCategory,
                            amount = summary.topCategoryAmount,
                            onClick = { summary.topCategory?.let { selectedCategoryForBehavior = it } }
                        )
                        BiggestTransactionCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            currency = summary.currency,
                            transaction = summary.biggestTransaction,
                            category = summary.biggestTransactionCategory
                        )
                    }
                }

                // ── Donut chart: Spending by Category (White Card) ─────────────────
                item(key = "category_donut") {
                    CategoryDonutChart(
                        slices = state.categorySlices,
                        currency = summary.currency,
                        monthLabel = if (state.isCurrentMonth) null else state.selectedMonthLabel,
                        onCategoryClick = { category ->
                            selectedCategoryForBehavior = category
                        }
                    )
                }

                // ── Monthly Spending & Payment sources (White Card) ───────────────
                item(key = "payment_sources") {
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
                item(key = "calendar_chart") {
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
private fun MonthForecastHeroCard(
    modifier: Modifier = Modifier,
    forecast: MonthForecastResult,
    currency: String
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    GlassSummaryCard(
        modifier = modifier.fillMaxWidth(),
        useLens = false
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Badge & Day Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Month-End Forecast",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                Text(
                    text = "Day ${forecast.daysElapsed}/${forecast.totalDaysInMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            // Hero Forecast & Trend Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "~${formatAmount(forecast.projectedMonthEndTotal, currency)}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Estimated total based on current pace",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B)
                    )
                }

                val pct = forecast.percentVsLastMonth
                if (pct != null) {
                    val isHigher = pct > 0
                    val pillBg = if (isHigher) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                    val pillColor = if (isHigher) Color(0xFFDC2626) else Color(0xFF16A34A)
                    val arrow = if (isHigher) "↗" else "↘"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(pillBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$arrow ${abs(pct)}% vs last mo",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = pillColor
                        )
                    }
                } else if (forecast.isStabilizingWithPrior) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Stabilizing pace",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Two-tone progress bar
            val totalEst = forecast.projectedMonthEndTotal.coerceAtLeast(1.0)
            val spentRatio = (forecast.spendToDate / totalEst).toFloat().coerceIn(0f, 1f)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFBAE6FD).copy(alpha = 0.6f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(spentRatio)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(CyberBlue, Color(0xFF38BDF8))
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Spent: ${formatAmount(forecast.spendToDate, currency)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "Proj. rem: ${formatAmount(forecast.projectedRemainingSpend, currency)} (${forecast.daysRemaining}d)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            // Tappable breakdown toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide breakdown" else "View breakdown & safe pace",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expandable details section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 140)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 100))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.04f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BreakdownDetailRow(
                        label = "Regular daily pace:",
                        value = "${formatAmount(forecast.variableDailyBurnRate, currency)} / day"
                    )
                    if (forecast.isolatedSpikesCount > 0) {
                        BreakdownDetailRow(
                            label = "Isolated one-off spikes (${forecast.isolatedSpikesCount}):",
                            value = "-${formatAmount(forecast.isolatedSpikesTotal, currency)}",
                            subtext = "Excluded from multiplying daily rate"
                        )
                    }
                    if (forecast.safeRemainingDailyPace != null && forecast.daysRemaining > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = Color(0xFFE2E8F0)
                        )
                        BreakdownDetailRow(
                            label = "Safe remaining pace:",
                            value = "${formatAmount(forecast.safeRemainingDailyPace, currency)} / day",
                            subtext = "Spend under this to stay below last month"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownDetailRow(
    label: String,
    value: String,
    subtext: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color(0xFF64748B)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
        }
        if (subtext != null) {
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun MonthTotalCard(
    modifier: Modifier = Modifier,
    currency: String,
    thisMonth: Double,
    lastMonth: Double,
    monthLabel: String = "This Month",
    isSamePeriod: Boolean = false
) {
    val delta = thisMonth - lastMonth
    val deltaPositive = delta >= 0
    val deltaColor = if (deltaPositive) Color(0xFFDC2626) else Color(0xFF16A34A)
    val deltaIcon = if (deltaPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward
    val deltaLabel = if (lastMonth > 0) {
        val pct = (abs(delta) / lastMonth * 100).toInt()
        if (isSamePeriod) "$pct% vs prev MTD" else "$pct% vs last month"
    } else {
        if (isSamePeriod) "No prev MTD data" else "No last month data"
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
    amount: Double,
    onClick: (() -> Unit)? = null
) {
    GlassSummaryCard(
        modifier = modifier.then(
            if (onClick != null && category != null) {
                Modifier.clickable { onClick() }
            } else Modifier
        )
    ) {
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
    useLens: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassEffect(
                shape = RoundedCornerShape(20.dp),
                useLens = useLens
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
