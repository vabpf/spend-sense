@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendsense.data.local.Currencies
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.data.service.ParsedTransactionResult
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BatchItemUiState(
    val id: String = UUID.randomUUID().toString(),
    val isSelected: Boolean = true,
    val amount: Double,
    val currency: String,
    val merchant: String,
    val categoryId: Long,
    val paymentSource: String,
    val paymentSourceType: String,
    val timestamp: Long,
    val notes: String?,
    val missingFields: List<String>
)

@Composable
fun BatchTransactionsDialog(
    transactions: List<ParsedTransactionResult>,
    categories: List<Category>,
    defaultCurrency: String = "USD",
    historyPaymentSources: List<HistoryPaymentSource> = emptyList(),
    onDismiss: () -> Unit,
    onSaveBatch: (List<Transaction>) -> Unit
) {
    val items = remember(transactions) {
        mutableStateListOf<BatchItemUiState>().apply {
            addAll(
                transactions.map { parsed ->
                    val matchedCat = parsed.categoryName?.let { catName ->
                        categories.find { it.name.equals(catName, ignoreCase = true) }
                    } ?: categories.firstOrNull()

                    val defaultSource = historyPaymentSources.firstOrNull()?.name ?: "Cash"
                    val defaultSourceType = historyPaymentSources.firstOrNull()?.type ?: "Manual"

                    BatchItemUiState(
                        isSelected = true,
                        amount = parsed.amount ?: 0.0,
                        currency = parsed.currency?.takeIf { it.isNotBlank() } ?: defaultCurrency,
                        merchant = parsed.merchant?.takeIf { it.isNotBlank() } ?: "Unknown Merchant",
                        categoryId = matchedCat?.id ?: 1L,
                        paymentSource = parsed.paymentSource?.takeIf { it.isNotBlank() } ?: defaultSource,
                        paymentSourceType = parsed.paymentSourceType?.takeIf { it.isNotBlank() } ?: defaultSourceType,
                        timestamp = parsed.timestamp ?: System.currentTimeMillis(),
                        notes = parsed.notes,
                        missingFields = parsed.missingFields
                    )
                }
            )
        }
    }

    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    val selectedCount = items.count { it.isSelected }
    val allSelected = items.isNotEmpty() && items.all { it.isSelected }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Batch Transactions (${items.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Review and approve before saving",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                if (items.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val newSelect = !allSelected
                            items.indices.forEach { idx ->
                                items[idx] = items[idx].copy(isSelected = newSelect)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (allSelected) "Deselect" else "Select All",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        text = {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No valid transactions to ingest.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        val category = categories.find { it.id == item.categoryId }
                        val categoryColor = category?.let { parseColor(it.colorHex) } ?: CyberBlue
                        val currencySymbol = Currencies.find(item.currency).symbol

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (item.isSelected) CyberBlue.copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            color = if (item.isSelected) Color(0xFFF0F9FF).copy(alpha = 0.5f) else Color(0xFFF8FAFC)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = item.isSelected,
                                            onCheckedChange = { checked ->
                                                items[index] = item.copy(isSelected = checked)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = CyberBlue,
                                                uncheckedColor = Color(0xFF94A3B8)
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.merchant,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${dateTimeFormatter.format(Date(item.timestamp))} • ${item.paymentSource}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = "$currencySymbol ${numberFormatter.format(item.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberBlue
                                        )

                                        category?.let { cat ->
                                            Surface(
                                                color = categoryColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = getCategoryIcon(cat.iconName),
                                                        contentDescription = null,
                                                        tint = categoryColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = cat.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = categoryColor,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Warning chips for incomplete fields
                                if (item.missingFields.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (item.missingFields.contains("date")) {
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "⚠️ Inferred Date",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFB45309),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (item.missingFields.contains("payment_source")) {
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "⚠️ Defaulted Source",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFB45309),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (item.missingFields.contains("merchant")) {
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "⚠️ Unclear Merchant",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFB45309),
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!item.notes.isNullOrBlank()) {
                                    Text(
                                        text = item.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { editingIndex = index },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit Transaction",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            items.removeAt(index)
                                            if (items.isEmpty()) onDismiss()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Remove Transaction",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val approvedTransactions = items.filter { it.isSelected }.map { item ->
                        Transaction(
                            amount = item.amount,
                            currencyCode = item.currency,
                            merchant = item.merchant,
                            categoryId = item.categoryId,
                            timestamp = item.timestamp,
                            sourcePackageName = "manual",
                            sourceAppName = "AI Batch Ingestion",
                            notes = item.notes,
                            paymentSource = item.paymentSource,
                            paymentSourceType = item.paymentSourceType
                        )
                    }
                    onSaveBatch(approvedTransactions)
                },
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberBlue,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Save Selected ($selectedCount)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Edit individual transaction inside batch
    editingIndex?.let { idx ->
        if (idx in items.indices) {
            val itemToEdit = items[idx]
            AddTransactionDialog(
                categories = categories,
                defaultCurrency = itemToEdit.currency,
                historyPaymentSources = historyPaymentSources,
                initialAmount = itemToEdit.amount,
                initialCurrency = itemToEdit.currency,
                initialMerchant = itemToEdit.merchant,
                initialCategoryId = itemToEdit.categoryId,
                initialPaymentSource = itemToEdit.paymentSource,
                initialPaymentSourceType = itemToEdit.paymentSourceType,
                initialTimestamp = itemToEdit.timestamp,
                initialNotes = itemToEdit.notes,
                missingFields = itemToEdit.missingFields,
                onDismiss = { editingIndex = null },
                onConfirm = { amount, currency, merchant, categoryId, paymentSource, paymentSourceType, timestamp, notes ->
                    items[idx] = itemToEdit.copy(
                        amount = amount,
                        currency = currency,
                        merchant = merchant,
                        categoryId = categoryId,
                        paymentSource = paymentSource,
                        paymentSourceType = paymentSourceType,
                        timestamp = timestamp,
                        notes = notes,
                        missingFields = emptyList() // User confirmed & resolved fields
                    )
                    editingIndex = null
                }
            )
        }
    }
}
