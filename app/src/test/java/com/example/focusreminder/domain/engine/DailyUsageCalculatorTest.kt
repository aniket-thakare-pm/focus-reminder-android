package com.example.focusreminder.domain.engine

import com.example.focusreminder.domain.model.UsageInterval
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DailyUsageCalculator (ADR-008 & PRD Section 5.2.1).
 */
class DailyUsageCalculatorTest {

    private lateinit var calculator: DailyUsageCalculator

    private fun minutes(m: Long) = m * 60 * 1000L
    private fun hours(h: Long) = h * 60 * 60 * 1000L

    @Before
    fun setUp() {
        calculator = DailyUsageCalculator(dailyBudgetMs = hours(2)) // 2 hour daily budget
    }

    @Test
    fun `accumulates multiple sessions across the day until 2-hour limit is reached`() {
        val midnight = 1_000_000_000L

        val intervals = listOf(
            UsageInterval(midnight + hours(2), midnight + hours(2) + minutes(30)), // 30m at 2am
            UsageInterval(midnight + hours(7), midnight + hours(7) + minutes(45)), // 45m at 7am
            UsageInterval(midnight + hours(12), midnight + hours(12) + minutes(45)) // 45m at 12pm
        )
        // Total = 30 + 45 + 45 = 120 minutes (2 hours)

        val result = calculator.evaluate(intervals, midnight, midnight + hours(13))

        assertEquals(hours(2), result.dailyAccumulatedUsageMs)
        assertEquals(0L, result.remainingDailyUsageMs)
        assertTrue(result.isDailyLimitExhausted)
    }

    @Test
    fun `ignores usage from before midnight`() {
        val midnight = 1_000_000_000L

        val intervals = listOf(
            UsageInterval(midnight - hours(1), midnight - minutes(15)), // Yesterday: 45 min
            UsageInterval(midnight + hours(1), midnight + hours(1) + minutes(30)) // Today: 30 min
        )

        val result = calculator.evaluate(intervals, midnight, midnight + hours(2))

        assertEquals(minutes(30), result.dailyAccumulatedUsageMs)
        assertEquals(hours(2) - minutes(30), result.remainingDailyUsageMs)
        assertFalse(result.isDailyLimitExhausted)
    }
}
