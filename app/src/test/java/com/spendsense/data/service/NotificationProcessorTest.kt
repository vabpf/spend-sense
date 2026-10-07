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
import com.spendsense.data.local.SecurePreferences
import com.spendsense.domain.model.NotificationRoutingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationProcessorTest {

    private fun createFakePreferences(mode: NotificationRoutingMode): SecurePreferences {
        return object : SecurePreferences(ContextWrapper(null)) {
            override fun getNotificationRoutingMode(): NotificationRoutingMode = mode
        }
    }

    private fun createFakeAiParser(
        resultToReturn: DirectAiParseResult?,
        onCalled: (() -> Unit)? = null
    ): DirectAiNotificationParser {
        return object : DirectAiNotificationParser {
            override suspend fun parseNotification(
                title: String?,
                text: String,
                mode: NotificationRoutingMode
            ): DirectAiParseResult? {
                onCalled?.invoke()
                return resultToReturn
            }
        }
    }

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
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = createFakePreferences(NotificationRoutingMode.REGEX_ONLY),
            directAiNotificationParser = createFakeAiParser(null)
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
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = createFakePreferences(NotificationRoutingMode.REGEX_ONLY),
            directAiNotificationParser = createFakeAiParser(null)
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

    @Test
    fun testProcess_aiOnly_createsTransactionWhenAiDetectsTransaction() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.AI_ONLY)
        val aiParser = createFakeAiParser(
            DirectAiParseResult(
                isTransaction = true,
                amount = 150000.0,
                currency = "VND",
                merchant = "Shopee",
                categoryName = "Shopping"
            )
        )

        var insertedTransaction: Transaction? = null
        val repo = object : BaseTransactionRepository() {
            override suspend fun insertTransaction(transaction: Transaction): Long {
                insertedTransaction = transaction
                return 101L
            }
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = BaseNotificationPatternDao(),
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = repo,
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.shopee.app",
            appName = "Shopee",
            title = "Order update",
            text = "Payment of 150,000 VND successful for order #123",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.TRANSACTION_CREATED, result)
        assertEquals(150000.0, insertedTransaction?.amount ?: 0.0, 0.001)
        assertEquals("Shopee", insertedTransaction?.merchant)
        assertEquals("AI Detected", insertedTransaction?.notes)
        assertEquals("Bank Account", insertedTransaction?.paymentSourceType)
    }

    @Test
    fun testProcess_aiOnly_discardsWhenAiDetectsNonTransaction() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.AI_ONLY)
        val aiParser = createFakeAiParser(
            DirectAiParseResult(isTransaction = false)
        )

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = BaseNotificationPatternDao(),
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = BaseTransactionRepository(),
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Promo",
            text = "Apply for credit card today with 0% interest!",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.SILENT_SKIPPED, result)
    }

    @Test
    fun testProcess_aiOnly_savesToInboxWhenAiFails() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.AI_ONLY)
        val aiParser = createFakeAiParser(null) // Simulate timeout/offline failure

        var savedRawNotif: RawNotificationEntity? = null
        val rawDao = object : BaseRawNotificationDao() {
            override suspend fun insert(notification: RawNotificationEntity): Long {
                savedRawNotif = notification
                return 202L
            }
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = BaseNotificationPatternDao(),
            rawNotificationDao = rawDao,
            transactionRepository = BaseTransactionRepository(),
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Alert",
            text = "GD: -50,000VND tai POS 123",
            timestamp = 123456789L
        )

        assertEquals(ProcessResult.INBOX_CREATED, result)
        assertEquals("com.bank.app", savedRawNotif?.packageName)
        assertEquals("GD: -50,000VND tai POS 123", savedRawNotif?.text)
    }

    @Test
    fun testProcess_regexAndAi_newAppRoutesToAiAndSavesPattern() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.REGEX_AND_AI)
        val aiParser = createFakeAiParser(
            DirectAiParseResult(
                isTransaction = true,
                amount = 75000.0,
                currency = "VND",
                merchant = "Grab",
                paymentSource = "GrabPay",
                regex = ".*-(?<amount>[0-9,]+)\\s*VND.*"
            )
        )

        var savedPattern: NotificationPatternEntity? = null
        val patternDao = object : BaseNotificationPatternDao() {
            override suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity> = emptyList()
            override suspend fun upsert(pattern: NotificationPatternEntity): Long {
                savedPattern = pattern
                return 501L
            }
        }

        var insertedTx: Transaction? = null
        val repo = object : BaseTransactionRepository() {
            override suspend fun insertTransaction(transaction: Transaction): Long {
                insertedTx = transaction
                return 201L
            }
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = patternDao,
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = repo,
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.grabtaxi.passenger",
            appName = "Grab",
            title = "Receipt",
            text = "Your ride cost: -75,000 VND. Thank you!",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.TRANSACTION_CREATED, result)
        assertEquals(".*-(?<amount>[0-9,]+)\\s*VND.*", savedPattern?.regex)
        assertEquals("com.grabtaxi.passenger", savedPattern?.packageName)
        assertEquals("Bank Account", savedPattern?.paymentSourceType)
        assertTrue(savedPattern?.isTransaction == true)
        assertEquals("AI Learned", insertedTx?.notes)
        assertEquals("Bank Account", insertedTx?.paymentSourceType)
    }

    @Test
    fun testProcess_regexAndAi_unmatchedPatternRoutesToAi() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.REGEX_AND_AI)
        val aiParser = createFakeAiParser(
            DirectAiParseResult(
                isTransaction = true,
                amount = 200000.0,
                currency = "VND",
                merchant = "Highlands",
                regex = "-(?<amount>[0-9,]+)\\s*VND.*"
            )
        )

        val patternDao = object : BaseNotificationPatternDao() {
            override suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity> {
                return listOf(
                    NotificationPatternEntity(
                        id = 1L,
                        packageName = "com.bank.app",
                        notificationTitle = "Unrelated Pattern",
                        paymentSource = "",
                        regex = "BALANCE UPDATE:\\s*(?<amount>[0-9,]+)",
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
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Debit Alert",
            text = "-200,000 VND at Highlands Coffee",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.TRANSACTION_CREATED, result)
    }

    @Test
    fun testProcess_regexAndAi_nonTransactionDiscarded() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.REGEX_AND_AI)
        val aiParser = createFakeAiParser(
            DirectAiParseResult(isTransaction = false)
        )

        val patternDao = object : BaseNotificationPatternDao() {
            override suspend fun getAllForPackage(packageName: String): List<NotificationPatternEntity> = emptyList()
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = patternDao,
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = BaseTransactionRepository(),
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Security Note",
            text = "Never share your OTP with anyone.",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.SILENT_SKIPPED, result)
    }

    @Test
    fun testProcess_regexOnly_unmatchedSavesToInboxWithoutCallingAi() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.REGEX_ONLY)
        var aiWasCalled = false
        val aiParser = createFakeAiParser(null, onCalled = { aiWasCalled = true })

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = BaseNotificationPatternDao(),
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = BaseTransactionRepository(),
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.bank.app",
            appName = "Bank",
            title = "Notice",
            text = "-99,000 VND subscription",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.INBOX_CREATED, result)
        assertEquals(false, aiWasCalled)
    }

    @Test
    fun testProcess_aiPaymentSourceType_allowedTypeAndNotesInTransaction() = runBlocking {
        val prefs = createFakePreferences(NotificationRoutingMode.AI_ONLY)
        val aiParser = createFakeAiParser(
            DirectAiParseResult(
                isTransaction = true,
                amount = 45.0,
                currency = "USD",
                merchant = "Amazon",
                paymentSource = "Card 1234",
                paymentSourceType = "Credit Card",
                categoryName = "Shopping"
            )
        )

        var insertedTransaction: Transaction? = null
        val repo = object : BaseTransactionRepository() {
            override suspend fun insertTransaction(transaction: Transaction): Long {
                insertedTransaction = transaction
                return 102L
            }
        }

        val processor = NotificationProcessor(
            context = ContextWrapper(null),
            notificationPatternDao = BaseNotificationPatternDao(),
            rawNotificationDao = BaseRawNotificationDao(),
            transactionRepository = repo,
            categoryDao = BaseCategoryDao(),
            merchantCategoryMappingDao = BaseMerchantCategoryMappingDao(),
            whitelistedAppDao = BaseWhitelistedAppDao(),
            securePreferences = prefs,
            directAiNotificationParser = aiParser
        )

        val result = processor.process(
            packageName = "com.amazon.app",
            appName = "Amazon",
            title = "Order Shipped",
            text = "Your card was charged $45.00",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ProcessResult.TRANSACTION_CREATED, result)
        assertEquals(45.0, insertedTransaction?.amount ?: 0.0, 0.001)
        assertEquals("Credit Card", insertedTransaction?.paymentSourceType)
        assertEquals("AI Detected", insertedTransaction?.notes)
    }
}

