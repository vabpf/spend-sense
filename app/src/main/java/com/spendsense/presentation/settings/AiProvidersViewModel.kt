package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.AiProviderPresets
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.ProviderAccountDao
import com.spendsense.data.local.dao.ProviderModelDao
import com.spendsense.data.local.entity.ProviderAccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val accountDao: ProviderAccountDao,
    private val modelDao: ProviderModelDao,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AiProvidersState())
    val state: StateFlow<AiProvidersState> = _state.asStateFlow()

    init {
        cleanDeprecatedAccounts()
        seedPresetAccounts()
        loadAccounts()
    }

    private fun cleanDeprecatedAccounts() {
        viewModelScope.launch {
            val oldUrls = listOf("https://api.ollama.com/v1")
            accountDao.getAll().filter { it.baseUrl in oldUrls }.forEach {
                accountDao.delete(it)
            }
        }
    }

    private fun seedPresetAccounts() {
        viewModelScope.launch {
            AiProviderPresets.ensureSeeded(accountDao)
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            combine(
                accountDao.getAllFlow(),
                modelDao.onModelsChanged()
            ) { accounts, _ ->
                accounts.map { account ->
                    val models = modelDao.getByAccountId(account.id)
                    val isConfigured = isAccountConfigured(account)
                    ProviderAccountDisplay(
                        account = account,
                        isConfigured = isConfigured,
                        enabledModelCount = models.count { it.isEnabled },
                        totalModelCount = models.size,
                        lastRefreshedAt = models.maxOfOrNull { it.lastRefreshedAt } ?: 0
                    )
                }
            }.collect { displays ->
                _state.value = _state.value.copy(accounts = displays)
            }
        }
    }

    fun isAccountConfigured(account: ProviderAccountEntity): Boolean {
        val isOpenCode = account.baseUrl.contains("opencode", ignoreCase = true)
        if (isOpenCode) return true
        val key = securePreferences.getApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl))
        return !key.isNullOrBlank()
    }

    fun toggleAddingProvider(show: Boolean) {
        _state.value = _state.value.copy(
            isAddingProvider = show,
            errorMessage = null
        )
    }

    fun onNameChange(name: String) { _state.value = _state.value.copy(name = name) }
    fun onBaseUrlChange(url: String) { _state.value = _state.value.copy(baseUrl = url) }
    fun onApiKeyChange(key: String) { _state.value = _state.value.copy(apiKey = key) }

    fun saveProvider() {
        val currentState = _state.value
        if (currentState.name.isBlank()) {
            _state.value = currentState.copy(errorMessage = "Name is required")
            return
        }
        if (currentState.apiKey.isBlank() && !currentState.baseUrl.contains("opencode", ignoreCase = true)) {
            _state.value = currentState.copy(errorMessage = "API Key is required for this provider")
            return
        }

        viewModelScope.launch {
            val account = ProviderAccountEntity(
                name = currentState.name,
                baseUrl = currentState.baseUrl,
                jobType = AiProviderPresets.JOB_REGEX_GEN
            )
            val id = accountDao.insert(account)
            if (currentState.apiKey.isNotBlank()) {
                securePreferences.saveApiKeyForProviderKey(
                    buildProviderGroupKey(account.baseUrl),
                    currentState.apiKey
                )
            }

            _state.value = currentState.copy(
                isAddingProvider = false,
                name = "",
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "",
                errorMessage = null
            )
        }
    }

    fun deleteAccount(account: ProviderAccountEntity) {
        viewModelScope.launch {
            accountDao.delete(account)
            securePreferences.deleteApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl))
        }
    }
}
