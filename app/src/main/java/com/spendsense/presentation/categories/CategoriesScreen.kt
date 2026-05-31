package com.spendsense.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.domain.model.Category
import com.spendsense.presentation.theme.GlassSurface
import com.spendsense.presentation.theme.CyberBlue
import com.spendsense.presentation.util.GlassAlertDialog
import com.spendsense.presentation.util.availableColors
import com.spendsense.presentation.util.availableIcons
import com.spendsense.presentation.util.getCategoryIcon
import com.spendsense.presentation.util.parseColor
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import com.spendsense.presentation.util.LocalLiquidState
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val categoriesLiquidState = rememberLiquidState()
 
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            CompositionLocalProvider(LocalLiquidState provides categoriesLiquidState) {
                Box(
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .size(56.dp)
                        .glassEffect(
                            shape = FloatingActionButtonDefaults.shape,
                            containerColor = GlassSurface.copy(alpha = 0.15f),
                            borderAlpha = 0.25f
                        )
                        .border(
                            width = 1.dp,
                            color = CyberBlue,
                            shape = FloatingActionButtonDefaults.shape
                        )
                        .clickable { viewModel.showAddEditDialog(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add Category",
                        tint = CyberBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .liquefiable(categoriesLiquidState)
            ) {
                Image(
                    painter = painterResource(id = com.spendsense.R.drawable.bg_pexel),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.30f))
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, 
                        end = 16.dp, 
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp, 
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.categories) { category ->
                        CategoryItem(
                            category = category,
                            onEdit = { viewModel.showAddEditDialog(category) },
                            onDelete = { viewModel.deleteCategory(category) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background,
                            0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            0.75f to MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                            1.0f to Color.Transparent
                        )
                    )
                    .align(Alignment.TopCenter)
            )

            CompositionLocalProvider(LocalLiquidState provides categoriesLiquidState) {
                SpendSenseTopBar(
                    title = "Categories",
                    onNavigationClick = onNavigateBack,
                    navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack
                )
            }
        }
 
        if (state.isAddingOrEditing) {
            AddEditCategoryDialog(
                initialCategory = state.editingCategory,
                onDismiss = viewModel::hideAddEditDialog,
                onSave = viewModel::saveCategory
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassEffect(
                shape = MaterialTheme.shapes.large,
                containerColor = GlassSurface.copy(alpha = 0.8f),
                borderAlpha = 0.24f
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconVector = getCategoryIcon(category.iconName)
            val iconPainter = rememberVectorPainter(iconVector)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        // Draw colored background (destination)
                        drawRect(color = parseColor(category.colorHex))
                        // Draw icon as cutout (removes destination where icon is opaque)
                        val iconSize = size * 0.6f
                        val offsetX = (size.width - iconSize.width) / 2f
                        val offsetY = (size.height - iconSize.height) / 2f
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                xfermode = android.graphics.PorterDuffXfermode(
                                    android.graphics.PorterDuff.Mode.DST_OUT
                                )
                            }
                            canvas.nativeCanvas.saveLayer(null, paint)
                        }
                        translate(left = offsetX, top = offsetY) {
                            with(iconPainter) {
                                draw(size = iconSize, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black))
                            }
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.restore()
                        }
                    }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (!category.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditCategoryDialog(
    initialCategory: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(initialCategory?.iconName ?: availableIcons.first()) }
    var selectedColor by remember { mutableStateOf(initialCategory?.colorHex ?: availableColors.first()) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCategory != null) "Edit Category" else "Add Category") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Select Icon", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableIcons) { iconName ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(iconName),
                                contentDescription = null,
                                tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text("Select Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableColors) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(parseColor(colorHex))
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
