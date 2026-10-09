package com.spendsense.domain.calculation

import com.spendsense.core.SpendSenseCore
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class TimeBucketProfile(
    val bucketName: String,     // "Morning Routine", "Workday Midday", "Evening & Night", "Daytime / Errands"
    val totalAmount: Double,
    val count: Int,
    val fraction: Float
)

data class TicketTierProfile(
    val tierName: String,       // e.g. "Micro / Quick Bites", "Standard Purchases", "Major Outings / Splurges"
    val thresholdLabel: String, // e.g. "≤ $10", "$10 – $35", "≥ $35"
    val totalAmount: Double,
    val count: Int,
    val averageTicket: Double,
    val fraction: Float
)

data class TopMerchantProfile(
    val merchantName: String,
    val totalAmount: Double,
    val visitCount: Int,
    val averageTicket: Double
)

data class CategoryBehaviorProfile(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val timeBuckets: List<TimeBucketProfile>,
    val ticketTiers: List<TicketTierProfile>,
    val topMerchants: List<TopMerchantProfile>
)

object CategoryBehaviorProfiler {

    fun profileCategory(
        category: Category,
        categoryTransactions: List<Transaction>,
        currency: String
    ): CategoryBehaviorProfile {
        return SpendSenseCore.profileCategory(category, categoryTransactions, currency)
    }

