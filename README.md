# SpendSense - Financial Tracking App

A financial tracking application that automatically captures banking notifications, extracts transaction data using AI-generated Regex patterns, and enables real-time expense categorization through a system overlay.

## Architecture

### Tech Stack
- **Language:** Kotlin 100%
- **UI:** Jetpack Compose with Material 3 (No XML)
- **Architecture:** MVVM with Clean Architecture
- **DI:** Hilt (Dagger)
- **Async:** Coroutines & Flow
- **Local DB:** Room
- **Cloud DB:** Firebase Firestore
- **Networking:** Retrofit (OpenRouter API)

### Project Structure

```
com.spendsense/
├── data/
│   ├── local/
│   │   ├── entity/          # Room entities
│   │   ├── dao/             # Data Access Objects
│   │   └── SpendSenseDatabase.kt
│   ├── remote/
│   │   ├── model/           # API models
│   │   └── OpenRouterApi.kt # Retrofit API interface
│   ├── repository/          # Repository implementations
│   └── service/
│       └── TransactionNotificationListener.kt
├── domain/
│   ├── model/               # Domain models
│   └── repository/          # Repository interfaces
├── presentation/
│   ├── home/                # Home screen (transaction list)
│   ├── settings/            # Settings & Regex Generator
│   ├── overlay/             # Action Overlay Service
│   └── theme/               # Material 3 theming
└── di/                      # Hilt modules
```

## Core Components

### 1. Notification Listener Service

**Location:** `data/service/TransactionNotificationListener.kt`

Monitors notifications from whitelisted banking apps and extracts transaction data using regex patterns.

**Features:**
- Filters notifications by whitelisted package names
- Applies regex patterns with named groups (amount, merchant)
- Uses Dispatchers.IO for pattern matching
- Triggers Action Overlay on successful match
- Handles malformed patterns gracefully

### 2. Action Overlay Service

**Location:** `presentation/overlay/ActionOverlayService.kt`

Displays a floating Compose UI for transaction categorization.

**Features:**
- System overlay window (TYPE_APPLICATION_OVERLAY)
- Editable amount with decimal input
- Editable merchant name
- Category selection grid with icons
- Reject/Save actions
- Lifecycle-aware service with proper cleanup

**UI Components:**
- Amount display (editable, validates > 0)
- Merchant text field
- Category grid (icon + name)
- Action buttons (Reject/Save)

### 3. Regex Generator Screen

**Location:** `presentation/settings/RegexGeneratorScreen.kt`

AI-powered regex pattern generator using OpenRouter API.

**Features:**
- Paste notification text
- AI generates regex with named groups
- Test extracted data
- Save pattern with package name
- Active/Inactive toggle

**OpenRouter Integration:**
- Model: `meta-llama/llama-3.2-3b-instruct:free`
- Endpoint: `https://openrouter.ai/api/v1/chat/completions`
- Client-side API calls (Firebase Spark limitation)

### 4. Database Schema

**Entities:**
- `TransactionEntity`: Stores completed transactions
- `CategoryEntity`: Expense categories with icons/colors
- `RegexPatternEntity`: AI-generated regex patterns
- `WhitelistedAppEntity`: Monitored banking apps

**Default Categories:**
- Food (Restaurant icon, #FF6B6B)
- Transport (DirectionsCar icon, #4ECDC4)
- Shopping (ShoppingCart icon, #FFE66D)
- Entertainment (Movie icon, #95E1D3)
- Bills (Receipt icon, #F38181)
- Health (LocalHospital icon, #AA96DA)
- Other (MoreHoriz icon, #FCBAD3)

## Permissions Required

### AndroidManifest.xml
```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- System overlay -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### Runtime Permissions Needed
1. **Notification Listener Access:** Settings > Notification access
2. **Display Over Other Apps:** Settings > Special app access > Display over other apps

## Setup Instructions

### 1. Firebase Configuration
1. Add `google-services.json` to `app/` directory
2. Configure Firestore with security rules:
```javascript
match /users/{userId}/transactions/{transactionId} {
  allow read, write: if request.auth.uid == userId;
}
```

### 2. OpenRouter API Key
- Obtain API key from https://openrouter.ai
- Enter in Regex Generator screen when generating patterns
- **Never hardcode API keys in source**

### 3. Build & Run
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### 4. Initial Setup (In App)
1. Navigate to Settings
2. Enable "Notification Access" permission
3. Enable "Display Over Other Apps" permission
4. Generate regex patterns for your banking apps
5. Add banking apps to whitelist

## Usage Flow

1. **User receives banking notification**
   → Notification Listener intercepts
   
2. **Service extracts transaction data**
   → Regex patterns match amount & merchant
   
3. **Action Overlay appears**
   → User edits amount/merchant if needed
   → Selects category
   
4. **User taps Save**
   → Data saved to Room database
   → Syncs to Firestore asynchronously
   
5. **Transaction appears in Home screen**
   → Grouped by date
   → Filterable by category

## Key Design Decisions

### Offline-First
- Room is the single source of truth
- Firestore syncs in background
- App works without internet connection

### Event-Driven
- No polling for notifications
- React only to `onNotificationPosted` events
- Efficient battery usage

### Client-Side AI
- OpenRouter calls from Android app
- Workaround for Firebase Spark Plan limitations
- User provides their own API key

### No XML
- 100% Jetpack Compose
- Material 3 design system
- Dark/Light theme support

## Testing

### Manual Testing Checklist
- [ ] Notification listener receives banking notifications
- [ ] Regex patterns correctly extract amount & merchant
- [ ] Action overlay displays with correct data
- [ ] Categories load from database
- [ ] Save button persists transaction to Room
- [ ] Home screen displays transactions
- [ ] Regex generator creates valid patterns
- [ ] Settings screen opens system permission screens

### Test Notification Text Examples
```
"You spent $45.50 at Starbucks on Card ending 1234"
"Payment of Rs. 1,250.00 at Amazon India successful"
"Debit of £30.00 from your account at Tesco"
```

## Known Limitations

1. **Firebase Spark Plan:** Cloud Functions cannot make outbound HTTP calls
2. **Regex Complexity:** AI-generated patterns may need manual refinement
3. **Notification Formats:** Each banking app has unique notification format
4. **Android Versions:** Tested on API 26+

## Future Enhancements

- [ ] Budget tracking & alerts
- [ ] Spending analytics & charts
- [ ] Receipt photo attachments
- [ ] Multiple currency support
- [ ] Export to CSV/PDF
- [ ] Recurring transaction detection
- [ ] Merchant logo fetching

## Contributing

This is a foundational implementation. Contributions welcome for:
- Additional regex pattern templates
- UI/UX improvements
- Analytics features
- Performance optimizations

## License

[Your license here]

---

**Built with ❤️ using Kotlin & Jetpack Compose**
