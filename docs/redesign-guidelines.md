# SpendSense — Full App Redesign Guidelines

> **Design Vision:** Cyber-Light Frosted Glass.  
> A premium hybrid aesthetic blending dark wallpaper liquid glass at the top with clean, ultra-responsive, high-contrast light surfaces for content-heavy lists, charts, and settings below.

---

## 1. Visual Architecture & Background Treatment

### 1.1 The "Top-to-Bottom Fade"
* **Top Header Area (Live Glass)**: The user-selected dark/cyberpunk wallpaper remains vibrant behind the status bar, top headers, and hero glass surfaces.
* **Vertical Gradient Scrim**: Behind the content layer, the wallpaper smoothly fades down into pure white (`#FFFFFF`) / soft ice-slate (`#F8FAFC`).
* **Content Zone**: The transaction list, charts, and settings forms live on this clean, high-contrast light surface. This completely eliminates readability issues and GPU rendering lag while preserving the brand's signature glassmorphism.

### 1.2 Elevation & Card Language
* **No Borders**: Remove hairline stroke borders (`border = null`) on white content cards.
* **Natural Drop Shadows**: Clean 2.dp elevation with subtle ambient shadowing (`ambientColor = Color.Black.copy(0.04f)`, `spotColor = Color.Black.copy(0.08f)`).
* **Curvature**: Standardize on `RoundedCornerShape(20.dp)` for list cards and settings grouped cards, `24.dp` for hero/chart cards, and `999.dp` for pills and chips.

### 1.3 Scroll Dissolve / Fade-Out Effect
* As items scroll up towards the transition boundary (or top of the viewport), they smoothly dissolve/disappear using an alpha gradient mask (`Modifier.graphicsLayer` with alpha or `drawWithContent` gradient blend).
* In **Charts Screen**: The cards dissolve when scrolled up near the top.
* In **Settings Screen**: The cards dissolve right at the transition line where the wallpaper fades into white, right below the pinned "Settings" title and subtitle.

---

## 2. Global Navigation & Chrome

### 2.1 Floating Liquid Glass Navigation Bar
* **Slimmer Profile (Y-size reduced)**:
  * Reduce bar height from `64.dp` down to **`54.dp – 56.dp`**.
  * Vertical padding inside the bar tightened to `6.dp`.
  * Icon size standardized at `22.dp`, typography at `labelSmall` (`11.sp`, `FontWeight.Medium` / `Bold`).
* **Visual Styling**:
  * Real-time Liquid Glass refraction (`liveBlur = true`, `useLens = true`).
  * Pure transparent base (`Color.Transparent`).
  * **No background chip** on the active tab: active tab highlights icon and label directly in `CyberBlue`, unselected in `Color(0xFF64748B)`.

### 2.2 Floating Action Button (FAB `+`)
* Placed at `Alignment.BottomEnd` floating above the bottom navigation bar.
* Compact `52.dp` circular pill with vibrant gradient (`CyberBlue` to `#00C6FF`) and crisp white `+` icon.

---

## 3. Screen 1: Home Screen (Transactions)

### 3.1 "Today at a glance" Summary Card
* **Glass Container**: Large frosted glass card with 14.dp blur and lens refraction over the top wallpaper.
* **Top Header**: `"Today at a glance"` label on the left, notification bell icon (`Icons.Rounded.Notifications`) on the right.
* **Hero Spending & Trend**:
  * Giant spending total (e.g. `60,000₫`) in bold slate (`#0F172A`).
  * Trend comparison pill next to total (e.g. `↗ 42%` in soft rose pill `#FEE2E2` with crimson text `#DC2626`).
  * Sub-label: `"You spent 103,000₫ yesterday"` in muted slate (`#64748B`).
* **Mini 7-Day Bar Chart** (Right Side of Card):
  * Mon–Sun vertical indicator bars proportional to daily spending.
  * Today's bar highlighted taller in vibrant blue (`CyberBlue`), other days in soft muted blue-gray (`#CBD5E1`).
* **Counter Pills** (Bottom Row):
  * `[ ≡ X entries ]` and `[ ✉ Y pending inbox ]` pills with subtle glass backdrop.

### 3.2 Search Bar & Category Chips
* **Search Bar**: Frosted glass pill with magnifying glass icon on the left and filter sliders icon on the right.
* **Category Filter Chips**:
  * `All` chip when selected: Solid vibrant blue pill with white text.
  * Category chips: Frosted glass pills with category-colored icon (Food, Shopping, Entertainment, Transport, etc.) + name.

### 3.3 Day Headers with Group Totals
* Two-column layout for each day group:
  * **Left**: Date label (`Today`, `Yesterday`, `Thursday, October 01`) in bold slate (`#475569`).
  * **Right**: Total spending for that day (e.g., `60,000₫`, `103,000₫`) in bold dark slate (`#0F172A`).

### 3.4 Compact Transaction Cards
* **Dimensions**: Compact vertical padding (`8.dp – 10.dp`), allowing 5–7 cards on screen at once.
* **Surface**: Pure white (`#FFFFFF`), `RoundedCornerShape(20.dp)`, **no border**, soft `2.dp` drop shadow.
* **Category Icon**: Circular pastel-tinted badge (`40.dp – 44.dp`) with category-themed icon.
* **Title & Sub-row**:
  * Merchant name in bold dark slate (`#0F172A`).
  * Metadata: `<CategoryName>` (tinted) `•` `<PaymentSource>` `•` `<Time>` in muted slate (`#64748B`).
