package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Materialized daily spending totals for O(1) indexed lookups in charts and home screen.
 */
@Entity(
    tableName = "daily_spending_aggregations",
    indices = [
        Index(value = ["year", "month"]),
        Index(value = ["timestampDayStart"])
    ]
)
data class DailySpendingAggregationEntity(
    @PrimaryKey
    val dateKey: String,                // Format: "YYYY-MM-DD"
    val year: Int,
    val month: Int,                     // 0-indexed (0 = Jan, 11 = Dec)
    val day: Int,
    val timestampDayStart: Long,        // Midnight epoch millis
    val totalAmount: Double,
    val transactionCount: Int
)
