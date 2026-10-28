package com.example.focusreminder.ui.squad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.focusreminder.util.formatHoursMinutes

/**
 * Infographic 3: 5-Friend Distracted vs. Total Time Horizon Chart.
 * Ranked strictly by monitored distraction time (least distraction at the top).
 * Displays full Total Screen Time with the Distraction segment visually contrasted.
 */
@Composable
fun DistractedVsTotalChart(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    // Ranked by monitored distraction minutes ascending (least distraction = #1)
    val ranked = members.sortedBy { it.monitoredMinutes }
    val maxTotalTime = (ranked.maxOfOrNull { it.totalScreenMinutes } ?: 1).coerceAtLeast(60)

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
            Column {
                Text(
                    text = "DISTRACTION CONTROL",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = "Distracted vs. Total Screen Time",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Ranked by lowest monitored time. Clean utility time is shown in slate.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            ranked.forEachIndexed { index, member ->
                DistractedVsTotalRow(
                    rank = index + 1,
                    member = member,
                    maxTotalTime = maxTotalTime
                )
            }
        }
    }
}

@Composable
private fun DistractedVsTotalRow(
    rank: Int,
    member: SquadMember,
    maxTotalTime: Int
) {
    val totalRatio = (member.totalScreenMinutes.toFloat() / maxTotalTime.toFloat()).coerceIn(0.15f, 1.0f)
    val distractedRatio = if (member.totalScreenMinutes > 0) {
        (member.monitoredMinutes.toFloat() / member.totalScreenMinutes.toFloat()).coerceIn(0.05f, 1.0f)
    } else {
        0.05f
    }

    val distractionColor = when {
        member.monitoredMinutes <= 30 -> EmeraldDark
        member.monitoredMinutes <= 60 -> Color(0xFFD97706)
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = "Distracted: ${formatHoursMinutes(member.monitoredMinutes)}",
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
                    text = "Total: ${formatHoursMinutes(member.totalScreenMinutes)}",
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
