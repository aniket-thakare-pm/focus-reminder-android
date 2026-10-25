package com.example.focusreminder.util

/**
 * Formats a duration in minutes into strict hours and minutes format (e.g. "1h 44m", "0h 30m", "0h 00m").
 */
fun formatHoursMinutes(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return "${h}h ${String.format("%02d", m)}m"
}
