package com.spendsense.data.repository

import com.spendsense.data.local.SecurePreferences
import com.spendsense.domain.model.CreditCardConfig
import com.spendsense.domain.repository.CreditCardConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardConfigRepositoryImpl @Inject constructor(
    private val securePreferences: SecurePreferences
) : CreditCardConfigRepository {

    private val _configsFlow = MutableStateFlow<List<CreditCardConfig>>(emptyList())

    init {
        _configsFlow.value = loadConfigsFromPrefs()
    }

    override fun getAllConfigs(): Flow<List<CreditCardConfig>> = _configsFlow.asStateFlow()

    override suspend fun getConfigForCard(cardName: String): CreditCardConfig? {
        return _configsFlow.value.find { it.cardName.trim().equals(cardName.trim(), ignoreCase = true) }
    }

    override suspend fun saveConfig(config: CreditCardConfig) {
        val current = _configsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.cardName.trim().equals(config.cardName.trim(), ignoreCase = true) }
        if (index >= 0) {
            current[index] = config
        } else {
            current.add(config)
        }
        persistConfigs(current)
    }

    override suspend fun deleteConfig(cardName: String) {
        val current = _configsFlow.value.filterNot { it.cardName.trim().equals(cardName.trim(), ignoreCase = true) }
        persistConfigs(current)
    }

    private fun loadConfigsFromPrefs(): List<CreditCardConfig> {
        val jsonStr = securePreferences.getCreditCardConfigsJson() ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CreditCardConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CreditCardConfig(
                        cardName = obj.getString("cardName"),
                        statementClosingDay = obj.getInt("statementClosingDay"),
                        paymentDueDay = if (obj.has("paymentDueDay") && !obj.isNull("paymentDueDay")) obj.getInt("paymentDueDay") else null,
                        creditLimit = if (obj.has("creditLimit") && !obj.isNull("creditLimit")) obj.getDouble("creditLimit") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistConfigs(list: List<CreditCardConfig>) {
        _configsFlow.value = list
        try {
            val array = JSONArray()
            for (cfg in list) {
                val obj = JSONObject().apply {
                    put("cardName", cfg.cardName)
                    put("statementClosingDay", cfg.statementClosingDay)
                    if (cfg.paymentDueDay != null) put("paymentDueDay", cfg.paymentDueDay)
                    if (cfg.creditLimit != null) put("creditLimit", cfg.creditLimit)
                }
                array.put(obj)
            }
            securePreferences.saveCreditCardConfigsJson(array.toString())
        } catch (e: Exception) {
            // Log/ignore
        }
    }
}
