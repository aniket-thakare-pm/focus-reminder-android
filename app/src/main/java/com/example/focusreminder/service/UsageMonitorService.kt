package com.example.focusreminder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focusreminder.MainActivity
import com.example.focusreminder.domain.engine.CooldownManager
import com.example.focusreminder.domain.engine.DailyUsageCalculator
import com.example.focusreminder.domain.engine.RollingWindowUsageCalculator
import com.example.focusreminder.platform.AppRestrictionRepository
import com.example.focusreminder.platform.ForegroundAppDetector
import com.example.focusreminder.platform.UsagePermissionHelper
import com.example.focusreminder.ui.blocker.BlockerActivity
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Foreground Service for continuous usage monitoring and focus enforcement (Prototype A).
 */
class UsageMonitorService : ComponentActivityService() {

    companion object {
        const val CHANNEL_ID = "focus_monitor_service_channel"
        const val BLOCKER_CHANNEL_ID = "focus_blocker_urgent_channel"
        const val NOTIFICATION_ID = 9001
        const val BLOCKER_NOTIFICATION_ID = 8888

        const val ACTION_START_MONITORING = "com.productivity.focusreminder.ACTION_START"
        const val ACTION_STOP_MONITORING = "com.productivity.focusreminder.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UsageMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null

    private lateinit var detector: ForegroundAppDetector
    private lateinit var repo: AppRestrictionRepository
    private val rollingCalculator = RollingWindowUsageCalculator()
    private val cooldownManager = CooldownManager()

    private var lastBlockedPackage: String? = null
    private var lastBlockedTimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        repo = AppRestrictionRepository.getInstance(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                startMonitoringLoop()
            }
        }
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = serviceScope.launch {
            while (isActive) {
                delay(1500L) // Poll every 1.5 seconds

                if (!UsagePermissionHelper.hasUsageAccessPermission(this@UsageMonitorService)) {
                    continue
                }

                val currentApp = detector.getForegroundPackageName() ?: continue

                // Don't monitor our own app
                if (currentApp == packageName) {
                    continue
                }

                val restrictedPackages = repo.getRestrictedPackages()

                // Check if foreground app is restricted
                if (restrictedPackages.contains(currentApp)) {
                    val now = System.currentTimeMillis()

                    // 1. Check if user currently has an emergency override active
                    if (repo.hasActiveEmergencyOverride(currentApp)) {
                        continue
                    }

                    val appLabel = repo.getAppName(currentApp, currentApp.substringAfterLast("."))
                    val midnightStart = getTodayMidnightMs()
                    val dailyBudgetMinutes = repo.getDailyLimitMinutes(currentApp)
                    val dailyBudgetMs = dailyBudgetMinutes * 60 * 1000L

                    // Accurate daily usage directly from OS UsageStatsManager
                    val dailyUsedMs = detector.getDailyUsageMs(currentApp)
                    val isDailyExhausted = dailyBudgetMs > 0L && dailyUsedMs >= dailyBudgetMs

                    // Calculate remaining seconds until midnight for daily lock
                    val nextMidnightMs = midnightStart + (24 * 60 * 60 * 1000L)
                    val secondsUntilMidnight = maxOf(60L, (nextMidnightMs - now) / 1000L)

                    // 2. STRICT DAILY LIMIT ENFORCEMENT
                    if (isDailyExhausted) {
                        // Lock app for the rest of the day until midnight
                        repo.lockApp(currentApp, appLabel, durationMinutes = (secondsUntilMidnight / 60).toInt())

                        if (currentApp != lastBlockedPackage || (now - lastBlockedTimeMs) >= 2000L) {
                            lastBlockedPackage = currentApp
                            lastBlockedTimeMs = now
                            triggerBlocker(currentApp, appLabel, isDailyLimit = true, cooldownSeconds = secondsUntilMidnight)
                        }
                        continue
                    }

                    // 3. Check if under direct Task Lock / Manual Cooldown
                    if (repo.isAppLocked(currentApp)) {
                        val remainingSecs = repo.getRemainingLockSeconds(currentApp)
                        if (currentApp != lastBlockedPackage || (now - lastBlockedTimeMs) >= 2000L) {
                            lastBlockedPackage = currentApp
                            lastBlockedTimeMs = now
                            triggerBlocker(currentApp, appLabel, isDailyLimit = false, cooldownSeconds = remainingSecs)
                        }
                        continue
                    }

                    // 4. Check rolling continuous session budget (last 60 minutes)
                    val rollingBudgetMinutes = repo.getRollingBudgetMinutes(currentApp)
                    val rollingCalculator = RollingWindowUsageCalculator(budgetMs = rollingBudgetMinutes * 60 * 1000L)
                    val windowStart = now - (60 * 60 * 1000L)
                    val intervals = detector.queryForegroundIntervals(currentApp, windowStart, now)

                    val rollingResult = rollingCalculator.evaluate(intervals, now)
                    if (rollingResult.isBudgetExhausted) {
                        // Automatically lock for rolling cooldown (30m)
                        repo.lockApp(currentApp, appLabel, durationMinutes = 30)
                        if (currentApp != lastBlockedPackage || (now - lastBlockedTimeMs) >= 2000L) {
                            lastBlockedPackage = currentApp
                            lastBlockedTimeMs = now
                            triggerBlocker(currentApp, appLabel, isDailyLimit = false, cooldownSeconds = 1800L)
                        }
                        continue
                    }
                }
            }
        }
    }

    private fun triggerBlocker(
        packageName: String,
        appName: String,
        isDailyLimit: Boolean,
        cooldownSeconds: Long
    ) {
        val blockerIntent = BlockerActivity.createIntent(
            context = this@UsageMonitorService,
            appName = appName,
            isDailyLimit = isDailyLimit,
            cooldownSeconds = cooldownSeconds
        )

        // If overlay permission is granted, start activity directly
        if (UsagePermissionHelper.hasOverlayPermission(this@UsageMonitorService)) {
            try {
                startActivity(blockerIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Post high-priority fullScreenIntent notification to ensure display across all Android versions
        showFullScreenBlockerNotification(appName, blockerIntent)
    }

    private fun showFullScreenBlockerNotification(appName: String, blockerIntent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            BLOCKER_NOTIFICATION_ID,
            blockerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BLOCKER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Focus Lock: $appName is Restricted")
            .setContentText("Focus session active. Step away or reflect.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BLOCKER_NOTIFICATION_ID, notification)
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun getTodayMidnightMs(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Enforcement Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors active app usage budgets and cooldown timers"
                setShowBadge(false)
            }

            val blockerChannel = NotificationChannel(
                BLOCKER_CHANNEL_ID,
                "Urgent Focus Restrictions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Presents the lock screen when a restricted app is opened"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(blockerChannel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Focus & Reminder Active")
            .setContentText("Monitoring rolling app usage budgets & reminders")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

/**
 * Base helper for Component Services.
 */
abstract class ComponentActivityService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
