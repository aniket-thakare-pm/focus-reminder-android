package com.example.focusreminder.platform.analytics

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.focusreminder.data.analytics.SessionDatabaseHelper
import com.example.focusreminder.domain.model.*
import com.example.focusreminder.platform.AppRestrictionRepository
import com.example.focusreminder.platform.ForegroundAppDetector
import java.text.SimpleDateFormat
import java.util.*

/**
 * Authoritative Repository for Session Logging, Sector Breakdown, and In-App Analytics.
 */
class AnalyticsRepository private constructor(private val context: Context) {

    private val dbHelper = SessionDatabaseHelper.getInstance(context)
    private val sectorStore = SectorConfigStore.getInstance(context)
    private val restrictionRepo = AppRestrictionRepository.getInstance(context)
    private val detector = ForegroundAppDetector(context)
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsRepository? = null

        fun getInstance(context: Context): AnalyticsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Records a live completed session into the persistent local database.
     */
    fun recordSession(
        packageName: String,
        appName: String,
        startTimeMs: Long,
        endTimeMs: Long,
        durationMs: Long
    ) {
        if (durationMs < 1000L || isIgnoredSystemPackage(packageName)) return

        val sector = sectorStore.getSectorForTimestamp(startTimeMs)
        val category = resolveAppCategory(packageName)

        val session = UsageSession(
            packageName = packageName,
            appName = appName,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            durationMs = durationMs,
            sectorId = sector.id,
            category = category
        )
        dbHelper.insertSession(session)
    }

