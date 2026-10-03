# Testing Bank Notification Extraction Without a Real Bank

This guide details the most effective, production-safe method to test the end-to-end transaction notification extraction pipeline in SpendSense on a test device or emulator, without installing any banking apps.

---

## The Challenge

SpendSense uses a security feature to filter out notifications from unauthorized apps. It will **only** process notifications if the sending application's package name is:
1. Registered in the `whitelisted_apps` database table.
2. Enabled (`isEnabled = 1`) by the user.

Because the Android System Shell (`com.android.shell`) is a system package, it is hidden from the "Whitelisted Apps" screen by default. This guide provides a clean, production-safe way to auto-whitelist it for local debugging.

---

## Step 1: Grant Notification Access & Sensitive Data Permissions

Android's `NotificationListenerService` requires explicit system-level authorization to intercept notifications.

### Option A: Via ADB (Fastest)
1. Run the following terminal command to grant standard notification access directly to your debug build:
   ```bash
   adb shell cmd notification allow_listener com.spendsense/com.spendsense.data.service.TransactionNotificationListener
   ```

2. **CRITICAL FOR ANDROID 15+:** Starting in Android 15, the system aggressively redacts notification bodies (replacing them with `"Sensitive notification content hidden"` or empty strings) when read by a `NotificationListenerService`. To bypass this safety feature for debugging:
   ```bash
   adb shell appops set com.spendsense RECEIVE_SENSITIVE_NOTIFICATIONS allow
   ```

### Option B: Manually on the Device
1. Open **Settings** -> **Apps** -> **Special App Access** -> **Notification Access**.
2. Select **SpendSense** and toggle **Allow notification access** to **ON**.
3. If on Android 15+, search Settings for **Enhanced notifications** and verify you allow displaying sensitive content.

---

## Step 2: Add the Production-Safe Auto-Whitelist Hook

To whitelist the system shell package (`com.android.shell`) so it can trigger mock notifications, add a quick, safe initialization block inside `MainActivity.kt`. Wrapping this inside a `BuildConfig.DEBUG` check ensures that the hook is active for development but completely omitted from production releases.

1. Open [MainActivity.kt](file:///home/vab/apps/spend-sense/app/src/main/java/com/spendsense/presentation/MainActivity.kt).
2. Inject the `WhitelistedAppDao` at the top of your class:
   ```kotlin
   @Inject
   lateinit var whitelistedAppDao: com.spendsense.data.local.dao.WhitelistedAppDao
   ```
3. Add the following insertion block inside `onCreate()`, immediately below `categoryRepository.initializeDefaultCategories()`:
   ```kotlin
   if (com.spendsense.BuildConfig.DEBUG) {
       lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
           whitelistedAppDao.insert(
               com.spendsense.data.local.entity.WhitelistedAppEntity(
                   packageName = "com.android.shell",
                   appName = "Android Shell (Debug)",
                   isEnabled = true
               )
           )
       }
   }
   ```
4. Build and run the app on your emulator or test device. The Android System Shell is now auto-whitelisted in the database.

---

## Step 3: Configure a Notification Pattern in SpendSense

Now that the shell package is whitelisted, we need to instruct SpendSense on how to parse notifications coming from it.

1. Open **SpendSense** on your test device.
2. Go to **Settings** -> **Notification Patterns** -> **Add Pattern**.
3. Fill in the pattern fields exactly as follows:
   * **App Package Name:** `com.android.shell`
   * **Title Contains:** `Chase`
   * **Regex Pattern:** `Spent (?<amount>\d+\.\d{2}) at (?<merchant>[\w\s\-\#\.\,\&]+)`
   * **Is Transaction:** *Enabled / Checked*
4. Save the pattern.

---

## Step 4: Fire Mock Notifications via ADB

You can now use `adb` to post custom notification texts from the system shell. Keep the SpendSense app active on your test screen to watch the integration happen in real-time.

> [!IMPORTANT]
> When executing `adb shell` from your host terminal, local shells (like Bash, Zsh, or PowerShell) strip the outer double quotes before passing them. This causes the remote Android shell to split your notification arguments incorrectly. To prevent this, you **MUST** wrap the entire command in double quotes and use single quotes inside, as shown below.

### Scenario A: Successful Extraction (Regex Matches)
Post a notification containing a transaction that matches your regex pattern:
```bash
adb shell "cmd notification post -t 'Chase Alert' test-tag-1 'Spent 45.50 at Starbucks'"
```
* **Expected Result:** SpendSense intercepts the notification, extracts **$45.50** and **Starbucks**, and immediately displays the system-level overlay prompt asking you to categorize the transaction.

### Scenario B: Failed Extraction (Saved to Inbox)
Post a notification from the whitelisted app that doesn't match the regex pattern (or has an unknown title):
```bash
adb shell "cmd notification post -t 'Chase Alert' test-tag-2 'Your monthly statement is ready'"
```
* **Expected Result:** Since the pattern matched the title "Chase Alert" but the body did not match the transaction regex, SpendSense captures it safely as a raw notification and puts it in the **Notification Inbox** on the Home screen for manual review.

---

## Alternative: Testing with Real Messaging Apps (Zero-Code)

If you prefer not to touch the codebase or use ADB terminal commands:

1. Open SpendSense -> **Whitelisted Apps**.
2. Find any user-installed chat or messaging app you already have on the device (e.g. **Telegram** or **WhatsApp**).
3. Toggle it **ON** (Since it's a user app, it is visible in the list).
4. Create a pattern for its package name (e.g. `org.telegram.messenger`).
5. Send yourself a message from another phone/account containing: `"Spent 12.50 at Costco"`.
