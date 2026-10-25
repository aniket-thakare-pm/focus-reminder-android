package com.example.focusreminder.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.platform.AppRestrictionRepository
import com.example.focusreminder.platform.ForegroundAppDetector
import com.example.focusreminder.platform.UsagePermissionHelper
import com.example.focusreminder.service.UsageMonitorService
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*
import com.example.focusreminder.util.formatHoursMinutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Main Application Dashboard featuring a Neumorphic Sunken Dial & Tactile Soft UI Cards.
 */
@Composable
fun DashboardScreen(
    onNavigateToApps: () -> Unit,
    onTestBlocker: () -> Unit,
    onTestStickyReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { AppRestrictionRepository.getInstance(context) }
    val updateKey by repo.restrictionsUpdateFlow.collectAsState()
    val detector = remember { ForegroundAppDetector(context) }

    var hasUsagePermission by remember { mutableStateOf(UsagePermissionHelper.hasUsageAccessPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(UsagePermissionHelper.hasOverlayPermission(context)) }

    // Start background service if permissions are granted
    LaunchedEffect(hasUsagePermission, hasOverlayPermission) {
        if (hasUsagePermission && hasOverlayPermission) {
            UsageMonitorService.start(context)
        }
    }

    val restrictedPackages = remember(updateKey) { repo.getRestrictedPackages().toList() }

    // Real live app usage & device screen time from UsageStatsManager
    var actualAppUsageMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalDeviceScreenTimeMin by remember { mutableStateOf(0) }

    // Continuous live refresh every 3 seconds from OS UsageStatsManager
    LaunchedEffect(hasUsagePermission, updateKey, restrictedPackages) {
        while (isActive) {
            val usageGranted = UsagePermissionHelper.hasUsageAccessPermission(context)
            val overlayGranted = UsagePermissionHelper.hasOverlayPermission(context)
            hasUsagePermission = usageGranted
            hasOverlayPermission = overlayGranted

            if (usageGranted) {
                withContext(Dispatchers.IO) {
                    val usageMap = detector.getDailyUsageMinutesMap(restrictedPackages)
                    val deviceTotal = detector.getTotalScreenTimeTodayMinutes()
                    withContext(Dispatchers.Main) {
                        actualAppUsageMap = usageMap
                        totalDeviceScreenTimeMin = deviceTotal
                    }
                }
            }
            delay(3000L)
        }
    }

    // Hero timer calculations
    val (heroUsedMin, heroBudgetMin) = remember(restrictedPackages, actualAppUsageMap, totalDeviceScreenTimeMin) {
        if (restrictedPackages.isNotEmpty()) {
            val totalBudget = restrictedPackages.sumOf { repo.getDailyLimitMinutes(it) }.coerceAtLeast(1)
            val totalUsed = restrictedPackages.sumOf { actualAppUsageMap[it] ?: 0 }
            Pair(totalUsed, totalBudget)
        } else {
            // Default when no restricted apps: device screen time vs 2h 00m target
            Pair(totalDeviceScreenTimeMin, 120)
        }
    }

    val heroRemainingMin = maxOf(0, heroBudgetMin - heroUsedMin)
    val heroFractionLeft = (heroRemainingMin.toFloat() / heroBudgetMin.toFloat()).coerceIn(0f, 1f)
    val heroSweepAngle = (360f * heroFractionLeft).coerceIn(4f, 360f)
    val heroRemainingHours = heroRemainingMin / 60
    val heroRemainingRemainderMin = heroRemainingMin % 60
    val heroTimeDisplay = String.format("%02d:%02d", heroRemainingHours, heroRemainingRemainderMin)
    val heroArcColor = when {
        heroRemainingMin == 0 -> ErrorRed
        heroFractionLeft <= 0.20f -> ErrorRed
        heroFractionLeft <= 0.40f -> Color(0xFFD97706)
        else -> EmeraldPrimary
    }

    // Gentle pulse animation for the central puck
    val infiniteTransition = rememberInfiniteTransition(label = "timer-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DAILY ATTENTION PACING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Focus Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-0.8).sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Toggle Pill (Dark Obsidian / Light Porcelain Soft UI)
                NeuPill(
                    onClick = { ThemeManager.toggleTheme() },
                    cornerRadius = 14.dp,
                    elevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.size(38.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeToggleIcon(
                            isDark = ThemeManager.isDarkTheme,
                            color = if (ThemeManager.isDarkTheme) Color(0xFFFBBF24) else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                NeuPill(
                    modifier = Modifier.padding(2.dp),
                    onClick = onNavigateToApps
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(EmeraldPrimary, CircleShape))
                        Text(
                            text = "+ Add App",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                    }
                }
            }
        }

        // ================= PERMISSION BANNER =================
        if (!hasUsagePermission || !hasOverlayPermission) {
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(
                            text = "Permissions Required for Focus Enforcement",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }

                    Text(
                        text = "Usage Access is needed to track foreground time, and Overlay permission is needed to show the intentional lock screen.",
                        fontSize = 11.sp,
                        color = Color(0xFF7F1D1D)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!hasUsagePermission) {
                            Button(
                                onClick = {
                                    val intent = UsagePermissionHelper.createUsageAccessSettingsIntent(context)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF991B1B)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("1. Usage Access", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!hasOverlayPermission) {
                            Button(
                                onClick = {
                                    val intent = UsagePermissionHelper.createOverlaySettingsIntent(context)
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF991B1B)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Text("2. Overlay Access", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ================= HERO: SUNKEN NEUMORPHIC TIMER CARD =================
        // (Directly modeled on the circular clock well from Reference Image 2)
        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 32.dp,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (restrictedPackages.isNotEmpty()) "Monitored Screen Budget" else "Daily Screen Budget",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${formatHoursMinutes(heroUsedMin)} / ${formatHoursMinutes(heroBudgetMin)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (heroUsedMin > heroBudgetMin) ErrorRed else EmeraldDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The Sunken Inset Circular Well
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    NeuCircle(
                        modifier = Modifier.size(200.dp),
                        isInset = true
                    ) {
                        val trackColor = NeuSunkenTrack
                        // Progress Arc inside the well
                        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            // Indented Track
                            drawCircle(
                                color = trackColor,
                                radius = size.minDimension / 2.3f,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            // Active Dynamic Progress Arc
                            drawArc(
                                color = heroArcColor,
                                startAngle = -90f,
                                sweepAngle = heroSweepAngle,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Center Extruded Tactile Puck (Floating inside the well)
                        NeuCircle(
                            modifier = Modifier.size(130.dp),
                            elevation = 6.dp,
                            isInset = false
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.scale(pulseScale)
                            ) {
                                Text(
                                    text = heroTimeDisplay,
                                    fontSize = 28.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = if (heroRemainingMin == 0 && heroUsedMin >= heroBudgetMin) ErrorRed else TextPrimary
                                )
                                Text(
                                    text = if (heroRemainingMin == 0 && heroUsedMin >= heroBudgetMin) "EXHAUSTED" else "REMAINING",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (heroRemainingMin == 0 && heroUsedMin >= heroBudgetMin) ErrorRed else EmeraldDark,
                                    letterSpacing = 1.6.sp
                                )
                                Text(
                                    text = if (heroUsedMin >= heroBudgetMin) "0% left" else "${(heroFractionLeft * 100).toInt()}% left",
                                    fontSize = 9.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Inset Metrics Row (Like Reference Image 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeuInsetWell(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ACTIVE PACING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text("Balanced", fontSize = 11.sp, fontWeight = FontWeight.Black, color = EmeraldDark)
                        }
                    }

                    NeuInsetWell(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL SCREEN", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            val devHours = totalDeviceScreenTimeMin / 60
                            val devMins = totalDeviceScreenTimeMin % 60
                            Text("${devHours}h ${devMins}m", fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }

                    NeuInsetWell(
                        modifier = Modifier.weight(1f),
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MONITORED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text("${restrictedPackages.size} Apps", fontSize = 11.sp, fontWeight = FontWeight.Black, color = EmeraldDark, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // ================= MONITORED APPLICATIONS LIST =================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monitored Apps & Task Locks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "${restrictedPackages.size} Active",
                    fontSize = 11.sp,
                    color = EmeraldDark,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            if (restrictedPackages.isEmpty()) {
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("No applications restricted yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Text("Tap below to quick-add distraction targets:", fontSize = 11.sp, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NeuPill(
                                onClick = {
                                    repo.setRestricted("com.instagram.android", "Instagram", true)
                                    UsageMonitorService.start(context)
                                }
                            ) {
                                Text("+ Instagram", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                            NeuPill(
                                onClick = {
                                    repo.setRestricted("com.google.android.youtube", "YouTube", true)
                                    UsageMonitorService.start(context)
                                }
                            ) {
                                Text("+ YouTube", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldDark, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                            }
                        }
                    }
                }
            } else {
                restrictedPackages.forEach { pkg ->
                    val appName = repo.getAppName(pkg, pkg.substringAfterLast("."))
                    val dailyMin = repo.getDailyLimitMinutes(pkg)
                    val isLocked = repo.isAppLocked(pkg)
                    val remainingSecs = repo.getRemainingLockSeconds(pkg)

                    // Real tracked daily usage from OS UsageStatsManager
                    val dailyUsedMin = actualAppUsageMap[pkg] ?: 0
                    val dailyLimitMin = dailyMin.coerceAtLeast(1)
                    val progress = (dailyUsedMin.toFloat() / dailyLimitMin.toFloat()).coerceIn(0f, 1f)
                    val progressColor = when {
                        progress >= 0.85f -> ErrorRed
                        progress >= 0.60f -> Color(0xFFD97706) // Yellow / Amber
                        else -> EmeraldDark // Green
                    }

                    // Embossed Outer Box (Golden Rule: Outer is Embossed)
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // TOP ROW: Debossed App Icon, App Name & Daily Usage, Debossed Lock Button
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
                                    // Debossed App Icon (Inside Embossed Card)
                                    NeuInsetWell(
                                        modifier = Modifier.size(46.dp),
                                        cornerRadius = 14.dp
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (isLocked) {
                                                SimpleLockIcon(color = ErrorRed, modifier = Modifier.size(20.dp), isLocked = true)
                                            } else {
                                                Text(
                                                    text = appName.take(2).uppercase(),
                                                    fontWeight = FontWeight.Black,
                                                    color = EmeraldDark,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }

                                    // App Info & Daily Progress Text (Rolling time skipped per user spec)
                                    Column(modifier = Modifier.padding(end = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(appName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            if (isLocked) {
                                                NeuInsetWell(
                                                    cornerRadius = 6.dp
                                                ) {
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
                                        if (isLocked) {
                                            if (dailyUsedMin >= dailyLimitMin) {
                                                Text(
                                                    text = "${formatHoursMinutes(dailyUsedMin)} / ${formatHoursMinutes(dailyLimitMin)} • Locked until midnight",
                                                    fontSize = 11.sp,
                                                    color = ErrorRed,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text("${remainingSecs / 60}m remaining in lock", fontSize = 11.sp, color = ErrorRed, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text(
                                                text = "${formatHoursMinutes(dailyUsedMin)} / ${formatHoursMinutes(dailyLimitMin)} daily",
                                                fontSize = 11.sp,
                                                color = progressColor,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Debossed Lock Button (Golden Rule: Debossed inside Embossed Card; Simple Icon, Zero Emojis, No Text!)
                                NeuInsetWell(
                                    modifier = Modifier.size(46.dp),
                                    cornerRadius = 14.dp,
                                    onClick = {
                                        if (isLocked) {
                                            repo.unlockApp(pkg)
                                            Toast.makeText(context, "$appName unlocked", Toast.LENGTH_SHORT).show()
                                        } else {
                                            repo.lockApp(pkg, appName, durationMinutes = 30)
                                            UsageMonitorService.start(context)
                                            Toast.makeText(context, "$appName locked for 30 minutes", Toast.LENGTH_LONG).show()
                                            onTestBlocker()
                                        }
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        SimpleLockIcon(
                                            color = if (isLocked) ErrorRed else EmeraldDark,
                                            modifier = Modifier.size(20.dp),
                                            isLocked = isLocked
                                        )
                                    }
                                }
                            }

                            // BOTTOM ROW: Debossed Daily Limit Progress Bar
                            // Spans from left of IG icon to right of Lock icon!
                            NeuInsetWell(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                cornerRadius = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.coerceAtLeast(0.04f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(progressColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ================= UPCOMING P0 STICKY ALERT CARD =================
        // Embossed card with debossed internal action button (Golden Rule)
        NeuCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NEXT P0 STICKY ALERT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldDark,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "09:45 AM",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Prepare Sprint Demo Deck",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )

                Text(
                    text = "Demands deliberate acknowledgement • Full-screen sticky alert",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Debossed Action Button inside the Embossed Card
                NeuInsetWell(
                    modifier = Modifier.align(Alignment.End),
                    cornerRadius = 12.dp,
                    onClick = onTestStickyReminder
                ) {
                    Text(
                        text = "Preview Sticky Alert →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(75.dp)) // Padding for bottom nav
    }
}
