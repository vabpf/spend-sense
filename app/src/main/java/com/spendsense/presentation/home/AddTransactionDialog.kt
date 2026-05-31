@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendsense.data.local.Currencies
import com.spendsense.domain.model.Category
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    defaultCurrency: String = "USD",
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, currencyCode: String, merchant: String, categoryId: Long, paymentSource: String, paymentSourceType: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(defaultCurrency) }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(categories.firstOrNull()) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentSource by remember { mutableStateOf("Manual") }
    var paymentSourceType by remember { mutableStateOf("Manual") }

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

                OutlinedTextField(
                    value = paymentSource,
                    onValueChange = { paymentSource = it },
                    label = { Text("Payment Source Identifier") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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
                            onClick = { paymentSourceType = type },
                            label = { Text(type) }
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
                        FilterChip(
                            selected = category.id == selectedCategory?.id,
                            onClick = { selectedCategory = category },
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
            val amountDouble = amount.toDoubleOrNull()
            val canSave = amountDouble != null && amountDouble > 0 && merchant.isNotBlank() && selectedCategory != null

            TextButton(
                onClick = {
                    if (canSave) {
                        selectedCategory?.let { category ->
                            onConfirm(
                                amountDouble!!,
                                currency,
                                merchant,
                                category.id,
                                paymentSource.trim(),
                                paymentSourceType.trim()
                            )
                        }
                    }
                },
                enabled = canSave
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
