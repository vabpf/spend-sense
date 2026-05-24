package com.spendsense.presentation.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.spendsense.presentation.theme.GlassSurface

/**
 * A Material3-style dialog with the liquid glass effect applied to its surface.
 *
 * Replaces [androidx.compose.material3.AlertDialog] with glass styling —
 * uses real Liquid Glass sampling on API 33+ and falls back to Haze on older devices.
 *
 * @see [Liquid Glass Guide](docs/LIQUID_GLASS.md)
 */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable RowScope.() -> Unit = {},
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    containerColor: Color = GlassSurface.copy(alpha = 0.9f),
    borderAlpha: Float = 0f
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .glassEffect(
                    shape = shape,
                    containerColor = containerColor,
                    borderAlpha = borderAlpha
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (title != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        title()
                    }
                }
                if (text != null) {
                    text()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton()
                    Spacer(Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}
