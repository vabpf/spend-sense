@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.data.local.Currencies
import com.spendsense.presentation.util.GlassAlertDialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import com.spendsense.presentation.util.LocalLiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

@Composable
fun RegexGeneratorScreen(
    viewModel: RegexGeneratorViewModel = hiltViewModel(),
    initialNotificationText: String? = null,
    initialNotificationTitle: String? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToNotificationPatterns: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showTargetAppSelector by remember { mutableStateOf(false) }
    var showProviderSelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }

    // Pre-fill initial text and title if provided
    LaunchedEffect(initialNotificationText, initialNotificationTitle) {
        if (initialNotificationText != null) {
            viewModel.updateNotificationText(initialNotificationText)
        }
        if (initialNotificationTitle != null) {
            viewModel.updateNotificationTitle(initialNotificationTitle)
        }
    }

    val regexLiquidState = rememberLiquidState()

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
                    .liquefiable(regexLiquidState)
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
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp,
                            bottom = 120.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            // Info Card
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
                    containerColor = Color.Transparent
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Paste a banking notification below. The AI will classify it and generate a regex pattern.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Input Section
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
                    containerColor = Color.Transparent
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
                            text = "Notification Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.notificationText.isNotBlank()) {
                            TextButton(onClick = { viewModel.clearInput() }) {
                                Icon(Icons.Rounded.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.notificationTitle,
                        onValueChange = { viewModel.updateNotificationTitle(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Transaction title from the notification") },
                        label = { Text("Notification Title") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.notificationText,
                        onValueChange = { viewModel.updateNotificationText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Paste the full notification body here...") },
                        label = { Text("Notification Body") },
                        maxLines = 6
                    )

                    HorizontalDivider()

                    Text(
                        text = "Regex Pattern",
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
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Test Pattern")
                                }
                            }
                        }
                    )
                }
            }

            // Model Selection
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
                    containerColor = Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Select Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (state.enabledModels.isEmpty()) {
                        Text(
                            "No models enabled. Go to AI Providers, open a provider, and enable models.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
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
                                        text = state.selectedModel?.displayName
                                            ?: state.selectedModel?.modelId
                                            ?: "Select a model",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "Tap to choose model",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                            }
                        }
                    }
                }
            }

            // Model Selection Dialog
            if (showProviderSelector) {
                GlassAlertDialog(
                    onDismissRequest = { showProviderSelector = false },
                    title = { Text("Select Model") },
                    text = {
                        val modelsByProvider = remember(state.enabledModels) {
                            state.enabledModels.groupBy { it.providerAccountId }
                        }
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            modelsByProvider.forEach { (providerId, models) ->
                                val provider = state.providerAccounts.find { it.id == providerId }
                                val providerName = provider?.name ?: "Unknown Provider"

                                Text(
                                    text = providerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp)
                                )

                                models.forEach { model ->
                                    val displayText = model.displayName ?: model.modelId
                                    val isSelected = state.selectedModel?.id == model.id
                                    Surface(
                                        onClick = {
                                            viewModel.onProviderSelected(model)
                                            showProviderSelector = false
                                        },
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(displayText, style = MaterialTheme.typography.bodyLarge)
                                            if (isSelected) {
                                                Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showProviderSelector = false }) { Text("Cancel") }
                    }
                )
            }

            // Generate Button
            Button(
                onClick = { viewModel.generateRegex() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.notificationText.isNotBlank() && !state.isGenerating && state.selectedModel != null
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Rule (AI)")
                }
            }

            // Error Message
            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = state.errorMessage!!,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Success Message
            if (state.successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = state.successMessage!!,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
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
                            HorizontalDivider()

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

                        HorizontalDivider()

                        // Save Section
                        Text(
                            text = "Save Pattern",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (state.notificationTitle.isNotBlank()) {
                            Text(
                                text = "Pattern will be keyed by (app × notification title) for precise matching",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

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
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
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
                                        Icons.Rounded.Info,
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
                                                    "__ALL_WHITELISTED__" -> "All whitelisted apps"
                                                    "" -> "Select whitelisted app"
                                                    else -> state.availableApps
                                                        .firstOrNull { it.packageName == state.selectedAppPackage }
                                                        ?.appName
                                                        ?: state.selectedAppPackage
                                                },
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            val subtitle = when (state.selectedAppPackage) {
                                                "__ALL_WHITELISTED__" -> "Applies to every enabled whitelisted app"
                                                "" -> "Choose one app or all whitelisted apps"
                                                else -> state.selectedAppPackage
                                            }
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
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
                                            viewModel.onTargetAppSelected("__ALL_WHITELISTED__")
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
                            Text("Transaction?")
                            Switch(
                                checked = state.isTransaction,
                                onCheckedChange = { viewModel.toggleIsTransaction() }
                            )
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
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add to Watchlist")
                            }
                        }

                        OutlinedButton(
                            onClick = onNavigateToNotificationPatterns,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Pattern, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Saved Patterns")
                        }
                    }
                }
            }
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

            CompositionLocalProvider(LocalLiquidState provides regexLiquidState) {
                SpendSenseTopBar(
                    title = "AI Regex Generator",
                    onNavigationClick = onNavigateBack,
                    navigationIcon = Icons.Rounded.ArrowBack
                )
            }
        } // outer Box
    } // Scaffold close
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
