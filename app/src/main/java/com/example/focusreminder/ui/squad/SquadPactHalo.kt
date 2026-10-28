package com.example.focusreminder.ui.squad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard

/**
 * Infographic 1: The Collective Squad Pact Halo.
 * Segmented 5-arc circular dial showing squad-wide discipline at a glance.
 */
@Composable
fun SquadPactHalo(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    val cleanCount = members.count { it.isWithinBudget }
    val isAllClean = cleanCount == members.size && members.isNotEmpty()

    NeuCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SQUAD PACT STATUS",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark,
                letterSpacing = 1.6.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val sunkenColor = NeuSurfaceSunken
            val emeraldColor = EmeraldDark
            val alertColor = Color(0xFFEF4444)

            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    val totalArcs = 5
                    val sweepAngle = 56f
                    val gapAngle = 16f
                    val startAngleBase = -90f

                    for (i in 0 until totalArcs) {
                        val member = members.getOrNull(i)
                        val startAngle = startAngleBase + i * (sweepAngle + gapAngle)

                        val arcColor = when {
                            member == null -> sunkenColor.copy(alpha = 0.4f)
                            member.isWithinBudget -> emeraldColor
                            else -> alertColor
                        }

                        // Background track
                        drawArc(
                            color = sunkenColor.copy(alpha = 0.5f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Active arc
                        if (member != null) {
                            drawArc(
                                color = arcColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // Center Label inside the Halo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isAllClean) "ALL CLEAN" else "$cleanCount/${members.size} CLEAN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isAllClean) EmeraldDark else Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isAllClean) "PACT INTACT" else "ATTENTION RISK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Collective discipline streak: 4 consecutive clean days",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
