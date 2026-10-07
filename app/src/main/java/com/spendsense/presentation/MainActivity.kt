package com.spendsense.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.spendsense.presentation.util.LocalBackdrop
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.whitelistedapps.WhitelistedAppsScreen
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import dagger.hilt.android.AndroidEntryPoint
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
                val pagerState = rememberPagerState(initialPage = 0) { 3 }
                val coroutineScope = rememberCoroutineScope()
                var homeFilterDate by remember { mutableStateOf<Long?>(null) }

                LaunchedEffect(reviewData) {
                    if (reviewData != null) {
                        pagerState.animateScrollToPage(0)
                    }
                }

                val backgroundBackdrop = rememberLayerBackdrop()
                val contentBackdrop = rememberLayerBackdrop()
                val backgroundTheme by securePreferences.backgroundThemeFlow.collectAsState()
                val customBackgroundPath by securePreferences.customBackgroundPathFlow.collectAsState()

                CompositionLocalProvider(
                    LocalBackdrop provides backgroundBackdrop,
                    LocalAppBackgroundTheme provides backgroundTheme,
                    LocalCustomBackgroundPath provides customBackgroundPath
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Sibling 1: Content + Background layer inside contentBackdrop
                        // Allows the floating bottom nav bar to blur both cards and background!
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(contentBackdrop)
                        ) {
                            AppBackground(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(backgroundBackdrop)
                            )

                            Scaffold(
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onBackground,
                            ) { innerPadding ->
                                NavHost(
                                    navController = navController,
                                    startDestination = "home",
                                    enterTransition = {
                                        slideIntoContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        ) + fadeIn(animationSpec = tween(200))
                                    },
                                    exitTransition = {
                                        slideOutOfContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                                            targetOffset = { -it / 3 }
                                        ) + fadeOut(animationSpec = tween(200))
                                    },
                                    popEnterTransition = {
                                        slideIntoContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing),
                                            initialOffset = { -it / 3 }
                                        ) + fadeIn(animationSpec = tween(200))
                                    },
                                    popExitTransition = {
                                        slideOutOfContainer(
                                            towards = AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        ) + fadeOut(animationSpec = tween(200))
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
                                        val navFilterDate = backStackEntry.arguments?.getLong("filterDate") ?: -1L
                                        val effectiveFilterDate = if (navFilterDate != -1L) navFilterDate else homeFilterDate

                                        BackHandler(enabled = pagerState.currentPage != 0) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        }

                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { page ->
                                            when (page) {
                                                0 -> HomeScreen(
                                                    reviewData = reviewData,
                                                    onReviewHandled = { reviewData = null },
                                                    initialFilterDate = effectiveFilterDate,
                                                    onNavigateToSettings = {
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(2)
                                                        }
                                                    },
                                                    onNavigateToRegexGenerator = { text, title, stalePatternId, packageName ->
                                                        val encodedText = text?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                        val encodedTitle = title?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                        val encodedPackage = packageName?.let { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
                                                        val baseRoute = "regex_generator?fromInbox=true"
                                                        val textParam = if (encodedText != null) "&text=$encodedText" else ""
                                                        val titleParam = if (encodedTitle != null) "&title=$encodedTitle" else ""
                                                        val packageParam = if (encodedPackage != null) "&packageName=$encodedPackage" else ""
                                                        val staleParam = if (stalePatternId != null) "&stalePatternId=$stalePatternId" else ""
                                                        navController.navigate(baseRoute + textParam + titleParam + packageParam + staleParam)
                                                    }
                                                )
                                                1 -> ChartsScreen(
                                                    onNavigateToHomeWithFilter = { date ->
                                                        homeFilterDate = date
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(0)
                                                        }
                                                    }
                                                )
                                                2 -> SettingsScreen(
                                                    onNavigateBack = {
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(0)
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
                                        }
                                    }

                                    composable("charts") {
                                        LaunchedEffect(Unit) {
                                            pagerState.scrollToPage(1)
                                            navController.popBackStack()
                                        }
                                    }

                                    composable("settings") {
                                        LaunchedEffect(Unit) {
                                            pagerState.scrollToPage(2)
                                            navController.popBackStack()
                                        }
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
                                          route = "regex_generator?text={text}&title={title}&packageName={packageName}&fromInbox={fromInbox}&stalePatternId={stalePatternId}",
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
                                              navArgument("packageName") {
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
                                           val pkgName = backStackEntry.arguments?.getString("packageName")
                                          val fromInbox = backStackEntry.arguments?.getBoolean("fromInbox") ?: false
                                          val stalePatternIdStr = backStackEntry.arguments?.getString("stalePatternId")
                                          val stalePatternId = stalePatternIdStr?.toLongOrNull()
                                          RegexGeneratorScreen(
                                              initialNotificationText = text,
                                              initialNotificationTitle = title,
                                               initialPackageName = pkgName,
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

                        val navItems = listOf(
                            Triple(0, Icons.Rounded.Home, "Home"),
                            Triple(1, Icons.Rounded.BarChart, "Charts"),
                            Triple(2, Icons.Rounded.Settings, "Settings")
                        )

                        val isMainScreen = currentDestination?.route?.let { route ->
                            route.startsWith("home")
                        } ?: false

                        if (isMainScreen) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp, vertical = 12.dp)
                                    .offset(y = (-16).dp)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = RoundedCornerShape(999.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.14f),
                                        spotColor = Color.Black.copy(alpha = 0.12f)
                                    )
                                    .glassEffect(
                                        shape = RoundedCornerShape(999.dp),
                                        liveBlur = true,
                                        useLens = true,
                                        backdrop = contentBackdrop
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    navItems.forEach { (pageIndex, icon, label) ->
                                        val selected = pagerState.currentPage == pageIndex

                                        Box(
                                             modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(pageIndex)
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = if (selected) CyberBlue else Color(0xFF64748B),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) CyberBlue else Color(0xFF64748B)
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
