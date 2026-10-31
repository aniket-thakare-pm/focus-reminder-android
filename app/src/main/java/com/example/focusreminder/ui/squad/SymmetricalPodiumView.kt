package com.example.focusreminder.ui.squad

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.example.focusreminder.ui.components.NeuPill
import com.example.focusreminder.util.formatHoursMinutes

/**
 * Infographic 2: The Symmetrical 5-Peak Connected Mountain Histogram [4, 2, 1, 3, 5]
 * and Interactive Baseline Overlap Bar Graph.
 *
 * High-Contrast Visual Architecture:
 * 1. Flat rectangular pillars with prominent fill contrast and crisp 1.2dp borders.
 * 2. Single unified divider lines between adjacent columns (zero doubled seams).
 * 3. Uniform neutral background for all columns.
 * 4. Tap-to-flip between the compact Mountain Histogram and the Baseline Overlap Bar graph.
 * 5. Single-tap top-right pill toggles between "vs Yesterday" and "vs Weekly Avg".
 * 6. High-contrast baseline ghost bar in overlap view (45% opacity + crisp border).
 */
@Composable
fun SymmetricalPodiumView(
    members: List<SquadMember>,
    modifier: Modifier = Modifier
) {
    var isWeeklyAvg by remember { mutableStateOf(false) }
    var isOverlapView by remember { mutableStateOf(false) }

    // Rank members dynamically by delta (positive improvement = #1)
    val ranked = remember(members, isWeeklyAvg) {
        members.sortedByDescending { it.getDeltaMinutes(isWeeklyAvg) }
    }

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
                .padding(vertical = 14.dp, horizontal = 14.dp)
        ) {
            // Card Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELF-MASTERY & DELTA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = if (isOverlapView) "Baseline Overlap Bars" else "Improvement Ladder",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isOverlapView) "Tap chart for Podium Ladder" else "Tap chart for Overlap Bars",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }

                // Single-Tap Toggle Pill: vs Yesterday <-> vs Weekly Avg
                NeuPill(
                    onClick = { isWeeklyAvg = !isWeeklyAvg },
                    cornerRadius = 10.dp,
                    elevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isWeeklyAvg) "vs Weekly Avg" else "vs Yesterday",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive Chart Body: Tapping flips between Histogram and Overlap Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isOverlapView = !isOverlapView
                    }
            ) {
                AnimatedContent(
                    targetState = isOverlapView,
                    label = "podium_overlap_flip"
                ) { showOverlap ->
                    if (showOverlap) {
                        // View 2: Horizontal Baseline Overlap Bars (same line thickness & high contrast)
                        OverlapBarsBody(
                            members = ranked,
                            isWeeklyAvg = isWeeklyAvg
                        )
                    } else {
                        // View 1: Connected Stepped Histogram [4, 2, 1, 3, 5]
                        ConnectedHistogramBody(
                            member1 = member1,
                            member2 = member2,
                            member3 = member3,
                            member4 = member4,
                            member5 = member5,
                            isWeeklyAvg = isWeeklyAvg
                        )
                    }
                }
            }
        }
    }
}

/**
 * View 1: Connected Stepped Histogram [4, 2, 1, 3, 5].
 * - Exact uniform 1.0f width across all 5 columns
 * - Flat rectangular shapes (zero corner radius)
 * - Single unified 1.2dp divider lines between adjacent columns (crisp contrast)
 * - Deeper contrasting fill for prominent silhouette presence
 * - No thick line at the bottom
 * - Compact height
 */
@Composable
private fun ConnectedHistogramBody(
    member1: SquadMember?,
    member2: SquadMember?,
    member3: SquadMember?,
    member4: SquadMember?,
    member5: SquadMember?,
    isWeeklyAvg: Boolean
) {
    // High-contrast border and deeper debossed fill for punchy definition
    val dividerColor = TextMuted.copy(alpha = 0.50f)
    val pillarFillColor = TextMuted.copy(alpha = 0.18f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Pos 1 (Far Left): Rank #4
        HistogramPedestal(
            member = member4,
            rank = 4,
            pedestalHeight = 55.dp,
            isWeeklyAvg = isWeeklyAvg,
            isLeftmost = true,
            dividerColor = dividerColor,
            pillarFillColor = pillarFillColor,
            modifier = Modifier.weight(1.0f)
        )

        // Pos 2 (Inner Left): Rank #2
        HistogramPedestal(
            member = member2,
            rank = 2,
            pedestalHeight = 95.dp,
            isWeeklyAvg = isWeeklyAvg,
            dividerColor = dividerColor,
            pillarFillColor = pillarFillColor,
            modifier = Modifier.weight(1.0f)
        )

        // Pos 3 (Center Peak): Rank #1
        HistogramPedestal(
            member = member1,
            rank = 1,
            pedestalHeight = 125.dp,
            isWeeklyAvg = isWeeklyAvg,
            dividerColor = dividerColor,
            pillarFillColor = pillarFillColor,
            modifier = Modifier.weight(1.0f)
        )

        // Pos 4 (Inner Right): Rank #3
        HistogramPedestal(
            member = member3,
            rank = 3,
            pedestalHeight = 75.dp,
            isWeeklyAvg = isWeeklyAvg,
            dividerColor = dividerColor,
            pillarFillColor = pillarFillColor,
            modifier = Modifier.weight(1.0f)
        )

        // Pos 5 (Far Right): Rank #5
        HistogramPedestal(
            member = member5,
            rank = 5,
            pedestalHeight = 40.dp,
            isWeeklyAvg = isWeeklyAvg,
            isRightmost = true,
            dividerColor = dividerColor,
            pillarFillColor = pillarFillColor,
            modifier = Modifier.weight(1.0f)
        )
    }
}

