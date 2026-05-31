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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.Currencies
import com.spendsense.data.local.entity.NotificationPatternEntity
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.util.GlassAlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import com.spendsense.presentation.util.LocalLiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPatternsScreen(
    viewModel: NotificationPatternsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val patterns by viewModel.patterns.collectAsState()
    val appNameMap by viewModel.appNameMap.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    val availableApps by viewModel.availableApps.collectAsState()
    val newTitle by viewModel.newTitle.collectAsState()
    val newRegex by viewModel.newRegex.collectAsState()
    val newIsTransaction by viewModel.newIsTransaction.collectAsState()
    val newCurrencyCode by viewModel.newCurrencyCode.collectAsState()
    val selectedAppIndex by viewModel.selectedAppIndex.collectAsState()
    val editTitle by viewModel.editTitle.collectAsState()
    val editRegex by viewModel.editRegex.collectAsState()
    val editIsTransaction by viewModel.editIsTransaction.collectAsState()
    val editCurrencyCode by viewModel.editCurrencyCode.collectAsState()
    val editSelectedAppIndex by viewModel.editSelectedAppIndex.collectAsState()

    val patternsLiquidState = rememberLiquidState()
 
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            CompositionLocalProvider(LocalLiquidState provides patternsLiquidState) {
                Box(
                    modifier = Modifier
                        .offset(y = (-20).dp)
                        .size(56.dp)
                        .glassEffect(
                            shape = FloatingActionButtonDefaults.shape,
                            containerColor = GlassSurface.copy(alpha = 0.15f),
                            borderAlpha = 0.25f
                        )
                        .border(
                            width = 1.dp,
                            color = CyberBlue,
                            shape = FloatingActionButtonDefaults.shape
                        )
                        .clickable { viewModel.showAddDialog() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add pattern",
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(patternsLiquidState)
            ) {
                Image(
                    painter = painterResource(id = com.spendsense.R.drawable.bg_pexel),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )
 
                if (patterns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(
                                start = 32.dp,
                                end = 32.dp,
                                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp,
                                bottom = 0.dp
                            )
                        ) {
                            Icon(
                                Icons.Rounded.Pattern,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                "No patterns yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Patterns are created automatically when you save from the Regex Generator, or you can add one manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, 
                            end = 16.dp, 
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp, 
                            bottom = 120.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(patterns, key = { it.id }) { pattern ->
                            PatternItem(
                                pattern = pattern,
                                appNameMap = appNameMap,
                                onEdit = { viewModel.startEdit(pattern) },
                                onDelete = { viewModel.deletePattern(pattern.id) },
                                onToggleTransaction = {
                                    viewModel.updatePattern(pattern.id, pattern.regex, !pattern.isTransaction)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }
 
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background,
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            0.75f to MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                            1.0f to Color.Transparent
                        )
                    )
                    .align(Alignment.TopCenter)
            )
 
            CompositionLocalProvider(LocalLiquidState provides patternsLiquidState) {
                SpendSenseTopBar(
                    title = "Notification Patterns",
                    onNavigationClick = onNavigateBack,
                    navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack
                )
            }
        }
    }

    if (showAddDialog) {
        AddPatternDialog(
            availableApps = availableApps,
            selectedAppIndex = selectedAppIndex,
            title = newTitle,
            regex = newRegex,
            isTransaction = newIsTransaction,
            currencyCode = newCurrencyCode,
            onDismiss = { viewModel.hideAddDialog() },
            onTitleChange = { viewModel.updateNewTitle(it) },
            onRegexChange = { viewModel.updateNewRegex(it) },
            onIsTransactionChange = { viewModel.updateNewIsTransaction(it) },
            onCurrencyCodeChange = { viewModel.updateNewCurrencyCode(it) },
            onAppSelected = { viewModel.selectApp(it) },
            onSave = { viewModel.saveNewPattern() }
        )
    }

    if (showEditDialog) {
        EditPatternDialog(
            availableApps = availableApps,
            selectedAppIndex = editSelectedAppIndex,
            title = editTitle,
            regex = editRegex,
            isTransaction = editIsTransaction,
            currencyCode = editCurrencyCode,
            onDismiss = { viewModel.hideEditDialog() },
            onTitleChange = { viewModel.updateEditTitle(it) },
            onRegexChange = { viewModel.updateEditRegex(it) },
            onIsTransactionChange = { viewModel.updateEditIsTransaction(it) },
            onCurrencyCodeChange = { viewModel.updateEditCurrencyCode(it) },
            onAppSelected = { viewModel.selectEditApp(it) },
            onSave = { viewModel.saveEditedPattern() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPatternDialog(
    availableApps: List<RegexTargetApp>,
    selectedAppIndex: Int,
    title: String,
    regex: String,
    isTransaction: Boolean,
    currencyCode: String,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onRegexChange: (String) -> Unit,
    onIsTransactionChange: (Boolean) -> Unit,
    onCurrencyCodeChange: (String) -> Unit,
    onAppSelected: (Int) -> Unit,
    onSave: () -> Unit
) {
    var showAppSelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("New Pattern")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // App selector
                Box {
                    OutlinedCard(
                        onClick = { showAppSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedAppIndex in availableApps.indices)
                                        availableApps[selectedAppIndex].appName
                                    else "Select app",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (selectedAppIndex in availableApps.indices)
                                        availableApps[selectedAppIndex].packageName
                                    else "Choose a whitelisted app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showAppSelector,
                        onDismissRequest = { showAppSelector = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        availableApps.forEachIndexed { index, app ->
                            val isSelected = index == selectedAppIndex
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(app.appName)
                                            Text(
                                                app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onAppSelected(index)
                                    showAppSelector = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Notification Title") },
                    placeholder = { Text("e.g. UPI payment received") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = regex,
                    onValueChange = onRegexChange,
                    label = { Text("Regex Pattern (optional)") },
                    placeholder = { Text("Leave blank to match all notifications with this title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    val selectedCurrency = Currencies.find(currencyCode)
                    OutlinedCard(
                        onClick = { showCurrencySelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Currency", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.name}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showCurrencySelector,
                        onDismissRequest = { showCurrencySelector = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Currencies.SUPPORTED.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text("${cur.symbol} ${cur.code} — ${cur.name}") },
                                onClick = {
                                    onCurrencyCodeChange(cur.code)
                                    showCurrencySelector = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaction?")
                    Switch(checked = isTransaction, onCheckedChange = onIsTransactionChange)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank() && selectedAppIndex >= 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPatternDialog(
    availableApps: List<RegexTargetApp>,
    selectedAppIndex: Int,
    title: String,
    regex: String,
    isTransaction: Boolean,
    currencyCode: String,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onRegexChange: (String) -> Unit,
    onIsTransactionChange: (Boolean) -> Unit,
    onCurrencyCodeChange: (String) -> Unit,
    onAppSelected: (Int) -> Unit,
    onSave: () -> Unit
) {
    var showAppSelector by remember { mutableStateOf(false) }
    var showCurrencySelector by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Text("Edit Pattern")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box {
                    OutlinedCard(
                        onClick = { showAppSelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedAppIndex in availableApps.indices)
                                        availableApps[selectedAppIndex].appName
                                    else "Select app",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (selectedAppIndex in availableApps.indices)
                                        availableApps[selectedAppIndex].packageName
                                    else "Choose a whitelisted app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showAppSelector,
                        onDismissRequest = { showAppSelector = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        availableApps.forEachIndexed { index, app ->
                            val isSelected = index == selectedAppIndex
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(app.appName)
                                            Text(
                                                app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                onClick = {
                                    onAppSelected(index)
                                    showAppSelector = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Notification Title") },
                    placeholder = { Text("e.g. UPI payment received") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = regex,
                    onValueChange = onRegexChange,
                    label = { Text("Regex Pattern (optional)") },
                    placeholder = { Text("Leave blank to match all notifications with this title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    val selectedCurrency = Currencies.find(currencyCode)
                    OutlinedCard(
                        onClick = { showCurrencySelector = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Currency", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${selectedCurrency.symbol} ${selectedCurrency.code} — ${selectedCurrency.name}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showCurrencySelector,
                        onDismissRequest = { showCurrencySelector = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Currencies.SUPPORTED.forEach { cur ->
                            DropdownMenuItem(
                                text = { Text("${cur.symbol} ${cur.code} — ${cur.name}") },
                                onClick = {
                                    onCurrencyCodeChange(cur.code)
                                    showCurrencySelector = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Transaction?")
                    Switch(checked = isTransaction, onCheckedChange = onIsTransactionChange)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank() && selectedAppIndex >= 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PatternItem(
    pattern: NotificationPatternEntity,
    appNameMap: Map<String, String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTransaction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.2f
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
                    Text(
                        text = if (pattern.packageName == "__ALL_WHITELISTED__") {
                            "All Whitelisted Apps"
                        } else {
                            appNameMap[pattern.packageName]
                                ?: pattern.packageName.split(".").lastOrNull()
                                ?: pattern.packageName
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pattern.notificationTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (pattern.isTransaction) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        }
                    ) {
                        Text(
                            text = if (pattern.isTransaction) "Transaction" else "Skip",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (pattern.matchCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${pattern.matchCount}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (pattern.regex != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = pattern.regex,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleTransaction) {
                    Icon(
                        if (pattern.isTransaction) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (pattern.isTransaction) "Mark as non-transaction" else "Mark as transaction")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
