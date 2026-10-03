# SpendSense — Android App

## Build & Test — Remote Only (GitHub Actions)

**CRITICAL RULE: DO NOT RUN GRADLE TASKS LOCALLY (`./gradlew`)**. All building, testing, and assembling must be performed remotely via GitHub Actions to preserve local CPU and RAM.

### 1. Trigger Remote Build or Run Tests
- **Run Unit Tests Only**:
  ```bash
  gh workflow run test.yml
  ```
- **Build Release APK Only** (fastest, skips tests):
  ```bash
  gh workflow run build.yml -f target=build
  ```
- **Run Both (Test & Build APK)**:
  ```bash
  gh workflow run build.yml -f target=all
  ```
- **Build Release APK & Publish GitHub Release**:
  ```bash
  gh workflow run build.yml -f target=build -f publish_release=true --ref main
  ```
- **Publish Release with Custom Tag**:
  ```bash
  gh workflow run build.yml -f target=build -f publish_release=true -f tag_name=v1.0.12 --ref main
  ```
- **Check status / watch progress**:
  ```bash
  gh run list --limit 3
  gh run watch <run_id> --exit-status
  ```

### 2. Download Built APK to Local Machine
Once the GitHub Action completes, download `SpendSense.apk` directly into the Windows Downloads folder:
```bash
rm -f /mnt/c/Users/vuong/Downloads/SpendSense.apk && gh run download <run_id> -n SpendSense-Release-APK -D /mnt/c/Users/vuong/Downloads
```

### 3. Install to Device (via ADB)
```bash
/mnt/d/Apps/Android/Sdk/platform-tools/adb.exe install -r /mnt/c/Users/vuong/Downloads/SpendSense.apk
```

---

## Project Structure & Architecture

- **Platform:** Android (Min SDK 26, Target SDK 35)
- **Language:** 100% Kotlin with Coroutines & Flow
- **UI:** Jetpack Compose (Material 3) with custom Cyber-Premium Liquid Glass design system
- **Architecture:** Clean Architecture (Presentation, Domain, Data) with Hilt DI and Room Database
- **Modules:** Single-module Android app (`app/`)
- **Key Patterns:**
  - **Sibling Background Architecture (120 FPS):** All screens place `AppBackground()` inside `Box(Modifier.fillMaxSize().liquefiable(screenLiquidState))` as Sibling 1, and place content (`LazyColumn`, `Column`, top bars) completely outside `liquefiable` as Sibling 2 to avoid recursive sampling jank.
  - **Dynamic Multi-AI Providers:** Supports OpenRouter, OpenAI, Anthropic Claude, Gemini, Ollama, and custom OpenAI-compatible endpoints with encrypted key storage.
  - **Notification Interception:** `TransactionNotificationListener` monitors whitelisted banking apps, applies cached Regex patterns, and triggers the `ActionOverlayService`.

---

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **spend-sense** (2877 symbols, 6030 relationships, 110 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/spend-sense/context` | Codebase overview, check index freshness |
| `gitnexus://repo/spend-sense/clusters` | All functional areas |
| `gitnexus://repo/spend-sense/processes` | All execution flows |
| `gitnexus://repo/spend-sense/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
