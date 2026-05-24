package com.spendsense.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.Currencies
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

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

    Scaffold(containerColor = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp)
                .padding(top = 72.dp)
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
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
                        description = "Required to read banking notifications",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                    
                    HorizontalDivider()
                    
                    SettingsItem(
                        icon = Icons.Rounded.Layers,
                        title = "Display Over Other Apps",
                        description = "Required to show transaction overlay",
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
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
        } // Column
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
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
