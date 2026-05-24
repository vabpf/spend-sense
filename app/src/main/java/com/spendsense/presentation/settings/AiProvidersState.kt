package com.spendsense.presentation.settings

data class AiProvidersState(
    val accounts: List<ProviderAccountDisplay> = emptyList(),
    val isAddingProvider: Boolean = false,
    val name: String = "",
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val errorMessage: String? = null,
    val existingApiKeyPreview: String? = null
)
