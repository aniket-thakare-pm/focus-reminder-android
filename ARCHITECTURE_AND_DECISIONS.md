# Architecture, System Overview, and Decision Records (ADRs)

> **App Version**: `1.13.0` (versionCode `20`)  
> **Package**: `com.productivity.focusreminder`  
> **Min SDK**: 29 (Android 10) | **Target SDK**: 36 (Android 16)  
> **Tech Stack**: Kotlin, Jetpack Compose, Coroutines/Flow, SQLite, Android `UsageStatsManager` / `UsageEvents`.

---

## 1. System Overview & Core Layer Responsibilities

```
+-----------------------------------------------------------------------------------+
|                                  JETPACK COMPOSE UI                                |
|  [DashboardScreen]     [AnalyticsScreen]     [UsageLogsScreen]   [AppSelection]   |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                  VIEWMODEL LAYER                                   |
|  MainScreenViewModel    AnalyticsViewModel    UsageLogsViewModel   DashboardState  |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                 REPOSITORY LAYER                                  |
|  AnalyticsRepository        UsageLogRepository         AppRestrictionRepository   |
|  - SQLite (focus_sessions)  - Hardware Audit Trail     - Controlled packages      |
|  - Sector aggregation       - Dynamic filters/merging  - Cooldowns / locks        |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                            PLATFORM & DETECTION ENGINE                            |
|  UsageMonitorService (1.2s Loop)        ForegroundAppDetector (AOSP Bridge)       |
|  - Sticky Chronometer Notification      - queryReconciledDailyUsageMap            |
|  - 2-Minute Warnings / Lock Triggers    - getForegroundPackageName                |
|  - Active session timers / cooldowns    - queryTotalScreenTimeToday               |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                       SECURITY, FILTERS, & SYSTEM GUARDS                          |
|  SystemPackageFilter                    DailyUsageHighWaterMarkStore (v4)         |
|  - Hardcoded OEM updater daemons        - Monotonic daily usage guarantee         |
|  - Dynamic non-launchable intent guard  - Midnight rollover & cache purge         |
+-----------------------------------------------------------------------------------+
```

### Module Responsibilities

1. **`service/UsageMonitorService.kt`**:
   * Runs as a foreground service with a 1.2-second polling loop.
   * Tracks active foreground sessions across controlled apps.
   * Maintains live sticky chronometer notification, 2-minute warning alerts, cooldown enforcement, and blocker triggers (`BlockerActivity`).
   * Logs completed application sessions directly to `AnalyticsRepository`.

2. **`platform/ForegroundAppDetector.kt`**:
   * Direct bridge to Android's `UsageStatsManager` and `UsageEvents`.
   * Computes ground-truth daily usage (`queryReconciledDailyUsageMap`) combining persistent OS daily buckets with post-flush in-memory events.
   * Identifies the active foreground package (`getForegroundPackageName`) while preserving session continuity during passive media viewing (YouTube) or reading.
   * Computes non-overlapping total screen time (`queryTotalScreenTimeToday`).

3. **`platform/SystemPackageFilter.kt`**:
   * Single source of truth for excluding background system daemons, OEM firmware updaters (`.sau`, `.ota`), launchers, and system UI.
   * Uses thread-safe cached `PackageManager.getLaunchIntentForPackage()` checks. If a process cannot be launched from the app drawer, it is classified as non-user background activity and excluded from screen time.

4. **`platform/analytics/AnalyticsRepository.kt`**:
   * Manages SQLite storage via `SessionDatabaseHelper` (`focus_sessions.db`).
   * Handles Day, Week, and Month aggregations, sector breakdowns, app rankings, and dynamic scope filtering (All Apps / Controlled Apps / Categories / Single App).
   * Reconstructs historical sessions from Android's hardware `UsageEvents` ring buffer (`syncFromSystemUsageEvents`).

5. **`platform/logs/UsageLogRepository.kt`**:
   * Reads raw hardware `UsageEvents` since midnight (or yesterday) to build the per-minute and second user audit logbook.
   * Debounces intra-app screen transitions, merges micro-gaps (<= 5s), and stamps sector metadata (`sectorId`, `sectorName`, `sectorColorLong`) on each session.

