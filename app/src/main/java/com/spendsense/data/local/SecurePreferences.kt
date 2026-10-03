package com.spendsense.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(providerId: Long, apiKey: String) {
        prefs.edit().putString("api_key_$providerId", apiKey).apply()
    }

    fun saveApiKeyForProviderKey(providerKey: String, apiKey: String) {
        prefs.edit().putString("api_key_group_$providerKey", apiKey).apply()
    }

    fun getApiKey(providerId: Long): String? {
        return prefs.getString("api_key_$providerId", null)
    }

    fun getApiKeyForProviderKey(providerKey: String): String? {
        return prefs.getString("api_key_group_$providerKey", null)
    }

    fun deleteApiKey(providerId: Long) {
        prefs.edit().remove("api_key_$providerId").apply()
    }

    fun deleteApiKeyForProviderKey(providerKey: String) {
        prefs.edit().remove("api_key_group_$providerKey").apply()
    }

    fun getAllApiKeys(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("api_key_") && value is String) {
                result[key] = value
            }
        }
        return result
    }

    fun restoreApiKeys(keys: Map<String, String>) {
        val editor = prefs.edit()
        for ((key, value) in keys) {
            editor.putString(key, value)
        }
        editor.apply()
    }

    fun setDefaultCurrency(currencyCode: String) {
        prefs.edit().putString("default_currency", currencyCode).apply()
    }

    fun getDefaultCurrency(): String {
        return prefs.getString("default_currency", "USD") ?: "USD"
    }

    fun saveRegexInput(title: String, text: String, manualPattern: String) {
        prefs.edit()
            .putString("regex_title", title)
            .putString("regex_text", text)
            .putString("regex_manual_pattern", manualPattern)
            .apply()
    }

    fun getRegexTitle(): String = prefs.getString("regex_title", "") ?: ""
    fun getRegexText(): String = prefs.getString("regex_text", "") ?: ""
    fun getRegexManualPattern(): String = prefs.getString("regex_manual_pattern", "") ?: ""

    fun clearRegexInput() {
        prefs.edit()
            .remove("regex_title")
            .remove("regex_text")
            .remove("regex_manual_pattern")
            .apply()
    }

    fun saveSelectedProviderId(providerId: Long) {
        prefs.edit().putLong("regex_selected_provider", providerId).apply()
    }

    fun getSelectedProviderId(): Long {
        return prefs.getLong("regex_selected_provider", -1L)
    }

    fun setDailyReportEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_report_enabled", enabled).apply()
    }

    fun isDailyReportEnabled(): Boolean {
        return prefs.getBoolean("daily_report_enabled", false)
    }

    fun setDailyReportTime(time: String) {
        prefs.edit().putString("daily_report_time", time).apply()
    }

    fun getDailyReportTime(): String {
        return prefs.getString("daily_report_time", "20:00") ?: "20:00"
    }

    private val _backgroundThemeFlow = MutableStateFlow(getBackgroundTheme())
    val backgroundThemeFlow: StateFlow<String> = _backgroundThemeFlow.asStateFlow()

    private val _customBackgroundPathFlow = MutableStateFlow(getCustomBackgroundPath())
    val customBackgroundPathFlow: StateFlow<String?> = _customBackgroundPathFlow.asStateFlow()

    fun getBackgroundTheme(): String {
        return prefs.getString("background_theme", "CYBERPUNK_DEFAULT") ?: "CYBERPUNK_DEFAULT"
    }

    fun setBackgroundTheme(theme: String) {
        prefs.edit().putString("background_theme", theme).apply()
        _backgroundThemeFlow.value = theme
    }

    fun getCustomBackgroundPath(): String? {
        return prefs.getString("custom_background_path", null)
    }

    fun setCustomBackgroundPath(path: String?) {
        if (path == null) {
            prefs.edit().remove("custom_background_path").apply()
        } else {
            prefs.edit().putString("custom_background_path", path).apply()
        }
        _customBackgroundPathFlow.value = path
    }
}
