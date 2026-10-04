package com.spendsense.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.util.GlassAlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import com.spendsense.presentation.util.fadingEdge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiProvidersScreen(
    viewModel: AiProvidersViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEnd = statusBarPadding + 98.dp
    val density = LocalDensity.current
    val fadeEndPx = with(density) { fadeEnd.toPx() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .offset(y = (-24).dp)
                    .size(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.20f)
                    )
                    .background(
                        Brush.linearGradient(listOf(CyberBlue, Color(0xFF00C6FF))),
                        shape = CircleShape
                    )
                    .clickable { viewModel.toggleAddingProvider(true) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add Provider",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
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
                            endY = fadeEndPx
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(
                        topFadeStart = 0.dp,
                        topFadeHeight = fadeEnd
                    ),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = fadeEnd + 6.dp, 
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        text = "AI Providers",
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
                        text = "Configure models and API credentials",
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
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = display.account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = display.account.baseUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
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
                    shape = RoundedCornerShape(8.dp),
                    color = if (display.isConfigured) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = if (display.isConfigured) "Configured" else "Not configured",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (display.isConfigured) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }

                if (display.totalModelCount > 0) {
                    val text = "${display.enabledModelCount} of ${display.totalModelCount} selected"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEDE9FE)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }

                if (display.lastRefreshedAt > 0) {
                    val ago = formatTimeAgo(display.lastRefreshedAt)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = ago,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
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

fun formatTimeAgo(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
