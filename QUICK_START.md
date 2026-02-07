# SpendSense Quick Start Guide

## Overview
SpendSense is a financial tracking app that automatically captures transaction data from banking notifications and helps you categorize expenses in real-time.

## Prerequisites

### Required Tools
- Android Studio Arctic Fox or later
- Android SDK 26+ (Android 8.0+)
- JDK 17
- Gradle 8.2+

### Required Accounts
- Firebase account (for Firestore - optional for initial testing)
- OpenRouter account with API key (for AI regex generation)

---

## Setup Steps

### 1. Clone and Open Project
```bash
git clone https://github.com/vabpf/spend-sense.git
cd spend-sense
```

Open the project in Android Studio.

### 2. Configure Firebase (Optional for Initial Testing)
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add an Android app with package name: `com.spendsense`
4. Download `google-services.json`
5. Place it in `app/` directory

**Note:** The app will work without Firebase for local testing. Firebase is only needed for cloud sync.

### 3. Build the Project
```bash
./gradlew build
```

Or in Android Studio: Build → Make Project

### 4. Install on Device/Emulator
```bash
./gradlew installDebug
```

Or use the Run button in Android Studio.

---

## First Launch Configuration

### 1. Grant Notification Access
1. Open SpendSense app
2. Navigate to Settings
3. Tap "Notification Access"
4. Enable SpendSense in system settings
5. Return to app

### 2. Grant Overlay Permission
1. In Settings screen
2. Tap "Display Over Other Apps"
3. Enable SpendSense in system settings
4. Return to app

### 3. Generate Your First Regex Pattern

#### Example: Test with Sample Notification
1. Navigate to Settings → Regex Generator
2. Paste this sample notification text:
   ```
   You spent $45.50 at Starbucks on Card ending 1234
   ```
3. Tap "Generate Rule (AI)"
4. Enter your OpenRouter API key when prompted
5. Review the generated regex pattern
6. Enter package name: `com.example.bankapp`
7. Toggle "Active" to ON
8. Tap "Add to Watchlist"

---

## Testing the Flow

### Simulate a Banking Notification

Since you may not want to make real transactions for testing, here's how to test:

#### Option 1: Use a Notification Testing App
1. Install a notification testing app from Play Store
2. Create a test notification with text like:
   ```
   Transaction of $25.00 at Amazon successful
   ```
3. Use your banking app's package name
4. Send the notification

#### Option 2: Create a Test Banking App
Create a simple test app that sends notifications with transaction-like text.

### Expected Behavior

When a matching notification is detected:
1. ✅ Notification is intercepted by TransactionNotificationListener
2. ✅ Regex pattern extracts amount ($45.50) and merchant (Starbucks)
3. ✅ Action Overlay appears on screen
4. ✅ Amount and merchant are pre-filled
5. ✅ User selects category (e.g., Food)
6. ✅ User taps Save
7. ✅ Transaction appears on Home screen

---

## Common Issues & Solutions

### Issue 1: "Notification Listener not working"
**Solution:**
- Go to Settings → Apps → SpendSense → Permissions
- Ensure all permissions are granted
- Restart the app
- Check in Settings → Notification Access that SpendSense is enabled

### Issue 2: "Overlay not appearing"
**Solution:**
- Go to Settings → Special app access → Display over other apps
- Ensure SpendSense is allowed
- Check that ActionOverlayService is running (you should see a persistent notification)

### Issue 3: "Regex pattern not matching"
**Solution:**
- Review the notification text format from your banking app
- The format may be different from the AI-generated pattern
- Manually edit the regex in the database or regenerate with a different sample
- Use a regex testing tool to validate the pattern

### Issue 4: "Build errors"
**Solution:**
- Ensure you have the correct Android SDK version (34)
- Check that Kotlin plugin version matches (1.9.22)
- Clean and rebuild: `./gradlew clean build`
- Invalidate caches in Android Studio: File → Invalidate Caches / Restart

---

## Sample Notification Formats

### Common Banking App Notification Patterns

**Format 1: Simple**
```
You spent $45.50 at Starbucks
```

**Format 2: Detailed**
```
Transaction of Rs. 1,250.00 at Amazon India successful. Card ending 1234
```

**Format 3: Debit**
```
Debit of £30.00 from your account at Tesco. Available balance: £500.00
```

**Format 4: UPI**
```
₹500 sent to MERCHANT_NAME via UPI. Ref: 123456789
```

