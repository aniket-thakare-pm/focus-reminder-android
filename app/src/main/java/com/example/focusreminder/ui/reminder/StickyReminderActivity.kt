package com.example.focusreminder.ui.reminder

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.focusreminder.platform.ReminderScheduler
import com.example.focusreminder.theme.FocusReminderTheme

class StickyReminderActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_TIME = "extra_time"

        fun createIntent(
            context: Context,
            id: Long,
            title: String,
            description: String?,
            time: String
        ): Intent {
            return Intent(context, StickyReminderActivity::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, id)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DESCRIPTION, description)
                putExtra(EXTRA_TIME, time)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure window to awaken device and display above keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Focus Reminder"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
        val timeFormatted = intent.getStringExtra(EXTRA_TIME) ?: "Now"

        setContent {
            FocusReminderTheme {
                StickyReminderScreen(
                    title = title,
                    description = description,
                    scheduledTimeFormatted = timeFormatted,
                    onAcknowledge = {
                        // Dismiss system notification
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(reminderId.toInt())

                        Toast.makeText(this@StickyReminderActivity, "Reminder Acknowledged!", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onSnooze = { snoozeMins ->
                        // Schedule snooze alarm
                        ReminderScheduler.scheduleReminder(
                            context = this@StickyReminderActivity,
                            reminderId = reminderId,
                            title = title,
                            description = description,
                            triggerAtMs = System.currentTimeMillis() + (snoozeMins * 60 * 1000L),
                            isP0 = true
                        )

                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(reminderId.toInt())

                        Toast.makeText(this@StickyReminderActivity, "Snoozed for $snoozeMins minutes", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}
