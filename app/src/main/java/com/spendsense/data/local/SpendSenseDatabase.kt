package com.spendsense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.*
 
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WhitelistedAppEntity::class,
        RawNotificationEntity::class,
        AiProviderEntity::class,
        MerchantCategoryMappingEntity::class,
        NotificationPatternEntity::class,
        ExchangeRateEntity::class,
        ProviderAccountEntity::class,
        ProviderModelEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SpendSenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao
    abstract fun notificationPatternDao(): NotificationPatternDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun providerAccountDao(): ProviderAccountDao
    abstract fun providerModelDao(): ProviderModelDao

    companion object {
        const val DATABASE_NAME = "spend_sense.db"
    }
}
