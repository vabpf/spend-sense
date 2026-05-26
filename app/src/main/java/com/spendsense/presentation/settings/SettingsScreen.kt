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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.Currencies
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
                    navigationIcon = Icons.Rounded.ArrowBack
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