    internal fun profileCategoryPureKotlin(
        category: Category,
        categoryTransactions: List<Transaction>,
        currency: String
    ): CategoryBehaviorProfile {
        val totalAmount = categoryTransactions.sumOf { it.amount }
        val count = categoryTransactions.size
        val safeTotal = totalAmount.takeIf { it > 0.0 } ?: 1.0

        if (categoryTransactions.isEmpty()) {
            return CategoryBehaviorProfile(
                category = category,
                totalAmount = 0.0,
                transactionCount = 0,
                timeBuckets = emptyList(),
                ticketTiers = emptyList(),
                topMerchants = emptyList()
            )
        }

        // ── 1. Temporal / Time-of-Day Profiling ─────────────────────────────────
        val cal = Calendar.getInstance()
        var morningAmount = 0.0; var morningCount = 0
        var middayAmount = 0.0; var middayCount = 0
        var eveningAmount = 0.0; var eveningCount = 0
        var otherAmount = 0.0; var otherCount = 0

        for (txn in categoryTransactions) {
            cal.timeInMillis = txn.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            val timeMinutes = hour * 60 + min

            when {
                // Morning: 06:00 to 10:30 (360 .. 630)
                timeMinutes in 360..630 -> {
                    morningAmount += txn.amount
                    morningCount++
                }
                // Midday Lunch: 11:30 to 14:00 (690 .. 840)
                timeMinutes in 690..840 -> {
                    middayAmount += txn.amount
                    middayCount++
                }
                // Evening & Late Night: 18:00 to 23:59 (1080 .. 1439) or 00:00 to 03:00 (0 .. 180)
                timeMinutes >= 1080 || timeMinutes <= 180 -> {
                    eveningAmount += txn.amount
                    eveningCount++
                }
                // Afternoon / other daytime (10:31-11:29, 14:01-17:59, 03:01-05:59)
                else -> {
                    otherAmount += txn.amount
                    otherCount++
                }
            }
        }

        val timeBuckets = listOf(
            TimeBucketProfile("Evening & Night", eveningAmount, eveningCount, (eveningAmount / safeTotal).toFloat()),
            TimeBucketProfile("Workday Midday", middayAmount, middayCount, (middayAmount / safeTotal).toFloat()),
            TimeBucketProfile("Morning Routine", morningAmount, morningCount, (morningAmount / safeTotal).toFloat()),
            TimeBucketProfile("Daytime / Errands", otherAmount, otherCount, (otherAmount / safeTotal).toFloat())
        ).filter { it.count > 0 }.sortedByDescending { it.totalAmount }

        // ── 2. Ticket-Size Tier Profiling (Partitioning) ────────────────────────
        val sortedAmounts = categoryTransactions.map { it.amount }.sorted()
        val n = sortedAmounts.size

        fun formatThresh(v: Double): String {
            val rounded = kotlin.math.round(v).toLong()
            val formatted = NumberFormat.getNumberInstance().format(rounded)
            return if (currency.trim().uppercase() == "VND") {
                "$formatted₫"
            } else {
                "$currency $formatted"
            }
        }

        fun makeTier(name: String, threshold: String, txns: List<Transaction>): TicketTierProfile {
            val amt = txns.sumOf { it.amount }
            val c = txns.size
            val avg = if (c > 0) amt / c else 0.0
            return TicketTierProfile(name, threshold, amt, c, avg, (amt / safeTotal).toFloat())
        }

        val ticketTiers = mutableListOf<TicketTierProfile>()

        if (n in 1..2) {
            if (n == 1 || sortedAmounts.first() == sortedAmounts.last()) {
                ticketTiers.add(makeTier("Regular Expenses", formatThresh(sortedAmounts.first()), categoryTransactions))
            } else {
                val low = sortedAmounts.first()
                val high = sortedAmounts.last()
                val micro = categoryTransactions.filter { it.amount == low }
                val major = categoryTransactions.filter { it.amount == high }
                ticketTiers.add(makeTier("Large Expenses", "≥ ${formatThresh(high)}", major))
                ticketTiers.add(makeTier("Small Expenses", "≤ ${formatThresh(low)}", micro))
            }
        } else {
            // n >= 3: Partition into tertiles
            val bottomCutoffIdx = (n / 3).coerceAtLeast(1)
            val topCutoffIdx = ((2 * n) / 3).coerceIn(bottomCutoffIdx, n - 1)
            val q1 = sortedAmounts[bottomCutoffIdx - 1]
            val q2 = sortedAmounts[topCutoffIdx]

            if (q1 < q2) {
                val microTxns = categoryTransactions.filter { it.amount <= q1 }
                val standardTxns = categoryTransactions.filter { it.amount > q1 && it.amount < q2 }
                val majorTxns = categoryTransactions.filter { it.amount >= q2 }

                if (majorTxns.isNotEmpty()) {
                    ticketTiers.add(makeTier("Large Expenses", "≥ ${formatThresh(q2)}", majorTxns))
                }
                if (standardTxns.isNotEmpty()) {
                    ticketTiers.add(makeTier("Regular Expenses", "${formatThresh(q1)} – ${formatThresh(q2)}", standardTxns))
                }
                if (microTxns.isNotEmpty()) {
                    ticketTiers.add(makeTier("Small Expenses", "≤ ${formatThresh(q1)}", microTxns))
                }
            } else {
                ticketTiers.add(makeTier("Regular Expenses", formatThresh(sortedAmounts.first()), categoryTransactions))
            }
        }

        // ── 3. Top Venues / Merchants Profiling ──────────────────────────────────
        val topMerchants = categoryTransactions
            .map { txn ->
                val m = txn.merchant.trim().ifBlank { txn.notes?.trim() ?: "" }.ifBlank { "Direct / Unnamed" }
                txn.copy(merchant = m)
            }
            .groupBy { it.merchant }
            .map { (name, txns) ->
                val amt = txns.sumOf { it.amount }
                val vCount = txns.size
                TopMerchantProfile(
                    merchantName = name,
                    totalAmount = amt,
                    visitCount = vCount,
                    averageTicket = if (vCount > 0) amt / vCount else 0.0
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(6)

        return CategoryBehaviorProfile(
            category = category,
            totalAmount = totalAmount,
            transactionCount = count,
            timeBuckets = timeBuckets,
            ticketTiers = ticketTiers,
            topMerchants = topMerchants
        )
    }
}
