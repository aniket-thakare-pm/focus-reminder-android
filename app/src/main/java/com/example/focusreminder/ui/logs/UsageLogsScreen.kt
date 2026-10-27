package com.example.focusreminder.ui.logs

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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

@Composable
fun UsageLogsScreen(
    onBack: () -> Unit,
    viewModel: UsageLogsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summary.collectAsState()
    val dateOffset by viewModel.selectedDateOffset.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val pm = remember { context.packageManager }

    val filteredLogs = remember(summary, searchQuery) {
        val all = summary?.logs ?: emptyList()
        if (searchQuery.isBlank()) {
            all
        } else {
            all.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Summary Card
        summary?.let { s ->
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
                        val hours = s.totalScreenTimeMinutes / 60
                        val mins = s.totalScreenTimeMinutes % 60
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
                        val cHours = s.controlledUsageMinutes / 60
                        val cMins = s.controlledUsageMinutes % 60
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
                            text = "${s.totalSessionsCount}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Date Toggle Row & Search Bar
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
                        .padding(vertical = 10.dp),
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
                        .padding(vertical = 10.dp),
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
                    text = if (searchQuery.isNotBlank()) "No sessions found matching \"$searchQuery\"." else "No sessions recorded for this day.",
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
