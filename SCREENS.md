# SpendSense Screens Documentation

This document describes the various screens and UI components available in the SpendSense application.

## 1. Home Screen
The main entry point of the application, providing a list of all recorded transactions.

- **Transaction List:** A scrollable list of transactions showing merchant name, category, date, and amount.
- **Empty State:** Displays a helpful message and icon when no transactions have been recorded yet.
- **Navigation:**
    - **Settings:** Quick access to the settings screen via the top bar.
    - **Regex Generator:** Quick access to the AI-powered regex generator via the top bar.
- **Manual Entry:** A Floating Action Button (FAB) to manually add a transaction (Implementation pending).

## 2. Settings Screen
Central hub for app configuration and permission management.

- **Permissions Section:**
    - **Notification Access:** Link to system settings to enable notification interception.
    - **Display Over Other Apps:** Link to system settings to enable the transaction overlay.
- **Configuration Section:**
    - **Regex Generator:** Navigation to the AI rule creation tool.
    - **Whitelisted Apps:** Placeholder for managing which apps are monitored.
    - **Categories:** Placeholder for managing expense categories.
- **About Section:** Displays the current app version.

## 3. AI Regex Generator Screen
A specialized tool for creating and testing transaction extraction rules.

- **Input Area:** Multi-line text field to paste sample banking notifications.
- **AI Generation:**
    - Integration with OpenRouter (LLM) to generate regex patterns.
    - Requires an API key (provided via a secure dialog).
- **Result Preview:**
    - Displays the generated regex pattern (copyable).
    - Shows real-time extraction results (Amount and Merchant) against the provided input.
- **Persistence:**
    - Assign the pattern to a specific app package name.
    - Toggle rule status (Active/Inactive).
    - Save to the local database.

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
