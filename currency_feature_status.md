# Currency Feature Status

## Goal
Allow users to choose a currency for transactions. The app strips currency from regex matches and stores amounts as raw numbers, so currency must be stored separately.

---

## Completed

### 1. Model & Data Layer
- **Transaction.kt** — added `currencyCode: String = "USD"` field
- **TransactionEntity.kt** — added `currencyCode: String = "USD"` field, matching DB column
- **RegexPattern.kt** — added `currencyCode: String = "USD"` field
- **RegexPatternEntity.kt** — added `currencyCode: String = "USD"` field
- **TransactionRepositoryImpl.kt** — updated `toEntity()` and `toDomain()` mappers to include currencyCode
- **RegexPatternRepositoryImpl.kt** — updated mappers to include currencyCode

### 2. Database
- **Migration3to4.kt** — created migration to add `currencyCode TEXT NOT NULL DEFAULT 'USD'` to both `transactions` and `regex_patterns` tables
- **SpendSenseDatabase.kt** — bumped version from 3 → 4, references `MIGRATION_3_4`
- **DatabaseModule.kt** — added `addMigrations(MIGRATION_3_4)`, removed `fallbackToDestructiveMigration()`
- **app/build.gradle.kts** — added `room.schemaLocation` for KSP schema export

### 3. Regex Generator (Pattern Creation)
- **RegexGeneratorState.kt** — added `currencyCode: String = "USD"` field
- **RegexGeneratorViewModel.kt** — added `updateCurrency()`, saved currency to `SecurePreferences`, initialized currency from stored default, passes currencyCode when creating RegexPattern
- **RegexGeneratorScreen.kt** — added currency dropdown picker (35 currencies) in the Save Pattern section
- **Experimental API opt-in** — added `@file:OptIn(ExperimentalMaterial3Api::class)`

### 4. Transaction Overlay (Auto-detected Transactions)
- **TransactionNotificationListener.kt** — added `EXTRA_CURRENCY`, passes currency from pattern entity to overlay via broadcast
- **ActionOverlayService.kt** — updated receiver to extract and pass currency; updated `showOverlay()` signature; **FIXED** missing/duplicated `CurrencyDropdown` composable
- **OverlayState.kt** — added `currencyCode: String = "USD"` field
- **ActionOverlayViewModel.kt** — updated `initialize()`, added `updateCurrency()`, passes currencyCode when creating Transaction
- **Currencies.kt** — new file with 35 supported currencies (code, symbol, name)
- **Experimental API opt-in** — added `@file:OptIn(ExperimentalMaterial3Api::class)`

### 5. Manual Add Transaction Dialog
- **AddTransactionDialog.kt** — added currency dropdown picker, passes currencyCode to `onConfirm`; now supports pre-filling with default currency
- **HomeViewModel.kt** — updated `addTransaction()` to accept and store currencyCode; now provides `defaultCurrency` from `SecurePreferences`
- **HomeScreen.kt** — updated dialog call site; fixed `formatCurrency()` to use locale-aware currency formatting with currencyCode; passes `defaultCurrency` to dialog
- **Experimental API opt-in** — added `@file:OptIn(ExperimentalMaterial3Api::class)`

### 6. Default Currency Preference
- **SecurePreferences.kt** — added `setDefaultCurrency()` and `getDefaultCurrency()` using encrypted SharedPreferences
- **SettingsViewModel.kt** — new ViewModel to manage global default currency setting
- **SettingsScreen.kt** — added global currency selector in Preferences section

### 7. Theme Fix
- **Theme.kt** — added all missing color container tokens (`primaryContainer`, `secondaryContainer`, `errorContainer`, `surfaceVariant`, etc.) so text renders properly on dark backgrounds

### 8. Build & Verification
- Project builds successfully with `./gradlew assembleDebug`

---

## Still Needed

1. **Test on physical device/emulator** — verify:
   - Currency dropdown appears in overlay when a notification triggers it
   - Currency dropdown appears in regex generator screen
   - Currency dropdown appears in manual add transaction dialog (pre-filled with default)
   - Transactions display with correct currency symbol (e.g., "€12.50" for EUR)
   - Changing default currency in Settings updates the pre-filled value in manual add dialog
