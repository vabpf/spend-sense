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
import com.spendsense.presentation.theme.SpendSenseTheme
import com.spendsense.R
import com.spendsense.presentation.theme.NeonRose
import com.spendsense.presentation.util.LocalGlassHazeState
import com.spendsense.presentation.util.LocalLiquidState
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.whitelistedapps.WhitelistedAppsScreen
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
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
                if (notificationPatternDao.getAllForPackage("com.android.shell").isEmpty()) {
                    notificationPatternDao.upsert(
                        com.spendsense.data.local.entity.NotificationPatternEntity(
                            packageName = "com.android.shell",
                            notificationTitle = "Chase",
                            regex = "Spent (?<amount>\\d+\\.\\d{2}) at (?<merchant>[\\w\\s\\-\\#\\.\\,\\&]+)",
                            isTransaction = true,
                            currencyCode = "USD"
                        )
                    )
                }
            }
        }
        
        handleIntent(intent)
        
        setContent {
            SpendSenseTheme {
                val navController = rememberNavController()
                val hazeState = rememberHazeState()
                val liquidState = rememberLiquidState()
                val bottomNavLiquidState = rememberLiquidState()

                CompositionLocalProvider(
                    LocalGlassHazeState provides hazeState,
                    LocalLiquidState provides liquidState
                ) {
                    // docs/LIQUID_GLASS.md §2-4: liquefiable source must be sibling, not ancestor
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .liquefiable(liquidState = bottomNavLiquidState)
                        ) {
                            // Sibling 1: liquefiable source (pexels bg overlay ONLY — no content)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .liquefiable(liquidState = liquidState)
                            ) {
                            Image(
                                painter = painterResource(id = R.drawable.bg_pexel),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.30f))
                            )
                        }

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
                                        slideIntoContainer(
                                            AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) + fadeIn(animationSpec = tween(300))
                                    },
                                    exitTransition = {
                                        slideOutOfContainer(
                                            AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                                        ) + fadeOut(animationSpec = tween(200))
                                    },
                                    popEnterTransition = {
                                        slideIntoContainer(
                                            AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                                        ) + fadeIn(animationSpec = tween(300))
                                    },
                                    popExitTransition = {
                                        slideOutOfContainer(
                                            AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(200, easing = FastOutLinearInEasing)
                                        ) + fadeOut(animationSpec = tween(200))
                                    }
                                ) {
                                    composable("home") {
                                        HomeScreen(
                                            reviewData = reviewData,
                                            onReviewHandled = { reviewData = null },
                                            onNavigateToSettings = {
                                                navController.navigate("settings") {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onNavigateToRegexGenerator = { text, title ->
                                                 val encodedText = text?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                 val encodedTitle = title?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                 val route = if (encodedText != null && encodedTitle != null) {
                                                     "regex_generator?text=$encodedText&title=$encodedTitle&fromInbox=true"
                                                 } else if (encodedText != null) {
                                                     "regex_generator?text=$encodedText&fromInbox=true"
                                                 } else if (encodedTitle != null) {
                                                     "regex_generator?title=$encodedTitle&fromInbox=true"
                                                 } else {
                                                     "regex_generator"
                                                 }
                                                 navController.navigate(route)
                                             }
                                        )
                                    }

                                    composable("charts") {
                                        ChartsScreen()
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
                                         route = "regex_generator?text={text}&title={title}&fromInbox={fromInbox}",
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
                                             }
                                         )
                                     ) { backStackEntry ->
                                         val text = backStackEntry.arguments?.getString("text")
                                         val title = backStackEntry.arguments?.getString("title")
                                         val fromInbox = backStackEntry.arguments?.getBoolean("fromInbox") ?: false
                                         RegexGeneratorScreen(
                                             initialNotificationText = text,
                                             initialNotificationTitle = title,
                                             isFromInbox = fromInbox,
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
                        } // end of bottomNavLiquefiable container Box

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

                        if (currentDestination?.route in mainScreens) {
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
                                        containerColor = GlassSurface.copy(alpha = 0.86f),
                                        borderAlpha = 0.16f,
                                        sheenAlpha = 0.06f,
                                        hazeState = hazeState,
                                        liquidState = bottomNavLiquidState
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
                                        val selected = currentDestination?.hierarchy?.any { it.route == route } == true

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
