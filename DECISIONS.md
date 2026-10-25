# Architectural & Product Decision Register (ADR)

**Project:** Focus & Reminder (Android)  
**Status:** Frozen for MVP  
**Last Updated:** October 2026  

---

## Summary of Decisions

| ID | Title | Status | Scope |
|---|---|---|---|
| ADR-001 | Rolling Usage Budget Configurability | Accepted | Core Usage Engine |
| ADR-002 | Reminder On-Screen Duration & Acknowledgement Policy | Accepted | Core Reminder Engine |
| ADR-003 | Calendar Integration Scope & Phasing | Accepted | Integrations (Phase 2) |
| ADR-004 | Local Retention of Reflective Emergency Answers | Accepted | Emergency Override / Privacy |
| ADR-005 | Emergency Access Window & Repeat Policy | Accepted | Emergency Override |
| ADR-006 | Target Android OS Support Matrix | Accepted | Platform / Gradle |
| ADR-007 | Full-Screen Reminder Fallback Presentation | Accepted | UI / Notifications |
| ADR-008 | Daily Total App Limit (Cumulative Budget) | Accepted | Core Usage Engine |
| ADR-009 | Session Contract, Live Chronometer & Cooldown | Accepted | UX / Focus Enforcement |

---

## ADR-001: Rolling Usage Budget Configurability

- **Context:** PRD Open Question 1 asked whether the rolling-hour usage budget should be fixed at 30 minutes or user-configurable.
- **Decision:** Make the usage budget configurable per restricted app, but default to 30 minutes within a rolling 60-minute window.
- **Rationale:** Different applications require different levels of friction (e.g., social media vs work communication). Providing a 30-minute default ensures immediate, frictionless onboarding without forcing setup decisions.
- **Consequences:** The data layer must store a `budgetMinutes` field per monitored app entity, and the domain engine must accept `budgetMinutes` dynamically rather than hardcoding 30 minutes.

---

## ADR-002: Reminder On-Screen Duration & Acknowledgement Policy

- **Context:** PRD Open Question 2 asked if reminders should remain on-screen indefinitely until acknowledged, or if this should apply selectively.
- **Decision:** Selective enforcement based on reminder priority:
  - **P0 Reminders:** High-urgency alerts stick on-screen indefinitely until explicit user acknowledgement (or snooze/complete).
  - **P1 / P2 Reminders:** Standard alerts display prominently for their configured duration, then dismiss gracefully to the system notification drawer.
- **Rationale:** Non-critical reminders should not hold the device hostage if the user is in an urgent situation, but high-priority (P0) tasks demand deliberate attention to prevent being missed.
- **Consequences:** The reminder entity requires a `priority` enum (`P0`, `P1`, `P2`) and the presentation layer must handle timeout-to-notification logic for P1/P2 vs permanent overlay for P0.

---

## ADR-003: Calendar Integration Scope & Phasing

- **Context:** PRD Open Question 3 asked whether calendar integration should include all device calendars or strictly Google Calendar, and whether it belongs in MVP.
- **Decision:** Defer calendar integration to Phase 2 (post-MVP), and restrict scope exclusively to Google Calendar.
- **Rationale:** Focus the MVP entirely on the two core pillars (enforceable usage blocking and rich local reminders). Keeping calendar out of MVP eliminates sync edge cases and Google account authentication complexity during initial validation.
- **Consequences:** No calendar permissions (`READ_CALENDAR`, `WRITE_CALENDAR`) or Google Calendar API dependencies will be included in the MVP codebase.

---

## ADR-004: Local Retention of Reflective Emergency Answers

- **Context:** PRD Open Question 4 asked whether reflective text answers submitted during emergency access should be discarded immediately or retained locally.
- **Decision:** Retain answers locally on the device in the Room database, strictly for the user's personal self-reflection journal. Answers are never processed, analyzed by AI, or transmitted off the device.
- **Rationale:** Enables intentional self-awareness over time while preserving 100% offline privacy and zero server footprint.
- **Consequences:** A `ReflectiveJournalEntry` table will be added to the Room database with timestamp, app package, questions, answers, and requested duration.

---

## ADR-005: Emergency Access Window & Repeat Policy

- **Context:** PRD Open Question 5 asked what duration and repeat policies apply to emergency overrides.
- **Decision:** 
  1. The user specifies their required access duration (in minutes) during the 5-question reflection flow.
  2. Access is granted for that exact window, after which the application automatically re-locks.
  3. If the user requires more time, the exact same cycle (answering all 5 reflective questions + specifying time) must be repeated in full.
