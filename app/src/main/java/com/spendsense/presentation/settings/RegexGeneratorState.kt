package com.spendsense.presentation.settings

import com.spendsense.data.local.entity.AiProviderEntity

data class RegexTargetApp(
    val packageName: String,
    val appName: String
)

data class RegexGeneratorState(
    val notificationText: String = "",
    val manualPattern: String = "",
    val isGenerating: Boolean = false,

    val generatedPattern: String? = null,
    val extractedAmount: String? = null,
    val extractedMerchant: String? = null,
    val availableApps: List<RegexTargetApp> = emptyList(),
    val selectedAppPackage: String = "",
    val currencyCode: String = "USD",
    val isActive: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    val providers: List<AiProviderEntity> = emptyList(),
    val providerKeyStatuses: Map<Long, Boolean> = emptyMap(),
    val selectedProvider: AiProviderEntity? = null
)
