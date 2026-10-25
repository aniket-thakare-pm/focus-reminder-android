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
