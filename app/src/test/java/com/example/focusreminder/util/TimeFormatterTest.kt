package com.example.focusreminder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeFormatterTest {

    @Test
    fun `formats zero minutes as 0h 00m`() {
        assertEquals("0h 00m", formatHoursMinutes(0))
    }

    @Test
    fun `formats under an hour correctly`() {
        assertEquals("0h 45m", formatHoursMinutes(45))
        assertEquals("0h 05m", formatHoursMinutes(5))
    }

    @Test
    fun `formats exact hours correctly`() {
        assertEquals("1h 00m", formatHoursMinutes(60))
        assertEquals("2h 00m", formatHoursMinutes(120))
    }

    @Test
    fun `formats hours and minutes correctly`() {
        // User's specific Instagram usage example: 1 hour 44 minutes = 104 minutes
        assertEquals("1h 44m", formatHoursMinutes(104))
        assertEquals("1h 30m", formatHoursMinutes(90))
        assertEquals("4h 31m", formatHoursMinutes(271))
    }

    @Test
    fun `formats relative countdown correctly`() {
        val now = 10_000_000L
        assertEquals("Due now", formatRelativeCountdown(now - 1000L, now))
        assertEquals("Due now", formatRelativeCountdown(now, now))
        assertEquals("in < 1m", formatRelativeCountdown(now + 25_000L, now))
        assertEquals("in 15m", formatRelativeCountdown(now + 15 * 60 * 1000L, now))
        assertEquals("in 1h 45m", formatRelativeCountdown(now + 105 * 60 * 1000L, now))
        assertEquals("in 2h", formatRelativeCountdown(now + 120 * 60 * 1000L, now))
        assertEquals("in 1d", formatRelativeCountdown(now + 24 * 60 * 60 * 1000L, now))
        assertEquals("in 1d 4h", formatRelativeCountdown(now + 28 * 60 * 60 * 1000L, now))
    }

    @Test
    fun `formats reminder date time for today and tomorrow`() {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        val todayFormatted = formatReminderDateTime(todayCal.timeInMillis)
        assertTrue("Expected 'Today,' but got '$todayFormatted'", todayFormatted.startsWith("Today,"))

        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 15)
        }
        val tomorrowFormatted = formatReminderDateTime(tomorrowCal.timeInMillis)
        assertTrue("Expected 'Tomorrow,' but got '$tomorrowFormatted'", tomorrowFormatted.startsWith("Tomorrow,"))
    }
}
