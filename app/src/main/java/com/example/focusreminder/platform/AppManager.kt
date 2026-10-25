package com.example.focusreminder.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * Data model representing an installed app on the device.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isRestricted: Boolean = false,
    val rollingBudgetMinutes: Int = 30,
    val dailyLimitMinutes: Int = 90,
    val isSystemApp: Boolean = false,
    val isLocked: Boolean = false,
    val remainingLockSeconds: Long = 0L
)

/**
 * Helper to query and filter installed user applications (PRD Section 5.1).
 */
class AppManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val repo = AppRestrictionRepository.getInstance(context)

    /**
     * Retrieves all applications, categorizing user apps vs system apps.
     */
    fun getLaunchableApps(): List<InstalledAppInfo> {
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
            val icon = resolveInfo.loadIcon(packageManager)
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val isRestricted = repo.isRestricted(pkg)
            val rolling = repo.getRollingBudgetMinutes(pkg)
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
                val icon = try {
                    packageManager.getApplicationIcon(pkg)
                } catch (_: Exception) {
                    null
                }
                result.add(
                    InstalledAppInfo(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        isRestricted = true,
                        rollingBudgetMinutes = repo.getRollingBudgetMinutes(pkg),
                        dailyLimitMinutes = repo.getDailyLimitMinutes(pkg),
                        isSystemApp = false,
                        isLocked = repo.isAppLocked(pkg),
                        remainingLockSeconds = repo.getRemainingLockSeconds(pkg)
                    )
                )
            }
        }

        // User apps first (non-system), then alphabetically
        return result.sortedWith(
            compareBy<InstalledAppInfo> { it.isSystemApp }
                .thenBy { it.appName.lowercase() }
        )
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
