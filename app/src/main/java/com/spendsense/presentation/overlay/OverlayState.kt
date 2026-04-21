package com.spendsense.presentation.overlay

data class OverlayState(
    val amount: String = "",
    val currencyCode: String = "USD",
    val merchant: String = "",
    val selectedCategoryId: Long? = null,
    val sourcePackageName: String = "",
    val sourceAppName: String = "",
    val rawNotificationId: Long? = null,
    val isAmountValid: Boolean = false,

    val isSaving: Boolean = false,
    val showSuccess: Boolean = false,
    val errorMessage: String? = null
)
