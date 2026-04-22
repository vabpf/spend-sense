package com.spendsense.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.entity.AiProviderEntity
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.SpendSenseTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProvidersScreen(
    viewModel: AiProvidersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            SpendSenseTopBar(
                title = "AI Providers",
                onNavigationClick = onNavigateBack,
                navigationIcon = Icons.Default.ArrowBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.toggleAddingProvider(true) },
                containerColor = GlassSurface
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Provider")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(modifier = Modifier.height(72.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.providerGroups) { group ->
                    ProviderGroupItem(
                        group = group,
                        configuredCount = group.models.count { state.providerKeyStatuses[it.id] == true },
                        onOpen = {
                            group.models.firstOrNull()?.let { viewModel.onEditProvider(it) }
                        },
                        canDelete = !group.isPreset,
                        onDelete = { viewModel.deleteProviderGroup(group) }
                    )
                }
            }
        }
    }

    if (state.isAddingProvider) {
        AddProviderDialog(
            state = state,
            onNameChange = viewModel::onNameChange,
            onBaseUrlChange = viewModel::onBaseUrlChange,
            onApiKeyChange = viewModel::onApiKeyChange,
            onModelChange = viewModel::onModelChange,
            onDismiss = { viewModel.toggleAddingProvider(false) },
            onSave = viewModel::saveProvider
        )
    }

    if (state.showKeyDialog && state.editingProvider != null) {
        EditKeyDialog(
            provider = state.editingProvider!!,
            apiKey = state.apiKey,
            existingApiKeyPreview = state.existingApiKeyPreview,
            isFreeProvider = state.editingProvider!!.baseUrl.contains("opencode", ignoreCase = true),
            onApiKeyChange = viewModel::onApiKeyChange,
            onDismiss = { viewModel.onEditProvider(null) },
            onSave = { viewModel.updateApiKeyForProvider(state.editingProvider!!, state.apiKey) }
        )
    }
}

@Composable
fun ProviderGroupItem(
    group: AiProviderGroup,
    configuredCount: Int,
    onOpen: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = GlassSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium)
                Text(group.baseUrl, style = MaterialTheme.typography.bodySmall)
                Text("${group.models.size} model(s), $configuredCount configured", style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(4.dp))

                val statusText = when {
                    configuredCount == group.models.size && group.models.isNotEmpty() -> "Configured"
                    configuredCount > 0 -> "Partially configured"
                    else -> "Not configured"
                }
                val statusColor = when {
                    configuredCount == group.models.size && group.models.isNotEmpty() -> MaterialTheme.colorScheme.primary
                    configuredCount > 0 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
            Row {
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EditKeyDialog(
    provider: AiProviderEntity,
    apiKey: String,
    existingApiKeyPreview: String?,
    isFreeProvider: Boolean,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = { Text(provider.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set one API key for this provider. It will be used for all models in this provider.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!existingApiKeyPreview.isNullOrBlank()) {
                    Text(
                        text = "Current API key: $existingApiKeyPreview",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text(if (isFreeProvider) "API Key (optional)" else "API Key") },
                    placeholder = {
                        Text(
                            if (existingApiKeyPreview.isNullOrBlank()) "Enter API key" else "Leave blank to keep current key"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (apiKey.isBlank() && existingApiKeyPreview.isNullOrBlank() && !isFreeProvider) {
                    Text(
                        text = "This provider is not configured yet. Enter an API key to enable it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = isFreeProvider || apiKey.isNotBlank() || !existingApiKeyPreview.isNullOrBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddProviderDialog(
    state: AiProvidersState,
    onNameChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Add AI Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.name, onValueChange = onNameChange, label = { Text("Name (e.g. OpenRouter)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.baseUrl, onValueChange = onBaseUrlChange, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.apiKey, onValueChange = onApiKeyChange, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.defaultModel, onValueChange = onModelChange, label = { Text("Default Model") }, modifier = Modifier.fillMaxWidth())
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
