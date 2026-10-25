package com.example.focusreminder.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    initialPackage: String? = null,
    onClearInitialPackage: () -> Unit = {},
    viewModel: AnalyticsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val timeframe by viewModel.timeframe.collectAsState()
    val dateOffset by viewModel.dateOffset.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isControlledOnly by viewModel.isControlledOnly.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showScopePickerModal by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<BufferGoal?>(null) }
    var isCreatingGoal by remember { mutableStateOf(false) }

    // If navigated with initialPackage, focus it once
    LaunchedEffect(initialPackage) {
        if (!initialPackage.isNullOrBlank()) {
            viewModel.focusPackage(initialPackage)
            onClearInitialPackage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ================= HEADER =================
        Column {
            Text(
                text = "Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Comprehensive phone usage & sector insights",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ================= FULL-WIDTH TIMEFRAME TABS (DAY | WEEK | MONTH) =================
        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimeframePill(
                    label = "Day",
                    isSelected = timeframe == AnalyticsTimeframe.TODAY,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTimeframe(AnalyticsTimeframe.TODAY) }
                )
                TimeframePill(
                    label = "Week",
                    isSelected = timeframe == AnalyticsTimeframe.WEEK,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTimeframe(AnalyticsTimeframe.WEEK) }
                )
                TimeframePill(
                    label = "Month",
                    isSelected = timeframe == AnalyticsTimeframe.MONTH,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.setTimeframe(AnalyticsTimeframe.MONTH) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

        Spacer(modifier = Modifier.height(10.dp))

        // ================= ACTIVE SCOPE BREADCRUMB PILL =================
        val isScoped = selectedPackage != null || selectedCategory != null || isControlledOnly
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NeuCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showScopePickerModal = true },
                cornerRadius = 14.dp,
                elevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Scope:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = summary?.scopeLabel ?: "All Apps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isScoped) EmeraldDark else TextPrimary
                        )
                    }
                    Text(
                        text = "▾",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            if (isScoped) {
                Spacer(modifier = Modifier.width(8.dp))
                NeuCircleButton(onClick = { viewModel.clearScope() }) {
                    Text("✕", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                                        text = if (timeframe == AnalyticsTimeframe.TODAY) "TOTAL SCREEN TIME TODAY" else "TOTAL TIME IN PERIOD",
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

                // 2. PRIMARY CHART (Day: Sectors | Week/Month: 7-Day Trend Bar Chart)
                if (timeframe == AnalyticsTimeframe.TODAY) {
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

                                SectorSegmentedBar(
                                    sectors = s.sectors,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(14.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SectorLegendItem(stat = s.sectors.getOrNull(0), modifier = Modifier.weight(1f))
                                        SectorLegendItem(stat = s.sectors.getOrNull(1), modifier = Modifier.weight(1f))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SectorLegendItem(stat = s.sectors.getOrNull(2), modifier = Modifier.weight(1f))
                                        SectorLegendItem(stat = s.sectors.getOrNull(3), modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else if (s.dailyPoints.isNotEmpty()) {
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

                // 3. APP & CATEGORY BREAKDOWN (MOVED UP HERE FOR IMMEDIATE CONTEXTUAL CLARITY)
                if (s.topApps.isNotEmpty() || s.categories.isNotEmpty()) {
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
                                        text = "APP & CATEGORY BREAKDOWN",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextSecondary,
                                        letterSpacing = 1.1.sp
                                    )
                                    Text(
                                        text = "Tap app to focus",
                                        fontSize = 9.sp,
                                        color = EmeraldDark
                                    )
                                }

                                // Category Pills
                                if (s.categories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (cat in s.categories.take(4)) {
                                            CategoryBadge(
                                                cat = cat,
                                                isSelected = selectedCategory == cat.category,
                                                onClick = { viewModel.focusCategory(cat.category) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Top Apps List
                                for (app in s.topApps.take(6)) {
                                    AppRankingRow(
                                        stat = app,
                                        isFocused = selectedPackage == app.packageName,
                                        onClick = { viewModel.focusPackage(app.packageName) }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
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
                                text = "Impulsive glances vs deep continuous sessions",
                                fontSize = 10.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val total = s.buckets.totalSessions.coerceAtLeast(1)

                            SessionBucketRow(
                                label = "< 1 min",
                                sublabel = "Micro-checks",
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
                                sublabel = "Active Sessions",
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

                // 5. CUSTOM FOCUS BUFFER GOALS (UP TO 3 GOALS)
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
                                    text = "FOCUS BUFFER GOALS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextSecondary,
                                    letterSpacing = 1.1.sp
                                )

                                if (s.bufferGoals.size < 3) {
                                    Text(
                                        text = "+ Add Goal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        modifier = Modifier
                                            .clickable { isCreatingGoal = true }
                                            .padding(4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            for (goal in s.bufferGoals) {
                                BufferGoalRow(
                                    goal = goal,
                                    onClick = {
                                        editingGoal = BufferGoal(
                                            id = goal.id,
                                            name = goal.name,
                                            startHour = goal.startHour,
                                            endHour = goal.endHour
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
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

    // Modal: Scope Picker (All Apps, Controlled Apps, Categories, Specific Apps)
    if (showScopePickerModal) {
        ScopePickerModal(
            summary = summary,
            onSelectAllApps = {
                viewModel.clearScope()
                showScopePickerModal = false
            },
            onSelectControlledOnly = {
                viewModel.setControlledOnly(true)
                showScopePickerModal = false
            },
            onSelectCategory = { cat ->
                viewModel.focusCategory(cat)
                showScopePickerModal = false
            },
            onSelectPackage = { pkg ->
                viewModel.focusPackage(pkg)
                showScopePickerModal = false
            },
            onDismiss = { showScopePickerModal = false }
        )
    }

    // Dialog: Edit / Add Buffer Goal
    if (editingGoal != null) {
        BufferGoalConfigDialog(
            goal = editingGoal!!,
            isNew = false,
            onDismiss = { editingGoal = null },
            onSave = { updated ->
                viewModel.saveBufferGoal(updated)
                editingGoal = null
            },
            onDelete = { id ->
                viewModel.deleteBufferGoal(id)
                editingGoal = null
            }
        )
    }

    if (isCreatingGoal) {
        val newId = "goal_${System.currentTimeMillis()}"
        BufferGoalConfigDialog(
            goal = BufferGoal(id = newId, name = "Deep Work Buffer", startHour = 14, endHour = 16),
            isNew = true,
            onDismiss = { isCreatingGoal = false },
            onSave = { created ->
                viewModel.saveBufferGoal(created)
                isCreatingGoal = false
            },
            onDelete = { isCreatingGoal = false }
        )
    }
}

// ================= SUB-COMPONENTS =================

@Composable
fun TimeframePill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                color = if (isSelected) EmeraldDark else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) Color.White else TextMuted
        )
    }
}

@Composable
fun CategoryBadge(
    cat: CategoryUsageStat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeuCard(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = 10.dp,
        elevation = if (isSelected) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) EmeraldDark.copy(alpha = 0.2f) else Color.Transparent)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = cat.category,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) EmeraldPrimary else TextSecondary,
                maxLines = 1
            )
            Text(
                text = formatMinutes(cat.durationMinutes),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun AppRankingRow(
    stat: AppUsageStat,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
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
                color = if (isFocused) Color(0xFF38BDF8) else EmeraldPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stat.appName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) Color(0xFF38BDF8) else TextPrimary
                    )
                    if (stat.isControlled) {
                        Text(
                            text = "CONTROLLED",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldDark
                        )
                    }
                }
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
fun BufferGoalRow(
    goal: BufferGoalStatus,
    onClick: () -> Unit
) {
    NeuInsetWell(
        cornerRadius = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = goal.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Window: ${goal.targetBufferHours}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                Text(
                    text = if (goal.isPassed) "Goal Met" else "Breached",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (goal.isPassed) EmeraldDark else Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.breachTimeDesc,
                    fontSize = 11.sp,
                    color = if (goal.isPassed) EmeraldDark else Color(0xFFF87171)
                )
                Text(
                    text = "${goal.weeklyMetDays}/7d (${(goal.weeklyComplianceRate * 100).toInt()}%)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun ScopePickerModal(
    summary: AnalyticsSummary?,
    onSelectAllApps: () -> Unit,
    onSelectControlledOnly: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectPackage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Analytics Scope", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("GLOBAL SCOPE", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    ScopeListItem(title = "All Apps (Entire Phone)", subtitle = "All apps combined", onClick = onSelectAllApps)
                    Spacer(modifier = Modifier.height(6.dp))
                    ScopeListItem(title = "Controlled Apps Only", subtitle = "Only apps configured in limits", onClick = onSelectControlledOnly)
                }

                summary?.availableCategories?.takeIf { it.isNotEmpty() }?.let { cats ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("CATEGORIES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted)
                    }
                    for (cat in cats) {
                        item {
                            ScopeListItem(title = cat, subtitle = "All $cat applications", onClick = { onSelectCategory(cat) })
                        }
                    }
                }

                summary?.availableApps?.takeIf { it.isNotEmpty() }?.let { apps ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("INDIVIDUAL APPS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted)
                    }
                    for (app in apps) {
                        item {
                            ScopeListItem(
                                title = app.appName,
                                subtitle = if (app.isControlled) "Controlled" else "Uncontrolled",
                                tagColor = if (app.isControlled) EmeraldDark else TextMuted,
                                onClick = { onSelectPackage(app.packageName) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextMuted) }
        },
        containerColor = NeuSurface
    )
}

@Composable
fun ScopeListItem(
    title: String,
    subtitle: String,
    tagColor: Color? = null,
    onClick: () -> Unit
) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 10.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 9.sp, color = tagColor ?: TextMuted)
            }
            Text("→", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun BufferGoalConfigDialog(
    goal: BufferGoal,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (BufferGoal) -> Unit,
    onDelete: (String) -> Unit
) {
    var name by remember { mutableStateOf(goal.name) }
    var startHour by remember { mutableStateOf(goal.startHour) }
    var endHour by remember { mutableStateOf(goal.endHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "Add Focus Buffer Goal" else "Edit Buffer Goal", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Set the time window when you commit to zero screen time:",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Hour", fontSize = 10.sp, color = TextMuted)
                        Text(String.format(java.util.Locale.US, "%02d:00", startHour), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = { startHour = (startHour + 23) % 24 }) { Text("-") }
                            Button(onClick = { startHour = (startHour + 1) % 24 }) { Text("+") }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("End Hour", fontSize = 10.sp, color = TextMuted)
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
                onClick = {
                    onSave(goal.copy(name = name.ifBlank { "Focus Buffer" }, startHour = startHour, endHour = endHour))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isNew) {
                    TextButton(onClick = { onDelete(goal.id) }) {
                        Text("Delete", color = Color(0xFFEF4444))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextMuted)
                }
            }
        },
        containerColor = NeuSurface
    )
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
        Row(modifier = Modifier.fillMaxSize()) {
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

        val benchmarkY = h - (h * (benchmarkDailyAverage / maxMins))
        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.6f),
            start = Offset(0f, benchmarkY),
            end = Offset(w, benchmarkY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )

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
