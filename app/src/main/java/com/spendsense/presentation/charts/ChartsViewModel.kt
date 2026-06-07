package com.spendsense.presentation.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.SecurePreferences
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.TransactionRepository
import com.spendsense.domain.repository.ExchangeRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject

data class ChartsSummaryState(
    val currency: String = "USD",
    val thisMonthTotal: Double = 0.0,
    val lastMonthTotal: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val lastMonthDailyAverage: Double = 0.0,
    val topCategory: Category? = null,
    val topCategoryAmount: Double = 0.0,
    val biggestTransaction: Transaction? = null,
    val biggestTransactionCategory: Category? = null,
    val categories: List<Category> = emptyList()
)

data class CategorySlice(
    val category: Category,
    val amount: Double,
    val fraction: Float
)

data class DailyBar(
    val dayLabel: String,
    val amount: Double,
    val transactionCount: Int = 0
)

data class MonthlyPoint(
    val monthLabel: String, // "Jan", "Feb", etc.
    val amount: Double
)

data class PaymentSourceBreakdown(
    val type: String,
    val identifier: String,
    val amount: Double
)

data class MonthlyPaymentSourceSlice(
    val type: String,
    val amount: Double,
    val sources: List<PaymentSourceBreakdown>,
    val fraction: Float
)

data class MonthlyPaymentSourceData(
    val monthLabel: String,
    val slices: List<MonthlyPaymentSourceSlice>,
    val total: Double
)

