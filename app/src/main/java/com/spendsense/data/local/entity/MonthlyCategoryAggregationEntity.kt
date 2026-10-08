package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Materialized monthly category totals for instant donut chart rendering.
 */
@Entity(
    tableName = "monthly_category_aggregations",
    primaryKeys = ["year", "month", "categoryId"],
    indices = [
        Index(value = ["year", "month"]),
        Index(value = ["categoryId"])
    ]
)
data class MonthlyCategoryAggregationEntity(
    val year: Int,
    val month: Int,                     // 0-indexed (0 = Jan, 11 = Dec)
    val categoryId: Long,
    val totalAmount: Double,
    val transactionCount: Int
)
