package com.spendsense.domain.model

data class RegexPattern(
    val id: Long = 0,
    val packageName: String,
    val pattern: String,
    val isActive: Boolean = true
)
