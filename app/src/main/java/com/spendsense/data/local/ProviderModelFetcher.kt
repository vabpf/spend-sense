package com.spendsense.data.local

import com.spendsense.data.local.entity.ProviderAccountEntity
import com.spendsense.data.local.entity.ProviderModelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderModelFetcher @Inject constructor() {

    suspend fun fetchModels(account: ProviderAccountEntity, apiKey: String?): List<ProviderModelEntity> {
        return withContext(Dispatchers.IO) {
            when {
                account.baseUrl.contains("opencode", ignoreCase = true) -> fetchOpenCodeModels(account)
                account.baseUrl.contains("generativelanguage", ignoreCase = true) -> fetchGeminiModels(account, apiKey)
                account.baseUrl.contains("nvidia", ignoreCase = true) -> fetchNvidiaModels(account)
                account.baseUrl.contains("openrouter", ignoreCase = true) -> fetchOpenRouterModels(account)
                account.baseUrl.contains("ollama", ignoreCase = true) -> fetchOllamaModels(account)
                else -> fetchGenericModels(account)
            }
        }
    }

    private fun parseModels(account: ProviderAccountEntity, models: List<Pair<String, String?>>): List<ProviderModelEntity> {
        val now = System.currentTimeMillis()
        return models.map { (modelId, displayName) ->
            ProviderModelEntity(
                providerAccountId = account.id,
                modelId = modelId,
                displayName = displayName,
                lastRefreshedAt = now
            )
        }
    }

    private fun fetchJson(url: String, apiKey: String? = null): JSONObject? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")
            if (apiKey != null) {
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().readText()
            JSONObject(body)
        } catch (_: Exception) { null }
    }

    private fun fetchJsonArray(url: String): JSONArray? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().readText()
            JSONArray(body)
        } catch (_: Exception) { null }
    }

    private fun fetchOpenCodeModels(account: ProviderAccountEntity): List<ProviderModelEntity> {
        val json = fetchJson("${account.baseUrl}/models") ?: return emptyList()
        val modelsArray = json.optJSONArray("data") ?: json.optJSONArray("models") ?: return emptyList()
        val models = mutableListOf<Pair<String, String?>>()
        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val modelId = obj.optString("id")
            if (modelId.endsWith("-free")) {
                models.add(modelId to (obj.optString("name").takeIf { it.isNotBlank() } ?: modelId))
            }
        }
        return parseModels(account, models)
    }

    private fun fetchGeminiModels(account: ProviderAccountEntity, apiKey: String?): List<ProviderModelEntity> {
        if (apiKey.isNullOrBlank()) return emptyList()
        val json = fetchJson("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            ?: return emptyList()
        val modelsArray = json.optJSONArray("models") ?: return emptyList()
        val models = mutableListOf<Pair<String, String?>>()
        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val methods = obj.optJSONArray("supportedGenerationMethods") ?: continue
            val hasGenerateContent = (0 until methods.length()).any { j ->
                methods.optString(j) == "generateContent"
            }
            if (!hasGenerateContent) continue
            val modelId = obj.optString("name").removePrefix("models/")
            val displayName = obj.optString("displayName").takeIf { it.isNotBlank() } ?: modelId
            models.add(modelId to displayName)
        }
        return parseModels(account, models)
    }

    private fun fetchNvidiaModels(account: ProviderAccountEntity): List<ProviderModelEntity> {
        val json = fetchJson("${account.baseUrl}/models") ?: return emptyList()
        val modelsArray = json.optJSONArray("data") ?: return emptyList()
        val models = mutableListOf<Pair<String, String?>>()
        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val modelId = obj.optString("id")
            if (modelId.isNotBlank()) {
                models.add(modelId to (obj.optString("name").takeIf { it.isNotBlank() } ?: modelId))
            }
        }
        return parseModels(account, models)
    }

    private fun fetchOpenRouterModels(account: ProviderAccountEntity): List<ProviderModelEntity> {
        val json = fetchJson("${account.baseUrl}/models") ?: return emptyList()
        val modelsArray = json.optJSONArray("data") ?: return emptyList()
        val models = mutableListOf<Pair<String, String?>>()
        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val pricing = obj.optJSONObject("pricing") ?: continue
            val prompt = pricing.optString("prompt", "0")
            val completion = pricing.optString("completion", "0")
            if (prompt == "0" && completion == "0") {
                val modelId = obj.optString("id")
                if (modelId.isNotBlank()) {
                    models.add(modelId to (obj.optString("name").takeIf { it.isNotBlank() } ?: modelId))
                }
            }
        }
        return parseModels(account, models)
    }

    private fun fetchOllamaModels(account: ProviderAccountEntity): List<ProviderModelEntity> {
        val json = fetchJson("https://ollama.com/api/tags") ?: return emptyList()
        val modelsArray = json.optJSONArray("models") ?: return emptyList()
        val models = mutableListOf<Pair<String, String?>>()
        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val modelId = obj.optString("name")
            if (modelId.isNotBlank()) {
                models.add(modelId to (obj.optString("name").takeIf { it.isNotBlank() } ?: modelId))
            }
        }
        return parseModels(account, models)
    }

    private fun fetchGenericModels(account: ProviderAccountEntity): List<ProviderModelEntity> {
        val json = fetchJson("${account.baseUrl}/models") ?: return emptyList()
        val modelsArray = json.optJSONArray("data") ?: json.optJSONArray("models") ?: return emptyList()
        val models = mutableListOf<Pair<String, String?>>()
        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val modelId = obj.optString("id")
            if (modelId.isNotBlank()) {
                models.add(modelId to (obj.optString("name").takeIf { it.isNotBlank() } ?: modelId))
            }
        }
        return parseModels(account, models)
    }
}
