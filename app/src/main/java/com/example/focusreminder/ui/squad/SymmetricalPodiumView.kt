package com.example.focusreminder.ui.squad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.SquadMember
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuInsetWell

/**
 * Infographic 2: The Symmetrical 5-Peak Mountain Podium [4, 2, 1, 3, 5].
 * Ranks all 5 squad members based on Self-Improvement Delta vs yesterday.
 * #1 stands tall at the center peak, flanked symmetrically by runners-up.
 */
@Composable
fun SymmetricalPodiumView(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    // Rank members by deltaMinutes descending (highest improvement = #1)
    val ranked = members.sortedByDescending { it.deltaMinutes }

    val member1 = ranked.getOrNull(0)
    val member2 = ranked.getOrNull(1)
    val member3 = ranked.getOrNull(2)
    val member4 = ranked.getOrNull(3)
    val member5 = ranked.getOrNull(4)

    NeuCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "IMPROVEMENT LADDER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = "Daily Self-Mastery Delta",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Vs. Yesterday",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Symmetrical 5-Peak Mountain Structure: [ #4, #2, #1, #3, #5 ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Pos 1 (Left Wing): Rank #4
                PodiumPedestal(
                    member = member4,
                    rank = 4,
                    height = 100.dp,
                    modifier = Modifier.weight(1f)
                )

                // Pos 2 (Inner Left): Rank #2
                PodiumPedestal(
                    member = member2,
                    rank = 2,
                    height = 135.dp,
                    modifier = Modifier.weight(1f)
                )

                // Pos 3 (Center Peak): Rank #1
                PodiumPedestal(
                    member = member1,
                    rank = 1,
                    height = 175.dp,
                    isLeader = true,
                    modifier = Modifier.weight(1.15f)
                )

                // Pos 4 (Inner Right): Rank #3
                PodiumPedestal(
                    member = member3,
                    rank = 3,
                    height = 120.dp,
                    modifier = Modifier.weight(1f)
                )

                // Pos 5 (Right Wing): Rank #5
                PodiumPedestal(
                    member = member5,
                    rank = 5,
                    height = 85.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PodiumPedestal(
    member: SquadMember?,
    rank: Int,
    height: Dp,
    isLeader: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (member == null) {
        Box(modifier = modifier.height(height))
        return
    }

    val isPositive = member.deltaMinutes >= 0
    val deltaColor = if (isPositive) EmeraldDark else Color(0xFFEF4444)
    val deltaText = if (isPositive) "+${member.deltaMinutes}m" else "${member.deltaMinutes}m"

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Avatar Inset Well
        NeuInsetWell(
            modifier = Modifier.size(if (isLeader) 40.dp else 34.dp),
            cornerRadius = 14.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.avatarInitials,
                    fontSize = if (isLeader) 13.sp else 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (member.isCurrentUser) EmeraldDark else TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Name
        Text(
            text = if (member.isCurrentUser) "You" else member.displayName,
            fontSize = 10.sp,
            fontWeight = if (member.isCurrentUser) FontWeight.Black else FontWeight.Bold,
            color = if (member.isCurrentUser) EmeraldDark else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Delta Badge
        Text(
            text = deltaText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = deltaColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Pedestal Pillar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    color = if (isLeader) EmeraldDark.copy(alpha = 0.12f) else NeuSurfaceSunken,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Rank Circle
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (isLeader) EmeraldDark else TextMuted.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLeader) Color.White else TextPrimary
                    )
                }

                // Positional Movement Indicator (Chevron)
                val moveText = when {
                    member.rankMovement > 0 -> "▲ ${member.rankMovement}"
                    member.rankMovement < 0 -> "▼ ${-member.rankMovement}"
                    else -> "="
                }
                val moveColor = when {
                    member.rankMovement > 0 -> EmeraldDark
                    member.rankMovement < 0 -> Color(0xFFEF4444)
                    else -> TextMuted
                }
                Text(
                    text = moveText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = moveColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
