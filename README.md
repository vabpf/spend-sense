# SpendSense - Smart Financial Tracking

SpendSense is a modern, cyber-premium Android personal finance application that automatically captures transaction data from banking notifications, categorizes expenses in real-time via an interactive system overlay, and provides rich analytics with a 120 FPS GPU-accelerated Liquid Glass interface.

---

## Key Features

- **Automated Capture:** Intercepts incoming SMS and push notifications from whitelisted banking and wallet apps in real-time.
- **AI-Powered Regex Generator:** Utilizes LLMs (OpenRouter, OpenAI, Claude, Gemini, Ollama, or custom endpoints) to generate resilient regular expression extraction rules.
- **Real-Time Floating Overlay:** Categorize expenses in 1 tap without leaving your current app via a non-intrusive system overlay (`TYPE_APPLICATION_OVERLAY`).
- **120 FPS Liquid Glass Aesthetic:** Custom AGSL runtime shaders (Android 13+) simulate optical refraction, chromatic dispersion, and depth lighting with a decoupled Sibling Background Architecture ensuring smooth 120 FPS scrolling.
- **Deep Financial Analytics:** Interactive charts for daily spending trends, category breakdown, and top merchant analysis.
- **Multi-Currency Support:** Native support for 35 world currencies with localized formatting and real-time default currency conversions.
- **Batch Transaction Management:** Multi-select mode with floating toolbar for bulk category assignment, editing, and deletion.
- **Privacy-First & Offline:** 100% of transaction data and API keys are stored locally on your device in an encrypted Room SQLite database.
- **Data Portability:** Full export to CSV and full database backup / restore via JSON.

---

## Documentation Index

All project documentation is consolidated in the [`docs/`](docs/) directory:

| Document | Description |
|---|---|
| 🚀 **[Quick Start Guide](docs/quick-start.md)** | Prerequisites, remote build workflow, ADB installation, permissions, and first-run setup |
| 🏗 **[Architecture Overview](docs/architecture.md)** | Clean Architecture layers, Inbox pattern sequence diagram, and Room v4 database schema |
| 📱 **[Screens & UI Guide](docs/screens.md)** | Detailed documentation of all 8 screens, batch operations, and the action overlay |
| 🧪 **[Testing Notifications](docs/testing-notifications.md)** | Step-by-step instructions to test notification extraction via ADB without a real bank |
| 💎 **[Liquid Glass & 120 FPS](docs/liquid-glass.md)** | AGSL shader guide, Sibling Background Architecture, and theme engine |
| 🎨 **[Design Tokens & Palette](docs/design.md)** | Color tokens, typography, surfaces, and cyber-premium design system |
| 📋 **[Implementation Status](docs/implementation.md)** | Completed capabilities, v1.0.11 release details, quality assurance, and future roadmap |

---

## Core Usage Flow

```
[Banking Notification Received]
             │
             ▼
[TransactionNotificationListener]
             │ (Matches Whitelisted App)
             ▼
[Regex Rule Extraction]
      ┌──────┴──────┐
  (Matched)     (Unmatched)
      │             │
      ▼             ▼
[Action Overlay]   [Notification Inbox on Home Screen]
  • 1-tap save       • Process with AI Regex Generator
  • Auto-categorize  • Manual review
```

---

## Technical Stack

- **Platform:** Android (Min SDK 26, Target SDK 35 / Android 15)
- **Language:** 100% Kotlin with Coroutines & StateFlow
- **UI Framework:** Jetpack Compose (Material 3)
- **Design System:** Custom Cyber-Premium Liquid Glass (AGSL Runtime Shaders + Haze fallback)
- **Dependency Injection:** Hilt
- **Local Persistence:** Room Database (SQLite, Version 4) with KSP
- **Security:** AndroidX `EncryptedSharedPreferences` backed by Android Keystore
- **Networking:** Retrofit + OkHttp with dynamic AI provider routing
- **AI Integrations:** OpenRouter, OpenAI, Anthropic Claude, Google Gemini, Ollama (Local)
- **CI/CD:** GitHub Actions remote build and release pipeline

---

## Build & Install (Remote Only)

To preserve local workstation performance, all building and compiling must be executed remotely on GitHub Actions:

```bash
# 1. Trigger remote release build & publish GitHub release
gh workflow run build.yml -f target=build -f publish_release=true --ref main

# 2. Download built APK
rm -f /mnt/c/Users/vuong/Downloads/SpendSense.apk && gh run download -n SpendSense-Release-APK -D /mnt/c/Users/vuong/Downloads

# 3. Install to device via ADB
/mnt/d/Apps/Android/Sdk/platform-tools/adb.exe install -r /mnt/c/Users/vuong/Downloads/SpendSense.apk
```

See [Quick Start Guide](docs/quick-start.md) for full instructions.

---

## AI Agent Guidelines

AI coding assistants (Antigravity, Claude Code, Cursor, OpenAI Codex) follow unified rules defined in [`AGENTS.md`](AGENTS.md) (and symlinked from `CLAUDE.md`).
