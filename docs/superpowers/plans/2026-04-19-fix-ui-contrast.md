# Fix UI Contrast: Black Text on Dark Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure consistent white/light-grey text contrast against dark navy/charcoal backgrounds by updating `Scaffold`, `Card`, and `Surface` components.

**Architecture:** Update Compose UI components to explicitly set `contentColor` when using transparent or glass backgrounds.

**Tech Stack:** Jetpack Compose, Material 3

---

### Task 1: Update HomeScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/home/HomeScreen.kt`

- [ ] **Step 1: Update Scaffold**
Add `contentColor = MaterialTheme.colorScheme.onBackground` to the `Scaffold` call.

- [ ] **Step 2: Update Card colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/home/HomeScreen.kt
git commit -m "style(ui): fix text contrast in HomeScreen"
```

### Task 2: Update CategoriesScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/categories/CategoriesScreen.kt`

- [ ] **Step 1: Update Scaffold**
Add `contentColor = MaterialTheme.colorScheme.onBackground` to the `Scaffold` call.

- [ ] **Step 2: Update Card colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/categories/CategoriesScreen.kt
git commit -m "style(ui): fix text contrast in CategoriesScreen"
```

### Task 3: Update WhitelistedAppsScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/whitelistedapps/WhitelistedAppsScreen.kt`

- [ ] **Step 1: Update Scaffold**
Add `contentColor = MaterialTheme.colorScheme.onBackground` to the `Scaffold` call.

- [ ] **Step 2: Update Card colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/whitelistedapps/WhitelistedAppsScreen.kt
git commit -m "style(ui): fix text contrast in WhitelistedAppsScreen"
```

### Task 4: Update RegexGeneratorScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorScreen.kt`

- [ ] **Step 1: Update Scaffold**
Add `contentColor = MaterialTheme.colorScheme.onBackground` to the `Scaffold` call.

- [ ] **Step 2: Update Card and OutlinedCard colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 3: Update Surface using GlassSurface**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to `Surface` call using `GlassSurface`.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/settings/RegexGeneratorScreen.kt
git commit -m "style(ui): fix text contrast in RegexGeneratorScreen"
```

### Task 5: Update AiProvidersScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/settings/AiProvidersScreen.kt`

- [ ] **Step 1: Update Scaffold**
Add `contentColor = MaterialTheme.colorScheme.onBackground` to the `Scaffold` call.

- [ ] **Step 2: Update Card colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/settings/AiProvidersScreen.kt
git commit -m "style(ui): fix text contrast in AiProvidersScreen"
```

### Task 6: Update SettingsScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/settings/SettingsScreen.kt`

- [ ] **Step 1: Update Scaffold**
Add `contentColor = MaterialTheme.colorScheme.onBackground` to the `Scaffold` call.

- [ ] **Step 2: Update Card colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 3: Update Surface using GlassSurface**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to `Surface` call using `GlassSurface`.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/settings/SettingsScreen.kt
git commit -m "style(ui): fix text contrast in SettingsScreen"
```

### Task 7: Update ChartsScreen.kt

**Files:**
- Modify: `app/src/main/java/com/spendsense/presentation/charts/ChartsScreen.kt`

- [ ] **Step 1: Update Card colors**
Add `contentColor = MaterialTheme.colorScheme.onSurface` to all `CardDefaults.cardColors` calls.

- [ ] **Step 2: Commit**
```bash
git add app/src/main/java/com/spendsense/presentation/charts/ChartsScreen.kt
git commit -m "style(ui): fix text contrast in ChartsScreen"
```
