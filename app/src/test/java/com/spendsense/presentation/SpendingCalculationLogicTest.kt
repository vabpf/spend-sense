package com.spendsense.presentation

import com.spendsense.domain.calculation.ForecastEngine
import com.spendsense.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.math.roundToInt

class SpendingCalculationLogicTest {

    @Test
    fun testDailyAverage_usesDaysElapsedUpToCurrentDay() {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val monthTotal = 400.0

        val dailyAverage = if (currentDay > 0) monthTotal / currentDay else 0.0
        val expected = 400.0 / currentDay

        assertEquals(expected, dailyAverage, 0.001)
    }

    @Test
    fun testHomeSummaryTrend_comparesWithCurrentDailyAverage() {
        val dailyAverage = 50.0
        val todaySpendingHigher = 75.0
        val todaySpendingLower = 30.0

        val percentDiffHigher = (((todaySpendingHigher - dailyAverage) / dailyAverage) * 100).roundToInt()
        val percentDiffLower = (((todaySpendingLower - dailyAverage) / dailyAverage) * 100).roundToInt()

        assertEquals(50, percentDiffHigher) // +50% above average
        assertEquals(-40, percentDiffLower) // -40% below average
    }

    @Test
    fun testChartsMonthToDateComparison_filtersPreviousMonthByCutoff() {
        val currentNow = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 8, 15, 0, 0)
        }

        val prevCutoffCal = (currentNow.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val prevCutoffMillis = prevCutoffCal.timeInMillis

        // Transaction on Sept 5 (before cutoff) -> should be included
        val txnBeforeCutoff = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5, 12, 0, 0)
        }.timeInMillis

        // Transaction on Sept 8 at 10:00 (before 15:00 cutoff) -> should be included
        val txnSameDayBeforeCutoff = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 8, 10, 0, 0)
        }.timeInMillis

        // Transaction on Sept 15 (after cutoff) -> should be excluded for MTD
        val txnAfterCutoff = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        }.timeInMillis

        assertTrue(txnBeforeCutoff <= prevCutoffMillis)
        assertTrue(txnSameDayBeforeCutoff <= prevCutoffMillis)
        assertTrue(txnAfterCutoff > prevCutoffMillis)

        val septTxns = listOf(
            Pair(txnBeforeCutoff, 100.0),
            Pair(txnSameDayBeforeCutoff, 50.0),
            Pair(txnAfterCutoff, 200.0)
        )

        val mtdLastMonth = septTxns.filter { it.first <= prevCutoffMillis }.sumOf { it.second }
        val fullLastMonth = septTxns.sumOf { it.second }

        assertEquals(150.0, mtdLastMonth, 0.001)
        assertEquals(350.0, fullLastMonth, 0.001)
    }

    @Test
    fun testForecast_day1_shrinksTowardsPriorMonthBaseline() {
        // Day 1: user spent $100
        val day1Txn = Transaction(
            id = 1L,
            amount = 100.0,
            currencyCode = "USD",
            merchant = "Grocery Store",
            categoryId = 1L,
            timestamp = 1000L,
            sourcePackageName = "com.test",
            sourceAppName = "Test"
        )
        // Prior month: $1,500 over 30 days ($50/day)
        val result = ForecastEngine.calculateMonthForecast(
            currentMonthTransactions = listOf(day1Txn),
            daysElapsed = 1,
            totalDaysInMonth = 31,
            priorMonthTotal = 1500.0,
            priorMonthTotalDays = 30
        )

        // w1 = 1 / (1 + 4) = 0.20
        // burnRate = 0.20 * 100 + 0.80 * 50 = $60.0/day
        // projectedRemaining = 60.0 * 30 days = $1800.0
        // projectedTotal = 100.0 + 1800.0 = $1900.0 (instead of naive $3100!)
        assertEquals(60.0, result.variableDailyBurnRate, 0.001)
        assertEquals(1800.0, result.projectedRemainingSpend, 0.001)
        assertEquals(1900.0, result.projectedMonthEndTotal, 0.001)
        assertTrue(result.isStabilizingWithPrior)
    }

    @Test
    fun testForecast_spikeIsolation_excludesAdHocExpenseFromProjectionRate() {
        // 10 transactions of $30-$50 totaling $400
        val regularTxns = (1..10).map { i ->
            Transaction(
                id = i.toLong(),
                amount = 40.0,
                currencyCode = "USD",
                merchant = "Daily Meal $i",
                categoryId = 1L,
                timestamp = 1000L * i,
                sourcePackageName = "com.test",
                sourceAppName = "Test"
            )
        }
        // 1 large ad-hoc spike: $400 car repair
        val spikeTxn = Transaction(
            id = 99L,
            amount = 400.0,
            currencyCode = "USD",
            merchant = "Auto Repair Garage",
            categoryId = 2L,
            timestamp = 15000L,
            sourcePackageName = "com.test",
            sourceAppName = "Test"
        )

        val allTxns = regularTxns + spikeTxn

        val result = ForecastEngine.calculateMonthForecast(
            currentMonthTransactions = allTxns,
            daysElapsed = 10,
            totalDaysInMonth = 30,
            priorMonthTotal = 0.0 // no prior month for pure variable rate test
        )

        // Spend so far = 10 * 40 + 400 = $800
        assertEquals(800.0, result.spendToDate, 0.001)
        // Spike $400 was detected and isolated
        assertEquals(1, result.isolatedSpikesCount)
        assertEquals(400.0, result.isolatedSpikesTotal, 0.001)
        // Variable spend is $400 over 10 days = $40/day
        assertEquals(40.0, result.variableDailyBurnRate, 0.001)
        // 20 remaining days projected at $40/day = $800
        assertEquals(800.0, result.projectedRemainingSpend, 0.001)
        // Total forecast = $800 spent so far + $800 projected remaining = $1600 (not naive $2400!)
        assertEquals(1600.0, result.projectedMonthEndTotal, 0.001)
    }

    @Test
    fun testForecast_endOfMonth_convergesToSpendToDate() {
        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 250.0,
                currencyCode = "USD",
                merchant = "Store",
                categoryId = 1L,
                timestamp = 1000L,
                sourcePackageName = "com.test",
                sourceAppName = "Test"
            )
        )
        // Last day of the month: daysElapsed = 30, totalDays = 30
        val result = ForecastEngine.calculateMonthForecast(
            currentMonthTransactions = txns,
            daysElapsed = 30,
            totalDaysInMonth = 30,
            priorMonthTotal = 500.0
        )

        assertEquals(0, result.daysRemaining)
        assertEquals(0.0, result.projectedRemainingSpend, 0.001)
        assertEquals(250.0, result.projectedMonthEndTotal, 0.001)
    }

    @Test
    fun testForecast_safeRemainingDailyPace_calculatesCorrectBudget() {
        val txns = listOf(
            Transaction(
                id = 1L,
                amount = 600.0,
                currencyCode = "USD",
                merchant = "Spend",
                categoryId = 1L,
                timestamp = 1000L,
                sourcePackageName = "com.test",
                sourceAppName = "Test"
            )
        )
        // Day 15 of 30. Remaining 15 days. Prior month total = $1,500.
        val result = ForecastEngine.calculateMonthForecast(
            currentMonthTransactions = txns,
            daysElapsed = 15,
            totalDaysInMonth = 30,
            priorMonthTotal = 1500.0
        )

        // Safe pace: ($1500 target - $600 spent) / 15 remaining days = $60/day
        assertEquals(60.0, result.safeRemainingDailyPace ?: 0.0, 0.001)
    }
}