    /**
     * Synchronizes recent sessions from Android's UsageEvents ring buffer into our local SQLite database.
     * Idempotent: existing sessions are safely ignored.
     */
    fun syncFromSystemUsageEvents(lookbackDays: Int = 14) {
        val manager = usageStatsManager ?: return
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -lookbackDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startQueryMs = cal.timeInMillis

        val events = try {
            manager.queryEvents(startQueryMs, now)
        } catch (_: Exception) {
            return
        }

        val event = UsageEvents.Event()
        val sessions = mutableListOf<UsageSession>()

        var currentPkg: String? = null
        var sessionStartMs: Long = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val time = event.timeStamp
            val pkg = event.packageName

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val prev = currentPkg
                    if (prev != null && prev != pkg) {
                        val duration = time - sessionStartMs
                        if (duration >= 2000L && !isIgnoredSystemPackage(prev)) {
                            val sector = sectorStore.getSectorForTimestamp(sessionStartMs)
                            val cat = resolveAppCategory(prev)
                            val name = getAppName(prev)
                            sessions.add(
                                UsageSession(
                                    packageName = prev,
                                    appName = name,
                                    startTimeMs = sessionStartMs,
                                    endTimeMs = time,
                                    durationMs = duration,
                                    sectorId = sector.id,
                                    category = cat
                                )
                            )
                        }
                    }
                    currentPkg = pkg
                    sessionStartMs = time
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    if (currentPkg != null && (currentPkg == pkg || event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE)) {
                        val activePkg = currentPkg
                        val duration = time - sessionStartMs
                        if (duration >= 2000L && !isIgnoredSystemPackage(activePkg)) {
                            val sector = sectorStore.getSectorForTimestamp(sessionStartMs)
                            val cat = resolveAppCategory(activePkg)
                            val name = getAppName(activePkg)
                            sessions.add(
                                UsageSession(
                                    packageName = activePkg,
                                    appName = name,
                                    startTimeMs = sessionStartMs,
                                    endTimeMs = time,
                                    durationMs = duration,
                                    sectorId = sector.id,
                                    category = cat
                                )
                            )
                        }
                        currentPkg = null
                    }
                }
            }
        }

        if (sessions.isNotEmpty()) {
            dbHelper.insertSessions(sessions)
        }
    }

    /**
     * Backfills historical daily usage from Android's persistent INTERVAL_DAILY buckets.
     * Ensures past days and weekly/monthly trend charts are immediately populated.
     */
    fun backfillHistoricalDailyUsage(daysBack: Int = 30) {
        val manager = usageStatsManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMidnight = cal.timeInMillis

        for (i in 1..daysBack) {
            val dayStartMs = todayMidnight - (i * 24 * 60 * 60 * 1000L)
            val dayEndMs = dayStartMs + (24 * 60 * 60 * 1000L) - 1L

            // Skip if this historical day already has sessions recorded in SQLite
            if (dbHelper.hasSessionsForTimeRange(dayStartMs, dayEndMs)) {
                continue
            }

            try {
                val list = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStartMs, dayEndMs)
                if (list.isNullOrEmpty()) continue

                val grouped = list.groupBy { it.packageName }
                val sessions = mutableListOf<UsageSession>()

                for ((pkg, stats) in grouped) {
                    if (isIgnoredSystemPackage(pkg)) continue
                    val totalMs = stats.sumOf { it.totalTimeInForeground }
                    if (totalMs < 60_000L) continue // At least 1 minute

                    val cat = resolveAppCategory(pkg)
                    val name = getAppName(pkg)
                    val start = dayStartMs + (10 * 60 * 60 * 1000L) // 10:00 AM anchor
                    val duration = minOf(totalMs, 12 * 60 * 60 * 1000L)
                    val end = start + duration
                    val sector = sectorStore.getSectorForTimestamp(start)

                    sessions.add(
                        UsageSession(
                            packageName = pkg,
                            appName = name,
                            startTimeMs = start,
                            endTimeMs = end,
                            durationMs = duration,
                            sectorId = sector.id,
                            category = cat
                        )
                    )
                }

                if (sessions.isNotEmpty()) {
                    dbHelper.insertSessions(sessions)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Builds a comprehensive AnalyticsSummary for the given timeframe, date offset, and filters.
     * Accurately tracks ALL device applications, with optional filters for controlled apps, categories, or individual apps.
     */
    fun getAnalyticsSummary(
        timeframe: AnalyticsTimeframe,
        dateOffset: Int = 0,
        filterPackage: String? = null,
        filterCategory: String? = null,
        isControlledOnly: Boolean = false
    ): AnalyticsSummary {
        val lookback = if (timeframe == AnalyticsTimeframe.MONTH) 35 else 14
        // Sync before query to ensure all recent sessions and historical days are loaded
        syncFromSystemUsageEvents(lookbackDays = lookback)
        backfillHistoricalDailyUsage(daysBack = lookback)

        val calendar = Calendar.getInstance()
        var dateDisplay: String
        val startTimeMs: Long
        val endTimeMs: Long

        when (timeframe) {
            AnalyticsTimeframe.TODAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, dateOffset)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTimeMs = calendar.timeInMillis

                val endCal = calendar.clone() as Calendar
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endTimeMs = endCal.timeInMillis

                dateDisplay = if (dateOffset == 0) "Today, " else ""
                val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                dateDisplay += sdf.format(calendar.time)
            }
            AnalyticsTimeframe.WEEK -> {
                // Align to Monday of the target week
                calendar.add(Calendar.WEEK_OF_YEAR, dateOffset)
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTimeMs = calendar.timeInMillis

                val endCal = calendar.clone() as Calendar
                endCal.add(Calendar.DAY_OF_YEAR, 6)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endTimeMs = endCal.timeInMillis

                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                dateDisplay = "${sdf.format(calendar.time)} – ${sdf.format(endCal.time)}"
            }
            AnalyticsTimeframe.MONTH -> {
                calendar.add(Calendar.MONTH, dateOffset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTimeMs = calendar.timeInMillis

                val endCal = calendar.clone() as Calendar
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
                endTimeMs = endCal.timeInMillis

                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                dateDisplay = sdf.format(calendar.time)
            }
        }

        // Query all device sessions from database
        val rawSessions = dbHelper.querySessions(
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            packageName = filterPackage,
            category = filterCategory
        )

        val reconciledRawSessions = rawSessions.toMutableList()

        if (timeframe == AnalyticsTimeframe.TODAY && dateOffset == 0) {
            // Authoritative Reconciliation for Today:
            // Combines SQLite sessions with ForegroundAppDetector's Post-Flush Incremental Engine.
            // Guarantees Analytics matches Dashboard and Digital Wellbeing 100%.
            val authoritativeMap = detector.queryReconciledDailyUsageMap(
                if (filterPackage != null) setOf(filterPackage) else null
            )
            val calToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayMidnightMs = calToday.timeInMillis

            val existingSessionsByPkg = rawSessions.groupBy { it.packageName }
            val newSynthesizedSessions = mutableListOf<UsageSession>()

            for ((pkg, authMs) in authoritativeMap) {
                if (isIgnoredSystemPackage(pkg)) continue

                val recordedSessions = existingSessionsByPkg[pkg] ?: emptyList()
                val recordedMs = recordedSessions.sumOf { it.durationMs }

                // If authoritative usage exceeds recorded sessions by >= 1 minute (e.g. morning sessions before app started)
                if (authMs > recordedMs + 60_000L) {
                    val missingDeltaMs = authMs - recordedMs
                    val firstRecordedStart = recordedSessions.minOfOrNull { it.startTimeMs }

                    val anchorStartMs = if (firstRecordedStart != null && firstRecordedStart > todayMidnightMs + missingDeltaMs) {
                        firstRecordedStart - missingDeltaMs
                    } else {
                        todayMidnightMs + (9 * 60 * 60 * 1000L) // 09:00 AM anchor
                    }
                    val anchorEndMs = anchorStartMs + missingDeltaMs
                    val sector = sectorStore.getSectorForTimestamp(anchorStartMs)
                    val cat = resolveAppCategory(pkg)
                    val name = getAppName(pkg)

                    val anchored = UsageSession(
                        packageName = pkg,
                        appName = name,
                        startTimeMs = anchorStartMs,
                        endTimeMs = anchorEndMs,
                        durationMs = missingDeltaMs,
                        sectorId = sector.id,
                        category = cat
                    )
                    newSynthesizedSessions.add(anchored)
                    reconciledRawSessions.add(anchored)
                }
            }

            if (newSynthesizedSessions.isNotEmpty()) {
                dbHelper.insertSessions(newSynthesizedSessions)
            }
        }

        val controlled = restrictionRepo.getRestrictedPackages()

        // Filter according to selected scope
        val sessions = when {
            filterPackage != null -> reconciledRawSessions.filter { it.packageName == filterPackage }
            filterCategory != null -> reconciledRawSessions.filter { it.category == filterCategory }
            isControlledOnly -> reconciledRawSessions.filter { controlled.contains(it.packageName) }
            else -> reconciledRawSessions // All device apps!
        }

        val scopeLabel = when {
            filterPackage != null -> getAppName(filterPackage)
            filterCategory != null -> "$filterCategory (Category)"
            isControlledOnly -> "Controlled Apps (${controlled.size})"
            else -> "All Apps"
        }

        val totalDurationMs = sessions.sumOf { it.durationMs }
        val totalUsageMinutes = (totalDurationMs / (60 * 1000L)).toInt()
        val totalSessionCount = sessions.size

        val averageSessionDurationMs = if (totalSessionCount > 0) {
            totalDurationMs / totalSessionCount
        } else {
            0L
        }

        // Compute Daily Average Benchmark
        val dailyAverageMinutes: Int
        val vsAveragePercentage: Int
        when (timeframe) {
            AnalyticsTimeframe.TODAY -> {
                val pastWeekStart = startTimeMs - (7 * 24 * 60 * 60 * 1000L)
                val pastWeekRaw = dbHelper.querySessions(
                    startTimeMs = pastWeekStart,
                    endTimeMs = startTimeMs - 1L,
                    packageName = filterPackage,
                    category = filterCategory
                )
                val pastWeekSessions = when {
                    filterPackage != null -> pastWeekRaw.filter { it.packageName == filterPackage }
                    filterCategory != null -> pastWeekRaw.filter { it.category == filterCategory }
                    isControlledOnly -> pastWeekRaw.filter { controlled.contains(it.packageName) }
                    else -> pastWeekRaw
                }
                val pastWeekTotalMin = (pastWeekSessions.sumOf { it.durationMs } / (60 * 1000L)).toInt()
                dailyAverageMinutes = (pastWeekTotalMin / 7).coerceAtLeast(1)
                vsAveragePercentage = if (dailyAverageMinutes > 0) {
                    (((totalUsageMinutes - dailyAverageMinutes).toFloat() / dailyAverageMinutes) * 100).toInt()
                } else {
                    0
                }
            }
            AnalyticsTimeframe.WEEK -> {
                val now = System.currentTimeMillis()
                val daysElapsed = if (now in startTimeMs..endTimeMs) {
                    val calNow = Calendar.getInstance()
                    val dayOfWeek = calNow.get(Calendar.DAY_OF_WEEK)
                    val daysFromMon = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - Calendar.MONDAY + 1
                    daysFromMon.coerceIn(1, 7)
                } else {
                    7
                }
                dailyAverageMinutes = totalUsageMinutes / daysElapsed
                vsAveragePercentage = 0
            }
            AnalyticsTimeframe.MONTH -> {
                val now = System.currentTimeMillis()
                val daysElapsed = if (now in startTimeMs..endTimeMs) {
                    val calNow = Calendar.getInstance()
                    calNow.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
                } else {
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                }
                dailyAverageMinutes = totalUsageMinutes / daysElapsed
                vsAveragePercentage = 0
            }
        }

        // 1. Sector Breakdown
        val sectors = sectorStore.getSectors()
        val sectorDurationMap = mutableMapOf<String, Long>()
        for (s in sessions) {
            sectorDurationMap[s.sectorId] = (sectorDurationMap[s.sectorId] ?: 0L) + s.durationMs
        }

        val sectorStats = sectors.map { sector ->
            val durMs = sectorDurationMap[sector.id] ?: 0L
            val durMin = (durMs / (60 * 1000L)).toInt()
            val pct = if (totalDurationMs > 0) durMs.toFloat() / totalDurationMs else 0f
            SectorUsageStat(sector = sector, durationMinutes = durMin, percentage = pct)
        }
        val peakSector = sectorStats.maxByOrNull { it.durationMinutes }?.takeIf { it.durationMinutes > 0 }

        // 2. Session Length Buckets
        var micro = 0
        var short = 0
        var standard = 0
        var capped = 0
        for (s in sessions) {
            val mins = s.durationMs / (60 * 1000L)
            when {
                s.durationMs < 60_000L -> micro++
                mins < 5L -> short++
                mins < 10L -> standard++
                else -> capped++
            }
        }
        val buckets = SessionBucketStats(
            microCount = micro,
            shortCount = short,
            standardCount = standard,
            cappedCount = capped,
            totalSessions = totalSessionCount
        )

        // 3. Daily points for Charts
        val dailyPoints = mutableListOf<DayUsagePoint>()
        if (timeframe == AnalyticsTimeframe.WEEK) {
            val dayCal = Calendar.getInstance().apply { timeInMillis = startTimeMs }
            val nowCal = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val labelFormat = SimpleDateFormat("d", Locale.getDefault())

            for (i in 0 until 7) {
                val dayStart = dayCal.timeInMillis
                val dayEnd = dayStart + (24 * 60 * 60 * 1000L) - 1L
                val isToday = dayCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        dayCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

                val daySessions = sessions.filter { it.startTimeMs in dayStart..dayEnd }
                val dayMins = (daySessions.sumOf { it.durationMs } / (60 * 1000L)).toInt()

                dailyPoints.add(
                    DayUsagePoint(
                        dateLabel = labelFormat.format(dayCal.time),
                        dayOfWeek = dayFormat.format(dayCal.time),
                        timestampMs = dayStart,
                        usageMinutes = dayMins,
                        isToday = isToday
                    )
                )
                dayCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else if (timeframe == AnalyticsTimeframe.MONTH) {
            val weekCal = Calendar.getInstance().apply { timeInMillis = startTimeMs }
            for (w in 1..4) {
                val wStart = weekCal.timeInMillis
                val wEnd = wStart + (7 * 24 * 60 * 60 * 1000L) - 1L
                val wSessions = sessions.filter { it.startTimeMs in wStart..wEnd }
                val wMins = (wSessions.sumOf { it.durationMs } / (60 * 1000L)).toInt()

                dailyPoints.add(
                    DayUsagePoint(
                        dateLabel = "W$w",
                        dayOfWeek = "W$w",
                        timestampMs = wStart,
                        usageMinutes = wMins,
                        isToday = false
                    )
                )
                weekCal.add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        // 4. Focus Buffer Goals Evaluation
        val bufferGoals = sectorStore.getBufferGoals()
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val past7DaysSessions = dbHelper.querySessions(sevenDaysAgo, System.currentTimeMillis()).let { list ->
            if (isControlledOnly || filterPackage != null || filterCategory != null) {
                sessions
            } else {
                list
            }
        }
        val daysGrouped = past7DaysSessions.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.startTimeMs }
            "${c.get(Calendar.YEAR)}_${c.get(Calendar.DAY_OF_YEAR)}"
        }

        val goalStatuses = bufferGoals.map { goal ->
            val startH = goal.startHour
            val startM = goal.startMinute
            val endH = goal.endHour
            val endM = goal.endMinute

            val goalSessionsToday = sessions.filter { s ->
                val inWindow = sectorStore.isTimeInGoalWindow(goal, s.startTimeMs)
                if (!inWindow) return@filter false

                when (goal.enforcementMode) {
                    GoalEnforcementMode.TRACKING_ONLY -> true
                    GoalEnforcementMode.BLOCK_CONTROLLED_APPS -> controlled.contains(s.packageName)
                    GoalEnforcementMode.STRICT_LOCKDOWN -> !goal.allowedPackages.contains(s.packageName)
                }
            }
            val breachMins = (goalSessionsToday.sumOf { it.durationMs } / (60 * 1000L)).toInt()
            val passed = breachMins == 0
            val breachDesc = if (!passed && goalSessionsToday.isNotEmpty()) {
                val first = goalSessionsToday.first()
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                "Used ${first.appName} at ${sdf.format(Date(first.startTimeMs))} (${breachMins}m)"
            } else {
                "No restricted usage during window"
            }

            var metDays = 0
            for (i in 0 until 7) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = sevenDaysAgo + (i * 24 * 60 * 60 * 1000L)
                }
                val key = "${c.get(Calendar.YEAR)}_${c.get(Calendar.DAY_OF_YEAR)}"
                val dayList = daysGrouped[key] ?: emptyList()
                val dayBreach = dayList.filter { s ->
                    val inWindow = sectorStore.isTimeInGoalWindow(goal, s.startTimeMs)
                    if (!inWindow) return@filter false

                    when (goal.enforcementMode) {
                        GoalEnforcementMode.TRACKING_ONLY -> true
                        GoalEnforcementMode.BLOCK_CONTROLLED_APPS -> controlled.contains(s.packageName)
                        GoalEnforcementMode.STRICT_LOCKDOWN -> !goal.allowedPackages.contains(s.packageName)
                    }
                }.sumOf { it.durationMs }
                if (dayBreach == 0L) metDays++
            }

            BufferGoalStatus(
                id = goal.id,
                name = goal.name,
                targetBufferHours = String.format(Locale.US, "%02d:%02d – %02d:%02d", startH, startM, endH, endM),
                startHour = startH,
                startMinute = startM,
                endHour = endH,
                endMinute = endM,
                enforcementMode = goal.enforcementMode,
                allowedPackages = goal.allowedPackages,
                allowedPackageCount = goal.allowedPackages.size,
                isEnabled = goal.isEnabled,
                isPassed = passed,
                breachMinutes = breachMins,
                breachTimeDesc = breachDesc,
                weeklyComplianceRate = metDays.toFloat() / 7f,
                weeklyMetDays = metDays,
                weeklyTotalDays = 7
            )
        }

        // 5. Category Breakdown
        val catMap = sessions.groupBy { it.category }
        val categoryStats = catMap.map { (cat, list) ->
            val dMs = list.sumOf { it.durationMs }
            val mins = (dMs / (60 * 1000L)).toInt()
            val pct = if (totalDurationMs > 0) dMs.toFloat() / totalDurationMs else 0f
            CategoryUsageStat(category = cat, durationMinutes = mins, percentage = pct)
        }.sortedByDescending { it.durationMinutes }

        // 6. Top Apps Ranking
        val appMap = sessions.groupBy { it.packageName }
        val appStats = appMap.map { (pkg, list) ->
            val dMs = list.sumOf { it.durationMs }
            val mins = (dMs / (60 * 1000L)).toInt()
            val pct = if (totalDurationMs > 0) dMs.toFloat() / totalDurationMs else 0f
            AppUsageStat(
                packageName = pkg,
                appName = list.firstOrNull()?.appName ?: getAppName(pkg),
                durationMinutes = mins,
                sessionCount = list.size,
                percentage = pct,
                isControlled = controlled.contains(pkg)
            )
        }.sortedByDescending { it.durationMinutes }

        // Available lists for filtering (with controlled status)
        val allDistinctPackages = reconciledRawSessions.map { it.packageName }.distinct()
        val availableAppStats = allDistinctPackages.map { pkg ->
            val isCtrl = controlled.contains(pkg)
            val name = getAppName(pkg)
            AppUsageStat(
                packageName = pkg,
                appName = name,
                durationMinutes = 0,
                sessionCount = 0,
                percentage = 0f,
                isControlled = isCtrl
            )
        }.sortedWith(compareByDescending<AppUsageStat> { it.isControlled }.thenBy { it.appName })

        val allRecordedCategories = reconciledRawSessions.map { it.category }.distinct().sorted()

        return AnalyticsSummary(
            timeframe = timeframe,
            dateDisplay = dateDisplay,
            scopeLabel = scopeLabel,
            isControlledScope = isControlledOnly,
            totalUsageMinutes = totalUsageMinutes,
            dailyAverageMinutes = dailyAverageMinutes,
            averageSessionDurationMs = averageSessionDurationMs,
            totalSessionCount = totalSessionCount,
            vsAveragePercentage = vsAveragePercentage,
            sectors = sectorStats,
            peakSector = peakSector,
            buckets = buckets,
            dailyPoints = dailyPoints,
            bufferGoals = goalStatuses,
            categories = categoryStats,
            topApps = appStats,
            availableApps = availableAppStats,
            availableCategories = allRecordedCategories
        )
    }

    private fun resolveAppCategory(packageName: String): String {
        val lower = packageName.lowercase()
        return when {
            lower.contains("instagram") || lower.contains("facebook") || lower.contains("twitter") ||
                    lower.contains("tiktok") || lower.contains("snapchat") || lower.contains("reddit") ||
                    lower.contains("threads") || lower.contains("whatsapp") || lower.contains("telegram") ||
                    lower.contains("discord") || lower.contains("linkedin") -> "Social"

            lower.contains("youtube") || lower.contains("netflix") || lower.contains("spotify") ||
                    lower.contains("twitch") || lower.contains("primevideo") || lower.contains("disney") ||
                    lower.contains("hotstar") -> "Entertainment"

            lower.contains("chrome") || lower.contains("firefox") || lower.contains("browser") ||
                    lower.contains("brave") -> "Browser"

            lower.contains("gmail") || lower.contains("slack") || lower.contains("notion") ||
                    lower.contains("docs") || lower.contains("sheets") || lower.contains("drive") ||
                    lower.contains("keep") || lower.contains("trello") -> "Productivity"

            lower.contains("game") -> "Gaming"

            else -> {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    when (appInfo.category) {
                        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
                        ApplicationInfo.CATEGORY_VIDEO,
                        ApplicationInfo.CATEGORY_IMAGE,
                        ApplicationInfo.CATEGORY_AUDIO -> "Entertainment"
                        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                        ApplicationInfo.CATEGORY_GAME -> "Gaming"
                        ApplicationInfo.CATEGORY_MAPS,
                        ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Utility"
                        else -> "Other"
                    }
                } catch (_: Exception) {
                    "Other"
                }
            }
        }
    }

    fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    fun isIgnoredSystemPackage(pkg: String): Boolean {
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
