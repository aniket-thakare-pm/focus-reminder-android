# Focus & Reminder — Version Changelog

### v1.5.0 (Current) — 2026-10-25
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