6. **`platform/DailyUsageHighWaterMarkStore.kt`**:
   * SharedPreferences store enforcing monotonic daily usage guarantees (usage within a single calendar day cannot decrease under memory pressure or circular buffer eviction).
   * Automatically resets at midnight using date keys (`YYYY-MM-DD`).

7. **`platform/analytics/SectorConfigStore.kt`**:
   * Manages 4 customizable time-of-day sectors:
     * **Morning**: 06:00 – 12:00
     * **Afternoon**: 12:00 – 17:00
     * **Evening**: 17:00 – 22:00
     * **Night**: 22:00 – 06:00 (wraps midnight)
   * Manages Focus Buffer Goals (Tracking only, Controlled Block, Strict Bedtime Lockdown).

---

## 2. Architecture Decision Records (ADRs)

### ADR-001: Single Authoritative Daily Bucket Selection (Preventing 3x Inflation)
* **Date**: October 2026 (v1.11.0)
* **Problem**:
  On OEM devices (ColorOS, OxygenOS, RealmeUI), `UsageStatsManager.queryUsageStats(INTERVAL_DAILY, midnightMs, nowMs)` returns multiple overlapping daily bucket slices for the same package (e.g. yesterday's rollover bucket + today's partial buckets). The previous implementation looped over all returned buckets and summed `stat.totalTimeInForeground`, causing Instagram (1h 11m) to be inflated ~3x to 3h 14m, and YouTube (39m) to 2h 19m.
* **Decision**:
  1. Strictly filter buckets where `stat.lastTimeStamp > midnightMs`.
  2. For each package, select **only the single latest bucket**: `todayBuckets.maxByOrNull { it.lastTimeStamp }`. **Never sum across multiple buckets for the same package.**
  3. If a bucket started before midnight, cap its today's contribution at `stat.lastTimeStamp - midnightMs` (not `stat.lastTimeUsed - midnightMs`).
* **Consequence**:
  Daily baseline usage matches Google's Digital Wellbeing 1:1 down to the exact minute.

---

### ADR-002: Intra-App Pause Filtering & Micro-Gap Debouncing (181 Micro-Sessions Fix)
* **Date**: October 2026 (v1.11.0)
* **Problem**:
  In Android, navigating inside an app (e.g. Instagram Feed to Reel, viewing Stories, opening DMs) fires `ACTIVITY_PAUSED` on the old Activity milliseconds before `ACTIVITY_RESUMED` on the new Activity in the same package. The previous implementation closed the session on every `ACTIVITY_PAUSED`, shredding a single 12-minute Instagram session into 42 micro-sessions (3s, 5s, 29s), resulting in 181 artificial sessions in the Logbook.
* **Decision**:
  1. Internal screen transitions (`event.packageName == currentPkg`) must **never** terminate a session.
  2. A session strictly terminates only when:
     * A **different** app moves to the foreground (`event.packageName != currentPkg`), or
     * The device locks / screen turns off (`SCREEN_NON_INTERACTIVE` / `KEYGUARD_SHOWN`).
  3. Consecutive sessions for the same package separated by <= 5 seconds (screen rotation, transient notification shade pull) are merged into a single continuous block.
* **Consequence**:
  Session count dropped from 181 micro-sessions to ~25 realistic, human-scale app sessions.

---

### ADR-003: Time Sectors are Strictly Subsets of the Calendar Day
* **Date**: October 2026 (v1.13.0)
* **Problem**:
  The Night sector spans across midnight (22:00 to 06:00). If the Night sector was queried as a raw interval, it would pull hours from yesterday night (22:00 to 24:00 yesterday) into today's metrics, causing sector sums to exceed the 24-hour day.
* **Decision**:
  1. Daily time is strictly bounded by the calendar day `[00:00:00.000, 23:59:59.999]`.
  2. Sectors partition this calendar window. For today:
     * Night Sector = `00:00–06:00` (today's early morning) + `22:00–24:00` (today's late night).
     * Morning Sector = `06:00–12:00`.
     * Afternoon Sector = `12:00–17:00`.
     * Evening Sector = `17:00–22:00`.
  3. Total daily time is computed first. Sector times are derived from the day's sessions.
  4. The sum of the 4 sectors strictly equals the daily total: `sector_morning + sector_afternoon + sector_evening + sector_night == totalDailyUsage`.
