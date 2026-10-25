# Focus & Reminder — Version Changelog

### v1.7.0 (Current) — 2026-10-25
* **Type:** Feature Release / In-App Analytics & Persistent Session Logging Engine
* **Summary:**
  - **In-App Analytics Suite:** Complete behavioral analytics interface accessible via a dedicated bottom navigation tab and by tapping the central sunken circular dial on the dashboard.
  - **Local Session Logging Database (`SessionDatabaseHelper`):** High-speed SQLite database engine logging exact session start, session end, elapsed duration, app category, and time-of-day sector.
  - **Time-of-Day Sectors:** Divides usage across 4 customizable biological and work sectors (Morning, Afternoon, Evening, Night) with color-coded segmented progress bars, peak vulnerability callout, and sector legends.
  - **Session Duration Distribution (Buckets):** Categorizes sessions into Micro-checks (<1m), Short visits (1–5m), Standard active browsing (5–10m), and Capped/Limit reached (10m+) with percentages and session counts.
  - **Sleep & Wake Buffer Goal Tracker:** User-configurable wind-down sleep buffer (default 23:00 to 00:00) tracking buffer breaches, exact breach times, and 7-day adherence rates.
  - **Daily, Weekly, and Monthly Time Horizons:** Timeframe selector with date steppers, multi-level filters (all monitored, by category, or by specific app), and a 7-day bar chart with a dashed Daily Average benchmark line.

### v1.6.1 — 2026-10-25
* **Type:** Patch / Usage Over-Counting Fix & High-Water Mark Cache Reset
* **Summary:**
  - **Eradicated `queryAndAggregateUsageStats`:** Permanently removed calls to `manager.queryAndAggregateUsageStats()`, which Android OS implements by expanding time intervals to whole daily disk bucket boundaries, inadvertently aggregating yesterday's usage into today's tally (e.g. 2.5h + 2.5h = 5h).
  - **Strict Today-Only Bucket Isolation:** Daily bucket queries (`UsageStatsManager.INTERVAL_DAILY`) now query strictly from today's midnight (`[midnightMs, nowMs]`) without 24-hour lookback, and explicitly filter out yesterday's bucket files using `firstTimeStamp >= midnightMs - 4h`.
  - **Hard Sanity Ceiling:** Clamped reported daily usage per-app and device screen time to the physical maximum possible today (`minOf(usageMs, nowMs - midnightMs)`), guaranteeing mathematically that usage can never exceed elapsed time since midnight.
  - **Automated High-Water Mark Invalidation (`Store Version 2`):** Bumped store schema to version 2, automatically purging previously corrupted inflated records on launch, and rejecting any cache entries exceeding today's elapsed wall-clock time.

### v1.6.0 — 2026-10-25
* **Type:** Feature Release / Persistent High-Water Mark Store & Service Resuscitation
* **Summary:**
  - **Persistent High-Water Mark Store (`DailyUsageHighWaterMarkStore`):** Added local database layer ensuring daily usage is strictly monotonic. Once minutes are verified, they are stored locally and can mathematically never shrink or drop throughout the day, even if the OS purges memory or lags disk flushes.
  - **Automatic Midnight Rollover & Auto-Prune:** Automatically rolls over high-water marks at 00:00:00 midnight using date keys (`YYYY-MM-DD`), and auto-purges data older than 7 days.
  - **Service Resuscitation on Unlock (`ACTION_USER_PRESENT`):** Registered unlock listener in `BootReceiver` and manifest so `UsageMonitorService` immediately restarts if killed by aggressive OEM battery managers during sleep.

### v1.5.5 — 2026-10-25
* **Commit:** `3b21ef5`
* **Type:** Patch / Daily Usage Truncation Fix
* **Summary:**
  - **Eviction-Proof Daily Usage Merge:** Fixed bug where Android's in-memory `UsageEvents` ring buffer, which automatically evicts morning events as phone activity accumulates throughout the day, caused Instagram to under-report (e.g. 45m instead of 2h+).
  - **Comprehensive Multi-Tier `maxOf`:** Upgraded `getDailyUsageMs`, `getDailyUsageMinutesMap`, and `getTotalScreenTimeTodayMinutes` to take `maxOf(eventMs, bucketMs, aggregateMs)` so persistent database records on disk are never discarded by truncated RAM event buffers.

### v1.5.4 — 2026-10-25
* **Commit:** `f6d5ee4`
* **Type:** Patch / Notification De-Clutter & Minimalist Design
* **Summary:**
  - **De-Cluttered to 2 Lines:** Removed all triple redundancy (`setSubText`, duplicate chronometer header, and "Wrap up before cooldown" text).
  - **Strict Minimalist Layout:** Line 1 is the App Name (`Instagram`), Line 2 is the exact countdown (`04:20 remaining`).
  - **Zero Desync:** Eliminated the 2-second timestamp drift between system chronometer and dynamic text by driving a single source of truth.

### v1.5.3 — 2026-10-25
* **Commit:** `ca4a487`
* **Type:** Patch / Continuous Sticky Timer Notification
* **Summary:**
  - **Dual-Rendering Continuous Timer:** In addition to the native Android chronometer in the header, the exact remaining digital countdown (`09:48`, `09:47`...) is dynamically rendered directly in the notification title and content body so it is universally visible across all OEM skins (OxygenOS, ColorOS, OneUI).
  - **Sticky & Non-Intrusive:** Marked with `setOngoing(true)` and `setOnlyAlertOnce(true)` so it stays pinned without repeated buzzes or sound.
  - **Clean Channel v3:** Upgraded to `focus_sticky_timer_channel_v3` with default alert importance, ensuring OxygenOS does not treat it as a silent hidden notification.
  - **Instant Exit Dismissal & Re-Entry Resume:** The notification disappears the instant Instagram is closed or left, and re-appears immediately with exact banked time upon returning within the 5-minute grace period.

