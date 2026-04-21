package com.spendsense.data.local

data class Currency(
    val code: String,
    val symbol: String,
    val name: String
)

object Currencies {
    val SUPPORTED = listOf(
        Currency("USD", "$", "US Dollar"),
        Currency("EUR", "€", "Euro"),
        Currency("GBP", "£", "British Pound"),
        Currency("JPY", "¥", "Japanese Yen"),
        Currency("INR", "₹", "Indian Rupee"),
        Currency("BRL", "R$", "Brazilian Real"),
        Currency("CAD", "C$", "Canadian Dollar"),
        Currency("AUD", "A$", "Australian Dollar"),
        Currency("CHF", "CHF", "Swiss Franc"),
        Currency("CNY", "¥", "Chinese Yuan"),
        Currency("KRW", "₩", "South Korean Won"),
        Currency("MXN", "$", "Mexican Peso"),
        Currency("SGD", "S$", "Singapore Dollar"),
        Currency("HKD", "HK$", "Hong Kong Dollar"),
        Currency("RUB", "₽", "Russian Ruble"),
        Currency("TRY", "₺", "Turkish Lira"),
        Currency("ZAR", "R", "South African Rand"),
        Currency("SEK", "kr", "Swedish Krona"),
        Currency("NOK", "kr", "Norwegian Krone"),
        Currency("DKK", "kr", "Danish Krone"),
        Currency("PLN", "zł", "Polish Zloty"),
        Currency("THB", "฿", "Thai Baht"),
        Currency("IDR", "Rp", "Indonesian Rupiah"),
        Currency("MYR", "RM", "Malaysian Ringgit"),
        Currency("PHP", "₱", "Philippine Peso"),
        Currency("VND", "₫", "Vietnamese Dong"),
        Currency("AED", "د.إ", "UAE Dirham"),
        Currency("SAR", "﷼", "Saudi Riyal"),
        Currency("NZD", "NZ$", "New Zealand Dollar"),
        Currency("CZK", "Kč", "Czech Koruna"),
        Currency("HUF", "Ft", "Hungarian Forint"),
        Currency("ILS", "₪", "Israeli Shekel"),
        Currency("PKR", "₨", "Pakistani Rupee"),
        Currency("EGP", "E£", "Egyptian Pound"),
        Currency("NGN", "₦", "Nigerian Naira"),
    )

    val default = SUPPORTED.first()

    fun find(code: String): Currency = SUPPORTED.find { it.code == code } ?: default
}
