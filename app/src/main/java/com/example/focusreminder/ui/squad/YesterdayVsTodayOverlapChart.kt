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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.util.formatHoursMinutes

/**
 * Infographic 4: Yesterday vs. Today Overlap Bar Chart.
 * Direct visual correlation:
 * - Low-opacity ghost bar represents yesterday's baseline.
 * - Solid foreground bar represents today's live distraction.
 * - Shorter solid bar than ghost bar = direct visual proof of self-improvement!
 */
@Composable
fun YesterdayVsTodayOverlapChart(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    val maxBarTime = members.flatMap { listOf(it.monitoredMinutes, it.yesterdayMonitoredMinutes) }
        .maxOrNull()?.coerceAtLeast(60) ?: 60

    NeuCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "SELF-MASTERY OVERLAP",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = "Today vs. Yesterday Baseline",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Solid bar is today. Translucent ghost bar is yesterday.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            members.forEach { member ->
                OverlapBarRow(
                    member = member,
                    maxBarTime = maxBarTime
                )
            }
        }
    }
}

@Composable
private fun OverlapBarRow(
    member: SquadMember,
    maxBarTime: Int
) {
    val todayRatio = (member.monitoredMinutes.toFloat() / maxBarTime.toFloat()).coerceIn(0.04f, 1f)
    val yesterdayRatio = (member.yesterdayMonitoredMinutes.toFloat() / maxBarTime.toFloat()).coerceIn(0.04f, 1f)
    val isImproved = member.deltaMinutes >= 0

    val accentColor = if (isImproved) EmeraldDark else Color(0xFFEF4444)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (member.isCurrentUser) "${member.displayName} (You)" else member.displayName,
                fontSize = 12.sp,
                fontWeight = if (member.isCurrentUser) FontWeight.Black else FontWeight.Bold,
                color = if (member.isCurrentUser) EmeraldDark else TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Today: ${formatHoursMinutes(member.monitoredMinutes)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isImproved) "-${member.deltaMinutes}m" else "+${-member.deltaMinutes}m",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
            }
        }

        // Overlap Visual Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeuSurfaceSunken, RoundedCornerShape(10.dp))
            )

            // Background Ghost Bar (Yesterday) - Low Opacity
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = yesterdayRatio)
                    .height(20.dp)
                    .background(
                        color = TextMuted.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp)
                    )
            )

            // Foreground Solid Bar (Today)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = todayRatio)
                    .height(10.dp)
                    .padding(start = 2.dp)
                    .background(
                        color = accentColor,
                        shape = RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}
