package com.spendsense.domain.model

data class ReviewTransactionData(
    val amount: Double,
    val merchant: String,
    val currencyCode: String,
    val sourcePackageName: String,
    val sourceAppName: String,
    val rawNotificationId: Long,
    val suggestedCategoryId: Long? = null
)
