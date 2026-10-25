package com.example.focusreminder.domain.engine

import com.example.focusreminder.domain.model.UsageInterval

/**
 * Reason an application is currently blocked.
 */
enum class BlockReason {
    NONE,
    ROLLING_BUDGET_EXHAUSTED,
    DAILY_LIMIT_EXHAUSTED,
    BOTH_LIMITS_EXHAUSTED
}

/**
 * Complete status of an application's access restriction.
 */
data class AppRestrictionStatus(
    val isBlocked: Boolean,
    val reason: BlockReason,
    val remainingCooldownMs: Long,
    val isEmergencyOverrideActive: Boolean,
    val remainingOverrideMs: Long
)

/**
 * Manages cooldown rules and unlock conditions (PRD Section 5.3).
 *
 * Enforces:
 * 1. Minimum 30-minute cooldown from the blocking event.
 * 2. Rolling-window availability check.
 * 3. Daily limit midnight reset check.
 * 4. Active emergency override time window.
 */
class CooldownManager(
    val minimumCooldownMs: Long = DEFAULT_MINIMUM_COOLDOWN_MS
) {
    companion object {
        const val DEFAULT_MINIMUM_COOLDOWN_MS: Long = 30 * 60 * 1000L // 30 minutes
    }

    /**
     * Calculates the remaining minimum cooldown duration in milliseconds.
     */
    fun remainingMinimumCooldownMs(
        blockingEventTimeMs: Long,
        currentTimeMs: Long
    ): Long {
        val unlockTimeMs = blockingEventTimeMs + minimumCooldownMs
        return maxOf(0L, unlockTimeMs - currentTimeMs)
    }

    /**
     * Determines whether the minimum cooldown period has expired.
     */
    fun isMinimumCooldownExpired(
        blockingEventTimeMs: Long,
        currentTimeMs: Long
    ): Boolean {
        return remainingMinimumCooldownMs(blockingEventTimeMs, currentTimeMs) == 0L
    }

    /**
     * Evaluates comprehensive restriction status for an app.
     */
    fun evaluateStatus(
        blockingEventTimeMs: Long?,
        intervals: List<UsageInterval>,
        rollingCalculator: RollingWindowUsageCalculator,
        dailyCalculator: DailyUsageCalculator?,
        midnightStartMs: Long,
        currentTimeMs: Long,
        emergencyOverrideExpiryMs: Long?
    ): AppRestrictionStatus {
        // 1. Check if temporary emergency override is active
        if (emergencyOverrideExpiryMs != null && emergencyOverrideExpiryMs > currentTimeMs) {
            val remainingOverride = emergencyOverrideExpiryMs - currentTimeMs
            return AppRestrictionStatus(
                isBlocked = false,
                reason = BlockReason.NONE,
                remainingCooldownMs = 0L,
                isEmergencyOverrideActive = true,
                remainingOverrideMs = remainingOverride
            )
        }

        // 2. Evaluate rolling window
        val rollingResult = rollingCalculator.evaluate(intervals, currentTimeMs)

        // 3. Evaluate daily limit
        val dailyResult = dailyCalculator?.evaluate(intervals, midnightStartMs, currentTimeMs)
        val isDailyExhausted = dailyResult?.isDailyLimitExhausted == true

        // 4. Cooldown time remaining
        val remainingCooldown = if (blockingEventTimeMs != null) {
            remainingMinimumCooldownMs(blockingEventTimeMs, currentTimeMs)
        } else {
            0L
        }

        val hasRollingLock = (blockingEventTimeMs != null && !isMinimumCooldownExpired(blockingEventTimeMs, currentTimeMs)) ||
                rollingResult.isBudgetExhausted

        val isBlocked = hasRollingLock || isDailyExhausted

        val reason = when {
            hasRollingLock && isDailyExhausted -> BlockReason.BOTH_LIMITS_EXHAUSTED
            hasRollingLock -> BlockReason.ROLLING_BUDGET_EXHAUSTED
            isDailyExhausted -> BlockReason.DAILY_LIMIT_EXHAUSTED
            else -> BlockReason.NONE
        }

        return AppRestrictionStatus(
            isBlocked = isBlocked,
            reason = reason,
            remainingCooldownMs = remainingCooldown,
            isEmergencyOverrideActive = false,
            remainingOverrideMs = 0L
        )
    }
}
