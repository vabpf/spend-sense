package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.AiProviderPresets
import com.spendsense.data.local.dao.AiProviderDao
import com.spendsense.data.local.entity.AiProviderEntity
import com.spendsense.data.local.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val aiProviderDao: AiProviderDao,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AiProvidersState())
    val state: StateFlow<AiProvidersState> = _state.asStateFlow()

    init {
        seedPresetProviders()
        loadProviders()
    }

    private fun seedPresetProviders() {
        viewModelScope.launch {
            AiProviderPresets.ensureSeeded(aiProviderDao)
        }
    }

    private fun loadProviders() {
        viewModelScope.launch {
            aiProviderDao.getAllProvidersFlow().collect { providers ->
                val sortedProviders = providers.sortedWith(
                    compareBy<AiProviderEntity>({ it.name.lowercase() }, { it.defaultModel.lowercase() })
                )
                val statuses = sortedProviders.associate { provider ->
                    provider.id to isProviderConfigured(provider)
                }
                _state.value = _state.value.copy(
                    providers = sortedProviders,
                    providerGroups = groupProviders(sortedProviders),
                    providerKeyStatuses = statuses
                )
            }
        }
    }

    private fun updateKeyStatuses() {
        val providers = _state.value.providers
        val statuses = providers.associate { provider ->
            provider.id to isProviderConfigured(provider)
        }
        _state.value = _state.value.copy(providerKeyStatuses = statuses)
    }

    private fun isProviderConfigured(provider: AiProviderEntity): Boolean {
        val isOpenCode = provider.baseUrl.contains("opencode", ignoreCase = true)
        return isOpenCode || !resolveProviderApiKey(provider).isNullOrBlank()
    }

    fun onNameChange(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun onBaseUrlChange(url: String) {
        _state.value = _state.value.copy(baseUrl = url)
    }

    fun onApiKeyChange(key: String) {
        _state.value = _state.value.copy(apiKey = key)
    }

    fun onModelChange(model: String) {
        _state.value = _state.value.copy(defaultModel = model)
    }

    fun toggleAddingProvider(show: Boolean) {
        _state.value = _state.value.copy(
            isAddingProvider = show,
            errorMessage = null
        )
    }

    fun onEditProvider(provider: AiProviderEntity?) {
        val existingKey = provider?.let { resolveProviderApiKey(it).orEmpty() }.orEmpty()

        _state.value = _state.value.copy(
            editingProvider = provider,
            showKeyDialog = provider != null,
            apiKey = "",
            existingApiKeyPreview = existingKey.maskForPreview(),
            errorMessage = null
        )
        if (provider != null) {
            _state.value = _state.value.copy(
                defaultModel = provider.defaultModel,
                name = provider.name,
                baseUrl = provider.baseUrl,
                jobType = provider.jobType
            )
        }
    }

    fun updateApiKeyForProvider(provider: AiProviderEntity, newKey: String) {
        val isFreeProvider = provider.baseUrl.contains("opencode", ignoreCase = true)
        val providerKey = buildProviderGroupKey(provider)
        val existingKey = resolveProviderApiKey(provider).orEmpty()
        val keyToUse = if (newKey.isBlank()) existingKey else newKey

        if (keyToUse.isBlank() && !isFreeProvider) {
            _state.value = _state.value.copy(errorMessage = "API Key is required for ${provider.name}")
            return
        }

        if (newKey.isNotBlank()) {
            securePreferences.saveApiKeyForProviderKey(providerKey, newKey)
        }

        onEditProvider(null)
        updateKeyStatuses()
    }

    fun saveProvider() {
        val currentState = _state.value
        if (currentState.name.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Name is required")
            return
        }

        val isFreeProvider = currentState.baseUrl.contains("opencode", ignoreCase = true)
        if (currentState.apiKey.isBlank() && !isFreeProvider) {
            _state.value = currentState.copy(errorMessage = "API Key is required for this provider")
            return
        }

        viewModelScope.launch {
            val provider = AiProviderEntity(
                name = currentState.name,
                baseUrl = currentState.baseUrl,
                defaultModel = currentState.defaultModel,
                jobType = currentState.jobType
            )
            aiProviderDao.insert(provider)
            if (currentState.apiKey.isNotBlank()) {
                securePreferences.saveApiKeyForProviderKey(
                    buildProviderGroupKey(provider),
                    currentState.apiKey
                )
            }
            
            _state.value = currentState.copy(
                isAddingProvider = false,
                name = "",
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "",
                defaultModel = "meta-llama/llama-3.2-3b-instruct:free",
                errorMessage = null,
                existingApiKeyPreview = null
            )
            updateKeyStatuses()
        }
    }

    fun deleteProvider(provider: AiProviderEntity) {
        if (provider.isPreset) {
            _state.value = _state.value.copy(errorMessage = "Preset providers cannot be deleted")
            return
        }

        viewModelScope.launch {
            aiProviderDao.delete(provider)
            securePreferences.deleteApiKeyForProviderKey(buildProviderGroupKey(provider))
            securePreferences.deleteApiKey(provider.id)
        }
    }

    fun deleteProviderGroup(group: AiProviderGroup) {
        if (group.isPreset) {
            _state.value = _state.value.copy(errorMessage = "Preset providers cannot be deleted")
            return
        }

        viewModelScope.launch {
            group.models.forEach { provider ->
                aiProviderDao.delete(provider)
                securePreferences.deleteApiKey(provider.id)
            }
            securePreferences.deleteApiKeyForProviderKey(group.key)
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

    private fun String.maskForPreview(): String? {
        if (isBlank()) return null
        return take(3) + "..."
    }
}