data class ChartsDataState(
    val summary: ChartsSummaryState = ChartsSummaryState(),
    val categorySlices: List<CategorySlice> = emptyList(),
    val dailyBars: List<DailyBar> = emptyList(),
    val monthlyPoints: List<MonthlyPoint> = emptyList(),
    val currentMonthPaymentSources: List<PaymentSourceBreakdown> = emptyList(),
    val monthlyPaymentSources: List<MonthlyPaymentSourceData> = emptyList(),
    val allTransactions: List<Transaction> = emptyList()
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val securePreferences: SecurePreferences,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChartsDataState())
    val state: StateFlow<ChartsDataState> = _state.asStateFlow()

    // Keep backward-compat accessor for summary cards
    val summary: StateFlow<ChartsSummaryState> get() = MutableStateFlow(_state.value.summary)

    init {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories()
            ) { transactions, categories ->
                transactions to categories
            }.collect { (transactions, categories) ->
                val currency = securePreferences.getDefaultCurrency()

                // Concurrently convert all transaction amounts to the display currency in parallel
                val convertedTransactions = transactions.map { txn ->
                    async(Dispatchers.IO) {
                        val rate = if (txn.currencyCode == currency) {
                            1.0
                        } else {
                            exchangeRateRepository.getRate(
                                from = txn.currencyCode,
                                to = currency,
                                dateMillis = txn.timestamp
                            ) ?: 0.0
                        }
                        txn.copy(amount = txn.amount * rate, currencyCode = currency)
                    }
                }.map { it.await() }

                val categoryMap = categories.associateBy { it.id }
                val now = Calendar.getInstance()

                val thisMonthStart = monthStart(now, 0)
                val lastMonthStart = monthStart(now, -1)
                val sixMonthsAgoStart = monthStart(now, -5)

                val thisMonthTxns = convertedTransactions.filter { it.timestamp >= thisMonthStart }
                val lastMonthTxns = convertedTransactions.filter { it.timestamp in lastMonthStart until thisMonthStart }

                // ── Summary ──────────────────────────────────────────────────
                val thisMonthTotal = thisMonthTxns.sumOf { it.amount }
                val lastMonthTotal = lastMonthTxns.sumOf { it.amount }
                val daysElapsed = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                val daysInLastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    .getActualMaximum(Calendar.DAY_OF_MONTH)
                val dailyAverage = thisMonthTotal / daysElapsed
                val lastMonthDailyAvg = if (lastMonthTxns.isNotEmpty()) lastMonthTotal / daysInLastMonth else 0.0

                val categoryTotals = thisMonthTxns.groupBy { it.categoryId }
                    .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                val topEntry = categoryTotals.maxByOrNull { it.value }
                val topCategory = topEntry?.key?.let { categoryMap[it] }
                val topCategoryAmount = topEntry?.value ?: 0.0
                val biggestTxn = thisMonthTxns.maxByOrNull { it.amount }
                val biggestTxnCat = biggestTxn?.categoryId?.let { categoryMap[it] }

                val summaryState = ChartsSummaryState(
                    currency = currency,
                    thisMonthTotal = thisMonthTotal,
                    lastMonthTotal = lastMonthTotal,
                    dailyAverage = dailyAverage,
                    lastMonthDailyAverage = lastMonthDailyAvg,
                    topCategory = topCategory,
                    topCategoryAmount = topCategoryAmount,
                    biggestTransaction = biggestTxn,
                    biggestTransactionCategory = biggestTxnCat,
                    categories = categories
                )

                // ── Donut: category slices for this month ─────────────────────
                val slices = categoryTotals
                    .mapNotNull { (catId, amount) ->
                        val cat = categoryMap[catId] ?: return@mapNotNull null
                        cat to amount
                    }
                    .sortedByDescending { it.second }
                    .take(6)
                val sliceTotal = slices.sumOf { it.second }.takeIf { it > 0 } ?: 1.0
                val categorySlices = slices.map { (cat, amount) ->
                    CategorySlice(cat, amount, (amount / sliceTotal).toFloat())
                }

                // ── Daily bar: last 7 days ────────────────────────────────────
                val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val dailyBars = (6 downTo 0).map { daysBack ->
                    val dayCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -daysBack)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val dayStart = dayCal.timeInMillis
                    val dayEnd = dayStart + 86_400_000L
                    val label = dayLabels[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
                    val dayTxns = convertedTransactions.filter { it.timestamp in dayStart until dayEnd }
                    DailyBar(label, dayTxns.sumOf { it.amount }, dayTxns.size)
                }

                // ── Monthly trend: last 6 months ──────────────────────────────
                val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthlyPoints = (5 downTo 0).map { monthsBack ->
                    val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsBack) }
                    val mStart = monthStart(mCal, 0)
                    val mEnd = monthStart(mCal, 1)
                    val label = monthLabels[mCal.get(Calendar.MONTH)]
                    val total = convertedTransactions.filter { it.timestamp in mStart until mEnd }.sumOf { it.amount }
                    MonthlyPoint(label, total)
                }

                // ── Payment source: current month breakdown ──────────────────
                val sourceGroups = thisMonthTxns
                    .groupBy { Pair(it.paymentSourceType, it.paymentSource) }
                    .map { (key, txns) ->
                        PaymentSourceBreakdown(key.first, key.second, txns.sumOf { it.amount })
                    }
                    .sortedByDescending { it.amount }

                // ── Payment source: monthly stacked data ─────────────────────
                val monthlyPaymentSources = (5 downTo 0).map { monthsBack ->
                    val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsBack) }
                    val mStart = monthStart(mCal, 0)
                    val mEnd = monthStart(mCal, 1)
                    val monthTxns = convertedTransactions.filter { it.timestamp in mStart until mEnd }
                    val monthTotal = monthTxns.sumOf { it.amount }.takeIf { it > 0 } ?: 1.0
                    val typeGroups = monthTxns
                        .groupBy { it.paymentSourceType }
                        .map { (type, txns) ->
                            val typeAmount = txns.sumOf { it.amount }
                            val sourceList = txns
                                .groupBy { it.paymentSource }
                                .map { (src, srcTxns) ->
                                    PaymentSourceBreakdown(type, src, srcTxns.sumOf { it.amount })
                                }
                                .sortedByDescending { it.amount }
                            MonthlyPaymentSourceSlice(
                                type = type,
                                amount = typeAmount,
                                sources = sourceList,
                                fraction = (typeAmount / monthTotal).toFloat()
                            )
                        }
                        .sortedByDescending { it.amount }
                    MonthlyPaymentSourceData(
                        monthLabel = monthLabels[mCal.get(Calendar.MONTH)],
                        slices = typeGroups,
                        total = monthTotal
                    )
                }

                _state.value = ChartsDataState(
                    summary = summaryState,
                    categorySlices = categorySlices,
                    dailyBars = dailyBars,
                    monthlyPoints = monthlyPoints,
                    currentMonthPaymentSources = sourceGroups,
                    monthlyPaymentSources = monthlyPaymentSources,
                    allTransactions = convertedTransactions
                )
            }
        }
    }

    fun refresh() {
        // Re-trigger currency refresh; the flow will recompute on next emission
        val current = _state.value
        _state.value = current.copy(
            summary = current.summary.copy(currency = securePreferences.getDefaultCurrency())
        )
    }

    private fun monthStart(base: Calendar, offset: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = base.timeInMillis
            add(Calendar.MONTH, offset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
