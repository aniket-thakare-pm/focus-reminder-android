package com.example.focusreminder.platform.squad

import com.example.focusreminder.domain.model.SquadMember

/**
 * Cloud Firestore document representation of a Focus Squad room.
 * Stored at: squads/{squadId}
 */
data class SquadCloudDto(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "",
    val createdById: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val memberIds: List<String> = emptyList()
)

/**
 * Cloud Firestore document representation of a squad member's daily stats.
 * Stored at: squads/{squadId}/members/{userId}
 *
 * PRIVACY GUARANTEE:
 * Only aggregated comparison minutes are stored here.
 * Zero package names, app titles, or private logs leave the device.
 */
data class SquadMemberCloudDto(
    val userId: String = "",
    val displayName: String = "Anonymous",
    val avatarInitials: String = "US",
    val totalScreenMinutes: Int = 0,
    val monitoredMinutes: Int = 0,
    val yesterdayMonitoredMinutes: Int = 0,
    val weeklyAvgMonitoredMinutes: Int = 0,
    val weeklyTotalMinutes: Int = 0,
    val weeklyMonitoredMinutes: Int = 0,
    val dailyBudgetMinutes: Int = 60,
    val streakDays: Int = 0,
    val isWithinBudget: Boolean = true,
    val past7DaysGoalMet: List<Boolean> = listOf(true, true, true, true, true, true, true),
    val rankMovement: Int = 0,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun toDomainMember(currentUserId: String): SquadMember {
        return SquadMember(
            id = userId,
            displayName = displayName,
            avatarInitials = avatarInitials,
            isCurrentUser = (userId == currentUserId),
            totalScreenMinutes = totalScreenMinutes,
            monitoredMinutes = monitoredMinutes,
            yesterdayMonitoredMinutes = yesterdayMonitoredMinutes,
            weeklyAvgMonitoredMinutes = weeklyAvgMonitoredMinutes,
            weeklyTotalMinutes = weeklyTotalMinutes,
            weeklyMonitoredMinutes = weeklyMonitoredMinutes,
            dailyBudgetMinutes = dailyBudgetMinutes,
            streakDays = streakDays,
            isWithinBudget = isWithinBudget,
            past7DaysGoalMet = past7DaysGoalMet,
            rankMovement = rankMovement
        )
    }
}
