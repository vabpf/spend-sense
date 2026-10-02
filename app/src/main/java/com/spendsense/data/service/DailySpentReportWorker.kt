package com.spendsense.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.TransactionDao
import com.spendsense.presentation.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.concurrent.TimeUnit

class DailySpentReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DailySpentReportWorkerEntryPoint {
        fun transactionDao(): TransactionDao
        fun securePreferences(): SecurePreferences
    }

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            DailySpentReportWorkerEntryPoint::class.java
        )
        val transactionDao = entryPoint.transactionDao()
        val securePreferences = entryPoint.securePreferences()

        if (!securePreferences.isDailyReportEnabled()) {
            return Result.success()
        }

        // Calculate date range for today (local timezone)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

        val transactions = transactionDao.getByDateRangeFlow(startOfDay, endOfDay).first()
        
        postReportNotification(appContext, transactions)
        
        return Result.success()
    }

    private fun postReportNotification(
        context: Context,
        transactions: List<com.spendsense.data.local.entity.TransactionEntity>
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_spent_report"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Spending Report",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily summary of your total expenses"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Daily Spending Summary"
        val bodyText: String
        val bigTextBuilder = StringBuilder()

        if (transactions.isEmpty()) {
            bodyText = "No transactions recorded today."
            bigTextBuilder.append("Keep tracking your expenses to build healthy financial habits!")
        } else {
            // Group by currency
            val currencyTotals = transactions.groupBy { it.currencyCode }
                .mapValues { (_, txs) -> txs.sumOf { it.amount } }

            val formattedTotals = currencyTotals.map { (currencyCode, sum) ->
                formatAmount(sum, currencyCode)
            }.joinToString(", ")

            bodyText = "Total spent today: $formattedTotals"

            bigTextBuilder.append("Here is your spending breakdown today:\n\n")
            transactions.forEach { tx ->
                val formattedVal = formatAmount(tx.amount, tx.currencyCode)
                bigTextBuilder.append("• ${tx.merchant}: $formattedVal\n")
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.spendsense.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextBuilder.toString()))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
    }

    private fun formatAmount(amount: Double, currencyCode: String): String {
        val cleanCurrencyCode = currencyCode.trim().uppercase()
        if (cleanCurrencyCode == "VND") {
            return try {
                val formatter = NumberFormat.getNumberInstance().apply {
                    if (amount % 1.0 == 0.0) {
                        this.minimumFractionDigits = 0
                        this.maximumFractionDigits = 0
                    } else {
                        this.minimumFractionDigits = 0
                        this.maximumFractionDigits = 2
                    }
                }
                val formattedNumber = formatter.format(amount)
                "$formattedNumber₫"
            } catch (e: Exception) {
                "${formatDoublePlain(amount)}₫"
            }
        }
        return try {
            val currency = Currency.getInstance(cleanCurrencyCode)
            val formatter = NumberFormat.getCurrencyInstance().apply {
                this.currency = currency
                if (amount % 1.0 == 0.0) {
                    this.minimumFractionDigits = 0
                    this.maximumFractionDigits = 0
                }
            }
            formatter.format(amount)
        } catch (e: Exception) {
            val cleanCode = currencyCode.trim()
            val symbol = try {
                Currency.getInstance(cleanCode.uppercase()).symbol
            } catch (_: Exception) {
                if (cleanCode.isNotBlank()) cleanCode else "$"
            }
            "$symbol ${formatDoublePlain(amount)}"
        }
    }

    private fun formatDoublePlain(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            java.math.BigDecimal.valueOf(value).toPlainString()
        }
    }

    companion object {
        fun schedule(context: Context, timeStr: String, enabled: Boolean) {
            val workManager = androidx.work.WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork("daily_spent_report_work")
                return
            }

            val parts = timeStr.split(":")
            val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
            val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val delay = calculateDelay(targetHour, targetMinute)

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<DailySpentReportWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "daily_spent_report_work",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        private fun calculateDelay(targetHour: Int, targetMinute: Int): Long {
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.HOUR_OF_DAY, 24)
            }
            return dueDate.timeInMillis - currentDate.timeInMillis
        }
    }
}
