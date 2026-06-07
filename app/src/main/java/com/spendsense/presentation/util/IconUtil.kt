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
        "Coffee" -> Icons.Rounded.Coffee
        "ShoppingBag" -> Icons.Rounded.ShoppingBag
        "MusicNote" -> Icons.Rounded.MusicNote
        "Train" -> Icons.Rounded.Train
        "Laptop" -> Icons.Rounded.Laptop
        "Spa" -> Icons.Rounded.Spa
        "AccountBalance" -> Icons.Rounded.AccountBalance
        "Build" -> Icons.Rounded.Build
        "CardGiftcard" -> Icons.Rounded.CardGiftcard
        "CameraAlt" -> Icons.Rounded.CameraAlt
        "Weekend" -> Icons.Rounded.Weekend
        "BeachAccess" -> Icons.Rounded.BeachAccess
        "Pool" -> Icons.Rounded.Pool
        "ChildCare" -> Icons.Rounded.ChildCare
        "Store" -> Icons.Rounded.Store
        "Kitchen" -> Icons.Rounded.Kitchen
        "Business" -> Icons.Rounded.Business
        "Wifi" -> Icons.Rounded.Wifi
        "Phone" -> Icons.Rounded.Phone
        "Work" -> Icons.Rounded.Work
        "VolunteerActivism" -> Icons.Rounded.VolunteerActivism
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
    "Pets", "FitnessCenter", "School",
    "Coffee", "ShoppingBag", "MusicNote", "Train", "Laptop",
    "Spa", "AccountBalance", "Build", "CardGiftcard", "CameraAlt",
    "Weekend", "BeachAccess", "Pool", "ChildCare", "Store",
    "Kitchen", "Business", "Wifi", "Phone", "Work",
    "VolunteerActivism"
)
