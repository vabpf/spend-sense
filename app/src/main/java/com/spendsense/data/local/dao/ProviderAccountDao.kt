package com.spendsense.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendsense.data.local.entity.ProviderAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: ProviderAccountEntity): Long

    @Delete
    suspend fun delete(account: ProviderAccountEntity)

    @Query("SELECT * FROM provider_accounts ORDER BY name ASC")
    fun getAllFlow(): Flow<List<ProviderAccountEntity>>

    @Query("SELECT * FROM provider_accounts ORDER BY name ASC")
    suspend fun getAll(): List<ProviderAccountEntity>

    @Query("SELECT * FROM provider_accounts WHERE id = :id")
    suspend fun getById(id: Long): ProviderAccountEntity?

    @Query("SELECT * FROM provider_accounts WHERE jobType = :jobType")
    suspend fun getByJobType(jobType: String): List<ProviderAccountEntity>
}
