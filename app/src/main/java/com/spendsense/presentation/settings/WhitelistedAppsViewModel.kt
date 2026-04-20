package com.spendsense.presentation.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.domain.model.WhitelistedApp
import com.spendsense.domain.repository.WhitelistedAppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhitelistedAppsViewModel @Inject constructor(
    private val repository: WhitelistedAppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WhitelistedAppsState())
    val state: StateFlow<WhitelistedAppsState> = _state.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLoading = true) }

            // 1. Get installed apps
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val installedApps = packages.filter {
                // Filter out system apps if desired, but we might want to track some system apps
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName == "com.android.vending" // allow play store maybe?
            }.map { appInfo ->
                val appName = pm.getApplicationLabel(appInfo).toString()
                WhitelistedApp(
                    packageName = appInfo.packageName,
                    appName = appName,
                    isEnabled = false,
                    addedAt = System.currentTimeMillis()
                )
            }.sortedBy { it.appName }

            // 2. Observe DB state
            repository.getAllApps().collect { dbApps ->
                val dbAppsMap = dbApps.associateBy { it.packageName }

                // 3. Merge installed apps with DB state
                val mergedApps = installedApps.map { installedApp ->
                    dbAppsMap[installedApp.packageName] ?: installedApp
                }.sortedWith(compareByDescending<WhitelistedApp> { it.isEnabled }.thenBy { it.appName })

                _state.update {
                    it.copy(
                        apps = mergedApps,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onEvent(event: WhitelistedAppsEvent) {
        when (event) {
            is WhitelistedAppsEvent.ToggleApp -> {
                viewModelScope.launch {
                    val app = event.app
                    if (app.isEnabled) {
                        repository.setAppEnabled(app.packageName, false)
                    } else {
                        // Insert if it doesn't exist, otherwise update to enabled
                        repository.insertApp(app.copy(isEnabled = true, addedAt = System.currentTimeMillis()))
                    }
                }
            }
            is WhitelistedAppsEvent.UpdateSearchQuery -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
        }
    }
}

sealed class WhitelistedAppsEvent {
    data class ToggleApp(val app: WhitelistedApp) : WhitelistedAppsEvent()
    data class UpdateSearchQuery(val query: String) : WhitelistedAppsEvent()
}
