package com.spendsense.data.local.dao

import androidx.room.*
import com.spendsense.data.local.entity.RawNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: RawNotificationEntity): Long

    @Update
    suspend fun update(notification: RawNotificationEntity)

    @Delete
    suspend fun delete(notification: RawNotificationEntity)

    @Query("SELECT * FROM raw_notifications WHERE isProcessed = 0 ORDER BY timestamp DESC")
    fun getUnprocessedNotificationsFlow(): Flow<List<RawNotificationEntity>>

    @Query("UPDATE raw_notifications SET isProcessed = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: Long)

    @Query("SELECT * FROM raw_notifications WHERE id = :id")
    suspend fun getById(id: Long): RawNotificationEntity?

    @Query("SELECT * FROM raw_notifications WHERE isProcessed = 0 AND packageName = :packageName AND title = :title")
    suspend fun getUnprocessedForPackageAndTitle(packageName: String, title: String): List<RawNotificationEntity>

    @Query("SELECT * FROM raw_notifications WHERE isProcessed = 1 AND packageName = :packageName ORDER BY timestamp DESC")
    suspend fun getProcessedForPackage(packageName: String): List<RawNotificationEntity>

    @Query("DELETE FROM raw_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        DELETE FROM raw_notifications 
        WHERE isProcessed = 1 AND packageName = :packageName AND id NOT IN (
            SELECT id FROM raw_notifications 
            WHERE isProcessed = 1 AND packageName = :packageName 
            ORDER BY timestamp DESC 
            LIMIT :limit
        )
    """)
    suspend fun pruneProcessedForPackage(packageName: String, limit: Int = 100)

    @Query("DELETE FROM raw_notifications WHERE isProcessed = 0")
    suspend fun deleteAllUnprocessed()
}
