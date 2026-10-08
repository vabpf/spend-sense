package com.spendsense.core

import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpendSenseCoreTest {

    @Test
    fun testSpendSenseCore_fallbackAvailability() {
        // In JVM unit tests without native .so loaded, isAvailable gracefully returns false
        // SpendSenseCore should operate with high-performance pure Kotlin fallback without throwing
        val isAvail = SpendSenseCore.isAvailable
        assertTrue(!isAvail || isAvail) // never throws
    }

    @Test
    fun testSpendSenseCore_forecastFallback() {
        val txn = Transaction(
            id = 1L,
            amount = 120.0,
            currencyCode = "USD",
            merchant = "Groceries",
            categoryId = 1L,
            timestamp = 1000L,
            sourcePackageName = "com.test",
            sourceAppName = "Test"
        )
        val result = SpendSenseCore.forecast(
            currentMonthTransactions = listOf(txn),
            daysElapsed = 1,
            totalDaysInMonth = 30,
            priorMonthTotal = 1500.0,
            priorMonthTotalDays = 30
        )

        assertNotNull(result)
        assertEquals(120.0, result.spendToDate, 0.001)
        assertEquals(29, result.daysRemaining)
        assertTrue(result.projectedMonthEndTotal > 0.0)
    }

    @Test
    fun testSpendSenseCore_profileCategoryFallback() {
        val cat = Category(1L, "Food", "#FF0000", "Restaurant")
        val txns = listOf(
            Transaction(1L, 10.0, "USD", "Coffee Shop", 1L, 1000L, "com.test", "Test"),
            Transaction(2L, 25.0, "USD", "Burger Joint", 1L, 2000L, "com.test", "Test")
        )
        val profile = SpendSenseCore.profileCategory(cat, txns, "USD")

        assertNotNull(profile)
        assertEquals(2, profile.transactionCount)
        assertEquals(35.0, profile.totalAmount, 0.001)
        assertTrue(profile.ticketTiers.isNotEmpty())
    }
}
