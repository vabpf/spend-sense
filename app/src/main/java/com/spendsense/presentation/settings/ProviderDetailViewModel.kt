package com.spendsense.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.ProviderModelFetcher
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.ProviderAccountDao
import com.spendsense.data.local.dao.ProviderModelDao
import com.spendsense.data.local.entity.ProviderAccountEntity
import com.spendsense.data.local.entity.ProviderModelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderDetailState(
    val account: ProviderAccountEntity? = null,
    val models: List<ProviderModelEntity> = emptyList(),
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val apiKey: String = "",
    val existingApiKeyPreview: String? = null,
    val showKeyDialog: Boolean = false,
    val errorMessage: String? = null,
    val lastRefreshedAt: Long = 0
)

@HiltViewModel
class ProviderDetailViewModel @Inject constructor(
    private val accountDao: ProviderAccountDao,
    private val modelDao: ProviderModelDao,
    private val modelFetcher: ProviderModelFetcher,
    private val securePreferences: SecurePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ProviderDetailState())
    val state: StateFlow<ProviderDetailState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    private var accountId: Long = -1

    fun load(accountId: Long) {
        this.accountId = accountId
        viewModelScope.launch {
            val account = accountDao.getById(accountId) ?: return@launch
            val key = securePreferences.getApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl))
            _state.value = _state.value.copy(
                account = account,
                apiKey = key ?: "",
                existingApiKeyPreview = key?.let { if (it.length >= 3) "${it.take(3)}..." else null }
            )
            observeModels()
        }
    }

    private fun observeModels() {
        modelDao.getByAccountIdFlow(accountId)
            .combine(_searchQuery) { models, query ->
                models to query
            }
            .onEach { (models, query) ->
                val lastRefreshed = models.maxOfOrNull { it.lastRefreshedAt } ?: 0
                val filtered = if (query.isNotBlank()) {
                    val q = query.lowercase()
                    models.filter { m ->
                        (m.displayName?.lowercase()?.contains(q) == true) ||
                        m.modelId.lowercase().contains(q)
                    }
                } else {
                    models
                }
                _state.value = _state.value.copy(
                    models = filtered,
                    lastRefreshedAt = lastRefreshed,
                    searchQuery = query
                )
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleModel(model: ProviderModelEntity) {
        viewModelScope.launch {
            modelDao.setEnabled(model.id, !model.isEnabled)
        }
    }

    fun refreshModels() {
        val account = _state.value.account ?: return
        val apiKey = _state.value.apiKey

        val isFreeListing = account.baseUrl.contains("opencode", ignoreCase = true) ||
                            account.baseUrl.contains("ollama", ignoreCase = true) ||
                            account.baseUrl.contains("nvidia", ignoreCase = true) ||
                            account.baseUrl.contains("openrouter", ignoreCase = true)

        if (!isFreeListing && apiKey.isBlank()) {
            _state.value = _state.value.copy(showKeyDialog = true)
            return
        }

        _state.value = _state.value.copy(isRefreshing = true, errorMessage = null)

        viewModelScope.launch {
            try {
                modelDao.deleteByAccountId(account.id)
                val fetched = modelFetcher.fetchModels(account, if (isFreeListing) "" else apiKey)
                modelDao.upsertAll(fetched)
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    lastRefreshedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRefreshing = false,
                    errorMessage = "Failed to fetch: ${e.message}"
                )
            }
        }
    }

    fun showKeyDialog(show: Boolean) {
        _state.value = _state.value.copy(showKeyDialog = show, errorMessage = null)
    }

    fun onApiKeyChange(key: String) {
        _state.value = _state.value.copy(apiKey = key)
    }

    fun saveApiKey() {
        val account = _state.value.account ?: return
        val key = _state.value.apiKey
        if (key.isNotBlank()) {
            securePreferences.saveApiKeyForProviderKey(buildProviderGroupKey(account.baseUrl), key)
        }
        _state.value = _state.value.copy(
            showKeyDialog = false,
            existingApiKeyPreview = if (key.length >= 3) "${key.take(3)}..." else null
        )
        refreshModels()
    }
}
