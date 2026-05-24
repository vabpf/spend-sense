@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.ReviewTransactionData
import com.spendsense.domain.model.Transaction
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.parseColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private sealed class TransactionListItem {
    abstract val key: String
    data class Header(val date: Long) : TransactionListItem() {
        override val key get() = "header_$date"
    }
    data class Item(val transaction: Transaction) : TransactionListItem() {
        override val key get() = "txn_${transaction.id}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    reviewData: ReviewTransactionData? = null,
    onReviewHandled: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRegexGenerator: (String?) -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val pendingNotifications by viewModel.pendingNotifications.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val convertedTotal by viewModel.convertedTotal.collectAsState()
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDefaultCurrency()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isAddingTransaction by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.offset(y = (-82).dp),
                onClick = { isAddingTransaction = true },
                containerColor = GlassSurface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            HomeSummaryCard(
                totalSpending = convertedTotal,
                transactionCount = transactions.size,
                pendingCount = pendingNotifications.size,
                defaultCurrency = defaultCurrency,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            )
            
            if (pendingNotifications.isNotEmpty()) {
                Text(
                    text = "Notification Inbox (${pendingNotifications.size})",
                    style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingNotifications) { notification ->
                        InboxItem(
                            notification = notification,
                            onProcess = {
                                onNavigateToRegexGenerator(notification.text)
                                viewModel.markNotificationAsProcessed(notification)
                            },
                            onDelete = { viewModel.deleteNotification(notification) }
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Transactions will appear here automatically",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val listItems = remember(transactions) {
                    transactions.groupBy { normalizeToDay(it.timestamp) }
                        .mapValues { (_, txns) -> txns.sortedByDescending { it.timestamp } }
                        .toSortedMap(compareByDescending { it })
                        .flatMap { (date, txns) ->
                            listOf(TransactionListItem.Header(date)) + txns.map { TransactionListItem.Item(it) }
                        }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = listItems,
                        key = { it.key }
                    ) { item ->
                        when (item) {
                            is TransactionListItem.Header -> {
                                Text(
                                    text = formatDayHeader(item.date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
                                )
                            }
                            is TransactionListItem.Item -> {
                                val transaction = item.transaction
                                val category = categories.find { it.id == transaction.categoryId }

                                val scope = rememberCoroutineScope()
                                val offsetAnim = remember { Animatable(0f) }
                                var dragOffset by remember { mutableFloatStateOf(0f) }
                                val density = LocalDensity.current
                                val revealThresholdPx = with(density) { 40.dp.toPx() }
                                val revealWidthPx = with(density) { 120.dp.toPx() }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .padding(8.dp)
                                            .background(Color.Red, MaterialTheme.shapes.medium)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .clickable {
                                                    viewModel.deleteTransaction(transaction)
                                                    dragOffset = 0f
                                                    scope.launch { offsetAnim.animateTo(0f) }
                                                }
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, "Delete", tint = Color.White)
                                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .clickable {
                                                    viewModel.deleteTransaction(transaction)
                                                    dragOffset = 0f
                                                    scope.launch { offsetAnim.animateTo(0f) }
                                                }
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, "Delete", tint = Color.White)
                                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                                            .fillMaxWidth()
                                            .pointerInput(Unit) {
                                                detectHorizontalDragGestures(
                                                    onDragEnd = {
                                                        scope.launch {
                                                            if (abs(dragOffset) > revealThresholdPx) {
                                                                val target = if (dragOffset < 0) -revealWidthPx else revealWidthPx
                                                                offsetAnim.animateTo(
                                                                    target,
                                                                    spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh)
                                                                )
                                                                dragOffset = target
                                                            } else {
                                                                offsetAnim.animateTo(
                                                                    0f,
                                                                    spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh)
                                                                )
                                                                dragOffset = 0f
                                                            }
                                                        }
                                                    },
                                                    onHorizontalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffset = (dragOffset + dragAmount).coerceIn(-revealWidthPx, revealWidthPx)
                                                        scope.launch { offsetAnim.snapTo(dragOffset) }
                                                    }
                                                )
                                            }
                                    ) {
                                        TransactionItem(
                                            transaction = transaction,
                                            category = category,
                                            onClick = { editingTransaction = transaction }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingTransaction?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            categories = categories,
            onDismiss = { editingTransaction = null },
            onConfirm = { updatedTransaction ->
                viewModel.updateTransaction(updatedTransaction)
                editingTransaction = null
            }
        )
    }

    if (isAddingTransaction) {
        AddTransactionDialog(
            categories = categories,
            defaultCurrency = defaultCurrency,
            onDismiss = { isAddingTransaction = false },
            onConfirm = { amount, currency, merchant, categoryId ->
                viewModel.addTransaction(amount, currency, merchant, categoryId)
                isAddingTransaction = false
            }
        )
    }

    reviewData?.let { data ->
        ReviewTransactionDialog(
            data = data,
            categories = categories,
            onDismiss = onReviewHandled,
            onConfirm = { amount, currency, merchant, categoryId ->
                viewModel.addTransaction(amount, currency, merchant, categoryId)
                data.rawNotificationId.let { id ->
                    if (id > 0) viewModel.markNotificationAsProcessedById(id)
                }
                onReviewHandled()
            }
        )
    }
}

@Composable
private fun HomeSummaryCard(
    totalSpending: Double,
    transactionCount: Int,
    pendingCount: Int,
    defaultCurrency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.82f),
                borderAlpha = 0.24f
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today at a glance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(totalSpending, defaultCurrency),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$transactionCount entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$pendingCount pending inbox",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InboxItem(
    notification: RawNotificationEntity,
    onProcess: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val appName = remember(notification.packageName) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(notification.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            notification.packageName.split(".").lastOrNull() ?: notification.packageName
        }
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .glassEffect(
                shape = MaterialTheme.shapes.medium,
                containerColor = GlassSurface.copy(alpha = 0.85f),
                borderAlpha = 0.2f
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (notification.stalePatternId != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Stale",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (notification.stalePatternId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onProcess,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Update Pattern", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Button(
                    onClick = onProcess,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Process", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit
) {
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var notes by remember { mutableStateOf(transaction.notes ?: "") }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Category", style = MaterialTheme.typography.titleSmall)
                
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val categoryColor = parseColor(category.colorHex)
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = { selectedCategoryId = category.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(category.name, color = categoryColor) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: transaction.amount
                    onConfirm(transaction.copy(
                        amount = amountDouble,
                        merchant = merchant,
                        categoryId = selectedCategoryId,
                        notes = notes.ifBlank { null }
                    ))
                }
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
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.medium,
                containerColor = GlassSurface,
                borderAlpha = 0.15f,
                contentModifier = Modifier.clickable(onClick = onClick)
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val categoryColor = if (category != null) parseColor(category.colorHex) else MaterialTheme.colorScheme.primary
                Icon(
                    imageVector = getCategoryIcon(category?.iconName ?: "Category"),
                    contentDescription = null,
                    tint = categoryColor
                )
                Column {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = category?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = categoryColor
                    )
                    Text(
                        text = formatDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = formatCurrency(transaction.amount, transaction.currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ReviewTransactionDialog(
    data: ReviewTransactionData,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, currencyCode: String, merchant: String, categoryId: Long) -> Unit
) {
    var amount by remember { mutableStateOf(data.amount.toString()) }
    var currency by remember { mutableStateOf(data.currencyCode) }
    var merchant by remember { mutableStateOf(data.merchant) }
    var selectedCategoryId by remember {
        mutableStateOf(data.suggestedCategoryId ?: categories.firstOrNull()?.id ?: -1L)
    }
    var currencyExpanded by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            com.spendsense.data.local.Currencies.SUPPORTED.forEach { cur ->
                                DropdownMenuItem(
                                    text = { Text("${cur.symbol} ${cur.code} — ${cur.name}") },
                                    onClick = {
                                        currency = cur.code
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val categoryColor = parseColor(category.colorHex)
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = { selectedCategoryId = category.id },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(category.name, color = categoryColor) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (amountDouble != null && amountDouble > 0 && merchant.isNotBlank() && selectedCategoryId > 0) {
                        onConfirm(amountDouble, currency, merchant, selectedCategoryId)
                    }
                },
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && merchant.isNotBlank() && selectedCategoryId > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatCurrency(amount: Double, currencyCode: String = "USD"): String {
    return try {
        val currency = java.util.Currency.getInstance(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance().apply {
            this.currency = currency
        }
        formatter.format(amount)
    } catch (e: Exception) {
        "$$amount"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun normalizeToDay(timestamp: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@Composable
private fun formatDayHeader(dateMillis: Long): String {
    val now = Calendar.getInstance()
    val date = Calendar.getInstance()
    date.timeInMillis = dateMillis

    return when {
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) -> "Today"

        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - date.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

        else -> SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date(dateMillis))
    }
}
