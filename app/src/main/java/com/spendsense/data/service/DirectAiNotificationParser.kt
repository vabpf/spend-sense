package com.spendsense.data.service

import android.util.Log
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.CategoryDao
import com.spendsense.data.local.dao.ProviderAccountDao
import com.spendsense.data.local.dao.ProviderModelDao
import com.spendsense.data.local.entity.ProviderAccountEntity
import com.spendsense.data.local.entity.ProviderModelEntity
import com.spendsense.data.remote.ChatCompletionApi
import com.spendsense.data.remote.DynamicBaseUrlInterceptor
import com.spendsense.data.remote.model.ChatCompletionRequest
import com.spendsense.data.remote.model.Message
import com.spendsense.domain.model.NotificationRoutingMode
import com.spendsense.presentation.settings.buildProviderGroupKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class DirectAiParseResult(
    val isTransaction: Boolean,
    val amount: Double = 0.0,
    val currency: String = "USD",
    val merchant: String = "Unknown",
    val paymentSource: String = "",
    val categoryName: String? = null,
    val regex: String? = null
)

@Singleton
open class DirectAiNotificationParser(
    private val chatCompletionApi: ChatCompletionApi?,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor?,
    private val accountDao: ProviderAccountDao?,
    private val modelDao: ProviderModelDao?,
    private val securePreferences: SecurePreferences?,
    private val categoryDao: CategoryDao?
) {
    @Inject
    constructor(
        chatCompletionApi: ChatCompletionApi,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
        accountDao: ProviderAccountDao,
        modelDao: ProviderModelDao,
        securePreferences: SecurePreferences,
        categoryDao: CategoryDao
    ) : this(
        chatCompletionApi = chatCompletionApi as ChatCompletionApi?,
        dynamicBaseUrlInterceptor = dynamicBaseUrlInterceptor as DynamicBaseUrlInterceptor?,
        accountDao = accountDao as ProviderAccountDao?,
        modelDao = modelDao as ProviderModelDao?,
        securePreferences = securePreferences as SecurePreferences?,
        categoryDao = categoryDao as CategoryDao?
    )

    constructor() : this(null, null, null, null, null, null)

    private val TAG = "DirectAiParser"

    open suspend fun parseNotification(
        title: String?,
        text: String,
        mode: NotificationRoutingMode
    ): DirectAiParseResult? = withContext(Dispatchers.IO) {
        val resolved = resolveActiveProviderAndModel() ?: run {
            Log.d(TAG, "No configured AI provider or model available")
            return@withContext null
        }
        val account = resolved.first
        val model = resolved.second
        val apiKey = resolved.third

        val categories = try {
            categoryDao?.getAll()?.map { it.name } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val prompt = if (mode == NotificationRoutingMode.AI_ONLY) {
            buildAiOnlyPrompt(title, text, categories)
        } else {
            buildRegexAndAiPrompt(title, text, categories)
        }

        try {
            dynamicBaseUrlInterceptor?.setBaseUrl(
                url = account.baseUrl,
                key = apiKey,
                isOpenRouter = account.name.contains("OpenRouter", ignoreCase = true) || account.baseUrl.contains("openrouter", ignoreCase = true),
                isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
            )

            val request = ChatCompletionRequest(
                model = model.modelId,
                messages = listOf(Message(role = "user", content = prompt))
            )

            // 15 second timeout for background notification parsing
            val response = withTimeoutOrNull(15_000L) {
                chatCompletionApi?.generateCompletion(request)
            } ?: run {
                Log.w(TAG, "AI completion timed out (15s)")
                return@withContext null
            }

            val content = response.choices.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                Log.w(TAG, "Empty response from AI")
                return@withContext null
            }

            return@withContext parseAiResponse(content)
        } catch (e: Exception) {
            Log.e(TAG, "AI completion error: ${e.message}", e)
            return@withContext null
        }
    }

    private suspend fun resolveActiveProviderAndModel(): Triple<ProviderAccountEntity, ProviderModelEntity, String?>? {
        val preferredModelId = securePreferences?.getActiveAiModelId() ?: -1L
        var selectedModel: ProviderModelEntity? = null
        if (preferredModelId > 0) {
            selectedModel = modelDao?.getById(preferredModelId)?.takeIf { it.isEnabled }
        }
        if (selectedModel == null) {
            selectedModel = modelDao?.getEnabledModels()?.firstOrNull()
        }
        if (selectedModel == null) {
            return null
        }

        val account = accountDao?.getById(selectedModel.providerAccountId) ?: return null
        val isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
        val apiKey = try {
            securePreferences?.getApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl))
                ?: securePreferences?.getApiKey(account.id)
        } catch (_: Exception) {
            securePreferences?.getApiKey(account.id)
        }

        if (apiKey.isNullOrBlank() && !isOpenCode) {
            return null
        }

        return Triple(account, selectedModel, apiKey)
    }

    private fun buildAiOnlyPrompt(title: String?, text: String, categories: List<String>): String {
        val catListStr = if (categories.isNotEmpty()) categories.joinToString(", ") else "Food & Dining, Shopping, Transportation, Entertainment, Bills & Utilities, Other"
        return """
You are an expert personal financial transaction analyzer.
Analyze this incoming notification:

Notification Title: "${title ?: ""}"
Notification Text: "$text"

Available Expense Categories:
$catListStr

Requirements:
1. Determine if this notification describes an actual financial expenditure/payment/purchase/money-transfer (true) or non-financial alert/OTP/marketing/spam/balance-inquiry (false).
2. If true:
   - Extract the numeric amount (e.g. "50000" or "14.50"). Do not include currency symbols in the amount.
   - Extract the currency code (e.g. "VND", "USD", "EUR"). Default to "USD" if unspecified.
   - Extract the merchant / recipient name (e.g. "GrabFood", "Target", "Amazon").
   - Extract the account/card identifier (e.g. "x1234", "03xxx589") or empty string if none.
   - Select the MOST SUITABLE category strictly from the Available Expense Categories list above.
3. If false:
   - Set isTransaction to false.

Return ONLY a single valid JSON object (no markdown, no backticks, no comments):
{
  "isTransaction": true,
  "amount": 50000.0,
  "currency": "VND",
  "merchant": "GrabFood",
  "paymentSource": "03xxx589",
  "category": "Food & Dining"
}
""".trimIndent()
    }

    private fun buildRegexAndAiPrompt(title: String?, text: String, categories: List<String>): String {
        val catListStr = if (categories.isNotEmpty()) categories.joinToString(", ") else "Food & Dining, Shopping, Transportation, Entertainment, Bills & Utilities, Other"
        return """
You are an expert personal financial transaction analyzer and regex engineer.
Analyze this incoming notification:

Notification Title: "${title ?: ""}"
Notification Text: "$text"

Available Expense Categories:
$catListStr

Requirements:
1. Determine if this notification describes a financial expenditure/payment/purchase/money-transfer (true) or non-financial alert/OTP/marketing/balance-inquiry (false).
2. If true:
   - Extract the numeric amount (e.g. 50000.0 or 14.50).
   - Extract the currency code (e.g. "VND", "USD").
   - Extract the merchant / recipient name.
   - Extract the account / card identifier or empty string.
   - Select the best category strictly from the Available Expense Categories list.
   - Generate a resilient Kotlin-compatible regular expression matching this notification template.
     - The regex MUST contain named group (?<amount>...) and named group (?<merchant>...).
     - Escape special characters like |, (, ), [, ], etc.
     - Capture dynamic fields with appropriate token patterns.
3. If false:
   - Set isTransaction to false.

Return ONLY a single valid JSON object (no markdown, no backticks, no comments):
{
  "isTransaction": true,
  "amount": 50000.0,
  "currency": "VND",
  "merchant": "GrabFood",
  "paymentSource": "03xxx589",
  "category": "Food & Dining",
  "regex": "pattern"
}
""".trimIndent()
    }

    private fun parseAiResponse(response: String): DirectAiParseResult? {
        try {
            val trimmed = response.trim()
            val firstBrace = trimmed.indexOf('{')
            val lastBrace = trimmed.lastIndexOf('}')
            val jsonStr = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                trimmed.substring(firstBrace, lastBrace + 1)
            } else {
                trimmed
            }
            val json = JSONObject(jsonStr)
            val isTransaction = json.optBoolean("isTransaction", false)
            if (!isTransaction) {
                return DirectAiParseResult(isTransaction = false)
            }

            val rawAmount = json.opt("amount")
            val amount = when (rawAmount) {
                is Number -> rawAmount.toDouble()
                is String -> parseDoubleSafely(rawAmount)
                else -> 0.0
            }
            if (amount <= 0.0) {
                return DirectAiParseResult(isTransaction = false)
            }

            val currency = json.optString("currency", "USD").takeIf { it.isNotBlank() && it != "null" } ?: "USD"
            val merchant = json.optString("merchant", "Unknown").takeIf { it.isNotBlank() && it != "null" } ?: "Unknown"
            val paymentSource = json.optString("paymentSource", "").takeIf { it != "null" } ?: ""
            val category = json.optString("category", null)?.takeIf { it.isNotBlank() && it != "null" }
            val regex = json.optString("regex", null)?.takeIf { it.isNotBlank() && it != "null" }

            return DirectAiParseResult(
                isTransaction = true,
                amount = amount,
                currency = currency,
                merchant = merchant,
                paymentSource = paymentSource,
                categoryName = category,
                regex = regex
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI JSON response: $response", e)
            return null
        }
    }

    private fun parseDoubleSafely(str: String): Double {
        val clean = str.replace(Regex("[^0-9.,]"), "")
        if (clean.isEmpty()) return 0.0
        return clean.replace(",", "").toDoubleOrNull()
            ?: clean.replace(".", "").toDoubleOrNull()
            ?: 0.0
    }
}
