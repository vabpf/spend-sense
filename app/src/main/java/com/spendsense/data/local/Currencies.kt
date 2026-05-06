package com.spendsense.data.local

data class Currency(
    val code: String,
    val symbol: String,
    val name: String
)

object Currencies {
    val SUPPORTED = listOf(
        Currency("VND", "₫", "Vietnamese Dong"),
        Currency("USD", "$", "US Dollar"),
        Currency("EUR", "€", "Euro"),
        Currency("GBP", "£", "British Pound"),
        Currency("JPY", "¥", "Japanese Yen"),
    )

    val default = SUPPORTED.first()

    fun find(code: String): Currency = SUPPORTED.find { it.code == code } ?: default
}
