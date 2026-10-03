# SpendSense Screens Documentation

This document describes all user-facing screens and system-level overlays in SpendSense, including interaction patterns, state management, and design components.

---

## 1. Home Screen (`HomeScreen.kt`)
The primary dashboard of the application, designed for rapid inspection and editing of transactions.

### Key Features
- **Summary Header (`HomeSummaryCard`):**
  - Displays today's total spending compared against yesterday's spending.
  - Shows total transaction count and pending notifications count.
  - Automatically calculates converted totals in the user's default currency.
- **Search & Filter:**
  - Real-time text search across merchant names, notes, and payment sources.
  - Filter bottom sheet to filter by date range, category, and minimum/maximum amounts.
- **Notification Inbox:**
  - An expandable inbox card displaying raw notifications that failed regex matching or were dismissed from the overlay.
  - Direct actions: **Process with AI** (opens Regex Generator), **Add New Rule**, or **Dismiss/Delete**.
- **Transaction List:**
  - Grouped chronologically with sticky day headers (`Today`, `Yesterday`, formatted date).
  - **Swipe-to-Reveal Actions:** Swipe right on any transaction row to reveal an animated Delete button with spring physics.
  - **Single Click Edit:** Tap any transaction to open the comprehensive `EditTransactionDialog`.
- **Multi-Selection & Batch Operations:**
  - Long-press any item to activate selection mode.
  - Floating glass toolbar provides:
    - Selected item counter and total monetary sum.
    - **Select All / Deselect All** button.
    - **Batch Edit:** Apply category, payment source, currency, or notes to all selected transactions simultaneously.
    - **Batch Delete:** Bulk remove selected entries with confirmation.
- **Manual Add Button (FAB):**
  - Cyber Blue floating action button launching the `AddTransactionDialog`.

---

## 2. Analytics & Charts Screen (`ChartsScreen.kt`)
Deep financial visualization and spending insights.

### Key Features
- **Date Range Filter:** Switch between **7 Days**, **30 Days**, **This Month**, **Year to Date**, or **Custom Date Range**.
- **Key Metrics Row:** Total expenditure, average daily spend, and highest expense recorded.
- **Category Spending Breakdown:**
  - Interactive doughnut/pie chart illustrating expense distribution.
  - Ranked category list showing percentage and amount per category.
- **Daily Spending Bar Chart:** Visualizes daily expenses over the selected timeframe.
- **Top Merchants:** Highlights frequent stores and spending destinations.

---

## 3. Categories Management Screen (`CategoriesScreen.kt`)
Allows users to customize expense tracking categories.

### Key Features
- **Category Grid / List:** Displays category cards with custom icons and color swatches.
- **Add / Edit Category Dialog:**
  - Custom name input.
  - **Color Palette Picker:** Selection of neon, pastel, and high-contrast color codes.
  - **Icon Picker:** Curated grid of financial, lifestyle, utility, and shopping icons.
- **Protection:** Prevents deletion of system default categories while allowing complete editing of custom categories.

---

## 4. Notification Patterns Screen (`NotificationPatternsScreen.kt`)
Central management for all regex matching rules.

### Key Features
- **Pattern Cards:**
  - Shows target bank package name, pattern string, and title filters.
  - Shows usage statistics (`lastUsed` date, `successCount`).
  - Active / Inactive switch for instant rule toggling.
- **Quick Test Modal:** Test any existing regex pattern against arbitrary sample text without leaving the screen.
- **Add Rule Navigation:** Floating action button that routes directly to the AI Regex Generator.

---

## 5. AI Regex Generator Screen (`RegexGeneratorScreen.kt`)
Specialized AI-powered tool for generating regular expressions from raw notification text.

### Key Features
- **Input Sandbox:** Multi-line text field to paste SMS or notification texts from any banking institution.
- **AI Generation Engine:** Sends sample text to the configured AI provider with a specialized prompt requesting named regex groups `(?<amount>...)` and `(?<merchant>...)`.
- **Manual Regex Mode:** Direct text editor for fine-tuning or writing custom patterns.
- **Real-Time Verification:** As patterns are typed or generated, the screen extracts and previews the detected Amount, Merchant, and Currency live against the sample text.
- **Persistence:** Select target package from installed apps, choose rule currency, and save directly to Room DB.

---

## 6. AI Providers Screen (`AiProvidersScreen.kt`)
Configures external Large Language Model APIs for automated pattern generation.

### Key Features
- **Supported Providers:**
  - **OpenRouter** (Unified multi-model access)
  - **OpenAI** (GPT-4o, GPT-4o-mini)
  - **Anthropic Claude** (Claude 3.5 Sonnet, Haiku)
  - **Google Gemini** (Gemini 1.5 Pro, Flash)
  - **Ollama / Local LLM** (Self-hosted offline models)
  - **Custom Endpoints** (Any OpenAI-compatible server)
- **Security:** API keys are encrypted at rest using Android's Keystore-backed `EncryptedSharedPreferences`.
- **Connection Test:** Built-in "Test Connection" button to verify API key and endpoint validity before saving.

---

## 7. Whitelisted Apps Screen (`WhitelistedAppsScreen.kt`)
Controls which apps on the device are monitored by the notification listener service.

### Key Features
- **Device App Scanner:** Scans installed packages and displays app name, package identifier, and app icon.
- **Search & Filter:** Rapid search bar to locate specific banking or payment apps.
- **Monitoring Switch:** One-tap toggle to enable or disable notification processing for each app.

---

## 8. Settings Screen (`SettingsScreen.kt`)
Central configuration hub for security, preferences, appearance, and data management.

### Key Features
- **Permissions Status Cards:**
  - Status indicators for **Notification Access** and **Display Over Other Apps**.
  - One-tap deep links into Android System Settings when permissions are missing.
- **Currency Preferences:**
  - Default currency selector supporting 35 global currencies (USD, EUR, GBP, JPY, VND, etc.).
- **Theme & Wallpaper Engine:**
  - **Preset Themes:** Cyberpunk (Default), Deep Space, Neon Sunset, Matrix, Minimalist Dark.
  - **Custom Wallpaper:** System image picker to select any personal background photo, automatically integrated into the Liquid Glass refraction pipeline.
- **Data Management (Backup & Restore):**
  - **Export:** Export transactions to CSV or full database backup to JSON.
  - **Import:** Restore transactions and rules from backup files.
- **App Version & Diagnostics:** Displays build version, database migration status, and quick links to documentation.

---

## 9. Action Overlay Service (`ActionOverlayService.kt`)
System-level floating dialog (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`) that triggers when a bank transaction is intercepted.

### Key Features
- **Real-Time Glass Card:** Floats above third-party applications without switching tasks.
- **Pre-filled Data:**
  - Extracted amount with numeric pad editor.
  - Extracted merchant name with text editor.
  - Detected or default currency dropdown.
- **1-Tap Category Grid:** Quick-tap category buttons to classify the expense instantly.
- **Haptics & Animations:** Confirms saving with subtle haptic feedback and automatically dismisses.
- **Dismiss to Inbox:** Dismissing the overlay without saving keeps the raw notification safely in the Home Screen Inbox.
