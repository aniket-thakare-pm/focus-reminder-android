package com.example.focusreminder.ui.logs

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusreminder.domain.model.UsageLogEntry
import com.example.focusreminder.platform.analytics.SectorConfigStore
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*
import java.util.Calendar
import java.util.Locale

@Composable
fun UsageLogsScreen(
    onBack: () -> Unit,
    initialSectorId: String? = null,
    initialPackage: String? = null,
    viewModel: UsageLogsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val dateOffset by viewModel.selectedDateOffset.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSectorId by viewModel.selectedSectorId.collectAsState()
    val customTimeRange by viewModel.customTimeRange.collectAsState()
    val customTimeRangeFormatted by viewModel.customTimeRangeFormatted.collectAsState()
    val selectedPackageFilter by viewModel.selectedPackageFilter.collectAsState()
    val selectedAppName by viewModel.selectedAppName.collectAsState()
    val sectors by viewModel.sectors.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val sectorStore = remember { SectorConfigStore.getInstance(context) }

    var showCalendarDialog by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }
    var showSectorEditor by remember { mutableStateOf(false) }

    LaunchedEffect(initialSectorId, initialPackage) {
        if (initialSectorId != null || !initialPackage.isNullOrBlank()) {
            viewModel.setInitialFilters(initialSectorId, initialPackage)
        }
    }

    // Accurate interval-intersection filter across ALL sectors and custom time windows
    val filteredLogs = remember(summary, searchQuery, selectedSectorId, customTimeRange, selectedPackageFilter, sectors) {
        val all = summary?.logs ?: emptyList()
        val activeSector = sectors.find { it.id == selectedSectorId }

        all.filter { entry ->
            val matchesSectorOrTime = when {
                activeSector != null -> sectorStore.isSessionInSector(entry.startTimeMs, entry.endTimeMs, activeSector)
                customTimeRange != null -> {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = entry.startTimeMs
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val midnightMs = cal.timeInMillis
                    val windowStart = midnightMs + (customTimeRange!!.first * 60 * 1000L)
                    val windowEnd = midnightMs + (customTimeRange!!.second * 60 * 1000L)
                    val effectiveEnd = maxOf(entry.endTimeMs, entry.startTimeMs + 1000L)

                    if (windowStart <= windowEnd) {
                        maxOf(entry.startTimeMs, windowStart) < minOf(effectiveEnd, windowEnd)
                    } else {
                        // Wraps midnight
                        val morningSlice = maxOf(entry.startTimeMs, midnightMs) < minOf(effectiveEnd, windowEnd)
                        val eveningSlice = maxOf(entry.startTimeMs, windowStart) < minOf(effectiveEnd, midnightMs + 24 * 3600000L)
                        morningSlice || eveningSlice
                    }
                }
                else -> true
            }

            val matchesPackage = selectedPackageFilter == null || entry.packageName == selectedPackageFilter
            val matchesSearch = searchQuery.isBlank() ||
                    entry.appName.contains(searchQuery, ignoreCase = true) ||
                    entry.packageName.contains(searchQuery, ignoreCase = true)

            matchesSectorOrTime && matchesPackage && matchesSearch
        }
    }

    // Dynamic metrics tailored to active sector/time/app filter
    val (displayScreenMinutes, displayControlledMinutes, displaySessionsCount) = remember(
        filteredLogs, summary, selectedSectorId, customTimeRange, selectedPackageFilter, searchQuery
    ) {
        val hasActiveFilter = selectedSectorId != null || customTimeRange != null || selectedPackageFilter != null || searchQuery.isNotBlank()
        if (!hasActiveFilter && summary != null) {
            Triple(summary!!.totalScreenTimeMinutes, summary!!.controlledUsageMinutes, summary!!.totalSessionsCount)
        } else {
            val nonOverlapping = filteredLogs.map { Pair(it.startTimeMs, it.endTimeMs) }.sortedBy { it.first }
            var mergedMs = 0L
            var curStart = -1L
            var curEnd = -1L
            for (interval in nonOverlapping) {
                if (curStart == -1L) {
                    curStart = interval.first
                    curEnd = interval.second
                } else if (interval.first <= curEnd) {
                    curEnd = maxOf(curEnd, interval.second)
                } else {
                    mergedMs += (curEnd - curStart)
                    curStart = interval.first
                    curEnd = interval.second
                }
            }
            if (curStart != -1L) {
                mergedMs += (curEnd - curStart)
            }
            val controlledMs = filteredLogs.filter { it.isControlled }.sumOf { it.durationMs }
            Triple((mergedMs / (60 * 1000L)).toInt(), (controlledMs / (60 * 1000L)).toInt(), filteredLogs.size)
        }
    }

    fun formatSectorTime(s: com.example.focusreminder.domain.model.TimeSector): String {
        return String.format(Locale.getDefault(), "%02d:%02d–%02d:%02d", s.startHour, s.startMinute, s.endHour, s.endMinute)
    }

    val dateDisplayLabel = when (dateOffset) {
        0 -> "Today"
        -1 -> "Yesterday"
        else -> summary?.dateLabel ?: "Day $dateOffset"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // ================= STICKY SECTION 1: TOP NAVIGATION HEADER =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuCard(
                onClick = onBack,
                cornerRadius = 12.dp,
                elevation = 3.dp
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BackArrowVectorIcon(
                        color = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AUDIT TRAIL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Usage Logbook",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            NeuCard(
                onClick = { viewModel.loadLogs() },
                cornerRadius = 12.dp,
                elevation = 3.dp
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }
        }

        // ================= STICKY SECTION 2: 1ST BOX (SUMMARY CARD LOCKED ON TOP) =================
        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL SCREEN TIME",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val hours = displayScreenMinutes / 60
                    val mins = displayScreenMinutes % 60
                    Text(
                        text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldDark
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(NeuSurfaceSunken)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CONTROLLED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val cHours = displayControlledMinutes / 60
                    val cMins = displayControlledMinutes % 60
                    Text(
                        text = if (cHours > 0) "${cHours}h ${cMins}m" else "${cMins}m",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD97706)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(NeuSurfaceSunken)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SESSIONS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$displaySessionsCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }
        }

        // ================= SCROLLABLE CONTENT (DATE STEPPER, FILTERS, SEARCH, LOGS SCROLL UP) =================
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Item 1: Multi-Day Stepper & Calendar Modal Trigger
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuPill(
                        onClick = { viewModel.stepDateOffset(-1) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("◀", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    NeuCard(
                        onClick = { showCalendarDialog = true },
                        modifier = Modifier.weight(1f),
                        cornerRadius = 14.dp,
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateDisplayLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dateOffset == 0) EmeraldDark else TextPrimary
                            )
                            Text(
                                text = "📅 Calendar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }

                    val canStepForward = dateOffset < 0
                    NeuPill(
                        onClick = { if (canStepForward) viewModel.stepDateOffset(1) },
                        modifier = Modifier.size(40.dp),
                        backgroundColor = if (canStepForward) NeuBackground else NeuBackground.copy(alpha = 0.5f)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "▶",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canStepForward) TextPrimary else TextMuted.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            // Item 2: Tier 1 Scope Bar (All Day vs Custom Window)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllDayActive = selectedSectorId == null && customTimeRange == null
                    NeuPill(
                        elevation = if (isAllDayActive) 3.dp else 1.dp,
                        borderColor = if (isAllDayActive) EmeraldDark.copy(alpha = 0.6f) else null,
                        onClick = { viewModel.resetAllTimeFilters() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All Day",
                                fontSize = 11.sp,
                                fontWeight = if (isAllDayActive) FontWeight.Black else FontWeight.Bold,
                                color = if (isAllDayActive) EmeraldDark else TextMuted
                            )
                        }
                    }

                    val isCustomActive = customTimeRange != null
                    NeuPill(
                        elevation = if (isCustomActive) 3.dp else 1.dp,
                        borderColor = if (isCustomActive) Color(0xFF8B5CF6).copy(alpha = 0.6f) else null,
                        onClick = { showTimeRangeDialog = true },
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isCustomActive) customTimeRangeFormatted ?: "Custom" else "Custom Window",
                                fontSize = 11.sp,
                                fontWeight = if (isCustomActive) FontWeight.Black else FontWeight.Bold,
                                color = if (isCustomActive) Color(0xFF8B5CF6) else TextMuted,
                                maxLines = 1
                            )
                            if (isCustomActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "×",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.clickable { viewModel.clearCustomTimeRange() }
                                )
                            }
                        }
                    }
                }
            }

            // Item 3: Tier 2 Sector Chips Bar + Edit Action
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (sector in sectors) {
                        val isSelected = selectedSectorId == sector.id
                        val chipColor = Color(sector.colorLong)
                        NeuPill(
                            elevation = if (isSelected) 3.dp else 1.dp,
                            borderColor = if (isSelected) chipColor.copy(alpha = 0.8f) else null,
                            onClick = {
                                if (isSelected) {
                                    viewModel.setSelectedSector(null)
                                } else {
                                    viewModel.setSelectedSector(sector.id)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(chipColor, CircleShape)
                                )
                                Text(
                                    text = "${sector.name} (${formatSectorTime(sector)})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) chipColor else TextMuted
                                )
                            }
                        }
                    }

                    // Edit Sectors action pill
                    NeuPill(
                        onClick = { showSectorEditor = true },
                        borderColor = EmeraldDark.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "⚙ Edit Sectors (${sectors.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Item 4: Active App Filter Banner (if redirected from Analytics)
            if (selectedPackageFilter != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "APP FILTER:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            NeuPill(
                                elevation = 2.dp,
                                borderColor = EmeraldDark.copy(alpha = 0.4f),
                                onClick = {}
                            ) {
                                Text(
                                    text = selectedAppName ?: selectedPackageFilter!!,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "Clear Filter [×]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            modifier = Modifier
                                .clickable { viewModel.clearPackageFilter() }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Item 5: Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Filter by app (e.g. YouTube)...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = EmeraldDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Item 6+: Session Entries or Empty State
            if (isLoading) {
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
            } else if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                searchQuery.isNotBlank() -> "No sessions found matching \"$searchQuery\"."
                                selectedSectorId != null -> "No sessions recorded in this sector."
                                customTimeRange != null -> "No sessions recorded in this custom time window."
                                selectedPackageFilter != null -> "No sessions recorded for this app."
                                else -> "No sessions recorded for this day."
                            },
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { entry ->
                    UsageLogItem(entry = entry, pm = pm)
                }
            }
        }
    }

    // Dialogs
    if (showCalendarDialog) {
        LogbookCalendarDialog(
            selectedDateOffset = dateOffset,
            onDismiss = { showCalendarDialog = false },
            onSelectDateOffset = { offset ->
                viewModel.setDateOffset(offset)
            }
        )
    }

    if (showTimeRangeDialog) {
        TimeRangePickerDialog(
            onDismiss = { showTimeRangeDialog = false },
            onConfirm = { sH, sM, eH, eM ->
                viewModel.setCustomTimeRange(sH, sM, eH, eM)
                showTimeRangeDialog = false
            }
        )
    }

    if (showSectorEditor) {
        SectorEditorDialog(
            initialSectors = sectors,
            onDismiss = { showSectorEditor = false },
            onSave = { updatedSectors ->
                viewModel.saveConfiguredSectors(updatedSectors)
                showSectorEditor = false
            }
        )
    }
}

@Composable
fun UsageLogItem(
    entry: UsageLogEntry,
    pm: PackageManager
) {
    val iconBitmap = remember(entry.packageName) {
        try {
            val drawable: Drawable = pm.getApplicationIcon(entry.packageName)
            drawable.toBitmap(width = 64, height = 64).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeuSurfaceSunken, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.appName.take(1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.appName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )

                    val (badgeBg, badgeColor) = when {
                        entry.isHostApp -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9)) // Purple for Blocker
                        entry.isControlled -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706)) // Amber for Controlled
                        else -> Pair(NeuSurface, TextMuted)
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.statusBadge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = badgeColor
                        )
                    }

                    if (entry.sectorName.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color(entry.sectorColorLong).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = entry.sectorName,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(entry.sectorColorLong)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = entry.timeRangeFormatted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )

                Text(
                    text = entry.packageName,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = entry.durationFormatted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (entry.isControlled) Color(0xFFD97706) else EmeraldDark
            )
        }
    }
}
