package com.example.focusreminder.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony

/**
 * Dynamically resolves standard essential application packages across all Android OEMs
 * (Google Pixel, Samsung OneUI, Xiaomi HyperOS, OnePlus OxygenOS, etc.).
 *
 * Used for Strict Lockdown (Digital Detox) mode to ensure vital emergency and utility
 * functions (Phone, Messages, Maps, Camera, Clock, Calculator, Contacts, Settings) remain accessible.
 */
object DefaultAllowedAppsHelper {

    // Common OEM fallback packages
    private val KNOWN_ESSENTIAL_PACKAGES = setOf(
        // Dialer
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.dialer",
        // Messages
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        // Maps
        "com.google.android.apps.maps",
        // Camera
        "com.google.android.GoogleCamera",
        "com.sec.android.app.camera",
        "com.android.camera",
        "com.android.camera2",
        // Clock / Alarm
        "com.google.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.android.deskclock",
        // Calculator
        "com.google.android.calculator",
        "com.sec.android.app.popupcalculator",
        "com.miui.calculator",
        "com.android.calculator2",
        // Contacts
        "com.google.android.contacts",
        "com.samsung.android.app.contacts",
        "com.android.contacts",
        // Settings
        "com.android.settings"
    )

    /**
     * Dynamically queries the system PackageManager for the device's default essential apps,
     * supplemented with installed OEM package fallbacks.
     */
    fun getDefaultAllowedPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val resolved = mutableSetOf<String>()

        // 1. Phone / Dialer
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            val info = pm.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { resolved.add(it) }
        } catch (_: Exception) {}

        // 2. Default SMS app
        try {
            val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
            if (!defaultSms.isNullOrBlank()) {
                resolved.add(defaultSms)
            }
        } catch (_: Exception) {}

        // 3. Navigation / Maps
        try {
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
            val info = pm.resolveActivity(mapIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { resolved.add(it) }
        } catch (_: Exception) {}

        // 4. Camera
        try {
            val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val info = pm.resolveActivity(camIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { resolved.add(it) }
        } catch (_: Exception) {}

        // 5. Clock / Alarm
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            val info = pm.resolveActivity(alarmIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { resolved.add(it) }
        } catch (_: Exception) {}

        // 6. Contacts
        try {
            val contactsIntent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
            val info = pm.resolveActivity(contactsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { resolved.add(it) }
        } catch (_: Exception) {}

        // 7. Settings
        try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            val info = pm.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName?.let { resolved.add(it) }
        } catch (_: Exception) {}

        // 8. Add known installed fallback packages
        for (pkg in KNOWN_ESSENTIAL_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                resolved.add(pkg)
            } catch (_: Exception) {}
        }

        return resolved
    }

    /**
     * Checks if a package is a system shell, launcher, input method, or emergency dialer
     * that must never be blocked under any circumstances.
     */
    fun isAlwaysExemptSystemPackage(pkg: String, context: Context): Boolean {
        if (pkg == context.packageName) return true
        if (pkg.startsWith("com.android.systemui")) return true
        if (pkg.startsWith("com.android.settings")) return true

        // Launchers / Home screens
        if (pkg.startsWith("com.android.launcher") ||
            pkg.startsWith("com.google.android.apps.nexuslauncher") ||
            pkg.startsWith("com.sec.android.app.launcher") ||
            pkg.startsWith("com.miui.home") ||
            pkg.contains("launcher")
        ) {
            return true
        }

        // Input methods (Keyboards)
        if (pkg.startsWith("com.google.android.inputmethod") ||
            pkg.contains("inputmethod") ||
            pkg.contains(".ime")
        ) {
            return true
        }

        return false
    }
}
