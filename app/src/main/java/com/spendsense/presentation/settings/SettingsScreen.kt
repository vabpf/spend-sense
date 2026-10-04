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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import com.spendsense.presentation.util.fadingEdge
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

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEnd = statusBarPadding + 98.dp
    val density = LocalDensity.current
    val fadeEndPx = with(density) { fadeEnd.toPx() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Background scrim: transparent at top wallpaper, smoothly fades directly from 0 to 100 into #F8FAFC
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF8FAFC)),
                            startY = 0f,
                            endY = fadeEndPx
                        )
                    )
            )

            // Scrollable settings cards with dissolve effect matching the exact background fade position
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(
                        topFadeStart = 0.dp,
                        topFadeHeight = fadeEnd
                    ),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = fadeEnd + 6.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Permissions Section
                item {
                    SettingsSectionHeader("Permissions")
                }
                item {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Notifications,
                            title = "Notification Access",
                            description = if (isAccessGranted) "Access granted" else "Required to read banking notifications",
                            iconBadgeBg = Color(0xFFDBEAFE),
                            iconTint = Color(0xFF2563EB),
                            descriptionColor = if (isAccessGranted) Color(0xFF16A34A) else Color(0xFFDC2626),
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                    }
                }

                // Preferences Section
                item {
                    SettingsSectionHeader("Preferences")
                }
                item {
                    SettingsGroupCard {
                        val selectedCurrency = Currencies.find(state.defaultCurrency)
                        SettingsItem(
                            icon = Icons.Rounded.CurrencyExchange,
                            title = "Default Currency",
                            description = "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.name}",
                            iconBadgeBg = Color(0xFFD1FAE5),
                            iconTint = Color(0xFF059669),
                            onClick = { showCurrencySelector = true }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsSwitchItem(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Daily Spent Summary",
                            description = "Get a daily push notification reporting total spending",
                            checked = state.isDailyReportEnabled,
                            iconBadgeBg = Color(0xFFEDE9FE),
                            iconTint = Color(0xFF7C3AED),
                            onCheckedChange = { enabled ->
                                viewModel.updateDailyReportEnabled(enabled, context)
                            }
                        )

                        if (state.isDailyReportEnabled) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            SettingsItem(
                                icon = Icons.Rounded.Schedule,
                                title = "Report Delivery Time",
                                description = "Scheduled at ${state.dailyReportTime}",
                                iconBadgeBg = Color(0xFFFFEDD5),
                                iconTint = Color(0xFFEA580C),
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
                item {
                    SettingsSectionHeader("Appearance")
                }
                item {
                    SettingsGroupCard {
                        val currentTheme = AppBackgroundOption.entries.find { it.key == state.backgroundTheme } ?: AppBackgroundOption.CYBERPUNK
                        SettingsItem(
                            icon = Icons.Rounded.Image,
                            title = "App Background",
                            description = "${currentTheme.title} — ${currentTheme.description}",
                            iconBadgeBg = Color(0xFFFFE4E6),
                            iconTint = Color(0xFFE11D48),
                            onClick = { showBackgroundSelector = true }
                        )
                    }
                }

                // Configuration Section
                item {
                    SettingsSectionHeader("Configuration")
                }
                item {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.SmartToy,
                            title = "AI Providers",
                            description = "Configure AI models and API keys",
                            iconBadgeBg = Color(0xFFDBEAFE),
                            iconTint = Color(0xFF2563EB),
                            onClick = onNavigateToAiProviders
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsItem(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Regex Generator",
                            description = "Create AI-powered regex patterns",
                            iconBadgeBg = Color(0xFFF3E8FF),
                            iconTint = Color(0xFF9333EA),
                            onClick = onNavigateToRegexGenerator
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsItem(
                            icon = Icons.Rounded.Pattern,
                            title = "Notification Patterns",
                            description = "View and manage (app × title) pattern rules",
                            iconBadgeBg = Color(0xFFE0F2FE),
                            iconTint = Color(0xFF0284C7),
                            onClick = onNavigateToNotificationPatterns
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsItem(
                            icon = Icons.Rounded.Apps,
                            title = "Whitelisted Apps",
                            description = "Manage apps to monitor",
                            iconBadgeBg = Color(0xFFD1FAE5),
                            iconTint = Color(0xFF059669),
                            onClick = onNavigateToWhitelistedApps
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsItem(
                            icon = Icons.Rounded.Category,
                            title = "Categories",
                            description = "Manage expense categories",
                            iconBadgeBg = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            onClick = onNavigateToCategories
                        )
                    }
                }

                // Data & Backup Section
                item {
                    SettingsSectionHeader("Data & Backup")
                }
                item {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.CloudDownload,
                            title = "Export All Data",
                            description = "Export transactions, categories, regexes & settings to JSON",
                            iconBadgeBg = Color(0xFFE0F2FE),
                            iconTint = Color(0xFF0284C7),
                            onClick = {
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                exportLauncher.launch("spendsense_backup_$timestamp.json")
                            }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsItem(
                            icon = Icons.Rounded.CloudUpload,
                            title = "Import All Data",
                            description = "Restore full app data from a SpendSense backup JSON",
                            iconBadgeBg = Color(0xFFF1F5F9),
                            iconTint = Color(0xFF475569),
                            onClick = { importBackupLauncher.launch("*/*") }
                        )

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        SettingsItem(
                            icon = Icons.Rounded.History,
                            title = "Import Notifications",
                            description = "Import and process historical CSV/JSON files",
                            iconBadgeBg = Color(0xFFEDE9FE),
                            iconTint = Color(0xFF7C3AED),
                            onClick = { filePickerLauncher.launch("*/*") }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSectionHeader("About")
                }
                item {
                    SettingsGroupCard {
                        SettingsItem(
                            icon = Icons.Rounded.Info,
                            title = "Version",
                            description = "1.1.0",
                            iconBadgeBg = Color(0xFFF1F5F9),
                            iconTint = Color(0xFF64748B),
                            onClick = null
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Pinned Header (Settings title + subtitle, no back button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarPadding + 10.dp,
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 10.dp
                    )
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.65f),
                            offset = Offset(0f, 2f),
                            blurRadius = 10f
                        )
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Customize capture, AI, and defaults",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.55f),
                            offset = Offset(0f, 1f),
                            blurRadius = 6f
                        )
                    ),
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }

    if (showCurrencySelector) {
        GlassAlertDialog(
            onDismissRequest = { showCurrencySelector = false },
            title = {
                Text(
                    text = "Default Currency",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currencies.SUPPORTED.forEach { cur ->
                        val isSelected = cur.code == state.defaultCurrency
                        Surface(
                            onClick = {
                                viewModel.updateDefaultCurrency(cur.code)
                                showCurrencySelector = false
                            },
                            color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${cur.symbol} ${cur.code} — ${cur.name}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0369A1) else Color(0xFF0F172A)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0284C7)
                                    )
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
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                            color = if (isSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1),
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
                                        color = if (isSelected) Color(0xFF0369A1) else Color(0xFF0F172A),
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
                                        color = Color(0xFF64748B)
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF0284C7),
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
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(Color.White, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF475569),
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    iconBadgeBg: Color = Color(0xFFDBEAFE),
    iconTint: Color = Color(0xFF2563EB),
    descriptionColor: Color = Color(0xFF64748B),
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
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
    iconBadgeBg: Color = Color(0xFFEDE9FE),
    iconTint: Color = Color(0xFF7C3AED),
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Color.Transparent,
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF0284C7),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCBD5E1)
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
