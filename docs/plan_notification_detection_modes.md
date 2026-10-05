# SpendSense — Notification Detection Modes & Engine Optimization Plan

## 1. Executive Summary & Goals

This document details the architectural design, implementation progress, and roadmap for the **3-Mode Notification Detection Switcher** and **Regex Engine Optimizations** in SpendSense.

### The 3 Detection Modes:
1. **Regex & AI (Smart Hybrid):**
   - Incoming notification is first matched locally against cached regex rules in `notification_patterns`.
   - If unmatched (or if it belongs to a new banking app with no rules yet), the notification payload routes directly to the active AI provider.
   - **Transaction Detected:** Automatically records the transaction, notifies the user, and auto-generates + saves the learned regular expression with named capture groups `(?<amount>...)` and `(?<merchant>...)` to SQLite for future 0ms matching.
   - **Non-Transaction:** Silently discarded (or pruned from inbox).
2. **AI Only (Direct AI Categorization):**
   - Bypasses local regex pattern matching entirely.
   - Routes every incoming notification to AI, passing the user's active categories retrieved from `CategoryDao`.
   - AI extracts the amount, currency, merchant, payment source, and maps the transaction directly to the user's category list.
   - Non-transactions are silently discarded.
3. **Regex Only (Baseline Local Matching):**
   - Standard local regex pattern matching.
   - Unmatched notifications are saved directly to the Pending Inbox for manual review.

### Zero-Data-Loss Safety Net
In all AI-enabled modes, if the AI network request fails (timeout, quota limit, offline, or unconfigured API key), the processor **safely falls back to saving the raw notification to the Pending Inbox**. No transaction alerts or receipts are ever lost.

---

## 2. Background Engine Optimizations

To keep background services running at 120 FPS without memory bloat or GC churn:
- **Compiled Regex LRU Cache:** Added an in-memory `LruCache<String, Regex>(40)` to `NotificationProcessor`. Pre-compiled regex patterns avoid repeated compilations on high-frequency notification streams.
- **Candidate String Pruning:** Pruned notification text candidate combinations from 6 down to 2–3 targeted candidates (`cleanText`, `"$cleanTitle\n$cleanText"`, and `cleanTitle`).
- **Cold App Routing Fix:** Fixed an early-return bug where apps without existing patterns would abort before reaching AI evaluation in hybrid mode.
- **In-Memory Category Resolution:** Fast case-insensitive matching against the in-memory active category list before querying database mappings.

---

## 3. Architecture & Dependency Injection

To avoid JVM platform signature collisions and Dagger compiler issues:
- **`DirectAiNotificationParser` (Interface):**
  Defines `suspend fun parseNotification(title: String?, text: String, mode: NotificationRoutingMode): DirectAiParseResult?`.
- **`DirectAiNotificationParserImpl`:**
  Concrete implementation with a single `@Inject constructor` containing the 6 required dependencies (`ChatCompletionApi`, `DynamicBaseUrlInterceptor`, `ProviderAccountDao`, `ProviderModelDao`, `SecurePreferences`, `CategoryDao`).
- **`RepositoryModule`:**
  Binds `DirectAiNotificationParserImpl` to `DirectAiNotificationParser` using `@Binds @Singleton`.
- **`NotificationProcessor`:**
  Single `@Inject constructor` injecting `DirectAiNotificationParser` interface and `SecurePreferences`. No overloaded constructors.
- **Unit Testing:**
  Tests implement `DirectAiNotificationParser` directly as an anonymous object, requiring zero mocking frameworks or fake DAOs.

---

## 4. Implementation Status Tracker

| Component | Target File | Status | Notes |
| :--- | :--- | :--- | :--- |
| **Routing Enum** | `domain/model/NotificationRoutingMode.kt` | Completed | 3 detection modes with UI titles & descriptions |
| **Secure Preferences** | `data/local/SecurePreferences.kt` | Completed | Mode & active model persistence with lazy prefs |
| **Model DAO Query** | `data/local/dao/ProviderModelDao.kt` | Completed | Added `getById(id: Long)` query |
| **AI Parser Interface** | `data/service/DirectAiNotificationParser.kt` | Completed | Clean interface + implementation with 15s timeout |
| **DI Binding** | `di/RepositoryModule.kt` | Completed | `@Binds` for `DirectAiNotificationParser` |
| **Engine Processor** | `data/service/NotificationProcessor.kt` | Completed | LRU cache, 3-mode branching, single constructor |
| **Settings ViewModel** | `presentation/settings/SettingsViewModel.kt` | Completed | Mode state & active model selection |
| **Settings UI** | `presentation/settings/SettingsScreen.kt` | Completed | Selection dialogs, auto-refresh on resume |
| **Unit Test Suite** | `test/.../NotificationProcessorTest.kt` | Completed | 7 comprehensive tests covering all modes & fallbacks |
| **Version Bump** | `app/build.gradle.kts` | Completed | Version 1.3.0 (versionCode 10) |

---

## 5. Chronology of Remote Build Issues & Resolutions

1. **Failure 1 — Multiple Injected Constructors:**
   - *Error:* `Type DirectAiNotificationParser may only contain one injected constructor.`
   - *Cause:* Adding default arguments (`= null`) to an `@Inject constructor` in Kotlin causes the compiler to generate synthetic `@Inject` overloads for Java/Dagger.
2. **Failure 2 — JVM Platform Declaration Clash:**
   - *Error:* `Platform declaration clash: The following declarations have the same JVM signature (<init>(...))`
   - *Cause:* Having a constructor with non-null types `(Foo, Bar)` alongside a secondary constructor with nullable types `(Foo?, Bar?)` produces identical Java bytecode signatures because Kotlin nullability is erased at the JVM level.
3. **Final Architecture Resolution:**
   - Introduced the `DirectAiNotificationParser` interface.
   - Bound implementation via Hilt `@Binds` in `RepositoryModule`.
   - Kept strictly **one** constructor on `NotificationProcessor` and `DirectAiNotificationParserImpl`.
   - Updated test cases to implement the interface directly.

---

## 6. Remote Build & Deployment Verification

All compilation and testing must be performed remotely via GitHub Actions as specified in `AGENTS.md`.

```bash
# 1. Commit and push all changes
git add app/src/main/java/com/spendsense/data/service/DirectAiNotificationParser.kt \
        app/src/main/java/com/spendsense/data/service/NotificationProcessor.kt \
        app/src/main/java/com/spendsense/di/RepositoryModule.kt \
        app/src/test/java/com/spendsense/data/service/NotificationProcessorTest.kt \
        docs/plan_notification_detection_modes.md

git commit -m "fix(di): introduce DirectAiNotificationParser interface and resolve JVM signature clash"
git push origin main

# 2. Trigger Remote Build
gh workflow run build.yml -f target=all

# 3. Watch status
gh run list --limit 2
gh run watch $(gh run list --limit 1 --json databaseId -q '.[0].databaseId') --exit-status
```
