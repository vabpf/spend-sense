package com.spendsense.core

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.spendsense.domain.calculation.CategoryBehaviorProfile
import com.spendsense.domain.calculation.CategoryBehaviorProfiler
import com.spendsense.domain.calculation.ForecastEngine
import com.spendsense.domain.calculation.MonthForecastResult
import com.spendsense.domain.calculation.TicketTierProfile
import com.spendsense.domain.calculation.TimeBucketProfile
import com.spendsense.domain.calculation.TopMerchantProfile
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.DailyAggregation
import com.spendsense.domain.model.MonthlyCategoryAggregation
import com.spendsense.domain.model.MonthlyPaymentAggregation
import com.spendsense.domain.model.Transaction
import java.util.TimeZone

data class AggregationBatchResult(
    val daily: List<DailyAggregation>,
    val categories: List<MonthlyCategoryAggregation>,
    val payments: List<MonthlyPaymentAggregation>
)

data class NativeMatchResult(
    @SerializedName("is_match") val isMatch: Boolean,
    val amount: Double?,
    val currency: String?,
    val merchant: String?
)

/**
 * SpendSenseCore is an isolated, high-performance computational bridge.
 * If the compiled Rust shared library (`libspend_sense_core.so`) is present in the runtime APK,
 * it performs heavy numerical forecasting, category behavioral profiling, and batch rollups
 * with zero GC / JIT overhead.
 *
 * If native binaries are absent (e.g. standard JVM unit tests or unsupported ABI), it
 * transparently and gracefully falls back to the pure Kotlin engines without failing.
 */
object SpendSenseCore {
    private const val TAG = "SpendSenseCore"
    private val gson = Gson()

    val isAvailable: Boolean = try {
        System.loadLibrary("spend_sense_core")
        nativeIsAvailable()
    } catch (t: Throwable) {
        Log.i(TAG, "Native Rust core not loaded (${t.message}), using pure Kotlin fallback")
        false
    }

    // ── JNI External Declarations ───────────────────────────────────────────

    @JvmStatic
    private external fun nativeIsAvailable(): Boolean

    @JvmStatic
    private external fun nativeForecast(
        amounts: DoubleArray,
        daysElapsed: Int,
        totalDays: Int,
        priorTotal: Double,
        priorDays: Int
    ): String

    @JvmStatic
    private external fun nativeAggregateBatch(
        txnsJson: String,
        tzOffsetMs: Long
    ): String

    @JvmStatic
    private external fun nativeProfileCategory(
        txnsJson: String,
        currencyStr: String
    ): String

    @JvmStatic
    private external fun nativeMatchNotification(
        textStr: String,
        patternStr: String
    ): String

    // ── Public Facade Methods ───────────────────────────────────────────────

