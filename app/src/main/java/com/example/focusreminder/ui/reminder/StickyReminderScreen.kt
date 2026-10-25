package com.example.focusreminder.ui.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

/**
 * P0 Sticky High-Attention Reminder (Pure Neumorphism / Soft UI).
 * Featuring centered typography, embossed Neumorphic timer, and slide-to-acknowledge with haptics.
 */
@Composable
fun StickyReminderScreen(
    title: String = "Prepare Sprint Demo Deck",
    description: String? = "Review the latest architecture slides and compile test metrics for the engineering sync.",
    scheduledTimeFormatted: String = "09:45 AM",
    onAcknowledge: () -> Unit,
    onSnooze: (snoozeMinutes: Int) -> Unit
) {
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Center-Aligned Neumorphic Status Badge
            NeuPill(
                modifier = Modifier.padding(top = 4.dp),
                cornerRadius = 16.dp,
                elevation = 4.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(EmeraldPrimary, CircleShape)
                    )
                    Text(
                        text = "P0 CRITICAL ALERT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldDark,
                        letterSpacing = 1.6.sp
                    )
                }
            }

            // MIDDLE: Big Thick Centered Title, Clean One-Sentence Description, and Embossed Time Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Authoritative, Big & Bold Title (Thick Font, Center Aligned)
                Text(
                    text = title,
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-1.2).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Clean single-sentence description (Center Aligned)
                if (!description.isNullOrBlank()) {
                    Text(
                        text = description,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tactile Embossed Neumorphic Timer Card (Dual Gaussian Shadows -5px -5px 15px / 5px 5px 15px)
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SCHEDULED FOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = EmeraldDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scheduledTimeFormatted,
                            fontSize = 52.sp,
                            lineHeight = 56.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            letterSpacing = (-1).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // BOTTOM: Slide-to-Acknowledge Slider & Snooze Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Slide-to-Acknowledge with progressive green fill and haptic vibration feedback
                NeuSlideToAcknowledge(
                    onAcknowledge = onAcknowledge,
                    modifier = Modifier.fillMaxWidth(),
                    height = 64.dp,
                    label = "Slide to Acknowledge"
                )

                // Secondary Actions: Neumorphic Soft Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeuPill(
                        onClick = { onSnooze(5) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Snooze 5m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    NeuPill(
                        onClick = { onSnooze(15) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Snooze 15m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
