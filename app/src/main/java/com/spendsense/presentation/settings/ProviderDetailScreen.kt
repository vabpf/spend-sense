package com.spendsense.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import com.spendsense.presentation.util.fadingEdge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    accountId: Long,
    viewModel: ProviderDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerBottom = statusBarPadding + 74.dp
    val fadeHeight = 24.dp
    val density = LocalDensity.current
    val headerBottomPx = with(density) { headerBottom.toPx() }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFFF8FAFC),
        unfocusedContainerColor = Color(0xFFF8FAFC),
        focusedBorderColor = Color(0xFF0284C7),
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedLabelColor = Color(0xFF0284C7),
        unfocusedLabelColor = Color(0xFF64748B),
        focusedPlaceholderColor = Color(0xFF94A3B8),
        unfocusedPlaceholderColor = Color(0xFF94A3B8),
        cursorColor = Color(0xFF0284C7)
    )

    LaunchedEffect(accountId) {
        viewModel.load(accountId)
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
                    .padding(top = headerBottom + fadeHeight + 4.dp)
            ) {
                // Header
                Text(
                    text = "Available Models",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                )

                if (state.lastRefreshedAt > 0) {
                    Text(
                        text = "Last refresh: ${formatTimeAgo(state.lastRefreshedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
                    )
                }

            // Refresh + Key row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.refreshModels() },
                    enabled = !state.isRefreshing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(if (state.isRefreshing) "Refreshing..." else "Refresh", style = MaterialTheme.typography.labelMedium)
                }

                state.account?.let { account ->
                    val needsKey = !account.baseUrl.contains("opencode", ignoreCase = true) &&
                                   !account.baseUrl.contains("nvidia", ignoreCase = true) &&
                                   !account.baseUrl.contains("openrouter", ignoreCase = true)
                    if (needsKey) {
                        Surface(
                            onClick = { viewModel.showKeyDialog(true) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF475569))
                                Text(
                                    if (state.existingApiKeyPreview != null) "API Key: ${state.existingApiKeyPreview}"
                                    else "Set API Key",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }

            // Search
            val focusManager = LocalFocusManager.current
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search models...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = inputColors,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )

            // Error
            if (state.errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(state.errorMessage!!, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Model list
            if (state.models.isEmpty() && !state.isRefreshing) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No models yet", style = MaterialTheme.typography.titleMedium)
                        Text("Tap Refresh to fetch available models", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(state.models, key = { it.id }) { model ->
                        ModelItem(
                            model = model,
                            onClick = { viewModel.toggleModel(model) }
                        )
                    }
                }
            }
            } // Close Column

            // Pinned Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarPadding + 10.dp,
                        start = 12.dp,
                        end = 20.dp,
                        bottom = 10.dp
                    )
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = state.account?.name ?: "Provider",
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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Manage provider models and configuration",
                        style = MaterialTheme.typography.bodyMedium.copy(
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

    // API Key dialog
    if (state.showKeyDialog) {
        GlassAlertDialog(
            onDismissRequest = { viewModel.showKeyDialog(false) },
            title = { Text("API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the API key for ${state.account?.name ?: "this provider"} to fetch available models.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.existingApiKeyPreview != null) {
                        Text(
                            text = "Current key: ${state.existingApiKeyPreview}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = { viewModel.onApiKeyChange(it) },
                        label = { Text("API Key") },
                        placeholder = { Text("Enter API key") },
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveApiKey() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Refresh")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.showKeyDialog(false) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ModelItem(
    model: com.spendsense.data.local.entity.ProviderModelEntity,
    onClick: () -> Unit
) {
    val displayText = model.displayName ?: model.modelId

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (model.isEnabled) Color(0xFFF0FDF4) else Color.White,
        border = BorderStroke(1.dp, if (model.isEnabled) Color(0xFF86EFAC) else Color(0xFFF1F5F9)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (model.isEnabled) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color(0xFF0F172A)
                )
            }
            Icon(
                imageVector = if (model.isEnabled) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (model.isEnabled) "Selected" else "Not selected",
                tint = if (model.isEnabled) Color(0xFF16A34A) else Color(0xFF94A3B8)
            )
        }
    }
}
