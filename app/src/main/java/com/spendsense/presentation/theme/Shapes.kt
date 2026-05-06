package com.spendsense.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════════════
// CYBER-PREMIUM SHAPES
// Soft geometric approach - "squircle" and pill-shaped rounding
// All containers use high corner radius for that premium, organic feel
// ═══════════════════════════════════════════════════════════════════════════════

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),    // Tiny elements, chips
    small = RoundedCornerShape(12.dp),        // Buttons, small cards
    medium = RoundedCornerShape(20.dp),      // Standard cards, inputs
    large = RoundedCornerShape(28.dp),        // Large summary cards
    extraLarge = RoundedCornerShape(36.dp)     // Bottom sheets, modals
)

// Custom shapes for specific components - pill selectors
val PillSelector = RoundedCornerShape(50)
// Circular inputs for calculator/calendar
val CircularInput = RoundedCornerShape(50)
// Squircle - premium card shape (adjusted from perfect circle for card aspect)
val Squircle = RoundedCornerShape(24.dp)
