package com.example.focusreminder.platform

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Filter for non-user system daemons, background updaters, and system UI components.
 * Prevents OS maintenance windows (e.g. ColorOS/OxygenOS Software Auto-Update) from
 * polluting user screen time, usage logs, or night sector metrics.
 */
object SystemPackageFilter {

    private val isLaunchableCache = ConcurrentHashMap<String, Boolean>()

    /**
     * Returns true if [pkg] is a system UI, launcher, OS updater daemon, host app,
     * or any non-launchable background service.
     */
    fun isIgnoredOrSystem(context: Context, pkg: String): Boolean {
        if (pkg == context.packageName) return true
        if (isKnownSystemOrUpdater(pkg)) return true
        if (AppRestrictionRepository.getInstance(context).isExcludedFromAnalytics(pkg)) return true

        return !isLaunchableCache.getOrPut(pkg) {
            try {
                context.packageManager.getLaunchIntentForPackage(pkg) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Fast prefix/pattern check for known OEM maintenance, updater, and system packages.
     */
    fun isKnownSystemOrUpdater(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.startsWith("com.android.launcher") ||
                lower.startsWith("com.google.android.apps.nexuslauncher") ||
                lower.startsWith("com.sec.android.app.launcher") ||
                lower.startsWith("com.coloros.launcher") ||
                lower.startsWith("com.oppo.launcher") ||
                lower.startsWith("com.oneplus.launcher") ||
                lower.contains(".launcher") ||
                lower.startsWith("com.miui.home") ||
                lower.startsWith("com.google.android.inputmethod") ||
                lower.contains(".inputmethod") ||
                lower.startsWith("com.android.systemui") ||
                lower.startsWith("com.coloros.sau") ||
                lower.startsWith("com.oplus.sau") ||
                lower.startsWith("com.oppo.ota") ||
                lower.startsWith("com.oplus.ota") ||
                lower.startsWith("com.oneplus.ota") ||
                lower.startsWith("com.android.updater") ||
                lower.startsWith("com.wssyncmldm") ||
                lower.startsWith("com.sec.android.soagent") ||
                lower.startsWith("com.samsung.sdm") ||
                lower.startsWith("com.google.android.gms") ||
                lower.startsWith("com.google.android.overlay") ||
                lower.startsWith("com.google.android.feedback") ||
                lower.startsWith("com.android.dynsystem") ||
                lower.startsWith("com.oplus.postman") ||
                lower.startsWith("com.oplus.deepthinker") ||
                lower.startsWith("com.oplus.subsys") ||
                lower.startsWith("com.coloros.oppoguardelf") ||
                lower.startsWith("com.oplus.battery") ||
                lower.startsWith("com.coloros.securityguard") ||
                lower.startsWith("com.oplus.athena") ||
                lower.contains(".ota") ||
                lower.contains(".sau") ||
                lower.contains("systemupdate") ||
                lower.contains("softwareupdate")
    }
}
