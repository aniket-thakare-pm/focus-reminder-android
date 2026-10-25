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

    private val highWaterMarkStore: DailyUsageHighWaterMarkStore =
        DailyUsageHighWaterMarkStore.getInstance(context)

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
     * Robust multi-tier calculation: Merges live RAM event buffer with persistent OS daily buckets & aggregate stats.
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

        // 1. High-precision event calculation (from in-memory buffer)
        val eventMap = queryAccurateDailyUsageMap()
        val eventMs = eventMap[packageName] ?: 0L

        // 2. Query persistent OS daily buckets (contains historical sessions flushed to disk)
        var bucketMs = 0L
        try {
            val list = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                midnightMs - (24 * 60 * 60 * 1000L),
                nowMs
            )
            val todayStat = list
                .filter { it.packageName == packageName && it.lastTimeUsed >= midnightMs }
                .maxByOrNull { it.firstTimeStamp }

            if (todayStat != null) {
                bucketMs = todayStat.totalTimeInForeground
            }
        } catch (_: Exception) {}

        // 3. Fallback/Corroboration from aggregated stats
        var aggregateMs = 0L
        try {
            val aggregated = manager.queryAndAggregateUsageStats(midnightMs, nowMs)
            val stat = aggregated[packageName]
            if (stat != null && stat.lastTimeUsed >= midnightMs) {
                aggregateMs = stat.totalTimeInForeground
            }
        } catch (_: Exception) {}

        // Crucial: Use maxOf across RAM events, disk buckets, and our persistent high-water mark
        val rawMaxMs = maxOf(eventMs, bucketMs, aggregateMs)
        return highWaterMarkStore.updateHighWaterMarkMs(packageName, rawMaxMs)
    }

    /**
     * Returns a map of package names to their real daily usage in minutes for today.
     * Uses multi-tier evaluation to work reliably across all Android versions and OEM skins.
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

        // 1. Event buffer map
        val eventMap = queryAccurateDailyUsageMap()

        // 2. Query OS daily usage buckets
        val bucketMap = mutableMapOf<String, Long>()
        try {
            val list = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                midnightMs - (24 * 60 * 60 * 1000L),
                nowMs
            )
            val grouped = list.groupBy { it.packageName }
            for (pkg in packageNames) {
                val todayStat = grouped[pkg]
                    ?.filter { it.lastTimeUsed >= midnightMs }
                    ?.maxByOrNull { it.firstTimeStamp }

                if (todayStat != null) {
                    bucketMap[pkg] = todayStat.totalTimeInForeground
                }
            }
        } catch (_: Exception) {}

        // 3. Aggregated stats
        val aggregateMap = mutableMapOf<String, Long>()
        try {
            val aggregated = manager.queryAndAggregateUsageStats(midnightMs, nowMs)
            for (pkg in packageNames) {
                val stat = aggregated[pkg]
                if (stat != null && stat.lastTimeUsed >= midnightMs) {
                    aggregateMap[pkg] = stat.totalTimeInForeground
                }
            }
        } catch (_: Exception) {}

        val result = mutableMapOf<String, Int>()
        for (pkg in packageNames) {
            val eMs = eventMap[pkg] ?: 0L
            val bMs = bucketMap[pkg] ?: 0L
            val aMs = aggregateMap[pkg] ?: 0L
            val rawMs = maxOf(eMs, bMs, aMs)
            val verifiedMs = highWaterMarkStore.updateHighWaterMarkMs(pkg, rawMs)
            result[pkg] = (verifiedMs / (1000 * 60)).toInt()
        }

        return result
    }

    /**
     * Calculates total device screen time for today in minutes (across all user apps, strictly since midnight).
     * Accurately reflects device total separate from individual apps.
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

        // 1. High-precision event calculation across all packages
        val eventMap = queryAccurateDailyUsageMap()
        val eventTotalMs = eventMap
            .filterKeys { pkg -> !isSystemOrLauncherPackage(pkg) }
            .values
            .sum()

        // 2. Daily buckets query with 24h lookback
        var bucketTotalMs = 0L
        try {
            val list = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                midnightMs - (24 * 60 * 60 * 1000L),
                nowMs
            )
            val grouped = list.groupBy { it.packageName }

            for ((pkg, statsGroup) in grouped) {
                if (isSystemOrLauncherPackage(pkg)) continue

                val todayStat = statsGroup
                    .filter { it.lastTimeUsed >= midnightMs }
                    .maxByOrNull { it.firstTimeStamp }

                if (todayStat != null) {
                    bucketTotalMs += todayStat.totalTimeInForeground
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback to aggregated stats
        var aggregateTotalMs = 0L
        try {
            val aggregated = manager.queryAndAggregateUsageStats(midnightMs, nowMs)
            aggregateTotalMs = aggregated
                .filterKeys { pkg -> !isSystemOrLauncherPackage(pkg) }
                .values
                .filter { it.lastTimeUsed >= midnightMs }
                .sumOf { it.totalTimeInForeground }
        } catch (_: Exception) {}

        val rawTotalMs = maxOf(eventTotalMs, bucketTotalMs, aggregateTotalMs)
        val verifiedTotalMs = highWaterMarkStore.updateScreenTimeHighWaterMarkMs(rawTotalMs)
        return (verifiedTotalMs / (1000 * 60)).toInt()
    }

    private fun isSystemOrLauncherPackage(pkg: String): Boolean {
        return pkg.startsWith("com.android.launcher") ||
                pkg.startsWith("com.google.android.apps.nexuslauncher") ||
                pkg.startsWith("com.sec.android.app.launcher") ||
                pkg.startsWith("com.miui.home") ||
                pkg.startsWith("com.google.android.inputmethod") ||
                pkg.startsWith("com.android.systemui") ||
                pkg.startsWith("com.android.settings") ||
                pkg == context.packageName
    }
}
