package com.spendsense.data.backup

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class BackupRestoreTest {

    private val gson = Gson()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> mockDao(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java)
        ) { _, _, _ -> null } as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> createUnsafeInstance(clazz: Class<T>): T {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val allocateInstanceMethod = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstanceMethod.invoke(unsafe, clazz) as T
    }

    private class DummySpendSenseDatabase : com.spendsense.data.local.SpendSenseDatabase() {
        override fun transactionDao() = TODO()
        override fun categoryDao() = TODO()
        override fun whitelistedAppDao() = TODO()
        override fun rawNotificationDao() = TODO()
        override fun merchantCategoryMappingDao() = TODO()
        override fun notificationPatternDao() = TODO()
        override fun exchangeRateDao() = TODO()
        override fun providerAccountDao() = TODO()
        override fun providerModelDao() = TODO()
        override fun aggregationDao() = TODO()
        override fun clearAllTables() {}
        override fun createInvalidationTracker() = TODO()
        override fun createOpenHelper(config: androidx.room.DatabaseConfiguration) = TODO()
    }

    @Test
    fun testSerializationAndDeserialization() {
        val payload = BackupPayload(
            version = 1,
            app = "SpendSense",
            exportedAt = 1758412800000L,
            exportedAtFormatted = "2026-09-21 07:45:00",
            preferences = BackupPreferences(
                defaultCurrency = "VND",
                dailyReportEnabled = true,
                dailyReportTime = "21:00",
                regexTitle = "MB Bank",
                regexText = "TK 1234 -50,000 VND",
                regexManualPattern = "-(?<amount>[0-9,]+)\\s*VND"
            ),
            categories = listOf(
                BackupCategory(
                    id = 1,
                    name = "Food",
                    iconName = "Restaurant",
                    colorHex = "#FF6B6B",
                    isDefault = true
                ),
                BackupCategory(
                    id = 2,
                    name = "Gaming",
                    iconName = "SportsEsports",
                    colorHex = "#9C27B0",
                    isDefault = false
                )
            ),
            notificationPatterns = listOf(
                BackupNotificationPattern(
                    id = 10,
                    packageName = "com.mbbank",
                    notificationTitle = "Notification",
                    paymentSource = "ATM Card",
                    paymentSourceType = "Debit Card",
                    regex = "-(?<amount>[0-9,]+)VND",
                    currencyCode = "VND",
                    isTransaction = true
                )
            ),
            transactions = listOf(
                BackupTransaction(
                    id = 100,
                    amount = 50000.0,
                    currencyCode = "VND",
                    merchant = "Coffee Shop",
                    categoryId = 1,
                    categoryName = "Food",
                    timestamp = 1758412800000L,
                    sourcePackageName = "com.mbbank",
                    sourceAppName = "MB Bank",
                    notes = "Iced Latte",
                    paymentSource = "ATM Card",
                    paymentSourceType = "Debit Card",
                    patternId = 10
                )
            ),
            whitelistedApps = listOf(
                BackupWhitelistedApp(
                    packageName = "com.mbbank",
                    appName = "MB Bank",
                    isEnabled = true
                )
            ),
            merchantCategoryMappings = listOf(
                BackupMerchantCategoryMapping(
                    merchant = "Coffee Shop",
                    categoryId = 1,
                    categoryName = "Food",
                    usageCount = 5
                )
            )
        )

        val json = gson.toJson(payload)
        assertNotNull(json)
        assertTrue(json.contains("SpendSense"))
        assertTrue(json.contains("Coffee Shop"))
        assertTrue(json.contains("Gaming"))

        val parsed = gson.fromJson(json, BackupPayload::class.java)
        assertEquals("SpendSense", parsed.app)
        assertEquals("VND", parsed.preferences?.defaultCurrency)
        assertEquals(2, parsed.categories.size)
        assertEquals("Gaming", parsed.categories[1].name)
        assertFalse(parsed.categories[1].isDefault)
        assertEquals(1, parsed.transactions.size)
        assertEquals(50000.0, parsed.transactions[0].amount, 0.001)
        assertEquals("Coffee Shop", parsed.transactions[0].merchant)
        assertEquals("Food", parsed.transactions[0].categoryName)
        assertEquals(1, parsed.notificationPatterns.size)
        assertEquals("-(?<amount>[0-9,]+)VND", parsed.notificationPatterns[0].regex)
    }

    @Test
    fun testParseBackupJsonValidation() {
        val manager = BackupRestoreManager(
            database = createUnsafeInstance(DummySpendSenseDatabase::class.java),
            transactionDao = mockDao(),
            categoryDao = mockDao(),
            notificationPatternDao = mockDao(),
            whitelistedAppDao = mockDao(),
            merchantCategoryMappingDao = mockDao(),
            providerAccountDao = mockDao(),
            providerModelDao = mockDao(),
            rawNotificationDao = mockDao(),
            securePreferences = createUnsafeInstance(com.spendsense.data.local.SecurePreferences::class.java)
        )

        // Invalid JSON string
        try {
            manager.parseBackupJson("not json")
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Invalid backup file"))
        }

        // Valid SpendSense backup string
        val validJson = """
            {
                "version": 1,
                "app": "SpendSense",
                "categories": [],
                "transactions": []
            }
        """.trimIndent()
        val parsed = manager.parseBackupJson(validJson)
        assertEquals("SpendSense", parsed.app)
    }
}
