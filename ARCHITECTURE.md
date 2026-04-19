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

## Transaction Capture Sequence

1.  **Banking App** sends a notification.
2.  **TransactionNotificationListener** intercepts the notification based on whitelisted package names.
3.  **Regex Patterns** are applied to the notification text to extract `amount` and `merchant`.
4.  **Action Overlay** is triggered via a broadcast intent.
5.  **User Interacts** with the overlay to confirm or edit details and select a category.
6.  **Transaction** is persisted to the Room database.
7.  **Home Screen** updates automatically via reactive Flow.

## Database Schema

- **Transaction:** `id`, `amount`, `merchant`, `categoryId`, `timestamp`, `sourcePackageName`, `notes`, `isSynced`.
- **Category:** `id`, `name`, `iconName`, `colorHex`, `isDefault`.
- **RegexPattern:** `id`, `packageName`, `pattern`, `isActive`, `lastUsed`, `successCount`.
- **WhitelistedApp:** `packageName`, `appName`, `isEnabled`, `addedAt`.
