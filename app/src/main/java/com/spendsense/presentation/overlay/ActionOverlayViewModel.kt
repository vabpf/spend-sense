package com.spendsense.presentation.overlay

import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ActionOverlayViewModel @Inject constructor(
    private val transactionRepository: com.spendsense.domain.repository.TransactionRepository,
    private val categoryRepository: com.spendsense.domain.repository.CategoryRepository,
    private val rawNotificationDao: com.spendsense.data.local.dao.RawNotificationDao
) {


    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(OverlayState())
    val state: StateFlow<OverlayState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    init {
        loadCategories()
    }

    fun initialize(amount: Double, merchant: String, packageName: String, appName: String, rawNotificationId: Long, currency: String = "USD") {
        _state.value = _state.value.copy(
            amount = amount.toString(),
            currencyCode = currency,
            merchant = merchant,
            sourcePackageName = packageName,
            sourceAppName = appName,
            rawNotificationId = rawNotificationId,
            isAmountValid = amount > 0
        )
    }


    private fun loadCategories() {
        scope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _categories.value = categories
            }
        }
    }

    fun updateAmount(amount: String) {
        _state.value = _state.value.copy(
            amount = amount,
            isAmountValid = amount.toDoubleOrNull()?.let { it > 0 } == true
        )
    }

    fun updateMerchant(merchant: String) {
        _state.value = _state.value.copy(merchant = merchant)
    }

    fun updateCurrency(currencyCode: String) {
        _state.value = _state.value.copy(currencyCode = currencyCode)
    }

    fun selectCategory(categoryId: Long) {
        _state.value = _state.value.copy(selectedCategoryId = categoryId)
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val currentState = _state.value
        
        // Validate
        val amount = currentState.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.value = currentState.copy(errorMessage = "Invalid amount")
            return
        }

        if (currentState.merchant.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Merchant name is required")
            return
        }

        if (currentState.selectedCategoryId == null) {
            _state.value = currentState.copy(errorMessage = "Please select a category")
            return
        }

        _state.value = currentState.copy(isSaving = true, errorMessage = null)

        scope.launch {
            try {
                val transaction = Transaction(
                    amount = amount,
                    currencyCode = currentState.currencyCode,
                    merchant = currentState.merchant,
                    categoryId = currentState.selectedCategoryId,
                    timestamp = System.currentTimeMillis(),
                    sourcePackageName = currentState.sourcePackageName,
                    sourceAppName = currentState.sourceAppName
                )

                transactionRepository.insertTransaction(transaction)

                // Mark raw notification as processed if it exists
                currentState.rawNotificationId?.let {
                    rawNotificationDao.markAsProcessed(it)
                }

                _state.value = currentState.copy(

                    isSaving = false,
                    showSuccess = true
                )

                // Delay to show success animation
                kotlinx.coroutines.delay(500)
                onSuccess()
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isSaving = false,
                    errorMessage = "Error saving transaction: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun dispose() {
        scope.cancel()
    }
}
