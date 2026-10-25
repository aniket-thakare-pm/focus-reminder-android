package com.example.focusreminder.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focusreminder.MainActivity
import com.example.focusreminder.R
import com.example.focusreminder.domain.engine.CooldownManager
import com.example.focusreminder.domain.engine.RollingWindowUsageCalculator
import com.example.focusreminder.platform.AppRestrictionRepository
import com.example.focusreminder.platform.ForegroundAppDetector
import com.example.focusreminder.platform.UsagePermissionHelper
import com.example.focusreminder.platform.analytics.AnalyticsRepository
import com.example.focusreminder.ui.blocker.BlockerActivity
import com.example.focusreminder.ui.session.SessionContractActivity
import kotlinx.coroutines.*
import java.util.Calendar

/**
 * Foreground Service for continuous usage monitoring, active session chronometer, and focus enforcement.
 */
class UsageMonitorService : ComponentActivityService() {

    companion object {
        const val CHANNEL_ID = "focus_monitor_service_channel"
        const val BLOCKER_CHANNEL_ID = "focus_blocker_urgent_channel"
        const val CHRONOMETER_CHANNEL_ID = "focus_sticky_timer_channel_v3"
        const val WARNING_CHANNEL_ID = "focus_session_warning_channel"

        const val NOTIFICATION_ID = 9001
        const val BLOCKER_NOTIFICATION_ID = 8888
        const val CHRONOMETER_NOTIFICATION_ID = 7777
        const val WARNING_NOTIFICATION_ID = 6666

        const val ACTION_START_MONITORING = "com.productivity.focusreminder.ACTION_START"
        const val ACTION_STOP_MONITORING = "com.productivity.focusreminder.ACTION_STOP"
        const val ACTION_START_SESSION = "com.productivity.focusreminder.ACTION_START_SESSION"

        const val EXTRA_SESSION_PKG = "extra_session_pkg"
        const val EXTRA_SESSION_NAME = "extra_session_name"
        const val EXTRA_SESSION_MINS = "extra_session_mins"
        const val EXTRA_COOLDOWN_MINS = "extra_cooldown_mins"

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

        fun startActiveSession(
            context: Context,
            packageName: String,
            appName: String,
            sessionMinutes: Int,
            cooldownMinutes: Int
        ) {
            val intent = Intent(context, UsageMonitorService::class.java).apply {
                action = ACTION_START_SESSION
                putExtra(EXTRA_SESSION_PKG, packageName)
                putExtra(EXTRA_SESSION_NAME, appName)
                putExtra(EXTRA_SESSION_MINS, sessionMinutes)
                putExtra(EXTRA_COOLDOWN_MINS, cooldownMinutes)
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
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null

    private lateinit var detector: ForegroundAppDetector
    private lateinit var repo: AppRestrictionRepository
    private lateinit var analyticsRepo: AnalyticsRepository

    private var lastBlockedPackage: String? = null
    private var lastBlockedTimeMs: Long = 0L

    // Active Focus Session State
    private var activeSessionPackage: String? = null
    private var activeSessionAppName: String? = null
    private var activeSessionEndTimeMs: Long = 0L
    private var activeSessionStartMs: Long = 0L
    private var activeCooldownMinutes: Int = 30
    private var remainingSessionMs: Long = 0L
    private var isSessionPaused: Boolean = false
    private var sessionPausedAtMs: Long = 0L
    private var hasFiredTwoMinWarning: Boolean = false
    private var lastSeenActiveTimeMs: Long = 0L

    private var lastContractPackage: String? = null
    private var lastContractTimeMs: Long = 0L

    // Continuous Real-Time App Session Tracking
    private var trackedForegroundPkg: String? = null
    private var trackedForegroundStartMs: Long = 0L
    private var trackedForegroundAppName: String? = null

    override fun onCreate() {
        super.onCreate()
        detector = ForegroundAppDetector(this)
        repo = AppRestrictionRepository.getInstance(this)
        analyticsRepo = AnalyticsRepository.getInstance(this)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
                stopSelf()
            }
            ACTION_START_SESSION -> {
                val pkg = intent.getStringExtra(EXTRA_SESSION_PKG)
                val name = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "App"
                val sessionMins = intent.getIntExtra(EXTRA_SESSION_MINS, 10)
                val cooldownMins = intent.getIntExtra(EXTRA_COOLDOWN_MINS, 30)

                if (pkg != null) {
                    val now = System.currentTimeMillis()
                    activeSessionPackage = pkg
                    activeSessionAppName = name
                    activeSessionStartMs = now
                    remainingSessionMs = sessionMins * 60 * 1000L
                    activeSessionEndTimeMs = now + remainingSessionMs
                    activeCooldownMinutes = cooldownMins
                    isSessionPaused = false
                    sessionPausedAtMs = 0L
                    hasFiredTwoMinWarning = false
                    lastSeenActiveTimeMs = now

                    showLiveChronometerNotification(name, activeSessionEndTimeMs, remainingSessionMs)
                }

                startForeground(NOTIFICATION_ID, buildForegroundNotification())
                startMonitoringLoop()
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
                delay(1200L) // Poll every 1.2 seconds for snappy response

                if (!UsagePermissionHelper.hasUsageAccessPermission(this@UsageMonitorService)) {
                    continue
                }

                val currentApp = detector.getForegroundPackageName()
                val now = System.currentTimeMillis()

                // Continuous background session logger across ALL user applications:
                // Immediately commits completed sessions to SQLite upon app switch or screen-off,
                // making data 100% immune to Android RAM circular buffer eviction.
                val activeForLogging = if (currentApp != null && currentApp != packageName && !analyticsRepo.isIgnoredSystemPackage(currentApp)) {
                    currentApp
                } else {
                    null
                }

                if (activeForLogging != trackedForegroundPkg) {
                    val prevPkg = trackedForegroundPkg
                    if (prevPkg != null) {
                        val duration = now - trackedForegroundStartMs
                        if (duration >= 2000L) {
                            val appLabel = trackedForegroundAppName ?: analyticsRepo.getAppName(prevPkg)
                            analyticsRepo.recordSession(prevPkg, appLabel, trackedForegroundStartMs, now, duration)
                        }
                    }

                    if (activeForLogging != null) {
                        trackedForegroundPkg = activeForLogging
                        trackedForegroundStartMs = now
                        trackedForegroundAppName = analyticsRepo.getAppName(activeForLogging)
                    } else {
                        trackedForegroundPkg = null
                        trackedForegroundStartMs = 0L
                        trackedForegroundAppName = null
                    }
                }

                // If phone is locked, screen is off, or user is in our app, skip enforcement
                if (currentApp == null || currentApp == packageName) {
                    continue
                }

                val restrictedPackages = repo.getRestrictedPackages()

                // Check active session status
                val activePkg = activeSessionPackage
                if (activePkg != null) {
                    if (currentApp == activePkg) {
                        lastSeenActiveTimeMs = now

                        // If returning from temporary pause, resume session timer!
                        if (isSessionPaused) {
                            isSessionPaused = false
                            activeSessionEndTimeMs = now + remainingSessionMs
                        } else {
                            remainingSessionMs = maxOf(0L, activeSessionEndTimeMs - now)
                        }

                        // Continuously maintain the sticky notification with live time while in the app!
                        showLiveChronometerNotification(activeSessionAppName ?: "App", activeSessionEndTimeMs, remainingSessionMs)

                        // Check 2-Minute Warning Alert
                        if (!hasFiredTwoMinWarning && remainingSessionMs <= (2 * 60 * 1000L) && remainingSessionMs > 0L) {
                            hasFiredTwoMinWarning = true
                            showTwoMinuteWarningNotification(activeSessionAppName ?: "App")
                        }

                        // Check Session Expiration (Hard Stop!)
                        if (now >= activeSessionEndTimeMs || remainingSessionMs <= 0L) {
                            val appLabel = activeSessionAppName ?: activePkg.substringAfterLast(".")
                            val cooldownMin = activeCooldownMinutes

                            // Record completed session in analytics database
                            if (trackedForegroundPkg == activePkg) {
                                val duration = now - trackedForegroundStartMs
                                if (duration >= 1000L) {
                                    analyticsRepo.recordSession(activePkg, appLabel, trackedForegroundStartMs, now, duration)
                                }
                                trackedForegroundPkg = null
                                trackedForegroundStartMs = 0L
                                trackedForegroundAppName = null
                            } else {
                                val duration = now - activeSessionStartMs
                                if (duration >= 1000L) {
                                    analyticsRepo.recordSession(activePkg, appLabel, activeSessionStartMs, now, duration)
                                }
                            }

                            // Enforce cooldown lock
                            repo.lockApp(activePkg, appLabel, durationMinutes = cooldownMin)

                            // Clear active session immediately
                            activeSessionPackage = null
                            remainingSessionMs = 0L
                            isSessionPaused = false
                            dismissChronometerNotification()

                            lastBlockedPackage = activePkg
                            lastBlockedTimeMs = now
                            triggerBlocker(activePkg, appLabel, isDailyLimit = false, cooldownSeconds = cooldownMin * 60L)
                            continue
                        }
                    } else {
                        // User closed or switched away from the restricted app!
                        if (!isSessionPaused) {
                            // Pause the session immediately and DISMISS the timer notification from the notification bar!
                            isSessionPaused = true
                            sessionPausedAtMs = now
                            remainingSessionMs = maxOf(0L, activeSessionEndTimeMs - now)
                            dismissChronometerNotification()
                        } else {
                            // Already paused: check 5-minute session continuity grace period
                            if ((now - sessionPausedAtMs) > (5 * 60 * 1000L)) {
                                // User stayed away for > 5 mins: cleanly expire the session
                                val appLabel = activeSessionAppName ?: activePkg.substringAfterLast(".")
                                val duration = sessionPausedAtMs - activeSessionStartMs
                                if (duration >= 1000L) {
                                    analyticsRepo.recordSession(activePkg, appLabel, activeSessionStartMs, sessionPausedAtMs, duration)
                                }
                                activeSessionPackage = null
                                remainingSessionMs = 0L
                                isSessionPaused = false
                                dismissChronometerNotification()
                            }
                        }
                    }
                }

                // Check if newly foregrounded app is restricted
                if (restrictedPackages.contains(currentApp)) {
                    // 1. Check emergency override
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

                    val nextMidnightMs = midnightStart + (24 * 60 * 60 * 1000L)
                    val secondsUntilMidnight = maxOf(60L, (nextMidnightMs - now) / 1000L)

                    // 2. DAILY LIMIT ENFORCEMENT
                    if (isDailyExhausted) {
                        repo.lockApp(currentApp, appLabel, durationMinutes = (secondsUntilMidnight / 60).toInt())
                        if (currentApp != lastBlockedPackage || (now - lastBlockedTimeMs) >= 2000L) {
                            lastBlockedPackage = currentApp
                            lastBlockedTimeMs = now
                            triggerBlocker(currentApp, appLabel, isDailyLimit = true, cooldownSeconds = secondsUntilMidnight)
                        }
                        continue
                    }

                    // 3. TASK LOCK / MANDATORY COOLDOWN ENFORCEMENT
                    if (repo.isAppLocked(currentApp)) {
                        val remainingSecs = repo.getRemainingLockSeconds(currentApp)
                        if (currentApp != lastBlockedPackage || (now - lastBlockedTimeMs) >= 2000L) {
                            lastBlockedPackage = currentApp
                            lastBlockedTimeMs = now
                            triggerBlocker(currentApp, appLabel, isDailyLimit = false, cooldownSeconds = remainingSecs)
                        }
                        continue
                    }

                    // 4. SESSION ENTRY CONTRACT: If not in active session, require user to slide to enter!
                    if (activeSessionPackage != currentApp) {
                        val sessionBudget = repo.getRollingBudgetMinutes(currentApp)
                        val cooldown = repo.getCooldownMinutes(currentApp)

                        // Calculate remaining time in trailing window (sessionBudget + cooldown)
                        val windowMs = (sessionBudget + cooldown) * 60 * 1000L
                        val windowStart = now - windowMs
                        val intervals = detector.queryForegroundIntervals(currentApp, windowStart, now)
                        val usedInWindowMs = intervals.sumOf { it.overlapMs(windowStart, now) }
                        val remainingMinutes = maxOf(1, ((sessionBudget * 60 * 1000L - usedInWindowMs) / (60 * 1000L)).toInt())

                        // Debounce contract presentation
                        if (currentApp != lastContractPackage || (now - lastContractTimeMs) >= 4000L) {
                            lastContractPackage = currentApp
                            lastContractTimeMs = now

                            val contractIntent = SessionContractActivity.createIntent(
                                context = this@UsageMonitorService,
                                packageName = currentApp,
                                appName = appLabel,
                                sessionMinutes = remainingMinutes,
                                cooldownMinutes = cooldown
                            )

                            if (UsagePermissionHelper.hasOverlayPermission(this@UsageMonitorService)) {
                                try {
                                    startActivity(contractIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            showFullScreenContractNotification(appLabel, contractIntent)
                        }
                    }
                }
            }
        }
    }

    private fun showLiveChronometerNotification(appName: String, endTimeMs: Long, remainingMs: Long) {
        val launchIntent = packageManager.getLaunchIntentForPackage(activeSessionPackage ?: "")
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                777,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val totalSecs = maxOf(0L, remainingMs / 1000L)
        val mins = totalSecs / 60L
        val secs = totalSecs % 60L
        val timeStr = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

        val builder = NotificationCompat.Builder(this, CHRONOMETER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(appName)
            .setContentText("$timeStr remaining")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(android.graphics.Color.parseColor("#4F46E5")) // Vibrant Indigo/Purple
            .setContentIntent(contentPendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(CHRONOMETER_NOTIFICATION_ID, builder.build())
    }

    private fun showTwoMinuteWarningNotification(appName: String) {
        val builder = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Warning: 2 Minutes Remaining ($appName)")
            .setContentText("Wrap up your activity. $appName will lock in 2 minutes for cooldown.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_SOUND)
            .setAutoCancel(true)
            .setColor(android.graphics.Color.parseColor("#D97706")) // Amber

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(WARNING_NOTIFICATION_ID, builder.build())
    }

    private fun dismissChronometerNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(CHRONOMETER_NOTIFICATION_ID)
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

        if (UsagePermissionHelper.hasOverlayPermission(this@UsageMonitorService)) {
            try {
                startActivity(blockerIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
            .setContentText("Continuous budget exhausted. Step away or reflect.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BLOCKER_NOTIFICATION_ID, notification)
    }

    private fun showFullScreenContractNotification(appName: String, contractIntent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            9999,
            contractIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BLOCKER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Session Contract: $appName")
            .setContentText("Slide to acknowledge focus window before entry.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(9999, notification)
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        dismissChronometerNotification()
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
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
                description = "Presents lock and session contracts when restricted apps are launched"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            try {
                manager.deleteNotificationChannel("focus_session_chronometer_channel")
                manager.deleteNotificationChannel("focus_session_chronometer_channel_v2")
            } catch (e: Exception) {
                // Ignore
            }

            val chronometerChannel = NotificationChannel(
                CHRONOMETER_CHANNEL_ID,
                "Active Focus Session Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows continuous countdown timer during active focus sessions"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val warningChannel = NotificationChannel(
                WARNING_CHANNEL_ID,
                "Session Expiry Warning",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up warning 2 minutes before app session budget ends"
                enableVibration(true)
                setShowBadge(true)
            }

            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(blockerChannel)
            manager.createNotificationChannel(chronometerChannel)
            manager.createNotificationChannel(warningChannel)
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
        dismissChronometerNotification()
        super.onDestroy()
    }
}

/**
 * Base helper for Component Services.
 */
abstract class ComponentActivityService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
