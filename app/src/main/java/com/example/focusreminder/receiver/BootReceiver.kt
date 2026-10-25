package com.example.focusreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.focusreminder.platform.ReminderRepository
import com.example.focusreminder.platform.UsagePermissionHelper
import com.example.focusreminder.service.UsageMonitorService

/**
 * Handles device boot completion and screen unlock resuscitation.
 *
 * Implements:
 * - Restores scheduled reminders from local persistence on boot.
 * - Resuscitates UsageMonitorService on screen unlock (ACTION_USER_PRESENT) if killed by OEM battery savers.
 * - Resets active temporary lock state on reboot according to MVP restart policy.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_USER_PRESENT) {
            // Screen unlocked: resuscitate enforcement service if permissions are active
            if (UsagePermissionHelper.hasUsageAccessPermission(context)) {
                UsageMonitorService.start(context)
            }
            return
        }

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.i("FocusReminder", "Device boot detected. Restoring scheduled reminders and resetting active lock state.")
            
            // MVP Restart Policy: Clear active temporary lock / override state
            val overridePrefs = context.getSharedPreferences("emergency_override", Context.MODE_PRIVATE)
            overridePrefs.edit().clear().apply()

            // Reschedule all active reminders from persistence
            try {
                ReminderRepository.getInstance(context).rescheduleAllActiveAlarms(context)
            } catch (e: Exception) {
                Log.e("FocusReminder", "Failed to reschedule reminders after boot", e)
            }

            if (UsagePermissionHelper.hasUsageAccessPermission(context)) {
                UsageMonitorService.start(context)
            }
        }
    }
}
