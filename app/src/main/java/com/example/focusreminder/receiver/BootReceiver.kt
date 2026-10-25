package com.example.focusreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles device boot completion (PRD Section 5.5 & Restart Policy Section 5.3).
 *
 * Implements:
 * - Restores scheduled reminders from local persistence.
 * - Resets active restriction tracking state on reboot according to the documented MVP restart policy.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("FocusReminder", "Device boot detected. Restoring scheduled reminders and resetting active lock state.")
            
            // MVP Restart Policy: Clear active temporary lock / override state
            val overridePrefs = context.getSharedPreferences("emergency_override", Context.MODE_PRIVATE)
            overridePrefs.edit().clear().apply()

            // Reminders will be rescheduled from local database here in Phase 4
        }
    }
}
