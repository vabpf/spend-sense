package com.spendsense.domain.model

data class CreditCardConfig(
    val cardName: String,
    val statementClosingDay: Int, // 1..31
    val paymentDueDay: Int? = null, // 1..31 (optional)
    val creditLimit: Double? = null // optional
)
