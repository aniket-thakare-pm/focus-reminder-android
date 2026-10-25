package com.example.focusreminder.ui.blocker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

/**
 * Full-screen Neumorphic Intentional Blocker Screen (PRD Section 5.3).
 */
@Composable
fun BlockerScreen(
    restrictedAppName: String = "Restricted App",
    isDailyLimit: Boolean = false,
    remainingCooldownSeconds: Long = 1800L, // 30 mins
    onNavigateHome: () -> Unit,
    onStartEmergencyReflection: () -> Unit
) {
    var secondsLeft by remember { mutableLongStateOf(remainingCooldownSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            secondsLeft -= 1
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuBackground) // Clean porcelain background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / App Lock Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                // Sunken Lock Well
                NeuInsetWell(
                    modifier = Modifier.size(58.dp),
                    cornerRadius = 20.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SimpleLockIcon(
                            color = EmeraldDark,
                            modifier = Modifier.size(24.dp),
                            isLocked = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Intentional Pause",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                val reasonText = if (isDailyLimit) {
                    "Daily total usage limit reached for $restrictedAppName"
                } else {
                    "Rolling 60m budget reached for $restrictedAppName"
                }

                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                )
            }

            // Circular Sunken Cooldown Timer (Like Reference Image 1 & 2)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                NeuCircle(
                    modifier = Modifier.size(240.dp),
                    isInset = true
                ) {
                    // Background Indented Track
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        drawCircle(
                            color = NeuSunkenTrack,
                            radius = size.minDimension / 2.3f,
                            style = Stroke(width = 10.dp.toPx())
                        )
                        val progress = (secondsLeft.toFloat() / remainingCooldownSeconds.coerceAtLeast(1L)).coerceIn(0f, 1f)
                        drawArc(
                            color = EmeraldPrimary,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Timer Typography
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDailyLimit) "RESET AT MIDNIGHT" else "MINIMUM COOLDOWN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = EmeraldDark
                        )
                    }
                }
            }

            // Reflection Quote (Unboxed floating quote)
            Text(
                text = "\"Your attention is your life. Take three slow breaths and step away.\"",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                ),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            // Action Controls: Deliberate Friction, No Easy Bypass
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Return to Home
                NeuPill(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Leave App & Step Away",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }

                // Reflective Emergency Access (requires 5 written answers)
                NeuPill(
                    onClick = onStartEmergencyReflection,
                    backgroundColor = EmeraldPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Reflective Emergency Access →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
