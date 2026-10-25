package com.example.focusreminder.platform

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.Locale

/**
 * Persistent High-Water Mark Store for daily app usage and total screen time.
 *
 * Implements monotonic daily usage guarantees:
 * - Daily usage within the same calendar day can NEVER decrease under any circumstances.
 * - Prevents Android RAM event buffer evictions from shrinking reported usage late in the day.
 * - Automatically rolls over at midnight (00:00:00) using date keys.
 * - Auto-purges records older than 7 days to maintain a clean storage footprint.
 * - Auto-wipes corrupted high-water marks from earlier schema versions on startup.
 */
class DailyUsageHighWaterMarkStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("daily_usage_high_water_mark_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val CURRENT_VERSION = 2
        private const val KEY_STORE_VERSION = "store_version"
        private const val PREFIX_USAGE = "usage_"
        private const val PREFIX_SCREEN_TIME = "screen_time_"
        private const val KEY_LAST_CLEANED_DATE = "last_cleaned_date"

        @Volatile
        private var INSTANCE: DailyUsageHighWaterMarkStore? = null

        fun getInstance(context: Context): DailyUsageHighWaterMarkStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DailyUsageHighWaterMarkStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getTodayDateKey(): String {
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        }
    }

    init {
        val storedVersion = prefs.getInt(KEY_STORE_VERSION, 1)
        if (storedVersion < CURRENT_VERSION) {
            // Version upgrade: Flush any corrupted inflated high-water marks from previous versions
            prefs.edit()
                .clear()
                .putInt(KEY_STORE_VERSION, CURRENT_VERSION)
                .apply()
        }
    }

    /**
     * Resets all recorded high-water marks for [dateKey] (defaults to today).
     */
    fun resetTodayCache(dateKey: String = getTodayDateKey()) {
        val editor = prefs.edit()
        for (k in prefs.all.keys) {
            if (k.startsWith("${PREFIX_USAGE}${dateKey}_") || k == "${PREFIX_SCREEN_TIME}$dateKey") {
                editor.remove(k)
            }
        }
        editor.apply()
    }

    /**
     * Gets the current persistent high-water mark for [packageName] on [dateKey] in milliseconds.
     */
    fun getHighWaterMarkMs(packageName: String, dateKey: String = getTodayDateKey()): Long {
        return prefs.getLong("${PREFIX_USAGE}${dateKey}_$packageName", 0L)
    }

    /**
     * Updates the persistent high-water mark for [packageName] on [dateKey].
     * Returns the monotonic maximum of (current stored, candidate [newUsageMs]).
     * Discards physically impossible high-water marks that exceed elapsed time today.
     */
    fun updateHighWaterMarkMs(
        packageName: String,
        newUsageMs: Long,
        dateKey: String = getTodayDateKey()
    ): Long {
        val key = "${PREFIX_USAGE}${dateKey}_$packageName"
        val current = prefs.getLong(key, 0L)

        // Sanity check: Daily usage cannot exceed wall-clock time elapsed since today's midnight
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val elapsedTodayMs = maxOf(0L, System.currentTimeMillis() - cal.timeInMillis)
        val validCurrent = if (dateKey == getTodayDateKey() && current > elapsedTodayMs) 0L else current

        val verifiedMax = maxOf(validCurrent, minOf(newUsageMs, elapsedTodayMs))
        if (verifiedMax > current || (current > elapsedTodayMs)) {
            prefs.edit().putLong(key, verifiedMax).apply()
        }
        checkAndCleanOldDays(dateKey)
        return verifiedMax
    }

    /**
     * Gets the current persistent high-water mark for total device screen time on [dateKey].
     */
    fun getScreenTimeHighWaterMarkMs(dateKey: String = getTodayDateKey()): Long {
        return prefs.getLong("${PREFIX_SCREEN_TIME}$dateKey", 0L)
    }

    /**
     * Updates the persistent high-water mark for total device screen time on [dateKey].
     * Returns the monotonic maximum of (current stored, candidate [newScreenTimeMs]).
     */
    fun updateScreenTimeHighWaterMarkMs(
        newScreenTimeMs: Long,
        dateKey: String = getTodayDateKey()
    ): Long {
        val key = "${PREFIX_SCREEN_TIME}$dateKey"
        val current = prefs.getLong(key, 0L)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val elapsedTodayMs = maxOf(0L, System.currentTimeMillis() - cal.timeInMillis)
        val validCurrent = if (dateKey == getTodayDateKey() && current > elapsedTodayMs) 0L else current

        val verifiedMax = maxOf(validCurrent, minOf(newScreenTimeMs, elapsedTodayMs))
        if (verifiedMax > current || (current > elapsedTodayMs)) {
            prefs.edit().putLong(key, verifiedMax).apply()
        }
        checkAndCleanOldDays(dateKey)
        return verifiedMax
    }

    /**
     * Periodically cleans up keys older than [retentionDays] days.
     */
    private fun checkAndCleanOldDays(todayKey: String, retentionDays: Int = 7) {
        val lastCleaned = prefs.getString(KEY_LAST_CLEANED_DATE, null)
        if (lastCleaned == todayKey) return

        val cutoffCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -retentionDays)
        }
        val cutoffKey = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cutoffCal.get(Calendar.YEAR),
            cutoffCal.get(Calendar.MONTH) + 1,
            cutoffCal.get(Calendar.DAY_OF_MONTH)
        )

        val editor = prefs.edit()
        val allKeys = prefs.all.keys
        for (k in allKeys) {
            if (k.startsWith(PREFIX_USAGE)) {
                // Key format: usage_YYYY-MM-DD_pkg
                val datePart = k.removePrefix(PREFIX_USAGE).substringBefore("_")
                if (datePart.length == 10 && datePart < cutoffKey) {
                    editor.remove(k)
                }
            } else if (k.startsWith(PREFIX_SCREEN_TIME)) {
                // Key format: screen_time_YYYY-MM-DD
                val datePart = k.removePrefix(PREFIX_SCREEN_TIME)
                if (datePart.length == 10 && datePart < cutoffKey) {
                    editor.remove(k)
                }
            }
        }
        editor.putString(KEY_LAST_CLEANED_DATE, todayKey).apply()
    }
}
