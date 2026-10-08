package com.spendsense.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WhitelistedAppEntity::class,
        RawNotificationEntity::class,
        MerchantCategoryMappingEntity::class,
        NotificationPatternEntity::class,
        ExchangeRateEntity::class,
        ProviderAccountEntity::class,
        ProviderModelEntity::class,
        DailySpendingAggregationEntity::class,
        MonthlyCategoryAggregationEntity::class,
        MonthlyPaymentAggregationEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class SpendSenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun whitelistedAppDao(): WhitelistedAppDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao
    abstract fun notificationPatternDao(): NotificationPatternDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun providerAccountDao(): ProviderAccountDao
    abstract fun providerModelDao(): ProviderModelDao
    abstract fun aggregationDao(): AggregationDao

    companion object {
        const val DATABASE_NAME = "spend_sense.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_spending_aggregations` (
                        `dateKey` TEXT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `timestampDayStart` INTEGER NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `transactionCount` INTEGER NOT NULL,
                        PRIMARY KEY(`dateKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_spending_aggregations_year_month` ON `daily_spending_aggregations` (`year`, `month`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_spending_aggregations_timestampDayStart` ON `daily_spending_aggregations` (`timestampDayStart`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_category_aggregations` (
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `transactionCount` INTEGER NOT NULL,
                        PRIMARY KEY(`year`, `month`, `categoryId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monthly_category_aggregations_year_month` ON `monthly_category_aggregations` (`year`, `month`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monthly_category_aggregations_categoryId` ON `monthly_category_aggregations` (`categoryId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_payment_aggregations` (
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `paymentSource` TEXT NOT NULL,
                        `paymentSourceType` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `transactionCount` INTEGER NOT NULL,
                        PRIMARY KEY(`year`, `month`, `paymentSource`, `paymentSourceType`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monthly_payment_aggregations_year_month` ON `monthly_payment_aggregations` (`year`, `month`)")
            }
        }
    }
}
