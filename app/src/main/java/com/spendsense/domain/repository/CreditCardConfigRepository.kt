package com.spendsense.domain.repository

import com.spendsense.domain.model.CreditCardConfig
import kotlinx.coroutines.flow.Flow

interface CreditCardConfigRepository {
    fun getAllConfigs(): Flow<List<CreditCardConfig>>
    suspend fun getConfigForCard(cardName: String): CreditCardConfig?
    suspend fun saveConfig(config: CreditCardConfig)
    suspend fun deleteConfig(cardName: String)
}