@Composable
private fun HistogramPedestal(
    member: SquadMember?,
    rank: Int,
    pedestalHeight: Dp,
    isWeeklyAvg: Boolean,
    dividerColor: Color,
    pillarFillColor: Color,
    isLeftmost: Boolean = false,
    isRightmost: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (member == null) {
        Box(modifier = modifier.height(pedestalHeight))
        return
    }

    val deltaMin = member.getDeltaMinutes(isWeeklyAvg)
    val isPositive = deltaMin >= 0
    val deltaColor = if (isPositive) EmeraldDark else Color(0xFFEF4444)
    val deltaText = if (isPositive) "+${deltaMin}m" else "${deltaMin}m"

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Member Name (tight line height)
        Text(
            text = if (member.isCurrentUser) "You" else member.displayName,
            fontSize = 11.sp,
            fontWeight = if (member.isCurrentUser) FontWeight.Black else FontWeight.Bold,
            color = if (member.isCurrentUser) EmeraldDark else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )

        // Delta Time (directly below Name, zero artificial gap)
        Text(
            text = deltaText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = deltaColor,
            lineHeight = 12.sp,
            modifier = Modifier.padding(bottom = 3.dp)
        )

        // Stepped Pedestal Pillar (Sharp rectangular box with single unified crisp dividers)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalHeight)
                .background(
                    color = pillarFillColor,
                    shape = RectangleShape
                )
                .drawBehind {
                    val strokePx = 1.2.dp.toPx()
                    // Top border
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokePx
                    )
                    // Left border (only for leftmost pillar to prevent double lines)
                    if (isLeftmost) {
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = strokePx
                        )
                    }
                    // Right border (drawn once per pillar)
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokePx
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Clean #rank text (high contrast)
                Text(
                    text = "#$rank",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )

                // Positional Movement Indicator
                val moveText = when {
                    member.rankMovement > 0 -> "▲${member.rankMovement}"
                    member.rankMovement < 0 -> "▼${-member.rankMovement}"
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
                    color = moveColor
                )
            }
        }
    }
}

/**
 * View 2: Horizontal Baseline Overlap Bars.
 * Direct visual correlation with EXACT SAME line thickness (12dp):
 * - Baseline bar (ghost): 45% opacity with crisp 1dp border for unmistakable visibility.
 * - Today bar (solid): high-contrast emerald or coral, seamless overlay.
 */
@Composable
private fun OverlapBarsBody(
    members: List<SquadMember>,
    isWeeklyAvg: Boolean
) {
    val maxBarTime = members.flatMap {
        listOf(it.monitoredMinutes, it.getBaselineMinutes(isWeeklyAvg))
    }.maxOrNull()?.coerceAtLeast(60) ?: 60

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        members.forEach { member ->
            val baselineMin = member.getBaselineMinutes(isWeeklyAvg)
            val todayRatio = (member.monitoredMinutes.toFloat() / maxBarTime.toFloat()).coerceIn(0.04f, 1f)
            val baselineRatio = (baselineMin.toFloat() / maxBarTime.toFloat()).coerceIn(0.04f, 1f)
            val deltaMin = member.getDeltaMinutes(isWeeklyAvg)
            val isImproved = deltaMin >= 0
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Now: ${formatHoursMinutes(member.monitoredMinutes)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "•",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Base: ${formatHoursMinutes(baselineMin)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        Text(
                            text = if (isImproved) "-${deltaMin}m" else "+${-deltaMin}m",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = accentColor
                        )
                    }
                }

                // Overlap Bar Track: Both today and baseline share EXACT SAME height (12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(NeuSurfaceSunken, RoundedCornerShape(6.dp))
                        .border(
                            width = 1.dp,
                            color = TextMuted.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Ghost Bar: Baseline (Yesterday or Weekly Avg) with crisp 45% opacity & border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(baselineRatio)
                            .fillMaxHeight()
                            .background(
                                color = TextMuted.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = TextMuted.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(6.dp)
                            )
                    )

                    // Foreground Bar: Today (Exact same 12dp height, overlays seamlessly)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(todayRatio)
                            .fillMaxHeight()
                            .background(
                                color = accentColor,
                                shape = RoundedCornerShape(6.dp)
                            )
                    )
                }
            }
        }
    }
}
