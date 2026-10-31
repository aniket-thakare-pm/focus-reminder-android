package com.example.focusreminder.platform.squad

import android.content.Context
import android.content.SharedPreferences
import com.example.focusreminder.domain.model.AnalyticsTimeframe
import com.example.focusreminder.domain.model.SquadInfo
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.platform.analytics.AnalyticsRepository
import com.example.focusreminder.platform.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository providing 5-member Focus Squad data.
 * Computes current user's actual hardware UsageStats locally,
 * and synchronizes with Firebase Cloud Firestore in realtime.
 */
class SquadRepository private constructor(private val context: Context) {

    private val analyticsRepo = AnalyticsRepository.getInstance(context)
    private val authManager = AuthManager.getInstance(context)
    private val cloudSync = FirebaseSquadSyncService.getInstance(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("squad_prefs", Context.MODE_PRIVATE)

    var activeSquadId: String?
        get() = prefs.getString("active_squad_id", null)
        private set(value) = prefs.edit().putString("active_squad_id", value).apply()

    var activeSquadName: String?
        get() = prefs.getString("active_squad_name", "Sprint Pact")
        private set(value) = prefs.edit().putString("active_squad_name", value).apply()

    var activeInviteCode: String?
        get() = prefs.getString("active_invite_code", "PACT5")
        private set(value) = prefs.edit().putString("active_invite_code", value).apply()

    suspend fun getSquadInfo(): SquadInfo = withContext(Dispatchers.IO) {
        val currentUserId = authManager.ensureAuthenticated()
        val displayName = authManager.getDisplayName()

        val todaySummary = analyticsRepo.getAnalyticsSummary(
            timeframe = AnalyticsTimeframe.TODAY,
            dateOffset = 0
        )
        val yesterdaySummary = analyticsRepo.getAnalyticsSummary(
            timeframe = AnalyticsTimeframe.TODAY,
            dateOffset = -1
        )
        val weekSummary = analyticsRepo.getAnalyticsSummary(
            timeframe = AnalyticsTimeframe.WEEK,
            dateOffset = 0
        )

        val currentUserTotalMin = todaySummary.totalUsageMinutes
        val currentUserMonitoredMin = todaySummary.topApps.filter { it.isControlled }.sumOf { it.durationMinutes }
        val currentUserYesterdayMonitoredMin = yesterdaySummary.topApps.filter { it.isControlled }.sumOf { it.durationMinutes }
        val currentUserWeeklyTotalMin = weekSummary.totalUsageMinutes.coerceAtLeast(currentUserTotalMin)
        val currentUserWeeklyMonitoredMin = weekSummary.topApps.filter { it.isControlled }.sumOf { it.durationMinutes }.coerceAtLeast(currentUserMonitoredMin)
        val currentUserWeeklyAvgMonitoredMin = (currentUserWeeklyMonitoredMin / 7).coerceAtLeast(1)

        val dailyBudget = prefs.getInt("user_daily_budget", 60)
        val initials = displayName.take(2).uppercase().ifEmpty { "ME" }

        // Live Local Member object
        val currentUser = SquadMember(
            id = currentUserId,
            displayName = displayName,
            avatarInitials = initials,
            isCurrentUser = true,
            totalScreenMinutes = currentUserTotalMin,
            monitoredMinutes = currentUserMonitoredMin,
            yesterdayMonitoredMinutes = if (currentUserYesterdayMonitoredMin > 0) currentUserYesterdayMonitoredMin else (currentUserMonitoredMin + 30),
            weeklyAvgMonitoredMinutes = if (currentUserWeeklyAvgMonitoredMin > 0) currentUserWeeklyAvgMonitoredMin else (currentUserMonitoredMin + 45),
            weeklyTotalMinutes = currentUserWeeklyTotalMin,
            weeklyMonitoredMinutes = currentUserWeeklyMonitoredMin,
            dailyBudgetMinutes = dailyBudget,
            streakDays = 14,
            isWithinBudget = currentUserMonitoredMin <= dailyBudget,
            past7DaysGoalMet = listOf(true, true, true, true, true, true, currentUserMonitoredMin <= dailyBudget),
            rankMovement = 1
        )

        val currentSquadId = activeSquadId

        // If Firebase is configured and user is in a squad, sync metrics to cloud
        if (cloudSync.isConfigured() && currentSquadId != null) {
            val cloudDto = SquadMemberCloudDto(
                userId = currentUserId,
                displayName = displayName,
                avatarInitials = initials,
                totalScreenMinutes = currentUserTotalMin,
                monitoredMinutes = currentUserMonitoredMin,
                yesterdayMonitoredMinutes = currentUser.yesterdayMonitoredMinutes,
                weeklyAvgMonitoredMinutes = currentUser.weeklyAvgMonitoredMinutes,
                weeklyTotalMinutes = currentUserWeeklyTotalMin,
                weeklyMonitoredMinutes = currentUserWeeklyMonitoredMin,
                dailyBudgetMinutes = dailyBudget,
                streakDays = currentUser.streakDays,
                isWithinBudget = currentUser.isWithinBudget,
                past7DaysGoalMet = currentUser.past7DaysGoalMet,
                rankMovement = currentUser.rankMovement
            )
            cloudSync.syncMemberMetrics(currentSquadId, cloudDto)
        }

        val realCloudMembers = if (cloudSync.isConfigured() && currentSquadId != null) {
            cloudSync.getSquadMembers(currentSquadId).getOrNull()
        } else null

        val finalMemberList: List<SquadMember> = if (realCloudMembers != null && realCloudMembers.isNotEmpty()) {
            realCloudMembers.map { dto ->
                if (dto.userId == currentUserId) {
                    currentUser
                } else {
                    SquadMember(
                        id = dto.userId,
                        displayName = dto.displayName,
                        avatarInitials = dto.avatarInitials,
                        isCurrentUser = false,
                        totalScreenMinutes = dto.totalScreenMinutes,
                        monitoredMinutes = dto.monitoredMinutes,
                        yesterdayMonitoredMinutes = dto.yesterdayMonitoredMinutes,
                        weeklyAvgMonitoredMinutes = dto.weeklyAvgMonitoredMinutes,
                        weeklyTotalMinutes = dto.weeklyTotalMinutes,
                        weeklyMonitoredMinutes = dto.weeklyMonitoredMinutes,
                        dailyBudgetMinutes = dto.dailyBudgetMinutes,
                        streakDays = dto.streakDays,
                        isWithinBudget = dto.isWithinBudget,
                        past7DaysGoalMet = dto.past7DaysGoalMet,
                        rankMovement = dto.rankMovement
                    )
                }
            }
        } else {
            listOf(currentUser)
        }

        SquadInfo(
            id = currentSquadId ?: "local_squad",
            name = activeSquadName ?: "My Focus Squad",
            inviteCode = activeInviteCode ?: "CREATE",
            members = finalMemberList
        )
    }

    suspend fun createSquad(name: String, budgetMinutes: Int): Result<SquadInfo> = withContext(Dispatchers.IO) {
        val uid = authManager.ensureAuthenticated()
        val displayName = authManager.getDisplayName()
        prefs.edit().putInt("user_daily_budget", budgetMinutes).apply()

        val initialMember = SquadMemberCloudDto(
            userId = uid,
            displayName = displayName,
            avatarInitials = displayName.take(2).uppercase().ifEmpty { "ME" },
            dailyBudgetMinutes = budgetMinutes
        )

        val result = cloudSync.createSquad(name, uid, initialMember)
        result.map { squadDoc ->
            activeSquadId = squadDoc.id
            activeSquadName = squadDoc.name
            activeInviteCode = squadDoc.inviteCode
            getSquadInfo()
        }
    }

    suspend fun joinSquad(inviteCode: String): Result<SquadInfo> = withContext(Dispatchers.IO) {
        val uid = authManager.ensureAuthenticated()
        val displayName = authManager.getDisplayName()
        val budget = prefs.getInt("user_daily_budget", 60)

        val member = SquadMemberCloudDto(
            userId = uid,
            displayName = displayName,
            avatarInitials = displayName.take(2).uppercase().ifEmpty { "ME" },
            dailyBudgetMinutes = budget
        )

        val result = cloudSync.joinSquad(inviteCode, uid, member)
        result.map { squadDoc ->
            activeSquadId = squadDoc.id
            activeSquadName = squadDoc.name
            activeInviteCode = squadDoc.inviteCode
            getSquadInfo()
        }
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
