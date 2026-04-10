package com.spendsense.data.remote

import com.spendsense.data.remote.model.OpenRouterRequest
import com.spendsense.data.remote.model.OpenRouterResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String,
        @Header("X-Title") appTitle: String,
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
