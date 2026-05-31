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
            currencyCode = currentState.currencyCode
        )
        securePreferences.clearRegexInput()
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
                            isTransaction = result.isTransaction
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
2. If it IS a transaction, generate ONE Kotlin-compatible regex pattern with named capture groups:
   - (?<amount>...) — captures the transaction amount
   - (?<merchant>...) — captures the merchant/payee name
3. If it is NOT a transaction, set regex to null.

Return ONLY valid JSON with no markdown formatting:
{"isTransaction": true/false, "regex": "pattern or null"}
        """.trimIndent()
    }

    private data class AiResponse(val isTransaction: Boolean, val regex: String?)

    private fun parseAiResponse(response: String): AiResponse? {
        try {
            val json = org.json.JSONObject(response.trim())
            val isTransaction = json.optBoolean("isTransaction", true)
            val regex = json.optString("regex", "").takeIf { it.isNotBlank() && it != "null" }
            return AiResponse(isTransaction, regex)
        } catch (e: Exception) {
            val regex = extractRegexPatternLegacy(response)
            return AiResponse(isTransaction = regex != null, regex = regex)
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
        return try { amountStr.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0 } catch (_: Exception) { 0.0 }
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

        _state.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val pattern = NotificationPatternEntity(
                    packageName = currentState.selectedAppPackage,
                    notificationTitle = currentState.notificationTitle,
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
                    manualPattern = ""
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
                    packageName = currentState.selectedAppPackage,
                    notificationTitle = currentState.notificationTitle,
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
                        notes = "Extracted during notification inbox pattern setup"
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
                    manualPattern = ""
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
