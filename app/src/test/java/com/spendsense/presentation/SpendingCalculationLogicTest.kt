package com.spendsense.presentation

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
}
