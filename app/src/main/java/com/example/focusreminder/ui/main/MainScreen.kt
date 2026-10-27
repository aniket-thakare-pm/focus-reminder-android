package com.example.focusreminder.ui.main

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.focusreminder.platform.ReminderScheduler
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.analytics.AnalyticsScreen
import com.example.focusreminder.ui.apps.AppSelectionScreen
import com.example.focusreminder.ui.blocker.BlockerActivity
import com.example.focusreminder.ui.components.*
import com.example.focusreminder.ui.dashboard.DashboardScreen
import com.example.focusreminder.ui.journal.JournalScreen
import com.example.focusreminder.ui.logs.UsageLogsScreen
import com.example.focusreminder.ui.reminder.StickyReminderActivity
import com.example.focusreminder.ui.reminders.RemindersScreen

enum class AppNavigationTab {
    TIMER,
    ANALYTICS,
    APPS,
    ALERTS,
    JOURNAL
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppNavigationTab.TIMER) }
    var analyticsFocusedPackage by remember { mutableStateOf<String?>(null) }
    var appSelectionInitialTab by remember { mutableStateOf<Int?>(0) }
    var showUsageLogsScreen by remember { mutableStateOf(false) }
    var usageLogsInitialSector by remember { mutableStateOf<String?>(null) }
    var usageLogsInitialPackage by remember { mutableStateOf<String?>(null) }

    if (showUsageLogsScreen) {
        UsageLogsScreen(
            initialSectorId = usageLogsInitialSector,
            initialPackage = usageLogsInitialPackage,
            onBack = {
                showUsageLogsScreen = false
                usageLogsInitialSector = null
                usageLogsInitialPackage = null
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        BioluminescentBackground(showGrid = false) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Screen Content (Adapts cleanly to full screen width)
                Crossfade(
                    targetState = currentTab,
                    label = "tab-crossfade",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        AppNavigationTab.TIMER -> DashboardScreen(
                            onNavigateToApps = {
                                appSelectionInitialTab = 1
                                currentTab = AppNavigationTab.APPS
                            },
                            onNavigateToAlerts = { currentTab = AppNavigationTab.ALERTS },
                            onNavigateToAnalytics = { currentTab = AppNavigationTab.ANALYTICS },
                            onNavigateToAppAnalytics = { pkg ->
                                analyticsFocusedPackage = pkg
                                currentTab = AppNavigationTab.ANALYTICS
                            },
                            onOpenUsageLogs = { showUsageLogsScreen = true },
                        onTestBlocker = {
                            val intent = BlockerActivity.createIntent(
                                context = context,
                                appName = "Instagram",
                                isDailyLimit = false,
                                cooldownSeconds = 1800L
                            )
                            context.startActivity(intent)
                        },
                        onTestStickyReminder = {
                            val triggerTime = System.currentTimeMillis() + 5000L
                            ReminderScheduler.scheduleReminder(
                                context = context,
                                reminderId = 101L,
                                title = "Prepare Sprint Demo Deck",
                                description = "Review the latest architecture slides and compile test metrics for the engineering sync.",
                                triggerAtMs = triggerTime,
                                isP0 = true
                            )
                            Toast.makeText(context, "Sticky P0 Alert scheduled in 5 seconds! Lock your phone to test.", Toast.LENGTH_LONG).show()
                        }
                    )
                    AppNavigationTab.ANALYTICS -> AnalyticsScreen(
                        initialPackage = analyticsFocusedPackage,
                        onClearInitialPackage = { analyticsFocusedPackage = null },
                        onNavigateToUsageLogs = { sectorId, pkg ->
                            usageLogsInitialSector = sectorId
                            usageLogsInitialPackage = pkg
                            showUsageLogsScreen = true
                        }
                    )
                    AppNavigationTab.APPS -> AppSelectionScreen(
                        initialTab = appSelectionInitialTab,
                        onNavigateToAppAnalytics = { pkg ->
                            analyticsFocusedPackage = pkg
                            currentTab = AppNavigationTab.ANALYTICS
                        }
                    )
                    AppNavigationTab.ALERTS -> RemindersScreen(
                        onPreviewSticky = {
                            val intent = StickyReminderActivity.createIntent(
                                context = context,
                                id = 101L,
                                title = "Prepare Sprint Demo Deck",
                                description = "Review the latest architecture slides and compile test metrics for the engineering sync.",
                                time = "09:45 AM"
                            )
                            context.startActivity(intent)
                        }
                    )
                    AppNavigationTab.JOURNAL -> JournalScreen()
                }
            }

            // ================= NEUMORPHIC FLOATING BOTTOM NAVIGATION (ICONS ONLY, NO EMOJIS) =================
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    elevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 1: Timer
                        NeuIconNavButton(
                            isSelected = currentTab == AppNavigationTab.TIMER,
                            onClick = { currentTab = AppNavigationTab.TIMER }
                        ) { color ->
                            NavTimerIcon(color = color)
                        }

                        // Tab 2: Analytics
                        NeuIconNavButton(
                            isSelected = currentTab == AppNavigationTab.ANALYTICS,
                            onClick = { currentTab = AppNavigationTab.ANALYTICS }
                        ) { color ->
                            NavAnalyticsIcon(color = color)
                        }

                        // Tab 3: Apps
                        NeuIconNavButton(
                            isSelected = currentTab == AppNavigationTab.APPS,
                            onClick = {
                                appSelectionInitialTab = 0
                                currentTab = AppNavigationTab.APPS
                            }
                        ) { color ->
                            NavAppsIcon(color = color)
                        }

                        // Tab 4: Alerts
                        NeuIconNavButton(
                            isSelected = currentTab == AppNavigationTab.ALERTS,
                            onClick = { currentTab = AppNavigationTab.ALERTS }
                        ) { color ->
                            NavAlertsIcon(color = color)
                        }

                        // Tab 5: Journal
                        NeuIconNavButton(
                            isSelected = currentTab == AppNavigationTab.JOURNAL,
                            onClick = { currentTab = AppNavigationTab.JOURNAL }
                        ) { color ->
                            NavJournalIcon(color = color)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun NeuIconNavButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    if (isSelected) {
        NeuInsetWell(
            modifier = Modifier.size(52.dp),
            cornerRadius = 16.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                icon(EmeraldDark)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            icon(TextMuted)
        }
    }
}
