package com.spendsense.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToRegexGenerator: () -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions Section
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleLarge
            )

            Card {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notification Access",
                        description = "Required to read banking notifications",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.Layers,
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
                text = "Configuration",
                style = MaterialTheme.typography.titleLarge
            )

            Card {
                Column {
                    SettingsItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "Regex Generator",
                        description = "Create AI-powered regex patterns",
                        onClick = onNavigateToRegexGenerator
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.Apps,
                        title = "Whitelisted Apps",
                        description = "Manage apps to monitor",
                        onClick = { /* TODO */ }
                    )
                    
                    Divider()
                    
                    SettingsItem(
                        icon = Icons.Default.Category,
                        title = "Categories",
                        description = "Manage expense categories",
                        onClick = { /* TODO */ }
                    )
                }
            }

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge
            )

            Card {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        description = "1.0.0",
                        onClick = null
                    )
                }
            }
        }
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
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
