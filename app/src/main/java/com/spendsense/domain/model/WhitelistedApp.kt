package com.spendsense.domain.model

data class WhitelistedApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val addedAt: Long
)
