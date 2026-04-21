package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.RegexPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexPatternDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pattern: RegexPatternEntity): Long

    @Update
    suspend fun update(pattern: RegexPatternEntity)

    @Delete
    suspend fun delete(pattern: RegexPatternEntity)

    @Query("SELECT * FROM regex_patterns WHERE id = :id")
    suspend fun getById(id: Long): RegexPatternEntity?

    @Query("SELECT * FROM regex_patterns WHERE isActive = 1 ORDER BY successCount DESC")
    suspend fun getActivePatterns(): List<RegexPatternEntity>

    @Query("SELECT * FROM regex_patterns WHERE (packageName = :packageName OR packageName = :allWhitelistedPackage) AND isActive = 1")
    suspend fun getActivePatternsForPackage(
        packageName: String,
        allWhitelistedPackage: String
    ): List<RegexPatternEntity>

    @Query("SELECT * FROM regex_patterns ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<RegexPatternEntity>>

    @Query("UPDATE regex_patterns SET lastUsed = :timestamp, successCount = successCount + 1 WHERE id = :id")
    suspend fun incrementSuccessCount(id: Long, timestamp: Long)

    @Query("UPDATE regex_patterns SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)
}
