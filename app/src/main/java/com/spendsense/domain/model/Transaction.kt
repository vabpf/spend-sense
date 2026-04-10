package com.spendsense.domain.model

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val categoryId: Long,
    val timestamp: Long,
    val sourcePackageName: String,
    val sourceAppName: String,
    val notes: String? = null
)
