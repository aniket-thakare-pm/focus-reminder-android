package com.example.focusreminder.platform

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Configuration and persistent storage for monitored applications and their task locks.
 */
data class MonitoredAppConfig(
    val packageName: String,
    val appName: String,
    val isRestricted: Boolean = true,
    val rollingBudgetMinutes: Int = 30,
    val dailyLimitMinutes: Int = 90,
    val lockExpiryMs: Long = 0L,
    val emergencyOverrideExpiryMs: Long = 0L
)

class AppRestrictionRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_restrictions_prefs", Context.MODE_PRIVATE)

    private val _restrictionsUpdateFlow = MutableStateFlow(System.currentTimeMillis())
    val restrictionsUpdateFlow: StateFlow<Long> = _restrictionsUpdateFlow.asStateFlow()

    companion object {
        private const val KEY_RESTRICTED_PACKAGES = "restricted_packages"
        private const val PREFIX_ROLLING = "rolling_min_"
        private const val PREFIX_DAILY = "daily_min_"
        private const val PREFIX_LOCK_EXPIRY = "lock_expiry_"
        private const val PREFIX_APP_NAME = "app_name_"
        private const val PREFIX_OVERRIDE = "override_expiry_"

        @Volatile
        private var INSTANCE: AppRestrictionRepository? = null

        fun getInstance(context: Context): AppRestrictionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRestrictionRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Set of package names that have restriction active.
     */
    fun getRestrictedPackages(): Set<String> {
        return prefs.getStringSet(KEY_RESTRICTED_PACKAGES, emptySet()) ?: emptySet()
    }

    /**
     * Checks if a package is restricted.
     */
    fun isRestricted(packageName: String): Boolean {
        return getRestrictedPackages().contains(packageName)
    }

    /**
     * Toggle or set an application's restricted status.
     */
    fun setRestricted(packageName: String, appName: String, restricted: Boolean) {
        val current = getRestrictedPackages().toMutableSet()
        if (restricted) {
            current.add(packageName)
            prefs.edit()
                .putStringSet(KEY_RESTRICTED_PACKAGES, current)
                .putString(PREFIX_APP_NAME + packageName, appName)
                .apply()
        } else {
            current.remove(packageName)
            prefs.edit()
                .putStringSet(KEY_RESTRICTED_PACKAGES, current)
                .remove(PREFIX_LOCK_EXPIRY + packageName)
                .apply()
        }
        notifyChange()
    }

    /**
     * Set rolling and daily budget limits in minutes.
     */
    fun setLimits(packageName: String, rollingMinutes: Int, dailyMinutes: Int) {
        prefs.edit()
            .putInt(PREFIX_ROLLING + packageName, rollingMinutes)
            .putInt(PREFIX_DAILY + packageName, dailyMinutes)
            .apply()
        notifyChange()
    }

    fun getRollingBudgetMinutes(packageName: String): Int {
        return prefs.getInt(PREFIX_ROLLING + packageName, 30)
    }

    fun getDailyLimitMinutes(packageName: String): Int {
        return prefs.getInt(PREFIX_DAILY + packageName, 90)
    }

    fun getAppName(packageName: String, fallback: String = ""): String {
        return prefs.getString(PREFIX_APP_NAME + packageName, fallback) ?: fallback
    }

    /**
     * Task Lock: Manually or automatically lock an application for a specified duration in minutes.
     */
    fun lockApp(packageName: String, appName: String, durationMinutes: Int = 30) {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        val current = getRestrictedPackages().toMutableSet()
        current.add(packageName)

        prefs.edit()
            .putStringSet(KEY_RESTRICTED_PACKAGES, current)
            .putString(PREFIX_APP_NAME + packageName, appName)
            .putLong(PREFIX_LOCK_EXPIRY + packageName, expiry)
            .apply()

        notifyChange()
    }

    /**
     * Release a task lock for an application.
     */
    fun unlockApp(packageName: String) {
        prefs.edit()
            .remove(PREFIX_LOCK_EXPIRY + packageName)
            .apply()
        notifyChange()
    }

    /**
     * Returns true if the package is currently under an active task lock.
     */
    fun isAppLocked(packageName: String): Boolean {
        val expiry = prefs.getLong(PREFIX_LOCK_EXPIRY + packageName, 0L)
        return expiry > System.currentTimeMillis()
    }

    /**
     * Returns remaining lock time in seconds.
     */
    fun getRemainingLockSeconds(packageName: String): Long {
        val expiry = prefs.getLong(PREFIX_LOCK_EXPIRY + packageName, 0L)
        val diff = expiry - System.currentTimeMillis()
        return if (diff > 0) diff / 1000L else 0L
    }

    /**
     * Emergency reflection override: grant access for a short duration.
     */
    fun grantEmergencyOverride(packageName: String, durationMinutes: Int) {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        prefs.edit()
            .putLong(PREFIX_OVERRIDE + packageName, expiry)
            .remove(PREFIX_LOCK_EXPIRY + packageName) // clears direct lock
            .apply()
        notifyChange()
    }

    fun hasActiveEmergencyOverride(packageName: String): Boolean {
        val expiry = prefs.getLong(PREFIX_OVERRIDE + packageName, 0L)
        return expiry > System.currentTimeMillis()
    }

    private fun notifyChange() {
        _restrictionsUpdateFlow.value = System.currentTimeMillis()
    }
}
