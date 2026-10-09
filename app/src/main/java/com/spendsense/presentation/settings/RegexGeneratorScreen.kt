@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.data.local.Currencies
import com.spendsense.presentation.util.GlassAlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import com.spendsense.presentation.util.fadingEdge
import com.spendsense.presentation.util.glassEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

@Composable
fun RegexGeneratorScreen(
    viewModel: RegexGeneratorViewModel = hiltViewModel(),
    initialNotificationText: String? = null,
    initialNotificationTitle: String? = null,
    initialPackageName: String? = null,
    isFromInbox: Boolean = false,
    stalePatternId: Long? = null,
    onNavigateBack: () -> Unit = {},
    onNavigateToNotificationPatterns: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showTargetAppSelector by remember { mutableStateOf(false) }
    var showProviderSelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }

    var editedAmount by remember(state.extractedAmount) { mutableStateOf(state.extractedAmount ?: "") }
    var editedMerchant by remember(state.extractedMerchant) { mutableStateOf(state.extractedMerchant ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    // Pre-fill initial text, title, and target package if provided
    LaunchedEffect(initialNotificationText, initialNotificationTitle, initialPackageName, isFromInbox) {
        viewModel.setIsFromInbox(isFromInbox)
        if (initialNotificationText != null) {
            viewModel.updateNotificationText(initialNotificationText)
        }
        if (initialNotificationTitle != null) {
            viewModel.updateNotificationTitle(initialNotificationTitle)
        }
        if (!initialPackageName.isNullOrBlank()) {
            viewModel.onTargetAppSelected(initialPackageName)
        }
    }

    LaunchedEffect(stalePatternId) {
        if (stalePatternId != null) {
            viewModel.loadStalePattern(stalePatternId)
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerBottom = statusBarPadding + 74.dp
    val fadeHeight = 24.dp

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color(0xFFF8FAFC),
        focusedBorderColor = Color(0xFF0284C7),
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedLabelColor = Color(0xFF0284C7),
        unfocusedLabelColor = Color(0xFF475569),
        focusedPlaceholderColor = Color(0xFF94A3B8),
        unfocusedPlaceholderColor = Color(0xFF94A3B8)
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            val density = LocalDensity.current
            val headerBottomPx = with(density) { headerBottom.toPx() }

            // Background scrim: transparent at top wallpaper, smoothly fades directly from 0 to 100 into #F8FAFC
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF8FAFC)),
                            startY = 0f,
                            endY = headerBottomPx
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(
                        topFadeStart = headerBottom,
                        topFadeHeight = fadeHeight
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = headerBottom + fadeHeight + 4.dp,
                        bottom = 120.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Paste a banking notification below. The AI will classify it and generate a regex pattern.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Input Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )

                    OutlinedTextField(
                        value = state.notificationText,
                        onValueChange = { viewModel.updateNotificationText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("Paste the full notification body here...") },
                        label = { Text("Notification Body") },
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )

                    HorizontalDivider()

                    Text(
                        text = "Payment Source",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = state.paymentSource,
                        onValueChange = { viewModel.updatePaymentSource(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. X1234, 484804....3488 or account number") },
                        label = { Text("Payment Source Identifier") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors
                    )

                    Text("Payment Source Type", style = MaterialTheme.typography.titleSmall)

                    val paymentSourceTypes = listOf("Credit Card", "Debit Card", "Bank Account", "Wallet", "Manual")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentSourceTypes.forEach { type ->
                            FilterChip(
                                selected = state.paymentSourceType == type,
                                onClick = { viewModel.updatePaymentSourceType(type) },
                                label = {
                                    Text(
                                        text = type,
                                        fontWeight = if (state.paymentSourceType == type) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDCFCE7),
                                    selectedLabelColor = Color(0xFF16A34A),
                                    containerColor = Color(0xFFF8FAFC),
                                    labelColor = Color(0xFF475569)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = state.paymentSourceType == type,
                                    borderColor = Color(0xFFCBD5E1),
                                    selectedBorderColor = Color(0xFF16A34A)
                                )
                            )
                        }
                    }

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
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors,
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
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
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
                        Surface(
                            onClick = { showProviderSelector = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.selectedModel?.displayName
                                            ?: state.selectedModel?.modelId
                                            ?: "Select a model",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Tap to choose model",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFF0F172A))
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
                            modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state.notificationText.isNotBlank() && !state.isGenerating && state.selectedModel != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(12.dp)
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
                    Text("Generate Rule (AI)", fontWeight = FontWeight.SemiBold)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
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

                        OutlinedTextField(
                            value = displayPattern,
                            onValueChange = { newPattern ->
                                viewModel.updateManualPattern(newPattern)
                                viewModel.testManualPattern()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            placeholder = { Text("Regex pattern...") },
                            label = { Text("Pattern String") },
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors
                        )

                        if (state.extractedAmount != null && state.extractedMerchant != null) {
                            HorizontalDivider()

                            Text(
                                text = if (state.isFromInbox) "Transaction Details Preview" else "Test Results",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            if (state.isFromInbox) {
                                OutlinedTextField(
                                    value = editedAmount,
                                    onValueChange = { editedAmount = it },
                                    label = { Text("Transaction Amount") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = inputColors
                                )

                                OutlinedTextField(
                                    value = editedMerchant,
                                    onValueChange = { editedMerchant = it },
                                    label = { Text("Merchant / Payee") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = inputColors
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDatePicker = true }
                                        .shadow(1.dp, RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Transaction Date & Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = dateTimeFormatter.format(Date(state.transactionTimestamp)),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarToday,
                                            contentDescription = "Change Date and Time",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
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
                                Surface(
                                    onClick = { showTargetAppSelector = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
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
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            val subtitle = when (state.selectedAppPackage) {
                                                "__ALL_WHITELISTED__" -> "Applies to every enabled whitelisted app"
                                                "" -> "Choose one app or all whitelisted apps"
                                                else -> state.selectedAppPackage
                                            }
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color(0xFF0F172A))
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
                            Text("Expense Transaction?")
                            Switch(
                                checked = state.isTransaction,
                                onCheckedChange = { viewModel.toggleIsTransaction() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1),
                                    uncheckedBorderColor = Color.Transparent
                                )
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
                                onCheckedChange = { viewModel.toggleActive() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF0284C7),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1),
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }

                        if (state.isFromInbox && state.extractedAmount != null) {
                                Button(
                                    onClick = { viewModel.savePatternAndTransaction(editedMerchant, editedAmount) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = !state.isSaving && state.selectedAppPackage.isNotBlank() && state.availableApps.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFE2E8F0),
                                        disabledContentColor = Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
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
                                        Text("Save Pattern & Transaction", fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.savePattern() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = !state.isSaving && state.selectedAppPackage.isNotBlank() && state.availableApps.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Pattern Only", fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.savePattern() },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    enabled = !state.isSaving && state.selectedAppPackage.isNotBlank() && state.availableApps.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color(0xFFE2E8F0),
                                        disabledContentColor = Color(0xFF94A3B8)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
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
                                        Text("Add to Watchlist", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = onNavigateToNotificationPatterns,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Icon(Icons.Rounded.Pattern, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Saved Patterns", fontWeight = FontWeight.Medium)
                            }
                    }
                }
            } // if (state.generatedRegex != null)
            } // scrolling Column

            // Pinned Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarPadding + 10.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "AI Regex Generator",
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
                    Text(
                        text = "Generate regex patterns from notification text",
                        style = MaterialTheme.typography.bodySmall.copy(
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
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.transactionTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val currentCal = Calendar.getInstance().apply { timeInMillis = state.transactionTimestamp }
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                            }
                            viewModel.updateTransactionTimestamp(newCal.timeInMillis)
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentCal = Calendar.getInstance().apply { timeInMillis = state.transactionTimestamp }
        var hourInput by remember { mutableStateOf(currentCal.get(Calendar.HOUR_OF_DAY).toString()) }
        var minuteInput by remember { mutableStateOf(currentCal.get(Calendar.MINUTE).toString().padStart(2, '0')) }

        GlassAlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Edit Time") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select time (24h format)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.isEmpty() || (clean.toIntOrNull() in 0..23)) {
                                    hourInput = clean.take(2)
                                }
                            },
                            label = { Text("Hour") },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = minuteInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.isEmpty() || (clean.toIntOrNull() in 0..59)) {
                                    minuteInput = clean.take(2)
                                }
                            },
                            label = { Text("Min") },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hr = hourInput.toIntOrNull() ?: 12
                        val min = minuteInput.toIntOrNull() ?: 0
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = state.transactionTimestamp
                            set(Calendar.HOUR_OF_DAY, hr)
                            set(Calendar.MINUTE, min)
                        }
                        viewModel.updateTransactionTimestamp(newCal.timeInMillis)
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
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
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }
    }
}
