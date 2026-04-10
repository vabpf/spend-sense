# SpendSense Data Flow Documentation

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     SpendSense Architecture                  │
├─────────────────────────────────────────────────────────────┤
│  Presentation Layer (UI)                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │  HomeScreen │  │ SettingsScr  │  │ RegexGenerator  │   │
│  └──────┬──────┘  └──────┬───────┘  └────────┬────────┘   │
│         │                 │                    │             │
│  ┌──────▼──────┐  ┌──────▼───────┐  ┌────────▼────────┐   │
│  │HomeViewModel│  │              │  │RegexGeneratorVM │   │
│  └──────┬──────┘  └──────────────┘  └────────┬────────┘   │
│         │                                     │             │
│  ┌──────▼─────────────────────────────────────▼────────┐   │
│  │            ActionOverlayService                      │   │
│  │  ┌──────────────────────────────────────────────┐   │   │
│  │  │      ActionOverlayViewModel                   │   │   │
│  │  └──────────────┬───────────────────────────────┘   │   │
│  └─────────────────┼───────────────────────────────────┘   │
├────────────────────┼───────────────────────────────────────┤
│  Domain Layer      │                                       │
│  ┌─────────────────▼────────────────────────────────┐     │
│  │  Repository Interfaces                            │     │
│  └────────────────┬──────────────────────────────────┘     │
├───────────────────┼────────────────────────────────────────┤
│  Data Layer       │                                        │
│  ┌────────────────▼───────────────────────────────────┐   │
│  │  Repository Implementations                         │   │
│  └────┬──────────────────────────────────┬────────────┘   │
│  ┌────▼────────────┐              ┌──────▼────────────┐   │
│  │ Room Database   │              │ Retrofit API      │   │
│  └─────────────────┘              └───────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Services                                                   │
│  ┌────────────────────────────────────────────────────┐   │
│  │  TransactionNotificationListener                    │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Transaction Capture Flow

When a banking notification arrives, the following sequence occurs:

1. **Banking App** sends notification
2. **TransactionNotificationListener** intercepts it
3. **Regex patterns** extract amount & merchant  
4. **Action Overlay** displays for user confirmation
5. **User selects** category and saves
6. **Transaction** persists to Room database
7. **Home Screen** displays the new transaction

## Key Data Flows

- **Notification → Overlay:** Broadcast Intent
- **Overlay → Database:** Repository pattern
- **Database → UI:** Kotlin Flow (reactive)
- **User → AI:** OpenRouter API (Retrofit)

See detailed diagrams in the full documentation.