* **Amount & Trailing Action**:
  * Bold crimson amount (`#DC2626`).
  * Subtle trailing chevron `>` (`Icons.Rounded.ChevronRight`) in `#94A3B8`.

---

## 4. Screen 2: Charts Screen (`ChartsScreen.kt`)
*(Reference: `Screenshot 2026-10-03 220608.png`)*

### 4.1 Background & Dissolve Behavior
* Same top wallpaper background fading down into white.
* **Dissolve Effect**: The cards smoothly dissolve/disappear as they are scrolled up towards the top status bar.

### 4.2 Top 4 KPI Cards (2×2 Grid — Frosted Glass)
These 4 cards keep the **Liquid Glass effect** over the top wallpaper:
1. **This Month (Top-Left)**:
   * Title `"This Month"` with a green calendar icon badge (`#DCFCE7`, icon `#16A34A`).
   * Large amount (e.g. `258,000₫`) in bold slate.
   * Trend text: `↓ 97% vs last month` in green `#16A34A`.
2. **Daily Average (Top-Right)**:
   * Title `"Daily Average"` with a purple chart badge (`#F3E8FF`, icon `#9333EA`).
   * Large amount (e.g. `86,000₫`) in bold slate.
   * Trend text: `↓ 71% vs last month` in green `#16A34A`.
3. **Top Category (Bottom-Left)**:
   * Title `"Top Category"`.
   * Row with pastel circular category icon badge, category title (`Food`), amount (`103,000₫`), and trailing chevron `>`.
4. **Biggest Spend (Bottom-Right)**:
   * Title `"Biggest Spend"`.
   * Row with pastel circular icon badge (e.g. fuel pump in blue `#DBEAFE`), merchant title (`xăng`), amount (`95,000₫`), and trailing chevron `>`.

### 4.3 Middle Section: "Spending by Category" Card (Solid White)
* **Surface**: Pure white (`#FFFFFF`), `RoundedCornerShape(24.dp)`, no border, soft drop shadow.
* **Header**: `"Spending by Category"` on left, dropdown filter pill `"This Month ⌵"` on right.
* **Donut Chart**: Left side shows a clean donut ring with total amount (e.g. `258,000₫`) and `"Total"` in the center.
* **Legend Breakdown**: Right side shows category dots, category names, amounts, and percentage shares (e.g. Food `103,000₫` 39%, Fuel `95,000₫` 36%, Other `60,000₫` 23%).

### 4.4 Bottom Section: "Monthly Spending" Card (Solid White)
* **Surface**: Pure white (`#FFFFFF`), `RoundedCornerShape(24.dp)`, no border, soft drop shadow.
* **Header**: `"Monthly Spending"` on left, segment toggle pill `[ Bar | Line ]` on right (active tab solid blue).
* **Stacked Bar Chart**: Multi-month bars (May, Jun, Jul, Aug, Sep, Oct) with Y-axis markers (`0`, `7.3Md`, `14.6Md`). The current month ("Oct") is highlighted with a soft column container and blue month pill.
* **Month Breakdown Table**: Below the chart, shows selected month total (e.g. `Oct Breakdown - 258,000₫`) with table rows for Source, Type, and Amount.

---

## 5. Screen 3: Settings Screen (`SettingsScreen.kt`)
*(Reference: `Screenshot 2026-10-03 221050.png`)*

### 5.1 Pinned Header
* Pinned directly on the dark top wallpaper backdrop.
* Title: **"Settings"** (Large bold white typography).
* Subtitle: **"Customize capture, AI, and defaults"** in soft translucent white/slate.
* **No back button** (as Settings is a main bottom navigation destination).

### 5.2 Dissolve Effect at the Transition Line
* Below the title/subtitle, the background wallpaper fades down into the white area.
* As the user scrolls up, settings cards **dissolve and fade out right at this transition line**, slipping gracefully underneath the pinned header.

### 5.3 Inset Grouped White Cards
Sections are formatted as clean white rounded cards (`RoundedCornerShape(20.dp)`), no border, soft drop shadow, with hairline row dividers (`#F1F5F9`):
1. **Permissions**:
   * *Notification Access* (Blue bell icon badge, status "Access granted", chevron `>`).
2. **Preferences**:
   * *Default Currency* (Mint green currency badge, e.g. `₫ VND — Vietnamese Dong`, chevron `>`).
   * *Daily Spent Summary* (Purple bell badge, description, Switch toggle).
   * *Report Delivery Time* (Orange clock badge, e.g. `Scheduled at 20:00`, chevron `>`).
3. **Appearance**:
   * *App Background* (Coral image badge, e.g. `Custom Photo — Pick a photo from your gallery`, chevron `>`).
4. **Configuration**:
   * *AI Providers* (Blue robot badge, `Configure AI models and API keys`, chevron `>`).
   * *Regex Generator* (Lavender wand/stars badge, `Create AI-powered regex patterns`, chevron `>`).
   * *Connected Apps* (Emerald grid badge, `Manage apps to monitor`, chevron `>`).
5. **Data & Security**:
   * *Export Data* / *Import Backup* / *Reset Data*.

---

## 6. Performance Standard (120 FPS Locked)
* **Zero Backdrop Shaders in Scroll Lists**: All cards inside `LazyColumn` (transactions, settings rows, category items) are 100% native Compose `Surface` elements.
* **Isolated Glass Elements**: Liquid Glass shaders are strictly limited to pinned top hero cards (Top summary on Home, 4 KPI cards on Charts) and the floating bottom navigation bar.
