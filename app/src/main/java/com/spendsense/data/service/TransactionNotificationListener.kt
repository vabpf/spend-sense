package com.spendsense.data.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.spendsense.data.local.dao.CategoryDao
import com.spendsense.data.local.dao.MerchantCategoryMappingDao
import com.spendsense.data.local.dao.NotificationPatternDao
import com.spendsense.data.local.dao.WhitelistedAppDao
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class TransactionNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var whitelistedAppDao: WhitelistedAppDao

    @Inject
    lateinit var rawNotificationDao: com.spendsense.data.local.dao.RawNotificationDao

    @Inject
    lateinit var merchantCategoryMappingDao: MerchantCategoryMappingDao

    @Inject
    lateinit var notificationPatternDao: NotificationPatternDao

    @Inject
    lateinit var categoryDao: CategoryDao

    @Inject
    lateinit var transactionRepository: com.spendsense.domain.repository.TransactionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var whitelistedPackages: Set<String> = emptySet()
    private var notificationIdCounter = 2000

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TransactionNotificationListener created")
        loadWhitelistedPackages()
        createNotificationChannel()
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

        val notificationTitle = extractNotificationTitle(notification)
        val notificationText = extractNotificationText(notification)
        val isDebuggable = (applicationContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Log.d(TAG, "Notification received: package=$packageName, title=$notificationTitle, text=$notificationText")
        }
        if (notificationText.isNullOrBlank()) {
            Log.d(TAG, "No text found in notification")
            return
        }

        // Check if this app has been configured with patterns
        val appPatterns = notificationPatternDao.getAllForPackage(packageName)
        if (appPatterns.isEmpty()) {
            // NEW APP — no patterns yet, save to inbox
            saveToInbox(packageName, notificationTitle, notificationText, null)
            Log.d(TAG, "New app $packageName — saved to inbox")
            return
        }

        // KNOWN APP — try to match notification title
        if (!notificationTitle.isNullOrBlank()) {
            val matchedPattern = appPatterns.firstOrNull { pattern ->
                notificationTitle.contains(pattern.notificationTitle, ignoreCase = true)
            }

            if (matchedPattern != null) {
                if (!matchedPattern.isTransaction) {
                    Log.d(TAG, "Known non-transaction pattern for ($packageName, $notificationTitle) — skipping")
                    return
                }

                // It's a transaction — try the regex
                if (matchedPattern.regex != null) {
                    val matched = tryMatchPattern(matchedPattern, notificationText, packageName)
                    if (matched) return
                    // Regex exists but failed to match — pattern is stale
                    saveToInbox(packageName, notificationTitle, notificationText, matchedPattern.id)
                    Log.d(TAG, "Stale pattern ${matchedPattern.id} for $packageName — saved to inbox")
                    return
                }

                // Transaction pattern with no regex — new body format, save to inbox
                saveToInbox(packageName, notificationTitle, notificationText, null)
                Log.d(TAG, "No regex for matched pattern ($packageName) — saved to inbox")
                return
            }
        }

        // Unknown title for a known app — silent skip (not a configured transaction type)
        Log.d(TAG, "Unknown title for known app $packageName — skipping")
    }

    private suspend fun saveToInbox(packageName: String, title: String?, text: String, stalePatternId: Long?) {
        rawNotificationDao.insert(
            com.spendsense.data.local.entity.RawNotificationEntity(
                packageName = packageName,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                stalePatternId = stalePatternId
            )
        )
    }

    private suspend fun tryMatchPattern(
        pattern: NotificationPatternEntity,
        notificationText: String,
        packageName: String
    ): Boolean {
        val regexStr = pattern.regex ?: return false
        try {
            val regex = Regex(regexStr)
            val matchResult = regex.find(notificationText)
            if (matchResult != null) {
                val amountStr = matchResult.groups["amount"]?.value
                val merchant = matchResult.groups["merchant"]?.value
                if (amountStr != null && merchant != null) {
                    val amount = parseAmount(amountStr)
                    if (amount > 0) {
                        notificationPatternDao.upsert(pattern.copy(
                            lastMatchedAt = System.currentTimeMillis(),
                            matchCount = pattern.matchCount + 1
                        ))
                        val appName = getAppName(packageName)
                        saveAndPostNotification(amount, merchant, packageName, appName, pattern.currencyCode, notificationText, pattern.notificationTitle)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error matching notification pattern: ${pattern.regex}", e)
        }
        return false
    }

    private suspend fun saveAndPostNotification(
        amount: Double,
        merchant: String,
        packageName: String,
        appName: String,
        currencyCode: String,
        notificationText: String,
        notificationTitle: String
    ) {
        // Save raw notification as already processed (since it is auto-saved)
        val rawId = rawNotificationDao.insert(
            com.spendsense.data.local.entity.RawNotificationEntity(
                packageName = packageName,
                title = notificationTitle,
                text = notificationText,
                timestamp = System.currentTimeMillis(),
                isProcessed = true
            )
        )

        val mapping = merchantCategoryMappingDao.getByMerchant(merchant.lowercase())
        val suggestedCategoryId = mapping?.categoryId
        val suggestedCategoryName = if (suggestedCategoryId != null) {
            categoryDao.getById(suggestedCategoryId)?.name
        } else null

        val finalCategoryId = if (suggestedCategoryId != null && suggestedCategoryId > 0) {
            suggestedCategoryId
        } else {
            val categories = categoryDao.getAll()
            categories.firstOrNull { it.name == "Other" }?.id
                ?: categories.firstOrNull()?.id
                ?: 1L
        }

        // Auto-save the transaction right away!
        val transactionId = transactionRepository.insertTransaction(
            com.spendsense.domain.model.Transaction(
                amount = amount,
                currencyCode = currencyCode,
                merchant = merchant,
                categoryId = finalCategoryId,
                timestamp = System.currentTimeMillis(),
                sourcePackageName = packageName,
                sourceAppName = appName
            )
        )

        withContext(Dispatchers.Main) {
            postTransactionNotification(
                amount, merchant, packageName, appName, rawId,
                currencyCode, suggestedCategoryId, suggestedCategoryName,
                transactionId
            )
        }
    }

    private fun extractNotificationTitle(notification: android.app.Notification): String? {
        return notification.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
            ?.takeIf { it.isNotBlank() }
    }

    private fun postTransactionNotification(
        amount: Double,
        merchant: String,
        packageName: String,
        appName: String,
        rawNotificationId: Long,
        currencyCode: String,
        suggestedCategoryId: Long?,
        suggestedCategoryName: String?,
        transactionId: Long
    ) {
        val notificationId = notificationIdCounter++

        val bodyText = if (suggestedCategoryName != null) {
            "$$amount auto-saved to $suggestedCategoryName"
        } else {
            "$$amount auto-saved (Tap to categorize)"
        }

        // Edit action — opens main app with data, passing transactionId for updating
        val editIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_REVIEW_AMOUNT, amount)
            putExtra(EXTRA_REVIEW_MERCHANT, merchant)
            putExtra(EXTRA_REVIEW_CURRENCY, currencyCode)
            putExtra(EXTRA_REVIEW_PACKAGE_NAME, packageName)
            putExtra(EXTRA_REVIEW_APP_NAME, appName)
            putExtra(EXTRA_REVIEW_RAW_NOTIFICATION_ID, rawNotificationId)
            putExtra(EXTRA_REVIEW_TRANSACTION_ID, transactionId)
            if (suggestedCategoryId != null) {
                putExtra(EXTRA_REVIEW_CATEGORY_ID, suggestedCategoryId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPendingIntent = PendingIntent.getActivity(
            this, notificationId + 1000, editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Undo action — deletes the auto-saved transaction
        val rejectIntent = Intent(TransactionNotificationReceiver.ACTION_REJECT).apply {
            setPackage(this@TransactionNotificationListener.packageName)
            putExtra(TransactionNotificationReceiver.EXTRA_RAW_NOTIFICATION_ID, rawNotificationId)
            putExtra(TransactionNotificationReceiver.EXTRA_TRANSACTION_ID, transactionId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, notificationId + 2000, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(merchant)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .addAction(android.R.drawable.ic_menu_edit, "Change Category", editPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Undo", rejectPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Transaction notification auto-saved & posted: $merchant — $amount (rawId=$rawNotificationId, transactionId=$transactionId)")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Transaction Detection",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from detected transactions"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun extractNotificationText(notification: android.app.Notification): String? {
        val extras = notification.extras ?: return null
        
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        
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

    companion object {
        private const val TAG = "TransactionNotification"
        private const val CHANNEL_ID = "transaction_detection"
        const val EXTRA_REVIEW_AMOUNT = "review_amount"
        const val EXTRA_REVIEW_MERCHANT = "review_merchant"
        const val EXTRA_REVIEW_CURRENCY = "review_currency"
        const val EXTRA_REVIEW_PACKAGE_NAME = "review_package_name"
        const val EXTRA_REVIEW_APP_NAME = "review_app_name"
        const val EXTRA_REVIEW_RAW_NOTIFICATION_ID = "review_raw_notification_id"
        const val EXTRA_REVIEW_CATEGORY_ID = "review_category_id"
        const val EXTRA_REVIEW_TRANSACTION_ID = "review_transaction_id"
    }
}
