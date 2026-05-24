package com.spendsense.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val cacheKey: String,
    val fromCurrency: String,
    val toCurrency: String,
    val date: String,
    val rate: Double
)
