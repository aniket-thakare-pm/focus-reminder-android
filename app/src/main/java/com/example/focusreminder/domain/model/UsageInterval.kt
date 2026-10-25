package com.example.focusreminder.domain.model

/**
 * Represents a discrete recorded interval of foreground application usage.
 *
 * @property startTimeMs Epoch millisecond when the app entered the foreground.
 * @property endTimeMs Epoch millisecond when the app left the foreground or current evaluation timestamp.
 */
data class UsageInterval(
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    init {
        require(endTimeMs >= startTimeMs) {
            "endTimeMs ($endTimeMs) cannot be before startTimeMs ($startTimeMs)"
        }
    }

    /**
     * Duration in milliseconds of this usage interval.
     */
    val durationMs: Long
        get() = endTimeMs - startTimeMs

    /**
     * Calculates the overlap in milliseconds of this interval with an arbitrary window [windowStartMs, windowEndMs].
     */
    fun overlapMs(windowStartMs: Long, windowEndMs: Long): Long {
        if (windowEndMs <= windowStartMs) return 0L
        val effectiveStart = maxOf(startTimeMs, windowStartMs)
        val effectiveEnd = minOf(endTimeMs, windowEndMs)
        return if (effectiveEnd > effectiveStart) effectiveEnd - effectiveStart else 0L
    }
}
