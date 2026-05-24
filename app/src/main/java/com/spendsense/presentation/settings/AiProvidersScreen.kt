package com.spendsense.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProvidersScreen(
    viewModel: AiProvidersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            SpendSenseTopBar(
                title = "AI Providers",
                onNavigationClick = onNavigateBack,
                navigationIcon = Icons.Rounded.ArrowBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.offset(y = (-24).dp),
                onClick = { viewModel.toggleAddingProvider(true) },
                containerColor = GlassSurface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Provider")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.accounts, key = { it.account.id }) { display ->
                AccountCard(
                    display = display,
                    onOpen = { onNavigateToDetail(display.account.id) },
                    onDelete = {
                        if (!display.account.isPreset) {
                            viewModel.deleteAccount(display.account)
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (state.isAddingProvider) {
        AddProviderDialog(
            state = state,
            onNameChange = viewModel::onNameChange,
            onBaseUrlChange = viewModel::onBaseUrlChange,
            onApiKeyChange = viewModel::onApiKeyChange,
            onDismiss = { viewModel.toggleAddingProvider(false) },
            onSave = viewModel::saveProvider
        )
    }
}

@Composable
private fun AccountCard(
    display: ProviderAccountDisplay,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.24f
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(display.account.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        display.account.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!display.account.isPreset) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (display.isConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                           else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (display.isConfigured) "Configured" else "Not configured",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (display.totalModelCount > 0) {
                    val text = "${display.enabledModelCount} of ${display.totalModelCount} selected"
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (display.lastRefreshedAt > 0) {
                    val ago = formatTimeAgo(display.lastRefreshedAt)
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = ago,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProviderDialog(
    state: AiProvidersState,
    onNameChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add AI Provider") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = state.name, onValueChange = onNameChange, label = { Text("Name (e.g. OpenRouter)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.baseUrl, onValueChange = onBaseUrlChange, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.apiKey, onValueChange = onApiKeyChange, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                if (state.errorMessage != null) {
                    Text(state.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTimeAgo(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
