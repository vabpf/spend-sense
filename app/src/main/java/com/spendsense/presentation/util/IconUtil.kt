package com.spendsense.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Restaurant" -> Icons.Rounded.Restaurant
        "DirectionsCar" -> Icons.Rounded.DirectionsCar
        "ShoppingCart" -> Icons.Rounded.ShoppingCart
        "Movie" -> Icons.Rounded.Movie
        "Receipt" -> Icons.Rounded.Receipt
        "LocalHospital" -> Icons.Rounded.LocalHospital
        "MoreHoriz" -> Icons.Rounded.MoreHoriz
        "Fastfood" -> Icons.Rounded.Fastfood
        "Home" -> Icons.Rounded.Home
        "LocalGasStation" -> Icons.Rounded.LocalGasStation
        "Flight" -> Icons.Rounded.Flight
        "Pets" -> Icons.Rounded.Pets
        "FitnessCenter" -> Icons.Rounded.FitnessCenter
        "School" -> Icons.Rounded.School
        else -> Icons.Rounded.Category
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
