package com.spendsense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RegexPatternEntity::class,
        WhitelistedAppEntity::class,
        RawNotificationEntity::class,
        AiProviderEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SpendSenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun regexPatternDao(): RegexPatternDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun aiProviderDao(): AiProviderDao


    companion object {
        const val DATABASE_NAME = "spend_sense.db"
    }
}
