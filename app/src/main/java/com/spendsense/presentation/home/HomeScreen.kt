@file:OptIn(ExperimentalMaterial3Api::class)
package com.spendsense.presentation.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.compose.LocalLifecycleOwner

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.spendsense.presentation.util.FrostGlassDefaults
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.util.fadingEdge
import com.spendsense.presentation.util.prismEdge
import com.spendsense.presentation.util.parseColor
import com.spendsense.presentation.util.bounceClickable
import com.spendsense.presentation.util.combinedBounceClickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onGloballyPositioned
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.NeonRose
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import android.widget.Toast


private fun isFuzzyMatch(target: String, query: String): Boolean {
    if (query.isEmpty()) return true
    var targetIdx = 0
    var queryIdx = 0
    val t = target.lowercase()
    val q = query.lowercase()
    while (targetIdx < t.length && queryIdx < q.length) {
        if (t[targetIdx] == q[queryIdx]) {
            queryIdx++
        }
        targetIdx++
    }
    return queryIdx == q.length
}

private sealed class TransactionListItem {
    abstract val key: String
    data class Header(val date: Long, val dayTotal: Double = 0.0) : TransactionListItem() {
        override val key get() = "header_$date"
    }
    data class Item(val transaction: Transaction) : TransactionListItem() {
        override val key get() = "txn_${transaction.id}"
    }
}

enum class SortOrder {
    NEWEST_FIRST, OLDEST_FIRST, HIGHEST_AMOUNT, LOWEST_AMOUNT
}

data class TransactionFilterState(
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedPaymentSources: Set<String> = emptySet(),
    val selectedPaymentSourceTypes: Set<String> = emptySet(),
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
)

