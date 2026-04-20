package com.spendsense.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.entity.RawNotificationEntity
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRegexGenerator: (String?) -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val pendingNotifications by viewModel.pendingNotifications.collectAsState()
    
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isAddingTransaction by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpendSense") },
                actions = {
                    IconButton(onClick = { onNavigateToRegexGenerator(null) }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Regex Generator")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddingTransaction = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (pendingNotifications.isNotEmpty()) {
                Text(
                    text = "Notification Inbox (${pendingNotifications.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Receipt,
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
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->
                        val category = categories.find { it.id == transaction.categoryId }
                        
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.StartToEnd || it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteTransaction(transaction)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> Color.Red
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .background(color, MaterialTheme.shapes.medium),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )
                                }
                            },
                            content = {
                                TransactionItem(
                                    transaction = transaction,
                                    category = category,
                                    onClick = { editingTransaction = transaction }
                                )
                            }
                        )
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
            onDismiss = { isAddingTransaction = false },
            onConfirm = { amount, merchant, categoryId ->
                viewModel.addTransaction(amount, merchant, categoryId)
                isAddingTransaction = false
            }
        )
    }
}

@Composable
fun InboxItem(
    notification: RawNotificationEntity,
    onProcess: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.width(280.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.packageName.split(".").last(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
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

    AlertDialog(
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
                        FilterChip(
                            selected = category.id == selectedCategoryId,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) }
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
            .clickable(onClick = onClick)
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
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(transaction.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = formatCurrency(transaction.amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(amount)
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
