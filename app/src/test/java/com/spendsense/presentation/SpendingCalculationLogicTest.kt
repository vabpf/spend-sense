package com.spendsense.presentation

import com.spendsense.domain.calculation.CreditStatementEngine
import com.spendsense.domain.calculation.ForecastEngine
import com.spendsense.domain.model.CreditCardConfig
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

    @Test
    fun testCreditCycle_currentDayBeforeClosing_calculatesActiveAndClosedWindows() {
        val config = CreditCardConfig(
            cardName = "Chase",
            statementClosingDay = 15,
            paymentDueDay = 5
        )

        // Current time: Oct 8, 2026 12:00:00 (day 8 <= statementDay 15)
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 8, 12, 0, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // Txn in Active window (Sep 16 - Oct 15): Sep 20 ($200) + Oct 5 ($300)
        val txnActive1 = Transaction(
            id = 1L, amount = 200.0, currencyCode = "USD", merchant = "Store A",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 20, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )
        val txnActive2 = Transaction(
            id = 2L, amount = 300.0, currencyCode = "USD", merchant = "Store B",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 5, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )

        // Txn in Closed window (Aug 16 - Sep 15): Aug 25 ($500) + Sep 10 ($400)
        val txnClosed1 = Transaction(
            id = 3L, amount = 500.0, currencyCode = "USD", merchant = "Store C",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 25, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )
        val txnClosed2 = Transaction(
            id = 4L, amount = 400.0, currencyCode = "USD", merchant = "Store D",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )

        val txns = listOf(txnActive1, txnActive2, txnClosed1, txnClosed2)
        val cycle = CreditStatementEngine.calculateCardCycle(config, txns, nowMillis)

        // Active balance: 200 + 300 = $500
        assertEquals(500.0, cycle.activeStatementBalance, 0.001)
        // Closed bill: 500 + 400 = $900 (due soon!)
        assertEquals(900.0, cycle.closedStatementBalance, 0.001)
        // Days until Oct 15 closing: Oct 8 to Oct 15 = 7 days
        assertEquals(7, cycle.daysUntilActiveCloses)
    }

    @Test
    fun testCreditCycle_currentDayAfterClosing_rollsOverToNextMonthCycle() {
        val config = CreditCardConfig(
            cardName = "Chase",
            statementClosingDay = 15
        )

        // Current time: Oct 20, 2026 (day 20 > statementDay 15)
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 20, 12, 0, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // Txn in Active window (Oct 16 - Nov 15): Oct 18 ($150)
        val txnActive = Transaction(
            id = 1L, amount = 150.0, currencyCode = "USD", merchant = "Store A",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 18, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )

        // Txn in Closed window (Sep 16 - Oct 15): Oct 5 ($350)
        val txnClosed = Transaction(
            id = 2L, amount = 350.0, currencyCode = "USD", merchant = "Store B",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 5, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )

        val cycle = CreditStatementEngine.calculateCardCycle(config, listOf(txnActive, txnClosed), nowMillis)

        assertEquals(150.0, cycle.activeStatementBalance, 0.001)
        assertEquals(350.0, cycle.closedStatementBalance, 0.001)
    }

    @Test
    fun testCreditLiquiditySummary_aggregatesMultipleCards() {
        val chaseConfig = CreditCardConfig("Chase", 15)
        val amexConfig = CreditCardConfig("Amex", 25)

        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 8, 12, 0, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // Chase: Active $200, Closed $500
        val txnChaseActive = Transaction(
            id = 1L, amount = 200.0, currencyCode = "USD", merchant = "M1",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 5, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )
        val txnChaseClosed = Transaction(
            id = 2L, amount = 500.0, currencyCode = "USD", merchant = "M2",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 10, 10, 0) }.timeInMillis,
            paymentSource = "Chase", sourcePackageName = "com.test", sourceAppName = "Test"
        )

        // Amex: Active $400, Closed $800
        val txnAmexActive = Transaction(
            id = 3L, amount = 400.0, currencyCode = "USD", merchant = "M3",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 2, 10, 0) }.timeInMillis,
            paymentSource = "Amex", sourcePackageName = "com.test", sourceAppName = "Test"
        )
        val txnAmexClosed = Transaction(
            id = 4L, amount = 800.0, currencyCode = "USD", merchant = "M4",
            categoryId = 1L,
            timestamp = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 20, 10, 0) }.timeInMillis,
            paymentSource = "Amex", sourcePackageName = "com.test", sourceAppName = "Test"
        )

        val summary = CreditStatementEngine.calculateLiquiditySummary(
            configs = listOf(chaseConfig, amexConfig),
            allTransactions = listOf(txnChaseActive, txnChaseClosed, txnAmexActive, txnAmexClosed),
            currentMonthTransactions = listOf(txnChaseActive, txnAmexActive),
            currentTimestamp = nowMillis
        )

        // Total immediate cash needed: $500 (Chase) + $800 (Amex) = $1,300
        assertEquals(1300.0, summary.totalImmediateCashNeeded, 0.001)
        // Total active floating debt: $200 (Chase) + $400 (Amex) = $600
        assertEquals(600.0, summary.totalActiveFloatingDebt, 0.001)
    }
}
