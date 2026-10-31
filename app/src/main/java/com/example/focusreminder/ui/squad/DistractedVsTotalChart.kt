package com.example.focusreminder.ui.squad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuPill
import com.example.focusreminder.util.formatHoursMinutes

/**
 * Infographic 3: 5-Friend Distracted vs. Total Time Horizon Chart.
 * Ranked strictly by monitored distraction time (least distraction at the top).
 *
 * Interactions:
 * 1. Top-Right Pill Tap: Toggles between "Today" and "This Week".
 * 2. Daily Streaks: Displayed inline on every member row.
 */
@Composable
fun DistractedVsTotalChart(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    var isWeekly by remember { mutableStateOf(false) }

    // Ranked by monitored distraction minutes ascending (least distraction = #1)
    val ranked = remember(members, isWeekly) {
        members.sortedBy { if (isWeekly) it.weeklyMonitoredMinutes else it.monitoredMinutes }
    }

    val maxTotalTime = remember(ranked, isWeekly) {
        val maxVal = if (isWeekly) {
            ranked.maxOfOrNull { it.weeklyTotalMinutes } ?: 1
        } else {
            ranked.maxOfOrNull { it.totalScreenMinutes } ?: 1
        }
        maxVal.coerceAtLeast(60)
    }

    NeuCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DISTRACTION CONTROL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = if (isWeekly) "Weekly Distracted vs. Total" else "Daily Distracted vs. Total",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Ranked by lowest monitored time. Clean utility in slate.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Single-Tap Toggle Pill: Today <-> This Week
                NeuPill(
                    onClick = { isWeekly = !isWeekly },
                    cornerRadius = 10.dp,
                    elevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isWeekly) "This Week" else "Today",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            ranked.forEachIndexed { index, member ->
                DistractedVsTotalRow(
                    rank = index + 1,
                    member = member,
                    maxTotalTime = maxTotalTime,
                    isWeekly = isWeekly
                )
            }
        }
    }
}

@Composable
private fun DistractedVsTotalRow(
    rank: Int,
    member: SquadMember,
    maxTotalTime: Int,
    isWeekly: Boolean
) {
    val totalMin = if (isWeekly) member.weeklyTotalMinutes else member.totalScreenMinutes
    val monitoredMin = if (isWeekly) member.weeklyMonitoredMinutes else member.monitoredMinutes

    val totalRatio = (totalMin.toFloat() / maxTotalTime.toFloat()).coerceIn(0.15f, 1.0f)
    val distractedRatio = if (totalMin > 0) {
        (monitoredMin.toFloat() / totalMin.toFloat()).coerceIn(0.05f, 1.0f)
    } else {
        0.05f
    }

    val distractionThresholdLow = if (isWeekly) 180 else 30
    val distractionThresholdMid = if (isWeekly) 360 else 60

    val distractionColor = when {
        monitoredMin <= distractionThresholdLow -> EmeraldDark
        monitoredMin <= distractionThresholdMid -> Color(0xFFD97706)
        else -> Color(0xFFEF4444)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "#$rank",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (rank == 1) EmeraldDark else TextMuted
                )
                Text(
                    text = if (member.isCurrentUser) "${member.displayName} (You)" else member.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (member.isCurrentUser) FontWeight.Black else FontWeight.Bold,
                    color = if (member.isCurrentUser) EmeraldDark else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Distracted: ${formatHoursMinutes(monitoredMin)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = distractionColor
                )
                Text(
                    text = "•",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "Total: ${formatHoursMinutes(totalMin)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }

        // Dual-Segmented Horizontal Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(NeuSurfaceSunken, RoundedCornerShape(7.dp))
        ) {
            // Full width proportional to total screen time
            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction = totalRatio)
                    .fillMaxHeight()
                    .background(TextMuted.copy(alpha = 0.25f), RoundedCornerShape(7.dp))
            ) {
                // Left segment: Distracted portion
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = distractedRatio)
                        .fillMaxHeight()
                        .background(distractionColor, RoundedCornerShape(7.dp))
                )
            }
        }
    }
}
