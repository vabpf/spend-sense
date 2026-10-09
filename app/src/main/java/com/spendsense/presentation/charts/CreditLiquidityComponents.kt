package com.spendsense.presentation.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendsense.domain.calculation.CardStatementCycleResult
import com.spendsense.domain.calculation.CreditLiquiditySummary
import com.spendsense.domain.model.CreditCardConfig
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.util.glassEffect

@Composable
fun CreditLiquidityCard(
    modifier: Modifier = Modifier,
    summary: CreditLiquiditySummary,
    currency: String,
    unsetCreditCards: List<String> = emptyList(),
    onConfigureCard: (cardName: String?) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(shape = RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFCE7F3)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = null,
                            tint = NeonRose,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Credit & Cash Liquidity",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBE185D)
                    )
                }

                IconButton(
                    onClick = { onConfigureCard(null) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Manage Credit Cards",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Key Metrics: Immediate Cash Demand vs Active Floating Debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Immediate Cash Needed (Closed Statements)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF1F2))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Due for Payment",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF9F1239),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatAmount(summary.totalImmediateCashNeeded, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBE123C)
                        )
                        Text(
                            text = "Closed statements",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = Color(0xFFE11D48).copy(alpha = 0.8f)
                        )
                    }
                }

                // Active Floating Debt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Floating Next Month",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatAmount(summary.totalActiveFloatingDebt, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                        Text(
                            text = "Active cycles",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = Color(0xFF16A34A).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Unset credit card prompt chips
            if (unsetCreditCards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Set statement closing day for:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        unsetCreditCards.forEach { cardName ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFFE0F2FE).copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier.clickable { onConfigureCard(cardName) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = cardName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (summary.cardCycles.isNotEmpty()) {
                // Tappable toggle for card breakdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide card cycles" else "View individual card cycles (${summary.cardCycles.size})",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0284C7)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 140)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(durationMillis = 100))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.04f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.cardCycles.forEach { cycle ->
                            CardCycleRow(
                                cycle = cycle,
                                currency = currency,
                                onEdit = { onConfigureCard(cycle.cardName) }
                            )
                        }
                    }
                }
            } else if (unsetCreditCards.isEmpty()) {
                // Prompt to configure credit cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFE0F2FE).copy(alpha = 0.5f))
                        .clickable { onConfigureCard(null) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+ Set statement closing day for your cards",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CardCycleRow(
    cycle: CardStatementCycleResult,
    currency: String,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = cycle.cardName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "(Closes ${cycle.statementClosingDay}th)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = Color(0xFF64748B)
                )
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Card",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Closed bill: ${formatAmount(cycle.closedStatementBalance, currency)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Medium,
                color = if (cycle.closedStatementBalance > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
            )
            Text(
                text = "Active: ${formatAmount(cycle.activeStatementBalance, currency)} (${cycle.daysUntilActiveCloses}d left)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun CreditCardConfigDialog(
    initialCardName: String = "",
    existingConfig: CreditCardConfig? = null,
    availableCardNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CreditCardConfig) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var cardName by remember { mutableStateOf(existingConfig?.cardName ?: initialCardName) }
    var statementClosingDay by remember { mutableStateOf(existingConfig?.statementClosingDay?.toString() ?: "15") }
    var paymentDueDay by remember { mutableStateOf(existingConfig?.paymentDueDay?.toString() ?: "") }
    var creditLimit by remember { mutableStateOf(existingConfig?.creditLimit?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingConfig != null) "Edit Credit Card Cycle" else "Configure Credit Card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (availableCardNames.isNotEmpty() && existingConfig == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Detected cards:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = Color(0xFF64748B)
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableCardNames.forEach { name ->
                                val isSelected = cardName.trim().equals(name.trim(), ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { cardName = name }
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card / Source Name") },
                    placeholder = { Text("e.g. Chase Freedom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = statementClosingDay,
                    onValueChange = { statementClosingDay = it.filter { char -> char.isDigit() }.take(2) },
                    label = { Text("Statement Closing Day (1–31)") },
                    placeholder = { Text("e.g. 15") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = paymentDueDay,
                    onValueChange = { paymentDueDay = it.filter { char -> char.isDigit() }.take(2) },
                    label = { Text("Payment Due Day (Optional 1–31)") },
                    placeholder = { Text("e.g. 5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Credit Limit (Optional)") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val day = statementClosingDay.toIntOrNull()?.coerceIn(1, 31) ?: 15
                    val due = paymentDueDay.toIntOrNull()?.coerceIn(1, 31)
                    val limit = creditLimit.toDoubleOrNull()
                    if (cardName.isNotBlank()) {
                        onSave(
                            CreditCardConfig(
                                cardName = cardName.trim(),
                                statementClosingDay = day,
                                paymentDueDay = due,
                                creditLimit = limit
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = cardName.isNotBlank() && statementClosingDay.toIntOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existingConfig != null && onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(existingConfig.cardName)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