### Corresponding Regex Patterns (Examples)

**Pattern 1:**
```regex
(?<amount>\$[\d,]+\.?\d*)\s+at\s+(?<merchant>[\w\s]+)
```

**Pattern 2:**
```regex
Transaction of (?<amount>Rs\.?\s?[\d,]+\.?\d*)\s+at\s+(?<merchant>[\w\s]+)
```

**Pattern 3:**
```regex
Debit of (?<amount>£[\d,]+\.?\d*)\s+.*\s+at\s+(?<merchant>[\w\s]+)\.
```

---

## Understanding the Database

### Checking Data in Database

You can use Android Studio's Database Inspector:
1. Run the app on an emulator/device
2. Go to View → Tool Windows → App Inspection
3. Select Database Inspector
4. Browse tables: transactions, categories, regex_patterns, whitelisted_apps

### Default Categories

On first launch, 7 default categories are created:
1. Food - Restaurant icon (#FF6B6B)
2. Transport - DirectionsCar icon (#4ECDC4)
3. Shopping - ShoppingCart icon (#FFE66D)
4. Entertainment - Movie icon (#95E1D3)
5. Bills - Receipt icon (#F38181)
6. Health - LocalHospital icon (#AA96DA)
7. Other - MoreHoriz icon (#FCBAD3)

---

## Development Tips

### Hot Reload
- Jetpack Compose supports hot reload
- Changes to @Composable functions update immediately
- No need to rebuild for UI changes

### Debugging Notifications
- Add breakpoints in `TransactionNotificationListener.onNotificationPosted()`
- Check logs with tag "TransactionNotification"
- Use `adb logcat | grep SpendSense` to filter logs

### Testing Regex Patterns
- Use online regex testers: regex101.com
- Set flavor to Java
- Use named groups syntax: `(?<name>pattern)`

### Database Migrations
- Room database version is 1
- Future changes need migration strategy
- Use `fallbackToDestructiveMigration()` for development only

---

## API Key Management

### Getting OpenRouter API Key
1. Visit https://openrouter.ai/
2. Sign up or log in
3. Go to Keys section
4. Create a new API key
5. Copy the key (starts with `sk-or-v1-...`)

### Using the API Key
- Enter in Regex Generator screen when prompted
- Never hardcode in source files
- Consider using encrypted shared preferences for production

### Free Tier Model
- Model: `meta-llama/llama-3.2-3b-instruct:free`
- Sufficient for regex generation
- No credit card required
- Rate limits apply

---

## Project Structure Tour

```
app/src/main/java/com/spendsense/
├── data/
│   ├── local/
│   │   ├── entity/          ← Database entities
│   │   ├── dao/             ← Database operations
│   │   └── SpendSenseDatabase.kt
│   ├── remote/              ← API integration
│   ├── repository/          ← Data layer implementations
│   └── service/             ← Background services
├── domain/
│   ├── model/               ← Business models
│   └── repository/          ← Repository contracts
├── presentation/
│   ├── home/                ← Transaction list screen
│   ├── settings/            ← Settings & Regex Generator
│   ├── overlay/             ← Action Overlay Service
│   └── theme/               ← Material 3 theme
└── di/                      ← Dependency injection
```

---

## Next Steps

### Immediate
1. ✅ Build and run the app
2. ✅ Configure permissions
3. ✅ Generate test regex patterns
4. ✅ Test with sample notifications

### Short Term
1. Add more regex patterns for different banks
2. Implement whitelisted apps management UI
3. Add category management UI
4. Implement manual transaction entry

### Long Term
1. Implement Firestore sync
2. Add analytics and charts
3. Budget tracking features
4. Receipt photo attachments
5. Export functionality

---

## Getting Help

### Resources
- README.md - Comprehensive documentation
- IMPLEMENTATION_SUMMARY.md - Technical details
- Code comments - In-line documentation

### Community
- File issues on GitHub
- Check existing issues for solutions
- Contribute improvements via Pull Requests

---

## Security Notes

### Important
- ⚠️ Never commit `google-services.json` to public repos
- ⚠️ Never hardcode API keys
- ⚠️ Use ProGuard rules for release builds
- ⚠️ Implement Firestore security rules properly

### Firestore Security Rules Template
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/transactions/{transactionId} {
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

---

## Congratulations! 🎉

You're now ready to use SpendSense. The app will automatically track your expenses from banking notifications and help you categorize them in real-time.

**Happy tracking!** 💰
