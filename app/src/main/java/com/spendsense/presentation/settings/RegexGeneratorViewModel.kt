package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.NotificationPatternDao
import com.spendsense.data.local.dao.ProviderAccountDao
import com.spendsense.data.local.dao.ProviderModelDao
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.dao.TransactionDao
import com.spendsense.data.local.dao.CategoryDao
import com.spendsense.data.local.dao.MerchantCategoryMappingDao
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.data.local.entity.ProviderAccountEntity
import com.spendsense.data.local.entity.ProviderModelEntity
import com.spendsense.data.local.entity.TransactionEntity
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity
import com.spendsense.data.remote.ChatCompletionApi
import com.spendsense.data.remote.DynamicBaseUrlInterceptor
import com.spendsense.data.remote.model.Message
import com.spendsense.data.remote.model.ChatCompletionRequest
import com.spendsense.data.service.NotificationProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegexGeneratorViewModel @Inject constructor(
    private val chatCompletionApi: ChatCompletionApi,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
    private val notificationPatternDao: NotificationPatternDao,
    private val accountDao: ProviderAccountDao,
    private val modelDao: ProviderModelDao,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val securePreferences: SecurePreferences,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val notificationProcessor: NotificationProcessor
) : ViewModel() {

    private val _state = MutableStateFlow(RegexGeneratorState())
    val state: StateFlow<RegexGeneratorState> = _state.asStateFlow()

    companion object {
        private const val REFRESH_THRESHOLD_MS = 24 * 60 * 60 * 1000L
    }

    init {
        _state.value = _state.value.copy(
            notificationTitle = securePreferences.getRegexTitle(),
            notificationText = securePreferences.getRegexText(),
            manualPattern = securePreferences.getRegexManualPattern(),
            currencyCode = securePreferences.getDefaultCurrency()
        )
        loadEnabledModels()
        loadProviderAccounts()
        loadWhitelistedApps()
    }

    private fun loadEnabledModels() {
        viewModelScope.launch {
            modelDao.getEnabledModelsFlow().collect { models ->
                val savedId = securePreferences.getSelectedProviderId()
                val savedModel = models.firstOrNull { it.id == savedId }
                val firstModel = savedModel ?: models.firstOrNull()
                _state.value = _state.value.copy(
                    enabledModels = models,
                    selectedModel = firstModel
                )
            }
        }
    }

    fun onProviderSelected(model: ProviderModelEntity) {
        _state.value = _state.value.copy(selectedModel = model)
        securePreferences.saveSelectedProviderId(model.id)
    }

    fun onTargetAppSelected(packageName: String) {
        _state.value = _state.value.copy(
            selectedAppPackage = packageName,
            errorMessage = null
        )
    }

    fun updateNotificationTitle(title: String) {
        _state.value = _state.value.copy(notificationTitle = title, errorMessage = null)
        securePreferences.saveRegexInput(title = title, text = _state.value.notificationText, manualPattern = _state.value.manualPattern)
    }

    fun updateNotificationText(text: String) {
        val s = _state.value
        _state.value = s.copy(notificationText = text, errorMessage = null)
        securePreferences.saveRegexInput(title = s.notificationTitle, text = text, manualPattern = s.manualPattern)
    }

    fun updateManualPattern(pattern: String) {
        val s = _state.value
        _state.value = s.copy(manualPattern = pattern, errorMessage = null)
        securePreferences.saveRegexInput(title = s.notificationTitle, text = s.notificationText, manualPattern = pattern)
    }

    fun updatePaymentSource(source: String) {
        _state.value = _state.value.copy(paymentSource = source, errorMessage = null)
    }

    fun updatePaymentSourceType(type: String) {
        _state.value = _state.value.copy(paymentSourceType = type, errorMessage = null)
    }

    fun toggleActive() { _state.value = _state.value.copy(isActive = !_state.value.isActive) }
    fun toggleIsTransaction() { _state.value = _state.value.copy(isTransaction = !_state.value.isTransaction) }
    fun updateCurrency(currencyCode: String) { _state.value = _state.value.copy(currencyCode = currencyCode) }

    fun clearInput() {
        val currentState = _state.value
        _state.value = RegexGeneratorState(
            providerAccounts = currentState.providerAccounts,
            enabledModels = currentState.enabledModels,
            selectedModel = currentState.selectedModel,
            availableApps = currentState.availableApps,
            currencyCode = currentState.currencyCode,
            editingPatternId = null
        )
        securePreferences.clearRegexInput()
    }

    fun loadStalePattern(stalePatternId: Long) {
        viewModelScope.launch {
            try {
                val pattern = notificationPatternDao.getById(stalePatternId)
                if (pattern != null) {
                    _state.value = _state.value.copy(
                        editingPatternId = stalePatternId,
                        selectedAppPackage = pattern.packageName,
                        notificationTitle = pattern.notificationTitle,
                        paymentSource = pattern.paymentSource,
                        paymentSourceType = pattern.paymentSourceType,
                        manualPattern = pattern.regex ?: "",
                        currencyCode = pattern.currencyCode,
                        isTransaction = pattern.isTransaction
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Error loading stale pattern: ${e.message}")
            }
        }
    }

    private fun loadWhitelistedApps() {
        viewModelScope.launch {
            val apps = whitelistedAppDao.getEnabledApps()
                .map { RegexTargetApp(packageName = it.packageName, appName = it.appName) }
                .sortedBy { it.appName.lowercase() }
            _state.value = _state.value.copy(availableApps = apps)
        }
    }

    private fun loadProviderAccounts() {
        viewModelScope.launch {
            accountDao.getAllFlow().collect { accounts ->
                _state.value = _state.value.copy(
                    providerAccounts = accounts
                )
            }
        }
    }

    fun testManualPattern() {
        val currentState = _state.value
        if (currentState.manualPattern.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please enter a regex pattern")
            return
        }
        if (currentState.notificationText.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please enter notification text to test against")
            return
        }
        testPattern(currentState.manualPattern, currentState.notificationText)
    }

    fun generateRegex() {
        val currentState = _state.value
        val notificationText = currentState.notificationText
        if (notificationText.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please enter notification text")
            return
        }

        val model = currentState.selectedModel
        if (model == null) {
            _state.value = currentState.copy(errorMessage = "Please select a model")
            return
        }
        
        _state.value = currentState.copy(
            isGenerating = true,
            errorMessage = null,
            generatedPattern = null
        )

        viewModelScope.launch {
            val account = accountDao.getById(model.providerAccountId)
            if (account == null) {
                _state.value = _state.value.copy(isGenerating = false, errorMessage = "Provider account not found for this model")
                return@launch
            }

            val apiKey = resolveProviderApiKey(account)
            val isFreeProvider = account.baseUrl.contains("opencode", ignoreCase = true)

            if (apiKey.isNullOrBlank() && !isFreeProvider) {
                _state.value = _state.value.copy(isGenerating = false, errorMessage = "API key not found for ${account.name}. Please add it in AI Providers settings.")
                return@launch
            }

            dynamicBaseUrlInterceptor.setBaseUrl(
                url = account.baseUrl,
                key = apiKey,
                isOpenRouter = account.name.contains("OpenRouter", ignoreCase = true),
                isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
            )


            try {
                val prompt = buildPrompt(currentState.notificationTitle, notificationText)
                val request = ChatCompletionRequest(
                    model = model.modelId,
                    messages = listOf(Message(role = "user", content = prompt))
                )

                val response = chatCompletionApi.generateCompletion(request = request)
                val generatedText = response.choices.firstOrNull()?.message?.content
                if (generatedText != null) {
                    val result = parseAiResponse(generatedText)
                    if (result != null) {
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            generatedPattern = result.regex,
                            manualPattern = "",
                            isTransaction = result.isTransaction,
                            currencyCode = result.currency ?: _state.value.currencyCode,
                            paymentSource = result.paymentSource ?: _state.value.paymentSource
                        )
                        if (result.regex != null) {
                            testPattern(result.regex, notificationText)
                        }
                    } else {
                        _state.value = _state.value.copy(isGenerating = false, errorMessage = "Could not parse AI response")
                    }
                } else {
                    _state.value = _state.value.copy(isGenerating = false, errorMessage = "No response from AI")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isGenerating = false, errorMessage = "Error: ${e.message}")
            }
        }
    }

    private fun buildPrompt(title: String, text: String): String {
        return """
You are a transaction detection expert. Analyze this notification.

Notification Title: "$title"
Notification Text: "$text"

Requirements:
1. Determine if this is a financial transaction notification (true) or not (false).
2. If it IS a transaction:
   - Identify the currency of the transaction (e.g. "VND", "USD", etc.).
   - Identify the thousands separator character used in the transaction amount (e.g. "," or "."). If none is used, set to null.
   - Extract the account/payment source identifier (e.g. "03xxx589" or "X4685") from the notification.
   - Generate ONE Kotlin-compatible regex pattern that matches the structural format of the notification.
     Guidelines for the regex pattern:
     - It MUST match the entire notification structure, preserving constant/static text, labels, and delimiters (e.g., "TK", "GD:", "|", "SD:", "DEN:", "ND:") as literals.
     - Escape regex special characters in the static text (e.g., escape "|" as "\|", parentheses as "\(", etc.).
     - Replace only the dynamic/variable values with specific regex patterns:
       - The transaction amount must be captured using the named group: (?<amount>[0-9,.]+VND) or (?<amount>[0-9,.]+USD) or similar. The currency letters/suffixes MUST be matched inside the capture group 'amount' if present in the text (it is required by the parser to extract and clean correctly).
       - The merchant/payee name must be captured using: (?<merchant>[^|]+) (or another appropriate non-greedy pattern that does not cross segment borders).
       - Match timestamps (e.g., "06/06/26 09:19") with specific date/time patterns (e.g., "\d{2}/\d{2}/\d{2}\s+\d{2}:\d{2}").
       - Match account identifiers (e.g., "03xxx589") with specific patterns (e.g., "\d+xxx\d+" or "\w+").
     - Do NOT use generic/lazy wildcards like `.*?` to skip entire fields or structure segments. Every structural segment of the notification template must be explicitly represented so that notifications with different structures fail to match.
3. If it is NOT a transaction, set regex, thousandsSeparator, currency, and paymentSource to null.

Return ONLY valid JSON with no markdown formatting:
{
  "isTransaction": true,
  "regex": "pattern",
  "thousandsSeparator": "," or "." or null,
  "currency": "VND" or "USD" or null,
  "paymentSource": "03xxx589" or "X4685" or null
}
        """.trimIndent()
    }

    private data class AiResponse(
        val isTransaction: Boolean,
        val regex: String?,
        val thousandsSeparator: String?,
        val currency: String?,
        val paymentSource: String?
    )

    private fun parseAiResponse(response: String): AiResponse? {
        try {
            val trimmed = response.trim()
            val firstBrace = trimmed.indexOf('{')
            val lastBrace = trimmed.lastIndexOf('}')
            val jsonStr = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                trimmed.substring(firstBrace, lastBrace + 1)
            } else {
                trimmed
            }
            val json = org.json.JSONObject(jsonStr.trim())
            val isTransaction = json.optBoolean("isTransaction", true)
            val regex = json.optString("regex", "").takeIf { it.isNotBlank() && it != "null" }
            val thousandsSeparator = json.optString("thousandsSeparator", "").takeIf { it.isNotBlank() && it != "null" }
            val currency = json.optString("currency", "").takeIf { it.isNotBlank() && it != "null" }
            val paymentSource = json.optString("paymentSource", "").takeIf { it.isNotBlank() && it != "null" }
            return AiResponse(isTransaction, regex, thousandsSeparator, currency, paymentSource)
        } catch (e: Exception) {
            val regex = extractRegexPatternLegacy(response)
            return AiResponse(isTransaction = regex != null, regex = regex, thousandsSeparator = null, currency = null, paymentSource = null)
        }
    }

    private fun extractRegexPatternLegacy(response: String): String? {
        val lines = response.trim().lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("(?<amount>") && trimmed.contains("(?<merchant>")) return trimmed
        }
        val trimmed = response.trim()
        if (trimmed.contains("(?<amount>") && trimmed.contains("(?<merchant>")) return trimmed
        return null
    }

    private fun testPattern(pattern: String, text: String) {
        try {
            val regex = Regex(pattern)
            val matchResult = regex.find(text)
            if (matchResult != null) {
                val rawAmount = matchResult.groups["amount"]?.value
                val merchant = matchResult.groups["merchant"]?.value
                val amount = rawAmount?.let { parseAmount(it) }?.toString() ?: rawAmount
                _state.value = _state.value.copy(extractedAmount = amount, extractedMerchant = merchant, errorMessage = null)
            } else {
                _state.value = _state.value.copy(extractedAmount = null, extractedMerchant = null, errorMessage = "Pattern does not match the notification text")
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(errorMessage = "Invalid regex pattern: ${e.message}")
        }
    }

    private fun parseAmount(amountStr: String): Double {
        val cleaned = amountStr.replace(Regex("[^0-9.,]"), "")
        if (cleaned.isEmpty()) return 0.0

        val hasComma = cleaned.contains(',')
        val hasDot = cleaned.contains('.')

        val normalized = if (hasComma && hasDot) {
            val commaIndex = cleaned.lastIndexOf(',')
            val dotIndex = cleaned.lastIndexOf('.')
            if (commaIndex > dotIndex) {
                cleaned.replace(".", "").replace(",", ".")
            } else {
                cleaned.replace(",", "")
            }
        } else if (hasComma) {
            val lastCommaOffset = cleaned.length - 1 - cleaned.lastIndexOf(',')
            if (lastCommaOffset == 3) {
                cleaned.replace(",", "")
            } else {
                cleaned.replace(",", ".")
            }
        } else if (hasDot) {
            val lastDotOffset = cleaned.length - 1 - cleaned.lastIndexOf('.')
            if (lastDotOffset == 3) {
                cleaned.replace(".", "")
            } else {
                cleaned
            }
        } else {
            cleaned
        }

        return try {
            normalized.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    fun savePattern() {
        val currentState = _state.value
        val patternToSave = if (currentState.manualPattern.isNotBlank()) currentState.manualPattern else currentState.generatedPattern

        if (currentState.availableApps.isEmpty()) {
            _state.value = currentState.copy(errorMessage = "No whitelisted apps found. Please whitelist at least one app first.")
            return
        }
        if (currentState.selectedAppPackage.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please select an app to apply this pattern")
            return
        }
        if (currentState.notificationTitle.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please enter a notification title — patterns are keyed by (app × title)")
            return
        }
        if (currentState.isTransaction && currentState.paymentSource.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please specify a Payment Source (e.g. card/account identifier like x1234)")
            return
        }

        _state.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val pattern = NotificationPatternEntity(
                    id = currentState.editingPatternId ?: 0L,
                    packageName = currentState.selectedAppPackage,
                    notificationTitle = currentState.notificationTitle,
                    paymentSource = currentState.paymentSource.trim(),
                    paymentSourceType = currentState.paymentSourceType.trim(),
                    regex = patternToSave,
                    currencyCode = currentState.currencyCode,
                    isTransaction = currentState.isTransaction
                )
                notificationPatternDao.upsert(pattern)

                val appName = currentState.availableApps
                    .firstOrNull { it.packageName == currentState.selectedAppPackage }
                    ?.appName ?: "App"

                // Reprocess the pending inbox for this newly saved pattern
                val recovered = notificationProcessor.reprocessInboxForPattern(pattern, appName = appName)

                securePreferences.setDefaultCurrency(currentState.currencyCode)
                securePreferences.clearRegexInput()

                val successMsg = if (recovered > 0) {
                    "Pattern saved successfully. $recovered past transactions recovered from pending inbox."
                } else {
                    "Pattern saved successfully!"
                }

                _state.value = _state.value.copy(
                    isSaving = false,
                    successMessage = successMsg,
                    notificationTitle = "",
                    notificationText = "",
                    manualPattern = "",
                    paymentSource = ""
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(isSaving = false, errorMessage = "Error saving pattern: ${e.message}")
            }
        }
    }

    fun setIsFromInbox(fromInbox: Boolean) {
        _state.value = _state.value.copy(isFromInbox = fromInbox)
    }

    fun updateTransactionTimestamp(timestamp: Long) {
        _state.value = _state.value.copy(transactionTimestamp = timestamp)
    }

    fun savePatternAndTransaction(editedMerchant: String? = null, editedAmount: String? = null) {
        val currentState = _state.value
        val patternToSave = if (currentState.manualPattern.isNotBlank()) currentState.manualPattern else currentState.generatedPattern

        if (currentState.availableApps.isEmpty()) {
            _state.value = currentState.copy(errorMessage = "No whitelisted apps found. Please whitelist at least one app first.")
            return
        }
        if (currentState.selectedAppPackage.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please select an app to apply this pattern")
            return
        }
        if (currentState.notificationTitle.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please enter a notification title — patterns are keyed by (app × title)")
            return
        }
        if (currentState.isTransaction && currentState.paymentSource.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please specify a Payment Source (e.g. card/account identifier like x1234)")
            return
        }

        val finalAmountStr = editedAmount ?: currentState.extractedAmount
        if (finalAmountStr.isNullOrBlank()) {
            _state.value = currentState.copy(errorMessage = "No transaction amount extracted to save")
            return
        }

        val finalMerchant = (editedMerchant ?: currentState.extractedMerchant ?: "Unknown").trim()

        _state.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                // 1. Save pattern
                val pattern = NotificationPatternEntity(
                    id = currentState.editingPatternId ?: 0L,
                    packageName = currentState.selectedAppPackage,
                    notificationTitle = currentState.notificationTitle,
                    paymentSource = currentState.paymentSource.trim(),
                    paymentSourceType = currentState.paymentSourceType.trim(),
                    regex = patternToSave,
                    currencyCode = currentState.currencyCode,
                    isTransaction = currentState.isTransaction
                )
                notificationPatternDao.upsert(pattern)

                // 2. Determine Category
                val mapping = merchantCategoryMappingDao.getByMerchant(finalMerchant.lowercase())
                var catId = mapping?.categoryId

                if (catId == null) {
                    val allCategories = categoryDao.getAll()
                    val otherCategory = allCategories.firstOrNull { it.name.equals("Other", ignoreCase = true) }
                    catId = otherCategory?.id ?: allCategories.firstOrNull()?.id ?: 1L
                }

                val appName = currentState.availableApps
                    .firstOrNull { it.packageName == currentState.selectedAppPackage }
                    ?.appName ?: "Notification"

                // 3. Save Transaction
                transactionDao.insert(
                    TransactionEntity(
                        amount = parseAmount(finalAmountStr),
                        currencyCode = currentState.currencyCode,
                        merchant = finalMerchant,
                        categoryId = catId,
                        timestamp = currentState.transactionTimestamp,
                        sourcePackageName = currentState.selectedAppPackage,
                        sourceAppName = appName,
                        notes = "Extracted during notification inbox pattern setup",
                        paymentSource = currentState.paymentSource.trim(),
                        paymentSourceType = currentState.paymentSourceType.trim()
                    )
                )

                // 4. Update Category mapping if new
                if (mapping == null) {
                    merchantCategoryMappingDao.upsert(
                        MerchantCategoryMappingEntity(
                            merchant = finalMerchant.lowercase(),
                            categoryId = catId
                        )
                    )
                }

                // 5. Reprocess the pending inbox for this newly saved pattern
                val recovered = notificationProcessor.reprocessInboxForPattern(pattern, appName = appName)

                securePreferences.setDefaultCurrency(currentState.currencyCode)
                securePreferences.clearRegexInput()

                val successMsg = if (recovered > 0) {
                    "Pattern and transaction saved successfully. $recovered past transactions recovered from pending inbox."
                } else {
                    "Pattern and transaction saved successfully!"
                }

                _state.value = _state.value.copy(
                    isSaving = false,
                    successMessage = successMsg,
                    notificationTitle = "",
                    notificationText = "",
                    manualPattern = "",
                    paymentSource = ""
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(isSaving = false, errorMessage = "Error: ${e.message}")
            }
        }
    }

    private fun resolveProviderApiKey(account: ProviderAccountEntity): String? {
        val key = securePreferences.getApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl))
        return key
    }
}
