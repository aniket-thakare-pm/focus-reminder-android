package com.example.focusreminder.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.focusreminder.platform.ReminderScheduler
import com.example.focusreminder.ui.reminder.StickyReminderActivity
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.productivity.focusreminder.ACTION_TRIGGER_REMINDER"
        const val ACTION_ACKNOWLEDGE_REMINDER = "com.productivity.focusreminder.ACTION_ACKNOWLEDGE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.productivity.focusreminder.ACTION_SNOOZE_REMINDER"

        const val CHANNEL_ID = "focus_reminder_p0_channel"
        const val CHANNEL_NAME = "High-Attention Focus Reminders"

        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_IS_P0 = "extra_is_p0"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_ID, 1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Scheduled Reminder"
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
        val isP0 = intent.getBooleanExtra(EXTRA_IS_P0, true)

        when (intent.action) {
            ACTION_ACKNOWLEDGE_REMINDER -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId.toInt())
            }
            ACTION_SNOOZE_REMINDER -> {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(reminderId.toInt())
                // Snooze for 10 minutes
                ReminderScheduler.scheduleReminder(
                    context = context,
                    reminderId = reminderId,
                    title = title,
                    description = description,
                    triggerAtMs = System.currentTimeMillis() + (10 * 60 * 1000L),
                    isP0 = isP0
                )
            }
            ACTION_TRIGGER_REMINDER -> {
                deliverReminder(context, reminderId, title, description, isP0)
            }
        }
    }

    private fun deliverReminder(
        context: Context,
        reminderId: Long,
        title: String,
        description: String?,
        isP0: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create high-importance notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Urgent, prominent alerts for scheduled productivity reminders"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        // Intent to launch full-screen StickyReminderActivity
        val fullScreenIntent = StickyReminderActivity.createIntent(
            context = context,
            id = reminderId,
            title = title,
            description = description,
            time = timeString
        )
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action: Acknowledge
        val ackIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_ACKNOWLEDGE_REMINDER
            putExtra(EXTRA_ID, reminderId)
        }
        val ackPendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 1000).toInt(),
            ackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action: Snooze 10m
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_ID, reminderId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_IS_P0, isP0)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 2000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Focus Alert: $title")
            .setContentText(description ?: "Scheduled for $timeString")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(!isP0) // P0 does NOT auto-dismiss
            .setOngoing(isP0)     // P0 sticks until acknowledged
            .setFullScreenIntent(fullScreenPendingIntent, true) // Prominent high-attention takeover
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Acknowledge", ackPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 10m", snoozePendingIntent)

        notificationManager.notify(reminderId.toInt(), builder.build())

        // Also directly attempt activity launch if screen is on or permitted
        try {
            context.startActivity(fullScreenIntent)
        } catch (_: Exception) {
            // Background activity start restriction fallback handled by setFullScreenIntent above
        }
    }
}
