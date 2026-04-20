package com.spendsense.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Default.Restaurant
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "Movie" -> Icons.Default.Movie
        "Receipt" -> Icons.Default.Receipt
        "LocalHospital" -> Icons.Default.LocalHospital
        "MoreHoriz" -> Icons.Default.MoreHoriz
        "Fastfood" -> Icons.Default.Fastfood
        "Home" -> Icons.Default.Home
        "LocalGasStation" -> Icons.Default.LocalGasStation
        "Flight" -> Icons.Default.Flight
        "Pets" -> Icons.Default.Pets
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "School" -> Icons.Default.School
        else -> Icons.Default.Category
    }
}

fun parseColor(colorHex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }
}

val availableColors = listOf(
    "#F44336", "#E91E63", "#9C27B0", "#673AB7",
    "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
    "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
    "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
)

val availableIcons = listOf(
    "Category", "Restaurant", "DirectionsCar", "ShoppingCart",
    "Movie", "Receipt", "LocalHospital", "MoreHoriz",
    "Fastfood", "Home", "LocalGasStation", "Flight",
    "Pets", "FitnessCenter", "School"
)
