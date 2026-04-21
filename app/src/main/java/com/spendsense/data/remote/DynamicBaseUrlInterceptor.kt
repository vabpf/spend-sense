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
    @Volatile private var isOpenCode: Boolean = false

    fun setBaseUrl(
        url: String,
        key: String?,
        isOpenRouter: Boolean = false,
        isOpenCode: Boolean = false
    ) {
        this.host = url
        this.apiKey = key
        this.isOpenRouter = isOpenRouter
        this.isOpenCode = isOpenCode
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val newHost = host?.toHttpUrlOrNull()

        if (newHost != null) {
            val originalPath = request.url.encodedPath
            val endpointPath = originalPath
                .removePrefix("/api/v1/")
                .removePrefix("/api/v1")
                .trimStart('/')

            val basePath = newHost.encodedPath.trimEnd('/')
            val combinedPath = when {
                endpointPath.isBlank() -> if (basePath.isBlank()) "/" else basePath
                basePath.isBlank() -> "/$endpointPath"
                else -> "$basePath/$endpointPath"
            }

            val newUrl = newHost.newBuilder()
                .encodedPath(combinedPath)
                .build()
            
            val requestBuilder = request.newBuilder().url(newUrl)

            if (isOpenCode) {
                requestBuilder.header("Authorization", "Bearer ${apiKey?.takeIf { it.isNotBlank() } ?: "public"}")
                requestBuilder.header("x-opencode-client", "desktop")
                requestBuilder.header("Accept", "application/json")
            } else {
                apiKey?.takeIf { it.isNotBlank() }?.let {
                    requestBuilder.header("Authorization", "Bearer $it")
                }
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
