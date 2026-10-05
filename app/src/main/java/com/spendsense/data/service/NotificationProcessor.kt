package com.spendsense.data.service

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.data.local.entity.MerchantCategoryMappingEntity
import com.spendsense.domain.model.NotificationRoutingMode
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
    private val whitelistedAppDao: WhitelistedAppDao,
    private val securePreferences: SecurePreferences? = null,
    private val directAiNotificationParser: DirectAiNotificationParser? = null
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

        fun onNotificationSkipped(
            packageName: String,
            appName: String,
            title: String?,
            text: String
        )

        fun onAddedToInbox(
            packageName: String,
            appName: String,
            title: String?,
            text: String
        )
    }

    private val TAG = "NotificationProcessor"
    private val regexCache = LruCache<String, Regex>(40)

    private fun getOrCreateRegex(patternStr: String): Regex? {
        return try {
            regexCache.get(patternStr) ?: run {
                val compiled = Regex(patternStr, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
                regexCache.put(patternStr, compiled)
                compiled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Invalid regex pattern: $patternStr", e)
            null
        }
    }

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

        val cleanText = normalizeNotificationText(text)
        val cleanTitle = title?.let { normalizeNotificationText(it) }?.takeIf { it.isNotBlank() }

        val routingMode = securePreferences?.getNotificationRoutingMode() ?: NotificationRoutingMode.REGEX_ONLY

        // ═════════════════════════════════════════════════════════════════════
        // MODE 2: AI ONLY — Completely bypass regex matching
        // ═════════════════════════════════════════════════════════════════════
        if (routingMode == NotificationRoutingMode.AI_ONLY) {
            val aiResult = directAiNotificationParser?.parseNotification(cleanTitle, cleanText, NotificationRoutingMode.AI_ONLY)
            if (aiResult == null) {
                saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId, appName, listener)
                Log.d(TAG, "AI Only mode: AI call failed or timed out for $packageName — saved to inbox")
                return@withContext ProcessResult.INBOX_CREATED
            }
            if (!aiResult.isTransaction) {
                Log.d(TAG, "AI Only mode: non-transaction for $packageName — discarded")
                if (existingRawNotificationId != null) {
                    rawNotificationDao.deleteById(existingRawNotificationId)
                }
                return@withContext ProcessResult.SILENT_SKIPPED
            }

            saveAndPostAiTransaction(
                aiResult = aiResult,
                packageName = packageName,
                appName = appName,
                notificationText = cleanText,
                notificationTitle = cleanTitle ?: appName,
                timestamp = timestamp,
                listener = listener,
                existingRawNotificationId = existingRawNotificationId
            )
            return@withContext ProcessResult.TRANSACTION_CREATED
        }

        // ═════════════════════════════════════════════════════════════════════
        // MODES 1 & 3: REGEX & AI OR REGEX ONLY
        // ═════════════════════════════════════════════════════════════════════
        val textCandidates = buildTextCandidates(cleanTitle, cleanText)
        val appPatterns = notificationPatternDao.getAllForPackage(packageName)

        if (appPatterns.isEmpty()) {
            if (routingMode == NotificationRoutingMode.REGEX_AND_AI) {
                val aiResult = directAiNotificationParser?.parseNotification(cleanTitle, cleanText, NotificationRoutingMode.REGEX_AND_AI)
                if (aiResult == null) {
                    saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId, appName, listener)
                    Log.d(TAG, "New app $packageName — AI failed/unavailable, saved to inbox")
                    return@withContext ProcessResult.INBOX_CREATED
                }
                if (!aiResult.isTransaction) {
                    Log.d(TAG, "New app $packageName — AI determined non-transaction — discarded")
                    if (existingRawNotificationId != null) {
                        rawNotificationDao.deleteById(existingRawNotificationId)
                    }
                    return@withContext ProcessResult.SILENT_SKIPPED
                }

                handleAiTransactionWithPattern(
                    aiResult = aiResult,
                    packageName = packageName,
                    appName = appName,
                    cleanTitle = cleanTitle,
                    cleanText = cleanText,
                    timestamp = timestamp,
                    listener = listener,
                    existingRawNotificationId = existingRawNotificationId
                )
                return@withContext ProcessResult.TRANSACTION_CREATED
            } else {
                saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId, appName, listener)
                Log.d(TAG, "New app $packageName — saved to inbox")
                return@withContext ProcessResult.INBOX_CREATED
            }
        }

        // Stage 0: Check if it matches any non-transaction (Skip/Ignore) patterns
        val skipPatterns = appPatterns.filter { !it.isTransaction }
        for (pattern in skipPatterns) {
            try {
                val isTitleMatch = cleanTitle != null && pattern.notificationTitle.isNotBlank() && cleanTitle.contains(pattern.notificationTitle, ignoreCase = true)
                val isRegexMatch = if (pattern.regex != null) {
                    val skipRegex = getOrCreateRegex(pattern.regex)
                    skipRegex != null && textCandidates.any { skipRegex.containsMatchIn(it) }
                } else {
                    isTitleMatch
                }

                if (isRegexMatch) {
                    if (pattern.regex != null) {
                        if (existingRawNotificationId != null) {
                            rawNotificationDao.markAsProcessed(existingRawNotificationId)
                        } else {
                            rawNotificationDao.insert(
                                RawNotificationEntity(
                                    packageName = packageName,
                                    title = title,
                                    text = text,
                                    timestamp = timestamp,
                                    isProcessed = true
                                )
                            )
                            listener?.onNotificationSkipped(packageName, appName, title, text)
                        }
                        rawNotificationDao.pruneProcessedForPackage(packageName, limit = 50)
                        Log.d(TAG, "Notification matched skip pattern (regex): ${pattern.regex} — skipped & saved")
                    } else {
                        if (existingRawNotificationId != null) {
                            rawNotificationDao.deleteById(existingRawNotificationId)
                        } else {
                            listener?.onNotificationSkipped(packageName, appName, title, text)
                        }
                        Log.d(TAG, "Notification matched skip pattern (title): ${pattern.notificationTitle} — skipped & deleted")
                    }
                    notificationPatternDao.upsert(pattern.copy(
                        lastMatchedAt = System.currentTimeMillis(),
                        matchCount = pattern.matchCount + 1
                    ))
                    return@withContext ProcessResult.SILENT_SKIPPED
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error matching skip pattern: ${pattern.regex}", e)
            }
        }

        val transactionPatterns = appPatterns.filter { it.isTransaction }
        if (transactionPatterns.isEmpty()) {
            if (routingMode == NotificationRoutingMode.REGEX_AND_AI) {
                val aiResult = directAiNotificationParser?.parseNotification(cleanTitle, cleanText, NotificationRoutingMode.REGEX_AND_AI)
                if (aiResult == null) {
                    saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId, appName, listener)
                    Log.d(TAG, "No transaction patterns for $packageName — AI failed, saved to inbox")
                    return@withContext ProcessResult.INBOX_CREATED
                }
                if (!aiResult.isTransaction) {
                    Log.d(TAG, "No transaction patterns for $packageName — AI determined non-transaction — discarded")
                    if (existingRawNotificationId != null) {
                        rawNotificationDao.deleteById(existingRawNotificationId)
                    }
                    return@withContext ProcessResult.SILENT_SKIPPED
                }

                handleAiTransactionWithPattern(
                    aiResult = aiResult,
                    packageName = packageName,
                    appName = appName,
                    cleanTitle = cleanTitle,
                    cleanText = cleanText,
                    timestamp = timestamp,
                    listener = listener,
                    existingRawNotificationId = existingRawNotificationId
                )
                return@withContext ProcessResult.TRANSACTION_CREATED
            } else {
                saveToInbox(packageName, title, text, timestamp, null, existingRawNotificationId, appName, listener)
                Log.d(TAG, "No transaction patterns configured for $packageName — saved to inbox")
                return@withContext ProcessResult.INBOX_CREATED
            }
        }

        // Stage 1: Scan for configured paymentSource identifiers in notification text or title
        val configuredPaymentSources = transactionPatterns
            .filter { it.paymentSource.isNotBlank() }
            .map { it.paymentSource.trim() }
            .distinct()
            .sortedByDescending { it.length }

        val matchedPaymentSource = configuredPaymentSources.firstOrNull { source ->
            textCandidates.any { it.contains(source, ignoreCase = true) }
        }

        // Stage 2: Prioritize candidate patterns
        val prioritizedPatterns = transactionPatterns.sortedWith(
            compareByDescending<NotificationPatternEntity> { pattern ->
                matchedPaymentSource != null && pattern.paymentSource.equals(matchedPaymentSource, ignoreCase = true)
            }.thenByDescending { pattern ->
                cleanTitle != null && pattern.notificationTitle.isNotBlank() && cleanTitle.contains(pattern.notificationTitle, ignoreCase = true)
            }.thenByDescending { pattern ->
                pattern.paymentSource.isBlank()
            }.thenByDescending { pattern ->
                pattern.notificationTitle.length
            }
        )

        var hasStalePattern = false
        var stalePatternId: Long? = null

        for (pattern in prioritizedPatterns) {
            if (pattern.regex != null) {
                val matched = tryMatchPattern(pattern, textCandidates, title ?: pattern.notificationTitle, packageName, appName, timestamp, listener, existingRawNotificationId)
                if (matched) {
                    return@withContext ProcessResult.TRANSACTION_CREATED
                }

                val isRelevant = (matchedPaymentSource != null && pattern.paymentSource.equals(matchedPaymentSource, ignoreCase = true)) ||
                        (cleanTitle != null && pattern.notificationTitle.isNotBlank() && cleanTitle.contains(pattern.notificationTitle, ignoreCase = true))
                if (isRelevant) {
                    hasStalePattern = true
                    stalePatternId = pattern.id
                }
            }
        }

        // If no regex matched:
        if (routingMode == NotificationRoutingMode.REGEX_AND_AI) {
            val aiResult = directAiNotificationParser?.parseNotification(cleanTitle, cleanText, NotificationRoutingMode.REGEX_AND_AI)
            if (aiResult == null) {
                saveToInbox(packageName, title, text, timestamp, stalePatternId, existingRawNotificationId, appName, listener)
                Log.d(TAG, "Unmatched regex for $packageName — AI failed/unavailable, saved to inbox")
                return@withContext ProcessResult.INBOX_CREATED
            }
            if (!aiResult.isTransaction) {
                Log.d(TAG, "Unmatched regex for $packageName — AI determined non-transaction — discarded")
                if (existingRawNotificationId != null) {
                    rawNotificationDao.deleteById(existingRawNotificationId)
                }
                return@withContext ProcessResult.SILENT_SKIPPED
            }

            handleAiTransactionWithPattern(
                aiResult = aiResult,
                packageName = packageName,
                appName = appName,
                cleanTitle = cleanTitle,
                cleanText = cleanText,
                timestamp = timestamp,
                listener = listener,
                existingRawNotificationId = existingRawNotificationId
            )
            return@withContext ProcessResult.TRANSACTION_CREATED
        } else {
            // Regex Only mode: route to pending inbox
            saveToInbox(packageName, title, text, timestamp, stalePatternId, existingRawNotificationId, appName, listener)
            Log.d(TAG, "No matching pattern regex for $packageName (stalePatternId=$stalePatternId) — saved to inbox")
            return@withContext ProcessResult.INBOX_CREATED
        }
    }

    private fun normalizeNotificationText(input: String): String {
        return input
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(Regex("[\u00A0\u2007\u202F\uFEFF]"), " ")
    }

    private fun buildTextCandidates(title: String?, text: String): List<String> {
        val list = mutableListOf(text)
        if (!title.isNullOrBlank()) {
            list.add("$title\n$text")
            list.add(title)
        }
        return list.distinct()
    }

    suspend fun reprocessInboxForPattern(
        pattern: NotificationPatternEntity,
        appName: String = "App"
    ): Int = withContext(Dispatchers.IO) {
        var recoveredCount = 0
        val unprocessed = rawNotificationDao.getUnprocessedForPackage(pattern.packageName)

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
            }
        }
        return@withContext recoveredCount
    }

    private suspend fun tryMatchPattern(
        pattern: NotificationPatternEntity,
        textCandidates: List<String>,
        notificationTitle: String,
        packageName: String,
        appName: String,
        timestamp: Long,
        listener: NotificationPostListener?,
        existingRawNotificationId: Long?
    ): Boolean {
        val regexStr = pattern.regex ?: return false
        val regex = getOrCreateRegex(regexStr) ?: return false
        try {
            for (candidate in textCandidates) {
                val matchResult = regex.find(candidate)
                if (matchResult != null) {
                    val amountStr = matchResult.groups["amount"]?.value
                    if (amountStr != null) {
                        val amount = parseAmount(amountStr)
                        if (amount > 0) {
                            val rawMerchant = matchResult.groups["merchant"]?.value?.trim()?.takeIf { it.isNotBlank() }
                            val merchant = rawMerchant
                                ?: pattern.paymentSource.takeIf { it.isNotBlank() }
                                ?: appName

                            notificationPatternDao.upsert(pattern.copy(
                                lastMatchedAt = System.currentTimeMillis()
                            ))
                            saveAndPostNotification(
                                amount = amount,
                                merchant = merchant,
                                packageName = packageName,
                                appName = appName,
                                currencyCode = pattern.currencyCode,
                                notificationText = candidate,
                                notificationTitle = notificationTitle,
                                timestamp = timestamp,
                                listener = listener,
                                existingRawNotificationId = existingRawNotificationId,
                                paymentSource = pattern.paymentSource,
                                paymentSourceType = pattern.paymentSourceType,
                                patternId = pattern.id
                            )
                            return true
                        }
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
        existingRawNotificationId: Long?,
        paymentSource: String,
        paymentSourceType: String,
        patternId: Long?
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

        // Prune processed raw notifications to keep storage efficient (limit to latest 50 per package)
        rawNotificationDao.pruneProcessedForPackage(packageName, limit = 50)

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
                sourceAppName = appName,
                paymentSource = paymentSource,
                paymentSourceType = paymentSourceType,
                patternId = patternId
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

    private suspend fun handleAiTransactionWithPattern(
        aiResult: DirectAiParseResult,
        packageName: String,
        appName: String,
        cleanTitle: String?,
        cleanText: String,
        timestamp: Long,
        listener: NotificationPostListener?,
        existingRawNotificationId: Long?
    ) {
        var savedPatternId: Long? = null
        if (!aiResult.regex.isNullOrBlank()) {
            try {
                Regex(aiResult.regex)
                val newPattern = NotificationPatternEntity(
                    packageName = packageName,
                    notificationTitle = cleanTitle ?: appName,
                    paymentSource = aiResult.paymentSource,
                    paymentSourceType = "AI Learned",
                    regex = aiResult.regex,
                    currencyCode = aiResult.currency.takeIf { it.isNotBlank() } ?: "USD",
                    isTransaction = true,
                    createdAt = System.currentTimeMillis(),
                    lastMatchedAt = System.currentTimeMillis(),
                    matchCount = 1
                )
                savedPatternId = notificationPatternDao.upsert(newPattern)
                Log.d(TAG, "Successfully learned and saved new regex pattern #$savedPatternId for $packageName: ${aiResult.regex}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to compile/save AI generated regex: ${aiResult.regex}", e)
            }
        }

        saveAndPostAiTransaction(
            aiResult = aiResult,
            packageName = packageName,
            appName = appName,
            notificationText = cleanText,
            notificationTitle = cleanTitle ?: appName,
            timestamp = timestamp,
            listener = listener,
            existingRawNotificationId = existingRawNotificationId,
            patternId = savedPatternId
        )
    }

    private suspend fun saveAndPostAiTransaction(
        aiResult: DirectAiParseResult,
        packageName: String,
        appName: String,
        notificationText: String,
        notificationTitle: String,
        timestamp: Long,
        listener: NotificationPostListener?,
        existingRawNotificationId: Long?,
        patternId: Long? = null
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

        rawNotificationDao.pruneProcessedForPackage(packageName, limit = 50)

        val allCategories = categoryDao.getAll()
        val aiMatchedCategory = aiResult.categoryName?.let { catName ->
            allCategories.firstOrNull { it.name.equals(catName.trim(), ignoreCase = true) }
        }

        val mapping = merchantCategoryMappingDao.getByMerchant(aiResult.merchant.lowercase())
        val suggestedCategoryId = aiMatchedCategory?.id ?: mapping?.categoryId
        val suggestedCategoryName = aiMatchedCategory?.name ?: if (suggestedCategoryId != null) {
            allCategories.firstOrNull { it.id == suggestedCategoryId }?.name
        } else null

        val finalCategoryId = suggestedCategoryId?.takeIf { it > 0 }
            ?: allCategories.firstOrNull { it.name.equals("Other", ignoreCase = true) }?.id
            ?: allCategories.firstOrNull()?.id
            ?: 1L

        val transactionId = transactionRepository.insertTransaction(
            Transaction(
                amount = aiResult.amount,
                currencyCode = aiResult.currency.takeIf { it.isNotBlank() } ?: "USD",
                merchant = aiResult.merchant.takeIf { it.isNotBlank() } ?: appName,
                categoryId = finalCategoryId,
                timestamp = timestamp,
                sourcePackageName = packageName,
                sourceAppName = appName,
                paymentSource = aiResult.paymentSource,
                paymentSourceType = "AI Detected",
                patternId = patternId
            )
        )

        listener?.onTransactionProcessed(
            amount = aiResult.amount,
            merchant = aiResult.merchant.takeIf { it.isNotBlank() } ?: appName,
            packageName = packageName,
            appName = appName,
            rawNotificationId = rawId,
            currencyCode = aiResult.currency.takeIf { it.isNotBlank() } ?: "USD",
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
        existingRawNotificationId: Long?,
        appName: String,
        listener: NotificationPostListener?
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
            listener?.onAddedToInbox(packageName, appName, title, text)
        }
    }

    private fun isPotentialTransaction(text: String): Boolean {
        val amountRegex = Regex("""(?i)(?:[$\u20AC\u00A3\u00A5]|VND|USD|EUR|GBP|SGD)\s*\d+[\d.,]*|\d+[\d.,]*\s*(?:[$\u20AB\u20A9\u20AC\u00A3\u00A5]|VND|USD|EUR|GBP|SGD|[₫đ]\b)""")
        val financialKeywordRegex = Regex("""(?i)\b(?:spent|charged|paid|received|withdrew|payment|transfer|sent|debit|credit|alert|card|account|gd|giao\s*dịch|giao\s*dich|tài\s*khoản|tai\s*khoan|số\s*dư|so\s*du|chuyển|chuyen|nhận|nhan|rút|rut|nạp|nap|trừ|tru|cộng|cong|tk|sd|ck|phí|phi)\b""")
        
        val hasAmount = amountRegex.containsMatchIn(text)
        val hasKeyword = financialKeywordRegex.containsMatchIn(text)
        return hasAmount && hasKeyword
    }

    fun parseAmount(amountStr: String): Double {
        val cleaned = amountStr.replace(Regex("[^0-9.,]"), "")
        if (cleaned.isEmpty()) return 0.0

        val hasComma = cleaned.contains(',')
        val hasDot = cleaned.contains('.')

        val normalized = if (hasComma && hasDot) {
            val commaIndex = cleaned.lastIndexOf(',')
            val dotIndex = cleaned.lastIndexOf('.')
            if (commaIndex > dotIndex) {
                cleaned.replace(".", "").replace(",", ".")
            } else {
                cleaned.replace(",", "")
            }
        } else if (hasComma) {
            val lastCommaOffset = cleaned.length - 1 - cleaned.lastIndexOf(',')
            if (lastCommaOffset == 3) {
                cleaned.replace(",", "")
            } else {
                cleaned.replace(",", ".")
            }
        } else if (hasDot) {
            val lastDotOffset = cleaned.length - 1 - cleaned.lastIndexOf('.')
            if (lastDotOffset == 3) {
                cleaned.replace(".", "")
            } else {
                cleaned
            }
        } else {
            cleaned
        }

        return try {
            normalized.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing amount", e)
            0.0
        }
    }
}
