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
}
