package com.spendsense.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.navArgument
import com.spendsense.data.service.TransactionNotificationListener
import com.spendsense.domain.model.ReviewTransactionData
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.domain.repository.WhitelistedAppRepository
import com.spendsense.presentation.home.HomeScreen
import com.spendsense.presentation.charts.ChartsScreen
import com.spendsense.presentation.categories.CategoriesScreen
import com.spendsense.presentation.settings.AiProvidersScreen
import com.spendsense.presentation.settings.ProviderDetailScreen
import com.spendsense.presentation.settings.RegexGeneratorScreen
import com.spendsense.presentation.settings.SettingsScreen
import com.spendsense.presentation.settings.NotificationPatternsScreen
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.DeepCharcoal
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.AppBackground
import com.spendsense.presentation.theme.LocalAppBackgroundTheme
import com.spendsense.presentation.theme.LocalCustomBackgroundPath
import com.spendsense.presentation.theme.SpendSenseTheme
import com.spendsense.R
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.util.LocalGlassHazeState
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.whitelistedapps.WhitelistedAppsScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var whitelistedAppRepository: WhitelistedAppRepository

    @Inject
    lateinit var whitelistedAppDao: com.spendsense.data.local.dao.WhitelistedAppDao

    @Inject
    lateinit var notificationPatternDao: com.spendsense.data.local.dao.NotificationPatternDao

    @Inject
    lateinit var securePreferences: com.spendsense.data.local.SecurePreferences

    private var reviewData by mutableStateOf<ReviewTransactionData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        lifecycleScope.launch {
            categoryRepository.initializeDefaultCategories()
            val isDebuggable = (applicationContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) {
                whitelistedAppDao.insert(
                    com.spendsense.data.local.entity.WhitelistedAppEntity(
                        packageName = "com.android.shell",
                        appName = "Android Shell (Debug)",
                        isEnabled = true
                    )
                )
                // Clean existing debug patterns to start fresh
                val existingShellPatterns = notificationPatternDao.getAllForPackage("com.android.shell")
                existingShellPatterns.forEach { notificationPatternDao.deleteById(it.id) }

                notificationPatternDao.upsert(
                    com.spendsense.data.local.entity.NotificationPatternEntity(
                        id = 10001L,
                        packageName = "com.android.shell",
                        notificationTitle = "Chase",
                        paymentSource = "Chase",
                        regex = "Spent (?<amount>\\d+\\.\\d{2}) at (?<merchant>[\\w\\s\\-\\#\\.\\,\\&]+)",
                        isTransaction = true,
                        currencyCode = "USD"
                    )
                )
                notificationPatternDao.upsert(
                    com.spendsense.data.local.entity.NotificationPatternEntity(
                        id = 10002L,
                        packageName = "com.android.shell",
                        notificationTitle = "Chase OTP",
                        paymentSource = "",
                        regex = "OTP: (?<code>\\d+)",
                        isTransaction = false
                    )
                )
            }
        }
        
        handleIntent(intent)
        
        setContent {
            SpendSenseTheme {
                val navController = rememberNavController()
                val hazeState = rememberHazeState()
                val backgroundTheme by securePreferences.backgroundThemeFlow.collectAsState()
                val customBackgroundPath by securePreferences.customBackgroundPathFlow.collectAsState()

                CompositionLocalProvider(
                    LocalGlassHazeState provides hazeState,
                    LocalAppBackgroundTheme provides backgroundTheme,
                    LocalCustomBackgroundPath provides customBackgroundPath
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppBackground(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(state = hazeState)
                        )

                        // Sibling 2: Content (Scaffold + NavHost) — NOT inside liquefiable
                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onBackground,
                            ) { innerPadding ->
                                NavHost(
                                    navController = navController,
                                    startDestination = "home",
                                    enterTransition = {
                                        fadeIn(animationSpec = tween(150))
                                    },
                                    exitTransition = {
                                        fadeOut(animationSpec = tween(150))
                                    },
                                    popEnterTransition = {
                                        fadeIn(animationSpec = tween(150))
                                    },
                                    popExitTransition = {
                                        fadeOut(animationSpec = tween(150))
                                    }
                                ) {
                                    composable(
                                        route = "home?filterDate={filterDate}",
                                        arguments = listOf(
                                            navArgument("filterDate") {
                                                type = NavType.LongType
                                                defaultValue = -1L
                                            }
                                        )
                                    ) { backStackEntry ->
                                        val filterDate = backStackEntry.arguments?.getLong("filterDate") ?: -1L
                                        HomeScreen(
                                            reviewData = reviewData,
                                            onReviewHandled = { reviewData = null },
                                            initialFilterDate = if (filterDate != -1L) filterDate else null,
                                            onNavigateToSettings = {
                                                navController.navigate("settings") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onNavigateToRegexGenerator = { text, title, stalePatternId ->
                                                val encodedText = text?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                val encodedTitle = title?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                val baseRoute = "regex_generator?fromInbox=true"
                                                val textParam = if (encodedText != null) "&text=$encodedText" else ""
                                                val titleParam = if (encodedTitle != null) "&title=$encodedTitle" else ""
                                                val staleParam = if (stalePatternId != null) "&stalePatternId=$stalePatternId" else ""
                                                navController.navigate(baseRoute + textParam + titleParam + staleParam)
                                            }
                                        )
                                    }

                                    composable("charts") {
                                        ChartsScreen(
                                            onNavigateToHomeWithFilter = { date ->
                                                navController.navigate("home?filterDate=$date") {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        // clear backstack up to home to apply the new argument cleanly
                                                    }
                                                    launchSingleTop = true
                                                }
                                            }
                                        )
                                    }

                                    composable("settings") {
                                        SettingsScreen(
                                            onNavigateBack = {
                                                navController.navigate("home") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onNavigateToRegexGenerator = {
                                                navController.navigate("regex_generator")
                                            },
                                            onNavigateToAiProviders = {
                                                navController.navigate("ai_providers")
                                            },
                                            onNavigateToWhitelistedApps = {
                                                navController.navigate("whitelisted_apps")
                                            },
                                            onNavigateToCategories = {
                                                navController.navigate("categories")
                                            },
                                            onNavigateToNotificationPatterns = {
                                                navController.navigate("notification_patterns")
                                            }
                                        )
                                    }

                                    composable("whitelisted_apps") {
                                        WhitelistedAppsScreen(
                                            onNavigateBack = {
                                                navController.popBackStack()
                                            }
                                        )
                                    }

                                    composable("categories") {
                                        CategoriesScreen(
                                            onNavigateBack = {
                                                navController.popBackStack()
                                            }
                                        )
                                    }

                                    composable("ai_providers") {
                                        AiProvidersScreen(
                                            onNavigateBack = {
                                                navController.popBackStack()
                                            },
                                            onNavigateToDetail = { accountId ->
                                                navController.navigate("provider_detail/$accountId")
                                            }
                                        )
                                    }

                                    composable(
                                        route = "provider_detail/{accountId}",
                                        arguments = listOf(
                                            navArgument("accountId") { type = NavType.LongType }
                                        )
                                    ) { backStackEntry ->
                                        val accountId = backStackEntry.arguments?.getLong("accountId") ?: return@composable
                                        ProviderDetailScreen(
                                            accountId = accountId,
                                            onNavigateBack = {
                                                navController.popBackStack()
                                            }
                                        )
                                    }

                                    composable("notification_patterns") {
                                        NotificationPatternsScreen(
                                            onNavigateBack = {
                                                navController.popBackStack()
                                            }
                                        )
                                    }

                                    composable(
                                          route = "regex_generator?text={text}&title={title}&fromInbox={fromInbox}&stalePatternId={stalePatternId}",
                                          arguments = listOf(
                                              navArgument("text") {
                                                  type = NavType.StringType
                                                  nullable = true
                                                  defaultValue = null
                                              },
                                              navArgument("title") {
                                                  type = NavType.StringType
                                                  nullable = true
                                                  defaultValue = null
                                              },
                                              navArgument("fromInbox") {
                                                  type = NavType.BoolType
                                                  defaultValue = false
                                              },
                                              navArgument("stalePatternId") {
                                                  type = NavType.StringType
                                                  nullable = true
                                                  defaultValue = null
                                              }
                                          )
                                      ) { backStackEntry ->
                                          val text = backStackEntry.arguments?.getString("text")
                                          val title = backStackEntry.arguments?.getString("title")
                                          val fromInbox = backStackEntry.arguments?.getBoolean("fromInbox") ?: false
                                          val stalePatternIdStr = backStackEntry.arguments?.getString("stalePatternId")
                                          val stalePatternId = stalePatternIdStr?.toLongOrNull()
                                          RegexGeneratorScreen(
                                              initialNotificationText = text,
                                              initialNotificationTitle = title,
                                              isFromInbox = fromInbox,
                                              stalePatternId = stalePatternId,
                                             onNavigateBack = {
                                                 navController.popBackStack()
                                             },
                                             onNavigateToNotificationPatterns = {
                                                 navController.navigate("notification_patterns")
                                             }
                                         )
                                     }
                                }
                            }
                        }

                        // Sibling 2: nav bar with glass effect (separate from Scaffold)
                        // Referenced as: GLASS_NAV_BAR (floating pill at bottom center)
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination

                        val mainScreens = listOf("home", "charts", "settings")
                        val navItems = listOf(
                            Triple("home", Icons.Rounded.Home, "Home"),
                            Triple("charts", Icons.Rounded.BarChart, "Charts"),
                            Triple("settings", Icons.Rounded.Settings, "Settings")
                        )

                        val isMainScreen = currentDestination?.route?.let { route ->
                            route.startsWith("home") || route == "charts" || route == "settings"
                        } ?: false

                        if (isMainScreen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color.Transparent,
                                            0.25f to MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                            0.45f to MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                                            0.7f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                            1.0f to MaterialTheme.colorScheme.background
                                        )
                                    )
                                    .align(Alignment.BottomCenter)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp, vertical = 12.dp)
                                    .offset(y = (-16).dp)
                                    .shadow(
                                        elevation = 22.dp,
                                        shape = RoundedCornerShape(999.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.25f),
                                        spotColor = Color.Black.copy(alpha = 0.18f)
                                    )
                                    .glassEffect(
                                        shape = RoundedCornerShape(999.dp),
                                        containerColor = GlassSurface.copy(alpha = 0.70f),
                                        borderAlpha = 0.20f,
                                        sheenAlpha = 0.08f,
                                        liveBlur = true,
                                        hazeState = hazeState
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    navItems.forEach { (route, icon, label) ->
                                        val selected = currentDestination?.hierarchy?.any { dest ->
                                            if (route == "home") {
                                                dest.route?.startsWith("home") == true
                                            } else {
                                                dest.route == route
                                            }
                                        } == true

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    if (selected) {
                                                        CyberBlue.copy(alpha = 0.16f)
                                                    } else {
                                                        Color.Transparent
                                                    }
                                                )
                                                .clickable {
                                                    navController.navigate(route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    icon,
                                                    contentDescription = label,
                                                    tint = if (selected) CyberBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) CyberBlue else MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra(TransactionNotificationListener.EXTRA_REVIEW_MERCHANT)) {
                reviewData = ReviewTransactionData(
                    amount = it.getDoubleExtra(TransactionNotificationListener.EXTRA_REVIEW_AMOUNT, 0.0),
                    merchant = it.getStringExtra(TransactionNotificationListener.EXTRA_REVIEW_MERCHANT) ?: return,
                    currencyCode = it.getStringExtra(TransactionNotificationListener.EXTRA_REVIEW_CURRENCY) ?: "USD",
                    sourcePackageName = it.getStringExtra(TransactionNotificationListener.EXTRA_REVIEW_PACKAGE_NAME) ?: "",
                    sourceAppName = it.getStringExtra(TransactionNotificationListener.EXTRA_REVIEW_APP_NAME) ?: "",
                    rawNotificationId = it.getLongExtra(TransactionNotificationListener.EXTRA_REVIEW_RAW_NOTIFICATION_ID, -1L),
                    suggestedCategoryId = it.getLongExtra(TransactionNotificationListener.EXTRA_REVIEW_CATEGORY_ID, -1L).let { id ->
                        if (id > 0) id else null
                    },
                    transactionId = it.getLongExtra(TransactionNotificationListener.EXTRA_REVIEW_TRANSACTION_ID, -1L).let { id ->
                        if (id > 0) id else null
                    }
                )
            }
        }
    }
}
