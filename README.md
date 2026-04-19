# SpendSense - Smart Financial Tracking

SpendSense is a modern Android application that automatically captures transaction data from banking notifications and helps you categorize expenses in real-time using an interactive system overlay.

## Key Features
- **Auto-Capture:** Automatically extracts data from banking app notifications.
- **AI-Powered:** Uses LLMs to generate robust Regex patterns for any bank notification format.
- **Real-Time Categorization:** Categorize expenses instantly via a system overlay without leaving your current app.
- **Clean Architecture:** Built with Jetpack Compose, Hilt, Room, and Coroutines.
- **Privacy-First:** All sensitive data remains local, with optional cloud synchronization.

## Documentation Index

### 🚀 Getting Started
- **[Quick Start Guide](QUICK_START.md):** Step-by-step instructions for developers and users to get the app running.
- **[Installation](QUICK_START.md#setup-steps):** Build and install the app on your device.

### 📱 Product & UI
- **[Screens Documentation](SCREENS.md):** Detailed guide to the app's user interface and screen features.
- **[Usage Flow](README.md#core-usage-flow):** How the app works in day-to-day use.

### 🛠 Technical Reference
- **[Architecture](ARCHITECTURE.md):** Deep dive into the system design, data flow, and layers.
- **[Implementation Details](IMPLEMENTATION.md):** Status of current features, testing checklists, and known limitations.

## Core Usage Flow

1.  **Receive Banking Notification:** The app intercepts notifications from your banking apps.
2.  **Data Extraction:** AI-generated rules extract the amount and merchant details.
3.  **Action Overlay:** A floating window appears for quick category selection.
4.  **Save & Sync:** Transactions are saved to the local database and optionally synced to the cloud.
5.  **Dashboard:** Review your spending on the main home screen.

## Technical Stack
- **Language:** 100% Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Dependency Injection:** Hilt
- **Local Persistence:** Room Database
- **Networking:** Retrofit with Coroutines/Flow
- **AI Integration:** OpenRouter API

---
Built with ❤️ for better financial clarity.
