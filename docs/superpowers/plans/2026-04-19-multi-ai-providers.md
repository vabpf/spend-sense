# Multi-AI Provider Support & Secure Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend SpendSense to support multiple AI providers (OpenRouter, NVIDIA NIM, Google Gemini, OpenCode) for regex generation with secure API key storage and a unified OpenAI-compatible API client.

**Architecture:** 
- Implement `SecurePreferences` using `EncryptedSharedPreferences` for API keys.
- Create a `DynamicBaseUrlInterceptor` for OkHttp to handle multi-provider routing.
- Refactor `OpenRouterApi` into a generic `ChatCompletionApi`.
- Pre-populate the database with AI provider presets (OpenCode, NVIDIA, Google).
- Update UI to allow per-task provider selection and secure key entry.

**Tech Stack:** Kotlin, Jetpack Compose, Retrofit, Room, Hilt, AndroidX Security (Crypto).

---

### Task 1: Setup Security Dependency

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add AndroidX Security dependency**

Add `implementation("androidx.security:security-crypto:1.1.0-alpha06")` to `dependencies` block.

- [ ] **Step 2: Sync Gradle**

Run: `./gradlew help` (to trigger sync)

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: add androidx security-crypto dependency"
```

---

### Task 2: Implement Secure Storage

**Files:**
- Create: `app/src/main/java/com/spendsense/data/local/SecurePreferences.kt`

- [ ] **Step 1: Create SecurePreferences class**

```kotlin
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

    fun getApiKey(providerId: Long): String? {
        return prefs.getString("api_key_$providerId", null)
    }

    fun deleteApiKey(providerId: Long) {
        prefs.edit().remove("api_key_$providerId").apply()
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/spendsense/data/local/SecurePreferences.kt
git commit -m "feat: implement SecurePreferences using EncryptedSharedPreferences"
```

---

### Task 3: Refactor AI Provider Entity & Database Initialization

**Files:**
- Modify: `app/src/main/java/com/spendsense/data/local/entity/AiProviderEntity.kt`
- Modify: `app/src/main/java/com/spendsense/data/local/SpendSenseDatabase.kt`

- [ ] **Step 1: Remove apiKey from AiProviderEntity (it's now in SecurePreferences)**

```kotlin
@Entity(tableName = "ai_providers")
data class AiProviderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val defaultModel: String,
    val jobType: String,
    val isPreset: Boolean = false // New field
)
```

- [ ] **Step 2: Update Room Database Version & Callback for Presets**

Increment version to 2 (or higher if already advanced) and add presets in `RoomDatabase.Callback`.

```kotlin
// Inside SpendSenseDatabase.kt
abstract class SpendSenseDatabase : RoomDatabase() {
    // ...
    companion object {
        fun buildDatabase(context: Context) = Room.databaseBuilder(
            context, SpendSenseDatabase::class.java, "spendsense.db"
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Insert presets manually via raw SQL for simplicity in Callback
                db.execSQL("INSERT INTO ai_providers (name, baseUrl, defaultModel, jobType, isPreset) VALUES " +
                    "('OpenCode (MiniMax)', 'https://opencode.ai/zen/v1', 'oc/minimax-m2.5-free', 'REGEX_GEN', 1), " +
                    "('OpenCode (Trinity)', 'https://opencode.ai/zen/v1', 'oc/trinity-large-preview-free', 'REGEX_GEN', 1), " +
                    "('OpenCode (Nemotron)', 'https://opencode.ai/zen/v1', 'oc/nemotron-3-super-free', 'REGEX_GEN', 1), " +
                    "('NVIDIA NIM', 'https://integrate.api.nvidia.com/v1', 'meta/llama-3.1-8b-instruct', 'REGEX_GEN', 1), " +
                    "('Google Gemini', 'https://generativelanguage.googleapis.com/v1beta/openai/', 'gemini-1.5-flash', 'REGEX_GEN', 1)")
            }
        }).fallbackToDestructiveMigration().build()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/spendsense/data/local/entity/AiProviderEntity.kt app/src/main/java/com/spendsense/data/local/SpendSenseDatabase.kt
git commit -m "feat: refactor AiProviderEntity and add database presets"
```

---

### Task 4: Dynamic API Client & Interceptor

**Files:**
- Create: `app/src/main/java/com/spendsense/data/remote/DynamicBaseUrlInterceptor.kt`
- Modify: `app/src/main/java/com/spendsense/data/remote/OpenRouterApi.kt` (Rename to `ChatCompletionApi.kt`)
- Modify: `app/src/main/java/com/spendsense/di/NetworkModule.kt`

- [ ] **Step 1: Create DynamicBaseUrlInterceptor**

```kotlin
package com.spendsense.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor() : Interceptor {
    @Volatile private var host: String? = null
    @Volatile private var apiKey: String? = null
    @Volatile private var isOpenRouter: Boolean = false

    fun setBaseUrl(url: String, key: String?, isOpenRouter: Boolean = false) {
        this.host = url
        this.apiKey = key
        this.isOpenRouter = isOpenRouter
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val newHost = host?.toHttpUrlOrNull()

        if (newHost != null) {
            val newUrl = request.url.newBuilder()
                .scheme(newHost.scheme)
                .host(newHost.host)
                .port(newHost.port)
                .build()
            
            val requestBuilder = request.newBuilder().url(newUrl)
            
            apiKey?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }
            
            if (isOpenRouter) {
                requestBuilder.header("HTTP-Referer", "https://spendsense.app")
                requestBuilder.header("X-Title", "SpendSense")
            }

            request = requestBuilder.build()
        }
        return chain.proceed(request)
    }
}
```

- [ ] **Step 2: Rename and Generalize API Interface**

Rename `OpenRouterApi.kt` to `ChatCompletionApi.kt` and `OpenRouterApi` to `ChatCompletionApi`.

- [ ] **Step 3: Update NetworkModule**

Inject the interceptor into OkHttpClient.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/spendsense/data/remote/DynamicBaseUrlInterceptor.kt app/src/main/java/com/spendsense/data/remote/ChatCompletionApi.kt app/src/main/java/com/spendsense/di/NetworkModule.kt
git commit -m "feat: implement DynamicBaseUrlInterceptor and generic ChatCompletionApi"
```

---

### Task 5: Update ViewModels (Settings & Generator)

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/settings/AiProvidersViewModel.kt`
- Modify: `app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorViewModel.kt`

- [ ] **Step 1: Update AiProvidersViewModel to use SecurePreferences**

Modify `saveProvider` to save key to `SecurePreferences`.

- [ ] **Step 2: Update RegexGeneratorViewModel to support provider selection**

Add `selectedProvider` to state, fetch providers from DAO, and call `interceptor.setBaseUrl(...)` before generating.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/spendsense/presentation/settings/AiProvidersViewModel.kt app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorViewModel.kt
git commit -m "feat: connect ViewModels to SecurePreferences and Dynamic Interceptor"
```

---

### Task 4: UI Updates (Dropdown & Secure Input)

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorScreen.kt`
- Modify: `app/src/main/java/com/spendsense/presentation/settings/AiProvidersScreen.kt`

- [ ] **Step 1: Add Dropdown to RegexGeneratorScreen**

- [ ] **Step 2: Update AiProvidersScreen with preset logic and secure key entry**

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorScreen.kt app/src/main/java/com/spendsense/presentation/settings/AiProvidersScreen.kt
git commit -m "feat: update UI for provider selection and secure key input"
```
