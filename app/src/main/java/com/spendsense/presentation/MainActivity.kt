package com.spendsense.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendsense.domain.repository.CategoryRepository
import com.spendsense.presentation.home.HomeScreen
import com.spendsense.presentation.overlay.ActionOverlayService
import com.spendsense.presentation.settings.AiProvidersScreen
import com.spendsense.presentation.settings.RegexGeneratorScreen
import com.spendsense.presentation.settings.SettingsScreen
import com.spendsense.presentation.theme.SpendSenseTheme
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
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                onNavigateToSettings = {
                                    navController.navigate("settings")
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
                        
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToRegexGenerator = {
                                    navController.navigate("regex_generator")
                                },
                                onNavigateToAiProviders = {
                                    navController.navigate("ai_providers")
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
    
    private fun startActionOverlayService() {
        val intent = Intent(this, ActionOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
