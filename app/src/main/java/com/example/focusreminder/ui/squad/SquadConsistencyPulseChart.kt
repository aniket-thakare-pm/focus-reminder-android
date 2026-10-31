package com.example.focusreminder.ui.squad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import java.text.SimpleDateFormat
import java.util.*

/**
 * Infographic: 7-Day Rolling Consistency Pulse.
 * Evaluates whether each squad member successfully stayed within their OWN daily goal/budget
 * over the last 7 days, honoring habit consistency over pure incremental reduction.
 */
@Composable
fun SquadConsistencyPulseChart(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    // Sorted by streak descending (highest active consistency at the top)
    val rankedByStreak = members.sortedByDescending { it.streakDays }

    // Generate day initials for the rolling 7-day window ending today
    val dayLabels = rememberRollingDayLabels()

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
            // Card Title Header
            Column {
                Text(
                    text = "SQUAD CONSISTENCY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = "7-Day Consistency Pulse",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Consecutive days defending personal daily limits.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Table Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEMBER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1.1f)
                )

                Text(
                    text = "STREAK",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1.0f)
                )

                // 7 Day Initial Headers
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dayLabels.forEach { label ->
                        Box(
                            modifier = Modifier.size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // 5 Member Rows
            rankedByStreak.forEach { member ->
                ConsistencyMemberRow(
                    member = member
                )
            }
        }
    }
}

@Composable
private fun ConsistencyMemberRow(
    member: SquadMember
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Member Name & Individual Budget
        Column(
            modifier = Modifier.weight(1.1f)
        ) {
            Text(
                text = if (member.isCurrentUser) "${member.displayName} (You)" else member.displayName,
                fontSize = 12.sp,
                fontWeight = if (member.isCurrentUser) FontWeight.Black else FontWeight.Bold,
                color = if (member.isCurrentUser) EmeraldDark else TextPrimary
            )
            Text(
                text = "Target: ${member.dailyBudgetMinutes}m",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }

        // Column 2: Current Streak
        Column(
            modifier = Modifier.weight(1.0f)
        ) {
            Text(
                text = "${member.streakDays} DAYS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (member.streakDays > 0) EmeraldDark else TextMuted
            )
        }

        // Column 3: 7-Day Pulse Tiles
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val history = member.past7DaysGoalMet
            val paddedHistory = if (history.size >= 7) {
                history.takeLast(7)
            } else {
                List(7 - history.size) { false } + history
            }

            paddedHistory.forEach { goalMet ->
                if (goalMet) {
                    // Solid Clean Emerald Block
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = EmeraldDark,
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                } else {
                    // Recessed Sunken Block (Goal Missed)
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(
                                color = NeuSurfaceSunken,
                                shape = RoundedCornerShape(3.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFEF4444).copy(alpha = 0.4f),
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Returns the rolling day initials (e.g. ["M", "T", "W", "T", "F", "S", "S"])
 * for the last 7 days ending on today.
 */
@Composable
private fun rememberRollingDayLabels(): List<String> {
    val calendar = Calendar.getInstance()
    val format = SimpleDateFormat("E", Locale.getDefault())
    val labels = mutableListOf<String>()

    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val initial = format.format(cal.time).take(1).uppercase()
        labels.add(initial)
    }
    return labels
}
