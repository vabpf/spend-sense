package com.spendsense.domain.calculation

import com.spendsense.domain.model.CreditCardConfig
import com.spendsense.domain.model.Transaction
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class CardStatementCycleResult(
    val cardName: String,
    val statementClosingDay: Int,
    val paymentDueDay: Int?,
    val creditLimit: Double?,
    val activeStatementBalance: Double,
    val activeCycleStartMillis: Long,
    val activeCycleEndMillis: Long,
    val daysUntilActiveCloses: Int,
    val closedStatementBalance: Double,
    val closedCycleStartMillis: Long,
    val closedCycleEndMillis: Long,
    val creditUtilizationPercent: Int?
)

data class CreditLiquiditySummary(
    val totalImmediateCashNeeded: Double,
    val totalActiveFloatingDebt: Double,
    val totalCreditSpendThisMonth: Double,
    val cardCycles: List<CardStatementCycleResult>
)

object CreditStatementEngine {

    fun calculateCardCycle(
        cardConfig: CreditCardConfig,
        cardTransactions: List<Transaction>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): CardStatementCycleResult {
        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val statementDay = cardConfig.statementClosingDay.coerceIn(1, 31)

        val activeEndCal = (nowCal.clone() as Calendar).apply {
            if (currentDay > statementDay) {
                add(Calendar.MONTH, 1)
            }
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, statementDay.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }

        val activeStartCal = (activeEndCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val startDay = (statementDay + 1).coerceAtMost(maxDay)
            set(Calendar.DAY_OF_MONTH, startDay)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val closedEndCal = (activeStartCal.clone() as Calendar).apply {
            timeInMillis = activeStartCal.timeInMillis - 1
        }

        val closedStartCal = (closedEndCal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val startDay = (statementDay + 1).coerceAtMost(maxDay)
            set(Calendar.DAY_OF_MONTH, startDay)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val activeStart = activeStartCal.timeInMillis
        val activeEnd = activeEndCal.timeInMillis
        val closedStart = closedStartCal.timeInMillis
        val closedEnd = closedEndCal.timeInMillis

        val activeTxns = cardTransactions.filter { it.timestamp in activeStart..activeEnd }
        val closedTxns = cardTransactions.filter { it.timestamp in closedStart..closedEnd }

        val activeBalance = activeTxns.sumOf { it.amount }
        val closedBalance = closedTxns.sumOf { it.amount }

        val millisUntilClose = (activeEnd - currentTimestamp).coerceAtLeast(0)
        val daysUntilClose = TimeUnit.MILLISECONDS.toDays(millisUntilClose).toInt().coerceAtLeast(0)

        val utilization = cardConfig.creditLimit?.takeIf { it > 0.0 }?.let { limit ->
            ((activeBalance / limit) * 100).roundToInt()
        }

        return CardStatementCycleResult(
            cardName = cardConfig.cardName,
            statementClosingDay = statementDay,
            paymentDueDay = cardConfig.paymentDueDay,
            creditLimit = cardConfig.creditLimit,
            activeStatementBalance = activeBalance,
            activeCycleStartMillis = activeStart,
            activeCycleEndMillis = activeEnd,
            daysUntilActiveCloses = daysUntilClose,
            closedStatementBalance = closedBalance,
            closedCycleStartMillis = closedStart,
            closedCycleEndMillis = closedEnd,
            creditUtilizationPercent = utilization
        )
    }

    fun calculateLiquiditySummary(
        configs: List<CreditCardConfig>,
        allTransactions: List<Transaction>,
        currentMonthTransactions: List<Transaction>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): CreditLiquiditySummary {
        val cardCycles = configs.map { config ->
            val cardTxns = allTransactions.filter {
                it.paymentSource.trim().equals(config.cardName.trim(), ignoreCase = true)
            }
            calculateCardCycle(config, cardTxns, currentTimestamp)
        }

        val immediateCashNeeded = cardCycles.sumOf { it.closedStatementBalance }
        val activeFloatingDebt = cardCycles.sumOf { it.activeStatementBalance }
        val creditSpendThisMonth = currentMonthTransactions
            .filter { it.paymentSourceType.equals("Credit Card", ignoreCase = true) }
            .sumOf { it.amount }

        return CreditLiquiditySummary(
            totalImmediateCashNeeded = immediateCashNeeded,
            totalActiveFloatingDebt = activeFloatingDebt,
            totalCreditSpendThisMonth = creditSpendThisMonth,
            cardCycles = cardCycles
        )
    }
}
