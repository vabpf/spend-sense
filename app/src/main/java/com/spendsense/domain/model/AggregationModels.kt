package com.spendsense.domain.model

data class DailyAggregation(
    val dateKey: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val timestampDayStart: Long,
    val totalAmount: Double,
    val transactionCount: Int
)

data class MonthlyCategoryAggregation(
    val year: Int,
    val month: Int,
    val categoryId: Long,
    val totalAmount: Double,
    val transactionCount: Int
)

data class MonthlyPaymentAggregation(
    val year: Int,
    val month: Int,
    val paymentSource: String,
    val paymentSourceType: String,
    val totalAmount: Double,
    val transactionCount: Int
)
