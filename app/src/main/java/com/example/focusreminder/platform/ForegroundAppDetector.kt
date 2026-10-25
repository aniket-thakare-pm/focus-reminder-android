package com.example.focusreminder.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.focusreminder.domain.model.UsageInterval

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
        var currentForeground: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentForeground = event.packageName
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (event.packageName == currentForeground) {
                        currentForeground = null
                    }
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    currentForeground = null
                }
            }
        }

        // Fallback: Check most recently used app in last 5 seconds
        if (currentForeground == null) {
            val statsList = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                endTime - 10_000L,
                endTime
            )
            val mostRecent = statsList?.maxByOrNull { it.lastTimeUsed }
            if (mostRecent != null && (endTime - mostRecent.lastTimeUsed) < 5000L) {
                currentForeground = mostRecent.packageName
            }
        }

        return currentForeground
    }
}
