package com.spendsense.presentation.settings

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.theme.DarkSurface
import com.spendsense.presentation.theme.VoidBlack
import kotlin.math.max

@Composable
fun CropBackgroundDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropConfirmed: (cropRect: RectF, additionalRotation: Float) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenAspect = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat().coerceAtLeast(1f)

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    val currentBitmap = remember(sourceBitmap, rotation) {
        val normRot = ((rotation % 360f) + 360f) % 360f
        if (normRot == 0f) {
            sourceBitmap
        } else {
            val matrix = android.graphics.Matrix().apply { postRotate(normRot) }
            Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Crop Wallpaper",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Drag and pinch to frame your wallpaper",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.70f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                rotation = (rotation + 90f) % 360f
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RotateRight,
                                contentDescription = "Rotate 90 degrees",
                                tint = CyberBlue
                            )
                        }

                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                rotation = 0f
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = "Reset transform",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Crop Viewport Frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .aspectRatio(screenAspect)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, CyberBlue, RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .onSizeChanged { viewportSize = it }
                        .pointerInput(currentBitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale

                                val vpW = viewportSize.width.toFloat().coerceAtLeast(1f)
                                val vpH = viewportSize.height.toFloat().coerceAtLeast(1f)
                                val srcW = currentBitmap.width.toFloat().coerceAtLeast(1f)
                                val srcH = currentBitmap.height.toFloat().coerceAtLeast(1f)

                                val curBaseScale = max(vpW / srcW, vpH / srcH)
                                val curDisplayedW = srcW * curBaseScale * newScale
                                val curDisplayedH = srcH * curBaseScale * newScale

                                val maxPanX = ((curDisplayedW - vpW) / 2f).coerceAtLeast(0f)
                                val maxPanY = ((curDisplayedH - vpH) / 2f).coerceAtLeast(0f)

                                offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val currentImageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }

                    Image(
                        bitmap = currentImageBitmap,
                        contentDescription = "Wallpaper preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentScale = ContentScale.Crop
                    )
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val vpW = viewportSize.width.toFloat().coerceAtLeast(1f)
                            val vpH = viewportSize.height.toFloat().coerceAtLeast(1f)

                            val srcW = currentBitmap.width.toFloat().coerceAtLeast(1f)
                            val srcH = currentBitmap.height.toFloat().coerceAtLeast(1f)

                            // Compute scale ratio matching ContentScale.Crop
                            val baseScale = max(vpW / srcW, vpH / srcH)
                            val totalScale = baseScale * scale

                            val displayedW = srcW * totalScale
                            val displayedH = srcH * totalScale

                            val viewLeftInDisplayed = (displayedW / 2f) - (vpW / 2f) - offsetX
                            val viewTopInDisplayed = (displayedH / 2f) - (vpH / 2f) - offsetY

                            val normLeft = (viewLeftInDisplayed / displayedW).coerceIn(0f, 1f)
                            val normTop = (viewTopInDisplayed / displayedH).coerceIn(0f, 1f)
                            val normW = (vpW / displayedW).coerceIn(0f, 1f - normLeft)
                            val normH = (vpH / displayedH).coerceIn(0f, 1f - normTop)

                            val cropRect = RectF(normLeft, normTop, normLeft + normW, normTop + normH)
                            onCropConfirmed(cropRect, rotation)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = VoidBlack)
                        Spacer(Modifier.width(8.dp))
                        Text("Set Wallpaper", color = VoidBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
