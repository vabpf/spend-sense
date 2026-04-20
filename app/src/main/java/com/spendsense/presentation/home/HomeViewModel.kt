package com.spendsense.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.dao.RawNotificationDao
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val rawNotificationDao: RawNotificationDao
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _pendingNotifications = MutableStateFlow<List<RawNotificationEntity>>(emptyList())
    val pendingNotifications: StateFlow<List<RawNotificationEntity>> = _pendingNotifications.asStateFlow()

    init {
        loadTransactions()
        loadCategories()
        loadPendingNotifications()
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
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _categories.value = categories
            }
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

    fun addTransaction(amount: Double, merchant: String, categoryId: Long) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(
                Transaction(
                    amount = amount,
                    merchant = merchant,
                    categoryId = categoryId,
                    timestamp = System.currentTimeMillis(),
                    sourcePackageName = "manual",
                    sourceAppName = "Manual Add"
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
}
