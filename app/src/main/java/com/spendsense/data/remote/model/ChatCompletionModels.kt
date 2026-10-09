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
    val content: Any
)

data class ContentPartText(
    @SerializedName("type")
    val type: String = "text",
    @SerializedName("text")
    val text: String
)

data class ImageUrlData(
    @SerializedName("url")
    val url: String
)

data class ContentPartImageUrl(
    @SerializedName("type")
    val type: String = "image_url",
    @SerializedName("image_url")
    val imageUrl: ImageUrlData
)

data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("choices")
    val choices: List<Choice>
)

data class ResponseMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String?
)

data class Choice(
    @SerializedName("message")
    val message: ResponseMessage
)
