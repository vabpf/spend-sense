package com.spendsense.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
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
import androidx.navigation.navArgument
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.presentation.home.HomeScreen
import com.spendsense.presentation.charts.ChartsScreen
import com.spendsense.presentation.categories.CategoriesScreen
import com.spendsense.presentation.overlay.ActionOverlayService
import com.spendsense.presentation.settings.AiProvidersScreen
import com.spendsense.presentation.settings.RegexGeneratorScreen
import com.spendsense.presentation.settings.SettingsScreen
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.HoloCyan
import com.spendsense.presentation.theme.HoloRose
import com.spendsense.presentation.theme.SpendSenseTheme
import com.spendsense.presentation.util.glassEffect
import com.spendsense.presentation.whitelistedapps.WhitelistedAppsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var categoryRepository: CategoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            categoryRepository.initializeDefaultCategories()
        }
        
        startActionOverlayService()
        
        setContent {
            SpendSenseTheme {
                val navController = rememberNavController()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF0F111A),
                                    Color(0xFF171629),
                                    Color(0xFF121A24)
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(HoloCyan.copy(alpha = 0.24f), Color.Transparent),
                                    radius = 900f
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(HoloRose.copy(alpha = 0.2f), Color.Transparent),
                                    radius = 1100f
                                )
                            )
                    )

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        bottomBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination

                            val mainScreens = listOf("home", "charts", "settings")

                            if (currentDestination?.route in mainScreens) {
                                // Sharp icons/text layer
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 20.dp)
                                ) {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Home") },
                                            selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                                            onClick = {
                                                navController.navigate("home") {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.BarChart, contentDescription = "Charts") },
                                            label = { Text("Charts") },
                                            selected = currentDestination?.hierarchy?.any { it.route == "charts" } == true,
                                            onClick = {
                                                navController.navigate("charts") {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings") },
                                            selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                                            onClick = {
                                                navController.navigate("settings") {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = 0.dp // Allow content under bottom bar
                            )
                        ) {
                            composable("home") {
                                HomeScreen(
                                    onNavigateToSettings = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateToRegexGenerator = { text ->
                                        val route = if (text != null) {
                                            "regex_generator?text=$text"
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
                                    }
                                )
                            }

                            composable(
                                route = "regex_generator?text={text}",
                                arguments = listOf(
                                    navArgument("text") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStackEntry ->
                                val text = backStackEntry.arguments?.getString("text")
                                RegexGeneratorScreen(
                                    initialNotificationText = text,
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun startActionOverlayService() {
        val intent = Intent(this, ActionOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
