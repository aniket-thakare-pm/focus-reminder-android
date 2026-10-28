package com.example.focusreminder.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Data model representing an installed app on the device.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isRestricted: Boolean = false,
    val rollingBudgetMinutes: Int = 15,
    val cooldownMinutes: Int = 30,
    val dailyLimitMinutes: Int = 90,
    val isSystemApp: Boolean = false,
    val isLocked: Boolean = false,
    val remainingLockSeconds: Long = 0L
)

/**
 * Helper to query and filter installed user applications (PRD Section 5.1).
 * Features in-memory caching and background dispatch to eliminate UI latency.
 */
class AppManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val repo = AppRestrictionRepository.getInstance(context)

    companion object {
        @Volatile
        private var cachedApps: List<InstalledAppInfo>? = null
        private val iconCache = ConcurrentHashMap<String, Drawable>()

        fun clearCache() {
            cachedApps = null
            iconCache.clear()
        }
    }

    /**
     * Retrieves all applications, categorizing user apps vs system apps.
     * Uses memory cache when available, updating restriction/budget state in < 1ms.
     */
    fun getLaunchableApps(forceScan: Boolean = false): List<InstalledAppInfo> {
        val existing = cachedApps
        if (!forceScan && existing != null) {
            // Quick update of dynamic repository state (< 1ms)
            return existing.map { app ->
                val pkg = app.packageName
                app.copy(
                    isRestricted = repo.isRestricted(pkg),
                    rollingBudgetMinutes = repo.getRollingBudgetMinutes(pkg),
                    cooldownMinutes = repo.getCooldownMinutes(pkg),
                    dailyLimitMinutes = repo.getDailyLimitMinutes(pkg),
                    isLocked = repo.isAppLocked(pkg),
                    remainingLockSeconds = repo.getRemainingLockSeconds(pkg)
                )
            }
        }

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val myPackageName = context.packageName
        val foundPackages = mutableSetOf<String>()
        val result = mutableListOf<InstalledAppInfo>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == myPackageName || foundPackages.contains(pkg)) continue
            foundPackages.add(pkg)

            val label = resolveInfo.loadLabel(packageManager).toString()
            val icon = iconCache.getOrPut(pkg) {
                try {
                    resolveInfo.loadIcon(packageManager)
                } catch (_: Exception) {
                    packageManager.defaultActivityIcon
                }
            }
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val isRestricted = repo.isRestricted(pkg)
            val rolling = repo.getRollingBudgetMinutes(pkg)
            val cooldown = repo.getCooldownMinutes(pkg)
            val daily = repo.getDailyLimitMinutes(pkg)
            val isLocked = repo.isAppLocked(pkg)
            val lockSecs = repo.getRemainingLockSeconds(pkg)

            result.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = icon,
                    isRestricted = isRestricted,
                    rollingBudgetMinutes = rolling,
                    cooldownMinutes = cooldown,
                    dailyLimitMinutes = daily,
                    isSystemApp = isSystem,
                    isLocked = isLocked,
                    remainingLockSeconds = lockSecs
                )
            )
        }

        // Also check if any restricted packages in repository were not found in launcher activities
        for (pkg in repo.getRestrictedPackages()) {
            if (!foundPackages.contains(pkg)) {
                foundPackages.add(pkg)
                val label = repo.getAppName(pkg, pkg.substringAfterLast("."))
                val icon = iconCache.getOrPut(pkg) {
                    try {
                        packageManager.getApplicationIcon(pkg)
                    } catch (_: Exception) {
                        packageManager.defaultActivityIcon
                    }
                }
                result.add(
                    InstalledAppInfo(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        isRestricted = true,
                        rollingBudgetMinutes = repo.getRollingBudgetMinutes(pkg),
                        cooldownMinutes = repo.getCooldownMinutes(pkg),
                        dailyLimitMinutes = repo.getDailyLimitMinutes(pkg),
                        isSystemApp = false,
                        isLocked = repo.isAppLocked(pkg),
                        remainingLockSeconds = repo.getRemainingLockSeconds(pkg)
                    )
                )
            }
        }

        // User apps first (non-system), then alphabetically
        val sorted = result.sortedWith(
            compareBy<InstalledAppInfo> { it.isSystemApp }
                .thenBy { it.appName.lowercase() }
        )
        cachedApps = sorted
        return sorted
    }

    suspend fun getLaunchableAppsAsync(forceScan: Boolean = false): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            getLaunchableApps(forceScan)
        }

    /**
     * Popular distraction apps presets so users can easily add/test them on any device or emulator.
     */
    fun getPopularDistractionApps(): List<Pair<String, String>> {
        return listOf(
            "com.instagram.android" to "Instagram",
            "com.google.android.youtube" to "YouTube",
            "com.reddit.frontpage" to "Reddit",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.twitter.android" to "X (Twitter)",
            "com.whatsapp" to "WhatsApp",
            "com.snapchat.android" to "Snapchat",
            "com.android.chrome" to "Chrome"
        )
    }
}
