package com.spendsense.presentation.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendsense.domain.model.Category
import com.spendsense.domain.model.Transaction
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.theme.TextSecondary
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.parseColor
import java.util.Calendar
import java.util.Locale

private data class CalendarDay(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val dateMillis: Long,
    val dailySpending: Double = 0.0,
    val transactions: List<Transaction> = emptyList()
)

@Composable
fun CalendarSpendingChart(
    allTransactions: List<Transaction>,
    categories: List<Category>,
    currency: String,
    modifier: Modifier = Modifier,
    onFilterDay: (Long) -> Unit = {}
) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }

    val monthYearFormatter = remember(currentMonth) {
        val monthLabels = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        "${monthLabels[currentMonth.get(Calendar.MONTH)]}, ${currentMonth.get(Calendar.YEAR)}"
    }

    val calendarDays = remember(currentMonth, allTransactions) {
        val year = currentMonth.get(Calendar.YEAR)
        val month = currentMonth.get(Calendar.MONTH)
        val days = mutableListOf<CalendarDay>()

        val firstDayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
        val startPadding = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        val prevMonthCal = Calendar.getInstance().apply {
            timeInMillis = firstDayCal.timeInMillis
            add(Calendar.MONTH, -1)
        }
        val daysInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in startPadding - 1 downTo 0) {
            val dayNum = daysInPrevMonth - i
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = prevMonthCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, dayNum)
            }
            days.add(
                CalendarDay(
                    dayOfMonth = dayNum,
                    isCurrentMonth = false,
                    dateMillis = dayCal.timeInMillis
                )
            )
        }

        val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..daysInMonth) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = firstDayCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            days.add(
                CalendarDay(
                    dayOfMonth = i,
                    isCurrentMonth = true,
                    dateMillis = dayCal.timeInMillis
                )
            )
        }

        val nextMonthCal = Calendar.getInstance().apply {
            timeInMillis = firstDayCal.timeInMillis
            add(Calendar.MONTH, 1)
        }
        val remainingDays = 42 - days.size
        for (i in 1..remainingDays) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = nextMonthCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, i)
            }
            days.add(
                CalendarDay(
                    dayOfMonth = i,
                    isCurrentMonth = false,
                    dateMillis = dayCal.timeInMillis
                )
            )
        }

        val tempCal = Calendar.getInstance()
        val txnsByDay = allTransactions.groupBy { txn ->
            tempCal.timeInMillis = txn.timestamp
            Triple(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH))
        }

        days.map { day ->
            tempCal.timeInMillis = day.dateMillis
            val key = Triple(tempCal.get(Calendar.YEAR), tempCal.get(Calendar.MONTH), tempCal.get(Calendar.DAY_OF_MONTH))
            val dayTxns = txnsByDay[key] ?: emptyList()
            day.copy(
                dailySpending = dayTxns.sumOf { it.amount },
                transactions = dayTxns
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.24f
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header: title + interactive month switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spending Calendar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newMonth = Calendar.getInstance().apply {
                                timeInMillis = currentMonth.timeInMillis
                                add(Calendar.MONTH, -1)
                            }
                            currentMonth = newMonth
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = monthYearFormatter,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            val newMonth = Calendar.getInstance().apply {
                                timeInMillis = currentMonth.timeInMillis
                                add(Calendar.MONTH, 1)
                            }
                            currentMonth = newMonth
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next Month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Days of the week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val headers = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 6 rows grid
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                val today = Calendar.getInstance()
                val cellCal = Calendar.getInstance()
                for (row in 0 until 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val dayIndex = row * 7 + col
                            val day = calendarDays[dayIndex]
                            cellCal.timeInMillis = day.dateMillis

                            val isToday = today.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                    today.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                                    today.get(Calendar.DAY_OF_MONTH) == cellCal.get(Calendar.DAY_OF_MONTH)

                            CalendarDayCell(
                                day = day,
                                isToday = isToday,
                                onClick = { selectedDay = day },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Popover Dialog
    selectedDay?.let { day ->
        val titleText = remember(day) {
            val dayCal = Calendar.getInstance().apply { timeInMillis = day.dateMillis }
            val monthLabels = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            "Spending — ${monthLabels[dayCal.get(Calendar.MONTH)]} ${dayCal.get(Calendar.DAY_OF_MONTH)}, ${dayCal.get(Calendar.YEAR)}"
        }

        GlassAlertDialog(
            onDismissRequest = { selectedDay = null },
            confirmButton = {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyberBlue,
                    modifier = Modifier
                        .clickable { selectedDay = null }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            },
            dismissButton = {
                Text(
                    text = "View This Day",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyberBlue,
                    modifier = Modifier
                        .clickable {
                            onFilterDay(day.dateMillis)
                            selectedDay = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            },
            title = {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    day.transactions.forEach { txn ->
                        val cat = categories.find { it.id == txn.categoryId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassEffect(
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = Color.White.copy(alpha = 0.04f),
                                    borderAlpha = 0.1f
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = parseColor(cat?.colorHex ?: "#7F7F7F").copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat?.iconName ?: "Other"),
                                        contentDescription = null,
                                        tint = parseColor(cat?.colorHex ?: "#7F7F7F"),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = txn.merchant,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${txn.paymentSource} (${txn.paymentSourceType})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatAmount(txn.amount, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonRose
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clickableModifier = if (day.transactions.isNotEmpty()) {
        Modifier.clickable { onClick() }
    } else Modifier

    val borderModifier = if (isToday) {
        Modifier.border(1.dp, CyberBlue, RoundedCornerShape(8.dp))
    } else Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(borderModifier)
            .then(clickableModifier)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.dayOfMonth.toString().padStart(2, '0'),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (day.isCurrentMonth) {
                if (isToday) CyberBlue else MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            }
        )
        val compactAmount = remember(day.dailySpending) {
            formatCompactAmount(day.dailySpending)
        }
        if (day.isCurrentMonth && compactAmount.isNotEmpty()) {
            Text(
                text = compactAmount,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = NeonRose,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            // Placeholder text to keep cell heights uniform
            Text(
                text = "",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

internal fun formatCompactAmount(amount: Double): String {
    if (amount <= 0.0) return ""
    return when {
        amount >= 1_000_000.0 -> {
            val valM = amount / 1_000_000.0
            val formatted = if (valM % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", valM)
            } else {
                String.format(Locale.US, "%.1f", valM)
            }
            "-${formatted}M"
        }
        amount >= 1_000.0 -> {
            val valK = amount / 1_000.0
            val formatted = if (valK % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f", valK)
            } else {
                String.format(Locale.US, "%.1f", valK)
            }
            "-${formatted}K"
        }
        else -> {
            "-${String.format(Locale.US, "%.0f", amount)}"
        }
    }
}
