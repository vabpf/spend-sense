package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_patterns",
    indices = [Index(value = ["packageName", "notificationTitle", "paymentSource"], unique = false)]
)
data class NotificationPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val notificationTitle: String,
    val paymentSource: String = "",
    val paymentSourceType: String = "Credit Card",
    val regex: String?,
    val currencyCode: String = "USD",
    val isTransaction: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMatchedAt: Long? = null,
    val matchCount: Int = 0
)
