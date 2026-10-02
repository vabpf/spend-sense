package com.spendsense.data.backup

import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.spendsense.data.local.SecurePreferences
import com.spendsense.data.local.SpendSenseDatabase
import com.spendsense.data.local.dao.*
import com.spendsense.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestoreManager @Inject constructor(
    private val database: SpendSenseDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val notificationPatternDao: NotificationPatternDao,
    private val whitelistedAppDao: WhitelistedAppDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val providerAccountDao: ProviderAccountDao,
    private val providerModelDao: ProviderModelDao,
    private val rawNotificationDao: RawNotificationDao,
    private val securePreferences: SecurePreferences
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val categories = categoryDao.getAll()
        val categoryNameMap = categories.associate { it.id to it.name }

        val transactions = transactionDao.getAll().map { t ->
            BackupTransaction(
                id = t.id,
                amount = t.amount,
                currencyCode = t.currencyCode,
                merchant = t.merchant,
                categoryId = t.categoryId,
                categoryName = categoryNameMap[t.categoryId] ?: "",
                timestamp = t.timestamp,
                sourcePackageName = t.sourcePackageName,
                sourceAppName = t.sourceAppName,
                notes = t.notes,
                isSynced = t.isSynced,
                firestoreId = t.firestoreId,
                paymentSource = t.paymentSource,
                paymentSourceType = t.paymentSourceType,
                patternId = t.patternId
            )
        }

        val patterns = notificationPatternDao.getAll().map { p ->
            BackupNotificationPattern(
                id = p.id,
                packageName = p.packageName,
                notificationTitle = p.notificationTitle,
                paymentSource = p.paymentSource,
                paymentSourceType = p.paymentSourceType,
                regex = p.regex,
                currencyCode = p.currencyCode,
                isTransaction = p.isTransaction,
                createdAt = p.createdAt,
                lastMatchedAt = p.lastMatchedAt,
                matchCount = p.matchCount
            )
        }

        val apps = whitelistedAppDao.getAll().map { a ->
            BackupWhitelistedApp(
                packageName = a.packageName,
                appName = a.appName,
                isEnabled = a.isEnabled,
                addedAt = a.addedAt
            )
        }

        val mappings = merchantCategoryMappingDao.getAll().map { m ->
            BackupMerchantCategoryMapping(
                merchant = m.merchant,
                categoryId = m.categoryId,
                categoryName = categoryNameMap[m.categoryId] ?: "",
                usageCount = m.usageCount,
                lastUsed = m.lastUsed
            )
        }

        val providerAccounts = providerAccountDao.getAll().map { acc ->
            val models = providerModelDao.getByAccountId(acc.id).map { m ->
                BackupProviderModel(
                    id = m.id,
                    modelId = m.modelId,
                    displayName = m.displayName,
                    isEnabled = m.isEnabled,
                    stale = m.stale,
                    lastRefreshedAt = m.lastRefreshedAt
                )
            }
            BackupProviderAccount(
                id = acc.id,
                name = acc.name,
                baseUrl = acc.baseUrl,
                jobType = acc.jobType,
                isPreset = acc.isPreset,
                apiKey = securePreferences.getApiKey(acc.id),
                models = models
            )
        }

        val rawNotifications = rawNotificationDao.getAll().map { r ->
            BackupRawNotification(
                id = r.id,
                packageName = r.packageName,
                title = r.title,
                text = r.text,
                timestamp = r.timestamp,
                isProcessed = r.isProcessed,
                stalePatternId = r.stalePatternId
            )
        }

        val now = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))

        val preferences = BackupPreferences(
            defaultCurrency = securePreferences.getDefaultCurrency(),
            dailyReportEnabled = securePreferences.isDailyReportEnabled(),
            dailyReportTime = securePreferences.getDailyReportTime(),
            regexTitle = securePreferences.getRegexTitle(),
            regexText = securePreferences.getRegexText(),
            regexManualPattern = securePreferences.getRegexManualPattern(),
            regexSelectedProvider = securePreferences.getSelectedProviderId(),
            apiKeys = securePreferences.getAllApiKeys()
        )

        val payload = BackupPayload(
            version = 1,
            app = "SpendSense",
            exportedAt = now,
            exportedAtFormatted = formattedDate,
            preferences = preferences,
            categories = categories.map { c ->
                BackupCategory(
                    id = c.id,
                    name = c.name,
                    iconName = c.iconName,
                    colorHex = c.colorHex,
                    isDefault = c.isDefault,
                    createdAt = c.createdAt
                )
            },
            notificationPatterns = patterns,
            transactions = transactions,
            whitelistedApps = apps,
            merchantCategoryMappings = mappings,
            providerAccounts = providerAccounts,
            rawNotifications = rawNotifications
        )

        gson.toJson(payload)
    }

    fun parseBackupJson(jsonString: String): BackupPayload {
        val trimmed = jsonString.trim()
        if (!trimmed.startsWith("{")) {
            throw IllegalArgumentException("Invalid backup file: Not a valid JSON object.")
        }
        val payload = gson.fromJson(trimmed, BackupPayload::class.java)
            ?: throw IllegalArgumentException("Failed to parse backup content.")

        if (payload.app != "SpendSense" && payload.transactions.isEmpty() && payload.categories.isEmpty() && payload.notificationPatterns.isEmpty()) {
            throw IllegalArgumentException("Selected file does not appear to be a SpendSense backup.")
        }

        return payload
    }

    suspend fun restoreBackup(payload: BackupPayload, replaceExisting: Boolean): RestoreSummary = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (replaceExisting) {
                transactionDao.deleteAll()
                merchantCategoryMappingDao.deleteAll()
                rawNotificationDao.deleteAll()
                notificationPatternDao.deleteAll()
                categoryDao.deleteAll()
                providerModelDao.deleteAll()
                providerAccountDao.deleteAll()
                whitelistedAppDao.deleteAll()
            }

            // 1. Restore Categories
            val currentCategories = categoryDao.getAll()
            val currentByName = currentCategories.associateBy { it.name.lowercase().trim() }.toMutableMap()
            val categoryIdMap = mutableMapOf<Long, Long>()

            var categoriesRestored = 0
            for (cat in payload.categories) {
                val normalizedName = cat.name.lowercase().trim()
                if (currentByName.containsKey(normalizedName)) {
                    val existing = currentByName[normalizedName]!!
                    categoryIdMap[cat.id] = existing.id
                } else {
                    val newId = categoryDao.insert(
                        CategoryEntity(
                            id = if (replaceExisting && cat.id > 0) cat.id else 0,
                            name = cat.name,
                            iconName = cat.iconName,
                            colorHex = cat.colorHex,
                            isDefault = cat.isDefault,
                            createdAt = if (cat.createdAt > 0) cat.createdAt else System.currentTimeMillis()
                        )
                    )
                    val insertedEntity = CategoryEntity(
                        id = newId,
                        name = cat.name,
                        iconName = cat.iconName,
                        colorHex = cat.colorHex,
                        isDefault = cat.isDefault
                    )
                    currentByName[normalizedName] = insertedEntity
                    categoryIdMap[cat.id] = newId
                    categoriesRestored++
                }
            }

            val fallbackCategoryId = categoryDao.getAll().firstOrNull()?.id ?: 1L

            // 2. Restore Notification Patterns
            val currentPatterns = notificationPatternDao.getAll()
            fun patternKey(pkg: String, title: String, source: String) = "${pkg}___${title}___${source}"
            val currentPatternMap = currentPatterns.associateBy { patternKey(it.packageName, it.notificationTitle, it.paymentSource) }.toMutableMap()
            val patternIdMap = mutableMapOf<Long, Long>()

            var patternsRestored = 0
            for (pat in payload.notificationPatterns) {
                val key = patternKey(pat.packageName, pat.notificationTitle, pat.paymentSource)
                if (currentPatternMap.containsKey(key)) {
                    val existing = currentPatternMap[key]!!
                    val updated = existing.copy(
                        paymentSourceType = pat.paymentSourceType,
                        regex = pat.regex,
                        currencyCode = pat.currencyCode,
                        isTransaction = pat.isTransaction,
                        matchCount = maxOf(existing.matchCount, pat.matchCount),
                        lastMatchedAt = pat.lastMatchedAt ?: existing.lastMatchedAt
                    )
                    notificationPatternDao.upsert(updated)
                    patternIdMap[pat.id] = existing.id
                    patternsRestored++
                } else {
                    val newId = notificationPatternDao.upsert(
                        NotificationPatternEntity(
                            id = if (replaceExisting && pat.id > 0) pat.id else 0,
                            packageName = pat.packageName,
                            notificationTitle = pat.notificationTitle,
                            paymentSource = pat.paymentSource,
                            paymentSourceType = pat.paymentSourceType,
                            regex = pat.regex,
                            currencyCode = pat.currencyCode,
                            isTransaction = pat.isTransaction,
                            createdAt = if (pat.createdAt > 0) pat.createdAt else System.currentTimeMillis(),
                            lastMatchedAt = pat.lastMatchedAt,
                            matchCount = pat.matchCount
                        )
                    )
                    patternIdMap[pat.id] = newId
                    patternsRestored++
                }
            }

            // 3. Restore Whitelisted Apps
            val currentApps = whitelistedAppDao.getAll().associateBy { it.packageName }
            var appsRestored = 0
            for (app in payload.whitelistedApps) {
                if (replaceExisting || !currentApps.containsKey(app.packageName)) {
                    whitelistedAppDao.insert(
                        WhitelistedAppEntity(
                            packageName = app.packageName,
                            appName = app.appName,
                            isEnabled = app.isEnabled,
                            addedAt = if (app.addedAt > 0) app.addedAt else System.currentTimeMillis()
                        )
                    )
                    appsRestored++
                } else {
                    whitelistedAppDao.setEnabled(app.packageName, app.isEnabled)
                }
            }

            // 4. Restore Transactions
            val existingSignatures = if (!replaceExisting) {
                transactionDao.getAll().map { "${it.timestamp}_${it.merchant}_${it.amount}_${it.sourcePackageName}" }.toSet()
            } else {
                emptySet()
            }

            val transactionsToInsert = mutableListOf<TransactionEntity>()
            var transactionsRestored = 0

            for (t in payload.transactions) {
                val sig = "${t.timestamp}_${t.merchant}_${t.amount}_${t.sourcePackageName}"
                if (!replaceExisting && existingSignatures.contains(sig)) {
                    continue
                }

                val targetCatId = categoryIdMap[t.categoryId]
                    ?: currentByName[t.categoryName.lowercase().trim()]?.id
                    ?: fallbackCategoryId

                val targetPatId = t.patternId?.let { patternIdMap[it] }

                transactionsToInsert.add(
                    TransactionEntity(
                        id = if (replaceExisting && t.id > 0) t.id else 0,
                        amount = t.amount,
                        currencyCode = t.currencyCode,
                        merchant = t.merchant,
                        categoryId = targetCatId,
                        timestamp = t.timestamp,
                        sourcePackageName = t.sourcePackageName,
                        sourceAppName = t.sourceAppName,
                        notes = t.notes,
                        isSynced = t.isSynced,
                        firestoreId = t.firestoreId,
                        paymentSource = t.paymentSource,
                        paymentSourceType = t.paymentSourceType,
                        patternId = targetPatId
                    )
                )
                transactionsRestored++
            }

            if (transactionsToInsert.isNotEmpty()) {
                transactionDao.insertAll(transactionsToInsert)
            }

            // 5. Restore Merchant Category Mappings
            var mappingsRestored = 0
            for (m in payload.merchantCategoryMappings) {
                val targetCatId = categoryIdMap[m.categoryId]
                    ?: currentByName[m.categoryName.lowercase().trim()]?.id
                    ?: fallbackCategoryId

                merchantCategoryMappingDao.upsert(
                    MerchantCategoryMappingEntity(
                        merchant = m.merchant,
                        categoryId = targetCatId,
                        usageCount = m.usageCount,
                        lastUsed = if (m.lastUsed > 0) m.lastUsed else System.currentTimeMillis()
                    )
                )
                mappingsRestored++
            }

            // 6. Restore Provider Accounts & Models
            val currentAccounts = providerAccountDao.getAll()
            var providerAccountsRestored = 0
            for (acc in payload.providerAccounts) {
                val existingAcc = currentAccounts.find { it.name.equals(acc.name, ignoreCase = true) || it.baseUrl.equals(acc.baseUrl, ignoreCase = true) }
                val targetAccId = if (replaceExisting || existingAcc == null) {
                    providerAccountDao.insert(
                        ProviderAccountEntity(
                            id = if (replaceExisting && acc.id > 0) acc.id else 0,
                            name = acc.name,
                            baseUrl = acc.baseUrl,
                            jobType = acc.jobType,
                            isPreset = acc.isPreset
                        )
                    )
                } else {
                    existingAcc.id
                }

                if (!acc.apiKey.isNullOrBlank()) {
                    securePreferences.saveApiKey(targetAccId, acc.apiKey)
                }

                if (acc.models.isNotEmpty()) {
                    providerModelDao.upsertAll(
                        acc.models.map { m ->
                            ProviderModelEntity(
                                id = 0,
                                providerAccountId = targetAccId,
                                modelId = m.modelId,
                                displayName = m.displayName,
                                isEnabled = m.isEnabled,
                                stale = m.stale,
                                lastRefreshedAt = m.lastRefreshedAt
                            )
                        }
                    )
                }
                providerAccountsRestored++
            }

            // 7. Restore Raw Notifications
            if (payload.rawNotifications.isNotEmpty()) {
                rawNotificationDao.insertAll(
                    payload.rawNotifications.map { r ->
                        RawNotificationEntity(
                            id = if (replaceExisting && r.id > 0) r.id else 0,
                            packageName = r.packageName,
                            title = r.title,
                            text = r.text,
                            timestamp = r.timestamp,
                            isProcessed = r.isProcessed,
                            stalePatternId = r.stalePatternId
                        )
                    }
                )
            }

            // 8. Restore Preferences
            payload.preferences?.let { p ->
                if (p.defaultCurrency.isNotBlank()) {
                    securePreferences.setDefaultCurrency(p.defaultCurrency)
                }
                securePreferences.setDailyReportEnabled(p.dailyReportEnabled)
                if (p.dailyReportTime.isNotBlank()) {
                    securePreferences.setDailyReportTime(p.dailyReportTime)
                }
                if (p.regexTitle.isNotBlank() || p.regexText.isNotBlank() || p.regexManualPattern.isNotBlank()) {
                    securePreferences.saveRegexInput(p.regexTitle, p.regexText, p.regexManualPattern)
                }
                if (p.regexSelectedProvider >= 0) {
                    securePreferences.saveSelectedProviderId(p.regexSelectedProvider)
                }
                if (p.apiKeys.isNotEmpty()) {
                    securePreferences.restoreApiKeys(p.apiKeys)
                }
            }

            RestoreSummary(
                transactionsRestored = transactionsRestored,
                categoriesRestored = categoriesRestored,
                patternsRestored = patternsRestored,
                appsRestored = appsRestored,
                mappingsRestored = mappingsRestored,
                providerAccountsRestored = providerAccountsRestored
            )
        }
    }
}
