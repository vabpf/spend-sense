package com.spendsense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendsense.data.local.entity.ProviderModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderModelDao {

    @Query("SELECT * FROM provider_models WHERE providerAccountId = :accountId ORDER BY displayName ASC, modelId ASC")
    fun getByAccountIdFlow(accountId: Long): Flow<List<ProviderModelEntity>>

    @Query("SELECT * FROM provider_models WHERE providerAccountId = :accountId ORDER BY displayName ASC, modelId ASC")
    suspend fun getByAccountId(accountId: Long): List<ProviderModelEntity>

    @Query("SELECT * FROM provider_models WHERE isEnabled = 1 ORDER BY displayName ASC, modelId ASC")
    suspend fun getEnabledModels(): List<ProviderModelEntity>

    @Query("SELECT * FROM provider_models WHERE isEnabled = 1 ORDER BY displayName ASC, modelId ASC")
    fun getEnabledModelsFlow(): Flow<List<ProviderModelEntity>>

    @Query("UPDATE provider_models SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM provider_models WHERE providerAccountId = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: ProviderModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(models: List<ProviderModelEntity>)

    @Query("SELECT COUNT(*) FROM provider_models")
    fun onModelsChanged(): Flow<Int>
}
