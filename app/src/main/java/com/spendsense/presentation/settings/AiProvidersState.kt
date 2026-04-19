package com.spendsense.presentation.settings

import com.spendsense.data.local.entity.AiProviderEntity

data class AiProvidersState(
    val providers: List<AiProviderEntity> = emptyList(),
    val isAddingProvider: Boolean = false,
    val name: String = "",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val defaultModel: String = "meta-llama/llama-3.2-3b-instruct:free",
    val jobType: String = "REGEX_GEN",
    val errorMessage: String? = null
)
