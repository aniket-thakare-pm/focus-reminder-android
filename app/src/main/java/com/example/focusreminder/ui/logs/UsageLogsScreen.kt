package com.example.focusreminder.ui.logs

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

private data class SectorFilterOption(
    val id: String?,
    val name: String,
    val timeRange: String,
    val colorLong: Long
)

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
    val selectedPackageFilter by viewModel.selectedPackageFilter.collectAsState()
    val selectedAppName by viewModel.selectedAppName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val pm = remember { context.packageManager }

    LaunchedEffect(initialSectorId, initialPackage) {
        if (initialSectorId != null || !initialPackage.isNullOrBlank()) {
            viewModel.setInitialFilters(initialSectorId, initialPackage)
        }
    }

    val filteredLogs = remember(summary, searchQuery, selectedSectorId, selectedPackageFilter) {
        val all = summary?.logs ?: emptyList()
        all.filter { entry ->
            val matchesSector = selectedSectorId == null || entry.sectorId == selectedSectorId
            val matchesPackage = selectedPackageFilter == null || entry.packageName == selectedPackageFilter
            val matchesSearch = searchQuery.isBlank() ||
                    entry.appName.contains(searchQuery, ignoreCase = true) ||
                    entry.packageName.contains(searchQuery, ignoreCase = true)
            matchesSector && matchesPackage && matchesSearch
        }
    }

    // Dynamic metrics tailored to active sector/app filter
    val (displayScreenMinutes, displayControlledMinutes, displaySessionsCount) = remember(
        filteredLogs, summary, selectedSectorId, selectedPackageFilter, searchQuery
    ) {
        val hasActiveFilter = selectedSectorId != null || selectedPackageFilter != null || searchQuery.isNotBlank()
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

    val sectorOptions = remember {
        listOf(
            SectorFilterOption(null, "Any Time", "All Day", 0xFF64748BL),
            SectorFilterOption("sector_morning", "Morning", "06:00–12:00", 0xFF10B981L),
            SectorFilterOption("sector_afternoon", "Afternoon", "12:00–17:00", 0xFFF59E0BL),
            SectorFilterOption("sector_evening", "Evening", "17:00–22:00", 0xFF8B5CF6L),
            SectorFilterOption("sector_night", "Night", "22:00–06:00", 0xFFEC4899L)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Top Navigation Header
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

        // Summary Card (Dynamically updates based on active filters)
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

        // Date Stepper Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuPill(
                modifier = Modifier.weight(1f),
                elevation = if (dateOffset == 0) 4.dp else 1.dp,
                borderColor = if (dateOffset == 0) EmeraldDark.copy(alpha = 0.4f) else null,
                onClick = { viewModel.setDateOffset(0) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Today",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dateOffset == 0) EmeraldDark else TextMuted
                    )
                }
            }

            NeuPill(
                modifier = Modifier.weight(1f),
                elevation = if (dateOffset == -1) 4.dp else 1.dp,
                borderColor = if (dateOffset == -1) EmeraldDark.copy(alpha = 0.4f) else null,
                onClick = { viewModel.setDateOffset(-1) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yesterday",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dateOffset == -1) EmeraldDark else TextMuted
                    )
                }
            }
        }

        // Sector Filter Bar (Any Time | Morning | Afternoon | Evening | Night)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (option in sectorOptions) {
                val isSelected = selectedSectorId == option.id
                val chipColor = if (option.id != null) Color(option.colorLong) else TextPrimary
                NeuPill(
                    elevation = if (isSelected) 4.dp else 1.dp,
                    borderColor = if (isSelected) chipColor.copy(alpha = 0.6f) else null,
                    onClick = { viewModel.setSelectedSector(option.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (option.id != null) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(chipColor, CircleShape)
                            )
                        }
                        Text(
                            text = option.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) chipColor else TextMuted
                        )
                    }
                }
            }
        }

        // Active App Filter Banner (if redirected with a specific app)
        if (selectedPackageFilter != null) {
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

        // Search Input
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

        // Session List
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.dp)
            }
        } else if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        searchQuery.isNotBlank() -> "No sessions found matching \"$searchQuery\"."
                        selectedSectorId != null -> "No sessions recorded in this sector."
                        selectedPackageFilter != null -> "No sessions recorded for this app."
                        else -> "No sessions recorded for this day."
                    },
                    fontSize = 12.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredLogs.size, key = { filteredLogs[it].id }) { index ->
                    val entry = filteredLogs[index]
                    UsageLogCard(entry = entry, pm = pm)
                }
            }
        }
    }
}

@Composable
fun UsageLogCard(
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
