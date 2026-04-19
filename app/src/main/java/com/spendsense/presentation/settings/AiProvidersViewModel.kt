package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.dao.AiProviderDao
import com.spendsense.data.local.entity.AiProviderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val aiProviderDao: AiProviderDao
) : ViewModel() {

    private val _state = MutableStateFlow(AiProvidersState())
    val state: StateFlow<AiProvidersState> = _state.asStateFlow()

    init {
        loadProviders()
    }

    private fun loadProviders() {
        viewModelScope.launch {
            aiProviderDao.getAllProvidersFlow().collect { providers ->
                _state.value = _state.value.copy(providers = providers)
            }
        }
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

    fun saveProvider() {
        val currentState = _state.value
        if (currentState.name.isBlank() || currentState.apiKey.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Name and API Key are required")
            return
        }

        viewModelScope.launch {
            val provider = AiProviderEntity(
                name = currentState.name,
                baseUrl = currentState.baseUrl,
                apiKey = currentState.apiKey,
                defaultModel = currentState.defaultModel,
                jobType = currentState.jobType
            )
            aiProviderDao.insert(provider)
            _state.value = currentState.copy(
                isAddingProvider = false,
                name = "",
                apiKey = "",
                errorMessage = null
            )
        }
    }

    fun deleteProvider(provider: AiProviderEntity) {
        viewModelScope.launch {
            aiProviderDao.delete(provider)
        }
    }
}
