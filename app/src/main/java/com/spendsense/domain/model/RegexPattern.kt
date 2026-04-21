package com.spendsense.domain.model

data class RegexPattern(
    val id: Long = 0,
    val packageName: String,
    val pattern: String,
    val currencyCode: String = "USD",
    val isActive: Boolean = true
) {
    companion object {
        const val TARGET_ALL_WHITELISTED = "__ALL_WHITELISTED__"
    }
}
