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
