# Dynamic API Client & Interceptor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a generic API client that supports dynamic base URLs and provider-specific headers via an interceptor.

**Architecture:** Use an OkHttp Interceptor to dynamically change the request URL and inject headers (Authorization, OpenRouter-specific headers) based on the current AI provider configuration. Refactor the existing OpenRouter-specific API interface into a generic ChatCompletionApi.

**Tech Stack:** Kotlin, OkHttp, Retrofit, Dagger Hilt.

---

### Task 1: Create DynamicBaseUrlInterceptor

**Files:**
- Create: `app/src/main/java/com/spendsense/data/remote/DynamicBaseUrlInterceptor.kt`

- [ ] **Step 1: Create the Interceptor file**

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

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/spendsense/data/remote/DynamicBaseUrlInterceptor.kt
git commit -m "feat: add DynamicBaseUrlInterceptor"
```

### Task 2: Refactor OpenRouterApi to ChatCompletionApi

**Files:**
- Rename: `app/src/main/java/com/spendsense/data/remote/OpenRouterApi.kt` -> `app/src/main/java/com/spendsense/data/remote/ChatCompletionApi.kt`
- Modify: `app/src/main/java/com/spendsense/data/remote/ChatCompletionApi.kt`

- [ ] **Step 1: Rename the file**

Run: `mv app/src/main/java/com/spendsense/data/remote/OpenRouterApi.kt app/src/main/java/com/spendsense/data/remote/ChatCompletionApi.kt`

- [ ] **Step 2: Update the interface name and remove manual headers**

```kotlin
package com.spendsense.data.remote

import com.spendsense.data.remote.model.ChatCompletionRequest
import com.spendsense.data.remote.model.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatCompletionApi {
    @POST("chat/completions")
    suspend fun generateCompletion(
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/spendsense/data/remote/ChatCompletionApi.kt
git commit -m "refactor: rename OpenRouterApi to ChatCompletionApi and remove manual headers"
```

### Task 3: Update NetworkModule

**Files:**
- Modify: `app/src/main/java/com/spendsense/di/NetworkModule.kt`

- [ ] **Step 1: Update NetworkModule to include DynamicBaseUrlInterceptor**

```kotlin
package com.spendsense.di

import com.spendsense.data.remote.ChatCompletionApi
import com.spendsense.data.remote.DynamicBaseUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/") // Default base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatCompletionApi(retrofit: Retrofit): ChatCompletionApi {
        return retrofit.create(ChatCompletionApi::class.java)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/spendsense/di/NetworkModule.kt
git commit -m "feat: inject DynamicBaseUrlInterceptor and provide ChatCompletionApi in NetworkModule"
```

### Task 4: Update RegexGeneratorViewModel

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorViewModel.kt`

- [ ] **Step 1: Update ViewModel to use ChatCompletionApi and set dynamic base URL**

```kotlin
// ... existing imports
import com.spendsense.data.remote.ChatCompletionApi
import com.spendsense.data.remote.DynamicBaseUrlInterceptor
// ... 

@HiltViewModel
class RegexGeneratorViewModel @Inject constructor(
    private val chatCompletionApi: ChatCompletionApi,
    private val dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor,
    private val regexPatternRepository: RegexPatternRepository,
    private val aiProviderDao: AiProviderDao
) : ViewModel() {
// ...
    fun generateRegex(apiKey: String) {
        // ...
        
        _state.value = _state.value.copy(
            isGenerating = true,
            errorMessage = null,
            generatedPattern = null
        )

        // Set the base URL for OpenRouter (default for now)
        dynamicBaseUrlInterceptor.setBaseUrl(
            url = "https://openrouter.ai/api/v1/",
            key = apiKey,
            isOpenRouter = true
        )

        viewModelScope.launch {
            try {
                val prompt = buildPrompt(notificationText)
                val request = ChatCompletionRequest(
                    model = provider.defaultModel,
                    messages = listOf(
                        Message(role = "user", content = prompt)
                    )
                )

                val response = chatCompletionApi.generateCompletion(
                    request = request
                )
                // ...
            }
            // ...
        }
    }
// ...
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorViewModel.kt
git commit -m "feat: update RegexGeneratorViewModel to use ChatCompletionApi and DynamicBaseUrlInterceptor"
```

### Task 5: Verify Compilation

- [ ] **Step 1: Run build**

Run: `./gradlew assembleDebug`

- [ ] **Step 2: Fix any remaining compilation errors (e.g., imports in other files if any)**

- [ ] **Step 3: Final Commit**

```bash
git commit --amend --no-edit # If minor fixes were needed
```
