package com.example.focusreminder.platform.analytics

import android.content.Context
import android.content.SharedPreferences
import com.example.focusreminder.domain.model.TimeSector
import java.util.Calendar

/**
 * Manages customizable time-of-day sector definitions and sleep/wake buffer goals.
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

        private const val KEY_SLEEP_BUFFER_START_HOUR = "sleep_buffer_start_hour"
        private const val KEY_SLEEP_BUFFER_END_HOUR = "sleep_buffer_end_hour"

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
     * Updates boundaries for a sector.
     */
    fun updateSector(sectorId: String, startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt("${sectorId}_start", startHour)
            .putInt("${sectorId}_end", endHour)
            .apply()
    }

    /**
     * Returns the sleep buffer window (default 23:00 to 00:00, 1 hour before sleep).
     */
    fun getSleepBufferWindow(): Pair<Int, Int> {
        val start = prefs.getInt(KEY_SLEEP_BUFFER_START_HOUR, 23)
        val end = prefs.getInt(KEY_SLEEP_BUFFER_END_HOUR, 0)
        return Pair(start, end)
    }

    /**
     * Configures the sleep buffer window.
     */
    fun setSleepBufferWindow(startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt(KEY_SLEEP_BUFFER_START_HOUR, startHour)
            .putInt(KEY_SLEEP_BUFFER_END_HOUR, endHour)
            .apply()
    }
}
