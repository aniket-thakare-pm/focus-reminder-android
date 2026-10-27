package com.example.focusreminder.platform.logs

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.focusreminder.domain.model.UsageLogEntry
import com.example.focusreminder.domain.model.UsageLogSummary
import com.example.focusreminder.platform.AppRestrictionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Extracts and formats per-minute and second ground-truth hardware session logs from
 * Android's UsageEvents stream for the user-facing Logbook audit trail.
 */
class UsageLogRepository private constructor(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val pm: PackageManager = context.packageManager
    private val restrictionRepo = AppRestrictionRepository.getInstance(context)

    companion object {
        @Volatile
        private var INSTANCE: UsageLogRepository? = null

        fun getInstance(context: Context): UsageLogRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsageLogRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun getUsageLogs(dateOffset: Int = 0): UsageLogSummary = withContext(Dispatchers.IO) {
        val manager = usageStatsManager ?: return@withContext UsageLogSummary(
            dateLabel = if (dateOffset == 0) "Today" else "Yesterday",
            totalScreenTimeMinutes = 0,
            totalSessionsCount = 0,
            controlledUsageMinutes = 0,
            logs = emptyList()
        )

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dateOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayMs = cal.timeInMillis
        val isToday = dateOffset == 0
        val endOfDayMs = if (isToday) System.currentTimeMillis() else (startOfDayMs + 24 * 60 * 60 * 1000L - 1L)

        val controlledPkgs = restrictionRepo.getRestrictedPackages().toSet()

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateLabel = when (dateOffset) {
            0 -> "Today"
            -1 -> "Yesterday"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time)
        }

        val events = try {
            manager.queryEvents(startOfDayMs, endOfDayMs)
        } catch (_: Exception) {
            null
        }

        val rawSessions = mutableListOf<RawSession>()
        var currentPkg: String? = null
        var sessionStartMs: Long = 0L

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val time = event.timeStamp
                val pkg = event.packageName

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        val prev = currentPkg
                        if (prev != null && prev != pkg) {
                            val duration = time - sessionStartMs
                            // Record any session >= 3 seconds (ignores transient system flicker)
                            if (duration >= 3000L) {
                                rawSessions.add(RawSession(prev, sessionStartMs, time, duration))
                            }
                        }
                        currentPkg = pkg
                        sessionStartMs = time
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED,
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.KEYGUARD_SHOWN -> {
                        val active = currentPkg
                        if (active != null && (active == pkg || event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE || event.eventType == UsageEvents.Event.KEYGUARD_SHOWN)) {
                            val duration = time - sessionStartMs
                            if (duration >= 3000L) {
                                rawSessions.add(RawSession(active, sessionStartMs, time, duration))
                            }
                            currentPkg = null
                        }
                    }
                }
            }
        }

        // Include current ongoing session if looking at today
        val activeAtEnd = currentPkg
        if (isToday && activeAtEnd != null && endOfDayMs > sessionStartMs) {
            val duration = endOfDayMs - sessionStartMs
            if (duration >= 3000L) {
                rawSessions.add(RawSession(activeAtEnd, sessionStartMs, endOfDayMs, duration))
            }
        }

        val appNameCache = mutableMapOf<String, String>()
        fun resolveName(pkg: String): String {
            return appNameCache.getOrPut(pkg) {
                if (pkg == context.packageName) {
                    "Focus Reminder"
                } else {
                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    }
                }
            }
        }

        fun formatDuration(ms: Long): String {
            val totalSec = ms / 1000L
            val mins = totalSec / 60L
            val secs = totalSec % 60L
            return when {
                mins > 0 && secs > 0 -> "${mins}m ${secs}s"
                mins > 0 -> "${mins}m"
                else -> "${secs}s"
            }
        }

        var totalControlledMs = 0L
        val entries = mutableListOf<UsageLogEntry>()

        for (session in rawSessions) {
            val pkg = session.packageName
            val isHost = pkg == context.packageName
            val isControlled = controlledPkgs.contains(pkg)

            if (isControlled) {
                totalControlledMs += session.durationMs
            }

            val appLabel = resolveName(pkg)
            val startTimeStr = timeFormat.format(session.startMs)
            val endTimeStr = timeFormat.format(session.endMs)

            val statusBadge = when {
                isHost -> "Blocker Screen"
                isControlled -> "Controlled"
                else -> "In Use"
            }

            entries.add(
                UsageLogEntry(
                    id = "${pkg}_${session.startMs}",
                    packageName = pkg,
                    appName = appLabel,
                    startTimeMs = session.startMs,
                    endTimeMs = session.endMs,
                    durationMs = session.durationMs,
                    timeRangeFormatted = "$startTimeStr – $endTimeStr",
                    durationFormatted = formatDuration(session.durationMs),
                    statusBadge = statusBadge,
                    isControlled = isControlled,
                    isHostApp = isHost
                )
            )
        }

        // Calculate non-overlapping screen time for this day
        val nonOverlappingIntervals = rawSessions.map { Pair(it.startMs, it.endMs) }.sortedBy { it.first }
        var mergedMs = 0L
        var curStart = -1L
        var curEnd = -1L
        for (interval in nonOverlappingIntervals) {
            if (curStart == -1L) {
                curStart = interval.first
                curEnd = interval.second
            } else if (interval.first <= curEnd) {
                curEnd = maxOf(curEnd, interval.second)
            } else {
                mergedMs += (curEnd - curStart)
                curStart = interval.first
                curEnd = interval.second
            }
        }
        if (curStart != -1L) {
            mergedMs += (curEnd - curStart)
        }

        // Reverse chronological (newest at the top)
        val sortedEntries = entries.reversed()

        UsageLogSummary(
            dateLabel = dateLabel,
            totalScreenTimeMinutes = (mergedMs / (60 * 1000L)).toInt(),
            totalSessionsCount = entries.size,
            controlledUsageMinutes = (totalControlledMs / (60 * 1000L)).toInt(),
            logs = sortedEntries
        )
    }

    private data class RawSession(
        val packageName: String,
        val startMs: Long,
        val endMs: Long,
        val durationMs: Long
    )
}
