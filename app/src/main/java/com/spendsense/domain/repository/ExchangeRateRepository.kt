package com.spendsense.domain.repository

interface ExchangeRateRepository {
    suspend fun getRate(from: String, to: String, dateMillis: Long): Double?
}
