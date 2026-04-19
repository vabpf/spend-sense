package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.AiProviderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: AiProviderEntity): Long

    @Update
    suspend fun update(provider: AiProviderEntity)

    @Delete
    suspend fun delete(provider: AiProviderEntity)

    @Query("SELECT * FROM ai_providers")
    fun getAllProvidersFlow(): Flow<List<AiProviderEntity>>

    @Query("SELECT * FROM ai_providers WHERE jobType = :jobType LIMIT 1")
    suspend fun getProviderForJob(jobType: String): AiProviderEntity?
}
