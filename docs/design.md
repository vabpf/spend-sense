---
$schema: https://raw.githubusercontent.com/google-labs-code/design.md/main/schema.json
title: SpendSense
description: Personal finance tracker with cyber-premium aesthetic - fusion of glassmorphism, neo-dark mode, and high-contrast minimalism
version: 1.0.0
date: 2026-05-06

tokens:
  colors:
    # Primary - Cyber Blue (sole action color for CTAs & critical data)
    primary:
      $value: "#00D4FF"
      $description: "Primary action color, buttons, highlights"
    primary-light:
      $value: "#6EE7FF"
      $description: "Hover/pressed states"
    primary-dark:
      $value: "#00A8C6"
      $description: "Dark mode accent variant"

    # Secondary - Neon Mint (positive/income indicators)
    secondary:
      $value: "#34D399"
      $description: "Secondary accents, income indicators"
    secondary-container:
      $value: "rgba(52, 211, 153, 0.12)"
      $description: "Container background for secondary"

    # Tertiary - Neon Violet (category markers)
    tertiary:
      $value: "#A855F7"
      $description: "Tertiary accents, category markers"
    tertiary-container:
      $value: "rgba(168, 85, 247, 0.12)"
      $description: "Container background for tertiary"

    # Semantic - Error, Warning, Success
    error:
      $value: "#EF4444"
      $description: "Error states, destructive actions"
    warning:
      $value: "#F59E0B"
      $description: "Warning states"
    success:
      $value: "#10B981"
      $description: "Success states"

    # Foundation - Backgrounds (deep charcoal/black)
    background:
      $value: "#0A0A0F"
      $description: "Main app background"
    surface:
      $value: "#111118"
      $description: "Card and surface layer"
    surface-elevated:
      $value: "#1A1A24"
      $description: "Elevated components"
    surface-glass:
      $value: "#16161F"
      $description: "Glass card base"
    on-background:
      $value: "#FFFFFF"
      $description: "Primary text on background"

    # Text
    text-primary:
      $value: "#FFFFFF"
      $description: "High-priority typography"
    text-secondary:
      $value: "#B4B4BE"
      $description: "Secondary/muted text"
    text-tertiary:
      $value: "#6B7280"
      $description: "Disabled/hint text"

    # Borders
    border-subtle:
      $value: "rgba(255, 255, 255, 0.10)"
      $description: "10% white - subtle dividers"
    border-medium:
      $value: "rgba(255, 255, 255, 0.20)"
      $description: "20% white - medium borders"

    # Special effects
    glass-highlight:
      $value: "#2A2A35"
      $description: "Top gradient of glass cards"
    glass-shadow:
      $value: "#0D0D12"
      $description: "Bottom edge for depth"

    # Liquid Glass (API 33+)
    liquid-refraction:
      $value: 0.6
      $description: "Bending of background pixels for lens effect"
    liquid-frost:
      $value: "4dp"
      $description: "Blur intensity for frosted glass"
    liquid-dispersion:
      $value: 0.6
      $description: "Chromatic aberration (RGB color splitting)"
    liquid-saturation:
      $value: 1.8
      $description: "Saturation boost for background colors"
    liquid-edge:
      $value: 0.3
      $description: "Brightness of the glass edge highlight"
    liquid-curve:
      $value: 0.3
      $description: "Lens curvature (0.0 = flat, 1.0 = spherical)"

  typography:
    font-family:
      $value: "Sans-serif (system default)"
      $description: "Bold sans-serif with thick weights for large numbers"

    # Material3 Typography scale
    display-large:
      $value: "44px / Bold / -0.5px letter-spacing"
      $description: "Hero numbers, total amounts"
    display-medium:
      $value: "36px / Bold / 0px letter-spacing"
      $description: "Section headers"
    display-small:
      $value: "28px / Bold / 0px letter-spacing"
      $description: "Card titles"

    headline-large:
      $value: "24px / SemiBold / 0px letter-spacing"
      $description: "Screen titles"
    headline-medium:
      $value: "20px / SemiBold / 0px letter-spacing"
      $description: "Section subtitles"

    title-large:
      $value: "18px / Medium / 0px letter-spacing"
      $description: "Card headers"
    title-medium:
      $value: "16px / Medium / 0.1px letter-spacing"
      $description: "List item titles"

    body-large:
      $value: "16px / Normal / 0.3px letter-spacing"
      $description: "Primary body text"
    body-medium:
      $value: "14px / Normal / 0.2px letter-spacing"
      $description: "Secondary body text"
    body-small:
      $value: "12px / Normal / 0.3px letter-spacing"
      $description: "Caption text"

    label-large:
      $value: "14px / Medium / 0.1px letter-spacing"
      $description: "Buttons, chips"

  spacing:
    $unit: dp
    extra-small:
      $value: 4
      $description: "Tight gaps, icon padding"
    small:
      $value: 8
      $description: "Icon gaps, inline spacing"
    medium:
      $value: 16
      $description: "Standard padding, list gaps"
    large:
      $value: 24
      $description: "Section padding"
    extra-large:
      $value: 32
      $description: "Large gaps, screen padding"
    double-extra-large:
      $value: 48
      $description: "Hero padding, major sections"

  radii:
    extra-small:
      $value: 8
      $description: "Tiny elements, chips"
    small:
      $value: 12
      $description: "Buttons, small cards"
    medium:
      $value: 20
      $description: "Standard cards, inputs"
    large:
      $value: 28
      $description: "Large summary cards"
    extra-large:
      $value: 36
      $description: "Bottom sheets, modals"
    pill:
      $value: 50
      $description: "Pill selectors, circular inputs"
    squircle:
      $value: 24
      $description: "Premium card shape"

  elevation:
    $description: "Using surface color variations rather than shadows"
    level-1:
      $value: "DarkSurface (#111118)"
      $description: "Base cards"
    level-2:
      $value: "ElevatedSurface (#1A1A24)"
      $description: "Elevated components"
    glass:
      $value: "GlassSurface with gradient"
      $description: "Glassmorphism cards with highlight/shadow edges"

  motion:
    duration-fast:
      $value: 150
      $description: "Quick responses"
    duration-normal:
      $value: 200
      $description: "Standard transitions"
    duration-slow:
      $value: 300
      $description: "Emphasized transitions"
    easing-standard:
      $value: "cubic-bezier(0.4, 0, 0.2, 1)"
      $description: "Material standard easing"
    easing-emphasized:
      $value: "cubic-bezier(0.4, 0, 0.2, 1)"
      $description: "Emphasized open/close"

