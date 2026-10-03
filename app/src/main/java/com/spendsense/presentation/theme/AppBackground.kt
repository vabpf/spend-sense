package com.spendsense.presentation.theme

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.spendsense.R
import androidx.compose.runtime.compositionLocalOf
import java.io.File

val LocalAppBackgroundTheme = compositionLocalOf { "CYBERPUNK_DEFAULT" }
val LocalCustomBackgroundPath = compositionLocalOf<String?> { null }

enum class AppBackgroundOption(
    val key: String,
    val title: String,
    val description: String
) {
    CYBERPUNK("CYBERPUNK_DEFAULT", "Cyberpunk", "Futuristic neon city (Default)"),
    DEEP_SPACE("DEEP_SPACE", "Deep Space", "Cosmic violet nebula gradient"),
    CYBER_NEON("CYBER_NEON", "Cyber Neon", "Dark oceanic teal and cyan glow"),
    OLED_BLACK("OLED_BLACK", "OLED Black", "Minimalist pitch black for battery saving"),
    CUSTOM("CUSTOM_IMAGE", "Custom Photo", "Pick a photo from your gallery")
}

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    themeKey: String = LocalAppBackgroundTheme.current,
    customImagePath: String? = LocalCustomBackgroundPath.current
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (themeKey) {
            AppBackgroundOption.DEEP_SPACE.key -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF070514),
                                    Color(0xFF100B29),
                                    Color(0xFF19113B),
                                    Color(0xFF09061A)
                                )
                            )
                        )
                )
            }
            AppBackgroundOption.CYBER_NEON.key -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF040D18),
                                    Color(0xFF081F2F),
                                    Color(0xFF0B2D3A),
                                    Color(0xFF05111B)
                                )
                            )
                        )
                )
            }
            AppBackgroundOption.OLED_BLACK.key -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF000000),
                                    Color(0xFF08090C),
                                    Color(0xFF0D0E12)
                                )
                            )
                        )
                )
            }
            AppBackgroundOption.CUSTOM.key -> {
                val customFile = customImagePath?.let { File(it) }
                val lastModified = customFile?.takeIf { it.exists() }?.lastModified() ?: 0L
                val bitmap = remember(customImagePath, lastModified) {
                    if (customFile != null && customFile.exists()) {
                        BitmapFactory.decodeFile(customFile.absolutePath)?.asImageBitmap()
                    } else null
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback to default
                    Image(
                        painter = painterResource(id = R.drawable.bg_pexel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            else -> {
                // CYBERPUNK (Default)
                Image(
                    painter = painterResource(id = R.drawable.bg_pexel),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Adaptive dark overlay to maintain legibility and high contrast for text and cards
        val overlayModifier = when (themeKey) {
            AppBackgroundOption.CUSTOM.key -> {
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.52f),
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.66f)
                        )
                    )
                )
            }
            AppBackgroundOption.OLED_BLACK.key -> null
            AppBackgroundOption.DEEP_SPACE.key,
            AppBackgroundOption.CYBER_NEON.key -> Modifier.background(Color.Black.copy(alpha = 0.15f))
            else -> Modifier.background(Color.Black.copy(alpha = 0.30f))
        }

        if (overlayModifier != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(overlayModifier)
            )
        }
    }
}
