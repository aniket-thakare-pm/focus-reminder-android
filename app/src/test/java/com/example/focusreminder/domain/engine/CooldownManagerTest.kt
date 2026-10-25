package com.example.focusreminder.domain.engine

import com.example.focusreminder.domain.model.UsageInterval
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CooldownManager (PRD Section 5.3 & ADR-005).
 */
class CooldownManagerTest {

    private lateinit var cooldownManager: CooldownManager
    private lateinit var rollingCalculator: RollingWindowUsageCalculator
    private lateinit var dailyCalculator: DailyUsageCalculator

    private fun minutes(m: Long) = m * 60 * 1000L

    @Before
    fun setUp() {
        cooldownManager = CooldownManager(minimumCooldownMs = minutes(30))
        rollingCalculator = RollingWindowUsageCalculator(budgetMs = minutes(30))
        dailyCalculator = DailyUsageCalculator(dailyBudgetMs = minutes(120)) // 2 hrs daily
    }

    @Test
    fun `remaining cooldown decreases over time and expires at 30 minutes`() {
        val blockTime = 100_000_000L

        // At 10 minutes after block: 20 minutes remaining
        assertEquals(minutes(20), cooldownManager.remainingMinimumCooldownMs(blockTime, blockTime + minutes(10)))
        assertFalse(cooldownManager.isMinimumCooldownExpired(blockTime, blockTime + minutes(10)))

        // At 30 minutes after block: 0 minutes remaining
        assertEquals(0L, cooldownManager.remainingMinimumCooldownMs(blockTime, blockTime + minutes(30)))
        assertTrue(cooldownManager.isMinimumCooldownExpired(blockTime, blockTime + minutes(30)))
    }

    @Test
    fun `cannot unlock even if 30m expired if rolling budget is still exhausted`() {
        val t0 = 100_000_000L
        // Suppose user spent 40 minutes in last 50 minutes (heavy usage)
        val intervals = listOf(
            UsageInterval(t0, t0 + minutes(40))
        )
        val blockTime = t0 + minutes(40)

        // Evaluate at blockTime + 30m = t0 + 70m.
        // Minimum cooldown of 30m has expired, but rolling window [t0+10m, t0+70m]
        // still overlaps [t0+10m, t0+40m] = 30 minutes = budget exhausted!
        val status = cooldownManager.evaluateStatus(
            blockingEventTimeMs = blockTime,
            intervals = intervals,
            rollingCalculator = rollingCalculator,
            dailyCalculator = dailyCalculator,
            midnightStartMs = t0 - minutes(60),
            currentTimeMs = blockTime + minutes(30),
            emergencyOverrideExpiryMs = null
        )

        assertTrue("Should still be blocked because rolling window condition is not satisfied", status.isBlocked)
        assertEquals(BlockReason.ROLLING_BUDGET_EXHAUSTED, status.reason)
    }

    @Test
    fun `emergency override temporarily bypasses active block`() {
        val t0 = 100_000_000L
        val intervals = listOf(UsageInterval(t0, t0 + minutes(30)))
        val blockTime = t0 + minutes(30)

        val overrideExpiry = blockTime + minutes(5) // User granted 5 minutes

        val status = cooldownManager.evaluateStatus(
            blockingEventTimeMs = blockTime,
            intervals = intervals,
            rollingCalculator = rollingCalculator,
            dailyCalculator = dailyCalculator,
            midnightStartMs = t0,
            currentTimeMs = blockTime + minutes(2), // 2 minutes into override
            emergencyOverrideExpiryMs = overrideExpiry
        )

        assertFalse("Should be unblocked during active override window", status.isBlocked)
        assertTrue(status.isEmergencyOverrideActive)
        assertEquals(minutes(3), status.remainingOverrideMs)
    }

    @Test
    fun `re-locks immediately when emergency override window expires`() {
        val t0 = 100_000_000L
        val intervals = listOf(UsageInterval(t0, t0 + minutes(30)))
        val blockTime = t0 + minutes(30)

        val overrideExpiry = blockTime + minutes(5)

        val status = cooldownManager.evaluateStatus(
            blockingEventTimeMs = blockTime,
            intervals = intervals,
            rollingCalculator = rollingCalculator,
            dailyCalculator = dailyCalculator,
            midnightStartMs = t0,
            currentTimeMs = blockTime + minutes(6), // 1 minute after override expired
            emergencyOverrideExpiryMs = overrideExpiry
        )

        assertTrue("Should be re-blocked once override expires", status.isBlocked)
        assertFalse(status.isEmergencyOverrideActive)
    }
}
