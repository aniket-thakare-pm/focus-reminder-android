package com.example.focusreminder.domain.engine

import com.example.focusreminder.domain.model.UsageInterval

/**
 * Result of evaluating daily accumulated usage against a daily budget limit.
 */
data class DailyEvaluationResult(
    val dailyAccumulatedUsageMs: Long,
    val dailyBudgetMs: Long,
    val remainingDailyUsageMs: Long,
    val isDailyLimitExhausted: Boolean
)

/**
 * Pure Kotlin calculator for the Daily Total App Limit (ADR-008 / PRD Section 5.2.1).
 *
 * Tracks accumulated usage from midnight (00:00:00) to midnight in local device time.
 */
class DailyUsageCalculator(
    val dailyBudgetMs: Long
) {
    /**
     * Calculates total usage accumulated between [midnightStartMs] and [evaluationTimeMs].
     */
    fun calculateDailyUsage(
        intervals: List<UsageInterval>,
        midnightStartMs: Long,
        evaluationTimeMs: Long
    ): Long {
        var totalMs = 0L
        for (interval in intervals) {
            totalMs += interval.overlapMs(midnightStartMs, evaluationTimeMs)
        }
        return totalMs
    }

    /**
     * Evaluates daily usage status.
     */
    fun evaluate(
        intervals: List<UsageInterval>,
        midnightStartMs: Long,
        evaluationTimeMs: Long
    ): DailyEvaluationResult {
        val dailyUsageMs = calculateDailyUsage(intervals, midnightStartMs, evaluationTimeMs)
        val remainingMs = maxOf(0L, dailyBudgetMs - dailyUsageMs)

        return DailyEvaluationResult(
            dailyAccumulatedUsageMs = dailyUsageMs,
            dailyBudgetMs = dailyBudgetMs,
            remainingDailyUsageMs = remainingMs,
            isDailyLimitExhausted = dailyUsageMs >= dailyBudgetMs
        )
    }
}
