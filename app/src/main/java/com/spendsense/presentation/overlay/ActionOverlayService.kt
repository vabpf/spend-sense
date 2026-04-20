package com.spendsense.presentation.overlay

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.spendsense.data.service.TransactionNotificationListener
import com.spendsense.domain.model.Category
import com.spendsense.presentation.theme.SpendSenseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class ActionOverlayService : Service(), ViewModelStoreOwner, LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
        
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    @Inject
    lateinit var viewModelFactory: dagger.Lazy<ActionOverlayViewModel>

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var currentOverlayViewModel: ActionOverlayViewModel? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val overlayReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TransactionNotificationListener.ACTION_SHOW_OVERLAY) {
                val amount = intent.getDoubleExtra(TransactionNotificationListener.EXTRA_AMOUNT, 0.0)
                val merchant = intent.getStringExtra(TransactionNotificationListener.EXTRA_MERCHANT) ?: ""
                val packageName = intent.getStringExtra(TransactionNotificationListener.EXTRA_PACKAGE_NAME) ?: ""
                val appName = intent.getStringExtra(TransactionNotificationListener.EXTRA_APP_NAME) ?: ""
                val rawNotificationId = intent.getLongExtra("extra_raw_notification_id", -1L)
                
                showOverlay(amount, merchant, packageName, appName, rawNotificationId)
            }

        }
    }

    companion object {
        private const val TAG = "ActionOverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "overlay_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        
        Log.d(TAG, "ActionOverlayService created")
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Register broadcast receiver
        val filter = IntentFilter(TransactionNotificationListener.ACTION_SHOW_OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(overlayReceiver, filter)
        }
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun showOverlay(amount: Double, merchant: String, packageName: String, appName: String, rawNotificationId: Long) {
        // Check permission
        if (!canDrawOverlays()) {
            Log.e(TAG, "Cannot draw overlays - permission not granted")
            return
        }

        // Remove existing overlay if any
        removeOverlay()

        val viewModel = viewModelFactory.get()
        currentOverlayViewModel = viewModel
        viewModel.initialize(amount, merchant, packageName, appName, rawNotificationId)


        // Create ComposeView
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ActionOverlayService)
            setViewTreeViewModelStoreOwner(this@ActionOverlayService)
            setViewTreeSavedStateRegistryOwner(this@ActionOverlayService)
            
            setContent {
                SpendSenseTheme {
                    OverlayContent(
                        viewModel = viewModel,
                        onDismiss = { removeOverlay() },
                        onSave = {
                            viewModel.saveTransaction {
                                removeOverlay()
                            }
                        }
                    )
                }
            }
        }

        // Setup window params
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager.addView(overlayView, params)
            Log.d(TAG, "Overlay shown")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                overlayView = null
                Log.d(TAG, "Overlay removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay", e)
            }
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the transaction overlay service running"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SpendSense")
            .setContentText("Monitoring for transaction notifications")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlay()
        try {
            unregisterReceiver(overlayReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        serviceScope.cancel()
        currentOverlayViewModel?.dispose()
        viewModelStore.clear()
        Log.d(TAG, "ActionOverlayService destroyed")
    }
}

@Composable
fun OverlayContent(
    viewModel: ActionOverlayViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.sourceAppName.ifBlank { "New Transaction" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Display
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Amount") },
                leadingIcon = { Text("$", style = MaterialTheme.typography.titleLarge) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = !state.isAmountValid && state.amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant Display
            OutlinedTextField(
                value = state.merchant,
                onValueChange = { viewModel.updateMerchant(it) },
                label = { Text("Merchant") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryGrid(
                categories = categories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving && state.isAmountValid
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else if (state.showSuccess) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long) -> Unit
) {
    val rows = categories.chunked(4)
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    CategoryItem(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        onSelected = { onCategorySelected(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space in incomplete rows
                repeat(4 - rowCategories.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        parseColor(category.colorHex)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onSelected() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = getCategoryIcon(category.iconName),
            contentDescription = category.name,
            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Default.Restaurant
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "Movie" -> Icons.Default.Movie
        "Receipt" -> Icons.Default.Receipt
        "LocalHospital" -> Icons.Default.LocalHospital
        "MoreHoriz" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}

fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
}
