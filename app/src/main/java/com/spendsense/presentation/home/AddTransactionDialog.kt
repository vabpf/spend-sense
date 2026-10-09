@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendsense.data.local.Currencies
import com.spendsense.domain.model.Category
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class HistoryPaymentSource(
    val name: String,
    val type: String = "Manual"
)

@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    defaultCurrency: String = "USD",
    historyPaymentSources: List<HistoryPaymentSource> = emptyList(),
    initialAmount: Double? = null,
    initialCurrency: String? = null,
    initialMerchant: String? = null,
    initialCategoryId: Long? = null,
    initialPaymentSource: String? = null,
    initialPaymentSourceType: String? = null,
    initialTimestamp: Long? = null,
    initialNotes: String? = null,
    missingFields: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        currencyCode: String,
        merchant: String,
        categoryId: Long,
        paymentSource: String,
        paymentSourceType: String,
        timestamp: Long,
        notes: String?
    ) -> Unit
) {
    var amount by remember(initialAmount) {
        mutableStateOf(
            if (initialAmount != null && initialAmount > 0) {
                if (initialAmount % 1.0 == 0.0) initialAmount.toLong().toString() else initialAmount.toString()
            } else ""
        )
    }
    var currency by remember(initialCurrency, defaultCurrency) {
        mutableStateOf(initialCurrency ?: defaultCurrency)
    }
    var merchant by remember(initialMerchant) {
        mutableStateOf(initialMerchant ?: "")
    }
    var selectedCategory by remember(initialCategoryId, categories) {
        mutableStateOf(
            if (initialCategoryId != null) {
                categories.find { it.id == initialCategoryId } ?: categories.firstOrNull()
            } else {
                categories.firstOrNull()
            }
        )
    }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentSourceExpanded by remember { mutableStateOf(false) }
    var paymentSource by remember(initialPaymentSource, historyPaymentSources) {
        mutableStateOf(
            initialPaymentSource?.takeIf { it.isNotBlank() }
                ?: historyPaymentSources.firstOrNull()?.name
                ?: "Cash"
        )
    }
    var paymentSourceType by remember(initialPaymentSourceType, historyPaymentSources) {
        mutableStateOf(
            initialPaymentSourceType?.takeIf { it.isNotBlank() }
                ?: historyPaymentSources.firstOrNull()?.type
                ?: "Manual"
        )
    }
    var transactionTimestamp by remember(initialTimestamp) {
        mutableStateOf(initialTimestamp ?: System.currentTimeMillis())
    }
    var notes by remember(initialNotes) {
        mutableStateOf(initialNotes ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

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

                if (missingFields.contains("merchant")) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Merchant unclear from receipt — please verify",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
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


                if (missingFields.contains("payment_source")) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Missing on receipt — defaulted to Cash / Manual",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

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

                if (missingFields.contains("date")) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Missing date on receipt — inferred as Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Transaction Date & Time", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dateTimeFormatter.format(Date(transactionTimestamp)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF0284C7),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Change Date and Time",
                            tint = Color(0xFF0284C7)
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

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Scanned receipt items or notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
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
                                paymentSourceType.trim().ifBlank { "Manual" },
                                transactionTimestamp,
                                notes.trim().ifBlank { null }
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = transactionTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis
                        if (selectedDate != null) {
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedDate }
                            val currentCal = Calendar.getInstance().apply { timeInMillis = transactionTimestamp }
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, currentCal.get(Calendar.SECOND))
                            }
                            transactionTimestamp = newCal.timeInMillis
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentCal = Calendar.getInstance().apply { timeInMillis = transactionTimestamp }
        var hourInput by remember { mutableStateOf(currentCal.get(Calendar.HOUR_OF_DAY).toString()) }
        var minuteInput by remember { mutableStateOf(currentCal.get(Calendar.MINUTE).toString().padStart(2, '0')) }

        GlassAlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select time (24h format)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.isEmpty() || (clean.toIntOrNull() in 0..23)) {
                                    hourInput = clean.take(2)
                                }
                            },
                            label = { Text("Hour") },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = minuteInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                if (clean.isEmpty() || (clean.toIntOrNull() in 0..59)) {
                                    minuteInput = clean.take(2)
                                }
                            },
                            label = { Text("Min") },
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hr = hourInput.toIntOrNull() ?: currentCal.get(Calendar.HOUR_OF_DAY)
                        val min = minuteInput.toIntOrNull() ?: currentCal.get(Calendar.MINUTE)
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = transactionTimestamp
                            set(Calendar.HOUR_OF_DAY, hr)
                            set(Calendar.MINUTE, min)
                        }
                        transactionTimestamp = newCal.timeInMillis
                        showTimePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
