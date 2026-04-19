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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                title = { Text("AI Providers") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddingProvider(true) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Provider")
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
                    onDelete = { viewModel.deleteProvider(provider) }
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
}

@Composable
fun AiProviderItem(
    provider: AiProviderEntity,
    onDelete: () -> Unit
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
                Text("Job: ${provider.jobType}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
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
