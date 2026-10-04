# Dots — Lock-screen progress wallpaper (Android, Pixel 7a)

An Android app that draws a minimalist dot-grid wallpaper and sets it as the **lock screen only**, redrawing it automatically every day. Three modes: **Year**, **Life** and **Goal**. Dark, aesthetic, and tiny. Personal use, sideloaded. "Dots" is a working name; rename freely.

`render.py` (next to this file) is the **visual reference**. It draws exactly what the app should draw, and the previews (`lock-*.png`, `wall-*.png`) show the target look on my lock screen. Port its geometry to Kotlin rather than inventing a new layout.

## How to work (instructions for Claude Code)

- Go phase by phase (section 6). After each phase: `./gradlew assembleDebug testDebugUnitTest lintDebug`, then `git commit`. Never start a phase while the build is red.
- Use the latest **stable** AGP, Kotlin, Compose BOM, WorkManager and DataStore. Check current docs for exact APIs.
- No device access. Things that need the phone go in the manual QA list (section 9).
- If the JDK or Android SDK is missing, stop and tell me what to install.

## 1. Fixed decisions

- Native Kotlin, single module `app`, package `dev.me.dots` (or what I chose in Android Studio, check `build.gradle.kts`).
- `minSdk 31`, `compileSdk`/`targetSdk` latest stable.
- UI: **Jetpack Compose + Material3** for the settings screen only, forced dark theme with the fixed palette (section 4). No dynamic color.
- Drawing: plain `android.graphics.Canvas` into a `Bitmap`. **No internet.** No `INTERNET` permission at all.
- Storage: Jetpack DataStore (Preferences).
- Allowed dependencies: Compose BOM (ui, foundation, material3, ui-tooling-preview as debugImplementation only), activity-compose, lifecycle-runtime-ktx, core-ktx, work-runtime-ktx, datastore-preferences. Nothing else; no image libraries, no Material icons extended (draw the few icons as vectors).
- Size target: release APK under about 4 MB (section 8).

## 2. Features

### Modes
1. **Year**: one dot per day of the current year. Past days filled, today in accent, future dim. Footer: `88d left · 76%`.
   - Style **Grid**: auto-fit columns to the area (see `best_cols` in `render.py`).
   - Style **Months**: 12 month blocks in 3 columns × 4 rows, each a mini 7-column calendar with a month label. The current month's label is in the accent color. Week starts Monday (setting: Monday or Sunday).
2. **Life**: one dot per week of life, **52 columns = one year per row**. Inputs: birthdate and expected lifespan (default 80 years, range 50–100). Footer: `57y left · 28% lived`.
3. **Goal**: one dot per day from a start date to a deadline. Inputs: title, start date (default today), deadline. Title shows above the grid. Footer: `88d left · 28%`. Fixed 14 columns, max dot pitch so short goals don't get huge dots.
   - Several goals can be saved; one is **active** at a time. When a deadline passes: show `Done` (or `Missed` if I mark it so) in the footer for 3 days, then fall back to Year mode automatically.

### Look and customisation (all with a live preview)
- Accent presets: Clay `#D97757` (default), Ember `#E5532D`, Mint `#5BC8A8`, Sky `#5B9BD5`, Lilac `#A58BDB`, Mono (white). Custom hex input too.
- Background: Charcoal `#0D0D0C` (default) or Pure black `#000000` (OLED).
- Dot shape: Circle (default) or Rounded square.
- Density: Comfortable (dot = 72% of pitch, default) or Compact (60%).
- Footer text on/off; Goal title on/off.
- **Placement band**: two sliders for the grid's top and bottom (default 37.5% and 90.5% of the screen height), side margin slider (default 8.5%). The preview shows grey ghost shapes for my clock, At a Glance lines, fingerprint and bottom shortcuts (as in `lock-*.png`) so I can see clearances.
- Apply to: Lock screen only (default) or Lock + Home.

### Automatic updates
- Redraw and re-apply shortly after midnight every day, after reboot, and when the time, date or time zone changes. Applying is idempotent: if today's image is already set, skip.

## 3. Rendering spec (port of `render.py`)

