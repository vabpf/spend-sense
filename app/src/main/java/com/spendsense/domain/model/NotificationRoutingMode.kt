package com.spendsense.domain.model

enum class NotificationRoutingMode(
    val title: String,
    val description: String
) {
    REGEX_AND_AI(
        title = "Regex & AI",
        description = "Unrecognized notifications auto-route to AI. Learns new regex if it's a transaction, else discards."
    ),
    AI_ONLY(
        title = "AI Only",
        description = "Bypasses regex. Routes all notifications to AI directly to extract details and assign categories."
    ),
    REGEX_ONLY(
        title = "Regex Only",
        description = "Standard local regex matching. Unmatched notifications are saved to Pending Inbox."
    );

    companion object {
        fun fromString(value: String?): NotificationRoutingMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: REGEX_ONLY
        }
    }
}
