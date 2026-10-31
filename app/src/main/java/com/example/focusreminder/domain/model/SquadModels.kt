package com.example.focusreminder.domain.model

/**
 * Represents a member of the 5-person Focus Squad.
 */
data class SquadMember(
    val id: String,
    val displayName: String,
    val avatarInitials: String,
    val isCurrentUser: Boolean = false,
    val totalScreenMinutes: Int,
    val monitoredMinutes: Int,
    val yesterdayMonitoredMinutes: Int,
    val weeklyAvgMonitoredMinutes: Int = yesterdayMonitoredMinutes,
    val weeklyTotalMinutes: Int = totalScreenMinutes * 6,
    val weeklyMonitoredMinutes: Int = monitoredMinutes * 6,
    val dailyBudgetMinutes: Int = 60,
    val streakDays: Int = 1,
    val isWithinBudget: Boolean = true,
    val past7DaysGoalMet: List<Boolean> = listOf(true, true, true, true, true, true, true),
    val rankMovement: Int = 0 // > 0 moved up, < 0 moved down, 0 steady
) {
    /**
     * Self-Improvement Delta in minutes.
     * Positive = improved (reduced distraction vs baseline).
     * Negative = increased distraction vs baseline.
     */
    fun getDeltaMinutes(isWeeklyAvg: Boolean): Int {
        val baseline = if (isWeeklyAvg) weeklyAvgMonitoredMinutes else yesterdayMonitoredMinutes
        return baseline - monitoredMinutes
    }

    fun getBaselineMinutes(isWeeklyAvg: Boolean): Int {
        return if (isWeeklyAvg) weeklyAvgMonitoredMinutes else yesterdayMonitoredMinutes
    }

    val deltaMinutes: Int
        get() = yesterdayMonitoredMinutes - monitoredMinutes

    val deltaPercentage: Int
        get() = if (yesterdayMonitoredMinutes > 0) {
            ((deltaMinutes.toFloat() / yesterdayMonitoredMinutes.toFloat()) * 100).toInt()
        } else if (monitoredMinutes == 0) {
            0
        } else {
            -100
        }
}

/**
 * Container for squad details and up to 5 members.
 */
data class SquadInfo(
    val id: String,
    val name: String,
    val inviteCode: String,
    val members: List<SquadMember>
)
