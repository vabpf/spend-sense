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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.R
import com.spendsense.data.local.entity.AiProviderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProvidersScreen(
    viewModel: AiProvidersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_providers_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddingProvider(true) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_provider))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.providers) { provider ->
                AiProviderItem(
                    provider = provider,
                    hasKey = state.providerKeyStatuses[provider.id] ?: false,
                    onDelete = { viewModel.deleteProvider(provider) },
                    onEditKey = { viewModel.onEditProvider(provider) }
                )
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
            onApiKeyChange = viewModel::onApiKeyChange,
            onDismiss = { viewModel.onEditProvider(null) },
            onSave = { viewModel.updateApiKeyForProvider(state.editingProvider!!, state.apiKey) }
        )
    }
}

@Composable
fun AiProviderItem(
    provider: AiProviderEntity,
    hasKey: Boolean,
    onDelete: () -> Unit,
    onEditKey: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, style = MaterialTheme.typography.titleMedium)
                Text(provider.defaultModel, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.job_type, provider.jobType), style = MaterialTheme.typography.labelSmall)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val isFree = provider.baseUrl.contains("opencode", ignoreCase = true)
                val statusText = when {
                    hasKey -> stringResource(R.string.key_set)
                    isFree -> stringResource(R.string.free_provider)
                    else -> stringResource(R.string.no_key)
                }
                val statusColor = when {
                    hasKey -> MaterialTheme.colorScheme.primary
                    isFree -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
            Row {
                IconButton(onClick = onEditKey) {
                    Icon(Icons.Default.VpnKey, contentDescription = stringResource(R.string.edit_key))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EditKeyDialog(
    provider: AiProviderEntity,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_api_key_for, provider.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.enter_new_api_key), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text(stringResource(R.string.api_key)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(R.string.update)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
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
        title = { Text(stringResource(R.string.add_ai_provider)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.name, onValueChange = onNameChange, label = { Text(stringResource(R.string.name_hint)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.baseUrl, onValueChange = onBaseUrlChange, label = { Text(stringResource(R.string.base_url)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.apiKey, onValueChange = onApiKeyChange, label = { Text(stringResource(R.string.api_key)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.defaultModel, onValueChange = onModelChange, label = { Text(stringResource(R.string.default_model)) }, modifier = Modifier.fillMaxWidth())
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
