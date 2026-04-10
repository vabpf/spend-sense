package com.spendsense.domain.model

data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val sourcePackageName: String,
    val sourceAppName: String
)
