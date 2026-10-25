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
     * Queries accurate foreground usage milliseconds for [packageName] from today's midnight (00:00:00).
     * Uses Android OS UsageStatsManager aggregation, matching Digital Wellbeing.
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

        // 1. Primary: queryAndAggregateUsageStats (exact OS aggregate since midnight)
        try {
            val statsMap = manager.queryAndAggregateUsageStats(midnightMs, nowMs)
            val stats = statsMap[packageName]
            if (stats != null && stats.totalTimeInForeground > 0L) {
                return stats.totalTimeInForeground
            }
        } catch (_: Exception) {}

        // 2. Direct daily bucket fallback
        try {
            val list = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnightMs, nowMs)
            val total = list.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
            if (total > 0L) return total
        } catch (_: Exception) {}

        return 0L
    }

    /**
     * Returns a map of package names to their real daily usage in minutes for today.
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
            val statsMap = manager.queryAndAggregateUsageStats(midnightMs, nowMs)
            for (pkg in packageNames) {
                val fgMs = statsMap[pkg]?.totalTimeInForeground ?: 0L
                result[pkg] = (fgMs / (1000 * 60)).toInt()
            }
        } catch (_: Exception) {
            try {
                val list = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, midnightMs, nowMs)
                val grouped = list.groupBy { it.packageName }
                for (pkg in packageNames) {
                    val fgMs = grouped[pkg]?.sumOf { it.totalTimeInForeground } ?: 0L
                    result[pkg] = (fgMs / (1000 * 60)).toInt()
                }
            } catch (_: Exception) {}
        }

        return result
    }

    /**
     * Calculates total device screen time for today in minutes (across all user apps).
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

        return try {
            val statsMap = manager.queryAndAggregateUsageStats(midnightMs, nowMs)
            val totalMs = statsMap.values
                .filter { stats ->
                    stats.totalTimeInForeground > 0L &&
                    !stats.packageName.startsWith("com.android.launcher") &&
                    !stats.packageName.startsWith("com.google.android.inputmethod") &&
                    stats.packageName != context.packageName
                }
                .sumOf { it.totalTimeInForeground }
            (totalMs / (1000 * 60)).toInt()
        } catch (_: Exception) {
            0
        }
    }
}