- Canvas size: `WindowManager.getCurrentWindowMetrics().bounds` in **portrait** (Pixel 7a: 1080×2400). Also call `WallpaperManager.suggestDesiredDimensions` is **not** needed; draw at the screen size and pass `visibleCropHint = null`.
- Band: `x0 = side*W`, `y0 = top*H`, `w = (1-2*side)*W`, `h = (bottom-top)*H`.
- Header (Goal title) and footer each reserve 56 px of height when shown.
- Grid fitting: for Year-grid, try columns 7..60, `rows = ceil(n/cols)`, `pitch = min(gw/cols, gh/rows)`, keep the largest pitch. Life uses 52 columns, Goal 14. Cap pitch at 70 px (Goal 58 px).
- Center the grid (plus header and footer) inside the band.
- Dot radius = pitch × 0.36 (Comfortable) or × 0.30 (Compact). **Today's dot** radius = `max(r × 1.35, r + 5px)`, so it stands out even in the dense Life grid.
- Colors: past `#ECEAE4`, future `#2A2927`, muted text `#7C7A74`, goal title `#BDBAB3`, accent as chosen.
- Footer: centered, 32 px medium sans-serif; left part (`88d left`) in accent, ` · ` and the right part in muted. Baseline 50 px below the grid.
- Months style: block width = band width / 3; pitch = `min((cellW - 40px)/7, (h - footer)/4/7.6)`; label 26 px semibold, 46 px label row; vertical gap between month rows up to 60 px. See `year_months()`.
- Font: `Typeface.create("sans-serif-medium", Typeface.NORMAL)`; anti-aliasing on for paints.
- All dates use the device's local time zone (`java.time.LocalDate.now()`).
- Pure function: `WallpaperRenderer.render(spec: RenderSpec, size: Size, today: LocalDate): Bitmap`, used by both the preview and the worker so they always match.

## 4. Settings app look

- Dark only. Background `#0D0D0C`, surfaces `#1A1A19`, text `#ECEAE4`, muted `#7C7A74`, accent = current accent.
- Layout (single screen, scrollable):
  1. Large live preview at the top: the rendered bitmap inside a rounded phone outline, with the ghost overlay toggle.
  2. Mode switcher: segmented control `Year · Life · Goal`.
  3. Mode settings (Year style; Life birthdate + lifespan; Goal list with add/edit/activate).
  4. Look: accent chips, background, shape, density, footer/title toggles.
  5. Placement sliders.
  6. Big **Apply** button, plus a line: `Auto-updates daily · last applied 00:05`.
- Use Material3 date pickers. Keep the UI calm: generous spacing, no cards inside cards.

## 5. Project structure

```
app/src/main/java/dev/me/dots/
  MainActivity.kt          Compose settings screen
  ui/                      Preview, ModeSwitcher, GoalEditor, LookSection, PlacementSection
  model/                   RenderSpec, Mode, YearStyle, Goal, Look, Placement
  data/SettingsRepo.kt     DataStore read/write, Flow<RenderSpec>
  render/
    WallpaperRenderer.kt   Canvas drawing (pure)
    GridMath.kt            best-cols, counts, progress text (pure, unit-tested)
  apply/
    WallpaperApplier.kt    render + WallpaperManager.setBitmap(..., FLAG_LOCK [| FLAG_SYSTEM])
    DailyWorker.kt         CoroutineWorker: apply, then schedule the next run
    Scheduler.kt           schedules the next run at 00:05 local time
    TimeChangeReceiver.kt  BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, DATE_CHANGED
app/src/test/              GridMath tests
```

## 6. Phases

### Phase 1 — Scaffold
- Gradle Kotlin DSL + version catalog, Compose enabled, `.gitignore`, `git init`.
- Apply the size config (section 8) from the start.
- Manifest: `SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED`. `allowBackup=false` is not needed (no secrets); allow backup of settings.
- Dark Compose theme with the palette. Empty screen titled "Dots".
- **Done when:** build green.

