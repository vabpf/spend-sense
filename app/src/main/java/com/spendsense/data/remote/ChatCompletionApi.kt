package com.spendsense.data.remote

import com.spendsense.data.remote.model.OpenRouterRequest
import com.spendsense.data.remote.model.OpenRouterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatCompletionApi {
    @POST("chat/completions")
    suspend fun generateCompletion(
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
