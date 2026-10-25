package com.example.focusreminder.ui.session

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.focusreminder.service.UsageMonitorService
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

class SessionContractActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_SESSION_MINUTES = "extra_session_minutes"
        const val EXTRA_COOLDOWN_MINUTES = "extra_cooldown_minutes"

        fun createIntent(
            context: Context,
            packageName: String,
            appName: String,
            sessionMinutes: Int,
            cooldownMinutes: Int
        ): Intent {
            return Intent(context, SessionContractActivity::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_SESSION_MINUTES, sessionMinutes)
                putExtra(EXTRA_COOLDOWN_MINUTES, cooldownMinutes)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen and display above keyguard if needed
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

        val targetPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: "com.instagram.android"
        val targetName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Application"
        val sessionMin = intent.getIntExtra(EXTRA_SESSION_MINUTES, 10)
        val cooldownMin = intent.getIntExtra(EXTRA_COOLDOWN_MINUTES, 30)

        val appIcon = try {
            packageManager.getApplicationIcon(targetPkg)
        } catch (_: Exception) {
            null
        }

        setContent {
            FocusReminderTheme {
                SessionContractScreen(
                    packageName = targetPkg,
                    appName = targetName,
                    appIcon = appIcon,
                    sessionMinutes = sessionMin,
                    cooldownMinutes = cooldownMin,
                    onAccept = {
                        UsageMonitorService.startActiveSession(
                            context = this@SessionContractActivity,
                            packageName = targetPkg,
                            appName = targetName,
                            sessionMinutes = sessionMin,
                            cooldownMinutes = cooldownMin
                        )
                        finish()
                    },
                    onCancel = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SessionContractScreen(
    packageName: String,
    appName: String,
    appIcon: Drawable?,
    sessionMinutes: Int,
    cooldownMinutes: Int,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    val bitmap = remember(appIcon) {
        try {
            appIcon?.toBitmap(width = 120, height = 120)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    BioluminescentBackground(showGrid = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Tag & App Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                NeuInsetWell(cornerRadius = 10.dp) {
                    Text(
                        text = "INTENTIONAL FOCUS CONTRACT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // App Icon in Sunken Inset
                NeuInsetWell(
                    modifier = Modifier.size(76.dp),
                    cornerRadius = 24.dp
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = appName,
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(appName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = EmeraldDark)
                        }
                    }
                }

                Text(
                    text = appName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            // Central Card: Rules of Engagement
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                elevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$sessionMinutes MINUTES ALLOWED",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = EmeraldDark
                    )

                    Text(
                        text = "Once you slide to enter, your live chronometer timer will tick in the status bar. When time is up, $appName will immediately lock for a $cooldownMinutes-minute mandatory cooldown.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), thickness = 0.8.dp)

                    // 3 Key Contract Checkpoints (Pure Vector Icons, Zero Emojis)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ContractCheckpointRow(
                            iconComposable = { ClockFaceIcon(color = EmeraldDark, modifier = Modifier.size(16.dp)) },
                            title = "Live Status Bar Timer",
                            subtitle = "Continuous chronometer shows exact remaining seconds"
                        )
                        ContractCheckpointRow(
                            iconComposable = { WarningTriangleIcon(color = WarningAmber, modifier = Modifier.size(16.dp)) },
                            title = "2-Minute Warning",
                            subtitle = "Heads-up notification buzzes before lock"
                        )
                        ContractCheckpointRow(
                            iconComposable = { SimpleLockIcon(color = ErrorRed, modifier = Modifier.size(16.dp), isLocked = true) },
                            title = "${cooldownMinutes}m Cooldown Lock",
                            subtitle = "Guaranteed blocker takeover when timer hits 00:00"
                        )
                    }
                }
            }

            // Bottom Actions: Slider & Exit
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tactile Slide to Acknowledge with Haptics
                NeuSlideToAcknowledge(
                    onAcknowledge = onAccept,
                    label = "Slide to Understand",
                    height = 64.dp
                )

                // Exit Button
                NeuInsetWell(
                    cornerRadius = 14.dp,
                    onClick = onCancel
                ) {
                    Text(
                        text = "Cancel & Return Home",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContractCheckpointRow(
    iconComposable: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeuInsetWell(
            modifier = Modifier.size(36.dp),
            cornerRadius = 10.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                iconComposable()
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}
