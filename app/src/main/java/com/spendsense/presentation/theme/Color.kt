package com.spendsense.presentation.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════════
// CYBER-PREMIUM COLOR PALETTE
// Fusion of Glassmorphism, Neo-Dark Mode, and High-Contrast Minimalism
// ═══════════════════════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────────────────────
// PRIMARY ACTION COLOR — Cyber Blue (sole "Action Color" for CTAs & critical data)
// ─────────────────────────────────────────────────────────────────────────────
val CyberBlue = Color(0xFF00D4FF)        // Primary action, buttons, highlights
val CyberBlueLight = Color(0xFF6EE7FF)      // Hover/pressed states
val CyberBlueDark = Color(0xFF00A8C6)      // Dark mode accent

// ─────────────────────────────────────────────────────────────────────────────
// ACCENT COLORS — Vibrant highlights for data visualization
// ─────────────────────────────────────────────────────────────────────────────
val NeonCyan = Color(0xFF22D3EE)          // Secondary accents
val NeonViolet = Color(0xFFA855F7)       // Tertiary/category markers
val NeonRose = Color(0xFFF43F5E)          // Warning/danger states
val NeonMint = Color(0xFF34D399)          // Positive/income indicators

// ─────────────────────────────────────────────────────────────────────────────
// SEMANTIC COLORS — Error, Success, Warning
// ─────────────────────────────────────────────────────────────────────────────
val SuccessGreen = Color(0xFF10B981)
val WarningAmber = Color(0xFFF59E0B)
val ErrorCoral = Color(0xFFEF4444)

// ─────────────────────────────────────────────────────────────────────────────
// FOUNDATION — Deep Charcoal/Black backgrounds
// ─────────────────────────────────────────────────────────────────────────────
val VoidBlack = Color(0xFF000000)          // True black for OLED
val DeepCharcoal = Color(0xFF0A0A0F)       // Main background
val DarkSurface = Color(0xFF111118)       // Card/surface layer
val ElevatedSurface = Color(0xFF1A1A24)   // Elevated components
val GlassSurface = Color(0xFF16161F)       // Glass card base

// ─────────────────────────────────────────────────────────────────────────────
// TEXT COLORS — Pure White for priority, gray for secondary
// ─────────────────────────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFFFFFFF)        // High-priority typography
val TextSecondary = Color(0xFFB4B4BE)     // Secondary/muted text
val TextTertiary = Color(0xFF6B7280)       // Disabled/hint text

// ─────────────────────────────────────────────────────────────────────────────
// BORDER & CHROME — Subtle dividers
// ─────────────────────────────────────────────────────────────────────────────
val BorderSubtle = Color(0x1AFFFFFF)      // 10% white
val BorderMedium = Color(0x33FFFFFF)       // 20% white
val BorderStrong = Color(0x4DFFFFFF)     // 30% white

// ─────────────────────────────────────────────────────────────────────────────
// GLASSMORPHISM HIGHLIGHTS — For glass effect gradient tops
// ─────────────────────────────────────────────────────────────────────────────
val GlassHighlight = Color(0xFF2A2A35)    // Top gradient of glass cards
val GlassShadow = Color(0xFF0D0D12)         // Bottom edge for depth