### v1.5.2 — 2026-10-25
* **Commit:** `80b0948`
* **Type:** Patch / Fix
* **Summary:**
  - **Removed Screen Overlay:** Completely eradicated intrusive status bar window overlay that collided with status bar icons.
  - **Instant Dismissal On App Close:** As soon as the user closes or leaves Instagram, the timer notification is immediately dismissed from the notification shade and status bar. No lingering notifications on home screen or in other apps.
  - **Pause & Resume with Grace Period:** When leaving Instagram, remaining session time is paused and banked. Returning within the 5-minute grace period smoothly resumes the timer from where it stopped. Away for > 5 minutes cleanly expires the session.

### v1.5.1 — 2026-10-25
* **Commit:** `9d158bd`
* **Type:** Patch / Status Bar Channel Fix
* **Summary:**
  - **Status Bar Notification Fix:** Upgraded chronometer channel to `IMPORTANCE_HIGH`, added `CATEGORY_STOPWATCH`, `setShowBadge(true)`, and `EXTRA_REQUEST_PROMOTED_ONGOING`.
  - **Dedicated Vector Asset:** Added `ic_stat_timer.xml` crisp stopwatch icon for status bar notification.

### v1.5.0 — 2026-10-25
* **Commit:** `095cb49`
* **Type:** Major Feature Release
* **Summary:**
  - **Session Entry Contract:** Full-screen pop-up upon opening restricted apps with tactile Neumorphic "Slide to Understand" slider and haptic feedback.
  - **Configurable Session & Mandatory Cooldown:** Configurable per app (e.g. 10m session followed by 30m mandatory cooldown wait time).
  - **Live Status Bar Chronometer:** Zero-battery native Android chronometer countdown ticking in the status bar and notification shade.
  - **2-Minute Heads-Up Warning Alert:** Drops down high-priority warning banner with sound/vibration 2 minutes before session lock.
  - **Hard Stop & Grace Period:** Exact lock enforcement at 00:00 with cooldown transition; 5-minute grace period for quick app-switching or brief screen locks.

### v1.4.1 — 2026-10-25
* **Commit:** `af7d4d2`
* **Type:** Patch / Multi-device fix
* **Summary:**
  - Resolved 0-minute usage tracking on external devices (Samsung, OnePlus, Xiaomi).
  - Replaced rigid 4-hour bucket filter with universal 3-tier usage resolution:
    1. Tier 1: Real-time event boundary clamping.
    2. Tier 2: 24h lookback daily bucket query (`lastTimeUsed >= midnight`).
    3. Tier 3: Aggregated stats fallback for restrictive OEM skins.

### v1.4.0 — 2026-10-25
* **Commit:** `91779f3`
* **Type:** Feature release
* **Summary:**
  - Full manual reminder scheduling with native Android `TimePickerDialog`.
  - Added day selector (`Today` / `Tomorrow`) and quick preset chips (`+15m`, `+30m`, `+1h`, `+3h`).
  - Implemented persistent `ReminderRepository` (local storage across restarts).
  - Synced Dashboard "NEXT P0 STICKY ALERT" card with real scheduled alerts.
  - Enabled device reboot restoration via `BootReceiver`.

### v1.3.2 — 2026-10-25
* **Commit:** `a093021`
* **Type:** Patch
* **Summary:**
  - Fixed Instagram usage (1h 44m) vs total device screen time (4h 20m) by isolating today's daily bucket from yesterday's history.

### v1.3.1 — 2026-10-25
* **Commit:** `4166643`
* **Type:** Patch & Security hardening
* **Summary:**
  - Enforced strict hour & minute formatting (`TimeFormatter`: `1h 44m / 2h 00m`).
  - Added midnight hard lock for exhausted daily limits with `RESET AT MIDNIGHT` countdown.
  - Added `USE_FULL_SCREEN_INTENT` permission for guaranteed lockscreen takeover.

### v1.3.0 — 2026-10-25
* **Commit:** `227715d`
* **Type:** Feature release
* **Summary:**
  - Restructured Apps screen into two top summary counters: `Active Apps` & `Total Apps`.
  - Created dedicated `All Apps` section with one-tap toggle switches.

### v1.2.0 — 2026-10-25
* **Commit:** `f587c68`
* **Type:** Feature release
* **Summary:**
  - Connected live `UsageStatsManager` queries to Dashboard hero sunken dial, device screen time metric, and restricted app cards.

### v1.1.1 — 2026-10-25
* **Commit:** `4943ced`
* **Type:** Patch
* **Summary:**
  - Refined Dark Mode neumorphism: removed unnatural white shadows, added calibrated directional floor drop shadows.

### v1.1.0 — 2026-10-25
* **Commit:** `ce0ca58`
* **Type:** Feature release
* **Summary:**
  - Added complete Dark Mode (Obsidian Soft UI) theme and calibrated debossed inset depth.

### v1.0.0 — 2026-10-24
* **Commit:** `3a8b1e5`
* **Type:** Baseline Prototype Release
* **Summary:**
  - Initial foundation with Neumorphic UI design system.
  - Prototype A: Usage detection, rolling budget, cooldown, emergency reflection blocker.
  - Prototype B: P0 sticky full-screen takeover reminders and alarm scheduling.
