package com.spendsense.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.RawNotificationDao
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.ExchangeRateRepository
import com.spendsense.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val rawNotificationDao: RawNotificationDao,
    private val securePreferences: SecurePreferences,
    private val exchangeRateRepository: ExchangeRateRepository
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
            
            val todayTxns = mutableListOf<Transaction>()
            val yesterdayTxns = mutableListOf<Transaction>()
            
            for (txn in transactions) {
                tempCal.timeInMillis = txn.timestamp
                val isSameYear = tempCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                if (isSameYear && tempCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                    todayTxns.add(txn)
                } else if (tempCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) && tempCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
                    yesterdayTxns.add(txn)
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

            val allDeferred = transactions.map { txn ->
                async {
                    convertTransactionAmount(txn, currency)
                }
            }

            _todayConvertedTotal.value = todayDeferred.sumOf { it.await() }
            _yesterdayConvertedTotal.value = yesterdayDeferred.sumOf { it.await() }
            _convertedTotal.value = allDeferred.sumOf { it.await() }
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

    fun updateTransactions(updatedList: List<Transaction>) {
        viewModelScope.launch {
            updatedList.forEach { transactionRepository.updateTransaction(it) }
        }
    }

    fun addTransaction(
        amount: Double,
        currencyCode: String,
        merchant: String,
        categoryId: Long,
        paymentSource: String = "Manual",
        paymentSourceType: String = "Manual"
    ) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = amount,
                    currencyCode = currencyCode,
                    merchant = merchant,
                    categoryId = categoryId,
                    timestamp = System.currentTimeMillis(),
                    sourcePackageName = "manual",
                    sourceAppName = "Manual Add",
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
