package com.spendsense.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spendsense.data.local.entity.NotificationPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationPatternDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pattern: NotificationPatternEntity)

    @Query("SELECT * FROM notification_patterns WHERE packageName = :packageName AND notificationTitle = :title")
    suspend fun getByPackageAndTitle(packageName: String, title: String): NotificationPatternEntity?

    @Query("SELECT * FROM notification_patterns WHERE packageName = :packageName")
    suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity>

    @Query("SELECT * FROM notification_patterns ORDER BY packageName ASC, notificationTitle ASC")
    fun getAllFlow(): Flow<List<NotificationPatternEntity>>

    @Query("DELETE FROM notification_patterns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE notification_patterns SET regex = :regex, isTransaction = :isTransaction WHERE id = :id")
    suspend fun update(id: Long, regex: String?, isTransaction: Boolean)

    @Query("UPDATE notification_patterns SET packageName = :packageName, notificationTitle = :notificationTitle, regex = :regex, currencyCode = :currencyCode, isTransaction = :isTransaction WHERE id = :id")
    suspend fun updateAll(id: Long, packageName: String, notificationTitle: String, regex: String?, currencyCode: String, isTransaction: Boolean)
}
