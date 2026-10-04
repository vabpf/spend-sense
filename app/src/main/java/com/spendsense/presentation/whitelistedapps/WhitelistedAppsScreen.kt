package com.spendsense.presentation.whitelistedapps

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.shimmer
import com.spendsense.domain.repository.AppItem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import com.spendsense.presentation.util.fadingEdge

@Composable
private fun rememberDrawablePainter(drawable: Drawable): Painter {
    return remember(drawable) {
        BitmapPainter(drawable.toBitmap().asImageBitmap())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistedAppsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WhitelistedAppsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEnd = statusBarPadding + 98.dp
    val density = LocalDensity.current
    val fadeEndPx = with(density) { fadeEnd.toPx() }

    val suggestedPackageNames = remember(state.suggestedApps) {
        state.suggestedApps.map { it.packageName }.toSet()
    }
    val nonSuggestedFilteredApps = remember(state.filteredApps, suggestedPackageNames) {
        state.filteredApps.filterNot { it.packageName in suggestedPackageNames }
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
                            endY = fadeEndPx
                        )
                    )
            )

            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = fadeEnd + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Box(modifier = Modifier.width(240.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(4.dp)).shimmer())
                    Spacer(modifier = Modifier.height(16.dp))
                    repeat(5) {
                        WhitelistedAppSkeletonItem()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .fadingEdge(
                            topFadeStart = 0.dp,
                            topFadeHeight = fadeEnd
                        ),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = fadeEnd + 6.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Select the apps you want SpendSense to monitor for transaction notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search apps") },
                            placeholder = { Text("Search by app name or package") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF00D4FF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    if (state.suggestedApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "Suggested Vietnam Banking Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(state.suggestedApps, key = { "sug_${it.packageName}" }) { app ->
                            WhitelistedAppCard(
                                app = app,
                                onToggle = { isChecked ->
                                    viewModel.toggleApp(app, isChecked)
                                }
                            )
                        }
                    }

                    if (nonSuggestedFilteredApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "All Installed Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                        items(nonSuggestedFilteredApps, key = { it.packageName }) { app ->
                            WhitelistedAppCard(
                                app = app,
                                onToggle = { isChecked ->
                                    viewModel.toggleApp(app, isChecked)
                                }
                            )
                        }
                    }
                }
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
                        text = "Whitelisted Apps",
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
                        text = "Manage apps monitored for banking alerts",
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
}

@Composable
fun WhitelistedAppSkeletonItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded App Icon placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmer()
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Two-line details placeholder
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Toggle/Switch placeholder
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmer()
        )
    }
}

@Composable
fun WhitelistedAppCard(
    app: AppItem,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                Image(
                    painter = rememberDrawablePainter(app.icon),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            Switch(
                checked = app.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