data class BatchChanges(
    val categoryId: Long?,
    val paymentSource: String?,
    val paymentSourceType: String?,
    val notes: String?,
    val currency: String?,
    val merchant: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    reviewData: ReviewTransactionData? = null,
    onReviewHandled: () -> Unit = {},
    initialFilterDate: Long? = null,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToRegexGenerator: (String?, String?, Long?) -> Unit = { _, _, _ -> }
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val pendingNotifications by viewModel.pendingNotifications.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    val convertedTotal by viewModel.convertedTotal.collectAsState()
    val todayConvertedTotal by viewModel.todayConvertedTotal.collectAsState()
    val yesterdayConvertedTotal by viewModel.yesterdayConvertedTotal.collectAsState()
    
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
    var selectedTransactionIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedTotalAmount by remember { mutableStateOf(0.0) }
    LaunchedEffect(selectedTransactionIds, transactions, defaultCurrency) {
        val selectedTxns = transactions.filter { selectedTransactionIds.contains(it.id) }
        var sum = 0.0
        for (txn in selectedTxns) {
            sum += viewModel.convertAmount(txn.amount, txn.currencyCode, txn.timestamp)
        }
        selectedTotalAmount = sum
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchEditConfirmDialog by remember { mutableStateOf(false) }
    var pendingBatchChanges by remember { mutableStateOf<BatchChanges?>(null) }
    val context = LocalContext.current

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var searchQuery by remember { mutableStateOf("") }
    var filterState by remember { mutableStateOf(TransactionFilterState()) }

    LaunchedEffect(initialFilterDate) {
        initialFilterDate?.let { dateMillis ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis

            filterState = filterState.copy(
                startDateMillis = startOfDay,
                endDateMillis = startOfDay
            )
        }
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val availablePaymentSources = remember(transactions) {
        transactions.map { it.paymentSource }.distinct().filter { it.isNotBlank() }
    }
    val availablePaymentSourceTypes = remember(transactions) {
        transactions.map { it.paymentSourceType }.distinct().filter { it.isNotBlank() }
    }

    val filteredTransactions = remember(transactions, searchQuery, filterState, categories) {
        var list = if (searchQuery.isBlank()) {
            transactions
        } else {
            transactions.filter { transaction ->
                val category = categories.find { it.id == transaction.categoryId }
                val categoryName = category?.name.orEmpty()
                
                isFuzzyMatch(transaction.merchant, searchQuery) ||
                isFuzzyMatch(categoryName, searchQuery) ||
                isFuzzyMatch(transaction.amount.toString(), searchQuery) ||
                isFuzzyMatch(transaction.currencyCode, searchQuery) ||
                isFuzzyMatch(transaction.paymentSource, searchQuery) ||
                isFuzzyMatch(transaction.paymentSourceType, searchQuery)
            }
        }

        // 1. Filter by Category
        if (filterState.selectedCategoryIds.isNotEmpty()) {
            list = list.filter { filterState.selectedCategoryIds.contains(it.categoryId) }
        }

        // 1a. Filter by Payment Source
        if (filterState.selectedPaymentSources.isNotEmpty()) {
            list = list.filter { filterState.selectedPaymentSources.contains(it.paymentSource) }
        }

        // 1b. Filter by Payment Source Type
        if (filterState.selectedPaymentSourceTypes.isNotEmpty()) {
            list = list.filter { filterState.selectedPaymentSourceTypes.contains(it.paymentSourceType) }
        }

        // 2. Filter by Custom Date Range (Start Date & End Date)
        filterState.startDateMillis?.let { start ->
            list = list.filter { it.timestamp >= start }
        }
        filterState.endDateMillis?.let { end ->
            val endOfDay = end + 86_390_000L
            list = list.filter { it.timestamp <= endOfDay }
        }

        // 3. Filter by Amount Range
        filterState.minAmount?.let { min ->
            list = list.filter { it.amount >= min }
        }
        filterState.maxAmount?.let { max ->
            list = list.filter { it.amount <= max }
        }

        // 4. Sort transactions
        list = when (filterState.sortOrder) {
            SortOrder.NEWEST_FIRST -> list.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> list.sortedBy { it.timestamp }
            SortOrder.HIGHEST_AMOUNT -> list.sortedByDescending { it.amount }
            SortOrder.LOWEST_AMOUNT -> list.sortedBy { it.amount }
        }

        list
    }

    var weeklySpending by remember { mutableStateOf<List<Pair<String, Double>>>(emptyList()) }
    val currentDayIndex = remember {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
    }
    var dayTotals by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }

    LaunchedEffect(transactions, defaultCurrency) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val list = mutableListOf<Pair<String, Double>>()

        for (i in 0..6) {
            val startOfDay = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = cal.timeInMillis - 1

            val dayTxns = transactions.filter { it.timestamp in startOfDay..endOfDay }
            var sum = 0.0
            for (txn in dayTxns) {
                sum += viewModel.convertAmount(txn.amount, txn.currencyCode, txn.timestamp)
            }
            list.add(dayLabels[i] to sum)
        }
        weeklySpending = list

        val days = transactions.map { normalizeToDay(it.timestamp) }.distinct()
        val totals = mutableMapOf<Long, Double>()
        for (day in days) {
            val dayTxns = transactions.filter { normalizeToDay(it.timestamp) == day }
            var sum = 0.0
            for (txn in dayTxns) {
                sum += viewModel.convertAmount(txn.amount, txn.currencyCode, txn.timestamp)
            }
            totals[day] = sum
        }
        dayTotals = totals
    }

    val listItems = remember(filteredTransactions, filterState.sortOrder, dayTotals) {
        when (filterState.sortOrder) {
            SortOrder.NEWEST_FIRST -> {
                filteredTransactions.groupBy { normalizeToDay(it.timestamp) }
                    .mapValues { (_, txns) -> txns.sortedByDescending { it.timestamp } }
                    .toSortedMap(compareByDescending { it })
                    .flatMap { (date, txns) ->
                        val dayTotal = dayTotals[date] ?: 0.0
                        listOf(TransactionListItem.Header(date, dayTotal)) + txns.map { TransactionListItem.Item(it) }
                    }
            }
            SortOrder.OLDEST_FIRST -> {
                filteredTransactions.groupBy { normalizeToDay(it.timestamp) }
                    .mapValues { (_, txns) -> txns.sortedBy { it.timestamp } }
                    .toSortedMap(compareBy { it })
                    .flatMap { (date, txns) ->
                        val dayTotal = dayTotals[date] ?: 0.0
                        listOf(TransactionListItem.Header(date, dayTotal)) + txns.map { TransactionListItem.Item(it) }
                    }
            }
            SortOrder.HIGHEST_AMOUNT -> {
                filteredTransactions.map { TransactionListItem.Item(it) }
            }
            SortOrder.LOWEST_AMOUNT -> {
                filteredTransactions.map { TransactionListItem.Item(it) }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .offset(y = (-90).dp)
                    .size(54.dp)
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
                    .clickable { isAddingTransaction = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add Transaction",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient scrim: smoothly transitions wallpaper to #F8FAFC
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF8FAFC)),
                            startY = 0f,
                            endY = with(LocalDensity.current) { (statusBarPadding + 280.dp).toPx() }
                        )
                    )
            )

            val listState = rememberLazyListState()
            val density = LocalDensity.current
            val topFadeHeight by remember {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) {
                        24.dp
                    } else {
                        val offsetDp = with(density) { listState.firstVisibleItemScrollOffset.toDp() }
                        offsetDp.coerceAtMost(24.dp)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed top pinned section: Summary Card, Search Bar, and Category Chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = statusBarPadding + 8.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. "Today at a glance" Summary Card
                    HomeSummaryCard(
                        todaySpending = todayConvertedTotal,
                        yesterdaySpending = yesterdayConvertedTotal,
                        weeklySpending = weeklySpending,
                        currentDayIndex = currentDayIndex,
                        transactionCount = transactions.size,
                        pendingCount = pendingNotifications.size,
                        defaultCurrency = defaultCurrency,
                        onNotificationClick = { /* Notifications */ }
                    )

                    // 2. Search Bar Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassEffect(
                                shape = RoundedCornerShape(999.dp)
                            ),
                        shape = RoundedCornerShape(999.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search transactions...", color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF64748B)
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Clear",
                                                tint = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { showFilterSheet = true }) {
                                        Icon(
                                            Icons.Rounded.Tune,
                                            contentDescription = "Filter",
                                            tint = if (filterState != TransactionFilterState()) CyberBlue else Color(0xFF64748B)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // 3. Quick Category Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            val selected = filterState.selectedCategoryIds.isEmpty()
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) CyberBlue else Color.White.copy(alpha = 0.85f))
                                    .clickable {
                                        filterState = filterState.copy(selectedCategoryIds = emptySet())
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                        items(categories) { category ->
                            val selected = filterState.selectedCategoryIds.contains(category.id)
                            val categoryColor = parseColor(category.colorHex)
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selected) categoryColor else Color.White.copy(alpha = 0.85f))
                                    .clickable {
                                        val newSet = if (selected) {
                                            filterState.selectedCategoryIds - category.id
                                        } else {
                                            filterState.selectedCategoryIds + category.id
                                        }
                                        filterState = filterState.copy(selectedCategoryIds = newSet)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(category.iconName),
                                        contentDescription = null,
                                        tint = if (selected) Color.White else categoryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }

                // Scrollable transactions dissolving when scrolled up under the pinned chips
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .fadingEdge(topFadeHeight = topFadeHeight),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 4. Notification Inbox (if any)
                    if (pendingNotifications.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notification Inbox (${pendingNotifications.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F172A)
                            )
                            Box(
                                modifier = Modifier
                                    .glassEffect(
                                        shape = CircleShape,
                                        borderWidth = 1.dp,
                                        borderAlpha = 0.22f
                                    )
                                    .clickable { viewModel.discardAllNotifications() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Discard All",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Discard All",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingNotifications) { notification ->
                                InboxItem(
                                    notification = notification,
                                    onProcess = {
                                        onNavigateToRegexGenerator(notification.text, notification.title, notification.stalePatternId)
                                        viewModel.markNotificationAsProcessed(notification)
                                    },
                                    onAddNew = {
                                        onNavigateToRegexGenerator(notification.text, notification.title, null)
                                        viewModel.markNotificationAsProcessed(notification)
                                    },
                                    onDelete = { viewModel.deleteNotification(notification) }
                                )
                            }
                        }
                    }
                }

                // 5. Transaction list / Empty State
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    if (transactions.isEmpty()) Icons.Rounded.Receipt else Icons.Rounded.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = if (transactions.isEmpty()) "No transactions yet" else "No matching transactions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (transactions.isEmpty()) "Transactions will appear here automatically" else "Try adjusting your search query",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = listItems,
                        key = { it.key }
                    ) { item ->
                        when (item) {
                            is TransactionListItem.Header -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDayHeader(item.date),
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )
                                    Text(
                                        text = formatCurrency(item.dayTotal, defaultCurrency),
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            is TransactionListItem.Item -> {
                                val transaction = item.transaction
                                val category = categories.find { it.id == transaction.categoryId }
                                
                                val scope = rememberCoroutineScope()
                                val offsetAnim = remember { Animatable(0f) }
                                var dragOffset by remember { mutableStateOf(0f) }
                                val revealWidth = 80.dp
                                val revealWidthPx = with(LocalDensity.current) { revealWidth.toPx() }
                                val revealThresholdPx = revealWidthPx * 0.4f

                                LaunchedEffect(selectedTransactionIds.isNotEmpty()) {
                                    if (selectedTransactionIds.isNotEmpty()) {
                                        offsetAnim.animateTo(0f)
                                        dragOffset = 0f
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedTransactionIds.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .background(Color(0xFFEF4444), RoundedCornerShape(20.dp))
                                                .align(Alignment.Center)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .clickable {
                                                        viewModel.deleteTransaction(transaction)
                                                        dragOffset = 0f
                                                        scope.launch { offsetAnim.animateTo(0f) }
                                                    }
                                                    .padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.Delete, "Delete", tint = Color.White)
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                                            .pointerInput(selectedTransactionIds.isEmpty()) {
                                                if (selectedTransactionIds.isEmpty()) {
                                                    detectHorizontalDragGestures(
                                                        onDragEnd = {
                                                            scope.launch {
                                                                if (offsetAnim.value > revealThresholdPx) {
                                                                    offsetAnim.animateTo(
                                                                        revealWidthPx,
                                                                        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessHigh)
                                                                    )
                                                                    dragOffset = revealWidthPx
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
                                                            dragOffset = (dragOffset + dragAmount).coerceIn(0f, revealWidthPx)
                                                            scope.launch { offsetAnim.snapTo(dragOffset) }
                                                        }
                                                    )
                                                }
                                            }
                                    ) {
                                        TransactionItem(
                                            transaction = transaction,
                                            category = category,
                                            isSelected = selectedTransactionIds.contains(transaction.id),
                                            inSelectionMode = selectedTransactionIds.isNotEmpty(),
                                            onLongClick = {
                                                if (selectedTransactionIds.isEmpty()) {
                                                    selectedTransactionIds = selectedTransactionIds + transaction.id
                                                }
                                            },
                                            onClick = {
                                                if (offsetAnim.value > 0f) {
                                                    scope.launch {
                                                        offsetAnim.animateTo(
                                                            0f,
                                                            spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh)
                                                        )
                                                        dragOffset = 0f
                                                    }
                                                } else if (selectedTransactionIds.isNotEmpty()) {
                                                    selectedTransactionIds = if (selectedTransactionIds.contains(transaction.id)) {
                                                        selectedTransactionIds - transaction.id
                                                    } else {
                                                        selectedTransactionIds + transaction.id
                                                    }
                                                } else {
                                                    editingTransaction = transaction
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating selection toolbar
            if (selectedTransactionIds.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = statusBarPadding + 8.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = Color.Black.copy(alpha = 0.15f),
                            spotColor = Color.Black.copy(alpha = 0.10f)
                        )
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { selectedTransactionIds = emptySet() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cancel selection",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${selectedTransactionIds.size} Selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = "Total: ${formatCurrency(selectedTotalAmount, defaultCurrency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CyberBlue
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val allVisibleSelected = filteredTransactions.all { selectedTransactionIds.contains(it.id) }
                            IconButton(
                                onClick = {
                                    selectedTransactionIds = if (allVisibleSelected) {
                                        emptySet()
                                    } else {
                                        filteredTransactions.map { it.id }.toSet()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (allVisibleSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                                    contentDescription = if (allVisibleSelected) "Deselect all" else "Select all",
                                    tint = CyberBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showBatchEditDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit selection",
                                    tint = CyberBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete selected",
                                    tint = NeonRose,
                                    modifier = Modifier.size(18.dp)
                                )
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
            onConfirm = { amount, currency, merchant, categoryId, paymentSource, paymentSourceType ->
                viewModel.addTransaction(amount, currency, merchant, categoryId, paymentSource, paymentSourceType)
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
                if (data.transactionId != null && data.transactionId > 0) {
                    val existingTxn = transactions.firstOrNull { it.id == data.transactionId }
                    viewModel.updateTransactionWithMapping(
                        com.spendsense.domain.model.Transaction(
                            id = data.transactionId,
                            amount = amount,
                            currencyCode = currency,
                            merchant = merchant,
                            categoryId = categoryId,
                            timestamp = existingTxn?.timestamp ?: System.currentTimeMillis(),
                            sourcePackageName = data.sourcePackageName,
                            sourceAppName = data.sourceAppName,
                            paymentSource = existingTxn?.paymentSource ?: "Manual",
                            paymentSourceType = existingTxn?.paymentSourceType ?: "Manual"
                        )
                    )
                } else {
                    viewModel.addTransaction(amount, currency, merchant, categoryId)
                }
                data.rawNotificationId.let { id ->
                    if (id > 0) viewModel.markNotificationAsProcessedById(id)
                }
                onReviewHandled()
            }
        )
    }

    if (showFilterSheet) {
        AdvancedFilterDialog(
            filterState = filterState,
            availablePaymentSources = availablePaymentSources,
            availablePaymentSourceTypes = availablePaymentSourceTypes,
            onFilterChange = { filterState = it },
            onDismiss = { showFilterSheet = false },
            onSelectStartDate = { showStartDatePicker = true },
            onSelectEndDate = { showEndDatePicker = true }
        )
    }

    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterState.startDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    filterState = filterState.copy(startDateMillis = datePickerState.selectedDateMillis)
                    showStartDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            },
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null
            )
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = filterState.endDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    filterState = filterState.copy(endDateMillis = datePickerState.selectedDateMillis)
                    showEndDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            },
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = null,
                headline = null
            )
        }
    }

    if (showDeleteConfirmation) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Selected") },
            text = {
                Text("Are you sure you want to delete the ${selectedTransactionIds.size} selected transactions? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedTxns = transactions.filter { selectedTransactionIds.contains(it.id) }
                        selectedTxns.forEach { txn ->
                            viewModel.deleteTransaction(txn)
                        }
                        selectedTransactionIds = emptySet()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = NeonRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBatchEditDialog) {
        BatchEditTransactionsDialog(
            selectedCount = selectedTransactionIds.size,
            categories = categories,
            onDismiss = { showBatchEditDialog = false },
            onConfirm = { categoryId, paymentSource, paymentSourceType, notes, currency, merchant ->
                if (categoryId != null || paymentSource != null || paymentSourceType != null || notes != null || currency != null || merchant != null) {
                    pendingBatchChanges = BatchChanges(
                        categoryId = categoryId,
                        paymentSource = paymentSource,
                        paymentSourceType = paymentSourceType,
                        notes = notes,
                        currency = currency,
                        merchant = merchant
                    )
                    showBatchEditConfirmDialog = true
                } else {
                    Toast.makeText(
                        context,
                        "No changes specified.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    if (showBatchEditConfirmDialog) {
        val changes = pendingBatchChanges
        if (changes != null) {
            val selectedCount = selectedTransactionIds.size
            GlassAlertDialog(
                onDismissRequest = { showBatchEditConfirmDialog = false },
                title = { Text("Confirm Batch Edit") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Are you sure you want to update $selectedCount transactions with the following changes?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (changes.categoryId != null) {
                                val catName = categories.firstOrNull { it.id == changes.categoryId }?.name ?: "Unknown"
                                Text("• Category: $catName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (changes.merchant != null) {
                                Text("• Merchant: ${changes.merchant}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (changes.notes != null) {
                                Text("• Notes: ${changes.notes}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (changes.paymentSource != null) {
                                Text("• Payment Source: ${changes.paymentSource}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (changes.paymentSourceType != null) {
                                Text("• Payment Source Type: ${changes.paymentSourceType}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (changes.currency != null) {
                                Text("• Currency: ${changes.currency}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedTxns = transactions.filter { selectedTransactionIds.contains(it.id) }
                            val updatedTxns = selectedTxns.map { txn ->
                                txn.copy(
                                    categoryId = changes.categoryId ?: txn.categoryId,
                                    merchant = changes.merchant ?: txn.merchant,
                                    notes = changes.notes ?: txn.notes,
                                    paymentSource = changes.paymentSource ?: txn.paymentSource,
                                    paymentSourceType = changes.paymentSourceType ?: txn.paymentSourceType,
                                    currencyCode = changes.currency ?: txn.currencyCode
                                )
                            }
                            viewModel.updateTransactions(updatedTxns)
                            Toast.makeText(
                                context,
                                "Updated $selectedCount transactions successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedTransactionIds = emptySet()
                            showBatchEditConfirmDialog = false
                            showBatchEditDialog = false
                            pendingBatchChanges = null
                        }
                    ) {
                        Text("Confirm", color = CyberBlue)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBatchEditConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun HomeSummaryCard(
    todaySpending: Double,
    yesterdaySpending: Double,
    weeklySpending: List<Pair<String, Double>>,
    currentDayIndex: Int,
    transactionCount: Int,
    pendingCount: Int,
    defaultCurrency: String,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassEffect(
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: "Today at a glance" + Bell icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today at a glance",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (pendingCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFEF4444),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "$pendingCount",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Hero Row: Total spending + Trend pill on left, Mini 7-day Bar Chart on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatCurrency(todaySpending, defaultCurrency),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        // Trend pill
                        val percentDiff = if (yesterdaySpending > 0.0) {
                            (((todaySpending - yesterdaySpending) / yesterdaySpending) * 100).roundToInt()
                        } else null

                        if (percentDiff != null && percentDiff != 0) {
                            val isHigher = percentDiff > 0
                            val pillBg = if (isHigher) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                            val pillColor = if (isHigher) Color(0xFFDC2626) else Color(0xFF16A34A)
                            val arrow = if (isHigher) "↗" else "↘"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(pillBg)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$arrow ${abs(percentDiff)}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = pillColor
                                )
                            }
                        }
                    }

                    Text(
                        text = "You spent ${formatCurrency(yesterdaySpending, defaultCurrency)} yesterday",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0369A1)
                    )
                }

                // Mini 7-day Bar Chart (Mon..Sun)
                val maxSpend = remember(weeklySpending) {
                    weeklySpending.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    weeklySpending.forEachIndexed { index, (dayLabel, amount) ->
                        val isToday = index == currentDayIndex
                        val barHeight = ((amount / maxSpend) * 36.0).coerceIn(8.0, 40.0).dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isToday) CyberBlue else Color(0xFFBAE6FD).copy(alpha = 0.55f)
                                    )
                            )
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) CyberBlue else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // Bottom Counter Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Entries count pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.65f),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Segment,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$transactionCount entries",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }

                // Pending inbox pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.65f),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mail,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$pendingCount pending inbox",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InboxItem(
    notification: RawNotificationEntity,
    onProcess: () -> Unit,
    onDelete: () -> Unit,
    onAddNew: (() -> Unit)? = null
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
            .height(148.dp)
            .glassEffect(
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
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
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
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
            }

            if (notification.stalePatternId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onProcess,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Update", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { onAddNew?.invoke() },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Add New", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Button(
                    onClick = onProcess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .align(Alignment.BottomCenter),
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
    var amount by remember { mutableStateOf(formatDoublePlain(transaction.amount)) }
    var currency by remember { mutableStateOf(transaction.currencyCode) }
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var notes by remember { mutableStateOf(transaction.notes ?: "") }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentSource by remember { mutableStateOf(transaction.paymentSource) }
    var paymentSourceType by remember { mutableStateOf(transaction.paymentSourceType) }
    
    var transactionTimestamp by remember { mutableStateOf(transaction.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val dateTimeFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    text = { Text("${cur.symbol} ${cur.code}") },
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

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
                        val selected = category.id == selectedCategoryId
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategoryId = category.id },
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
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: transaction.amount
                    onConfirm(transaction.copy(
                        amount = amountDouble,
                        currencyCode = currency,
                        merchant = merchant,
                        categoryId = selectedCategoryId,
                        notes = notes.ifBlank { null },
                        timestamp = transactionTimestamp,
                        paymentSource = paymentSource.trim(),
                        paymentSourceType = paymentSourceType.trim()
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
                            val currentCal = Calendar.getInstance().apply { timeInMillis = transactionTimestamp }
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
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
            title = { Text("Edit Time") },
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
                        val hr = hourInput.toIntOrNull() ?: 12
                        val min = minuteInput.toIntOrNull() ?: 0
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

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    isSelected: Boolean = false,
    inSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedBounceClickable(
                onLongClick = onLongClick,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        border = if (isSelected) BorderStroke(1.5.dp, CyberBlue) else null,
        shadowElevation = if (isSelected) 3.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inSelectionMode) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) CyberBlue else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                        .background(
                            if (isSelected) CyberBlue else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                val categoryColor = if (category != null) parseColor(category.colorHex) else CyberBlue
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category?.iconName ?: "Category"),
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sourceText = if (transaction.paymentSource.equals("Manual", ignoreCase = true)) {
                        "Manual"
                    } else {
                        "${transaction.paymentSourceType} (${transaction.paymentSource})"
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category?.name ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Medium,
                            color = categoryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = formatTime(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatCurrency(transaction.amount, transaction.currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
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
    var amount by remember { mutableStateOf(formatDoublePlain(data.amount)) }
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
                                    text = { Text("${cur.symbol} ${cur.code}") },
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
    val cleanCurrencyCode = currencyCode.trim().uppercase()
    if (cleanCurrencyCode == "VND") {
        return try {
            val formatter = NumberFormat.getNumberInstance().apply {
                if (amount % 1.0 == 0.0) {
                    this.minimumFractionDigits = 0
                    this.maximumFractionDigits = 0
                } else {
                    this.minimumFractionDigits = 0
                    this.maximumFractionDigits = 2
                }
            }
            val formattedNumber = formatter.format(amount)
            "$formattedNumber₫"
        } catch (e: Exception) {
            "${formatDoublePlain(amount)}₫"
        }
    }
    return try {
        val currency = java.util.Currency.getInstance(cleanCurrencyCode)
        val formatter = NumberFormat.getCurrencyInstance().apply {
            this.currency = currency
            if (amount % 1.0 == 0.0) {
                this.minimumFractionDigits = 0
                this.maximumFractionDigits = 0
            }
        }
        formatter.format(amount)
    } catch (e: Exception) {
        val cleanCode = currencyCode.trim()
        val symbol = try {
            java.util.Currency.getInstance(cleanCode.uppercase()).symbol
        } catch (_: Exception) {
            if (cleanCode.isNotBlank()) cleanCode else "$"
        }
        "$symbol ${formatDoublePlain(amount)}"
    }
}

private fun formatDoublePlain(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        java.math.BigDecimal.valueOf(value).toPlainString()
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

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun LensDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    minThickness: Dp = 1.dp,
    maxThickness: Dp = 4.dp
) {
    val minPx = with(androidx.compose.ui.platform.LocalDensity.current) { minThickness.toPx() }
    val maxPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxThickness.toPx() }
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(maxThickness)
    ) {
        val width = size.width
        val height = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, (height - minPx) / 2)
            quadraticTo(width / 2, (height - maxPx) / 2, width, (height - minPx) / 2)
            lineTo(width, (height + minPx) / 2)
            quadraticTo(width / 2, (height + maxPx) / 2, 0f, (height + minPx) / 2)
            close()
        }
        val gradient = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                color.copy(alpha = 0.35f),
                color,
                color.copy(alpha = 0.35f),
                Color.Transparent
            )
        )
        drawPath(
            path = path,
            brush = gradient
        )
    }
}

@Composable
fun GlassFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    activeColor: Color = CyberBlue
) {
    val borderAlpha = if (selected) 0.6f else 0.22f
    val containerColor = if (selected) activeColor.copy(alpha = 0.22f) else Color.Transparent
    val textColor = if (selected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    
    Box(
        modifier = Modifier
            .glassEffect(
                shape = CircleShape,
                containerColor = containerColor,
                borderWidth = 1.dp,
                borderAlpha = borderAlpha,
                useLens = true
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun AdvancedFilterDialog(
    filterState: TransactionFilterState,
    availablePaymentSources: List<String>,
    availablePaymentSourceTypes: List<String>,
    onFilterChange: (TransactionFilterState) -> Unit,
    onDismiss: () -> Unit,
    onSelectStartDate: () -> Unit,
    onSelectEndDate: () -> Unit
) {
    var minAmount by remember { mutableStateOf(filterState.minAmount?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(filterState.maxAmount?.toString() ?: "") }
    var sortOrder by remember { mutableStateOf(filterState.sortOrder) }
    var selectedPaymentSources by remember { mutableStateOf(filterState.selectedPaymentSources) }
    var selectedPaymentSourceTypes by remember { mutableStateOf(filterState.selectedPaymentSourceTypes) }

    val isMinValid = minAmount.isEmpty() || minAmount.toDoubleOrNull() != null
    val isMaxValid = maxAmount.isEmpty() || maxAmount.toDoubleOrNull() != null

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Date Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable(onClick = onSelectStartDate)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Start Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = filterState.startDateMillis?.let { dateFormat.format(Date(it)) } ?: "Anytime",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (filterState.startDateMillis != null) Color(0xFF0284C7) else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable(onClick = onSelectEndDate)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "End Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = filterState.endDateMillis?.let { dateFormat.format(Date(it)) } ?: "Anytime",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (filterState.endDateMillis != null) Color(0xFF0284C7) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Text(
                    "Amount Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmount,
                        onValueChange = { minAmount = it },
                        label = { Text("Min Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !isMinValid,
                        supportingText = {
                            if (!isMinValid) {
                                Text("Invalid amount", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmount,
                        onValueChange = { maxAmount = it },
                        label = { Text("Max Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = !isMaxValid,
                        supportingText = {
                            if (!isMaxValid) {
                                Text("Invalid amount", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                if (availablePaymentSourceTypes.isNotEmpty()) {
                    Text(
                        "Payment Source Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availablePaymentSourceTypes.forEach { type ->
                            val selected = selectedPaymentSourceTypes.contains(type)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedPaymentSourceTypes = if (selected) {
                                        selectedPaymentSourceTypes - type
                                    } else {
                                        selectedPaymentSourceTypes + type
                                    }
                                },
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
                }

                if (availablePaymentSources.isNotEmpty()) {
                    Text(
                        "Payment Source",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availablePaymentSources.forEach { source ->
                            val selected = selectedPaymentSources.contains(source)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedPaymentSources = if (selected) {
                                        selectedPaymentSources - source
                                    } else {
                                        selectedPaymentSources + source
                                    }
                                },
                                label = { Text(source) },
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
                }

                Text(
                    "Sort Order",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortChip(
                            text = "Newest First",
                            selected = sortOrder == SortOrder.NEWEST_FIRST,
                            onClick = { sortOrder = SortOrder.NEWEST_FIRST },
                            modifier = Modifier.weight(1f)
                        )
                        SortChip(
                            text = "Oldest First",
                            selected = sortOrder == SortOrder.OLDEST_FIRST,
                            onClick = { sortOrder = SortOrder.OLDEST_FIRST },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortChip(
                            text = "Highest Amount",
                            selected = sortOrder == SortOrder.HIGHEST_AMOUNT,
                            onClick = { sortOrder = SortOrder.HIGHEST_AMOUNT },
                            modifier = Modifier.weight(1f)
                        )
                        SortChip(
                            text = "Lowest Amount",
                            selected = sortOrder == SortOrder.LOWEST_AMOUNT,
                            onClick = { sortOrder = SortOrder.LOWEST_AMOUNT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val min = minAmount.toDoubleOrNull()
                    val max = maxAmount.toDoubleOrNull()
                    onFilterChange(
                        filterState.copy(
                            minAmount = min,
                            maxAmount = max,
                            sortOrder = sortOrder,
                            selectedPaymentSources = selectedPaymentSources,
                            selectedPaymentSourceTypes = selectedPaymentSourceTypes
                        )
                    )
                    onDismiss()
                },
                enabled = isMinValid && isMaxValid
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onFilterChange(TransactionFilterState())
                        minAmount = ""
                        maxAmount = ""
                        sortOrder = SortOrder.NEWEST_FIRST
                        selectedPaymentSources = emptySet()
                        selectedPaymentSourceTypes = emptySet()
                        onDismiss()
                    }
                ) {
                    Text("Clear All")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun SortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC)
    val borderColor = if (selected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
    val textColor = if (selected) Color(0xFF0369A1) else Color(0xFF334155)

    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(10.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun BatchEditTransactionsDialog(
    selectedCount: Int,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (
        categoryId: Long?,
        paymentSource: String?,
        paymentSourceType: String?,
        notes: String?,
        currency: String?,
        merchant: String?
    ) -> Unit
) {
    var currency by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(-1L) }
    var notes by remember { mutableStateOf("") }
    var currencyExpanded by remember { mutableStateOf(false) }
    var paymentSource by remember { mutableStateOf("") }
    var paymentSourceType by remember { mutableStateOf("No Change") }

    val paymentSourceTypes = listOf("No Change", "Credit Card", "Debit Card", "Bank Account", "Wallet", "Manual")

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Selection ($selectedCount)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Only non-blank fields entered in the window will be updated for all selected items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.width(130.dp)
                    ) {
                        OutlinedTextField(
                            value = if (currency.isEmpty()) "No Change" else currency,
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
                            DropdownMenuItem(
                                text = { Text("No Change") },
                                onClick = {
                                    currency = ""
                                    currencyExpanded = false
                                }
                            )
                            com.spendsense.data.local.Currencies.SUPPORTED.forEach { cur ->
                                DropdownMenuItem(
                                    text = { Text("${cur.symbol} ${cur.code}") },
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
                        placeholder = { Text("No Change") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("No Change") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paymentSource,
                    onValueChange = { paymentSource = it },
                    label = { Text("Payment Source Identifier") },
                    placeholder = { Text("No Change") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Payment Source Type", style = MaterialTheme.typography.titleSmall)

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
                    val noChangeSelected = selectedCategoryId == -1L || selectedCategoryId == null
                    FilterChip(
                        selected = noChangeSelected,
                        onClick = { selectedCategoryId = -1L },
                        label = { Text("No Change") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFF8FAFC),
                            labelColor = Color(0xFF475569),
                            selectedContainerColor = Color(0xFFE0F2FE),
                            selectedLabelColor = Color(0xFF0369A1)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = noChangeSelected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = Color(0xFF0284C7)
                        )
                    )
                    
                    categories.forEach { category ->
                        val categoryColor = parseColor(category.colorHex)
                        val selected = category.id == selectedCategoryId
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategoryId = category.id },
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
            Button(
                onClick = {
                    onConfirm(
                        if (selectedCategoryId == -1L) null else selectedCategoryId,
                        paymentSource.trim().ifBlank { null },
                        if (paymentSourceType == "No Change") null else paymentSourceType,
                        notes.trim().ifBlank { null },
                        currency.ifBlank { null },
                        merchant.trim().ifBlank { null }
                    )
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
