package com.example.focusreminder.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formats a duration in minutes into strict hours and minutes format (e.g. "1h 44m", "0h 30m", "0h 00m").
 */
fun formatHoursMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "${h}h ${String.format("%02d", m)}m"
}

/**
 * Formats an epoch millisecond timestamp into a 12-hour time string with AM/PM (e.g. "04:30 PM").
 */
fun formatTimeOfDay(epochMs: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

/**
 * Formats an epoch millisecond timestamp into a user-friendly date-time string
 * (e.g. "Today, 04:30 PM", "Tomorrow, 09:15 AM", "Oct 26, 04:30 PM").
 */
fun formatReminderDateTime(epochMs: Long): String {
    val targetCal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val todayCal = Calendar.getInstance()
    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    val isToday = targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

    val isTomorrow = targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

    val timeStr = formatTimeOfDay(epochMs)
    return when {
        isToday -> "Today, $timeStr"
        isTomorrow -> "Tomorrow, $timeStr"
        else -> {
            val dateSdf = SimpleDateFormat("MMM d", Locale.getDefault())
            "${dateSdf.format(Date(epochMs))}, $timeStr"
        }
    }
}

/**
 * Formats the relative remaining time until epochMs (e.g. "in 25m", "in 1h 45m", "in 2d 4h", or "Due now").
 */
fun formatRelativeCountdown(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val diff = epochMs - nowMs
    if (diff <= 0) return "Due now"
    val diffMin = diff / (60 * 1000L)
    val diffHours = diffMin / 60
    val diffDays = diffHours / 24

    return when {
        diffMin < 1 -> "in < 1m"
        diffMin < 60 -> "in ${diffMin}m"
        diffHours < 24 -> {
            val remMin = diffMin % 60
            if (remMin == 0L) "in ${diffHours}h" else "in ${diffHours}h ${remMin}m"
        }
        else -> {
            val remHours = diffHours % 24
            if (remHours == 0L) "in ${diffDays}d" else "in ${diffDays}d ${remHours}h"
        }
    }
}

/**
 * Formats hour (0-23) and minute into standard 12-hour format with AM/PM (e.g. "09:30 AM", "04:15 PM").
 * Single unified implementation used across Logbook, Sectors, and Dialogs.
 */
fun formatHourMinute12(hour: Int, min: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format(Locale.getDefault(), "%02d:%02d %s", h12, min, amPm)
}

/**
 * Formats duration in minutes to compact string (e.g. "1h 45m" or "45m").
 */
fun formatCompactMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Formats duration in milliseconds to compact string.
 */
fun formatDurationMs(durationMs: Long): String {
    return formatCompactMinutes((durationMs / 60000L).toInt())
}

