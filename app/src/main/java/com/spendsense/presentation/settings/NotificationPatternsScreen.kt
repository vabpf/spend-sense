package com.spendsense.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.spendsense.presentation.util.fadingEdge

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
    val newPaymentSource by viewModel.newPaymentSource.collectAsState()
    val newPaymentSourceType by viewModel.newPaymentSourceType.collectAsState()
    
    val editTitle by viewModel.editTitle.collectAsState()
    val editRegex by viewModel.editRegex.collectAsState()
    val editIsTransaction by viewModel.editIsTransaction.collectAsState()
    val editCurrencyCode by viewModel.editCurrencyCode.collectAsState()
    val editSelectedAppIndex by viewModel.editSelectedAppIndex.collectAsState()
    val editPaymentSource by viewModel.editPaymentSource.collectAsState()
    val editPaymentSourceType by viewModel.editPaymentSourceType.collectAsState()
    val editPackageName by viewModel.editPackageName.collectAsState()

    val selectedPatternForHistory by viewModel.selectedPatternForHistory.collectAsState()
    val matchedNotifications by viewModel.matchedNotifications.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()

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
                    .offset(y = (-20).dp)
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
                    .clickable { viewModel.showAddDialog() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add pattern",
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

            if (patterns.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = fadeEnd + 24.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Pattern,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF00D4FF).copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No patterns yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Patterns are created automatically when you save from the Regex Generator, or you can add one manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
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
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = fadeEnd + 6.dp, 
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(patterns, key = { it.id }) { pattern ->
                        PatternItem(
                            pattern = pattern,
                            appNameMap = appNameMap,
                            onEdit = { viewModel.startEdit(pattern) },
                            onDelete = { viewModel.deletePattern(pattern.id) },
                            onToggleTransaction = {
                                viewModel.updatePattern(pattern.id, pattern.regex, !pattern.isTransaction)
                            },
                            onShowHistory = { viewModel.showHistoryForPattern(pattern) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
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
                        text = "Notification Patterns",
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
                        text = "Manage (app × title) pattern rules",
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

    if (showAddDialog) {
        AddPatternDialog(
            availableApps = availableApps,
            selectedAppIndex = selectedAppIndex,
            title = newTitle,
            regex = newRegex,
            isTransaction = newIsTransaction,
            currencyCode = newCurrencyCode,
            paymentSource = newPaymentSource,
            paymentSourceType = newPaymentSourceType,
            onDismiss = { viewModel.hideAddDialog() },
            onTitleChange = { viewModel.updateNewTitle(it) },
            onRegexChange = { viewModel.updateNewRegex(it) },
            onIsTransactionChange = { viewModel.updateNewIsTransaction(it) },
            onCurrencyCodeChange = { viewModel.updateNewCurrencyCode(it) },
            onPaymentSourceChange = { viewModel.updateNewPaymentSource(it) },
            onPaymentSourceTypeChange = { viewModel.updateNewPaymentSourceType(it) },
            onAppSelected = { viewModel.selectApp(it) },
            onSave = { viewModel.saveNewPattern() }
        )
    }

    if (showEditDialog) {
        EditPatternDialog(
            availableApps = availableApps,
            selectedAppIndex = editSelectedAppIndex,
            packageName = editPackageName,
            title = editTitle,
            regex = editRegex,
            isTransaction = editIsTransaction,
            currencyCode = editCurrencyCode,
            paymentSource = editPaymentSource,
            paymentSourceType = editPaymentSourceType,
            onDismiss = { viewModel.hideEditDialog() },
            onTitleChange = { viewModel.updateEditTitle(it) },
            onRegexChange = { viewModel.updateEditRegex(it) },
            onIsTransactionChange = { viewModel.updateEditIsTransaction(it) },
            onCurrencyCodeChange = { viewModel.updateEditCurrencyCode(it) },
            onPaymentSourceChange = { viewModel.updateEditPaymentSource(it) },
            onPaymentSourceTypeChange = { viewModel.updateEditPaymentSourceType(it) },
            onAppSelected = { viewModel.selectEditApp(it) },
            onSave = { viewModel.saveEditedPattern() }
        )
    }

    if (selectedPatternForHistory != null) {
        val pattern = selectedPatternForHistory!!
        GlassAlertDialog(
            onDismissRequest = { viewModel.showHistoryForPattern(null) },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Notification History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pattern: ${pattern.notificationTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlue
                    )
                    if (pattern.regex != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = pattern.regex,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            text = {
                if (isLoadingHistory) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CyberBlue)
                    }
                } else if (matchedNotifications.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No processed notifications found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Matched processed notifications will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        matchedNotifications.forEach { notif ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val formattedTime = remember(notif.timestamp) {
                                        val cal = java.util.Calendar.getInstance().apply {
                                            timeInMillis = notif.timestamp
                                        }
                                        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        format.format(cal.time)
                                    }
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = notif.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showHistoryForPattern(null) }) {
                    Text("Close", color = CyberBlue)
                }
            }
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
    paymentSource: String,
    paymentSourceType: String,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onRegexChange: (String) -> Unit,
    onIsTransactionChange: (Boolean) -> Unit,
    onCurrencyCodeChange: (String) -> Unit,
    onPaymentSourceChange: (String) -> Unit,
    onPaymentSourceTypeChange: (String) -> Unit,
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

                if (isTransaction) {
                    OutlinedTextField(
                        value = paymentSource,
                        onValueChange = onPaymentSourceChange,
                        label = { Text("Payment Source Identifier") },
                        placeholder = { Text("e.g. x1234 or account number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Source Type", style = MaterialTheme.typography.titleSmall)

                    val paymentSourceTypes = listOf("Credit Card", "Debit Card", "Bank Account", "Wallet", "Manual")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentSourceTypes.forEach { type ->
                            FilterChip(
                                selected = paymentSourceType == type,
                                onClick = { onPaymentSourceTypeChange(type) },
                                label = { Text(type) }
                            )
                        }
                    }
                }

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
                    Text("Expense Transaction?")
                    Switch(checked = isTransaction, onCheckedChange = onIsTransactionChange)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank() && selectedAppIndex >= 0 && (!isTransaction || paymentSource.isNotBlank())
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
    packageName: String,
    title: String,
    regex: String,
    isTransaction: Boolean,
    currencyCode: String,
    paymentSource: String,
    paymentSourceType: String,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onRegexChange: (String) -> Unit,
    onIsTransactionChange: (Boolean) -> Unit,
    onCurrencyCodeChange: (String) -> Unit,
    onPaymentSourceChange: (String) -> Unit,
    onPaymentSourceTypeChange: (String) -> Unit,
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
                                    else {
                                        if (packageName == "__ALL_WHITELISTED__") "All Whitelisted Apps"
                                        else packageName.split(".").lastOrNull()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() } ?: packageName
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = if (selectedAppIndex in availableApps.indices)
                                        availableApps[selectedAppIndex].packageName
                                    else packageName,
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

                if (isTransaction) {
                    OutlinedTextField(
                        value = paymentSource,
                        onValueChange = onPaymentSourceChange,
                        label = { Text("Payment Source Identifier") },
                        placeholder = { Text("e.g. x1234 or account number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Source Type", style = MaterialTheme.typography.titleSmall)

                    val paymentSourceTypes = listOf("Credit Card", "Debit Card", "Bank Account", "Wallet", "Manual")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentSourceTypes.forEach { type ->
                            FilterChip(
                                selected = paymentSourceType == type,
                                onClick = { onPaymentSourceTypeChange(type) },
                                label = { Text(type) }
                            )
                        }
                    }
                }

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
                    Text("Expense Transaction?")
                    Switch(checked = isTransaction, onCheckedChange = onIsTransactionChange)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank() && packageName.isNotBlank() && (!isTransaction || paymentSource.isNotBlank())
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
    onToggleTransaction: () -> Unit,
    onShowHistory: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowHistory() }
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
                        text = if (pattern.packageName == "__ALL_WHITELISTED__") {
                            "All Whitelisted Apps"
                        } else {
                            appNameMap[pattern.packageName]
                                ?: pattern.packageName.split(".").lastOrNull()
                                ?: pattern.packageName
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = pattern.notificationTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (pattern.isTransaction) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (pattern.isTransaction) "Expense" else "Skip",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pattern.isTransaction) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                    if (pattern.matchCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${pattern.matchCount}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            if (pattern.regex != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Text(
                        text = pattern.regex,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF334155),
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
                    Text(if (pattern.isTransaction) "Mark as skip" else "Mark as expense")
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
