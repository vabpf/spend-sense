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
    val isLoading: Boolean = true
)

@HiltViewModel
class WhitelistedAppsViewModel @Inject constructor(
    private val whitelistedAppDao: WhitelistedAppDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

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

                _state.value = _state.value.copy(
                    apps = installedApps,
                    isLoading = false
                )
            }
        }
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
}
