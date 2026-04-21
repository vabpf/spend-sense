package com.spendsense.data.service

import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.spendsense.data.local.dao.RegexPatternDao
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.domain.model.RegexPattern
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var regexPatternDao: RegexPatternDao

    @Inject
    lateinit var whitelistedAppDao: WhitelistedAppDao

    @Inject
    lateinit var rawNotificationDao: com.spendsense.data.local.dao.RawNotificationDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var whitelistedPackages: Set<String> = emptySet()


    companion object {
        private const val TAG = "TransactionNotification"
        const val ACTION_SHOW_OVERLAY = "com.spendsense.ACTION_SHOW_OVERLAY"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_MERCHANT = "extra_merchant"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_CURRENCY = "extra_currency"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TransactionNotificationListener created")
        loadWhitelistedPackages()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected")
        loadWhitelistedPackages()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName
        
        // Check if this app is whitelisted
        if (!whitelistedPackages.contains(packageName)) {
            return
        }

        Log.d(TAG, "Processing notification from whitelisted app: $packageName")

        serviceScope.launch(Dispatchers.IO) {
            try {
                processNotification(sbn)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    private suspend fun processNotification(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val packageName = sbn.packageName
        
        // Extract notification text
        val notificationText = extractNotificationText(notification)
        if (notificationText.isNullOrBlank()) {
            Log.d(TAG, "No text found in notification")
            return
        }

        // Save to raw_notifications first
        val rawId = rawNotificationDao.insert(
            com.spendsense.data.local.entity.RawNotificationEntity(
                packageName = packageName,
                text = notificationText,
                timestamp = System.currentTimeMillis()
            )
        )

        // Get regex patterns for this package
        val patterns = regexPatternDao.getActivePatternsForPackage(
            packageName = packageName,
            allWhitelistedPackage = RegexPattern.TARGET_ALL_WHITELISTED
        )

        if (patterns.isEmpty()) {
            Log.d(TAG, "No active patterns found for package: $packageName")
            return
        }

        // Try to match with each pattern
        for (patternEntity in patterns) {
            try {
                val regex = Regex(patternEntity.pattern)
                val matchResult = regex.find(notificationText)
                
                if (matchResult != null) {
                    // Extract named groups
                    val amountStr = matchResult.groups["amount"]?.value
                    val merchant = matchResult.groups["merchant"]?.value

                    if (amountStr != null && merchant != null) {
                        // Parse amount (remove currency symbols and parse)
                        val amount = parseAmount(amountStr)
                        
                        if (amount > 0) {
                            // Increment success count
                            regexPatternDao.incrementSuccessCount(
                                patternEntity.id,
                                System.currentTimeMillis()
                            )

                            // Get app name
                            val appName = getAppName(packageName)
                            val currencyCode = patternEntity.currencyCode

                            // Trigger the Action Overlay
                            withContext(Dispatchers.Main) {
                                showActionOverlay(amount, merchant, packageName, appName, rawId, currencyCode)
                            }

                            return // Stop after first match
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error matching pattern: ${patternEntity.pattern}", e)
            }
        }
    }

    private fun extractNotificationText(notification: Notification): String? {
        val extras = notification.extras ?: return null
        
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
        // Combine all text fields
        return listOf(title, text, bigText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    }

    private fun parseAmount(amountStr: String): Double {
        return try {
            // Remove currency symbols, commas, and spaces
            val cleanedStr = amountStr.replace(Regex("[^0-9.]"), "")
            cleanedStr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing amount", e)
            0.0
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun showActionOverlay(
        amount: Double,
        merchant: String,
        packageName: String,
        appName: String,
        rawNotificationId: Long,
        currency: String
    ) {
        val intent = Intent(ACTION_SHOW_OVERLAY).apply {
            setPackage(this@TransactionNotificationListener.packageName)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_MERCHANT, merchant)
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_NAME, appName)
            putExtra("extra_raw_notification_id", rawNotificationId)
            putExtra(EXTRA_CURRENCY, currency)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        sendBroadcast(intent)
    }


    private fun loadWhitelistedPackages() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val apps = whitelistedAppDao.getEnabledApps()
                whitelistedPackages = apps.map { it.packageName }.toSet()
                Log.d(TAG, "Loaded ${whitelistedPackages.size} whitelisted packages")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading whitelisted packages", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "TransactionNotificationListener destroyed")
    }
}
