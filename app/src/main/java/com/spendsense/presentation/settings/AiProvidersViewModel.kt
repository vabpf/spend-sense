package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        loadProviders()
    }

    private fun loadProviders() {
        viewModelScope.launch {
            aiProviderDao.getAllProvidersFlow().collect { providers ->
                val statuses = providers.associate { it.id to !securePreferences.getApiKey(it.id).isNullOrBlank() }
                _state.value = _state.value.copy(
                    providers = providers,
                    providerKeyStatuses = statuses
                )
            }
        }
    }

    private fun updateKeyStatuses() {
        val providers = _state.value.providers
        val statuses = providers.associate { it.id to !securePreferences.getApiKey(it.id).isNullOrBlank() }
        _state.value = _state.value.copy(providerKeyStatuses = statuses)
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
        _state.value = _state.value.copy(isAddingProvider = show)
    }

    fun onEditProvider(provider: AiProviderEntity?) {
        _state.value = _state.value.copy(
            editingProvider = provider,
            showKeyDialog = provider != null,
            apiKey = "" // Reset key field for the dialog
        )
    }

    fun updateApiKeyForProvider(provider: AiProviderEntity, newKey: String) {
        if (newKey.isBlank()) return
        securePreferences.saveApiKey(provider.id, newKey)
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
            val newProviderId = aiProviderDao.insert(provider)
            if (currentState.apiKey.isNotBlank()) {
                securePreferences.saveApiKey(newProviderId, currentState.apiKey)
            }
            
            _state.value = currentState.copy(
                isAddingProvider = false,
                name = "",
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "",
                defaultModel = "meta-llama/llama-3.2-3b-instruct:free",
                errorMessage = null
            )
            updateKeyStatuses()
        }
    }

    fun deleteProvider(provider: AiProviderEntity) {
        viewModelScope.launch {
            aiProviderDao.delete(provider)
            securePreferences.deleteApiKey(provider.id)
        }
    }
}