- **Rationale:** Eliminates arbitrary developer-set limits while forcing intentional estimation of task time. The friction of re-answering all 5 questions discourages repeated extensions.
- **Consequences:** The blocker needs a countdown timer service tracking granted override expiration. Once expired, the foreground overlay immediately re-engages if the user remains in the target app.

---

## ADR-006: Target Android OS Support Matrix

- **Context:** PRD Open Question 6 asked which Android versions and devices are in the support matrix.
- **Decision:** Support the latest Android release and 5 versions back:
  - `minSdk = 29` (Android 10)
  - `targetSdk = 35` (Android 15)
  - `compileSdk = 35`
- **Rationale:** Covering Android 10 through Android 15 encompasses over 90% of active Android devices globally while providing modern background execution, notification channels, and `AppOpsManager` / `UsageStatsManager` APIs.
- **Consequences:** Gradle build scripts will configure `minSdk 29` and `targetSdk 35`. Any version-specific permission checks (e.g. `POST_NOTIFICATIONS` in Android 13+, exact alarms in Android 12+) will be cleanly gated.

---

## ADR-007: Full-Screen Reminder Fallback Presentation

- **Context:** PRD Open Question 7 asked what fallback presentation should be used when full-screen takeover is unavailable.
- **Decision:** Use a standard Android high-priority heads-up notification with prominent sound, vibration, and actionable buttons ("Acknowledge", "Snooze").
- **Rationale:** Android strictly limits background activity launches (`SYSTEM_ALERT_WINDOW` or Full-Screen Intent permissions may be restricted by OEM battery managers). A high-priority notification channel is guaranteed to alert the user without crashing or failing silently.
- **Consequences:** The reminder service will attempt full-screen intent/overlay, but always construct and dispatch a heads-up notification channel concurrently as the resilient carrier.

---

## ADR-008: Daily Total App Limit (Cumulative Budget)

- **Context:** User requested a daily total usage limit per app in addition to the rolling 60-minute budget.
- **Decision:** Add a configurable daily total usage limit for each restricted app:
  - Tracks accumulated foreground time from midnight to midnight (00:00:00 to 23:59:59 local time).
  - App is blocked if *either* the rolling-hour limit *or* the daily total limit is exhausted.
  - Automatically resets at midnight local device time.
  - Blocker clearly distinguishes daily limit exhaustion from rolling-hour exhaustion.
- **Rationale:** Prevents "budget pacing" exploits where a user spreads 30-minute sessions across the entire day, accumulating excessive daily screen time.
- **Consequences:** Usage engine will maintain dual accounting:
  1. `TrailingHourUsageCalculator` (rolling 60-minute window).
  2. `DailyTotalUsageCalculator` (midnight-to-midnight cumulative accumulator with a daily midnight reset alarm).

---

## ADR-009: Session Contract, Live Chronometer & Cooldown

- **Context:** When opening a restricted app, users can mindlessly browse past their intended window without awareness, and scrolling within an app does not emit new activity transition events.
- **Decision:**
  1. **Entry Contract Pop-up:** Upon opening a restricted app, a full-screen `SessionContractActivity` appears requiring the user to drag a Neumorphic `NeuSlideToAcknowledge` slider ("Slide to Understand") with haptic feedback before entry is permitted.
  2. **Configurable Session & Mandatory Cooldown:** Users configure allowed session duration (e.g. 10m) and mandatory cooldown wait time (e.g. 30m) instead of abstract window math. Window = Session + Cooldown.
  3. **Live Status Bar Chronometer:** An ongoing low-priority notification with Android's native `setUsesChronometer(true)` and `setChronometerCountDown(true)` renders a live, ticking countdown in the status bar and shade.
  4. **2-Minute Heads-Up Warning:** A high-priority heads-up notification with sound and vibration warns the user 2 minutes before the session expires.
  5. **Exact Lock & Grace Period:** At 00:00, the app locks immediately for the configured cooldown period. A 5-minute grace period applies to temporary app-switches or screen locks.
- **Rationale:** Introduces intentional entry friction, continuous ambient time awareness without active polling battery drain, and guaranteed enforcement.
- **Consequences:** Created `SessionContractActivity`, updated `UsageMonitorService` with active session tracking and notification channels, and updated `AppRestrictionRepository` to persist `cooldownMinutes`.
