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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
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
import com.spendsense.presentation.util.SpendSenseTopBar
import com.spendsense.presentation.util.glassEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.spendsense.presentation.util.fadingEdge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val fadeEnd = statusBarPadding + 98.dp
    val density = LocalDensity.current
    val fadeEndPx = with(density) { fadeEnd.toPx() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .offset(y = (-24).dp)
                    .size(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.20f)
                    )
                    .background(
                        Brush.linearGradient(listOf(CyberBlue, Color(0xFF00C6FF))),
                        shape = CircleShape
                    )
                    .clickable { viewModel.showAddEditDialog(null) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add Category",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Background scrim: transparent at top wallpaper, smoothly fades directly from 0 to 100 into #F8FAFC
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFF8FAFC)),
                            startY = 0f,
                            endY = fadeEndPx
                        )
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .fadingEdge(
                        topFadeStart = 0.dp,
                        topFadeHeight = fadeEnd
                    ),
                contentPadding = PaddingValues(
                    start = 16.dp, 
                    end = 16.dp, 
                    top = fadeEnd + 6.dp, 
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

            // Pinned Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = statusBarPadding + 10.dp,
                        start = 12.dp,
                        end = 20.dp,
                        bottom = 10.dp
                    )
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.65f),
                                offset = Offset(0f, 2f),
                                blurRadius = 10f
                            )
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Manage expense and income categories",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.55f),
                                offset = Offset(0f, 1f),
                                blurRadius = 6f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
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

@Composable
fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.03f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
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
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A),
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
