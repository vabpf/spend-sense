@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendsense.data.local.Currencies
import com.spendsense.domain.model.Category
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor

data class HistoryPaymentSource(
    val name: String,
    val type: String = "Manual"
)

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    defaultCurrency: String = "USD",
    historyPaymentSources: List<HistoryPaymentSource> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, currencyCode: String, merchant: String, categoryId: Long, paymentSource: String, paymentSourceType: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(categories.firstOrNull()) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentSourceExpanded by remember { mutableStateOf(false) }
    var paymentSource by remember {
        mutableStateOf(historyPaymentSources.firstOrNull()?.name ?: "Cash")
    }
    var paymentSourceType by remember {
        mutableStateOf(historyPaymentSources.firstOrNull()?.type ?: "Manual")
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    leadingIcon = { Text("${Currencies.find(currency).symbol}", style = MaterialTheme.typography.bodyMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded }
                ) {
                    OutlinedTextField(
                        value = "${Currencies.find(currency).symbol} $currency",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        Currencies.SUPPORTED.forEach { cur ->
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
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )


                ExposedDropdownMenuBox(
                    expanded = paymentSourceExpanded,
                    onExpandedChange = { paymentSourceExpanded = !paymentSourceExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentSource,
                        onValueChange = {
                            paymentSource = it
                            val match = historyPaymentSources.find { h -> h.name.equals(it.trim(), ignoreCase = true) }
                            if (match != null && match.type.isNotBlank()) {
                                paymentSourceType = match.type
                            }
                        },
                        label = { Text("Payment Source Identifier") },
                        placeholder = { Text("e.g. Cash, Momo, Chase") },
                        trailingIcon = {
                            if (historyPaymentSources.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentSourceExpanded)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )

                    if (historyPaymentSources.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = paymentSourceExpanded,
                            onDismissRequest = { paymentSourceExpanded = false }
                        ) {
                            historyPaymentSources.forEach { history ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(history.name, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                history.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    },
                                    onClick = {
                                        paymentSource = history.name
                                        if (history.type.isNotBlank()) {
                                            paymentSourceType = history.type
                                        }
                                        paymentSourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Payment Source Type", style = MaterialTheme.typography.titleSmall)

                val paymentSourceTypes = listOf("Credit Card", "Debit Card", "Bank Account", "Wallet", "Manual")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentSourceTypes.forEach { type ->
                        val selected = paymentSourceType == type
                        FilterChip(
                            selected = selected,
                            onClick = { paymentSourceType = type },
                            label = { Text(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFFF8FAFC),
                                labelColor = Color(0xFF475569),
                                selectedContainerColor = Color(0xFFE0F2FE),
                                selectedLabelColor = Color(0xFF0369A1)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.titleSmall)

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val categoryColor = parseColor(category.colorHex)
                        val selected = category.id == selectedCategory?.id
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = category },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text(category.name, color = categoryColor) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFFF8FAFC),
                                selectedContainerColor = categoryColor.copy(alpha = 0.15f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = categoryColor
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amountDouble = amount.toDoubleOrNull()
            val canSave = amountDouble != null && amountDouble > 0 && merchant.isNotBlank() && selectedCategory != null

            Button(
                onClick = {
                    if (canSave) {
                        selectedCategory?.let { category ->
                            onConfirm(
                                amountDouble!!,
                                currency,
                                merchant.trim(),
                                category.id,
                                paymentSource.trim().ifBlank { "Manual" },
                                paymentSourceType.trim().ifBlank { "Manual" }
                            )
                        }
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
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
