package com.spendsense.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.WhitelistedAppEntity
import com.spendsense.domain.repository.AppItem
import com.spendsense.domain.repository.WhitelistedAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistedAppRepositoryImpl @Inject constructor(
    private val whitelistedAppDao: WhitelistedAppDao,
    @ApplicationContext private val context: Context
) : WhitelistedAppRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val installedAppsFlow = MutableStateFlow<List<AppItem>>(emptyList())
    private var isPrefetchStarted = false

    init {
        // Automatically start prefetching on repository creation
        prefetchInstalledApps()
    }

    override fun getWhitelistedAppsFlow(): Flow<List<AppItem>> {
        return whitelistedAppDao.getAllFlow().combine(installedAppsFlow) { dbApps, installed ->
            val dbAppsMap = dbApps.associateBy { it.packageName }
            
            // If the background scan is still warming up, fall back to whitelisted database items directly
            val baseList = if (installed.isEmpty()) {
                dbApps.map {
                    AppItem(
                        packageName = it.packageName,
                        appName = it.appName,
                        isEnabled = it.isEnabled,
                        icon = null
                    )
                }
            } else {
                installed
            }
            
            baseList.map { app ->
                val dbApp = dbAppsMap[app.packageName]
                app.copy(
                    isEnabled = dbApp?.isEnabled ?: false
                )
            }.sortedBy { it.appName.lowercase() }
        }
    }

    override suspend fun toggleApp(packageName: String, appName: String, isEnabled: Boolean) {
        withContext(Dispatchers.IO) {
            val entity = WhitelistedAppEntity(
                packageName = packageName,
                appName = appName,
                isEnabled = isEnabled
            )
            whitelistedAppDao.insert(entity)
        }
    }

    override fun prefetchInstalledApps() {
        synchronized(this) {
            if (isPrefetchStarted) return
            isPrefetchStarted = true
        }

        repositoryScope.launch(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                
                // Get one-time whitelisted db list to ensure system-whitelisted apps are retained
                val dbAppsMap = whitelistedAppDao.getEnabledApps().associateBy { it.packageName }

                val items = packages.filter { appInfo ->
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || dbAppsMap.containsKey(appInfo.packageName)
                }.map { appInfo ->
                    val packageName = appInfo.packageName
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val isEnabled = dbAppsMap[packageName]?.isEnabled ?: false
                    val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }

                    AppItem(
                        packageName = packageName,
                        appName = appName,
                        isEnabled = isEnabled,
                        icon = icon
                    )
                }
                
                installedAppsFlow.value = items
            } catch (e: Exception) {
                // Fail silently in pre-fetch; fallback logic handles empty state gracefully
            }
        }
    }
}
