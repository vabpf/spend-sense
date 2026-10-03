package com.spendsense.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.Currencies
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.LocalLiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.spendsense.presentation.theme.AppBackgroundOption
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRegexGenerator: () -> Unit = {},
    onNavigateToAiProviders: () -> Unit = {},
    onNavigateToWhitelistedApps: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToNotificationPatterns: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showCurrencySelector by remember { mutableStateOf(false) }
    var showBackgroundSelector by remember { mutableStateOf(false) }
    val settingsLiquidState = rememberLiquidState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomBackground(uri, context)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    viewModel.importNotificationsFromFile(content)
                }
            } catch (_: Exception) {}
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                    viewModel.exportBackup(outputStream)
                }
            } catch (_: Exception) {}
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    viewModel.onBackupFileSelected(content)
                }
            } catch (_: Exception) {}
        }
    }

    var replaceExistingData by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAccessGranted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessGranted = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
            Text(
                text = "Customize capture, AI, and defaults",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Permissions Section
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        shape = MaterialTheme.shapes.large,
                        containerColor = GlassSurface.copy(alpha = 0.8f),
                        borderAlpha = 0.24f
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Rounded.Notifications,
                        title = "Notification Access",
                        description = if (isAccessGranted) "Access granted" else "Required to read banking notifications",
                        descriptionColor = if (isAccessGranted) Color(0xFF81C784) else Color(0xFFE57373),
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }

            // Configuration Section
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        shape = MaterialTheme.shapes.large,
                        containerColor = GlassSurface.copy(alpha = 0.8f),
                        borderAlpha = 0.24f
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    val selectedCurrency = Currencies.find(state.defaultCurrency)
                    SettingsItem(
                        icon = Icons.Rounded.CurrencyExchange,
                        title = "Default Currency",
                        description = "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.name}",
                        onClick = { showCurrencySelector = true }
                    )

                    HorizontalDivider()

                    SettingsSwitchItem(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Daily Spent Summary",
                        description = "Get a daily push notification reporting total spending",
                        checked = state.isDailyReportEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateDailyReportEnabled(enabled, context)
                        }
                    )

                    if (state.isDailyReportEnabled) {
                        HorizontalDivider()

                        SettingsItem(
                            icon = Icons.Rounded.Schedule,
                            title = "Report Delivery Time",
                            description = "Scheduled at ${state.dailyReportTime}",
                            onClick = {
                                val parts = state.dailyReportTime.split(":")
                                val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
                                val currentMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
                                        viewModel.updateDailyReportTime(formattedTime, context)
                                    },
                                    currentHour,
                                    currentMinute,
                                    true
                                ).show()
                            }
                        )
                    }
                }
            }

            // Appearance Section
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        shape = MaterialTheme.shapes.large,
                        containerColor = GlassSurface.copy(alpha = 0.8f),
                        borderAlpha = 0.24f
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    val currentTheme = AppBackgroundOption.entries.find { it.key == state.backgroundTheme } ?: AppBackgroundOption.CYBERPUNK
                    SettingsItem(
                        icon = Icons.Rounded.Wallpaper,
                        title = "App Background",
                        description = "${currentTheme.title} — ${currentTheme.description}",
                        onClick = { showBackgroundSelector = true }
                    )
                }
            }

            // Configuration Section
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        shape = MaterialTheme.shapes.large,
                        containerColor = GlassSurface.copy(alpha = 0.8f),
                        borderAlpha = 0.24f
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Rounded.SmartToy,
                        title = "AI Providers",
                        description = "Configure AI models and API keys",
                        onClick = onNavigateToAiProviders
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Regex Generator",
                        description = "Create AI-powered regex patterns",
                        onClick = onNavigateToRegexGenerator
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Rounded.Pattern,
                        title = "Notification Patterns",
                        description = "View and manage (app × title) pattern rules",
                        onClick = onNavigateToNotificationPatterns
                    )

                    HorizontalDivider()

                    
                    SettingsItem(
                        icon = Icons.Rounded.Apps,
                        title = "Whitelisted Apps",
                        description = "Manage apps to monitor",
                        onClick = onNavigateToWhitelistedApps
                    )
                    
                    HorizontalDivider()
                    
                    SettingsItem(
                        icon = Icons.Rounded.Category,
                        title = "Categories",
                        description = "Manage expense categories",
                        onClick = onNavigateToCategories
                    )
                }
            }

            // Data & Backup Section
            Text(
                text = "Data & Backup",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        shape = MaterialTheme.shapes.large,
                        containerColor = GlassSurface.copy(alpha = 0.8f),
                        borderAlpha = 0.24f
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Rounded.CloudDownload,
                        title = "Export All Data",
                        description = "Export transactions, categories, regexes & settings to JSON",
                        onClick = {
                            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            exportLauncher.launch("spendsense_backup_$timestamp.json")
                        }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Rounded.CloudUpload,
                        title = "Import All Data",
                        description = "Restore full app data from a SpendSense backup JSON",
                        onClick = { importBackupLauncher.launch("*/*") }
                    )

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Rounded.History,
                        title = "Import Notifications",
                        description = "Import and process historical CSV/JSON files",
                        onClick = { filePickerLauncher.launch("*/*") }
                    )
                }
            }

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp),
                modifier = Modifier.padding(top = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassEffect(
                        shape = MaterialTheme.shapes.large,
                        containerColor = GlassSurface.copy(alpha = 0.8f),
                        borderAlpha = 0.24f
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Rounded.Info,
                        title = "Version",
                        description = "1.0.0",
                        onClick = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
            } // inner Column
            } // inner liquefiable Box

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background,
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            0.75f to MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                            1.0f to Color.Transparent
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            CompositionLocalProvider(LocalLiquidState provides settingsLiquidState) {
                SpendSenseTopBar(
                    title = "Settings",
                    onNavigationClick = onNavigateBack,
                    navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack
                )
            }
        } // outer Box
    }

    if (showCurrencySelector) {
        GlassAlertDialog(
            onDismissRequest = { showCurrencySelector = false },
            title = { Text("Default Currency") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Currencies.SUPPORTED.forEach { cur ->
                        val isSelected = cur.code == state.defaultCurrency
                        Surface(
                            onClick = {
                                viewModel.updateDefaultCurrency(cur.code)
                                showCurrencySelector = false
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${cur.symbol} ${cur.code} — ${cur.name}", style = MaterialTheme.typography.bodyLarge)
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencySelector = false }) { Text("Cancel") }
            }
        )
    }

    if (showBackgroundSelector) {
        GlassAlertDialog(
            onDismissRequest = { showBackgroundSelector = false },
            title = {
                Text(
                    text = "App Background",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppBackgroundOption.entries.forEach { option ->
                        val isSelected = option.key == state.backgroundTheme
                        Surface(
                            onClick = {
                                if (option == AppBackgroundOption.CUSTOM) {
                                    if (state.customBackgroundPath == null || isSelected) {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                        showBackgroundSelector = false
                                    } else {
                                        viewModel.updateBackgroundTheme(option.key)
                                        showBackgroundSelector = false
                                    }
                                } else {
                                    viewModel.updateBackgroundTheme(option.key)
                                    showBackgroundSelector = false
                                }
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            when (option) {
                                                AppBackgroundOption.CYBERPUNK -> Modifier.background(
                                                    Brush.linearGradient(listOf(Color(0xFF38006B), Color(0xFF00E5FF)))
                                                )
                                                AppBackgroundOption.DEEP_SPACE -> Modifier.background(
                                                    Brush.verticalGradient(listOf(Color(0xFF070514), Color(0xFF19113B)))
                                                )
                                                AppBackgroundOption.CYBER_NEON -> Modifier.background(
                                                    Brush.verticalGradient(listOf(Color(0xFF040D18), Color(0xFF0B2D3A)))
                                                )
                                                AppBackgroundOption.OLED_BLACK -> Modifier.background(
                                                    Color.Black
                                                )
                                                AppBackgroundOption.CUSTOM -> Modifier.background(
                                                    Brush.sweepGradient(listOf(Color(0xFFFF007F), Color(0xFF00E5FF), Color(0xFFFFD700), Color(0xFFFF007F)))
                                                )
                                            }
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (option == AppBackgroundOption.CUSTOM) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = if (option == AppBackgroundOption.CUSTOM) {
                                            if (isSelected && state.customBackgroundPath != null) {
                                                "Active custom photo • Tap to change"
                                            } else if (state.customBackgroundPath != null) {
                                                "Saved photo • Tap to activate"
                                            } else {
                                                option.description
                                            }
                                        } else {
                                            option.description
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackgroundSelector = false }) { Text("Cancel") }
            }
        )
    }

    if (state.isImporting) {
        GlassAlertDialog(
            onDismissRequest = {},
            title = { Text("Importing Notifications") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Processing your notification archive file. This might take a few moments.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }

    state.importResult?.let { result ->
        GlassAlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF81C784)
                    )
                    Text("Import Complete")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Historical notification file has been successfully processed:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Notifications Parsed", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${result.totalParsed}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("New Apps Whitelisted", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${result.newAppsWhitelisted}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transactions Auto-Saved", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${result.transactionsCreated}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sent to Pending Inbox", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${result.inboxCreated}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Marketing Messages Skipped", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${result.skipped}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearImportResult() }) {
                    Text("Close")
                }
            }
        )
    }

    // Full Backup Restore Confirmation Dialog
    state.pendingRestorePayload?.let { payload ->
        GlassAlertDialog(
            onDismissRequest = { viewModel.dismissRestoreConfirmDialog() },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Restore Backup")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val dateText = if (payload.exportedAtFormatted.isNotBlank()) {
                        payload.exportedAtFormatted
                    } else if (payload.exportedAt > 0) {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(payload.exportedAt))
                    } else "Unknown"

                    Text(
                        text = "Backup dated: $dateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transactions", style = MaterialTheme.typography.bodyMedium)
                        Text("${payload.transactions.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Categories", style = MaterialTheme.typography.bodyMedium)
                        Text("${payload.categories.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Regex Patterns", style = MaterialTheme.typography.bodyMedium)
                        Text("${payload.notificationPatterns.size}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Whitelisted Apps", style = MaterialTheme.typography.bodyMedium)
                        Text("${payload.whitelistedApps.size}", fontWeight = FontWeight.Bold)
                    }
                    if (payload.providerAccounts.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AI Provider Accounts", style = MaterialTheme.typography.bodyMedium)
                            Text("${payload.providerAccounts.size}", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Restore Strategy",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        onClick = { replaceExistingData = false },
                        color = if (!replaceExistingData) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !replaceExistingData,
                                onClick = { replaceExistingData = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Merge with existing data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Keeps current data and imports new items without overwriting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Surface(
                        onClick = { replaceExistingData = true },
                        color = if (replaceExistingData) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = replaceExistingData,
                                onClick = { replaceExistingData = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Replace existing data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                Text("Clears current records and performs a clean restore", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.executeRestore(replaceExisting = replaceExistingData)
                    },
                    colors = if (replaceExistingData) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestoreConfirmDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restoring / Exporting Progress Dialog
    if (state.isRestoring || state.isExporting) {
        GlassAlertDialog(
            onDismissRequest = {},
            title = {
                Text(if (state.isRestoring) "Restoring Data" else "Exporting Data")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (state.isRestoring) "Restoring your SpendSense data archive. Please wait..." else "Generating and saving your backup archive. Please wait...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }

    // Restore Success Dialog
    state.restoreSummary?.let { summary ->
        GlassAlertDialog(
            onDismissRequest = { viewModel.clearRestoreSummary() },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF81C784)
                    )
                    Text("Restore Complete")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Your SpendSense backup has been successfully restored:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Transactions Restored", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary.transactionsRestored}", fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Categories Restored", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary.categoriesRestored}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Regex Patterns Restored", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary.patternsRestored}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Whitelisted Apps Restored", style = MaterialTheme.typography.bodyMedium)
                        Text("${summary.appsRestored}", fontWeight = FontWeight.Bold)
                    }
                    if (summary.providerAccountsRestored > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AI Providers Restored", style = MaterialTheme.typography.bodyMedium)
                            Text("${summary.providerAccountsRestored}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearRestoreSummary() }) {
                    Text("OK")
                }
            }
        )
    }

    // Export Success Dialog
    state.exportSuccessMessage?.let { msg ->
        GlassAlertDialog(
            onDismissRequest = { viewModel.clearExportMessage() },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF81C784)
                    )
                    Text("Export Complete")
                }
            },
            text = {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.clearExportMessage() }) {
                    Text("OK")
                }
            }
        )
    }

    // Error Dialog (Restore or Export error)
    val errorMessage = state.restoreError ?: state.exportError
    errorMessage?.let { err ->
        GlassAlertDialog(
            onDismissRequest = {
                viewModel.clearRestoreError()
                viewModel.clearExportMessage()
            },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Backup Error")
                }
            },
            text = {
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearRestoreError()
                    viewModel.clearExportMessage()
                }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    descriptionColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)?
) {
    Surface(
        color = Color.Transparent,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = descriptionColor
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Color.Transparent,
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            )
        }
    }
}

private fun isNotificationAccessGranted(context: android.content.Context): Boolean {
    val enabledListeners = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabledListeners != null && enabledListeners.split(":").any {
        it.startsWith(context.packageName)
    }
}
