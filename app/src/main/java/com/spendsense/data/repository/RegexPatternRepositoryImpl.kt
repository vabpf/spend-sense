package com.spendsense.data.repository

import com.spendsense.data.local.dao.RegexPatternDao
import com.spendsense.data.local.entity.RegexPatternEntity
import com.spendsense.domain.model.RegexPattern
import com.spendsense.domain.repository.RegexPatternRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegexPatternRepositoryImpl @Inject constructor(
    private val regexPatternDao: RegexPatternDao
) : RegexPatternRepository {

    override suspend fun insertPattern(pattern: RegexPattern): Long {
        return regexPatternDao.insert(pattern.toEntity())
    }

    override suspend fun updatePattern(pattern: RegexPattern) {
        regexPatternDao.update(pattern.toEntity())
    }

    override suspend fun deletePattern(pattern: RegexPattern) {
        regexPatternDao.delete(pattern.toEntity())
    }

    override suspend fun getPatternById(id: Long): RegexPattern? {
        return regexPatternDao.getById(id)?.toDomain()
    }

    override suspend fun getActivePatterns(): List<RegexPattern> {
        return regexPatternDao.getActivePatterns().map { it.toDomain() }
    }

    override suspend fun getActivePatternsForPackage(packageName: String): List<RegexPattern> {
        return regexPatternDao
            .getActivePatternsForPackage(packageName, RegexPattern.TARGET_ALL_WHITELISTED)
            .map { it.toDomain() }
    }

    override fun getAllPatterns(): Flow<List<RegexPattern>> {
        return regexPatternDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun incrementSuccessCount(id: Long) {
        regexPatternDao.incrementSuccessCount(id, System.currentTimeMillis())
    }

    override suspend fun setPatternActive(id: Long, isActive: Boolean) {
        regexPatternDao.setActive(id, isActive)
    }

    private fun RegexPattern.toEntity() = RegexPatternEntity(
        id = id,
        packageName = packageName,
        pattern = pattern,
        currencyCode = currencyCode,
        isActive = isActive
    )

    private fun RegexPatternEntity.toDomain() = RegexPattern(
        id = id,
        packageName = packageName,
        pattern = pattern,
        currencyCode = currencyCode,
        isActive = isActive
    )
}
