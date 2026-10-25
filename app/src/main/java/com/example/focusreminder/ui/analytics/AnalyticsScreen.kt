package com.example.focusreminder.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusreminder.domain.model.*
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val timeframe by viewModel.timeframe.collectAsState()
    val dateOffset by viewModel.dateOffset.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showSleepConfigDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ================= HEADER & TIMEFRAME SELECTOR =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Analytics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Behavioral usage & sector insights",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // Neumorphic Timeframe Segmented Control
            NeuCard(
                cornerRadius = 14.dp,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TimeframePill(
                        label = "Day",
                        isSelected = timeframe == AnalyticsTimeframe.TODAY,
                        onClick = { viewModel.setTimeframe(AnalyticsTimeframe.TODAY) }
                    )
                    TimeframePill(
                        label = "Week",
                        isSelected = timeframe == AnalyticsTimeframe.WEEK,
                        onClick = { viewModel.setTimeframe(AnalyticsTimeframe.WEEK) }
                    )
                    TimeframePill(
                        label = "Month",
                        isSelected = timeframe == AnalyticsTimeframe.MONTH,
                        onClick = { viewModel.setTimeframe(AnalyticsTimeframe.MONTH) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= DATE STEPPER ROW =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuCircleButton(onClick = { viewModel.setDateOffset(-1) }) {
                Text("◀", fontSize = 11.sp, color = TextSecondary)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = summary?.dateDisplay ?: "Loading...",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
                if (dateOffset != 0) {
                    Text(
                        text = "Reset",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier
                            .clickable { viewModel.resetDateOffset() }
                            .padding(4.dp)
                    )
                }
            }

            NeuCircleButton(
                onClick = { if (dateOffset < 0) viewModel.setDateOffset(1) },
                enabled = dateOffset < 0
            ) {
                Text("▶", fontSize = 11.sp, color = if (dateOffset < 0) TextSecondary else TextMuted.copy(alpha = 0.3f))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ================= SCOPE FILTER CHIPS =================
        val apps = summary?.availableApps ?: emptyList()
        val categories = summary?.availableCategories ?: emptyList()

        if (apps.isNotEmpty() || categories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipPill(
                    label = "All Monitored",
                    isSelected = selectedPackage == null && selectedCategory == null,
                    onClick = {
                        viewModel.filterByPackage(null)
                        viewModel.filterByCategory(null)
                    }
                )

                for (cat in categories) {
                    FilterChipPill(
                        label = cat,
                        isSelected = selectedCategory == cat,
                        onClick = { viewModel.filterByCategory(if (selectedCategory == cat) null else cat) }
                    )
                }

                for (pkg in apps) {
                    val label = pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                    FilterChipPill(
                        label = label,
                        isSelected = selectedPackage == pkg,
                        onClick = { viewModel.filterByPackage(if (selectedPackage == pkg) null else pkg) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ================= SCROLLABLE ANALYTICS CARDS =================
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val s = summary
            if (s != null) {
                // 1. TOP KPI SUMMARY CARD
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = if (timeframe == AnalyticsTimeframe.TODAY) "TOTAL USAGE TODAY" else "TOTAL TIME IN PERIOD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextMuted,
                                        letterSpacing = 1.2.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatMinutes(s.totalUsageMinutes),
                                        fontSize = 32.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                }

                                if (timeframe == AnalyticsTimeframe.TODAY && s.dailyAverageMinutes > 0) {
                                    val isReduction = s.vsAveragePercentage <= 0
                                    val badgeColor = if (isReduction) EmeraldDark else Color(0xFFF59E0B)
                                    NeuInsetWell(
                                        cornerRadius = 10.dp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = "${if (s.vsAveragePercentage > 0) "+" else ""}${s.vsAveragePercentage}% vs avg",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sub-metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                NeuInsetWell(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 14.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    ) {
                                        Text("AVG SESSION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                        Text(
                                            text = formatDuration(s.averageSessionDurationMs),
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDark
                                        )
                                    }
                                }

                                NeuInsetWell(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 14.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    ) {
                                        Text("SESSIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                        Text(
                                            text = "${s.totalSessionCount}",
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                NeuInsetWell(
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 14.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    ) {
                                        Text("DAILY AVG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                        Text(
                                            text = formatMinutes(s.dailyAverageMinutes),
                                            fontSize = 15.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. TIME-OF-DAY SECTORS CARD
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TIME OF DAY SECTORS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextSecondary,
                                    letterSpacing = 1.1.sp
                                )

                                s.peakSector?.let { peak ->
                                    Text(
                                        text = "Peak: ${peak.sector.name} (${(peak.percentage * 100).toInt()}%)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(peak.sector.colorLong)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Segmented Sector Progress Bar
                            SectorSegmentedBar(
                                sectors = s.sectors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sector Legend Grid (2x2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectorLegendItem(
                                        stat = s.sectors.getOrNull(0),
                                        modifier = Modifier.weight(1f)
                                    )
                                    SectorLegendItem(
                                        stat = s.sectors.getOrNull(1),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectorLegendItem(
                                        stat = s.sectors.getOrNull(2),
                                        modifier = Modifier.weight(1f)
                                    )
                                    SectorLegendItem(
                                        stat = s.sectors.getOrNull(3),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. WEEKLY OR MONTHLY TREND CHART (When in Week or Month view)
                if (timeframe != AnalyticsTimeframe.TODAY && s.dailyPoints.isNotEmpty()) {
                    item {
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            elevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (timeframe == AnalyticsTimeframe.WEEK) "7-DAY USAGE TREND" else "MONTHLY TREND",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextSecondary,
                                        letterSpacing = 1.1.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp, 2.dp)
                                                .background(Color(0xFF38BDF8))
                                        )
                                        Text(
                                            text = "Avg: ${formatMinutes(s.dailyAverageMinutes)}/d",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                DailyUsageBarChart(
                                    points = s.dailyPoints,
                                    benchmarkDailyAverage = s.dailyAverageMinutes,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                )
                            }
                        }
                    }
                }

                // 4. SESSION DURATION DISTRIBUTION (BUCKETS)
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "SESSION DURATION DISTRIBUTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextSecondary,
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = "Impulsive checks vs intentional deep sessions",
                                fontSize = 10.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val total = s.buckets.totalSessions.coerceAtLeast(1)

                            SessionBucketRow(
                                label = "< 1 min",
                                sublabel = "Micro-checks / Glances",
                                count = s.buckets.microCount,
                                total = total,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            SessionBucketRow(
                                label = "1 – 5 min",
                                sublabel = "Short Visits",
                                count = s.buckets.shortCount,
                                total = total,
                                color = Color(0xFF34D399)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            SessionBucketRow(
                                label = "5 – 10 min",
                                sublabel = "Standard Active Sessions",
                                count = s.buckets.standardCount,
                                total = total,
                                color = Color(0xFFFBBF24)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            SessionBucketRow(
                                label = "10 min+",
                                sublabel = "Capped / Limit Reached",
                                count = s.buckets.cappedCount,
                                total = total,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }

                // 5. SLEEP & WAKE BUFFER GOAL CARD
                item {
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "WIND-DOWN SLEEP GOAL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextSecondary,
                                        letterSpacing = 1.1.sp
                                    )
                                    Text(
                                        text = "Window: ${s.sleepGoal.targetBufferHours}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextMuted
                                    )
                                }

                                NeuInsetWell(
                                    cornerRadius = 10.dp
                                ) {
                                    val passed = s.sleepGoal.isPassed
                                    Text(
                                        text = if (passed) "Goal Met" else "Breached",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (passed) EmeraldDark else Color(0xFFEF4444),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = s.sleepGoal.breachTimeDesc,
                                fontSize = 12.sp,
                                color = if (s.sleepGoal.isPassed) EmeraldDark else Color(0xFFF87171),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Weekly Adherence bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "7-Day Adherence",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${s.sleepGoal.weeklyMetDays} / ${s.sleepGoal.weeklyTotalDays} Days (${(s.sleepGoal.weeklyComplianceRate * 100).toInt()}%)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            NeuInsetWell(
                                cornerRadius = 6.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(s.sleepGoal.weeklyComplianceRate.coerceIn(0.04f, 1f))
                                        .background(EmeraldPrimary, RoundedCornerShape(6.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Adjust Sleep Hours",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier
                                    .clickable { showSleepConfigDialog = true }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // 6. TOP MONITORED APPS RANKING
                if (s.topApps.isNotEmpty()) {
                    item {
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            elevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = "APP USAGE RANKING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextSecondary,
                                    letterSpacing = 1.1.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                for (app in s.topApps) {
                                    AppRankingRow(stat = app)
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            } else if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.dp)
                    }
                }
            }
        }
    }

    // Config Dialog for Sleep Buffer Window
    if (showSleepConfigDialog) {
        SleepBufferConfigDialog(
            currentWindow = Pair(23, 0),
            onDismiss = { showSleepConfigDialog = false },
            onConfirm = { startH, endH ->
                viewModel.updateSleepBuffer(startH, endH)
                showSleepConfigDialog = false
            }
        )
    }
}

// ================= HELPER SUB-COMPONENTS =================

@Composable
fun TimeframePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) EmeraldDark else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) Color.White else TextMuted
        )
    }
}

@Composable
fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NeuCard(
        cornerRadius = 14.dp,
        elevation = if (isSelected) 6.dp else 2.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isSelected) EmeraldDark.copy(alpha = 0.25f) else Color.Transparent,
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) EmeraldPrimary else TextSecondary
            )
        }
    }
}

@Composable
fun SectorSegmentedBar(
    sectors: List<SectorUsageStat>,
    modifier: Modifier = Modifier
) {
    NeuInsetWell(
        cornerRadius = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            for (stat in sectors) {
                if (stat.percentage > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(stat.percentage)
                            .background(Color(stat.sector.colorLong))
                    )
                }
            }
        }
    }
}

@Composable
fun SectorLegendItem(
    stat: SectorUsageStat?,
    modifier: Modifier = Modifier
) {
    if (stat == null) return
    NeuInsetWell(
        cornerRadius = 12.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(stat.sector.colorLong), CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.sector.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = String.format(java.util.Locale.US, "%02d:00–%02d:00", stat.sector.startHour, stat.sector.endHour),
                    fontSize = 8.sp,
                    color = TextMuted
                )
            }
            Text(
                text = formatMinutes(stat.durationMinutes),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(stat.sector.colorLong)
            )
        }
    }
}

@Composable
fun DailyUsageBarChart(
    points: List<DayUsagePoint>,
    benchmarkDailyAverage: Int,
    modifier: Modifier = Modifier
) {
    val emeraldPrimary = EmeraldPrimary
    val emeraldDark = EmeraldDark
    val maxMins = maxOf(benchmarkDailyAverage * 1.3f, points.maxOfOrNull { it.usageMinutes.toFloat() } ?: 60f).coerceAtLeast(30f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barCount = points.size
        val barWidth = (w / (barCount * 2.0f)).coerceIn(8.dp.toPx(), 24.dp.toPx())
        val spacing = (w - (barWidth * barCount)) / (barCount + 1)

        // Draw Benchmark Line (Dashed)
        val benchmarkY = h - (h * (benchmarkDailyAverage / maxMins))
        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.6f),
            start = Offset(0f, benchmarkY),
            end = Offset(w, benchmarkY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

        // Draw Bars
        points.forEachIndexed { index, point ->
            val barH = (h * (point.usageMinutes / maxMins)).coerceAtLeast(4.dp.toPx())
            val x = spacing + (index * (barWidth + spacing))
            val y = h - barH

            val barColor = when {
                point.isToday -> emeraldPrimary
                point.usageMinutes > benchmarkDailyAverage -> Color(0xFFF59E0B)
                else -> emeraldDark
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }

    // X-Axis Labels Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        for (p in points) {
            Text(
                text = p.dayOfWeek,
                fontSize = 9.sp,
                color = if (p.isToday) EmeraldPrimary else TextMuted,
                fontWeight = if (p.isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SessionBucketRow(
    label: String,
    sublabel: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = (count.toFloat() / total).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "($sublabel)", fontSize = 9.sp, color = TextMuted)
            }
            Text(
                text = "$count (${(pct * 100).toInt()}%)",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        NeuInsetWell(
            cornerRadius = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pct.coerceAtLeast(0.02f))
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun AppRankingRow(stat: AppUsageStat) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NeuCircle(
            modifier = Modifier.size(36.dp),
            elevation = 3.dp
        ) {
            Text(
                text = stat.appName.firstOrNull()?.uppercase() ?: "A",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = EmeraldPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stat.appName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = formatMinutes(stat.durationMinutes),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stat.sessionCount} sessions",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "${(stat.percentage * 100).toInt()}% share",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun NeuCircleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    NeuCard(
        modifier = Modifier
            .size(32.dp)
            .clickable(enabled = enabled, onClick = onClick),
        cornerRadius = 16.dp,
        elevation = if (enabled) 4.dp else 1.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun SleepBufferConfigDialog(
    currentWindow: Pair<Int, Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var startHour by remember { mutableStateOf(currentWindow.first) }
    var endHour by remember { mutableStateOf(currentWindow.second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Sleep Buffer", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Set the hours before sleep when you want to avoid screen time completely:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Hour", fontSize = 11.sp, color = TextMuted)
                        Text(String.format(java.util.Locale.US, "%02d:00", startHour), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = { startHour = (startHour + 23) % 24 }) { Text("-") }
                            Button(onClick = { startHour = (startHour + 1) % 24 }) { Text("+") }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("End Hour", fontSize = 11.sp, color = TextMuted)
                        Text(String.format(java.util.Locale.US, "%02d:00", endHour), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = { endHour = (endHour + 23) % 24 }) { Text("-") }
                            Button(onClick = { endHour = (endHour + 1) % 24 }) { Text("+") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(startHour, endHour) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = NeuSurface
    )
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = durationMs / 1000L
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(java.util.Locale.US, "%dm %02ds", m, s)
}
