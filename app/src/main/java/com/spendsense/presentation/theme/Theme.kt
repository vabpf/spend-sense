package com.spendsense.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import android.os.Build
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════════════════════════════════════════════
// CYBER-PREMIUM THEME
// Fusion of Glassmorphism, Neo-Dark Mode, and High-Contrast Minimalism
// ═══════════════════════════════════════════════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    // Primary - Cyber Blue (sole action color)
    primary = CyberBlue,
    onPrimary = VoidBlack,
    primaryContainer = CyberBlue.copy(alpha = 0.15f),
    onPrimaryContainer = CyberBlue,

    // Secondary - Neon Mint (positive/income)
    secondary = NeonMint,
    onSecondary = VoidBlack,
    secondaryContainer = NeonMint.copy(alpha = 0.12f),
    onSecondaryContainer = NeonMint,

    // Tertiary - Neon Violets
    tertiary = NeonViolet,
    onTertiary = VoidBlack,
    tertiaryContainer = NeonViolet.copy(alpha = 0.12f),
    onTertiaryContainer = NeonViolet,

    // Error - Coral Red
    error = ErrorCoral,
    onError = Color.White,
    errorContainer = ErrorCoral.copy(alpha = 0.12f),
    onErrorContainer = ErrorCoral,

    // Background - Deep Charcoal
    background = DeepCharcoal,
    onBackground = TextPrimary,

    // Surface layers
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,

    // Outlines & borders
    outline = BorderSubtle,
    outlineVariant = BorderMedium,

    // Inverse for any inverted content
    inverseSurface = TextPrimary,
    inverseOnSurface = DeepCharcoal,
    inversePrimary = CyberBlueDark,
)

@Composable
fun SpendSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Only dark theme for Cyber-Premium aesthetic
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes
    ) {
        androidx.compose.material3.Surface(
            color = Color.Transparent,
            contentColor = colorScheme.onBackground,
            content = content
        )
    }
}
