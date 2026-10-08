package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Materialized monthly payment source totals for instant payment method breakdown.
 */
@Entity(
    tableName = "monthly_payment_aggregations",
    primaryKeys = ["year", "month", "paymentSource", "paymentSourceType"],
    indices = [
        Index(value = ["year", "month"])
    ]
)
data class MonthlyPaymentAggregationEntity(
    val year: Int,
    val month: Int,                     // 0-indexed (0 = Jan, 11 = Dec)
    val paymentSource: String,
    val paymentSourceType: String,
    val totalAmount: Double,
    val transactionCount: Int
)
