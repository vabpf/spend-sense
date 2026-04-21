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

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = TextPrimary,
    primaryContainer = ElectricIndigo.copy(alpha = 0.2f),
    onPrimaryContainer = TextPrimary,
    secondary = MintGreen,
    onSecondary = TextPrimary,
    secondaryContainer = MintGreen.copy(alpha = 0.15f),
    onSecondaryContainer = TextPrimary,
    tertiary = Amber,
    onTertiary = TextPrimary,
    tertiaryContainer = Amber.copy(alpha = 0.15f),
    onTertiaryContainer = TextPrimary,
    error = CoralRed,
    onError = TextPrimary,
    errorContainer = CoralRed.copy(alpha = 0.2f),
    onErrorContainer = TextPrimary,
    background = DeepCharcoal,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface.copy(alpha = 0.7f),
    onSurfaceVariant = TextSecondary,
    outline = TextSecondary.copy(alpha = 0.5f),
    outlineVariant = TextSecondary.copy(alpha = 0.3f),
)

@Composable
fun SpendSenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
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
        typography = Typography
    ) {
        androidx.compose.material3.Surface(
            color = Color.Transparent,
            contentColor = colorScheme.onBackground,
            content = content
        )
    }
}
