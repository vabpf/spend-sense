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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexGeneratorScreen(
    viewModel: RegexGeneratorViewModel = hiltViewModel(),
    initialNotificationText: String? = null,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showProviderSelector by remember { mutableStateOf(false) }

    // Pre-fill initial text if provided
    LaunchedEffect(initialNotificationText) {
        if (initialNotificationText != null) {
            viewModel.updateNotificationText(initialNotificationText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Regex Generator") },
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
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Provider",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (state.providers.isEmpty()) {
                        Text(
                            "No AI providers configured. Please add one in Settings.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Box {
                            OutlinedCard(
                                onClick = { showProviderSelector = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            state.selectedProvider?.name ?: "Select Provider",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        state.selectedProvider?.let {
                                            Text(it.defaultModel, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showProviderSelector,
                                onDismissRequest = { showProviderSelector = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                state.providers.forEach { provider ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(provider.name)
                                                Text(provider.defaultModel, style = MaterialTheme.typography.labelSmall)
                                            }
                                        },
                                        onClick = {
                                            viewModel.onProviderSelected(provider)
                                            showProviderSelector = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input Section
            Card {
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
                        containerColor = MaterialTheme.colorScheme.errorContainer
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                Card {
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
                            shape = RoundedCornerShape(8.dp)
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

                        OutlinedTextField(
                            value = state.packageName,
                            onValueChange = { viewModel.updatePackageName(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("App Package Name") },
                            placeholder = { Text("e.g., com.example.bankapp") },
                            leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null) }
                        )

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
                            enabled = !state.isSaving && state.packageName.isNotBlank()
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
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
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