* **Consequence**:
  Zero boundary leakage between adjacent days. Sector percentages always sum to 100%.

---

### ADR-004: Exclusion of OEM Update Daemons & Non-Launchable Background Tasks
* **Date**: October 2026 (v1.12.0)
* **Problem**:
  ColorOS/OxygenOS runs an automated overnight maintenance window ("Auto-update overnight", 02:00–05:00 AM) while charging. The daemon `com.coloros.sau` (Software Auto-Update) wakes up, compiles app bytecode (`dex2oat`), and checks security patches, holding an internal window for 2 to 3 hours. Raw Android `UsageStats` logged this as foreground screen time, causing a mysterious 3-hour "Software Update" entry in the Night sector.
* **Decision**:
  1. Centralized filtering inside `SystemPackageFilter.kt`.
  2. Explicit blacklist for OEM updater daemons (`com.coloros.sau`, `com.oplus.sau`, `com.oplus.ota`, `com.android.updater`, `com.wssyncmldm`, `.sau`, `.ota`, etc.).
  3. Dynamic non-launchable guard: `packageManager.getLaunchIntentForPackage(pkg) == null`. If an app has no launcher icon in the app drawer, it cannot be launched by a human and is excluded from user screen time.
* **Consequence**:
  Overnight maintenance daemons are invisible in Analytics, Usage Logs, and Screen Time totals.

---

### ADR-005: Deep-Linking Analytics Sectors to Filtered Usage Logs
* **Date**: October 2026 (v1.13.0)
* **Problem**:
  Users viewing the Day Sector breakdown in Analytics had no direct way to audit which exact apps and sessions occurred during a specific sector (e.g. "What did I use during the Evening sector?").
* **Decision**:
  1. In `AnalyticsScreen.kt`, tapping any of the 4 sectors (in `SectorSegmentedBar` or `SectorLegendItem`) triggers `onNavigateToUsageLogs(sectorId, selectedPackage)`.
  2. In `UsageLogsScreen.kt`, added a Neumorphic Sector Filter Bar (`Any Time`, `Morning`, `Afternoon`, `Evening`, `Night`).
  3. If an app was focused in Analytics (e.g. Instagram), the Logbook opens pre-filtered by **both** that sector and that app (`APP FILTER: Instagram [Clear Filter ×]`).
  4. Top summary metrics (`SCREEN TIME`, `CONTROLLED`, `SESSIONS`) dynamically recalculate to reflect the filtered slice.
* **Consequence**:
  Smooth, one-tap drill-down from macro sector analytics to micro per-minute session logs.

---

## 3. Schema & Cache Version Matrix

| Store / Database | Version | Purpose of Latest Schema Upgrade |
| :--- | :--- | :--- |
| **`SessionDatabaseHelper`** (`focus_sessions.db`) | **`v3`** | Auto-drops legacy tables containing inflated synthetic sessions or OEM updater entries; repopulates clean sessions directly from hardware `UsageEvents`. |
| **`DailyUsageHighWaterMarkStore`** (`daily_usage_high_water_mark_prefs`) | **`v4`** | Auto-purges legacy cached high-water marks for `com.coloros.sau` and previously multiplied bucket totals on app launch. |
| **`app/build.gradle.kts`** | **`v1.13.0`** (vc: `20`) | Current production-ready build. |

---

## 4. How to Consult This Document

* **When answering "Why does X show Y time?"**: Review **ADR-001** (bucket selection), **ADR-003** (calendar sector bounds), or **ADR-004** (updater filtering).
* **When modifying the session logger or timeline**: Review **ADR-002** (intra-app transitions and 5s micro-gap debouncing).
* **When adding new navigation or drill-downs**: Review **ADR-005** (`MainScreen` state handshake and deep-linking contracts).
* **Whenever a new architectural decision is made**: Add a new ADR to this document in the same commit.
