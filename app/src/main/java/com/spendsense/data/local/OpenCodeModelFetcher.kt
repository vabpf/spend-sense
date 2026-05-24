package com.spendsense.data.local

import com.spendsense.data.local.dao.AiProviderDao
import com.spendsense.data.local.entity.AiProviderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OpenCodeModelFetcher {
    private const val MODELS_URL = "${AiProviderPresets.OPENCODE_BASE_URL}/models"

    suspend fun fetchAndSeed(aiProviderDao: AiProviderDao) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(MODELS_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000

                if (conn.responseCode != 200) return@withContext

                val body = conn.inputStream.bufferedReader().readText()
                val response = JSONObject(body)
                val modelsArray = response.optJSONArray("data")
                    ?: response.optJSONArray("models")
                    ?: return@withContext

                val existing = aiProviderDao.getAllProviders()
                    .filter { it.baseUrl.contains("opencode", ignoreCase = true) }
                    .associateBy { it.defaultModel }

                for (i in 0 until modelsArray.length()) {
                    val modelObj = modelsArray.getJSONObject(i)
                    val modelId = modelObj.optString("id")
                    if (modelId.endsWith("-free") && modelId !in existing) {
                        aiProviderDao.insert(
                            AiProviderEntity(
                                name = "OpenCode",
                                baseUrl = AiProviderPresets.OPENCODE_BASE_URL,
                                defaultModel = modelId,
                                jobType = "REGEX_GEN",
                                isPreset = true
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
