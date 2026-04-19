# SpendSense Architecture Documentation

## Overview

SpendSense is built using Clean Architecture and the MVVM (Model-View-ViewModel) design pattern. The application is designed to be offline-first, with Room as the single source of truth and potential for future cloud synchronization.

## System Architecture

```mermaid
graph TD
    subgraph Presentation
        UI[Screens & Components]
        VM[ViewModels]
        OS[ActionOverlayService]
        NLS[TransactionNotificationListener]
    end

    subgraph Domain
        Models[Domain Models]
        RepoInt[Repository Interfaces]
    end

    subgraph Data
        RepoImpl[Repository Implementations]
        Room[Room Database]
        API[OpenRouter API]
    end

    NLS --> VM
    UI --> VM
    VM --> RepoInt
    RepoImpl ..|> RepoInt
    RepoImpl --> Room
    RepoImpl --> API
```

## Data Layers

### 1. Presentation Layer
- **UI:** 100% Jetpack Compose using Material 3.
- **ViewModels:** Handle UI state and user interactions using Kotlin `StateFlow`.
- **Services:** 
    - `TransactionNotificationListener`: Intercepts banking notifications.
    - `ActionOverlayService`: Manages the system-level overlay for quick categorization.

### 2. Domain Layer
- **Models:** Pure Kotlin data classes representing business entities.
- **Repository Interfaces:** Define the contracts for data access, decoupled from the implementation details.

### 3. Data Layer
- **Repository Implementations:** Coordinate data between Room, API, and the UI.
- **Room Database:** Local persistent storage for transactions, categories, regex patterns, and whitelisted apps.
- **Retrofit API:** Handles communication with OpenRouter for AI regex generation.

## Transaction Capture Sequence (The "Inbox" Pattern)

1.  **Banking App** sends a notification.
2.  **TransactionNotificationListener** intercepts the notification based on whitelisted package names.
3.  **Raw Capture:** The notification text and metadata are immediately saved to the `raw_notifications` table. This ensures the data is captured even if processing fails.
4.  **Extraction:** Regex patterns are applied to the text to extract `amount` and `merchant`.
5.  **Action Overlay:** If a match is found, the overlay is triggered via a broadcast intent.
6.  **User Interacts:** The user confirms/edits details and selects a category.
7.  **Persistence:** The finalized transaction is saved to the `transactions` table, and the corresponding `raw_notification` is marked as processed or deleted.
8.  **Inbox:** If no regex match is found, or if the overlay is dismissed without saving, the entry remains in the "Notification Inbox" on the Home Screen for manual processing.

## Database Schema

- **Transaction:** `id`, `amount`, `merchant`, `categoryId`, `timestamp`, `sourcePackageName`, `notes`, `isSynced`.
- **Category:** `id`, `name`, `iconName`, `colorHex`, `isDefault`.
- **RegexPattern:** `id`, `packageName`, `pattern`, `isActive`, `lastUsed`, `successCount`.
- **WhitelistedApp:** `packageName`, `appName`, `isEnabled`, `addedAt`.
- **RawNotification:** `id`, `packageName`, `text`, `timestamp`, `isProcessed`.
- **AiProvider:** `id`, `name`, `baseUrl`, `apiKey`, `defaultModel`, `jobType` (e.g., REGEX_GEN).

