package com.spendsense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendsense.data.local.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {

    @Query("SELECT * FROM exchange_rates WHERE cacheKey = :cacheKey")
    suspend fun getRate(cacheKey: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: ExchangeRateEntity)
}
