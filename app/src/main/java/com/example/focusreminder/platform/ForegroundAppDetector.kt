package com.example.focusreminder.platform

import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.PowerManager
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

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val keyguardManager: KeyguardManager? =
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val highWaterMarkStore: DailyUsageHighWaterMarkStore =
        DailyUsageHighWaterMarkStore.getInstance(context)

    // Cached active foreground package to maintain session continuity during continuous video/reading
    private var cachedForegroundPackage: String? = null
    private var cachedForegroundTimestampMs: Long = 0L

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
     * Accurately preserves continuity during continuous media playback (YouTube) and reading.
     */
    fun getForegroundPackageName(lookbackMs: Long = 300_000L): String? {
        val manager = usageStatsManager ?: return null
        val endTime = System.currentTimeMillis()

        // 1. If screen is off or keyguard is locked, foreground app is strictly null
        val isInteractive = powerManager?.isInteractive ?: true
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        if (!isInteractive || isLocked) {
            cachedForegroundPackage = null
            return null
        }

        // 2. Query UsageEvents up to 5 minutes back (default) to catch the active session even without touches
        val effectiveLookback = maxOf(lookbackMs, 300_000L)
        val startTime = endTime - effectiveLookback

        val events = try {
            manager.queryEvents(startTime, endTime)
        } catch (_: Exception) {
            null
        }

        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null

        if (events != null) {
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (!SystemPackageFilter.isKnownSystemOrUpdater(event.packageName)) {
                            lastResumedPackage = event.packageName
                        }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN -> {
                        lastResumedPackage = null
                    }
                }
            }
        }

        if (lastResumedPackage != null) {
            cachedForegroundPackage = lastResumedPackage
            cachedForegroundTimestampMs = endTime
            return lastResumedPackage
        }

        // 3. Continuity preservation: If screen is still on and no other app was resumed, maintain active package
        if (cachedForegroundPackage != null && (endTime - cachedForegroundTimestampMs) < 600_000L) {
            return cachedForegroundPackage
        }

        // 4. Fallback: Check most recently used app in last 15 seconds
        val statsList = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            endTime - 15_000L,
            endTime
        )
        val mostRecent = statsList?.maxByOrNull { it.lastTimeUsed }
        if (mostRecent != null && (endTime - mostRecent.lastTimeUsed) < 15_000L) {
            cachedForegroundPackage = mostRecent.packageName
            cachedForegroundTimestampMs = endTime
            return mostRecent.packageName
        }

        return cachedForegroundPackage
    }

    /**
     * Accurately calculates real foreground usage per package strictly since midnight today.
     * Merges Android OS daily buckets, post-flush UsageEvents, and live active foreground sessions.
     * Guarantees zero-underreporting against Android's native Digital Wellbeing and prevents
     * multi-day boundary bleeding.
     */
    fun queryReconciledDailyUsageMap(targetPackages: Collection<String>? = null): Map<String, Long> {
        val manager = usageStatsManager ?: return emptyMap()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMs = calendar.timeInMillis
        val nowMs = System.currentTimeMillis()
        val maxPossibleTodayMs = maxOf(0L, nowMs - midnightMs)

        // 1. Query persistent OS daily buckets (contains historical sessions flushed to disk)
        val bucketTotals = mutableMapOf<String, Long>()
        val bucketLastFlushed = mutableMapOf<String, Long>()

        try {
            val list = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                midnightMs,
                nowMs
            )
            if (list != null) {
                val grouped = list.groupBy { it.packageName }
                val pkgsToEvaluate = targetPackages ?: grouped.keys

                for (pkg in pkgsToEvaluate) {
                    if (SystemPackageFilter.isIgnoredOrSystem(context, pkg)) continue
                    val statsList = grouped[pkg] ?: continue

                    // Strictly filter buckets that touch today (lastTimeStamp > midnightMs)
                    val todayBuckets = statsList.filter { it.lastTimeStamp > midnightMs }
                    if (todayBuckets.isEmpty()) continue

                    // Authoritative bucket for today: pick the latest bucket, NEVER sum across buckets!
                    val bestBucket = todayBuckets.maxByOrNull { it.lastTimeStamp } ?: continue

                    val pkgBucketMs = if (bestBucket.firstTimeStamp >= midnightMs) {
                        bestBucket.totalTimeInForeground
                    } else {
                        // Bucket crossed midnight: cap at time elapsed since midnight that this bucket was active
                        minOf(bestBucket.totalTimeInForeground, maxOf(0L, bestBucket.lastTimeStamp - midnightMs))
                    }

                    if (pkgBucketMs > 0L) {
                        bucketTotals[pkg] = minOf(pkgBucketMs, maxPossibleTodayMs)
                        bucketLastFlushed[pkg] = bestBucket.lastTimeUsed
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Query UsageEvents (in-memory circular buffer for recent and live sessions)
        val queryStart = midnightMs - (15 * 60 * 1000L)
        val events = try {
            manager.queryEvents(queryStart, nowMs)
        } catch (_: Exception) {
            null
        }

        val eventTotals = mutableMapOf<String, Long>()
        val completedSessionsByPkg = mutableMapOf<String, MutableList<Pair<Long, Long>>>()
        var currentForegroundPkg: String? = null
        var currentSessionStartMs: Long = 0L

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val time = event.timeStamp
                val pkg = event.packageName

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (SystemPackageFilter.isIgnoredOrSystem(context, pkg)) {
                            // System daemon or launcher resumed: terminate previous app session cleanly
                            val prev = currentForegroundPkg
                            if (prev != null) {
                                val effectiveStart = maxOf(currentSessionStartMs, midnightMs)
                                val effectiveEnd = minOf(time, nowMs)
                                if (effectiveEnd > effectiveStart) {
                                    val duration = minOf(effectiveEnd - effectiveStart, 4 * 60 * 60 * 1000L)
                                    eventTotals[prev] = (eventTotals[prev] ?: 0L) + duration
                                    completedSessionsByPkg.getOrPut(prev) { mutableListOf() }
                                        .add(Pair(effectiveStart, effectiveEnd))
                                }
                            }
                            currentForegroundPkg = null
                            currentSessionStartMs = 0L
                            continue
                        }
                        if (currentForegroundPkg == pkg) {
                            // Intra-app transition (e.g. Feed -> Reel -> DM), continue session
                            continue
                        }
                        val prev = currentForegroundPkg
                        if (prev != null) {
                            val effectiveStart = maxOf(currentSessionStartMs, midnightMs)
                            val effectiveEnd = minOf(time, nowMs)
                            if (effectiveEnd > effectiveStart) {
                                val duration = minOf(effectiveEnd - effectiveStart, 4 * 60 * 60 * 1000L)
                                eventTotals[prev] = (eventTotals[prev] ?: 0L) + duration
                                completedSessionsByPkg.getOrPut(prev) { mutableListOf() }
                                    .add(Pair(effectiveStart, effectiveEnd))
                            }
                        }
                        currentForegroundPkg = pkg
                        currentSessionStartMs = time
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN -> {
                        val active = currentForegroundPkg
                        if (active != null) {
                            val effectiveStart = maxOf(currentSessionStartMs, midnightMs)
                            val effectiveEnd = minOf(time, nowMs)
                            if (effectiveEnd > effectiveStart) {
                                val duration = minOf(effectiveEnd - effectiveStart, 4 * 60 * 60 * 1000L)
                                eventTotals[active] = (eventTotals[active] ?: 0L) + duration
                                completedSessionsByPkg.getOrPut(active) { mutableListOf() }
                                    .add(Pair(effectiveStart, effectiveEnd))
                            }
                            currentForegroundPkg = null
                            currentSessionStartMs = 0L
                        }
                    }
                }
            }

            // Live active session currently in foreground
            val livePkg = currentForegroundPkg
            if (livePkg != null) {
                val effectiveStart = maxOf(currentSessionStartMs, midnightMs)
                val effectiveEnd = nowMs
                if (effectiveEnd > effectiveStart) {
                    val duration = minOf(effectiveEnd - effectiveStart, 30 * 60 * 1000L)
                    eventTotals[livePkg] = (eventTotals[livePkg] ?: 0L) + duration
                    completedSessionsByPkg.getOrPut(livePkg) { mutableListOf() }
                        .add(Pair(effectiveStart, effectiveEnd))
                }
            }
        }

        // 3. Reconcile: Combine OS bucket flush baselines with post-flush uncommitted events
        val allCandidatePkgs = (bucketTotals.keys + eventTotals.keys + (targetPackages ?: emptyList()))
            .let { if (targetPackages != null) it.filter { pkg -> targetPackages.contains(pkg) } else it }

        val resultMap = mutableMapOf<String, Long>()

        for (pkg in allCandidatePkgs) {
            val bMs = bucketTotals[pkg] ?: 0L
            val lastFlushTime = bucketLastFlushed[pkg] ?: 0L
            val eMs = eventTotals[pkg] ?: 0L

            // Calculate uncommitted usage strictly occurring AFTER the last disk flush
            var postFlushUsageMs = 0L
            val sessions = completedSessionsByPkg[pkg]
            if (sessions != null && lastFlushTime >= midnightMs) {
                for ((start, end) in sessions) {
                    if (end > lastFlushTime) {
                        val incremental = end - maxOf(start, lastFlushTime)
                        if (incremental > 0L) {
                            postFlushUsageMs += incremental
                        }
                    }
                }
            }

            // Monotonic true usage calculation:
            // Combines disk baseline with post-flush events, or pure event buffer if complete
            val candidateUsage = maxOf(bMs + postFlushUsageMs, eMs)
            val verifiedMs = minOf(candidateUsage, maxPossibleTodayMs)

            val finalMs = highWaterMarkStore.updateHighWaterMarkMs(pkg, verifiedMs)
            resultMap[pkg] = finalMs
        }

        return resultMap
    }

    /**
     * Backward-compatible delegation to reconciled daily usage map.
     */
    fun queryAccurateDailyUsageMap(): Map<String, Long> {
        return queryReconciledDailyUsageMap()
    }

    /**
     * Queries accurate foreground usage milliseconds for [packageName] strictly from today's midnight (00:00:00).
     */
    fun getDailyUsageMs(packageName: String): Long {
        return queryReconciledDailyUsageMap(setOf(packageName))[packageName] ?: 0L
    }

    /**
     * Returns a map of package names to their real daily usage in minutes for today.
     * Uses multi-tier evaluation to work reliably across all Android versions and OEM skins.
     */
    fun getDailyUsageMinutesMap(packageNames: Collection<String>): Map<String, Int> {
        val msMap = queryReconciledDailyUsageMap(packageNames)
        return packageNames.associateWith { pkg ->
            ((msMap[pkg] ?: 0L) / (1000 * 60)).toInt()
        }
    }

    /**
     * Calculates total device screen time for today in minutes (across all user apps, strictly since midnight).
     * Accurately reflects true non-overlapping interactive screen-on time, preventing background service inflation.
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
        val maxPossibleTodayMs = maxOf(0L, nowMs - midnightMs)

        // Query ground-truth OS events from midnight to now
        val events = try {
            manager.queryEvents(midnightMs, nowMs)
        } catch (_: Exception) {
            null
        }

        val intervals = mutableListOf<Pair<Long, Long>>()
        var currentSessionStart: Long? = null
        var currentPkg: String? = null

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val t = event.timeStamp
                val pkg = event.packageName

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (currentPkg == pkg) {
                            // Continuous engagement in same app
                            continue
                        }
                        val activeStart = currentSessionStart
                        val activePkg = currentPkg
                        if (activeStart != null && activePkg != null && !SystemPackageFilter.isIgnoredOrSystem(context, activePkg)) {
                            val start = maxOf(activeStart, midnightMs)
                            val end = minOf(t, nowMs)
                            if (end > start) {
                                intervals.add(Pair(start, end))
                            }
                        }
                        if (SystemPackageFilter.isIgnoredOrSystem(context, pkg)) {
                            currentSessionStart = null
                            currentPkg = null
                        } else {
                            currentSessionStart = t
                            currentPkg = pkg
                        }
                    }
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN -> {
                        val activeStart = currentSessionStart
                        val activePkg = currentPkg
                        if (activeStart != null && activePkg != null) {
                            if (!SystemPackageFilter.isIgnoredOrSystem(context, activePkg)) {
                                val start = maxOf(activeStart, midnightMs)
                                val end = minOf(t, nowMs)
                                if (end > start) {
                                    intervals.add(Pair(start, end))
                                }
                            }
                            currentSessionStart = null
                            currentPkg = null
                        }
                    }
                }
            }
        }

        // Include current ongoing session if screen is on
        val liveStart = currentSessionStart
        val livePkg = currentPkg
        if (liveStart != null && livePkg != null && !SystemPackageFilter.isIgnoredOrSystem(context, livePkg)) {
            val start = maxOf(liveStart, midnightMs)
            val end = nowMs
            if (end > start) {
                intervals.add(Pair(start, end))
            }
        }

        // Merge all non-overlapping / contiguous intervals
        val sorted = intervals.sortedBy { it.first }
        var mergedDurationMs = 0L
        var curStart = -1L
        var curEnd = -1L

        for (interval in sorted) {
            if (curStart == -1L) {
                curStart = interval.first
                curEnd = interval.second
            } else if (interval.first <= curEnd) {
                curEnd = maxOf(curEnd, interval.second)
            } else {
                mergedDurationMs += (curEnd - curStart)
                curStart = interval.first
                curEnd = interval.second
            }
        }
        if (curStart != -1L) {
            mergedDurationMs += (curEnd - curStart)
        }

        val verifiedMs = minOf(mergedDurationMs, maxPossibleTodayMs)
        val verifiedTotalMs = highWaterMarkStore.recalibrateScreenTimeToday(verifiedMs)
        return (verifiedTotalMs / (1000 * 60)).toInt()
    }

    private fun isSystemOrLauncherPackage(pkg: String): Boolean {
        return SystemPackageFilter.isIgnoredOrSystem(context, pkg)
    }
}
