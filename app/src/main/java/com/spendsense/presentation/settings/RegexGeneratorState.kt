package com.spendsense.presentation.settings

import com.spendsense.data.local.entity.ProviderModelEntity

data class RegexTargetApp(
    val packageName: String,
    val appName: String
)

data class RegexGeneratorState(
    val notificationTitle: String = "",
    val notificationText: String = "",
    val manualPattern: String = "",
    val isGenerating: Boolean = false,
    val isTransaction: Boolean = true,

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

    val enabledModels: List<ProviderModelEntity> = emptyList(),
    val selectedModel: ProviderModelEntity? = null
)
