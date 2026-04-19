# SpendSense Quick Start Guide

## Overview
This guide will help you get SpendSense up and running on your development environment and physical Android device.

## Prerequisites
- **Android Studio:** Arctic Fox or later
- **Android SDK:** API 26+ (Android 8.0+)
- **JDK:** 17
- **Gradle:** 8.2+
- **OpenRouter Account:** For AI-powered regex generation.

---

## Setup Steps

### 1. Clone and Open Project
```bash
git clone https://github.com/vabpf/spend-sense.git
cd spend-sense
```
Open the project in Android Studio and allow Gradle to sync.

### 2. Configure Firebase (Optional)
Cloud synchronization is optional for initial testing.
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package: `com.spendsense`.
3. Download `google-services.json` and place it in the `app/` directory.

### 3. Build and Install
Connect your physical device with USB Debugging enabled.
```bash
./gradlew installDebug
```

---

## First Launch Configuration

### 1. Grant Mandatory Permissions
SpendSense requires two special permissions to function:
1.  **Notification Access:** Go to **Settings > Notification Access** and enable SpendSense. This allows the app to intercept banking alerts.
2.  **Display Over Other Apps:** Go to **Settings > Special App Access > Display Over Other Apps** and allow SpendSense. This enables the categorization overlay.

### 2. Configure Your First Rule
1. Navigate to **Settings > Regex Generator**.
2. Paste a sample notification from your bank.
3. Provide your **OpenRouter API Key** when prompted.
4. Tap **Generate Rule (AI)** and review the result.
5. Enter your bank app's package name (e.g., `com.example.bankapp`).
6. Tap **Add to Watchlist**.

---

## Testing the Transaction Flow

To test the system without making a real purchase:
1. Use a notification simulator app or `adb` to send a test notification.
2. Ensure the text matches your saved regex pattern.
3. The **Action Overlay** should appear automatically.
4. Select a category and tap **Save**.
5. Verify the transaction appears on the **Home Screen**.

---

## Common Troubleshooting

### "Notifications are not being intercepted"
- Double-check that SpendSense is enabled in **Notification Access** settings.
- Ensure the bank app's package name in your rule matches exactly.
- Check the **Implementation Details** doc for known limitations.

### "Overlay does not appear"
- Verify the **Display Over Other Apps** permission is granted.
- Ensure the `ActionOverlayService` is running (check for a persistent notification).

---

## Resources
- **[Screens Documentation](SCREENS.md):** Learn about the app's UI.
- **[Architecture Overview](ARCHITECTURE.md):** Understand the system design.
- **[Implementation Status](IMPLEMENTATION.md):** Track current features and limitations.
