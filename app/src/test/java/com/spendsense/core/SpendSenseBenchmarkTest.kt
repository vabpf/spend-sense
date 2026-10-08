package com.spendsense.core

import com.spendsense.domain.calculation.CategoryBehaviorProfiler
import com.spendsense.domain.calculation.ForecastEngine
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureTimeMillis

class SpendSenseBenchmarkTest {

    @Test
    fun benchmark_forecast_1000_iterations() {
        val transactions = (0..50).map { i ->
            Transaction(
                id = i.toLong(),
                amount = 15.0 + (i % 40),
                currencyCode = "USD",
                merchant = "Merchant $i",
                categoryId = (i % 5).toLong(),
                timestamp = 1791417600000L - (i * 3600_000L),
                sourcePackageName = "com.test",
                sourceAppName = "Test"
            )
        }

        // Warmup
        repeat(50) {
            SpendSenseCore.forecast(transactions, 15, 30, 1500.0, 30)
        }

        val elapsed = measureTimeMillis {
            repeat(1000) {
                val result = SpendSenseCore.forecast(transactions, 15, 30, 1500.0, 30)
                assertNotNull(result)
            }
        }

        println(">>> Kotlin/Bridge Forecast (1000 iterations): ${elapsed}ms")
        assertTrue(elapsed < 2000) // comfortably under 2s on any test runner
    }

    @Test
    fun benchmark_category_profiler_1000_transactions() {
        val testCat = Category(1L, "Food", "#FF5722", "Restaurant")
        val transactions = (0 until 1000).map { i ->
            Transaction(
                id = i.toLong(),
                amount = 5.0 + (i % 100),
                currencyCode = "USD",
                merchant = "Venue ${i % 10}",
                categoryId = 1L,
                timestamp = 1791417600000L - (i * 60_000L),
                sourcePackageName = "com.test",
                sourceAppName = "Test"
            )
        }

        val elapsed = measureTimeMillis {
            val profile = SpendSenseCore.profileCategory(testCat, transactions, "USD")
            assertEquals(1000, profile.transactionCount)
            assertTrue(profile.ticketTiers.isNotEmpty())
            assertTrue(profile.topMerchants.isNotEmpty())
        }

        println(">>> Category Profiler (1000 txns): ${elapsed}ms")
        assertTrue(elapsed < 500)
    }

    @Test
    fun test_forecast_pure_kotlin_vs_spendsense_core_parity() {
        val transactions = (0..20).map { i ->
            Transaction(
                id = i.toLong(),
                amount = 20.0 + i * 5,
                currencyCode = "USD",
                merchant = "Shop $i",
                categoryId = 1L,
                timestamp = 1791417600000L,
                sourcePackageName = "com.test",
                sourceAppName = "Test"
            )
        }

        val directResult = ForecastEngine.calculateMonthForecastPureKotlin(transactions, 10, 30, 1200.0, 30)
        val facadeResult = SpendSenseCore.forecast(transactions, 10, 30, 1200.0, 30)

        assertEquals(directResult.spendToDate, facadeResult.spendToDate, 0.001)
        assertEquals(directResult.projectedMonthEndTotal, facadeResult.projectedMonthEndTotal, 0.001)
        assertEquals(directResult.daysRemaining, facadeResult.daysRemaining)
    }
}
