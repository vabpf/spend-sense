package com.spendsense.data.repository

import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.WhitelistedAppEntity
import com.spendsense.domain.model.WhitelistedApp
import com.spendsense.domain.repository.WhitelistedAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WhitelistedAppRepositoryImpl @Inject constructor(
    private val dao: WhitelistedAppDao
) : WhitelistedAppRepository {

    override fun getAllApps(): Flow<List<WhitelistedApp>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getEnabledApps(): List<WhitelistedApp> {
        return dao.getEnabledApps().map { it.toDomainModel() }
    }

    override suspend fun insertApp(app: WhitelistedApp) {
        dao.insert(app.toEntity())
    }

    override suspend fun deleteApp(app: WhitelistedApp) {
        dao.delete(app.toEntity())
    }

    override suspend fun setAppEnabled(packageName: String, isEnabled: Boolean) {
        dao.setEnabled(packageName, isEnabled)
    }

    private fun WhitelistedAppEntity.toDomainModel(): WhitelistedApp {
        return WhitelistedApp(
            packageName = packageName,
            appName = appName,
            isEnabled = isEnabled,
            addedAt = addedAt
        )
    }

    private fun WhitelistedApp.toEntity(): WhitelistedAppEntity {
        return WhitelistedAppEntity(
            packageName = packageName,
            appName = appName,
            isEnabled = isEnabled,
            addedAt = addedAt
        )
    }
}
