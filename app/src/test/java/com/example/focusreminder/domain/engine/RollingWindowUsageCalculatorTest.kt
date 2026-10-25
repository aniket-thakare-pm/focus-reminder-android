package com.example.focusreminder.domain.engine

import com.example.focusreminder.domain.model.UsageInterval
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the RollingWindowUsageCalculator (PRD Section 5.2).
 */
class RollingWindowUsageCalculatorTest {

    private lateinit var calculator: RollingWindowUsageCalculator

    // Helper: minutes to milliseconds
    private fun minutes(m: Long) = m * 60 * 1000L

    @Before
    fun setUp() {
        calculator = RollingWindowUsageCalculator(
            budgetMs = minutes(30),
            rollingWindowMs = minutes(60),
            warningLeadTimeMs = minutes(5),
            sessionGracePeriodMs = minutes(5)
        )
    }

    @Test
    fun `single session within budget does not trigger warning or block`() {
        val t0 = 100_000_000L
        val intervals = listOf(
            UsageInterval(t0, t0 + minutes(15)) // 15 mins usage
        )

        val result = calculator.evaluate(intervals, t0 + minutes(15))

        assertEquals(minutes(15), result.accumulatedUsageMs)
        assertEquals(minutes(15), result.remainingUsageMs)
        assertFalse(result.isWarningThresholdReached)
        assertFalse(result.isBudgetExhausted)
    }

    @Test
    fun `reaches warning threshold at 25 minutes of usage`() {
        val t0 = 100_000_000L
        val intervals = listOf(
            UsageInterval(t0, t0 + minutes(25)) // 25 mins usage
        )

        val result = calculator.evaluate(intervals, t0 + minutes(25))

        assertEquals(minutes(25), result.accumulatedUsageMs)
        assertEquals(minutes(5), result.remainingUsageMs)
        assertTrue(result.isWarningThresholdReached)
        assertFalse(result.isBudgetExhausted)
    }

    @Test
    fun `blocks when budget of 30 minutes is exhausted`() {
        val t0 = 100_000_000L
        val intervals = listOf(
            UsageInterval(t0, t0 + minutes(30)) // 30 mins usage
        )

        val result = calculator.evaluate(intervals, t0 + minutes(30))

        assertEquals(minutes(30), result.accumulatedUsageMs)
        assertEquals(0L, result.remainingUsageMs)
        assertTrue(result.isBudgetExhausted)
    }

    @Test
    fun `preserves usage across multiple sessions with 5-minute break`() {
        val t0 = 100_000_000L
        val session1 = UsageInterval(t0, t0 + minutes(10)) // 10 mins
        // 4 minute break (within grace period)
        val session2 = UsageInterval(t0 + minutes(14), t0 + minutes(24)) // 10 mins

        assertTrue("Break <= 5 min should be considered same session", calculator.isSameSession(session1, session2))

        val intervals = listOf(session1, session2)
        val result = calculator.evaluate(intervals, t0 + minutes(24))

        assertEquals(minutes(20), result.accumulatedUsageMs)
        assertFalse(result.isBudgetExhausted)
    }

    @Test
    fun `break longer than 5 minutes creates new session without resetting rolling usage`() {
        val t0 = 100_000_000L
        val session1 = UsageInterval(t0, t0 + minutes(15)) // 15 mins
        // 10 minute break (exceeds 5 min grace period)
        val session2 = UsageInterval(t0 + minutes(25), t0 + minutes(40)) // 15 mins

        assertFalse("Break > 5 min should be a new session", calculator.isSameSession(session1, session2))

        val intervals = listOf(session1, session2)
        val result = calculator.evaluate(intervals, t0 + minutes(40))

        // Total usage in window = 15 + 15 = 30 mins -> budget exhausted!
        assertEquals(minutes(30), result.accumulatedUsageMs)
        assertTrue(result.isBudgetExhausted)
    }

    @Test
    fun `usage older than 60 minutes ages out of the rolling window`() {
        val t0 = 100_000_000L
        // Session 1: 20 minutes usage, occurred from t0 to t0+20
        val session1 = UsageInterval(t0, t0 + minutes(20))
        // Session 2: 15 minutes usage, occurred from t0+50 to t0+65
        val session2 = UsageInterval(t0 + minutes(50), t0 + minutes(65))

        val intervals = listOf(session1, session2)

        // Evaluate at t0 + 75 minutes:
        // Rolling window is [t0 + 15m, t0 + 75m].
        // Overlap with session 1 [t0, t0+20] is [t0+15, t0+20] = 5 minutes.
        // Overlap with session 2 [t0+50, t0+65] is full 15 minutes.
        // Total expected usage = 5 + 15 = 20 minutes (not 35!).
        val result = calculator.evaluate(intervals, t0 + minutes(75))

        assertEquals(minutes(20), result.accumulatedUsageMs)
        assertEquals(minutes(10), result.remainingUsageMs)
        assertFalse(result.isBudgetExhausted)
    }
}
