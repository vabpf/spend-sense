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
import com.spendsense.data.remote.model.ContentPartImageUrl
import com.spendsense.data.remote.model.ContentPartText
import com.spendsense.data.remote.model.ImageUrlData
import com.spendsense.data.remote.model.Message
import com.spendsense.presentation.settings.buildProviderGroupKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTransactionResult(
    val amount: Double?,
    val currency: String?,
    val merchant: String?,
    val timestamp: Long?,
    val paymentSource: String?,
    val paymentSourceType: String?,
    val categoryName: String?,
    val notes: String?,
    val missingFields: List<String> = emptyList()
)

interface AiTransactionParser {
    suspend fun parseFromText(text: String): ParsedTransactionResult?
    suspend fun parseFromImage(base64ImageDataUrl: String): ParsedTransactionResult?
}

@Singleton
class AiTransactionParserImpl @Inject constructor(
    private val chatCompletionApi: ChatCompletionApi,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
    private val accountDao: ProviderAccountDao,
    private val modelDao: ProviderModelDao,
    private val securePreferences: SecurePreferences,
    private val categoryDao: CategoryDao
) : AiTransactionParser {

    private val TAG = "AiTransactionParser"

    override suspend fun parseFromText(text: String): ParsedTransactionResult? = withContext(Dispatchers.IO) {
        val resolved = resolveActiveProviderAndModel() ?: run {
            Log.w(TAG, "No active AI provider or model configured")
            return@withContext null
        }
        val account = resolved.first
        val model = resolved.second
        val apiKey = resolved.third

        val categories = try {
            categoryDao.getAll().map { it.name }
        } catch (_: Exception) {
            emptyList()
        }

        val prompt = buildTextPrompt(text, categories)

        try {
            dynamicBaseUrlInterceptor.setBaseUrl(
                url = account.baseUrl,
                key = apiKey,
                isOpenRouter = account.name.contains("OpenRouter", ignoreCase = true) || account.baseUrl.contains("openrouter", ignoreCase = true),
                isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
            )

            val request = ChatCompletionRequest(
                model = model.modelId,
                messages = listOf(Message(role = "user", content = prompt))
            )

            val response = withTimeoutOrNull(20_000L) {
                chatCompletionApi.generateCompletion(request)
            } ?: run {
                Log.w(TAG, "AI text completion timed out (20s)")
                return@withContext null
            }

            val rawContent = response.choices.firstOrNull()?.message?.content
            if (rawContent.isNullOrBlank()) return@withContext null

            parseJsonResponse(rawContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transaction from text", e)
            null
        }
    }

    override suspend fun parseFromImage(base64ImageDataUrl: String): ParsedTransactionResult? = withContext(Dispatchers.IO) {
        val resolved = resolveActiveProviderAndModel() ?: run {
            Log.w(TAG, "No active AI provider or model configured")
            return@withContext null
        }
        val account = resolved.first
        val model = resolved.second
        val apiKey = resolved.third

        val categories = try {
            categoryDao.getAll().map { it.name }
        } catch (_: Exception) {
            emptyList()
        }

        val prompt = buildImagePrompt(categories)

        try {
            dynamicBaseUrlInterceptor.setBaseUrl(
                url = account.baseUrl,
                key = apiKey,
                isOpenRouter = account.name.contains("OpenRouter", ignoreCase = true) || account.baseUrl.contains("openrouter", ignoreCase = true),
                isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
            )

            val request = ChatCompletionRequest(
                model = model.modelId,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            ContentPartText(text = prompt),
                            ContentPartImageUrl(imageUrl = ImageUrlData(url = base64ImageDataUrl))
                        )
                    )
                )
            )

            val response = withTimeoutOrNull(30_000L) {
                chatCompletionApi.generateCompletion(request)
            } ?: run {
                Log.w(TAG, "AI vision completion timed out (30s)")
                return@withContext null
            }

            val rawContent = response.choices.firstOrNull()?.message?.content
            if (rawContent.isNullOrBlank()) return@withContext null

            parseJsonResponse(rawContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transaction from image", e)
            null
        }
    }

    private fun resolveActiveProviderAndModel(): Triple<ProviderAccountEntity, ProviderModelEntity, String>? {
        val selectedModel = try {
            val selectedId = securePreferences.selectedModelId
            if (selectedId > 0) {
                modelDao.getById(selectedId)
            } else {
                modelDao.getAll().firstOrNull()
            }
        } catch (_: Exception) {
            modelDao.getAll().firstOrNull()
        } ?: return null

        val account = accountDao.getById(selectedModel.providerAccountId) ?: return null
        val isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
        val apiKey = try {
            securePreferences.getApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl))
                ?: securePreferences.getApiKey(account.id)
        } catch (_: Exception) {
            securePreferences.getApiKey(account.id)
        }

        if (apiKey.isNullOrBlank() && !isOpenCode) {
            return null
        }

        return Triple(account, selectedModel, apiKey ?: "")
    }

    private fun buildTextPrompt(text: String, categories: List<String>): String {
        val catListStr = if (categories.isNotEmpty()) categories.joinToString(", ") else "Food & Dining, Shopping, Transportation, Entertainment, Bills & Utilities, Other"
        return """
You are an expert personal financial transaction analyzer.
The user quickly typed or pasted this transaction note:
"$text"

Available Expense Categories:
$catListStr

Allowed Payment Source Types:
Bank Account, Credit Card, Debit Card, Wallet, Cash, Manual

Extract transaction details into strict JSON:
{
  "is_transaction": true,
  "amount": 50000.0,
  "currency": "VND",
  "merchant": "Highlands Coffee",
  "date_time": null,
  "payment_source": "Momo",
  "payment_source_type": "Wallet",
  "category": "Food & Dining",
  "notes": "$text",
  "missing_fields": ["date_time"]
}

Rules:
1. "amount": Final charged or spent monetary number (e.g. 50000.0 or 14.50). Do not include currency symbols. Return null if none found.
2. "currency": ISO 4217 code (e.g. "VND", "USD", "EUR"). Return null if not indicated.
3. "merchant": Store, recipient, or seller name. Return null if unclear.
4. "date_time": ISO format (YYYY-MM-DDTHH:mm:ss) only if user mentioned a date/time (e.g. "yesterday", "last Friday"). If none mentioned, return null.
5. "payment_source": Payment method name or card digits (e.g. "Momo", "Visa 1234", "Cash"). Return null if not mentioned.
6. "payment_source_type": One of ["Bank Account", "Credit Card", "Debit Card", "Wallet", "Cash", "Manual"]. Return null if not mentioned.
7. "category": Pick the best matching category from the Available Expense Categories list.
8. "notes": The original text or line items.
9. "missing_fields": Array of field names that were not provided in the input (e.g. ["date_time", "payment_source"]).

Return ONLY the raw JSON object, without markdown blocks.
""".trimIndent()
    }

    private fun buildImagePrompt(categories: List<String>): String {
        val catListStr = if (categories.isNotEmpty()) categories.joinToString(", ") else "Food & Dining, Shopping, Transportation, Entertainment, Bills & Utilities, Other"
        return """
You are an expert financial receipt, invoice, bill, and payment screenshot parser.
Analyze this receipt image and extract transaction data into strict JSON:

Available Expense Categories:
$catListStr

Allowed Payment Source Types:
Bank Account, Credit Card, Debit Card, Wallet, Cash, Manual

Output JSON Schema:
{
  "is_transaction": true,
  "amount": 42.50,
  "currency": "USD",
  "merchant": "Target",
  "date_time": "2026-10-09T14:30:00",
  "payment_source": "Visa 4848",
  "payment_source_type": "Credit Card",
  "category": "Shopping",
  "notes": "Itemized: Milk, Bread, Coffee",
  "missing_fields": []
}

Rules:
1. "amount": The FINAL total amount paid/charged as a number. Ignore subtotal or tax lines. Return null if unreadable.
2. "currency": ISO 4217 code (USD, VND, EUR, JPY...). If not indicated or cannot be inferred, return null.
3. "merchant": Store, business, or brand name. If unreadable, return null.
4. "date_time": ISO-8601 string (YYYY-MM-DDTHH:mm:ss) if visible. If only date is present without time, use 12:00:00. If date is not visible or obscured, return null.
5. "payment_source": Card last 4 digits (e.g. "Visa 1234"), bank name, or "Cash". If not indicated, return null.
6. "payment_source_type": One of ["Bank Account", "Credit Card", "Debit Card", "Wallet", "Cash", "Manual"]. If not indicated, return null.
7. "category": Pick the single most suitable category strictly from the Available Expense Categories list.
8. "notes": Concise 1-line summary of purchased items or receipt reference.
9. "missing_fields": Array of fields that were missing or unreadable on the receipt (e.g. ["date_time", "payment_source"]).

Return ONLY the raw JSON object, without markdown blocks.
""".trimIndent()
    }

    private fun parseJsonResponse(rawContent: String): ParsedTransactionResult? {
        val cleanJson = cleanMarkdown(rawContent)
        return try {
            val json = JSONObject(cleanJson)
            val isTransaction = json.optBoolean("is_transaction", json.optBoolean("isTransaction", true))
            if (!isTransaction) return null

            val amount = if (json.has("amount") && !json.isNull("amount")) json.optDouble("amount") else null
            val currency = if (json.has("currency") && !json.isNull("currency")) {
                json.optString("currency").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val merchant = if (json.has("merchant") && !json.isNull("merchant")) {
                json.optString("merchant").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val paymentSource = if (json.has("payment_source") && !json.isNull("payment_source")) {
                json.optString("payment_source").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else if (json.has("paymentSource") && !json.isNull("paymentSource")) {
                json.optString("paymentSource").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val paymentSourceType = if (json.has("payment_source_type") && !json.isNull("payment_source_type")) {
                json.optString("payment_source_type").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else if (json.has("paymentSourceType") && !json.isNull("paymentSourceType")) {
                json.optString("paymentSourceType").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val category = if (json.has("category") && !json.isNull("category")) {
                json.optString("category").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else if (json.has("category_suggestion") && !json.isNull("category_suggestion")) {
                json.optString("category_suggestion").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val notes = if (json.has("notes") && !json.isNull("notes")) {
                json.optString("notes").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else if (json.has("items_summary") && !json.isNull("items_summary")) {
                json.optString("items_summary").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val dateTimeStr = if (json.has("date_time") && !json.isNull("date_time")) {
                json.optString("date_time").trim().takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            } else null

            val timestamp = dateTimeStr?.let { parseDateTimeToMillis(it) }

            val missingList = mutableListOf<String>()
            val jsonMissing = json.optJSONArray("missing_fields")
            if (jsonMissing != null) {
                for (i in 0 until jsonMissing.length()) {
                    val field = jsonMissing.optString(i).lowercase()
                    if (field.contains("date") || field.contains("time")) missingList.add("date")
                    if (field.contains("payment") || field.contains("source")) missingList.add("paymentSource")
                    if (field.contains("merchant")) missingList.add("merchant")
                    if (field.contains("currency")) missingList.add("currency")
                }
            }

            if (timestamp == null && !missingList.contains("date")) missingList.add("date")
            if (paymentSource.isNullOrBlank() && !missingList.contains("paymentSource")) missingList.add("paymentSource")
            if (merchant.isNullOrBlank() && !missingList.contains("merchant")) missingList.add("merchant")
            if (currency.isNullOrBlank() && !missingList.contains("currency")) missingList.add("currency")

            ParsedTransactionResult(
                amount = amount,
                currency = currency,
                merchant = merchant,
                timestamp = timestamp,
                paymentSource = paymentSource,
                paymentSourceType = paymentSourceType,
                categoryName = category,
                notes = notes,
                missingFields = missingList.distinct()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response: $cleanJson", e)
            null
        }
    }

    private fun cleanMarkdown(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json", ignoreCase = true)) {
            clean = clean.substring(7)
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - 3)
        }
        return clean.trim()
    }

    private fun parseDateTimeToMillis(dateTimeStr: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                val date = sdf.parse(dateTimeStr)
                if (date != null) {
                    return date.time
                }
            } catch (_: Exception) {
                // try next format
            }
        }
        return null
    }
}
