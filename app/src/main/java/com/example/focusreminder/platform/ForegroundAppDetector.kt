package com.example.focusreminder.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.focusreminder.domain.model.UsageInterval
import java.util.Calendar

/**
 * Extracts accurate foreground usage intervals from Android's UsageStatsManager.
 *
 * Implements PRD Section 5.2:
 * - Detects foreground transitions (ACTIVITY_RESUMED / ACTIVITY_PAUSED).
 * - Excludes screen-off periods (SCREEN_NON_INTERACTIVE).
 * - Excludes ordinary app switches.
 */
class ForegroundAppDetector(private val context: Context) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Queries foreground intervals for [targetPackageName] within [startTimeMs, endTimeMs].
     */
    fun queryForegroundIntervals(
        targetPackageName: String,
        startTimeMs: Long,
        endTimeMs: Long
    ): List<UsageInterval> {
        val manager = usageStatsManager ?: return emptyList()
        val intervals = mutableListOf<UsageInterval>()

        val events = manager.queryEvents(startTimeMs, endTimeMs)
        val event = UsageEvents.Event()

        var currentResumeTime: Long? = null
        var isScreenInteractive = true

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    isScreenInteractive = false
                    // If target app was active when screen turned off, close the current interval
                    if (currentResumeTime != null) {
                        val sessionEnd = event.timeStamp
                        if (sessionEnd > currentResumeTime) {
                            intervals.add(UsageInterval(currentResumeTime, sessionEnd))
                        }
                        currentResumeTime = null
                    }
                }
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    isScreenInteractive = true
                }
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (event.packageName == targetPackageName && isScreenInteractive) {
                        currentResumeTime = event.timeStamp
                    } else if (currentResumeTime != null) {
                        // Another app was resumed, close target app session
                        val sessionEnd = event.timeStamp
                        if (sessionEnd > currentResumeTime) {
                            intervals.add(UsageInterval(currentResumeTime, sessionEnd))
                        }
                        currentResumeTime = null
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (event.packageName == targetPackageName && currentResumeTime != null) {
                        val sessionEnd = event.timeStamp
                        if (sessionEnd > currentResumeTime) {
                            intervals.add(UsageInterval(currentResumeTime, sessionEnd))
                        }
                        currentResumeTime = null
                    }
                }
            }
        }

        // If app is currently in the foreground at evaluation time
        if (currentResumeTime != null && currentResumeTime < endTimeMs) {
            intervals.add(UsageInterval(currentResumeTime, endTimeMs))
        }

        return intervals
    }

    /**
     * Returns the package name of the app currently in the foreground.
     */
    fun getForegroundPackageName(lookbackMs: Long = 60_000L): String? {
        val manager = usageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - lookbackMs

        val events = manager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumedPackage = event.packageName
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    lastResumedPackage = null
                }
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    lastResumedPackage = null
                }
            }
        }

        if (lastResumedPackage != null) {
            return lastResumedPackage
        }

        // Fallback: Check most recently used app in last 10 seconds
        val statsList = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            endTime - 10_000L,
            endTime
        )
        val mostRecent = statsList?.maxByOrNull { it.lastTimeUsed }
        if (mostRecent != null && (endTime - mostRecent.lastTimeUsed) < 6000L) {
            return mostRecent.packageName
        }

        return null
    }

    /**
     * Accurately calculates real foreground usage per package strictly since midnight today.
     * Prevents Android's queryAndAggregateUsageStats interval boundary expansion bug
     * which bleeds into the previous 24-48 hours.
     */
    fun queryAccurateDailyUsageMap(): Map<String, Long> {
        val manager = usageStatsManager ?: return emptyMap()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMs = calendar.timeInMillis
        val nowMs = System.currentTimeMillis()

        // Query events from 15 minutes prior to midnight to catch cross-midnight sessions
        val queryStart = midnightMs - (15 * 60 * 1000L)
        val events = try {
            manager.queryEvents(queryStart, nowMs)
        } catch (_: Exception) {
            return emptyMap()
        }

        val usageMsMap = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        var currentForegroundPkg: String? = null
        var sessionStartMs: Long = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val time = event.timeStamp
            val pkg = event.packageName

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // When another app resumes, close the previous active app session
                    val prev = currentForegroundPkg
                    if (prev != null && prev != pkg) {
                        val effectiveStart = maxOf(sessionStartMs, midnightMs)
                        val effectiveEnd = minOf(time, nowMs)
                        if (effectiveEnd > effectiveStart) {
                            val duration = minOf(effectiveEnd - effectiveStart, 4 * 60 * 60 * 1000L)
                            usageMsMap[prev] = (usageMsMap[prev] ?: 0L) + duration
                        }
                    }
                    currentForegroundPkg = pkg
                    sessionStartMs = time
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentForegroundPkg == pkg) {
                        val effectiveStart = maxOf(sessionStartMs, midnightMs)
                        val effectiveEnd = minOf(time, nowMs)
                        if (effectiveEnd > effectiveStart) {
                            val duration = minOf(effectiveEnd - effectiveStart, 4 * 60 * 60 * 1000L)
                            usageMsMap[pkg] = (usageMsMap[pkg] ?: 0L) + duration
                        }
                        currentForegroundPkg = null
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    // Screen off or phone locked: close active app session
                    val active = currentForegroundPkg
                    if (active != null) {
                        val effectiveStart = maxOf(sessionStartMs, midnightMs)
                        val effectiveEnd = minOf(time, nowMs)
                        if (effectiveEnd > effectiveStart) {
                            val duration = minOf(effectiveEnd - effectiveStart, 4 * 60 * 60 * 1000L)
                            usageMsMap[active] = (usageMsMap[active] ?: 0L) + duration
                        }
                        currentForegroundPkg = null
                    }
                }
            }
        }

        // If an app is still in the foreground right now:
        val live = currentForegroundPkg
        if (live != null) {
            val effectiveStart = maxOf(sessionStartMs, midnightMs)
            val effectiveEnd = nowMs
            if (effectiveEnd > effectiveStart) {
                val duration = minOf(effectiveEnd - effectiveStart, 30 * 60 * 1000L)
                usageMsMap[live] = (usageMsMap[live] ?: 0L) + duration
            }
        }

        return usageMsMap
    }

    /**
     * Queries accurate foreground usage milliseconds for [packageName] strictly from today's midnight (00:00:00).
     * Filters specifically for today's daily bucket to avoid merging with yesterday's usage.
     */
    fun getDailyUsageMs(packageName: String): Long {
        val manager = usageStatsManager ?: return 0L
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMs = calendar.timeInMillis
        val nowMs = System.currentTimeMillis()

        // 1. Primary: Query today's specific daily bucket
        try {
            val list = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnightMs, nowMs)
            val todayStat = list
                .filter { it.packageName == packageName && it.lastTimeUsed >= midnightMs }
                .maxByOrNull { it.firstTimeStamp }

            if (todayStat != null && todayStat.firstTimeStamp >= midnightMs - (4 * 60 * 60 * 1000L)) {
                return todayStat.totalTimeInForeground
            }
        } catch (_: Exception) {}

        // 2. Fallback to event-based calculation strictly from midnight
        val map = queryAccurateDailyUsageMap()
        return map[packageName] ?: 0L
    }

    /**
     * Returns a map of package names to their real daily usage in minutes for today.
     * Accurately distinguishes package-specific usage from total device usage.
     */
    fun getDailyUsageMinutesMap(packageNames: Collection<String>): Map<String, Int> {
        val manager = usageStatsManager ?: return emptyMap()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMs = calendar.timeInMillis
        val nowMs = System.currentTimeMillis()

        val result = mutableMapOf<String, Int>()

        try {
            val list = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnightMs, nowMs)
            val grouped = list.groupBy { it.packageName }
            for (pkg in packageNames) {
                val todayStat = grouped[pkg]
                    ?.filter { it.lastTimeUsed >= midnightMs }
                    ?.maxByOrNull { it.firstTimeStamp }

                if (todayStat != null && todayStat.firstTimeStamp >= midnightMs - (4 * 60 * 60 * 1000L)) {
                    result[pkg] = (todayStat.totalTimeInForeground / (1000 * 60)).toInt()
                }
            }
        } catch (_: Exception) {}

        // Fallback for any package not found in daily buckets
        if (result.size < packageNames.size) {
            val map = queryAccurateDailyUsageMap()
            for (pkg in packageNames) {
                if (!result.containsKey(pkg)) {
                    val ms = map[pkg] ?: 0L
                    result[pkg] = (ms / (1000 * 60)).toInt()
                }
            }
        }

        return result
    }

    /**
     * Calculates total device screen time for today in minutes (across all user apps, strictly since midnight).
     * Accurately reflects device total (e.g. ~4h 20m) separate from individual apps (e.g. Instagram ~1h 44m).
     */
    fun getTotalScreenTimeTodayMinutes(): Int {
        val manager = usageStatsManager ?: return 0
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMs = calendar.timeInMillis
        val nowMs = System.currentTimeMillis()

        try {
            val list = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnightMs, nowMs)
            val grouped = list.groupBy { it.packageName }
            var totalMs = 0L

            for ((pkg, statsGroup) in grouped) {
                if (pkg.startsWith("com.android.launcher") ||
                    pkg.startsWith("com.google.android.apps.nexuslauncher") ||
                    pkg.startsWith("com.sec.android.app.launcher") ||
                    pkg.startsWith("com.miui.home") ||
                    pkg.startsWith("com.google.android.inputmethod") ||
                    pkg.startsWith("com.android.systemui") ||
                    pkg.startsWith("com.android.settings") ||
                    pkg == context.packageName
                ) {
                    continue
                }

                val todayStat = statsGroup
                    .filter { it.lastTimeUsed >= midnightMs }
                    .maxByOrNull { it.firstTimeStamp }

                if (todayStat != null && todayStat.firstTimeStamp >= midnightMs - (4 * 60 * 60 * 1000L)) {
                    totalMs += todayStat.totalTimeInForeground
                }
            }

            if (totalMs > 0L) {
                return (totalMs / (1000 * 60)).toInt()
            }
        } catch (_: Exception) {}

        // Fallback to event map
        val map = queryAccurateDailyUsageMap()
        val totalMs = map
            .filterKeys { pkg ->
                !pkg.startsWith("com.android.launcher") &&
                !pkg.startsWith("com.google.android.inputmethod") &&
                !pkg.startsWith("com.android.systemui") &&
                pkg != context.packageName
            }
            .values
            .sum()
        return (totalMs / (1000 * 60)).toInt()
    }
}
