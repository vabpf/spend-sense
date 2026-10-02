package com.spendsense.data.backup

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BackupPayload(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("app") val app: String = "SpendSense",
    @SerializedName("exportedAt") val exportedAt: Long = System.currentTimeMillis(),
    @SerializedName("exportedAtFormatted") val exportedAtFormatted: String = "",
    @SerializedName("preferences") val preferences: BackupPreferences? = null,
    @SerializedName("categories") val categories: List<BackupCategory> = emptyList(),
    @SerializedName("notificationPatterns") val notificationPatterns: List<BackupNotificationPattern> = emptyList(),
    @SerializedName("transactions") val transactions: List<BackupTransaction> = emptyList(),
    @SerializedName("whitelistedApps") val whitelistedApps: List<BackupWhitelistedApp> = emptyList(),
    @SerializedName("merchantCategoryMappings") val merchantCategoryMappings: List<BackupMerchantCategoryMapping> = emptyList(),
    @SerializedName("providerAccounts") val providerAccounts: List<BackupProviderAccount> = emptyList(),
    @SerializedName("rawNotifications") val rawNotifications: List<BackupRawNotification> = emptyList()
)

@Keep
data class BackupPreferences(
    @SerializedName("defaultCurrency") val defaultCurrency: String = "USD",
    @SerializedName("dailyReportEnabled") val dailyReportEnabled: Boolean = false,
    @SerializedName("dailyReportTime") val dailyReportTime: String = "20:00",
    @SerializedName("regexTitle") val regexTitle: String = "",
    @SerializedName("regexText") val regexText: String = "",
    @SerializedName("regexManualPattern") val regexManualPattern: String = "",
    @SerializedName("regexSelectedProvider") val regexSelectedProvider: Long = -1L,
    @SerializedName("apiKeys") val apiKeys: Map<String, String> = emptyMap()
)

@Keep
data class BackupCategory(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String,
    @SerializedName("iconName") val iconName: String,
    @SerializedName("colorHex") val colorHex: String,
    @SerializedName("isDefault") val isDefault: Boolean = false,
    @SerializedName("createdAt") val createdAt: Long = 0L
)

@Keep
data class BackupNotificationPattern(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("notificationTitle") val notificationTitle: String,
    @SerializedName("paymentSource") val paymentSource: String = "",
    @SerializedName("paymentSourceType") val paymentSourceType: String = "Credit Card",
    @SerializedName("regex") val regex: String? = null,
    @SerializedName("currencyCode") val currencyCode: String = "USD",
    @SerializedName("isTransaction") val isTransaction: Boolean = true,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("lastMatchedAt") val lastMatchedAt: Long? = null,
    @SerializedName("matchCount") val matchCount: Int = 0
)

@Keep
data class BackupTransaction(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("amount") val amount: Double,
    @SerializedName("currencyCode") val currencyCode: String = "USD",
    @SerializedName("merchant") val merchant: String,
    @SerializedName("categoryId") val categoryId: Long,
    @SerializedName("categoryName") val categoryName: String = "",
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("sourcePackageName") val sourcePackageName: String = "",
    @SerializedName("sourceAppName") val sourceAppName: String = "",
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("isSynced") val isSynced: Boolean = false,
    @SerializedName("firestoreId") val firestoreId: String? = null,
    @SerializedName("paymentSource") val paymentSource: String = "Manual",
    @SerializedName("paymentSourceType") val paymentSourceType: String = "Manual",
    @SerializedName("patternId") val patternId: Long? = null
)

@Keep
data class BackupWhitelistedApp(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("isEnabled") val isEnabled: Boolean = true,
    @SerializedName("addedAt") val addedAt: Long = 0L
)

@Keep
data class BackupMerchantCategoryMapping(
    @SerializedName("merchant") val merchant: String,
    @SerializedName("categoryId") val categoryId: Long,
    @SerializedName("categoryName") val categoryName: String = "",
    @SerializedName("usageCount") val usageCount: Int = 1,
    @SerializedName("lastUsed") val lastUsed: Long = 0L
)

@Keep
data class BackupProviderAccount(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String,
    @SerializedName("baseUrl") val baseUrl: String,
    @SerializedName("jobType") val jobType: String,
    @SerializedName("isPreset") val isPreset: Boolean = false,
    @SerializedName("apiKey") val apiKey: String? = null,
    @SerializedName("models") val models: List<BackupProviderModel> = emptyList()
)

@Keep
data class BackupProviderModel(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("modelId") val modelId: String,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("isEnabled") val isEnabled: Boolean = false,
    @SerializedName("stale") val stale: Boolean = false,
    @SerializedName("lastRefreshedAt") val lastRefreshedAt: Long = 0L
)

@Keep
data class BackupRawNotification(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("title") val title: String? = null,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("isProcessed") val isProcessed: Boolean = false,
    @SerializedName("stalePatternId") val stalePatternId: Long? = null
)

data class RestoreSummary(
    val transactionsRestored: Int,
    val categoriesRestored: Int,
    val patternsRestored: Int,
    val appsRestored: Int,
    val mappingsRestored: Int,
    val providerAccountsRestored: Int
)
