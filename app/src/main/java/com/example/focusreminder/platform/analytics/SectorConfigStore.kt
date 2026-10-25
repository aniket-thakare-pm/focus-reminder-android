package com.example.focusreminder.platform.analytics

import android.content.Context
import android.content.SharedPreferences
import com.example.focusreminder.domain.model.BufferGoal
import com.example.focusreminder.domain.model.TimeSector
import java.util.Calendar

/**
 * Manages customizable time-of-day sector definitions and custom focus buffer goals.
 */
class SectorConfigStore private constructor(context: Context) {

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

        private const val KEY_GOAL_IDS = "custom_goal_ids"
        private const val PREFIX_GOAL_NAME = "goal_name_"
        private const val PREFIX_GOAL_START = "goal_start_"
        private const val PREFIX_GOAL_END = "goal_end_"

        @Volatile
        private var INSTANCE: SectorConfigStore? = null

        fun getInstance(context: Context): SectorConfigStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SectorConfigStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Returns the active list of configured time sectors.
     */
    fun getSectors(): List<TimeSector> {
        return listOf(
            TimeSector(
                id = SECTOR_MORNING,
                name = "Morning",
                startHour = prefs.getInt("${SECTOR_MORNING}_start", 6),
                endHour = prefs.getInt("${SECTOR_MORNING}_end", 12),
                colorLong = COLOR_MORNING
            ),
            TimeSector(
                id = SECTOR_AFTERNOON,
                name = "Afternoon",
                startHour = prefs.getInt("${SECTOR_AFTERNOON}_start", 12),
                endHour = prefs.getInt("${SECTOR_AFTERNOON}_end", 17),
                colorLong = COLOR_AFTERNOON
            ),
            TimeSector(
                id = SECTOR_EVENING,
                name = "Evening",
                startHour = prefs.getInt("${SECTOR_EVENING}_start", 17),
                endHour = prefs.getInt("${SECTOR_EVENING}_end", 22),
                colorLong = COLOR_EVENING
            ),
            TimeSector(
                id = SECTOR_NIGHT,
                name = "Night",
                startHour = prefs.getInt("${SECTOR_NIGHT}_start", 22),
                endHour = prefs.getInt("${SECTOR_NIGHT}_end", 6),
                colorLong = COLOR_NIGHT
            )
        )
    }

    /**
     * Resolves which sector a given timestamp belongs to.
     */
    fun getSectorForTimestamp(timestampMs: Long): TimeSector {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val sectors = getSectors()

        for (s in sectors) {
            if (s.startHour <= s.endHour) {
                if (hour >= s.startHour && hour < s.endHour) return s
            } else {
                // Wraps across midnight (e.g. 22:00 to 06:00)
                if (hour >= s.startHour || hour < s.endHour) return s
            }
        }
        return sectors.last()
    }

    /**
     * Returns the list of custom buffer goals (max 3).
     * If uninitialized, creates default Wind-Down and Morning Calm goals.
     */
    fun getBufferGoals(): List<BufferGoal> {
        val rawIds = prefs.getString(KEY_GOAL_IDS, null)
        if (rawIds == null) {
            // Initialize default goals
            val defaults = listOf(
                BufferGoal(id = "goal_wind_down", name = "Wind-Down Sleep Buffer", startHour = 23, endHour = 0),
                BufferGoal(id = "goal_morning_calm", name = "Morning Calm Buffer", startHour = 6, endHour = 7)
            )
            saveAllGoals(defaults)
            return defaults
        }

        val idList = rawIds.split(",").filter { it.isNotBlank() }
        val result = mutableListOf<BufferGoal>()
        for (id in idList) {
            val name = prefs.getString("$PREFIX_GOAL_NAME$id", "Focus Buffer") ?: "Focus Buffer"
            val start = prefs.getInt("$PREFIX_GOAL_START$id", 23)
            val end = prefs.getInt("$PREFIX_GOAL_END$id", 0)
            result.add(BufferGoal(id = id, name = name, startHour = start, endHour = end))
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

    private fun saveAllGoals(goals: List<BufferGoal>) {
        val editor = prefs.edit()
        val ids = goals.joinToString(",") { it.id }
        editor.putString(KEY_GOAL_IDS, ids)
        for (g in goals) {
            editor.putString("$PREFIX_GOAL_NAME${g.id}", g.name)
            editor.putInt("$PREFIX_GOAL_START${g.id}", g.startHour)
            editor.putInt("$PREFIX_GOAL_END${g.id}", g.endHour)
        }
        editor.apply()
    }
}
