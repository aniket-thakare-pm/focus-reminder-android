package com.example.focusreminder.util

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
