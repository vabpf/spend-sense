package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.WhitelistedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: WhitelistedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<WhitelistedAppEntity>)

    @Update
    suspend fun update(app: WhitelistedAppEntity)

    @Delete
    suspend fun delete(app: WhitelistedAppEntity)

    @Query("SELECT * FROM whitelisted_apps WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): WhitelistedAppEntity?

    @Query("SELECT * FROM whitelisted_apps WHERE isEnabled = 1")
    suspend fun getEnabledApps(): List<WhitelistedAppEntity>

    @Query("SELECT * FROM whitelisted_apps ORDER BY appName ASC")
    fun getAllFlow(): Flow<List<WhitelistedAppEntity>>

    @Query("UPDATE whitelisted_apps SET isEnabled = :isEnabled WHERE packageName = :packageName")
    suspend fun setEnabled(packageName: String, isEnabled: Boolean)
}
