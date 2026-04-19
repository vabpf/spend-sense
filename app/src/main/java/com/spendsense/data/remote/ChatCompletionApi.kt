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
