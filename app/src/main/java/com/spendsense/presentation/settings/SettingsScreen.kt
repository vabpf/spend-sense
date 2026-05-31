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
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    val settingsLiquidState = rememberLiquidState()

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
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(settingsLiquidState)
            ) {
                Image(
                    painter = painterResource(id = com.spendsense.R.drawable.bg_pexel),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )

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

                    HorizontalDivider()

                    SettingsItem(
                        icon = Icons.Rounded.CloudUpload,
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

private fun isNotificationAccessGranted(context: android.content.Context): Boolean {
    val enabledListeners = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabledListeners != null && enabledListeners.split(":").any {
        it.startsWith(context.packageName)
    }
}