---

# SpendSense Design System

## Overview

SpendSense is a personal finance tracking Android app with a **Cyber-Premium** aesthetic—the fusion of glassmorphism, neo-dark mode, and high-contrast minimalism. The design language conveys trust, technological sophistication, and clarity for financial data.

### Visual Identity

### Color Philosophy


**Trust Blue + Profit Green on Deep Dark**

The color palette centers on Cyber Blue as the sole action color for all CTAs, buttons, and critical data visualization. This creates a focused, uncluttered interface where user attention naturally flows to important elements. Income and positive states use Neon Mint (green), while categories and highlights use Neon Violet, creating clear semantic differentiation without color overload.

The background uses deep charcoal (#0A0A0F) rather than pure black, providing richness and depth while maintaining OLED efficiency. Surfaces layer progressively from DarkSurface to ElevatedSurface, creating natural hierarchy through subtle lightness differences.

### Typography Voice

**Bold, Readable, Professional**

The typography uses system sans-serif fonts with bold weights for large display numbers—critical for showing financial totals at a glance. The weight progression from display (Bold) through headline (SemiBold) to body (Normal) creates clear visual hierarchy without relying on size alone.

Letter spacing is carefully tuned: tighter (-0.5px) for large displays where readability matters most, relaxed (0.3px) for body text where scannability wins.

### Shape Language

**Soft Geometric Premium**

All containers use high corner radii (12dp-36dp) creating a premium, organic feel. The "squircle" shape (24dp) serves as the signature card form—neither aggressive rectangle nor soft circle but something uniquely modern. Pill-shaped elements (50dp radius) distinguish interactive selectors from content containers.

### The Glass Effect

Glassmorphism appears through subtle gradient highlights on cards: lighter edge at the top (GlassHighlight #2A2A35) fading to darker edge at the bottom (GlassShadow #0D0D12). This creates depth without heavy shadows, with the glass surface color (#16161F) providing the main body.

## Component Behavior

### Buttons

- Primary buttons use Cyber Blue background with dark text for maximum contrast
- Touch targets minimum 48dp for accessibility
- Subtle opacity change on press (0.9) rather than dramatic shifts
- No layout shift during state transitions

### Cards

- Glass surface with subtle top-edge highlight
- 20dp-28dp corner radius depending on hierarchy
- No drop shadows—depth through surface color layering
- Hover/focus indicated by border glow (BorderMedium)

### Inputs

- 20dp corner radius (medium)
- Subtle border (BorderSubtle), prominent on focus (CyberBlue)
- 16dp font size to prevent zoom on mobile keyboards

### Bottom Sheets & Modals

- ExtraLarge radius (36dp) for premium feel
- Elevated surface color for hierarchy
- Drag handle indicator

## Anti-Patterns

- **Never use pure white backgrounds** — violates the dark-first aesthetic
- **Never use emojis as icons** — use Material Icons exclusively
- **Never instant state changes** — always animate (150-300ms)
- **Never invisible focus states** — accessibility requirement
- **Never low contrast text** — maintain 4.5:1 minimum

## Responsive Strategy

The design adapts through:
- Minimum touch target: 48dp
- Edge-to-edge content with system bar insets
- Single column layout priority
- Horizontal scrolling lists for overflow
- Bottom navigation for thumb-zone access