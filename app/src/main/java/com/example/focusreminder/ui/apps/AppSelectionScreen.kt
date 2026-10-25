package com.example.focusreminder.ui.apps

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.focusreminder.platform.AppManager
import com.example.focusreminder.platform.AppRestrictionRepository
import com.example.focusreminder.platform.InstalledAppInfo
import com.example.focusreminder.service.UsageMonitorService
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

/**
 * App Selection & Budget Configuration Screen with Neumorphic Porcelain Aesthetics.
 */
@Composable
fun AppSelectionScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appManager = remember { AppManager(context) }
    val repo = remember { AppRestrictionRepository.getInstance(context) }
    val updateKey by repo.restrictionsUpdateFlow.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = User Apps, 1 = Presets, 2 = All Apps
    var appsList by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selectedAppForConfig by remember { mutableStateOf<InstalledAppInfo?>(null) }

    LaunchedEffect(updateKey) {
        appsList = appManager.getLaunchableApps()
    }

    val popularPresets = remember { appManager.getPopularDistractionApps() }

    val filteredApps = appsList.filter { app ->
        val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesTab = when (selectedTab) {
            0 -> !app.isSystemApp || app.isRestricted
            1 -> true
            else -> true
        }
        matchesSearch && matchesTab
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
                text = "FEATURE A: RESTRICTIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark,
                letterSpacing = 1.8.sp
            )
            Text(
                text = "Monitored Applications",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Select installed apps or presets subject to rolling & daily limits.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Sunken Inset Search Bar (Like Reference Image 3)
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

        // Category Filter Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("User Apps", "Presets", "All Apps").forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                NeuPill(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = index }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // Popular Presets Quick-Add Strip (if on Presets tab)
        if (selectedTab == 1) {
            Text(
                text = "Tap to immediately add and lock popular distraction targets:",
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularPresets) { (pkg, name) ->
                    val isAlreadyRestricted = repo.isRestricted(pkg)
                    NeuPill(
                        onClick = {
                            val nextState = !isAlreadyRestricted
                            repo.setRestricted(pkg, name, nextState)
                            UsageMonitorService.start(context)
                            val statusMsg = if (nextState) "Restricted" else "Unrestricted"
                            Toast.makeText(context, "$name is now $statusMsg", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(if (isAlreadyRestricted) "✓" else "+", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Black)
                            Text(name, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // App List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(filteredApps, key = { it.packageName }) { appInfo ->
                AppRowItem(
                    app = appInfo,
                    onToggleRestricted = { isRestricted ->
                        repo.setRestricted(appInfo.packageName, appInfo.appName, isRestricted)
                        UsageMonitorService.start(context)
                    },
                    onConfigureLimits = {
                        selectedAppForConfig = appInfo
                    },
                    onToggleTaskLock = {
                        if (appInfo.isLocked) {
                            repo.unlockApp(appInfo.packageName)
                            Toast.makeText(context, "${appInfo.appName} unlocked", Toast.LENGTH_SHORT).show()
                        } else {
                            repo.lockApp(appInfo.packageName, appInfo.appName, durationMinutes = 30)
                            UsageMonitorService.start(context)
                            Toast.makeText(
                                context,
                                "App locked: ${appInfo.appName} (30m)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    // Modal Sheet for Budget Configuration
    selectedAppForConfig?.let { app ->
        BudgetConfigDialog(
            app = app,
            onDismiss = { selectedAppForConfig = null },
            onSave = { rollingBudget, dailyLimit ->
                repo.setLimits(app.packageName, rollingBudget, dailyLimit)
                selectedAppForConfig = null
            }
        )
    }
}

@Composable
fun AppRowItem(
    app: InstalledAppInfo,
    onToggleRestricted: (Boolean) -> Unit,
    onConfigureLimits: () -> Unit,
    onToggleTaskLock: () -> Unit
) {
    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        onClick = if (app.isRestricted) onConfigureLimits else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = app.appName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (app.isSystemApp) {
                                NeuInsetWell(cornerRadius = 4.dp) {
                                    Text("SYSTEM", fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }

                        if (app.isRestricted) {
                            Text(
                                text = "Rolling: ${app.rollingBudgetMinutes}m • Daily: ${app.dailyLimitMinutes / 60}h ${app.dailyLimitMinutes % 60}m",
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

            // Task Lock Row (if restricted) - Golden Rule: Debossed well inside Embossed card
            if (app.isRestricted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (app.isLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SimpleLockIcon(
                                color = ErrorRed,
                                modifier = Modifier.size(13.dp),
                                isLocked = true
                            )
                            Text(
                                text = "Locked: ${app.remainingLockSeconds / 60}m remaining",
                                fontSize = 11.sp,
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                            text = "Tap to configure limits • or",
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
}

@Composable
fun BudgetConfigDialog(
    app: InstalledAppInfo,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var rollingBudget by remember { mutableFloatStateOf(app.rollingBudgetMinutes.toFloat()) }
    var dailyLimit by remember { mutableFloatStateOf(app.dailyLimitMinutes.toFloat()) }

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure maximum continuous rolling usage and total daily screen budget.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                // Rolling Window Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Continuous Session Budget", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${rollingBudget.toInt()} mins", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Black)
                    }
                    Slider(
                        value = rollingBudget,
                        onValueChange = { rollingBudget = it },
                        valueRange = 10f..60f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = NeuSunkenTrack
                        )
                    )
                }

                // Daily Limit Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Total Allowance", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        val h = dailyLimit.toInt() / 60
                        val m = dailyLimit.toInt() % 60
                        Text("${h}h ${m}m", fontSize = 12.sp, color = EmeraldDark, fontWeight = FontWeight.Black)
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
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rollingBudget.toInt(), dailyLimit.toInt()) }) {
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
