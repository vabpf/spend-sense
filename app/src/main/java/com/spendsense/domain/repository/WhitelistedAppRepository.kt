package com.spendsense.domain.repository

import com.spendsense.domain.model.WhitelistedApp
import kotlinx.coroutines.flow.Flow

interface WhitelistedAppRepository {
    fun getAllApps(): Flow<List<WhitelistedApp>>
    suspend fun getEnabledApps(): List<WhitelistedApp>
    suspend fun insertApp(app: WhitelistedApp)
    suspend fun deleteApp(app: WhitelistedApp)
    suspend fun setAppEnabled(packageName: String, isEnabled: Boolean)
}
