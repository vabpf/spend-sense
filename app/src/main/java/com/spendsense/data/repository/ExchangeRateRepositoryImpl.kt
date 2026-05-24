package com.spendsense.data.repository

import com.spendsense.data.local.dao.ExchangeRateDao
import com.spendsense.data.local.entity.ExchangeRateEntity
import com.spendsense.data.remote.FrankfurterApi
import com.spendsense.domain.repository.ExchangeRateRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: FrankfurterApi,
    private val dao: ExchangeRateDao
) : ExchangeRateRepository {

    override suspend fun getRate(from: String, to: String, dateMillis: Long): Double? {
        if (from == to) return 1.0

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(dateMillis))
        val cacheKey = "$from->$to@$dateStr"

        dao.getRate(cacheKey)?.let { return it.rate }

        return try {
            val response = api.getRate(base = from, quote = to, date = dateStr)
            dao.upsertRate(ExchangeRateEntity(cacheKey, from, to, dateStr, response.rate))
            response.rate
        } catch (e: Exception) {
            null
        }
    }
}
