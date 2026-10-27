package com.example.focusreminder.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusreminder.domain.model.*
import com.example.focusreminder.platform.AppManager
import com.example.focusreminder.platform.DefaultAllowedAppsHelper
import com.example.focusreminder.platform.InstalledAppInfo
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun AnalyticsScreen(
    initialPackage: String? = null,
    onClearInitialPackage: () -> Unit = {},
    onNavigateToUsageLogs: (sectorId: String, packageName: String?) -> Unit = { _, _ -> },
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

    val context = LocalContext.current
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
                                    onSectorClick = { sectorId ->
                                        onNavigateToUsageLogs(sectorId, selectedPackage)
                                    },
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
                                        SectorLegendItem(
                                            stat = s.sectors.getOrNull(0),
                                            onClick = { stat ->
                                                onNavigateToUsageLogs(stat.sector.id, selectedPackage)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SectorLegendItem(
                                            stat = s.sectors.getOrNull(1),
                                            onClick = { stat ->
                                                onNavigateToUsageLogs(stat.sector.id, selectedPackage)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SectorLegendItem(
                                            stat = s.sectors.getOrNull(2),
                                            onClick = { stat ->
                                                onNavigateToUsageLogs(stat.sector.id, selectedPackage)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SectorLegendItem(
                                            stat = s.sectors.getOrNull(3),
                                            onClick = { stat ->
                                                onNavigateToUsageLogs(stat.sector.id, selectedPackage)
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
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
                                            startMinute = goal.startMinute,
                                            endHour = goal.endHour,
                                            endMinute = goal.endMinute,
                                            enforcementMode = goal.enforcementMode,
                                            allowedPackages = goal.allowedPackages,
                                            isEnabled = goal.isEnabled
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
        val defaultAllowed = remember { DefaultAllowedAppsHelper.getDefaultAllowedPackages(context) }
        BufferGoalConfigDialog(
            goal = BufferGoal(
                id = newId,
                name = "Bedtime Buffer",
                startHour = 23,
                startMinute = 0,
                endHour = 7,
                endMinute = 0,
                enforcementMode = GoalEnforcementMode.BLOCK_CONTROLLED_APPS,
                allowedPackages = defaultAllowed,
                isEnabled = true
            ),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Window: ${goal.targetBufferHours}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }

                val (badgeLabel, badgeBg, badgeColor) = when (goal.enforcementMode) {
                    GoalEnforcementMode.TRACKING_ONLY -> Triple(
                        "TRACK ONLY",
                        NeuSurface,
                        TextSecondary
                    )
                    GoalEnforcementMode.BLOCK_CONTROLLED_APPS -> Triple(
                        "BLOCK CONTROLLED",
                        Color(0xFFFEF3C7),
                        Color(0xFFD97706)
                    )
                    GoalEnforcementMode.STRICT_LOCKDOWN -> Triple(
                        "STRICT (${goal.allowedPackageCount} ALLOWED)",
                        Color(0xFFFEE2E2),
                        Color(0xFFDC2626)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (!goal.isEnabled) "Disabled" else goal.breachTimeDesc,
                    fontSize = 11.sp,
                    color = if (!goal.isEnabled) TextMuted else if (goal.isPassed) EmeraldDark else Color(0xFFF87171)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (!goal.isEnabled) "Disabled" else if (goal.isPassed) "Goal Met" else "Breached",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (!goal.isEnabled) TextMuted else if (goal.isPassed) EmeraldDark else Color(0xFFEF4444)
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
    val context = LocalContext.current
    var name by remember { mutableStateOf(goal.name) }
    var startHour by remember { mutableStateOf(goal.startHour) }
    var startMinute by remember { mutableStateOf(goal.startMinute) }
    var endHour by remember { mutableStateOf(goal.endHour) }
    var endMinute by remember { mutableStateOf(goal.endMinute) }
    var enforcementMode by remember { mutableStateOf(goal.enforcementMode) }
    var allowedPackages by remember {
        mutableStateOf(
            if (goal.allowedPackages.isNotEmpty()) goal.allowedPackages
            else DefaultAllowedAppsHelper.getDefaultAllowedPackages(context)
        )
    }
    var isEnabled by remember { mutableStateOf(goal.isEnabled) }
    var showAllowedAppsModal by remember { mutableStateOf(false) }

    // Window duration calculation with overnight wrap
    val startTotalMins = startHour * 60 + startMinute
    val endTotalMins = endHour * 60 + endMinute
    val diffMins = if (endTotalMins >= startTotalMins) {
        endTotalMins - startTotalMins
    } else {
        (1440 - startTotalMins) + endTotalMins
    }
    val diffHours = diffMins / 60
    val diffRemMins = diffMins % 60
    val durationText = buildString {
        append("${diffHours}h")
        if (diffRemMins > 0) append(" ${diffRemMins}m")
        append(" window")
        if (endTotalMins < startTotalMins) append(" (crosses midnight)")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) "Add Focus Window" else "Edit Focus Window",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEnabled) "Active" else "Off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) EmeraldDark else TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldDark
                        )
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Goal Name
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

                // 2. Start & End Time Pickers
                Text(
                    text = "TIME WINDOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Start Time Card
                    NeuCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        startHour = h
                                        startMinute = m
                                    },
                                    startHour,
                                    startMinute,
                                    false
                                ).show()
                            },
                        cornerRadius = 12.dp,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("START", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", startHour, startMinute),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldDark
                            )
                            val ampm = if (startHour >= 12) "PM" else "AM"
                            val h12 = when {
                                startHour == 0 -> 12
                                startHour > 12 -> startHour - 12
                                else -> startHour
                            }
                            Text(
                                text = String.format(Locale.US, "%d:%02d %s", h12, startMinute, ampm),
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text("Tap to change", fontSize = 8.sp, color = TextMuted)
                        }
                    }

                    // End Time Card
                    NeuCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        endHour = h
                                        endMinute = m
                                    },
                                    endHour,
                                    endMinute,
                                    false
                                ).show()
                            },
                        cornerRadius = 12.dp,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("END", fontSize = 9.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", endHour, endMinute),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldDark
                            )
                            val ampm = if (endHour >= 12) "PM" else "AM"
                            val h12 = when {
                                endHour == 0 -> 12
                                endHour > 12 -> endHour - 12
                                else -> endHour
                            }
                            Text(
                                text = String.format(Locale.US, "%d:%02d %s", h12, endMinute, ampm),
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text("Tap to change", fontSize = 8.sp, color = TextMuted)
                        }
                    }
                }

                Text(
                    text = durationText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Enforcement Mode Options (Tier 1, 2, 3)
                Text(
                    text = "ENFORCEMENT MODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )

                ModeSelectionCard(
                    title = "Tracking Only",
                    description = "Monitors compliance and logs breaches in analytics. No apps are locked.",
                    isSelected = enforcementMode == GoalEnforcementMode.TRACKING_ONLY,
                    onClick = { enforcementMode = GoalEnforcementMode.TRACKING_ONLY }
                )

                ModeSelectionCard(
                    title = "Block Controlled Apps",
                    description = "Locks apps on your controlled list (e.g. YouTube, Instagram) during this window.",
                    isSelected = enforcementMode == GoalEnforcementMode.BLOCK_CONTROLLED_APPS,
                    onClick = { enforcementMode = GoalEnforcementMode.BLOCK_CONTROLLED_APPS }
                )

                ModeSelectionCard(
                    title = "Strict Lockdown",
                    description = "Locks ALL apps on your phone except whitelisted essential apps.",
                    isSelected = enforcementMode == GoalEnforcementMode.STRICT_LOCKDOWN,
                    onClick = { enforcementMode = GoalEnforcementMode.STRICT_LOCKDOWN }
                )

                // 4. Whitelisted Apps for Strict Lockdown
                if (enforcementMode == GoalEnforcementMode.STRICT_LOCKDOWN) {
                    NeuInsetWell(
                        cornerRadius = 12.dp,
                        modifier = Modifier.fillMaxWidth()
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ALLOWED APPS (${allowedPackages.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Only these apps will be usable during lockdown.",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Button(
                                    onClick = { showAllowedAppsModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Manage", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        goal.copy(
                            name = name.ifBlank { "Focus Buffer" },
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            enforcementMode = enforcementMode,
                            allowedPackages = allowedPackages,
                            isEnabled = isEnabled
                        )
                    )
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

    // Allowed Apps Selector Dialog
    if (showAllowedAppsModal) {
        AllowedAppsSelectionDialog(
            initialAllowed = allowedPackages,
            onDismiss = { showAllowedAppsModal = false },
            onConfirm = { updated ->
                allowedPackages = updated
                showAllowedAppsModal = false
            }
        )
    }
}

@Composable
fun ModeSelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 12.dp,
        elevation = if (isSelected) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = EmeraldDark,
                    unselectedColor = TextMuted
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) EmeraldDark else TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
fun AllowedAppsSelectionDialog(
    initialAllowed: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val appManager = remember { AppManager(context) }
    var allApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf(initialAllowed) }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allApps = appManager.getLaunchableApps().sortedBy { it.appName.lowercase() }
            isLoadingApps = false
        }
    }

    val defaultEssentials = remember { DefaultAllowedAppsHelper.getDefaultAllowedPackages(context) }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allowed Apps",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    text = "${selectedPackages.size} allowed",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            selectedPackages = defaultEssentials
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Reset Defaults", fontSize = 11.sp, color = EmeraldDark)
                    }

                    TextButton(
                        onClick = {
                            if (selectedPackages.size == allApps.size) {
                                selectedPackages = defaultEssentials
                            } else {
                                selectedPackages = allApps.map { it.packageName }.toSet()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (selectedPackages.size == allApps.size) "Only Defaults" else "Allow All",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.dp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps.size) { index ->
                            val app = filteredApps[index]
                            val isAllowed = selectedPackages.contains(app.packageName)
                            val isDefaultEssential = defaultEssentials.contains(app.packageName)

                            NeuCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPackages = if (isAllowed) {
                                            selectedPackages - app.packageName
                                        } else {
                                            selectedPackages + app.packageName
                                        }
                                    },
                                cornerRadius = 10.dp,
                                elevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val iconBitmap = remember(app.icon) {
                                        app.icon?.toBitmap(width = 64, height = 64)?.asImageBitmap()
                                    }
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(NeuSurfaceSunken, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                        if (isDefaultEssential) {
                                            Text(
                                                text = "Default Essential",
                                                fontSize = 9.sp,
                                                color = EmeraldDark
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isAllowed,
                                        onCheckedChange = { checked ->
                                            selectedPackages = if (checked) {
                                                selectedPackages + app.packageName
                                            } else {
                                                selectedPackages - app.packageName
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = EmeraldDark,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPackages) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
            ) {
                Text("Done", color = Color.White)
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

@Composable
fun SectorSegmentedBar(
    sectors: List<SectorUsageStat>,
    onSectorClick: ((String) -> Unit)? = null,
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
                            .then(
                                if (onSectorClick != null) {
                                    Modifier.clickable { onSectorClick(stat.sector.id) }
                                } else Modifier
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun SectorLegendItem(
    stat: SectorUsageStat?,
    onClick: ((SectorUsageStat) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (stat == null) return
    NeuInsetWell(
        cornerRadius = 12.dp,
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable { onClick(stat) }
            } else Modifier
        )
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
