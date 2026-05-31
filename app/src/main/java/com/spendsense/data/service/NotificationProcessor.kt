package com.spendsense.data.service

import android.content.Context
import android.util.Log
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity
import com.spendsense.domain.repository.TransactionRepository
import com.spendsense.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ProcessResult {
    TRANSACTION_CREATED,
    INBOX_CREATED,
    SILENT_SKIPPED
}

@Singleton
class NotificationProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationPatternDao: NotificationPatternDao,
    private val rawNotificationDao: RawNotificationDao,
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val whitelistedAppDao: WhitelistedAppDao
) {

    interface NotificationPostListener {
        fun onTransactionProcessed(
            amount: Double,
            merchant: String,
            packageName: String,
            appName: String,
            rawNotificationId: Long,
            currencyCode: String,
            suggestedCategoryId: Long?,
            suggestedCategoryName: String?,
            transactionId: Long
        )
    }

    private val TAG = "NotificationProcessor"

    suspend fun process(
        packageName: String,
        appName: String,
        title: String?,
        text: String,
        timestamp: Long,
        listener: NotificationPostListener? = null,
        existingRawNotificationId: Long? = null
    ): ProcessResult = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            Log.d(TAG, "No text found in notification")
            return@withContext ProcessResult.SILENT_SKIPPED
        }

        // Check if this app has been configured with patterns
        val appPatterns = notificationPatternDao.getAllForPackage(packageName)
        if (appPatterns.isEmpty()) {
            saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId)
            Log.d(TAG, "New app $packageName — saved to inbox")
            return@withContext ProcessResult.INBOX_CREATED
        }

        // KNOWN APP — try to match notification title
        if (!title.isNullOrBlank()) {
            val matchedPatterns = appPatterns.filter { pattern ->
                title.contains(pattern.notificationTitle, ignoreCase = true)
            }.sortedByDescending { it.notificationTitle.length }

            if (matchedPatterns.isNotEmpty()) {
                val transactionPatterns = matchedPatterns.filter { it.isTransaction }
                if (transactionPatterns.isEmpty()) {
                    Log.d(TAG, "Known non-transaction patterns for ($packageName, $title) — skipping")
                    return@withContext ProcessResult.SILENT_SKIPPED
                }

                var hasStalePattern = false
                var stalePatternId: Long? = null
                var hasNoRegexPattern = false

                for (pattern in transactionPatterns) {
                    if (pattern.regex != null) {
                        val matched = tryMatchPattern(pattern, text, packageName, appName, timestamp, listener, existingRawNotificationId)
                        if (matched) {
                            return@withContext ProcessResult.TRANSACTION_CREATED
                        }
                        hasStalePattern = true
                        stalePatternId = pattern.id
                    } else {
                        hasNoRegexPattern = true
                    }
                }

                if (hasNoRegexPattern) {
                    saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId)
                    Log.d(TAG, "No regex for matched pattern ($packageName) — saved to inbox")
                    return@withContext ProcessResult.INBOX_CREATED
                }

                if (hasStalePattern) {
                    saveToInbox(packageName, title, text, timestamp, stalePatternId, existingRawNotificationId)
                    Log.d(TAG, "Stale pattern $stalePatternId for $packageName — saved to inbox")
                    return@withContext ProcessResult.INBOX_CREATED
                }
            }
        }

        // Gate 5: Multi-Language Transaction Heuristic for Unknown Title
        if (isPotentialTransaction(text)) {
            saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId)
            Log.d(TAG, "Unknown title $title for known app $packageName (Looks like transaction) — saved to inbox")
            return@withContext ProcessResult.INBOX_CREATED
        } else {
            Log.d(TAG, "Unknown title $title for known app $packageName (Looks like marketing/chat) — skipping")
            return@withContext ProcessResult.SILENT_SKIPPED
        }
    }

    suspend fun reprocessInboxForPattern(
        pattern: NotificationPatternEntity,
        appName: String = "App"
    ): Int = withContext(Dispatchers.IO) {
        var recoveredCount = 0
        val unprocessed = rawNotificationDao.getUnprocessedForPackageAndTitle(
            packageName = pattern.packageName,
            title = pattern.notificationTitle
        )
        
        for (notif in unprocessed) {
            val outcome = process(
                packageName = notif.packageName,
                appName = appName,
                title = notif.title,
                text = notif.text,
                timestamp = notif.timestamp,
                listener = null,
                existingRawNotificationId = notif.id
            )
            if (outcome == ProcessResult.TRANSACTION_CREATED) {
                recoveredCount++
            } else if (outcome == ProcessResult.SILENT_SKIPPED) {
                // Clear skipped notifications from the pending inbox
                rawNotificationDao.markAsProcessed(notif.id)
            }
        }
        return@withContext recoveredCount
    }

    private suspend fun tryMatchPattern(
        pattern: NotificationPatternEntity,
        notificationText: String,
        packageName: String,
        appName: String,
        timestamp: Long,
        listener: NotificationPostListener?,
        existingRawNotificationId: Long?
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
                        saveAndPostNotification(amount, merchant, packageName, appName, pattern.currencyCode, notificationText, pattern.notificationTitle, timestamp, listener, existingRawNotificationId)
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
        notificationTitle: String,
        timestamp: Long,
        listener: NotificationPostListener?,
        existingRawNotificationId: Long?
    ) {
        val rawId = if (existingRawNotificationId != null) {
            val existing = rawNotificationDao.getById(existingRawNotificationId)
            if (existing != null) {
                rawNotificationDao.update(existing.copy(isProcessed = true, stalePatternId = null))
            }
            existingRawNotificationId
        } else {
            rawNotificationDao.insert(
                RawNotificationEntity(
                    packageName = packageName,
                    title = notificationTitle,
                    text = notificationText,
                    timestamp = timestamp,
                    isProcessed = true
                )
            )
        }

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

        val transactionId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                currencyCode = currencyCode,
                merchant = merchant,
                categoryId = finalCategoryId,
                timestamp = timestamp,
                sourcePackageName = packageName,
                sourceAppName = appName
            )
        )

        listener?.onTransactionProcessed(
            amount = amount,
            merchant = merchant,
            packageName = packageName,
            appName = appName,
            rawNotificationId = rawId,
            currencyCode = currencyCode,
            suggestedCategoryId = suggestedCategoryId,
            suggestedCategoryName = suggestedCategoryName,
            transactionId = transactionId
        )
    }

    private suspend fun saveToInbox(
        packageName: String,
        title: String?,
        text: String,
        timestamp: Long,
        stalePatternId: Long?,
        existingRawNotificationId: Long?
    ) {
        if (existingRawNotificationId != null) {
            val existing = rawNotificationDao.getById(existingRawNotificationId)
            if (existing != null) {
                rawNotificationDao.update(existing.copy(stalePatternId = stalePatternId))
            }
        } else {
            rawNotificationDao.insert(
                RawNotificationEntity(
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = timestamp,
                    stalePatternId = stalePatternId
                )
            )
        }
    }

    private fun isPotentialTransaction(text: String): Boolean {
        val amountRegex = Regex("""(?i)(?:[$\u20AC\u00A3\u00A5]|VND|USD|EUR|GBP|SGD)\s*\d+[\d.,]*|\d+[\d.,]*\s*(?:[$\u20AB\u20A9\u20AC\u00A3\u00A5]|VND|USD|EUR|GBP|SGD|[₫đ]\b)""")
        val financialKeywordRegex = Regex("""(?i)\b(?:spent|charged|paid|received|withdrew|payment|transfer|sent|debit|credit|alert|card|account|gd|giao\s*dịch|giao\s*dich|tài\s*khoản|tai\s*khoan|số\s*dư|so\s*du|chuyển|chuyen|nhận|nhan|rút|rut|nạp|nap|trừ|tru|cộng|cong|tk|sd|ck|phí|phi)\b""")
        
        val hasAmount = amountRegex.containsMatchIn(text)
        val hasKeyword = financialKeywordRegex.containsMatchIn(text)
        return hasAmount && hasKeyword
    }

    private fun parseAmount(amountStr: String): Double {
        val cleanedStr = amountStr.replace(Regex("[^0-9.,]"), "")
            .replace(",", ".")
        
        val firstDot = cleanedStr.indexOf('.')
        val lastDot = cleanedStr.lastIndexOf('.')
        
        val normalizedStr = if (firstDot != lastDot && firstDot != -1) {
            cleanedStr.replace(".", "")
        } else {
            cleanedStr
        }

        return try {
            normalizedStr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing amount", e)
            0.0
        }
    }
}
