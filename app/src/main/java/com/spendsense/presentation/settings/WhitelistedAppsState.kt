package com.spendsense.presentation.settings

import com.spendsense.domain.model.WhitelistedApp

data class WhitelistedAppsState(
    val apps: List<WhitelistedApp> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)
