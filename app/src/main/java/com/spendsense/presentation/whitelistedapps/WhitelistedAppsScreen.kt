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
            SpendSenseTopBar(
                title = "Whitelisted Apps",
                onNavigationClick = onNavigateBack,
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Shimmer Placeholders for explanation text and search bar
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
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 0.dp),
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

                        items(
                            items = state.suggestedApps,
                            key = { "suggested_${it.packageName}" }
                        ) { app ->
                            WhitelistedAppCard(
                                app = app,
                                onToggle = { isChecked -> viewModel.toggleApp(app, isChecked) }
                            )
                        }
                    }

                    if (nonSuggestedFilteredApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "Installed Applications",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(
                            items = nonSuggestedFilteredApps,
                            key = { "all_${it.packageName}" }
                        ) { app ->
                            WhitelistedAppCard(
                                app = app,
                                onToggle = { isChecked -> viewModel.toggleApp(app, isChecked) }
                            )
                        }
                    }
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
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.24f
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
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
