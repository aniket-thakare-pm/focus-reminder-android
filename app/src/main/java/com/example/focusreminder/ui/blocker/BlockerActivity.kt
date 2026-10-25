package com.example.focusreminder.ui.blocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.focusreminder.theme.FocusReminderTheme

class BlockerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_IS_DAILY_LIMIT = "extra_is_daily_limit"
        const val EXTRA_COOLDOWN_SECONDS = "extra_cooldown_seconds"
        const val EXTRA_LOCK_REASON = "extra_lock_reason"
        const val EXTRA_GOAL_NAME = "extra_goal_name"

        fun createIntent(
            context: Context,
            appName: String,
            isDailyLimit: Boolean = false,
            cooldownSeconds: Long = 1800L,
            lockReason: String? = null,
            goalName: String? = null
        ): Intent {
            return Intent(context, BlockerActivity::class.java).apply {
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_IS_DAILY_LIMIT, isDailyLimit)
                putExtra(EXTRA_COOLDOWN_SECONDS, cooldownSeconds)
                putExtra(EXTRA_LOCK_REASON, lockReason)
                putExtra(EXTRA_GOAL_NAME, goalName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
        }
    }

    private var appNameState by mutableStateOf("Selected Application")
    private var isDailyLimitState by mutableStateOf(false)
    private var cooldownSecondsState by mutableLongStateOf(1800L)
    private var lockReasonState by mutableStateOf<String?>(null)
    private var goalNameState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateFromIntent(intent)

        setContent {
            FocusReminderTheme {
                var isReflecting by remember { mutableStateOf(false) }

                if (isReflecting) {
                    ReflectiveAccessScreen(
                        appName = appNameState,
                        onCancel = { isReflecting = false },
                        onGrantOverride = { answers, requestedMinutes ->
                            // Record answers locally and grant temporary override in repository
                            val repo = com.example.focusreminder.platform.AppRestrictionRepository.getInstance(this@BlockerActivity)
                            repo.grantEmergencyOverride(appNameState, requestedMinutes)

                            val prefs = getSharedPreferences("emergency_override", Context.MODE_PRIVATE)
                            val expiryMs = System.currentTimeMillis() + (requestedMinutes * 60 * 1000L)
                            prefs.edit()
                                .putLong("override_expiry_ms", expiryMs)
                                .putInt("override_minutes", requestedMinutes)
                                .apply()

                            Toast.makeText(
                                this@BlockerActivity,
                                "Access granted for $requestedMinutes minutes. Stay focused on your stated goal.",
                                Toast.LENGTH_LONG
                            ).show()

                            finish()
                        }
                    )
                } else {
                    BlockerScreen(
                        restrictedAppName = appNameState,
                        isDailyLimit = isDailyLimitState,
                        remainingCooldownSeconds = cooldownSecondsState,
                        lockReason = lockReasonState,
                        goalName = goalNameState,
                        onNavigateHome = {
                            // Navigate to Android home launcher
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(homeIntent)
                            finish()
                        },
                        onStartEmergencyReflection = {
                            isReflecting = true
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent?) {
        intent?.let {
            appNameState = it.getStringExtra(EXTRA_APP_NAME) ?: "Selected Application"
            isDailyLimitState = it.getBooleanExtra(EXTRA_IS_DAILY_LIMIT, false)
            cooldownSecondsState = it.getLongExtra(EXTRA_COOLDOWN_SECONDS, 1800L)
            lockReasonState = it.getStringExtra(EXTRA_LOCK_REASON)
            goalNameState = it.getStringExtra(EXTRA_GOAL_NAME)
        }
    }

    // Prevent bypassing via Android back button
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
