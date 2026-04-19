# SpendSense Screens Documentation

This document describes the various screens and UI components available in the SpendSense application.

## 1. Home Screen
The main entry point of the application, providing access to recorded transactions and unhandled notifications.

- **Transaction List:** A scrollable list of transactions showing merchant name, category, date, and amount.
    - **Swipe-to-Delete:** Quickly remove invalid or unwanted transactions.
    - **Click-to-Edit:** Tap any transaction to open an edit dialog for modifying the amount, merchant (message), and category.
- **Notification Inbox:** Access to a list of notifications from whitelisted apps that failed regex matching or were dismissed from the overlay. This ensures no data is lost.
- **Empty State:** Displays a helpful message and icon when no transactions have been recorded yet.
- **Navigation:**
    - **Settings:** Quick access to the settings screen via the top bar.
    - **Regex Generator:** Quick access to the AI-powered regex generator via the top bar.
- **Manual Entry:** A Floating Action Button (FAB) to manually add a transaction.

## 2. Settings Screen
Central hub for app configuration and permission management.

- **Permissions Section:**
    - **Notification Access:** Link to system settings to enable notification interception.
    - **Display Over Other Apps:** Link to system settings to enable the transaction overlay.
- **Configuration Section:**
    - **Regex Generator:** Navigation to the AI rule creation tool.
    - **AI Providers:** Configure and manage AI API providers (OpenRouter, OpenAI, etc.) and assign them to specific tasks.
    - **Whitelisted Apps:** Manage which banking/payment apps are monitored.
    - **Categories:** Manage expense categories and their icons/colors.
- **About Section:** Displays the current app version.

## 3. AI Regex Generator Screen
A specialized tool for creating and testing transaction extraction rules.

- **Input Area:** Multi-line text field to paste sample banking notifications.
- **Rule Definition:**
    - **AI Generation:** Integration with selected AI provider to generate regex patterns based on sample text.
    - **Manual Entry:** Direct text field for manually typing or pasting regex patterns.
- **Result Preview:**
    - Displays the generated or manual regex pattern (copyable).
    - Shows real-time extraction results (Amount and Merchant) against the provided input.
- **Persistence:**
    - Assign the pattern to a specific app package name.
    - Toggle rule status (Active/Inactive).
    - Save to the local database.

## 4. AI Providers Screen
Manage and configure external AI models for app tasks.

- **Provider Management:** Add, edit, or remove AI providers (OpenRouter, local models, etc.).
- **Configuration:** Set Base URLs, API Keys (stored securely), and default models for each provider.
- **Task Assignment:** Choose which provider to use for specific jobs like "Regex Rule Generation".


## 4. Action Overlay
A system-level overlay that appears immediately when a transaction is detected.

- **Contextual Info:** Shows the source app name and a close button.
- **Editable Fields:**
    - **Amount:** Pre-filled from notification, editable with a numeric keypad.
    - **Merchant:** Pre-filled from notification, editable text field.
- **Category Selection:** A grid of icons for quick one-tap categorization.
- **Actions:**
    - **Save:** Persists the transaction to the database and closes the overlay.
    - **Reject:** Closes the overlay without saving.
- **Visual Feedback:** Shows loading indicators during save and success animations on completion.
