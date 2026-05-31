package com.spendsense.domain.repository

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow

data class AppItem(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val icon: Drawable? = null
)

interface WhitelistedAppRepository {
    fun getWhitelistedAppsFlow(): Flow<List<AppItem>>
    suspend fun toggleApp(packageName: String, appName: String, isEnabled: Boolean)
    fun prefetchInstalledApps()
}
