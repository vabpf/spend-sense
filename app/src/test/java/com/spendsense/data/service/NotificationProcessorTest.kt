package com.spendsense.data.service

import android.content.ContextWrapper
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.domain.model.Transaction
import com.spendsense.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationProcessorTest {

    private open class BaseNotificationPatternDao : NotificationPatternDao {
        override suspend fun upsert(pattern: NotificationPatternEntity): Long = 1L
        override suspend fun insertAll(patterns: List<NotificationPatternEntity>) {}
        override suspend fun getByPackageAndTitle(packageName: String, title: String): NotificationPatternEntity? = null
        override suspend fun getByPackageTitleAndSource(packageName: String, title: String, paymentSource: String): NotificationPatternEntity? = null
        override suspend fun getById(id: Long): NotificationPatternEntity? = null
        override suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity> = emptyList()
        override fun getAllFlow(): Flow<List<NotificationPatternEntity>> = emptyFlow()
        override suspend fun getAll(): List<NotificationPatternEntity> = emptyList()
        override suspend fun deleteById(id: Long) {}
        override suspend fun deleteAll() {}
        override suspend fun update(id: Long, regex: String?, isTransaction: Boolean) {}
        override suspend fun updateAll(id: Long, packageName: String, notificationTitle: String, paymentSource: String, paymentSourceType: String, regex: String?, currencyCode: String, isTransaction: Boolean) {}
    }

    private open class BaseRawNotificationDao : RawNotificationDao {
        override suspend fun insert(notification: RawNotificationEntity): Long = 1L
        override suspend fun update(notification: RawNotificationEntity) {}
        override suspend fun delete(notification: RawNotificationEntity) {}
        override fun getUnprocessedNotificationsFlow(): Flow<List<RawNotificationEntity>> = emptyFlow()
        override suspend fun markAsProcessed(id: Long) {}
        override suspend fun getById(id: Long): RawNotificationEntity? = null
        override suspend fun getUnprocessedForPackageAndTitle(packageName: String, title: String): List<RawNotificationEntity> = emptyList()
        override suspend fun getUnprocessedForPackage(packageName: String): List<RawNotificationEntity> = emptyList()
        override suspend fun getProcessedForPackage(packageName: String): List<RawNotificationEntity> = emptyList()
        override suspend fun deleteById(id: Long) {}
        override suspend fun pruneProcessedForPackage(packageName: String, limit: Int) {}
        override suspend fun getAll(): List<RawNotificationEntity> = emptyList()
        override suspend fun insertAll(notifications: List<RawNotificationEntity>) {}
        override suspend fun deleteAll() {}
        override suspend fun deleteAllUnprocessed() {}
    }

    private open class BaseTransactionRepository : TransactionRepository {
        override suspend fun insertTransaction(transaction: Transaction): Long = 1L
        override suspend fun updateTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun getTransactionById(id: Long): Transaction? = null
        override fun getAllTransactions(): Flow<List<Transaction>> = emptyFlow()
        override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = emptyFlow()
        override fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> = emptyFlow()
    }

    private open class BaseCategoryDao : CategoryDao {
        override suspend fun insert(category: com.spendsense.data.local.entity.CategoryEntity): Long = 1L
        override suspend fun insertAll(categories: List<com.spendsense.data.local.entity.CategoryEntity>) {}
        override suspend fun update(category: com.spendsense.data.local.entity.CategoryEntity) {}
        override suspend fun delete(category: com.spendsense.data.local.entity.CategoryEntity) {}
        override suspend fun getById(id: Long): com.spendsense.data.local.entity.CategoryEntity? = null
        override fun getAllFlow(): Flow<List<com.spendsense.data.local.entity.CategoryEntity>> = emptyFlow()
        override suspend fun getAll(): List<com.spendsense.data.local.entity.CategoryEntity> = emptyList()
        override suspend fun getDefaultCategories(): List<com.spendsense.data.local.entity.CategoryEntity> = emptyList()
        override suspend fun deleteAll() {}
    }

    private open class BaseMerchantCategoryMappingDao : MerchantCategoryMappingDao {
        override suspend fun upsert(mapping: com.spendsense.data.local.entity.MerchantCategoryMappingEntity) {}
        override suspend fun insertAll(mappings: List<com.spendsense.data.local.entity.MerchantCategoryMappingEntity>) {}
        override suspend fun getAll(): List<com.spendsense.data.local.entity.MerchantCategoryMappingEntity> = emptyList()
        override suspend fun deleteAll() {}
        override suspend fun getByMerchant(merchant: String): com.spendsense.data.local.entity.MerchantCategoryMappingEntity? = null
    }

    private open class BaseWhitelistedAppDao : WhitelistedAppDao {
        override suspend fun insert(app: com.spendsense.data.local.entity.WhitelistedAppEntity) {}
        override suspend fun insertAll(apps: List<com.spendsense.data.local.entity.WhitelistedAppEntity>) {}
        override suspend fun update(app: com.spendsense.data.local.entity.WhitelistedAppEntity) {}
        override suspend fun delete(app: com.spendsense.data.local.entity.WhitelistedAppEntity) {}
        override suspend fun getByPackageName(packageName: String): com.spendsense.data.local.entity.WhitelistedAppEntity? = null
        override suspend fun getEnabledApps(): List<com.spendsense.data.local.entity.WhitelistedAppEntity> = emptyList()
        override fun getEnabledAppsFlow(): Flow<List<com.spendsense.data.local.entity.WhitelistedAppEntity>> = emptyFlow()
        override fun getAllFlow(): Flow<List<com.spendsense.data.local.entity.WhitelistedAppEntity>> = emptyFlow()
        override suspend fun getAll(): List<com.spendsense.data.local.entity.WhitelistedAppEntity> = emptyList()
        override suspend fun deleteAll() {}
        override suspend fun setEnabled(packageName: String, isEnabled: Boolean) {}
    }

    @Test
    fun testProcess_matchesPattern_whenPaymentSourceIsBlank() = runBlocking {
        val patternDao = object : BaseNotificationPatternDao() {
            override suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity> {
                return listOf(
                    NotificationPatternEntity(
                        id = 1L,
                        packageName = "com.bank.app",
                        notificationTitle = "Bank Alert",
                        paymentSource = "",
                        regex = "-(?<amount>[0-9,]+)\\s*VND\\s*at\\s*(?<merchant>[^.]+)",
                        isTransaction = true
                    )
                )
            }
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = patternDao,
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = BaseTransactionRepository(),
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao()
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Bank Alert",
            text = "Tai khoan 123: -50,000 VND at Highlands Coffee.",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.TRANSACTION_CREATED, result)
    }

    @Test
    fun testProcess_matchesPattern_whenPaymentSourceDoesNotAppearInText() = runBlocking {
        val patternDao = object : BaseNotificationPatternDao() {
            override suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity> {
                return listOf(
                    NotificationPatternEntity(
                        id = 1L,
                        packageName = "com.bank.app",
                        notificationTitle = "Bank Alert",
                        paymentSource = "Credit Card Label",
                        regex = "-(?<amount>[0-9,]+)\\s*VND\\s*at\\s*(?<merchant>[^.]+)",
                        isTransaction = true
                    )
                )
            }
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = patternDao,
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = BaseTransactionRepository(),
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao()
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Bank Alert",
            text = "-25,000 VND at Circle K.",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.TRANSACTION_CREATED, result)
    }
}
