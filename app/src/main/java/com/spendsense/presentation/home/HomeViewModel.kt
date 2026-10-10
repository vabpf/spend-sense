package com.spendsense.presentation.home

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.MerchantCategoryMappingDao
import com.spendsense.data.local.dao.RawNotificationDao
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.data.service.AiTransactionParser
import com.spendsense.data.service.ParsedTransactionResult
import com.spendsense.domain.calculation.ForecastEngine
import com.spendsense.domain.calculation.MonthForecastResult
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.ExchangeRateRepository
import com.spendsense.domain.repository.TransactionRepository
import com.spendsense.presentation.util.ReceiptImageHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val rawNotificationDao: RawNotificationDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val securePreferences: SecurePreferences,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val aiTransactionParser: AiTransactionParser
) : ViewModel() {


    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _pendingNotifications = MutableStateFlow<List<RawNotificationEntity>>(emptyList())
    val pendingNotifications: StateFlow<List<RawNotificationEntity>> = _pendingNotifications.asStateFlow()

    private val _defaultCurrency = MutableStateFlow("USD")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    private val _convertedTotal = MutableStateFlow(0.0)
    val convertedTotal: StateFlow<Double> = _convertedTotal.asStateFlow()

    private val _todayConvertedTotal = MutableStateFlow(0.0)
    val todayConvertedTotal: StateFlow<Double> = _todayConvertedTotal.asStateFlow()

    private val _yesterdayConvertedTotal = MutableStateFlow(0.0)
    val yesterdayConvertedTotal: StateFlow<Double> = _yesterdayConvertedTotal.asStateFlow()

    private val _currentDailyAverage = MutableStateFlow(0.0)
    val currentDailyAverage: StateFlow<Double> = _currentDailyAverage.asStateFlow()

    private val _currentMonthForecast = MutableStateFlow<MonthForecastResult?>(null)
    val currentMonthForecast: StateFlow<MonthForecastResult?> = _currentMonthForecast.asStateFlow()

    init {
        loadTransactions()
        loadCategories()
        loadPendingNotifications()
        refreshDefaultCurrency()
    }

    fun refreshDefaultCurrency() {
        _defaultCurrency.value = securePreferences.getDefaultCurrency()
        recalculateConvertedTotal(_transactions.value)
    }

    suspend fun convertAmount(amount: Double, from: String, timestamp: Long): Double {
        val to = _defaultCurrency.value
        if (from == to) return amount
        val rate = exchangeRateRepository.getRate(from, to, timestamp)
        return if (rate != null) amount * rate else amount
    }

    private fun loadPendingNotifications() {
        viewModelScope.launch {
            rawNotificationDao.getUnprocessedNotificationsFlow().collect { notifications ->
                _pendingNotifications.value = notifications
            }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { transactions ->
                _transactions.value = transactions
                recalculateConvertedTotal(transactions)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val defaultCategoryOrder = listOf(
                "Food", "Shopping", "Entertainment", "Transport", "Bills", "Health", "Other"
            )
            categoryRepository.getAllCategories().collect { categories ->
                _categories.value = categories.sortedBy { cat ->
                    val idx = defaultCategoryOrder.indexOfFirst { it.equals(cat.name, ignoreCase = true) }
                    if (idx >= 0) {
                        if (cat.name.equals("Other", ignoreCase = true)) Int.MAX_VALUE else idx
                    } else {
                        defaultCategoryOrder.size + cat.id.toInt()
                    }
                }
            }
        }
    }

    private fun recalculateConvertedTotal(transactions: List<Transaction>) {
        viewModelScope.launch {
            val currency = _defaultCurrency.value
            
            val todayCal = Calendar.getInstance()
            val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val tempCal = Calendar.getInstance()

            val curYear = todayCal.get(Calendar.YEAR)
            val curMonth = todayCal.get(Calendar.MONTH)
            val curDay = todayCal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
            
            val prevMonthCal = (todayCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            val prevYear = prevMonthCal.get(Calendar.YEAR)
            val prevMonth = prevMonthCal.get(Calendar.MONTH)
            val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val totalDaysInCurrentMonth = todayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val todayTxns = mutableListOf<Transaction>()
            val yesterdayTxns = mutableListOf<Transaction>()
            val currentMonthTxns = mutableListOf<Transaction>()
            val prevMonthTxns = mutableListOf<Transaction>()
            
            for (txn in transactions) {
                tempCal.timeInMillis = txn.timestamp
                val isSameYear = tempCal.get(Calendar.YEAR) == curYear
                if (isSameYear && tempCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                    todayTxns.add(txn)
                } else if (tempCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) && tempCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
                    yesterdayTxns.add(txn)
                }
                if (isSameYear && tempCal.get(Calendar.MONTH) == curMonth) {
                    currentMonthTxns.add(txn)
                } else if (tempCal.get(Calendar.YEAR) == prevYear && tempCal.get(Calendar.MONTH) == prevMonth) {
                    prevMonthTxns.add(txn)
                }
            }

            val todayDeferred = todayTxns.map { txn ->
                async {
                    convertTransactionAmount(txn, currency)
                }
            }
            
            val yesterdayDeferred = yesterdayTxns.map { txn ->
                async {
                    convertTransactionAmount(txn, currency)
                }
            }

            val currentMonthConvertedDeferred = currentMonthTxns.map { txn ->
                async {
                    val amt = convertTransactionAmount(txn, currency)
                    txn.copy(amount = amt, currencyCode = currency)
                }
            }

            val prevMonthDeferred = prevMonthTxns.map { txn ->
                async {
                    convertTransactionAmount(txn, currency)
                }
            }

            val allDeferred = transactions.map { txn ->
                async {
                    convertTransactionAmount(txn, currency)
                }
            }

            val convertedCurrentMonthTxns = currentMonthConvertedDeferred.map { it.await() }
            val currentMonthTotal = convertedCurrentMonthTxns.sumOf { it.amount }
            val prevMonthTotal = prevMonthDeferred.sumOf { it.await() }

            _todayConvertedTotal.value = todayDeferred.sumOf { it.await() }
            _yesterdayConvertedTotal.value = yesterdayDeferred.sumOf { it.await() }
            _currentDailyAverage.value = if (curDay > 0) currentMonthTotal / curDay else 0.0
            _convertedTotal.value = allDeferred.sumOf { it.await() }

            _currentMonthForecast.value = ForecastEngine.calculateMonthForecast(
                currentMonthTransactions = convertedCurrentMonthTxns,
                daysElapsed = curDay,
                totalDaysInMonth = totalDaysInCurrentMonth,
                priorMonthTotal = prevMonthTotal,
                priorMonthTotalDays = daysInPrevMonth
            )
        }
    }

    private suspend fun convertTransactionAmount(txn: Transaction, targetCurrency: String): Double {
        return if (txn.currencyCode == targetCurrency) {
            txn.amount
        } else {
            val rate = exchangeRateRepository.getRate(
                from = txn.currencyCode,
                to = targetCurrency,
                dateMillis = txn.timestamp
            )
            if (rate != null) txn.amount * rate else 0.0
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
        }
    }

    fun updateTransactionWithMapping(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.updateTransaction(transaction)
            if (transaction.merchant.isNotBlank() && transaction.categoryId > 0) {
                merchantCategoryMappingDao.upsert(
                    MerchantCategoryMappingEntity(
                        merchant = transaction.merchant.lowercase(),
                        categoryId = transaction.categoryId
                    )
                )
            }
        }
    }

    fun updateTransactions(updatedList: List<Transaction>) {
        viewModelScope.launch {
            updatedList.forEach { transactionRepository.updateTransaction(it) }
        }
    }


    var isAnalyzingTransaction by mutableStateOf(false)
        private set
    var analyzingStatusText by mutableStateOf("Analyzing with AI...")
        private set
    var prefilledTransactionData by mutableStateOf<ParsedTransactionResult?>(null)
        private set
    var batchTransactionsData by mutableStateOf<List<ParsedTransactionResult>>(emptyList())
        private set

    fun parseQuickText(text: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            isAnalyzingTransaction = true
            analyzingStatusText = "Analyzing text with AI..."
            var count = 0
            try {
                val results = aiTransactionParser.parseBatchFromText(text)
                count = results.size
                if (results.size == 1) {
                    prefilledTransactionData = results.first()
                    batchTransactionsData = emptyList()
                } else if (results.size > 1) {
                    prefilledTransactionData = null
                    batchTransactionsData = results
                } else {
                    prefilledTransactionData = null
                    batchTransactionsData = emptyList()
                }
            } catch (_: Exception) {
            } finally {
                isAnalyzingTransaction = false
                onComplete(count)
            }
        }
    }

    fun parseReceiptImages(context: Context, uris: List<Uri>, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            isAnalyzingTransaction = true
            val allResults = mutableListOf<ParsedTransactionResult>()
            try {
                val total = uris.size
                for ((index, uri) in uris.withIndex()) {
                    val progress = if (total > 1) " (${index + 1}/$total)" else ""
                    analyzingStatusText = "Preparing receipt$progress..."
                    val base64DataUrl = withContext(Dispatchers.IO) {
                        ReceiptImageHelper.processAndEncodeImage(context, uri)
                    }
                    if (base64DataUrl != null) {
                        analyzingStatusText = "Analyzing receipt$progress with AI..."
                        val results = aiTransactionParser.parseBatchFromImage(base64DataUrl)
                        allResults.addAll(results)
                    }
                }
                if (allResults.size == 1) {
                    prefilledTransactionData = allResults.first()
                    batchTransactionsData = emptyList()
                } else if (allResults.size > 1) {
                    prefilledTransactionData = null
                    batchTransactionsData = allResults
                } else {
                    prefilledTransactionData = null
                    batchTransactionsData = emptyList()
                }
            } catch (_: Exception) {
            } finally {
                isAnalyzingTransaction = false
                onComplete(allResults.size)
            }
        }
    }

    fun parseReceiptImage(context: Context, uri: Uri, onComplete: () -> Unit = {}) {
        parseReceiptImages(context, listOf(uri)) {
            onComplete()
        }
    }

    fun clearPrefilledTransactionData() {
        prefilledTransactionData = null
    }

    fun clearBatchTransactionsData() {
        batchTransactionsData = emptyList()
    }

    fun addBatchTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            for (t in transactions) {
                transactionRepository.insertTransaction(t)
            }
        }
    }

    fun addTransaction(
        amount: Double,
        currencyCode: String,
        merchant: String,
        categoryId: Long,
        paymentSource: String = "Manual",
        paymentSourceType: String = "Manual",
        timestamp: Long = System.currentTimeMillis(),
        notes: String? = null
    ) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = amount,
                    currencyCode = currencyCode,
                    merchant = merchant,
                    categoryId = categoryId,
                    timestamp = timestamp,
                    sourcePackageName = "manual",
                    sourceAppName = "Manual Add",
                    notes = notes,
                    paymentSource = paymentSource,
                    paymentSourceType = paymentSourceType
                )
            )
        }
    }

    fun deleteNotification(notification: RawNotificationEntity) {
        viewModelScope.launch {
            rawNotificationDao.delete(notification)
        }
    }

    fun markNotificationAsProcessed(notification: RawNotificationEntity) {
        viewModelScope.launch {
            rawNotificationDao.markAsProcessed(notification.id)
        }
    }

    fun markNotificationAsProcessedById(id: Long) {
        viewModelScope.launch {
            rawNotificationDao.markAsProcessed(id)
        }
    }

    fun discardAllNotifications() {
        viewModelScope.launch {
            rawNotificationDao.deleteAllUnprocessed()
        }
    }
}
