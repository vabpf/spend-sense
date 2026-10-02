package com.spendsense.presentation.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.preferences.SecurePreferences
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.ExchangeRateRepository
import com.spendsense.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategorySlice(
    val category: Category,
    val amount: Double,
    val fraction: Float
)

data class DailyBar(
    val label: String,
    val amount: Double,
    val transactionCount: Int
)

data class MonthlyPoint(
    val monthLabel: String,
    val amount: Double
)

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
    val year: Int = 0,
    val month: Int = 0,
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
    val allTransactions: List<Transaction> = emptyList(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedMonthLabel: String = "",
    val isCurrentMonth: Boolean = true
)

private data class ChartsRawInput(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val selYear: Int,
    val selMonth: Int
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val securePreferences: SecurePreferences,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(now.get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(now.get(Calendar.MONTH))

    private val _state = MutableStateFlow(ChartsDataState())
    val state: StateFlow<ChartsDataState> = _state.asStateFlow()

    // Keep backward-compat accessor for summary cards
    val summary: StateFlow<ChartsSummaryState> get() = MutableStateFlow(_state.value.summary)

    init {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                _selectedYear,
                _selectedMonth
            ) { transactions, categories, selYear, selMonth ->
                ChartsRawInput(transactions, categories, selYear, selMonth)
            }.collect { input ->
                val transactions = input.transactions
                val categories = input.categories
                val selectedYear = input.selYear
                val selectedMonth = input.selMonth

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
                val currentNow = Calendar.getInstance()
                val curYear = currentNow.get(Calendar.YEAR)
                val curMonth = currentNow.get(Calendar.MONTH)
                val isCurrentMonth = selectedYear == curYear && selectedMonth == curMonth

                val monthLabels = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val fullMonthNames = listOf(
                    "January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"
                )
                val selectedMonthLabel = "${fullMonthNames[selectedMonth]} $selectedYear"

                val selCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val selMonthStart = selCal.timeInMillis
                val nextMonthCal = (selCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                val selMonthEnd = nextMonthCal.timeInMillis
                val prevMonthCal = (selCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                val prevMonthStart = prevMonthCal.timeInMillis

                val selMonthTxns = convertedTransactions.filter { it.timestamp in selMonthStart until selMonthEnd }
                val prevMonthTxns = convertedTransactions.filter { it.timestamp in prevMonthStart until selMonthStart }

                // ── Summary for selected month ───────────────────────────────
                val selMonthTotal = selMonthTxns.sumOf { it.amount }
                val prevMonthTotal = prevMonthTxns.sumOf { it.amount }
                val daysElapsed = if (isCurrentMonth) {
                    currentNow.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                } else {
                    selCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dailyAverage = if (daysElapsed > 0) selMonthTotal / daysElapsed else 0.0
                val prevMonthDailyAvg = if (prevMonthTxns.isNotEmpty()) prevMonthTotal / daysInPrevMonth else 0.0

                val categoryTotals = selMonthTxns.groupBy { it.categoryId }
                    .mapValues { (_, txns) -> txns.sumOf { it.amount } }
                val topEntry = categoryTotals.maxByOrNull { it.value }
                val topCategory = topEntry?.key?.let { categoryMap[it] }
                val topCategoryAmount = topEntry?.value ?: 0.0
                val biggestTxn = selMonthTxns.maxByOrNull { it.amount }
                val biggestTxnCat = biggestTxn?.categoryId?.let { categoryMap[it] }

                val summaryState = ChartsSummaryState(
                    currency = currency,
                    thisMonthTotal = selMonthTotal,
                    lastMonthTotal = prevMonthTotal,
                    dailyAverage = dailyAverage,
                    lastMonthDailyAverage = prevMonthDailyAvg,
                    topCategory = topCategory,
                    topCategoryAmount = topCategoryAmount,
                    biggestTransaction = biggestTxn,
                    biggestTransactionCategory = biggestTxnCat,
                    categories = categories
                )

                // ── Donut: category slices for selected month ─────────────────
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
                val monthlyPoints = (5 downTo 0).map { monthsBack ->
                    val mCal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsBack) }
                    val mStart = monthStart(mCal, 0)
                    val mEnd = monthStart(mCal, 1)
                    val label = monthLabels[mCal.get(Calendar.MONTH)]
                    val total = convertedTransactions.filter { it.timestamp in mStart until mEnd }.sumOf { it.amount }
                    MonthlyPoint(label, total)
                }

                // ── Payment source: selected month breakdown ──────────────────
                val sourceGroups = selMonthTxns
                    .groupBy { Pair(it.paymentSourceType, it.paymentSource) }
                    .map { (key, txns) ->
                        PaymentSourceBreakdown(key.first, key.second, txns.sumOf { it.amount })
                    }
                    .sortedByDescending { it.amount }

                // ── Payment source: monthly stacked data (6 months) ───────────
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
                        year = mCal.get(Calendar.YEAR),
                        month = mCal.get(Calendar.MONTH),
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
                    allTransactions = convertedTransactions,
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    selectedMonthLabel = selectedMonthLabel,
                    isCurrentMonth = isCurrentMonth
                )
            }
        }
    }

    fun selectMonth(year: Int, month: Int) {
        val currentNow = Calendar.getInstance()
        val curYear = currentNow.get(Calendar.YEAR)
        val curMonth = currentNow.get(Calendar.MONTH)
        if (_selectedYear.value == year && _selectedMonth.value == month) {
            // Tapping already-selected month resets to current month if not already on current month
            if (year != curYear || month != curMonth) {
                _selectedYear.value = curYear
                _selectedMonth.value = curMonth
            }
        } else {
            _selectedYear.value = year
            _selectedMonth.value = month
        }
    }

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    fun refresh() {
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
