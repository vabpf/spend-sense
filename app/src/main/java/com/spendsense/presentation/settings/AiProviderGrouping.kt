package com.spendsense.presentation.settings

import com.spendsense.data.local.entity.ProviderAccountEntity

data class ProviderAccountDisplay(
    val account: ProviderAccountEntity,
    val isConfigured: Boolean,
    val enabledModelCount: Int,
    val totalModelCount: Int,
    val lastRefreshedAt: Long
)

fun buildProviderGroupKey(baseUrl: String): String = baseUrl
