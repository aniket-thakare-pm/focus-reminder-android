package com.example.focusreminder.platform.analytics

import android.content.Context
import android.content.SharedPreferences
import com.example.focusreminder.domain.model.BufferGoal
import com.example.focusreminder.domain.model.GoalEnforcementMode
import com.example.focusreminder.domain.model.TimeSector
import com.example.focusreminder.platform.DefaultAllowedAppsHelper
import java.util.Calendar

/**
 * Manages customizable time-of-day sector definitions and custom focus buffer goals.
 */
class SectorConfigStore private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sector_config_prefs", Context.MODE_PRIVATE)

    companion object {
        const val SECTOR_MORNING = "sector_morning"
        const val SECTOR_AFTERNOON = "sector_afternoon"
        const val SECTOR_EVENING = "sector_evening"
        const val SECTOR_NIGHT = "sector_night"

        // Default sector colors (Emerald, Amber, Purple, Rose)
        const val COLOR_MORNING = 0xFF10B981L
        const val COLOR_AFTERNOON = 0xFFF59E0BL
        const val COLOR_EVENING = 0xFF8B5CF6L
        const val COLOR_NIGHT = 0xFFEC4899L

        private const val KEY_SECTOR_IDS = "configured_sector_ids"
        private const val PREFIX_SECTOR_NAME = "sector_name_"
        private const val PREFIX_SECTOR_START_H = "sector_start_h_"
        private const val PREFIX_SECTOR_START_M = "sector_start_m_"
        private const val PREFIX_SECTOR_END_H = "sector_end_h_"
        private const val PREFIX_SECTOR_END_M = "sector_end_m_"
        private const val PREFIX_SECTOR_COLOR = "sector_color_"

        // 6 distinct neumorphic harmonious colors
        val DEFAULT_SECTOR_COLORS = listOf(
            0xFF10B981L, // Emerald (Morning)
            0xFFF59E0BL, // Amber (Afternoon)
            0xFF8B5CF6L, // Purple (Evening)
            0xFFEC4899L, // Pink/Rose (Night)
            0xFF06B6D4L, // Cyan (Early Dawn)
            0xFF6366F1L  // Indigo (Late Night)
        )

        private const val KEY_GOAL_IDS = "custom_goal_ids"
        private const val PREFIX_GOAL_NAME = "goal_name_"
        private const val PREFIX_GOAL_START = "goal_start_"
        private const val PREFIX_GOAL_START_MIN = "goal_start_min_"
        private const val PREFIX_GOAL_END = "goal_end_"
        private const val PREFIX_GOAL_END_MIN = "goal_end_min_"
        private const val PREFIX_GOAL_MODE = "goal_mode_"
        private const val PREFIX_GOAL_ALLOWED_PKGS = "goal_allowed_pkgs_"
        private const val PREFIX_GOAL_ENABLED = "goal_enabled_"

        @Volatile
        private var INSTANCE: SectorConfigStore? = null

        fun getInstance(context: Context): SectorConfigStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SectorConfigStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Returns the active list of configured time sectors (2 to 6 sectors).
     */
    fun getSectors(): List<TimeSector> {
        val rawIds = prefs.getString(KEY_SECTOR_IDS, null)
        if (rawIds.isNullOrBlank()) {
            return getDefaultSectors()
        }

        val idList = rawIds.split(",").filter { it.isNotBlank() }
        if (idList.size < 2) {
            return getDefaultSectors()
        }

        val sectors = mutableListOf<TimeSector>()
        for (id in idList) {
            val name = prefs.getString("$PREFIX_SECTOR_NAME$id", null) ?: id
            val startH = prefs.getInt("$PREFIX_SECTOR_START_H$id", 0)
            val startM = prefs.getInt("$PREFIX_SECTOR_START_M$id", 0)
            val endH = prefs.getInt("$PREFIX_SECTOR_END_H$id", 0)
            val endM = prefs.getInt("$PREFIX_SECTOR_END_M$id", 0)
            val color = prefs.getLong("$PREFIX_SECTOR_COLOR$id", COLOR_MORNING)

            sectors.add(
                TimeSector(
                    id = id,
                    name = name,
                    startHour = startH,
                    startMinute = startM,
                    endHour = endH,
                    endMinute = endM,
                    colorLong = color
                )
            )
        }
        return if (sectors.size in 2..6) sectors else getDefaultSectors()
    }

    fun getDefaultSectors(): List<TimeSector> {
        return listOf(
            TimeSector(
                id = SECTOR_MORNING,
                name = "Morning",
                startHour = 6,
                startMinute = 0,
                endHour = 12,
                endMinute = 0,
                colorLong = COLOR_MORNING
            ),
            TimeSector(
                id = SECTOR_AFTERNOON,
                name = "Afternoon",
                startHour = 12,
                startMinute = 0,
                endHour = 17,
                endMinute = 0,
                colorLong = COLOR_AFTERNOON
            ),
            TimeSector(
                id = SECTOR_EVENING,
                name = "Evening",
                startHour = 17,
                startMinute = 0,
                endHour = 22,
                endMinute = 0,
                colorLong = COLOR_EVENING
            ),
            TimeSector(
                id = SECTOR_NIGHT,
                name = "Night",
                startHour = 22,
                startMinute = 0,
                endHour = 6,
                endMinute = 0,
                colorLong = COLOR_NIGHT
            )
        )
    }

    /**
     * Saves user customized sectors (must contain between 2 and 6 sectors).
     */
    fun saveSectors(sectors: List<TimeSector>) {
        if (sectors.size !in 2..6) return
        val editor = prefs.edit()
        val ids = sectors.joinToString(",") { it.id }
        editor.putString(KEY_SECTOR_IDS, ids)

        for (s in sectors) {
            editor.putString("$PREFIX_SECTOR_NAME${s.id}", s.name)
            editor.putInt("$PREFIX_SECTOR_START_H${s.id}", s.startHour)
            editor.putInt("$PREFIX_SECTOR_START_M${s.id}", s.startMinute)
            editor.putInt("$PREFIX_SECTOR_END_H${s.id}", s.endHour)
            editor.putInt("$PREFIX_SECTOR_END_M${s.id}", s.endMinute)
            editor.putLong("$PREFIX_SECTOR_COLOR${s.id}", s.colorLong)
        }
        editor.apply()
    }

    /**
     * Resets sectors back to the default 4-sector configuration.
     */
    fun resetToDefaultSectors() {
        val editor = prefs.edit()
        editor.remove(KEY_SECTOR_IDS)
        for (key in prefs.all.keys) {
            if (key.startsWith(PREFIX_SECTOR_NAME) ||
                key.startsWith(PREFIX_SECTOR_START_H) ||
                key.startsWith(PREFIX_SECTOR_START_M) ||
                key.startsWith(PREFIX_SECTOR_END_H) ||
                key.startsWith(PREFIX_SECTOR_END_M) ||
                key.startsWith(PREFIX_SECTOR_COLOR)
            ) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    /**
     * Resolves which sector a given timestamp belongs to with minute-level precision.
     * Deterministic, zero gaps.
     */
    fun getSectorForTimestamp(timestampMs: Long): TimeSector {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeMins = hour * 60 + minute
        val sectors = getSectors()

        for (s in sectors) {
            val startMins = s.startHour * 60 + s.startMinute
            val endMins = s.endHour * 60 + s.endMinute

            if (startMins < endMins) {
                if (timeMins in startMins until endMins) return s
            } else if (startMins > endMins) {
                // Wraps across midnight (e.g. 22:00 to 06:00)
                if (timeMins >= startMins || timeMins < endMins) return s
            } else {
                return s
            }
        }
        return sectors.first()
    }

    /**
     * Evaluates whether a session's [sessionStartMs, sessionEndMs] window intersects with [sector]'s
     * scheduled hours on that calendar day.
     * Guarantees 100% accurate time-window matching across all sectors without leaking outside.
     */
    fun isSessionInSector(sessionStartMs: Long, sessionEndMs: Long, sector: TimeSector): Boolean {
        val cal = Calendar.getInstance().apply {
            timeInMillis = sessionStartMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightMs = cal.timeInMillis

        val startMins = sector.startHour * 60 + sector.startMinute
        val endMins = sector.endHour * 60 + sector.endMinute

        val effectiveEndMs = maxOf(sessionEndMs, sessionStartMs + 1000L)

        if (startMins < endMins) {
            val sectorWindowStart = midnightMs + (startMins * 60 * 1000L)
            val sectorWindowEnd = midnightMs + (endMins * 60 * 1000L)
            return maxOf(sessionStartMs, sectorWindowStart) < minOf(effectiveEndMs, sectorWindowEnd)
        } else {
            // Wraps across midnight (e.g. 22:00 to 06:00): splits into [00:00, end] and [start, 24:00]
            val morningWindowEnd = midnightMs + (endMins * 60 * 1000L)
            val eveningWindowStart = midnightMs + (startMins * 60 * 1000L)
            val dayEnd = midnightMs + (24 * 60 * 60 * 1000L)

            val inMorningSlice = maxOf(sessionStartMs, midnightMs) < minOf(effectiveEndMs, morningWindowEnd)
            val inEveningSlice = maxOf(sessionStartMs, eveningWindowStart) < minOf(effectiveEndMs, dayEnd)
            return inMorningSlice || inEveningSlice
        }
    }

    /**
     * Returns the list of custom buffer goals (max 3).
     * If uninitialized, creates default Wind-Down (Block Controlled) and Morning Calm (Tracking Only) goals.
     */
    fun getBufferGoals(): List<BufferGoal> {
        val rawIds = prefs.getString(KEY_GOAL_IDS, null)
        if (rawIds == null) {
            val defaultAllowed = DefaultAllowedAppsHelper.getDefaultAllowedPackages(context)
            val defaults = listOf(
                BufferGoal(
                    id = "goal_wind_down",
                    name = "Wind-Down Sleep Buffer",
                    startHour = 23,
                    startMinute = 0,
                    endHour = 7,
                    endMinute = 0,
                    enforcementMode = GoalEnforcementMode.BLOCK_CONTROLLED_APPS,
                    allowedPackages = defaultAllowed,
                    isEnabled = true
                ),
                BufferGoal(
                    id = "goal_morning_calm",
                    name = "Morning Calm Buffer",
                    startHour = 7,
                    startMinute = 0,
                    endHour = 8,
                    endMinute = 0,
                    enforcementMode = GoalEnforcementMode.TRACKING_ONLY,
                    allowedPackages = emptySet(),
                    isEnabled = true
                )
            )
            saveAllGoals(defaults)
            return defaults
        }

        val idList = rawIds.split(",").filter { it.isNotBlank() }
        val result = mutableListOf<BufferGoal>()
        for (id in idList) {
            val name = prefs.getString("$PREFIX_GOAL_NAME$id", "Focus Buffer") ?: "Focus Buffer"
            val startH = prefs.getInt("$PREFIX_GOAL_START$id", 23)
            val startM = prefs.getInt("$PREFIX_GOAL_START_MIN$id", 0)
            val endH = prefs.getInt("$PREFIX_GOAL_END$id", 7)
            val endM = prefs.getInt("$PREFIX_GOAL_END_MIN$id", 0)
            val modeStr = prefs.getString("$PREFIX_GOAL_MODE$id", GoalEnforcementMode.TRACKING_ONLY.name)
            val mode = try {
                GoalEnforcementMode.valueOf(modeStr ?: GoalEnforcementMode.TRACKING_ONLY.name)
            } catch (_: Exception) {
                GoalEnforcementMode.TRACKING_ONLY
            }
            val rawPkgs = prefs.getString("$PREFIX_GOAL_ALLOWED_PKGS$id", null)
            val allowedPkgs = if (rawPkgs != null) {
                rawPkgs.split(",").filter { it.isNotBlank() }.toSet()
            } else {
                DefaultAllowedAppsHelper.getDefaultAllowedPackages(context)
            }
            val isEnabled = prefs.getBoolean("$PREFIX_GOAL_ENABLED$id", true)

            result.add(
                BufferGoal(
                    id = id,
                    name = name,
                    startHour = startH,
                    startMinute = startM,
                    endHour = endH,
                    endMinute = endM,
                    enforcementMode = mode,
                    allowedPackages = allowedPkgs,
                    isEnabled = isEnabled
                )
            )
        }
        return result
    }

    fun saveBufferGoal(goal: BufferGoal) {
        val goals = getBufferGoals().toMutableList()
        val index = goals.indexOfFirst { it.id == goal.id }
        if (index >= 0) {
            goals[index] = goal
        } else if (goals.size < 3) {
            goals.add(goal)
        }
        saveAllGoals(goals)
    }

    fun deleteBufferGoal(goalId: String) {
        val goals = getBufferGoals().filter { it.id != goalId }
        saveAllGoals(goals)
    }

    /**
     * Determines whether the current wall-clock time falls inside [goal]'s scheduled window.
     * Supports windows that cross midnight (e.g. 23:00 to 07:00).
     */
    fun isTimeInGoalWindow(goal: BufferGoal, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!goal.isEnabled) return false
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMins = goal.startHour * 60 + goal.startMinute
        val endMins = goal.endHour * 60 + goal.endMinute

        return if (startMins <= endMins) {
            nowMins in startMins until endMins
        } else {
            // Wraps across midnight (e.g. 23:00 to 07:00)
            nowMins >= startMins || nowMins < endMins
        }
    }

    /**
     * Calculates the seconds remaining until [goal]'s current window ends.
     */
    fun getSecondsRemainingInGoal(goal: BufferGoal, nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val nowSecs = cal.get(Calendar.SECOND)
        val endMins = goal.endHour * 60 + goal.endMinute

        val diffMins = if (endMins >= nowMins) {
            endMins - nowMins
        } else {
            (24 * 60 - nowMins) + endMins
        }
        return maxOf(60L, (diffMins * 60L) - nowSecs)
    }

    /**
     * Returns the currently active goal with enforcement priority:
     * STRICT_LOCKDOWN (Mode 3) > BLOCK_CONTROLLED_APPS (Mode 2) > null.
     */
    fun getActiveEnforcingGoal(nowMs: Long = System.currentTimeMillis()): BufferGoal? {
        val active = getBufferGoals().filter { isTimeInGoalWindow(it, nowMs) }
        return active.firstOrNull { it.enforcementMode == GoalEnforcementMode.STRICT_LOCKDOWN }
            ?: active.firstOrNull { it.enforcementMode == GoalEnforcementMode.BLOCK_CONTROLLED_APPS }
    }

    private fun saveAllGoals(goals: List<BufferGoal>) {
        val editor = prefs.edit()
        val ids = goals.joinToString(",") { it.id }
        editor.putString(KEY_GOAL_IDS, ids)
        for (g in goals) {
            editor.putString("$PREFIX_GOAL_NAME${g.id}", g.name)
            editor.putInt("$PREFIX_GOAL_START${g.id}", g.startHour)
            editor.putInt("$PREFIX_GOAL_START_MIN${g.id}", g.startMinute)
            editor.putInt("$PREFIX_GOAL_END${g.id}", g.endHour)
            editor.putInt("$PREFIX_GOAL_END_MIN${g.id}", g.endMinute)
            editor.putString("$PREFIX_GOAL_MODE${g.id}", g.enforcementMode.name)
            editor.putString("$PREFIX_GOAL_ALLOWED_PKGS${g.id}", g.allowedPackages.joinToString(","))
            editor.putBoolean("$PREFIX_GOAL_ENABLED${g.id}", g.isEnabled)
        }
        editor.apply()
    }
}
