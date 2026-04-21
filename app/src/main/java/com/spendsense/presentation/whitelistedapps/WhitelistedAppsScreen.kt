package com.spendsense.presentation.whitelistedapps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistedAppsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WhitelistedAppsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val suggestedPackageNames = remember(state.suggestedApps) {
        state.suggestedApps.map { it.packageName }.toSet()
    }
    val nonSuggestedFilteredApps = remember(state.filteredApps, suggestedPackageNames) {
        state.filteredApps.filterNot { it.packageName in suggestedPackageNames }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = { Text("Whitelisted Apps") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GlassSurface
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Select the apps you want SpendSense to monitor for transaction notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search apps") },
                        placeholder = { Text("Search by app name or package") },
                        singleLine = true
                    )
                }

                if (state.suggestedApps.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggested Vietnam Banking Apps",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.suggestedApps) { app ->
                        AppListItem(
                            app = app,
                            onToggle = { isEnabled -> viewModel.toggleApp(app, isEnabled) }
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                item {
                    Text(
                        text = "All Installed Apps",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(nonSuggestedFilteredApps) { app ->
                    AppListItem(
                        app = app,
                        onToggle = { isEnabled -> viewModel.toggleApp(app, isEnabled) }
                    )
                }

                if (nonSuggestedFilteredApps.isEmpty()) {
                    item {
                        Text(
                            text = "No apps match your search.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: AppItem,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GlassSurface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = app.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
