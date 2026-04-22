@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.domain.model.RegexPattern
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.data.local.Currencies
import com.spendsense.presentation.util.SpendSenseTopBar

@Composable
fun RegexGeneratorScreen(
    viewModel: RegexGeneratorViewModel = hiltViewModel(),
    initialNotificationText: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showTargetAppSelector by remember { mutableStateOf(false) }
    var showProviderSelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }
    val configuredProviders = remember(state.providers, state.providerKeyStatuses) {
        state.providers.filter { state.providerKeyStatuses[it.id] == true }
    }
    val groupedProviders = remember(configuredProviders) {
        groupProviders(configuredProviders)
    }

    // Pre-fill initial text if provided
    LaunchedEffect(initialNotificationText) {
        if (initialNotificationText != null) {
            viewModel.updateNotificationText(initialNotificationText)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            SpendSenseTopBar(
                title = "AI Regex Generator",
                onNavigationClick = onNavigateBack,
                navigationIcon = Icons.Default.ArrowBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(72.dp))

            // Info Card
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = GlassSurface
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Paste a banking notification text below, and the AI will generate a regex pattern to extract transaction details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Provider Selection
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = GlassSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "configure models",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (configuredProviders.isEmpty()) {
                        Text(
                            "No configured AI models found. Please configure a model in AI Providers first.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Box {
                            OutlinedCard(
                                onClick = { showProviderSelector = true },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.selectedProvider?.let {
                                                "${it.name} • ${it.defaultModel}"
                                            } ?: "Select a model"
                                        )
                                        Text(
                                            text = "Tap to configure model",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = showProviderSelector,
                                onDismissRequest = { showProviderSelector = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                groupedProviders.forEachIndexed { index, group ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = group.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {},
                                        enabled = false
                                    )
                                    Divider()

                                    group.models.forEach { model ->
                                        val isSelected = state.selectedProvider?.id == model.id
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(model.defaultModel)
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = "Selected model"
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.onProviderSelected(model)
                                                showProviderSelector = false
                                            }
                                        )
                                    }

                                    if (index != groupedProviders.lastIndex) {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Section
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = GlassSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notification Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.notificationText.isNotBlank()) {
                            TextButton(onClick = { viewModel.clearInput() }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.notificationText,
                        onValueChange = { viewModel.updateNotificationText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Paste your notification text here...") },
                        maxLines = 6
                    )

                    Divider()

                    Text(
                        text = "Regex Pattern (Manual or AI)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = state.manualPattern,
                        onValueChange = { viewModel.updateManualPattern(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter regex manually or generate with AI...") },
                        label = { Text("Regex Pattern") },
                        trailingIcon = {
                            if (state.manualPattern.isNotBlank()) {
                                IconButton(onClick = { viewModel.testManualPattern() }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Test Pattern")
                                }
                            }
                        }
                    )
                }
            }


            // Generate Button
            Button(
                onClick = { viewModel.generateRegex() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.notificationText.isNotBlank() && !state.isGenerating && state.selectedProvider != null
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Rule (AI)")
                }
            }

            // Error Message
            if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Success Message
            if (state.successMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = state.successMessage!!,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Result Section
            val displayPattern = state.manualPattern.takeIf { it.isNotBlank() } ?: state.generatedPattern
            
            if (displayPattern != null) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = GlassSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (state.manualPattern.isNotBlank()) "Manual Pattern" else "Generated Pattern",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            SelectionContainer {
                                Text(
                                    text = displayPattern,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }

                        if (state.extractedAmount != null && state.extractedMerchant != null) {
                            Divider()

                            Text(
                                text = "Test Results",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TestResultChip(
                                    label = "Amount",
                                    value = state.extractedAmount!!,
                                    modifier = Modifier.weight(1f)
                                )
                                TestResultChip(
                                    label = "Merchant",
                                    value = state.extractedMerchant!!,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Divider()

                        // Save Section
                        Text(
                            text = "Save Pattern",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Currency Selector
                        Box {
                            val selectedCurrency = Currencies.find(state.currencyCode)
                            OutlinedCard(
                                onClick = { showCurrencySelector = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Default Currency",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.name}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = showCurrencySelector,
                                onDismissRequest = { showCurrencySelector = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Currencies.SUPPORTED.forEach { cur ->
                                    DropdownMenuItem(
                                        text = { Text("${cur.symbol} ${cur.code} — ${cur.name}") },
                                        onClick = {
                                            viewModel.updateCurrency(cur.code)
                                            showCurrencySelector = false
                                        }
                                    )
                                }
                            }
                        }

                        if (state.availableApps.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "No whitelisted apps yet. Please add at least one app in Whitelisted Apps settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else {
                            Box {
                                OutlinedCard(
                                    onClick = { showTargetAppSelector = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = when (state.selectedAppPackage) {
                                                    RegexPattern.TARGET_ALL_WHITELISTED -> "All whitelisted apps"
                                                    "" -> "Select whitelisted app"
                                                    else -> state.availableApps
                                                        .firstOrNull { it.packageName == state.selectedAppPackage }
                                                        ?.appName
                                                        ?: state.selectedAppPackage
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            val subtitle = when (state.selectedAppPackage) {
                                                RegexPattern.TARGET_ALL_WHITELISTED -> "Applies to every enabled whitelisted app"
                                                "" -> "Choose one app or all whitelisted apps"
                                                else -> state.selectedAppPackage
                                            }
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showTargetAppSelector,
                                    onDismissRequest = { showTargetAppSelector = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text("All whitelisted apps")
                                                Text(
                                                    "Use this pattern for every enabled whitelisted app",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onTargetAppSelected(RegexPattern.TARGET_ALL_WHITELISTED)
                                            showTargetAppSelector = false
                                        }
                                    )

                                    state.availableApps.forEach { app ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(app.appName)
                                                    Text(app.packageName, style = MaterialTheme.typography.labelSmall)
                                                }
                                            },
                                            onClick = {
                                                viewModel.onTargetAppSelected(app.packageName)
                                                showTargetAppSelector = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active")
                            Switch(
                                checked = state.isActive,
                                onCheckedChange = { viewModel.toggleActive() }
                            )
                        }

                        Button(
                            onClick = { viewModel.savePattern() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving && state.selectedAppPackage.isNotBlank() && state.availableApps.isNotEmpty()
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving...")
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to Watchlist")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestResultChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = GlassSurface,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
