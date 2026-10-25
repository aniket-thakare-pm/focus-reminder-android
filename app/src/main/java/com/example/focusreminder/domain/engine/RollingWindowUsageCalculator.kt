package com.example.focusreminder.domain.engine

import com.example.focusreminder.domain.model.UsageInterval

/**
 * Result of evaluating usage against the rolling-window budget.
 */
data class RollingEvaluationResult(
    val accumulatedUsageMs: Long,
    val budgetMs: Long,
    val remainingUsageMs: Long,
    val isWarningThresholdReached: Boolean,
    val isBudgetExhausted: Boolean
)

/**
 * Pure Kotlin rolling-window usage budget calculator.
 *
 * Implements PRD Section 5.2:
 * Evaluates foreground usage over a trailing 60-minute window and enforces
 * the configured usage budget (default 30 minutes).
 */
class RollingWindowUsageCalculator(
    val budgetMs: Long = DEFAULT_BUDGET_MS,
    val rollingWindowMs: Long = DEFAULT_WINDOW_MS,
    val warningLeadTimeMs: Long = DEFAULT_WARNING_LEAD_TIME_MS,
    val sessionGracePeriodMs: Long = DEFAULT_SESSION_GRACE_PERIOD_MS
) {
    companion object {
        const val DEFAULT_BUDGET_MS: Long = 30 * 60 * 1000L // 30 minutes
        const val DEFAULT_WINDOW_MS: Long = 60 * 60 * 1000L // 60 minutes
        const val DEFAULT_WARNING_LEAD_TIME_MS: Long = 5 * 60 * 1000L // 5 minutes before exhaustion
        const val DEFAULT_SESSION_GRACE_PERIOD_MS: Long = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Calculates the accumulated usage in the rolling window [evaluationTimeMs - rollingWindowMs, evaluationTimeMs].
     *
     * @param intervals All recorded intervals for this app.
     * @param evaluationTimeMs The timestamp (epoch ms) at which the evaluation is being performed.
     */
    fun calculateUsageInWindow(
        intervals: List<UsageInterval>,
        evaluationTimeMs: Long
    ): Long {
        val windowStartMs = evaluationTimeMs - rollingWindowMs
        var totalUsageMs = 0L

        for (interval in intervals) {
            totalUsageMs += interval.overlapMs(windowStartMs, evaluationTimeMs)
        }

        return totalUsageMs
    }

    /**
     * Determines whether two consecutive usage intervals belong to the same session
     * based on the 5-minute session grace period rule.
     *
     * @param previousInterval The earlier interval.
     * @param nextInterval The later interval.
     * @return True if the gap between them is <= sessionGracePeriodMs (5 minutes).
     */
    fun isSameSession(
        previousInterval: UsageInterval,
        nextInterval: UsageInterval
    ): Boolean {
        val gapMs = nextInterval.startTimeMs - previousInterval.endTimeMs
        return gapMs in 0..sessionGracePeriodMs
    }

    /**
     * Evaluates the current state for a restricted app.
     */
    fun evaluate(
        intervals: List<UsageInterval>,
        evaluationTimeMs: Long
    ): RollingEvaluationResult {
        val usageMs = calculateUsageInWindow(intervals, evaluationTimeMs)
        val remainingMs = maxOf(0L, budgetMs - usageMs)
        val warningThresholdMs = maxOf(0L, budgetMs - warningLeadTimeMs)

        return RollingEvaluationResult(
            accumulatedUsageMs = usageMs,
            budgetMs = budgetMs,
            remainingUsageMs = remainingMs,
            isWarningThresholdReached = usageMs >= warningThresholdMs && usageMs < budgetMs,
            isBudgetExhausted = usageMs >= budgetMs
        )
    }
}
