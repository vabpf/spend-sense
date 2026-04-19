package com.spendsense.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<Message>
)

data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("choices")
    val choices: List<Choice>
)

data class Choice(
    @SerializedName("message")
    val message: Message
)
