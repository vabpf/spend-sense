package com.spendsense.domain.repository

import com.spendsense.domain.model.RegexPattern
import kotlinx.coroutines.flow.Flow

interface RegexPatternRepository {
    suspend fun insertPattern(pattern: RegexPattern): Long
    suspend fun updatePattern(pattern: RegexPattern)
    suspend fun deletePattern(pattern: RegexPattern)
    suspend fun getPatternById(id: Long): RegexPattern?
    suspend fun getActivePatterns(): List<RegexPattern>
    suspend fun getActivePatternsForPackage(packageName: String): List<RegexPattern>
    fun getAllPatterns(): Flow<List<RegexPattern>>
    suspend fun incrementSuccessCount(id: Long)
    suspend fun setPatternActive(id: Long, isActive: Boolean)
}
