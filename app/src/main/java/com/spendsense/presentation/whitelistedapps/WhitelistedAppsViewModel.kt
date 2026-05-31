package com.spendsense.presentation.whitelistedapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.domain.repository.AppItem
import com.spendsense.domain.repository.WhitelistedAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhitelistedAppsState(
    val apps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val suggestedApps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class WhitelistedAppsViewModel @Inject constructor(
    private val whitelistedAppRepository: WhitelistedAppRepository
) : ViewModel() {

    private val suggestedBankKeywords = listOf(
        "mb bank", "mbbank", "com.mbbank",
        "hsbc", "com.hsbc",
        "tpbank", "com.tpb",
        "vietin", "vietinbank",
        "vietcom", "vietcombank",
        "bidv", "com.bidv",
        "acb", "asia commercial bank",
        "techcom", "techcombank",
        "vpbank", "com.vnpay.vpbank",
        "sacombank", "sacom"
    )

    private val _state = MutableStateFlow(WhitelistedAppsState())
    val state: StateFlow<WhitelistedAppsState> = _state.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            whitelistedAppRepository.getWhitelistedAppsFlow().collect { installedApps ->
                val currentQuery = _state.value.searchQuery
                val suggestedApps = installedApps.filter { app ->
                    val name = app.appName.lowercase()
                    val pkg = app.packageName.lowercase()
                    suggestedBankKeywords.any { keyword ->
                        name.contains(keyword) || pkg.contains(keyword)
                    }
                }

                // If installedApps list is not empty, we are no longer loading
                val isStillLoading = installedApps.isEmpty() && _state.value.isLoading

                _state.value = _state.value.copy(
                    apps = installedApps,
                    filteredApps = filterApps(installedApps, currentQuery),
                    suggestedApps = suggestedApps.sortedBy { it.appName.lowercase() },
                    isLoading = isStillLoading
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val apps = _state.value.apps
        _state.value = _state.value.copy(
            searchQuery = query,
            filteredApps = filterApps(apps, query)
        )
    }

    fun toggleApp(app: AppItem, isEnabled: Boolean) {
        viewModelScope.launch {
            whitelistedAppRepository.toggleApp(app.packageName, app.appName, isEnabled)
        }
    }

    private fun filterApps(apps: List<AppItem>, query: String): List<AppItem> {
        if (query.isBlank()) {
            return apps
        }

        val normalizedQuery = query.trim().lowercase()
        return apps.filter { app ->
            app.appName.lowercase().contains(normalizedQuery) ||
                app.packageName.lowercase().contains(normalizedQuery)
        }
    }
}
