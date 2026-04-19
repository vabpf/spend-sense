package com.spendsense.presentation.settings

import com.spendsense.data.local.entity.AiProviderEntity

data class RegexGeneratorState(
    val notificationText: String = "",
    val manualPattern: String = "",
    val isGenerating: Boolean = false,

    val generatedPattern: String? = null,
    val extractedAmount: String? = null,
    val extractedMerchant: String? = null,
    val packageName: String = "",
    val isActive: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    val providers: List<AiProviderEntity> = emptyList(),
    val selectedProvider: AiProviderEntity? = null
)
