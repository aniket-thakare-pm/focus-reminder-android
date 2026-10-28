package com.example.focusreminder.domain.model

/**
 * An individual foreground usage session for an app.
 */
data class UsageSession(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val sectorId: String,
    val category: String
)

/**
 * A time-of-day sector (e.g. Morning, Afternoon, Evening, Night).
 */
data class TimeSector(
    val id: String,
    val name: String,
    val startHour: Int,       // 0..23
    val startMinute: Int = 0, // 0..59
    val endHour: Int,         // 0..23 (can wrap around midnight, e.g. 22 to 6)
    val endMinute: Int = 0,   // 0..59
    val colorLong: Long
)

/**
 * A persistent reflective journal entry recorded during an intentional emergency override.
 */
data class JournalReflectionRecord(
    val id: Long = 0,
    val timestampMs: Long,
    val packageName: String,
    val appName: String,
    val requestedMinutes: Int,
    val answers: List<String>
)

enum class AnalyticsTimeframe {
    TODAY,
    WEEK,
    MONTH
}

data class SectorUsageStat(
    val sector: TimeSector,
    val durationMinutes: Int,
    val percentage: Float
)

data class SessionBucketStats(
    val microCount: Int,     // < 1 min
    val shortCount: Int,     // 1 - 5 min
    val standardCount: Int,  // 5 - 10 min
    val cappedCount: Int,    // 10 min+
    val totalSessions: Int
)

data class DayUsagePoint(
    val dateLabel: String,      // e.g. "Mon 23"
    val dayOfWeek: String,      // e.g. "Mon"
    val timestampMs: Long,
    val usageMinutes: Int,
    val isToday: Boolean
)

enum class GoalEnforcementMode {
    TRACKING_ONLY,            // Option 1: Track screen time & breaches in analytics, no apps locked
    BLOCK_CONTROLLED_APPS,    // Option 2: Lock all controlled/monitored apps (Instagram, YouTube, etc.)
    STRICT_LOCKDOWN           // Option 3: Lock ALL apps on the device except allowed essential apps
}

data class BufferGoal(
    val id: String,
    val name: String,
    val startHour: Int,
    val startMinute: Int = 0,
    val endHour: Int,
    val endMinute: Int = 0,
    val enforcementMode: GoalEnforcementMode = GoalEnforcementMode.TRACKING_ONLY,
    val allowedPackages: Set<String> = emptySet(),
    val isEnabled: Boolean = true
)

data class BufferGoalStatus(
    val id: String,
    val name: String,
    val targetBufferHours: String, // e.g. "23:00 – 07:00"
    val startHour: Int,
    val startMinute: Int = 0,
    val endHour: Int,
    val endMinute: Int = 0,
    val enforcementMode: GoalEnforcementMode = GoalEnforcementMode.TRACKING_ONLY,
    val allowedPackages: Set<String> = emptySet(),
    val allowedPackageCount: Int = 0,
    val isEnabled: Boolean = true,
    val isPassed: Boolean,
    val breachMinutes: Int,
    val breachTimeDesc: String,
    val weeklyComplianceRate: Float, // e.g. 0.71f for 71%
    val weeklyMetDays: Int,
    val weeklyTotalDays: Int
)

data class CategoryUsageStat(
    val category: String,
    val durationMinutes: Int,
    val percentage: Float
)

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val durationMinutes: Int,
    val sessionCount: Int,
    val percentage: Float,
    val isControlled: Boolean = false
)

data class AnalyticsSummary(
    val timeframe: AnalyticsTimeframe,
    val dateDisplay: String,
    val scopeLabel: String,
    val isControlledScope: Boolean,
    val totalUsageMinutes: Int,
    val dailyAverageMinutes: Int,
    val averageSessionDurationMs: Long,
    val totalSessionCount: Int,
    val vsAveragePercentage: Int, // -14 for -14% vs avg, +10 for +10%
    val sectors: List<SectorUsageStat>,
    val peakSector: SectorUsageStat?,
    val buckets: SessionBucketStats,
    val dailyPoints: List<DayUsagePoint>,
    val bufferGoals: List<BufferGoalStatus>,
    val categories: List<CategoryUsageStat>,
    val topApps: List<AppUsageStat>,
    val availableApps: List<AppUsageStat> = emptyList(),
    val availableCategories: List<String> = emptyList()
)
