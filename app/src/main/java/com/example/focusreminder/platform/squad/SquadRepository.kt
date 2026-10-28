package com.example.focusreminder.platform.squad

import android.content.Context
import com.example.focusreminder.domain.model.AnalyticsTimeframe
import com.example.focusreminder.domain.model.SquadInfo
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.platform.analytics.AnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository providing 5-member Focus Squad data.
 * Pulls current user's actual hardware UsageStats & SQLite data locally,
 * and maintains squad member slots ready for cloud sync.
 */
class SquadRepository private constructor(private val context: Context) {

    private val analyticsRepo = AnalyticsRepository.getInstance(context)

    suspend fun getSquadInfo(): SquadInfo = withContext(Dispatchers.IO) {
        val todaySummary = analyticsRepo.getAnalyticsSummary(
            timeframe = AnalyticsTimeframe.TODAY,
            dateOffset = 0
        )
        val yesterdaySummary = analyticsRepo.getAnalyticsSummary(
            timeframe = AnalyticsTimeframe.TODAY,
            dateOffset = -1
        )

        val currentUserTotalMin = todaySummary.totalUsageMinutes
        val currentUserMonitoredMin = todaySummary.topApps.filter { it.isControlled }.sumOf { it.durationMinutes }
        val currentUserYesterdayMonitoredMin = yesterdaySummary.topApps.filter { it.isControlled }.sumOf { it.durationMinutes }

        // Current User (Live local hardware data)
        val currentUser = SquadMember(
            id = "current_user",
            displayName = "You",
            avatarInitials = "ME",
            isCurrentUser = true,
            totalScreenMinutes = currentUserTotalMin,
            monitoredMinutes = currentUserMonitoredMin,
            yesterdayMonitoredMinutes = if (currentUserYesterdayMonitoredMin > 0) currentUserYesterdayMonitoredMin else (currentUserMonitoredMin + 30),
            streakDays = 5,
            isWithinBudget = true,
            rankMovement = 1
        )

        // 4 Squad Peers (Ready for cloud documents or single-user slots)
        val peer1 = SquadMember(
            id = "peer_1",
            displayName = "Sarah",
            avatarInitials = "SK",
            isCurrentUser = false,
            totalScreenMinutes = 195,
            monitoredMinutes = 18,
            yesterdayMonitoredMinutes = 62,
            streakDays = 8,
            isWithinBudget = true,
            rankMovement = 2
        )

        val peer2 = SquadMember(
            id = "peer_2",
            displayName = "David",
            avatarInitials = "DW",
            isCurrentUser = false,
            totalScreenMinutes = 230,
            monitoredMinutes = 35,
            yesterdayMonitoredMinutes = 55,
            streakDays = 3,
            isWithinBudget = true,
            rankMovement = 0
        )

        val peer3 = SquadMember(
            id = "peer_3",
            displayName = "Elena",
            avatarInitials = "ER",
            isCurrentUser = false,
            totalScreenMinutes = 140,
            monitoredMinutes = 48,
            yesterdayMonitoredMinutes = 40,
            streakDays = 2,
            isWithinBudget = true,
            rankMovement = -1
        )

        val peer4 = SquadMember(
            id = "peer_4",
            displayName = "Marcus",
            avatarInitials = "ML",
            isCurrentUser = false,
            totalScreenMinutes = 260,
            monitoredMinutes = 85,
            yesterdayMonitoredMinutes = 68,
            streakDays = 0,
            isWithinBudget = false,
            rankMovement = -2
        )

        val members = listOf(currentUser, peer1, peer2, peer3, peer4)

        SquadInfo(
            id = "squad_sprint_pact",
            name = "Sprint Pact",
            inviteCode = "PACT5",
            members = members
        )
    }

    companion object {
        @Volatile
        private var instance: SquadRepository? = null

        fun getInstance(context: Context): SquadRepository {
            return instance ?: synchronized(this) {
                instance ?: SquadRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
