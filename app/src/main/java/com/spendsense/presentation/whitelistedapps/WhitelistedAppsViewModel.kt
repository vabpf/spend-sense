package com.spendsense.presentation.whitelistedapps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.WhitelistedAppEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppItem(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean
)

data class WhitelistedAppsState(
    val apps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val suggestedApps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class WhitelistedAppsViewModel @Inject constructor(
    private val whitelistedAppDao: WhitelistedAppDao,
    @ApplicationContext private val context: Context
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

            // Collect flow of whitelisted apps from DB
            whitelistedAppDao.getAllFlow().collect { dbApps ->
                val dbAppsMap = dbApps.associateBy { it.packageName }

                // Get installed apps
                val installedApps = withContext(Dispatchers.IO) {
                    val pm = context.packageManager
                    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                    packages.filter { appInfo ->
                        // Filter out system apps, mostly
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || dbAppsMap.containsKey(appInfo.packageName)
                    }.map { appInfo ->
                        val packageName = appInfo.packageName
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val isEnabled = dbAppsMap[packageName]?.isEnabled ?: false

                        AppItem(
                            packageName = packageName,
                            appName = appName,
                            isEnabled = isEnabled
                        )
                    }.sortedBy { it.appName }
                }

                val currentQuery = _state.value.searchQuery
                val suggestedApps = installedApps.filter { app ->
                    val name = app.appName.lowercase()
                    val pkg = app.packageName.lowercase()
                    suggestedBankKeywords.any { keyword ->
                        name.contains(keyword) || pkg.contains(keyword)
                    }
                }

                _state.value = _state.value.copy(
                    apps = installedApps,
                    filteredApps = filterApps(installedApps, currentQuery),
                    suggestedApps = suggestedApps.sortedBy { it.appName.lowercase() },
                    isLoading = false
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
            val entity = WhitelistedAppEntity(
                packageName = app.packageName,
                appName = app.appName,
                isEnabled = isEnabled
            )
            whitelistedAppDao.insert(entity) // Insert or replace
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