### Phase 2 — Grid math and models
- `GridMath`: day index for Year (leap years), weeks lived for Life, days in Goal range, progress %, "left" text (`88d left`, `57y left`), best columns.
- Models and `SettingsRepo` with sensible defaults (Year / Grid / Clay / Charcoal / Circle / Comfortable / band 0.375–0.905 / side 0.085 / lock only).
- Unit tests: leap year (2028 has 366 dots), Dec 31 and Jan 1, Life with lifespan reached, goal where today < start / today > end / start == end, % rounding, best-cols results matching `render.py` for 365 and 366 at 1080×2400.
- **Done when:** tests pass.

### Phase 3 — Renderer and preview
- `WallpaperRenderer` per section 3, all modes and styles, both shapes and densities.
- Compose preview: `Image(bitmap.asImageBitmap())` scaled into a phone frame; ghost overlay drawn on top in the preview only (never in the wallpaper).
- Render off the main thread; debounce slider changes by about 100 ms.
- Debug-only button: "Save PNG to Pictures" so I can compare against `wall-*.png`.
- **Done when:** build green; a JVM-free sanity test isn't possible for Canvas, so add the comparison step to manual QA.

### Phase 4 — Settings UI
- Everything in section 2 "Look and customisation" and the mode settings, per section 4.
- Goal editor: title (max 40 chars), start, deadline (must be after start), list with activate/delete.
- **Done when:** build green; every setting changes the preview immediately.

### Phase 5 — Apply and daily updates
- `WallpaperApplier.apply()`: render at the screen size, `WallpaperManager.getInstance(ctx).setBitmap(bmp, null, true, FLAG_LOCK)` (add `FLAG_SYSTEM` for Lock + Home). Store `lastAppliedDate` and a hash of the spec; skip if both match today.
- `Scheduler`: enqueue a unique `OneTimeWorkRequest` (`ExistingWorkPolicy.REPLACE`) with initial delay until the next **00:05** local time. `DailyWorker` applies, then calls the scheduler again. No exact-alarm permission.
- `TimeChangeReceiver`: on boot/time/time-zone/date change, apply now and reschedule.
- Apply button: apply now, schedule, show a short confirmation.
- Goal expiry fallback (section 2).
- **Done when:** build green; unit test the "next 00:05" calculation (including across DST, even though India has none) and the skip logic with fakes.

### Phase 6 — Polish and size
- Match the previews: compare a debug-exported PNG against `wall-year.png` etc. Fix spacing differences.
- App icon: an original icon (e.g. a 3×3 dot grid with one accent dot) as an adaptive vector with a monochrome layer for Pixel themed icons.
- `assembleRelease`, report APK size, trim if over about 4 MB.

### Phase 7 — README
- Build, sign for personal use, `adb install -r ...`, how to set it up, how to change placement if the clock style changes, known limits.

## 7. Known Pixel behaviours to handle

- The Pixel lock-screen clock changes size (large when there are no notifications, small when there are). Default band starts below the large clock + At a Glance; the placement sliders cover other setups.
- Setting a lock-only wallpaper makes home and lock different; that's intended. If I pick Lock + Home, set both flags in one call.
- Android may restrict background work under battery saver; the date-change and boot receivers plus "apply on app open" cover missed runs.

## 8. Build and size configuration

```kotlin
android {
    defaultConfig { minSdk = 31; androidResources { localeFilters += listOf("en") } }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin") }
}
```

## 9. Manual QA (I run these on the Pixel 7a)

1. Install, open, see the Year preview with the ghost overlay matching my lock screen.
2. Apply. Lock the phone: the grid sits below the weather lines, runs under the fingerprint, and clears the bottom shortcuts. The home screen wallpaper is unchanged.
3. Switch to Months, Life (enter birthdate) and a Goal; each preview matches the reference PNGs; apply each once.
4. Change accent, background, shape, density and placement; preview and applied wallpaper match.
5. Set the phone date forward a day (or wait past midnight): the wallpaper updates within a few minutes after 00:05, or on unlock after a date change.
6. Reboot: the wallpaper is still correct and the next update is scheduled (`adb shell dumpsys jobscheduler | grep dots`).
7. Create a goal that ends tomorrow, move the date past it: `Done` shows, and after 3 days it falls back to Year.
