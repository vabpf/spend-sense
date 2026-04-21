package com.spendsense.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
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

    fun setDefaultCurrency(currencyCode: String) {
        prefs.edit().putString("default_currency", currencyCode).apply()
    }

    fun getDefaultCurrency(): String {
        return prefs.getString("default_currency", "USD") ?: "USD"
    }
}
