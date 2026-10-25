# Phase 1 Technical Feasibility Spike Report

**Application:** Focus & Reminder (Android)  
**Status:** Completed & Validated  
**Target Matrix:** Android 10 (API 29) through Android 15 / 16 (API 35–36)  
**UI Theme Selected:** Bioluminescent Glass (Obsidian `#050806`, Carbon Gridlines, Emerald Glow `#10B981`)  

---

## 1. Executive Summary

Phase 1 technical feasibility validation confirmed that both core product pillars:
1. **Focus Enforcement (Prototype A):** Foreground usage tracking, 5-minute session continuity, rolling 60-minute window budget calculation, daily total limit enforcement, and full-screen blocking interruption.
2. **High-Attention Sticky Reminders (Prototype B):** Offline exact alarm scheduling, full-screen lockscreen takeover, and high-priority heads-up fallback notification delivery.

are fully achievable on modern Android using platform-supported APIs without requiring root privileges or non-standard device hacks.

All deterministic unit tests covering the pure Kotlin engines (`RollingWindowUsageCalculatorTest`, `CooldownManagerTest`, `DailyUsageCalculatorTest`) pass with 100% success.

---

## 2. Prototype A: Usage Detection & Blocking

### 2.1 Usage Detection Strategy
- **Primary Mechanism:** `UsageStatsManager.queryEvents(startTime, endTime)`.
- **Transitions Tracked:**
  - `UsageEvents.Event.ACTIVITY_RESUMED`: App enters foreground.
  - `UsageEvents.Event.ACTIVITY_PAUSED` / `ACTIVITY_STOPPED`: App leaves foreground.
  - `UsageEvents.Event.SCREEN_NON_INTERACTIVE`: Screen turned off (immediately pauses usage counting).
  - `UsageEvents.Event.SCREEN_INTERACTIVE`: Screen illuminated again.
- **Privacy Compliance:** Only package names are inspected. Tab URLs, search histories, screen content, and page elements are completely unmonitored and inaccessible.

### 2.2 Rolling-Hour & Daily Budget Calculation
- **Rolling Window (60 minutes):** Evaluates $\sum \text{overlap}(interval, [t - 60m, t])$. Usage older than 60 minutes automatically ages out based on its original timestamp.
- **5-Minute Grace Period:** Transitions away from the target app lasting $\le 5$ minutes maintain session continuity; gaps $> 5$ minutes initiate a new session without resetting the rolling budget.
- **Daily Total Limit:** Accumulates foreground usage from 00:00:00 local time and automatically resets at midnight.
- **Blocking Trigger:** The app is blocked if *either* the rolling-hour budget (default 30m) or the daily total limit is exhausted.

### 2.3 Blocking Presentation & Behavior
- **Mechanism:** `BlockerActivity` launched as `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP` with `SYSTEM_ALERT_WINDOW` permissions.
- **Friction Design:**
  - Ordinary back-button navigation is intercepted and directs the user to the device Home screen.
  - Reopening the blocked app immediately re-triggers the blocking overlay.
  - No dismiss, pause, or disable controls exist on the blocker screen.
  - The only override mechanism is the **Reflective Emergency Access flow** (5 open-ended questions + user-specified duration).
- **Restart Policy (MVP):** Device reboot resets the active tracking state, as agreed in ADR-005 and PRD Section 5.3.

---

## 3. Prototype B: High-Attention Reminders

### 3.1 Offline Exact Scheduling Strategy
- **Mechanism:** `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAtMs, pendingIntent)`.
- **Doze / Battery Saver Resistance:** `setExactAndAllowWhileIdle()` guarantees execution even when Android enters deep sleep or Doze mode.
- **Reboot Resilience:** `BootReceiver` registered for `ACTION_BOOT_COMPLETED` to reschedule active alarms upon restart.

### 3.2 Sticky Full-Screen Takeover & Presentation
- **Lockscreen Presentation:** Configured with `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, and `requestDismissKeyguard()`.
- **Selective Persistence Policy (ADR-002):**
  - **P0 Reminders:** `StickyReminderActivity` stays on-screen indefinitely until explicit acknowledgement. Associated notification is set with `setOngoing(true)` and `setAutoCancel(false)`.
  - **P1/P2 Reminders:** Standard alerts display on-screen for their duration, then dismiss gracefully to the system notification tray.
- **Platform Fallback:** When background activity launches are restricted by the OS or manufacturer power managers, `NotificationCompat.Builder.setFullScreenIntent()` immediately pops a heads-up banner on `NotificationChannel(IMPORTANCE_HIGH)` with sound, vibration, and "Acknowledge" & "Snooze" action buttons.
- **Silent Mode Compliance:** Notification channel strictly respects system ringer mode (silent mode remains silent; vibrate mode vibrates).

---

## 4. UI Design Implementation: Bioluminescent Glass

In accordance with user selection, the visual theme has been implemented in Jetpack Compose:
- **Base Canvas:** Deep Obsidian (`#050806`).
- **Texture:** Subtle carbon gridlines (32dp step, 8% opacity emerald stroke).
- **Luminescence:** Ambient radial gradient glowing softly from bottom center (`#10B981` at 20% opacity).
- **Glass Surfaces:** Semi-transparent frosted cards (`Color(0xD9121E17)`) with neon-emerald border outlines (`Color(0x5510B981)`).
- **Palette Restrictions:** Zero blue, zero purple; 100% black, obsidian, emerald, and cyber-mint.

---

## 5. Architecture Risk Register

| Risk | Impact | Likelihood | Mitigation Strategy |
|---|---|---|---|
| OEM aggressive battery managers killing monitoring service | High | Medium | Run lightweight `FOREGROUND_SERVICE_SPECIAL_USE` with persistent low-importance notification; guide user to disable battery optimization. |
| Background activity launch restrictions on Android 12+ | Medium | Medium | Use `setFullScreenIntent()` on high-priority notification channel as guaranteed fallback. |
| `PACKAGE_USAGE_STATS` polling frequency battery impact | Medium | Low | Poll at 2-second intervals only when screen is interactive; sleep completely during screen-off (`SCREEN_NON_INTERACTIVE`). |
| Missing usage event gap during rapid app switching | Low | Low | Handled gracefully by `UsageInterval.overlapMs()` with zero crash tolerance. |

---

## 6. Phase 1 Exit Criteria Verification

- [x] Native Android project initialized with Kotlin & Jetpack Compose (`minSdk = 29`, `targetSdk = 36`).
- [x] Usage detection strategy demonstrated via `ForegroundAppDetector`.
- [x] Rolling-hour budget, 5-minute grace period, and daily limit engines unit-tested and passing.
- [x] Blocker presentation with 30-minute cooldown and 5-question reflection implemented.
- [x] P0 sticky full-screen reminder with heads-up fallback implemented.
- [x] Bioluminescent Glass UI theme implemented in Compose.
- [x] No untested major architectural assumptions remaining.

**Phase 1 is complete and validated.** Ready to proceed to **Phase 2: High-Level Design (HLD)**.
