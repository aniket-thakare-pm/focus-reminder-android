package com.example.focusreminder.domain.model

/**
 * Represents a single foreground usage session in the hardware audit logbook.
 * Provides per-minute and second precision for timeline verification.
 */
data class UsageLogEntry(
    val id: String,
    val packageName: String,
    val appName: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val timeRangeFormatted: String,   // e.g. "10:14 AM – 10:23 AM"
    val durationFormatted: String,    // e.g. "9m 12s" or "45s"
    val statusBadge: String,          // "In Use", "Blocker Screen", "Controlled"
    val isControlled: Boolean,
    val isHostApp: Boolean,
    val sectorId: String = "",
    val sectorName: String = "",
    val sectorColorLong: Long = 0xFF10B981L
)

data class UsageLogSummary(
    val dateLabel: String,
    val totalScreenTimeMinutes: Int,
    val totalSessionsCount: Int,
    val controlledUsageMinutes: Int,
    val logs: List<UsageLogEntry>
)
