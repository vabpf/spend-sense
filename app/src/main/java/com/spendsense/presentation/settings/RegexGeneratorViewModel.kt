package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.AiProviderPresets
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.AiProviderDao
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.AiProviderEntity
import com.spendsense.data.remote.ChatCompletionApi
import com.spendsense.data.remote.DynamicBaseUrlInterceptor
import com.spendsense.data.remote.model.Message
import com.spendsense.data.remote.model.ChatCompletionRequest
import com.spendsense.domain.model.RegexPattern
import com.spendsense.domain.repository.RegexPatternRepository
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
    private val regexPatternRepository: RegexPatternRepository,
    private val aiProviderDao: AiProviderDao,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(RegexGeneratorState())
    val state: StateFlow<RegexGeneratorState> = _state.asStateFlow()

    init {
        loadProviders()
        loadWhitelistedApps()
        _state.value = _state.value.copy(currencyCode = securePreferences.getDefaultCurrency())
    }

    private fun loadProviders() {
        viewModelScope.launch {
            AiProviderPresets.ensureSeeded(aiProviderDao)
            val providers = aiProviderDao.getAllProviders().sortedWith(
                compareBy<AiProviderEntity>({ it.name.lowercase() }, { it.defaultModel.lowercase() })
            )
            val keyStatuses = providers.associate { provider ->
                val isOpenCode = provider.baseUrl.contains("opencode", ignoreCase = true)
                provider.id to (isOpenCode || !resolveProviderApiKey(provider).isNullOrBlank())
            }
            val firstConfiguredProvider = providers.firstOrNull { keyStatuses[it.id] == true }
            _state.value = _state.value.copy(
                providers = providers,
                providerKeyStatuses = keyStatuses,
                selectedProvider = firstConfiguredProvider
            )
        }
    }

    fun onProviderSelected(provider: AiProviderEntity) {
        _state.value = _state.value.copy(selectedProvider = provider)
    }

    fun onTargetAppSelected(packageName: String) {
        _state.value = _state.value.copy(
            selectedAppPackage = packageName,
            errorMessage = null
        )
    }

    fun updateNotificationText(text: String) {
        _state.value = _state.value.copy(
            notificationText = text,
            errorMessage = null
        )
    }

    fun updateManualPattern(pattern: String) {
        _state.value = _state.value.copy(
            manualPattern = pattern,
            errorMessage = null
        )
    }

    fun toggleActive() {
        _state.value = _state.value.copy(isActive = !_state.value.isActive)
    }

    fun updateCurrency(currencyCode: String) {
        _state.value = _state.value.copy(currencyCode = currencyCode)
    }

    fun clearInput() {
        val currentState = _state.value
        _state.value = RegexGeneratorState(
            providers = currentState.providers,
            providerKeyStatuses = currentState.providerKeyStatuses,
            selectedProvider = currentState.selectedProvider,
            availableApps = currentState.availableApps
        )
    }

    private fun loadWhitelistedApps() {
        viewModelScope.launch {
            val apps = whitelistedAppDao.getEnabledApps()
                .map { RegexTargetApp(packageName = it.packageName, appName = it.appName) }
                .sortedBy { it.appName.lowercase() }

            _state.value = _state.value.copy(availableApps = apps)
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

        val provider = currentState.selectedProvider
        if (provider == null) {
            _state.value = currentState.copy(errorMessage = "Please select an AI provider")
            return
        }

        val apiKey = resolveProviderApiKey(provider)
        val isFreeProvider = provider.baseUrl.contains("opencode", ignoreCase = true)
        
        if (apiKey.isNullOrBlank() && !isFreeProvider) {
            _state.value = currentState.copy(errorMessage = "API key not found for ${provider.name}. Please add it in AI Providers settings.")
            return
        }
        
        _state.value = currentState.copy(
            isGenerating = true,
            errorMessage = null,
            generatedPattern = null
        )

        // Set the base URL for the selected provider
        dynamicBaseUrlInterceptor.setBaseUrl(
            url = provider.baseUrl,
            key = apiKey,
            isOpenRouter = provider.name.contains("OpenRouter", ignoreCase = true),
            isOpenCode = provider.baseUrl.contains("opencode", ignoreCase = true)
        )

        viewModelScope.launch {
            try {
                val prompt = buildPrompt(notificationText)
                val request = ChatCompletionRequest(
                    model = provider.defaultModel,
                    messages = listOf(
                        Message(role = "user", content = prompt)
                    )
                )

                val response = chatCompletionApi.generateCompletion(
                    request = request
                )

                val generatedText = response.choices.firstOrNull()?.message?.content
                if (generatedText != null) {
                    val pattern = extractRegexPattern(generatedText)
                    if (pattern != null) {
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            generatedPattern = pattern,
                            manualPattern = "" // Clear manual if AI succeeds
                        )
                        testPattern(pattern, notificationText)
                    } else {
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            errorMessage = "Could not extract regex pattern from response"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isGenerating = false,
                        errorMessage = "No response from AI"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isGenerating = false,
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }

    private fun buildPrompt(notificationText: String): String {
        return """
You are a Regex expert. Create a Java/Kotlin compatible Regex pattern to extract transaction details from this banking notification.

Requirements:
1. The Regex must have TWO named capture groups:
   - 'amount': Captures the transaction amount (numbers with optional decimal, may include currency symbol)
   - 'merchant': Captures the merchant/payee name

2. Use Java/Kotlin named group syntax: (?<groupName>pattern)

3. The pattern should be flexible to match variations but precise enough to extract correct data.

4. Return ONLY the regex pattern, nothing else. No explanations, no code blocks, just the raw regex string.

Notification text:
"$notificationText"

Respond with only the regex pattern:
        """.trimIndent()
    }

    private fun extractRegexPattern(response: String): String? {
        val lines = response.trim().lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("(?<amount>") && trimmed.contains("(?<merchant>")) {
                return trimmed
            }
        }
        val trimmed = response.trim()
        if (trimmed.contains("(?<amount>") && trimmed.contains("(?<merchant>")) {
            return trimmed
        }
        return null
    }

    private fun testPattern(pattern: String, text: String) {
        try {
            val regex = Regex(pattern)
            val matchResult = regex.find(text)
            
            if (matchResult != null) {
                val amount = matchResult.groups["amount"]?.value
                val merchant = matchResult.groups["merchant"]?.value
                
                _state.value = _state.value.copy(
                    extractedAmount = amount,
                    extractedMerchant = merchant,
                    errorMessage = null
                )
            } else {
                _state.value = _state.value.copy(
                    extractedAmount = null,
                    extractedMerchant = null,
                    errorMessage = "Pattern does not match the notification text"
                )
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                errorMessage = "Invalid regex pattern: ${e.message}"
            )
        }
    }

    fun savePattern() {
        val currentState = _state.value
        val patternToSave = if (currentState.manualPattern.isNotBlank()) {
            currentState.manualPattern
        } else {
            currentState.generatedPattern
        }
        
        if (patternToSave.isNullOrBlank()) {
            _state.value = currentState.copy(errorMessage = "No pattern to save")
            return
        }

        if (currentState.availableApps.isEmpty()) {
            _state.value = currentState.copy(
                errorMessage = "No whitelisted apps found. Please whitelist at least one app first."
            )
            return
        }

        if (currentState.selectedAppPackage.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Please select an app to apply this pattern")
            return
        }

        _state.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val pattern = RegexPattern(
                    packageName = currentState.selectedAppPackage,
                    pattern = patternToSave,
                    currencyCode = currentState.currencyCode,
                    isActive = currentState.isActive
                )

                regexPatternRepository.insertPattern(pattern)
                securePreferences.setDefaultCurrency(currentState.currencyCode)

                _state.value = _state.value.copy(
                    isSaving = false,
                    successMessage = "Pattern saved successfully!"
                )
            } catch (e: Exception) {
                _state.value = currentState.copy(
                    isSaving = false,
                    errorMessage = "Error saving pattern: ${e.message}"
                )
            }
        }
    }

    private fun resolveProviderApiKey(provider: AiProviderEntity): String? {
        val providerKey = buildProviderGroupKey(provider)
        val providerLevelKey = securePreferences.getApiKeyForProviderKey(providerKey)
        if (!providerLevelKey.isNullOrBlank()) {
            return providerLevelKey
        }

        val legacyModelKey = securePreferences.getApiKey(provider.id)
        if (!legacyModelKey.isNullOrBlank()) {
            securePreferences.saveApiKeyForProviderKey(providerKey, legacyModelKey)
        }
        return legacyModelKey
    }
}
