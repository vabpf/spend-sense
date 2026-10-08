package com.spendsense.domain.calculation

import com.spendsense.core.SpendSenseCore
import com.spendsense.domain.model.Transaction
import kotlin.math.roundToInt

/**
 * Result model representing the month-end spending forecast computed via
 * Robust Empirical-Bayes Decomposed Forecast (REBD).
 */
data class MonthForecastResult(
    val projectedMonthEndTotal: Double,
    val spendToDate: Double,
    val daysElapsed: Int,
    val totalDaysInMonth: Int,
    val daysRemaining: Int,
    val variableDailyBurnRate: Double,
    val isolatedSpikesTotal: Double,
    val isolatedSpikesCount: Int,
    val projectedRemainingSpend: Double,
    val safeRemainingDailyPace: Double?,
    val percentVsLastMonth: Int?,
    val isStabilizingWithPrior: Boolean
)

/**
 * Pure calculation engine for Month-End Spending Forecasting using the
 * Robust Empirical-Bayes Decomposed Forecast (REBD) algorithm.
 *
 * 1. Outlier Fence (Tukey's Non-Parametric): Identifies large, irregular one-offs
 *    (e.g. vehicle repair, healthcare, sudden bills) and removes them from the
 *    multiplying daily projection rate (while preserving them in spendToDate).
 * 2. Bayesian Shrinkage: For early month (days 1-4), smoothly shrinks the variable
 *    burn rate towards the prior month's daily baseline to prevent wild swings.
 * 3. Assembly: Projects remaining days and derives actionable safe spending pace.
 *
 * Automatically delegates to high-performance Rust core (via [SpendSenseCore])
 * when native libraries are loaded, falling back seamlessly to pure Kotlin.
 */
object ForecastEngine {

    fun calculateMonthForecast(
        currentMonthTransactions: List<Transaction>,
        daysElapsed: Int,
        totalDaysInMonth: Int,
        priorMonthTotal: Double = 0.0,
        priorMonthTotalDays: Int = 30,
        spendingTarget: Double? = null
    ): MonthForecastResult {
        return SpendSenseCore.forecast(
            currentMonthTransactions = currentMonthTransactions,
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDaysInMonth,
            priorMonthTotal = priorMonthTotal,
            priorMonthTotalDays = priorMonthTotalDays,
            spendingTarget = spendingTarget
        )
    }

