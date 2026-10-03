# SpendSense Quick Start Guide

This guide helps you set up, build, install, and configure SpendSense on your development environment and physical Android device.

---

## Prerequisites
- **Android SDK:** API 26+ (Targeting API 35 / Android 15)
- **JDK:** OpenJDK 17
- **GitHub CLI (`gh`):** Authenticated with repository access for remote builds
- **Android Platform Tools (`adb`):** For installing APKs and firing test events
- **AI Provider Key (Optional):** OpenRouter, OpenAI, Anthropic Claude, Google Gemini, or a local Ollama instance for automated Regex generation

---

## Remote Build & Installation

> [!IMPORTANT]
> **DO NOT RUN GRADLE TASKS LOCALLY (`./gradlew`)**. All building, testing, and assembling are performed remotely via GitHub Actions to preserve local CPU, RAM, and battery.

### Step 1: Trigger Remote Build
To compile the release APK on GitHub Actions:
```bash
gh workflow run build.yml -f target=build -f publish_release=true --ref main
```

Monitor the progress of the workflow:
```bash
gh run list --limit 1
gh run watch <run_id> --exit-status
```

### Step 2: Download the Built APK
Once the workflow finishes with `success`, download the artifact directly to your local machine:
```bash
# Example for Windows/WSL:
rm -f /mnt/c/Users/vuong/Downloads/SpendSense.apk && gh run download <run_id> -n SpendSense-Release-APK -D /mnt/c/Users/vuong/Downloads
```

### Step 3: Install to Physical Device via ADB
Connect your Android device via USB (or wireless ADB) with **USB Debugging** enabled:
```bash
# Path to your ADB platform-tools
/mnt/d/Apps/Android/Sdk/platform-tools/adb.exe install -r /mnt/c/Users/vuong/Downloads/SpendSense.apk
```

---

## First Launch Configuration

### 1. Grant Mandatory Android Permissions
SpendSense requires two special permissions to capture and categorize transactions:

1. **Notification Access (NotificationListenerService):**
   - Open **Settings > Notification Access** (or **Special App Access > Notification Access**).
   - Locate **SpendSense** and toggle **Allow notification access** to **ON**.
   - *Android 15+ Note:* If notifications appear redacted, enable sensitive notifications:
     ```bash
     adb shell appops set com.spendsense RECEIVE_SENSITIVE_NOTIFICATIONS allow
     ```
2. **Display Over Other Apps (SYSTEM_ALERT_WINDOW):**
   - Open **Settings > Apps > Special App Access > Display over other apps**.
   - Select **SpendSense** and enable **Allow display over other apps**.
   - This powers the real-time floating categorization overlay.

### 2. Whitelist Monitored Apps
SpendSense will only process notifications from apps explicitly enabled in its watchlist.
1. Open SpendSense and tap **Whitelisted Apps** (from the Home top bar or Settings).
2. Browse or search through your installed apps (e.g., your banking, mobile wallet, or payment apps).
3. Toggle them **ON**.

### 3. Configure Your First Rule (Regex Pattern)
1. Go to **Settings > Regex Generator**.
2. Paste a real or sample transaction SMS/notification (e.g., `Spent $14.50 at Trader Joe's with card ending 4321`).
3. Select an AI Provider (or enter a manual pattern).
4. Tap **Generate Pattern** — the AI extracts named capture groups `(?<amount>...)` and `(?<merchant>...)`.
5. Select the target bank package, choose default currency, and tap **Save Pattern**.

---

## Testing the Transaction Flow Without a Real Bank

SpendSense includes full support for testing via ADB or standard messaging apps without spending money:
- See the complete step-by-step testing guide in [Testing Notifications](testing-notifications.md).

Quick ADB test:
```bash
# Whitelist Android Shell for testing
adb shell "cmd notification post -t 'Chase Alert' test-1 'Spent 24.99 at Target'"
```

---

## Troubleshooting

### Play Protect Warning during Sideload
If Google Play Protect warns about an untrusted developer key during sideloading:
1. Tap **More details** -> **Install anyway**.
2. To permanently trust the certificate, follow the developer appeals form outlined in `docs/implementation.md`.

### Overlay Does Not Appear
1. Verify **Display Over Other Apps** is enabled.
2. Confirm the sending app's package name is toggled **ON** in **Whitelisted Apps**.
3. Verify that the notification title and body match an active **Notification Pattern**. Unmatched notifications are safely stored in the **Notification Inbox** on the Home screen.

---

## Next Steps
- [System Architecture](architecture.md) — Understand Clean Architecture, Room entities, and layers.
- [Screens Documentation](screens.md) — Explore all features of each screen.
- [Liquid Glass Guide](liquid-glass.md) — Learn about our 120 FPS Sibling Glass shader architecture.
