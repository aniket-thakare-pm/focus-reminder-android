package com.example.focusreminder.ui.apps

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.focusreminder.platform.AppManager
import com.example.focusreminder.platform.AppRestrictionRepository
import com.example.focusreminder.platform.InstalledAppInfo
import com.example.focusreminder.service.UsageMonitorService
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*
import com.example.focusreminder.util.formatHoursMinutes

/**
 * App Selection & Budget Configuration Screen with Neumorphic Porcelain Aesthetics.
 *
 * Top Controls:
 * - Active Apps (Contains apps that have TIMER active)
 * - Total Apps (Displays the "All Apps" section with all downloaded apps and toggle option)
 */
@Composable
fun AppSelectionScreen(
    initialTab: Int? = 0,
    onNavigateToAppAnalytics: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appManager = remember { AppManager(context) }
    val repo = remember { AppRestrictionRepository.getInstance(context) }
    val updateKey by repo.restrictionsUpdateFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selectedAppForConfig by remember { mutableStateOf<InstalledAppInfo?>(null) }

    LaunchedEffect(updateKey) {
        appsList = appManager.getLaunchableApps()
    }

    // Downloaded apps = user-installed applications (or currently restricted)
    val allDownloadedApps = remember(appsList) {
        appsList.filter { !it.isSystemApp || it.isRestricted }
    }

    // Active apps = apps that have TIMER active
    val activeApps = remember(appsList) {
        appsList.filter { it.isRestricted }
    }

    // 0 = Active Apps, 1 = Total Apps (All Apps), null = Show Both
    var selectedTab by remember(initialTab) { mutableStateOf<Int?>(initialTab) }

    val filteredActiveApps = remember(activeApps, searchQuery) {
        if (searchQuery.isBlank()) activeApps
        else activeApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredDownloadedApps = remember(allDownloadedApps, searchQuery) {
        if (searchQuery.isBlank()) allDownloadedApps
        else allDownloadedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Header
        Column {
            Text(
                text = "ATTENTION MANAGEMENT",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark,
                letterSpacing = 1.8.sp
            )
            Text(
                text = "Application Limits",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Configure timers and rolling attention budgets for installed applications.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Sunken Inset Search Bar
        NeuInsetWell(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchVectorIcon(color = TextMuted)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search Instagram, YouTube, Reddit...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        }

        // ================= TOP CONTROLS: EXACTLY TWO THINGS =================
        // Active Apps & Total Apps (Replacing old 3 items)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. ACTIVE APPS
            val isActiveSelected = selectedTab == 0
            NeuPill(
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = if (selectedTab == 0) null else 0 },
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isActiveSelected) EmeraldPrimary else Color.Transparent)
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isActiveSelected) Color.White else EmeraldPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Active Apps (${activeApps.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isActiveSelected) Color.White else TextPrimary
                    )
                }
            }

            // 2. TOTAL APPS
            val isTotalSelected = selectedTab == 1
            NeuPill(
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = if (selectedTab == 1) null else 1 },
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isTotalSelected) EmeraldPrimary else Color.Transparent)
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isTotalSelected) Color.White else TextMuted, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Total Apps (${allDownloadedApps.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isTotalSelected) Color.White else TextPrimary
                    )
                }
            }
        }

        // ================= LIST: SECTION 1 (ACTIVE APPS) & SECTION 2 (ALL APPS) =================
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // SECTION 1: ACTIVE APPS
            if (selectedTab == null || selectedTab == 0) {
                item(key = "header_active_apps") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Apps",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredActiveApps.size} with timer active",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }
                }

                if (filteredActiveApps.isEmpty()) {
                    item(key = "empty_active_apps") {
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching active timers" else "No active app timers",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Turn on the timer toggle next to any downloaded app in All Apps to start attention pacing.",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isBlank()) {
                                    NeuPill(
                                        onClick = { selectedTab = 1 },
                                        cornerRadius = 12.dp
                                    ) {
                                        Text(
                                            text = "Browse All Apps →",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldDark,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredActiveApps, key = { "active_${it.packageName}" }) { appInfo ->
                        ActiveAppRowItem(
                            app = appInfo,
                            onToggleRestricted = { isRestricted ->
                                repo.setRestricted(appInfo.packageName, appInfo.appName, isRestricted)
                                UsageMonitorService.start(context)
                                Toast.makeText(context, "${appInfo.appName} timer removed", Toast.LENGTH_SHORT).show()
                            },
                            onConfigureLimits = {
                                selectedAppForConfig = appInfo
                            },
                            onNavigateToAppAnalytics = onNavigateToAppAnalytics,
                            onToggleTaskLock = {
                                if (appInfo.isLocked) {
                                    repo.unlockApp(appInfo.packageName)
                                    Toast.makeText(context, "${appInfo.appName} unlocked", Toast.LENGTH_SHORT).show()
                                } else {
                                    repo.lockApp(appInfo.packageName, appInfo.appName, durationMinutes = 30)
                                    UsageMonitorService.start(context)
                                    Toast.makeText(context, "${appInfo.appName} locked (30m)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // SECTION 2: ALL APPS ("All Apps would be the section title")
            if (selectedTab == null || selectedTab == 1) {
                item(key = "header_all_apps") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (selectedTab == null) 16.dp else 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Apps",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredDownloadedApps.size} downloaded",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                items(filteredDownloadedApps, key = { "all_${it.packageName}" }) { appInfo ->
                    AllAppRowItem(
                        app = appInfo,
                        onToggleRestricted = { isRestricted ->
                            repo.setRestricted(appInfo.packageName, appInfo.appName, isRestricted)
                            UsageMonitorService.start(context)
                            val msg = if (isRestricted) "${appInfo.appName} timer activated" else "${appInfo.appName} timer removed"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onConfigureLimits = {
                            selectedAppForConfig = appInfo
                        },
                        onNavigateToAppAnalytics = onNavigateToAppAnalytics
                    )
                }
            }
        }
    }

    // Modal Sheet for Budget Configuration
    selectedAppForConfig?.let { app ->
        BudgetConfigDialog(
            app = app,
            onDismiss = { selectedAppForConfig = null },
            onSave = { sessionMinutes, cooldownMinutes, dailyLimit ->
                repo.setLimits(app.packageName, sessionMinutes, cooldownMinutes, dailyLimit)
                selectedAppForConfig = null
            }
        )
    }
}

/**
 * Row item for apps with active timer in the "Active Apps" section.
 * Shows configured limits, task lock button, and toggle switch.
 * Single tap opens Analytics for this app; Long press opens limits configuration.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActiveAppRowItem(
    app: InstalledAppInfo,
    onToggleRestricted: (Boolean) -> Unit,
    onConfigureLimits: () -> Unit,
    onNavigateToAppAnalytics: (String) -> Unit = {},
    onToggleTaskLock: () -> Unit
) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavigateToAppAnalytics(app.packageName) },
                onLongClick = onConfigureLimits
            ),
        cornerRadius = 24.dp,
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Icon, App Name & Timer Summary, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val bitmap = remember(app.icon) {
                        try {
                            app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = app.appName,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        NeuInsetWell(
                            modifier = Modifier.size(44.dp),
                            cornerRadius = 14.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(app.appName.take(1).uppercase(), fontWeight = FontWeight.Black, color = EmeraldDark, fontSize = 16.sp)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = app.appName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (app.isLocked) {
                                NeuInsetWell(cornerRadius = 6.dp) {
                                    Text(
                                        text = "LOCKED",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ErrorRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val dailyStr = formatHoursMinutes(app.dailyLimitMinutes)
                        Text(
                            text = "Session: ${app.rollingBudgetMinutes}m • Wait: ${app.cooldownMinutes}m • Daily: $dailyStr",
                            fontSize = 11.sp,
                            color = EmeraldDark,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = true,
                    onCheckedChange = onToggleRestricted,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFFD5DBE5)
                    )
                )
            }

            // Bottom Action Row: Edit Hint & Task Lock Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.isLocked) {
                    Text(
                        text = "Locked: ${app.remainingLockSeconds / 60}m remaining",
                        fontSize = 11.sp,
                        color = ErrorRed,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    NeuInsetWell(
                        onClick = onToggleTaskLock,
                        cornerRadius = 10.dp
                    ) {
                        Text(
                            text = "Unlock",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Tap card to edit limits • or",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    NeuInsetWell(
                        onClick = onToggleTaskLock,
                        cornerRadius = 10.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimpleLockIcon(
                                color = EmeraldDark,
                                modifier = Modifier.size(13.dp),
                                isLocked = false
                            )
                            Text(
                                text = "Lock (30m)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldDark
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row item for apps in the "All Apps" section.
 * Shows all downloaded apps with a toggle option just like requested.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppRowItem(
    app: InstalledAppInfo,
    onToggleRestricted: (Boolean) -> Unit,
    onConfigureLimits: () -> Unit,
    onNavigateToAppAnalytics: (String) -> Unit = {}
) {
    NeuCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavigateToAppAnalytics(app.packageName) },
                onLongClick = onConfigureLimits
            ),
        cornerRadius = 24.dp,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                val bitmap = remember(app.icon) {
                    try {
                        app.icon?.toBitmap(width = 96, height = 96)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.appName,
                        modifier = Modifier.size(42.dp)
                    )
                } else {
                    NeuInsetWell(
                        modifier = Modifier.size(42.dp),
                        cornerRadius = 14.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(app.appName.take(1).uppercase(), fontWeight = FontWeight.Black, color = EmeraldDark)
                        }
                    }
                }

                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (app.isRestricted) {
                        val dailyStr = formatHoursMinutes(app.dailyLimitMinutes)
                        Text(
                            text = "Timer Active • Rolling: ${app.rollingBudgetMinutes}m • Daily: $dailyStr",
                            fontSize = 11.sp,
                            color = EmeraldDark,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = app.packageName,
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Switch(
                checked = app.isRestricted,
                onCheckedChange = onToggleRestricted,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EmeraldPrimary,
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFFD5DBE5)
                )
            )
        }
    }
}

@Composable
fun BudgetConfigDialog(
    app: InstalledAppInfo,
    onDismiss: () -> Unit,
    onSave: (sessionMinutes: Int, cooldownMinutes: Int, dailyMinutes: Int) -> Unit
) {
    var sessionBudget by remember { mutableFloatStateOf(app.rollingBudgetMinutes.toFloat().coerceIn(5f, 60f)) }
    var cooldownMinutes by remember { mutableFloatStateOf(app.cooldownMinutes.toFloat().coerceIn(15f, 120f)) }
    var dailyLimit by remember { mutableFloatStateOf(app.dailyLimitMinutes.toFloat().coerceIn(30f, 360f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        title = {
            Text(
                text = "Budget: ${app.appName}",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Rule 1: Continuous Session Budget
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Session Duration", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${sessionBudget.toInt()} mins", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Black)
                    }
                    Slider(
                        value = sessionBudget,
                        onValueChange = { sessionBudget = it },
                        valueRange = 5f..45f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = NeuSunkenTrack
                        )
                    )
                    // Quick chips for session
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(5, 10, 15, 20, 30).forEach { mins ->
                            NeuInsetWell(
                                modifier = Modifier.weight(1f).clickable { sessionBudget = mins.toFloat() },
                                cornerRadius = 6.dp,
                                backgroundColor = if (sessionBudget.toInt() == mins) EmeraldDark.copy(alpha = 0.2f) else null
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 10.sp,
                                    fontWeight = if (sessionBudget.toInt() == mins) FontWeight.Black else FontWeight.Normal,
                                    color = if (sessionBudget.toInt() == mins) EmeraldDark else TextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp).align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                // Rule 2: Mandatory Cooldown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cooldown / Wait Time", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${cooldownMinutes.toInt()} mins", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Black)
                    }
                    Slider(
                        value = cooldownMinutes,
                        onValueChange = { cooldownMinutes = it },
                        valueRange = 15f..90f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = NeuSunkenTrack
                        )
                    )
                    // Quick chips for cooldown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            NeuInsetWell(
                                modifier = Modifier.weight(1f).clickable { cooldownMinutes = mins.toFloat() },
                                cornerRadius = 6.dp,
                                backgroundColor = if (cooldownMinutes.toInt() == mins) EmeraldDark.copy(alpha = 0.2f) else null
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 10.sp,
                                    fontWeight = if (cooldownMinutes.toInt() == mins) FontWeight.Black else FontWeight.Normal,
                                    color = if (cooldownMinutes.toInt() == mins) EmeraldDark else TextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp).align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                // Rule 3: Daily Total Limit
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Limit", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        val h = dailyLimit.toInt() / 60
                        val m = dailyLimit.toInt() % 60
                        Text("${h}h ${String.format("%02d", m)}m", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Black)
                    }
                    Slider(
                        value = dailyLimit,
                        onValueChange = { dailyLimit = it },
                        valueRange = 30f..240f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = NeuSunkenTrack
                        )
                    )
                }

                // Summary Note
                NeuInsetWell(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 10.dp
                ) {
                    Text(
                        text = "Use for up to ${sessionBudget.toInt()}m, then locked for ${cooldownMinutes.toInt()}m. Full-screen entry contract and live chronometer timer will be active.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 16.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(sessionBudget.toInt(), cooldownMinutes.toInt(), dailyLimit.toInt()) }) {
                Text("Save Changes", color = EmeraldDark, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
