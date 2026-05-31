package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_patterns",
    indices = [Index(value = ["packageName", "notificationTitle"])]
)
data class NotificationPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val notificationTitle: String,
    val regex: String?,
    val currencyCode: String = "USD",
    val isTransaction: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMatchedAt: Long? = null,
    val matchCount: Int = 0
)