    internal fun calculateMonthForecastPureKotlin(
        currentMonthTransactions: List<Transaction>,
        daysElapsed: Int,
        totalDaysInMonth: Int,
        priorMonthTotal: Double = 0.0,
        priorMonthTotalDays: Int = 30,
        spendingTarget: Double? = null
    ): MonthForecastResult {
        val safeElapsed = daysElapsed.coerceIn(1, totalDaysInMonth)
        val daysRemaining = (totalDaysInMonth - safeElapsed).coerceAtLeast(0)
        val spendToDate = currentMonthTransactions.sumOf { it.amount }

        if (currentMonthTransactions.isEmpty()) {
            val priorDailyRate = if (priorMonthTotalDays > 0) priorMonthTotal / priorMonthTotalDays else 0.0
            val projectedRemaining = priorDailyRate * daysRemaining
            val benchmark = spendingTarget ?: priorMonthTotal.takeIf { it > 0.0 }
            val safePace = benchmark?.let { if (daysRemaining > 0) (it / daysRemaining).coerceAtLeast(0.0) else 0.0 }

            return MonthForecastResult(
                projectedMonthEndTotal = projectedRemaining,
                spendToDate = 0.0,
                daysElapsed = safeElapsed,
                totalDaysInMonth = totalDaysInMonth,
                daysRemaining = daysRemaining,
                variableDailyBurnRate = priorDailyRate,
                isolatedSpikesTotal = 0.0,
                isolatedSpikesCount = 0,
                projectedRemainingSpend = projectedRemaining,
                safeRemainingDailyPace = safePace,
                percentVsLastMonth = if (priorMonthTotal > 0.0) {
                    (((projectedRemaining - priorMonthTotal) / priorMonthTotal) * 100).roundToInt()
                } else null,
                isStabilizingWithPrior = safeElapsed <= 4 && priorMonthTotal > 0.0
            )
        }

        // ── 1. Outlier Fence (Tukey's IQR + Median multiplier) ──────────────────
        val sortedAmounts = currentMonthTransactions.map { it.amount }.sorted()
        val n = sortedAmounts.size

        val median = if (n % 2 == 0) {
            (sortedAmounts[n / 2 - 1] + sortedAmounts[n / 2]) / 2.0
        } else {
            sortedAmounts[n / 2]
        }

        val tukeyUpper = if (n >= 4) {
            val q1 = sortedAmounts[(n * 0.25).toInt().coerceIn(0, n - 1)]
            val q3 = sortedAmounts[(n * 0.75).toInt().coerceIn(0, n - 1)]
            val iqr = q3 - q1
            q3 + 1.5 * iqr
        } else {
            Double.MAX_VALUE
        }

        val isolatedSpikes = currentMonthTransactions.filter { txn ->
            val amt = txn.amount
            val isTukeyOutlier = amt > tukeyUpper && amt > median * 2.0 && amt >= 20.0
            val isMedianMultiplierOutlier = median > 0.0 && amt >= 3.0 * median && amt >= 50.0
            isTukeyOutlier || isMedianMultiplierOutlier
        }

        val isolatedSpikesTotal = isolatedSpikes.sumOf { it.amount }
        val isolatedSpikesCount = isolatedSpikes.size
        val variableSpendToDate = (spendToDate - isolatedSpikesTotal).coerceAtLeast(0.0)

        // ── 2. Bayesian Shrinkage Variable Burn Rate ────────────────────────────
        val currentDailyVariableRate = variableSpendToDate / safeElapsed
        val priorDailyRate = if (priorMonthTotalDays > 0) priorMonthTotal / priorMonthTotalDays else 0.0

        val wt = safeElapsed.toDouble() / (safeElapsed + 4.0)

        val lambdaBurn = if (priorDailyRate > 0.0) {
            wt * currentDailyVariableRate + (1.0 - wt) * priorDailyRate
        } else {
            currentDailyVariableRate
        }

        // ── 3. Assembly & Projections ───────────────────────────────────────────
        val projectedRemainingSpend = lambdaBurn * daysRemaining
        val projectedMonthEndTotal = spendToDate + projectedRemainingSpend

        val benchmark = spendingTarget ?: priorMonthTotal.takeIf { it > 0.0 }
        val safeRemainingDailyPace = if (benchmark != null && daysRemaining > 0) {
            ((benchmark - spendToDate) / daysRemaining).coerceAtLeast(0.0)
        } else null

        val percentVsLastMonth = if (priorMonthTotal > 0.0) {
            (((projectedMonthEndTotal - priorMonthTotal) / priorMonthTotal) * 100).roundToInt()
        } else null

        val isStabilizingWithPrior = safeElapsed <= 4 && priorMonthTotal > 0.0

        return MonthForecastResult(
            projectedMonthEndTotal = projectedMonthEndTotal,
            spendToDate = spendToDate,
            daysElapsed = safeElapsed,
            totalDaysInMonth = totalDaysInMonth,
            daysRemaining = daysRemaining,
            variableDailyBurnRate = lambdaBurn,
            isolatedSpikesTotal = isolatedSpikesTotal,
            isolatedSpikesCount = isolatedSpikesCount,
            projectedRemainingSpend = projectedRemainingSpend,
            safeRemainingDailyPace = safeRemainingDailyPace,
            percentVsLastMonth = percentVsLastMonth,
            isStabilizingWithPrior = isStabilizingWithPrior
        )
    }
}