    fun forecast(
        currentMonthTransactions: List<Transaction>,
        daysElapsed: Int,
        totalDaysInMonth: Int,
        priorMonthTotal: Double = 0.0,
        priorMonthTotalDays: Int = 30,
        spendingTarget: Double? = null
    ): MonthForecastResult {
        if (!isAvailable) {
            return ForecastEngine.calculateMonthForecastPureKotlin(
                currentMonthTransactions = currentMonthTransactions,
                daysElapsed = daysElapsed,
                totalDaysInMonth = totalDaysInMonth,
                priorMonthTotal = priorMonthTotal,
                priorMonthTotalDays = priorMonthTotalDays,
                spendingTarget = spendingTarget
            )
        }

        return try {
            val amounts = currentMonthTransactions.map { it.amount }.toDoubleArray()
            val json = nativeForecast(
                amounts,
                daysElapsed,
                totalDaysInMonth,
                priorMonthTotal,
                priorMonthTotalDays
            )
            if (json.isBlank()) {
                ForecastEngine.calculateMonthForecastPureKotlin(
                    currentMonthTransactions,
                    daysElapsed,
                    totalDaysInMonth,
                    priorMonthTotal,
                    priorMonthTotalDays,
                    spendingTarget
                )
            } else {
                val nativeOut = gson.fromJson(json, NativeForecastOutput::class.java)
                val benchmark = spendingTarget ?: priorMonthTotal.takeIf { it > 0.0 }
                val safeRemainingDays = (totalDaysInMonth - daysElapsed.coerceIn(1, totalDaysInMonth)).coerceAtLeast(0)
                val safePace = if (benchmark != null && safeRemainingDays > 0) {
                    ((benchmark - nativeOut.spendToDate) / safeRemainingDays).coerceAtLeast(0.0)
                } else null

                MonthForecastResult(
                    projectedMonthEndTotal = nativeOut.projectedMonthEndTotal,
                    spendToDate = nativeOut.spendToDate,
                    daysElapsed = nativeOut.daysElapsed,
                    totalDaysInMonth = totalDaysInMonth,
                    daysRemaining = nativeOut.daysRemaining,
                    variableDailyBurnRate = nativeOut.variableDailyBurnRate,
                    isolatedSpikesTotal = nativeOut.isolatedSpikesTotal,
                    isolatedSpikesCount = nativeOut.isolatedSpikesCount,
                    projectedRemainingSpend = nativeOut.projectedRemainingSpend,
                    safeRemainingDailyPace = safePace ?: nativeOut.safeRemainingDailyPace,
                    percentVsLastMonth = nativeOut.percentVsLastMonth,
                    isStabilizingWithPrior = nativeOut.isStabilizingWithPrior
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Native forecast error, falling back to Kotlin: ${e.message}")
            ForecastEngine.calculateMonthForecastPureKotlin(
                currentMonthTransactions,
                daysElapsed,
                totalDaysInMonth,
                priorMonthTotal,
                priorMonthTotalDays,
                spendingTarget
            )
        }
    }

    fun profileCategory(
        category: Category,
        categoryTransactions: List<Transaction>,
        currency: String
    ): CategoryBehaviorProfile {
        if (!isAvailable) {
            return CategoryBehaviorProfiler.profileCategoryPureKotlin(category, categoryTransactions, currency)
        }

        return try {
            val inputs = categoryTransactions.map {
                NativeProfileInput(
                    amount = it.amount,
                    timestamp = it.timestamp,
                    merchant = it.merchant,
                    notes = it.notes
                )
            }
            val inputJson = gson.toJson(inputs)
            val outputJson = nativeProfileCategory(inputJson, currency)
            if (outputJson.isBlank()) {
                CategoryBehaviorProfiler.profileCategoryPureKotlin(category, categoryTransactions, currency)
            } else {
                val out = gson.fromJson(outputJson, NativeCategoryBehaviorOutput::class.java)
                CategoryBehaviorProfile(
                    category = category,
                    totalAmount = out.totalAmount,
                    transactionCount = out.transactionCount,
                    timeBuckets = out.timeBuckets.map {
                        TimeBucketProfile(it.bucketName, it.totalAmount, it.count, it.fraction)
                    },
                    ticketTiers = out.ticketTiers.map {
                        TicketTierProfile(it.tierName, it.thresholdLabel, it.totalAmount, it.count, it.averageTicket, it.fraction)
                    },
                    topMerchants = out.topMerchants.map {
                        TopMerchantProfile(it.merchantName, it.totalAmount, it.visitCount, it.averageTicket)
                    }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Native profileCategory error, falling back to Kotlin: ${e.message}")
            CategoryBehaviorProfiler.profileCategoryPureKotlin(category, categoryTransactions, currency)
        }
    }

    fun aggregateBatch(
        transactions: List<Transaction>,
        tzOffsetMs: Long = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()
    ): AggregationBatchResult? {
        if (!isAvailable) return null

        return try {
            val inputs = transactions.map {
                NativeRawTransactionInput(
                    id = it.id,
                    amount = it.amount,
                    timestamp = it.timestamp,
                    categoryId = it.categoryId,
                    paymentSource = it.paymentSource,
                    paymentSourceType = it.paymentSourceType
                )
            }
            val inputJson = gson.toJson(inputs)
            val outputJson = nativeAggregateBatch(inputJson, tzOffsetMs)
            if (outputJson.isBlank()) return null

            val out = gson.fromJson(outputJson, NativeAggregationResult::class.java)
            AggregationBatchResult(
                daily = out.daily.map {
                    DailyAggregation(
                        dateKey = it.dateKey,
                        year = it.year,
                        month = it.month,
                        day = it.day,
                        timestampDayStart = it.dayStart,
                        totalAmount = it.totalAmount,
                        transactionCount = it.transactionCount
                    )
                },
                categories = out.categories.map {
                    MonthlyCategoryAggregation(
                        year = it.year,
                        month = it.month,
                        categoryId = it.categoryId,
                        totalAmount = it.totalAmount,
                        transactionCount = it.transactionCount
                    )
                },
                payments = out.payments.map {
                    MonthlyPaymentAggregation(
                        year = it.year,
                        month = it.month,
                        paymentSource = it.paymentSource,
                        paymentSourceType = it.paymentSourceType,
                        totalAmount = it.totalAmount,
                        transactionCount = it.transactionCount
                    )
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Native aggregateBatch error: ${e.message}")
            null
        }
    }

    fun matchNotification(text: String, pattern: String): NativeMatchResult? {
        if (!isAvailable) return null
        return try {
            val json = nativeMatchNotification(text, pattern)
            if (json.isBlank()) null else gson.fromJson(json, NativeMatchResult::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ── Internal Transfer Objects ───────────────────────────────────────────

    private data class NativeForecastOutput(
        @SerializedName("spend_to_date") val spendToDate: Double,
        @SerializedName("projected_remaining_spend") val projectedRemainingSpend: Double,
        @SerializedName("projected_month_end_total") val projectedMonthEndTotal: Double,
        @SerializedName("variable_daily_burn_rate") val variableDailyBurnRate: Double,
        @SerializedName("days_elapsed") val daysElapsed: Int,
        @SerializedName("days_remaining") val daysRemaining: Int,
        @SerializedName("isolated_spikes_count") val isolatedSpikesCount: Int,
        @SerializedName("isolated_spikes_total") val isolatedSpikesTotal: Double,
        @SerializedName("safe_remaining_daily_pace") val safeRemainingDailyPace: Double?,
        @SerializedName("percent_vs_last_month") val percentVsLastMonth: Int?,
        @SerializedName("is_stabilizing_with_prior") val isStabilizingWithPrior: Boolean
    )

    private data class NativeProfileInput(
        val amount: Double,
        val timestamp: Long,
        val merchant: String,
        val notes: String?
    )

    private data class NativeTimeBucket(
        @SerializedName("bucket_name") val bucketName: String,
        @SerializedName("total_amount") val totalAmount: Double,
        val count: Int,
        val fraction: Float
    )

    private data class NativeTicketTier(
        @SerializedName("tier_name") val tierName: String,
        @SerializedName("threshold_label") val thresholdLabel: String,
        @SerializedName("total_amount") val totalAmount: Double,
        val count: Int,
        @SerializedName("average_ticket") val averageTicket: Double,
        val fraction: Float
    )

    private data class NativeTopMerchant(
        @SerializedName("merchant_name") val merchantName: String,
        @SerializedName("total_amount") val totalAmount: Double,
        @SerializedName("visit_count") val visitCount: Int,
        @SerializedName("average_ticket") val averageTicket: Double
    )

    private data class NativeCategoryBehaviorOutput(
        @SerializedName("total_amount") val totalAmount: Double,
        @SerializedName("transaction_count") val transactionCount: Int,
        @SerializedName("time_buckets") val timeBuckets: List<NativeTimeBucket>,
        @SerializedName("ticket_tiers") val ticketTiers: List<NativeTicketTier>,
        @SerializedName("top_merchants") val topMerchants: List<NativeTopMerchant>
    )

    private data class NativeRawTransactionInput(
        val id: Long,
        val amount: Double,
        val timestamp: Long,
        @SerializedName("category_id") val categoryId: Long,
        @SerializedName("payment_source") val paymentSource: String,
        @SerializedName("payment_source_type") val paymentSourceType: String
    )

    private data class NativeDailyRollup(
        @SerializedName("date_key") val dateKey: String,
        val year: Int,
        val month: Int,
        val day: Int,
        @SerializedName("day_start") val dayStart: Long,
        @SerializedName("total_amount") val totalAmount: Double,
        @SerializedName("transaction_count") val transactionCount: Int
    )

    private data class NativeCategoryRollup(
        val year: Int,
        val month: Int,
        @SerializedName("category_id") val categoryId: Long,
        @SerializedName("total_amount") val totalAmount: Double,
        @SerializedName("transaction_count") val transactionCount: Int
    )

    private data class NativePaymentRollup(
        val year: Int,
        val month: Int,
        @SerializedName("payment_source") val paymentSource: String,
        @SerializedName("payment_source_type") val paymentSourceType: String,
        @SerializedName("total_amount") val totalAmount: Double,
        @SerializedName("transaction_count") val transactionCount: Int
    )

    private data class NativeAggregationResult(
        val daily: List<NativeDailyRollup>,
        val categories: List<NativeCategoryRollup>,
        val payments: List<NativePaymentRollup>
    )
}
