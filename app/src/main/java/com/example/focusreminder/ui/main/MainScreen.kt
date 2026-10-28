package com.example.focusreminder.ui.main

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.example.focusreminder.platform.AppManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppNavigationTab {
    TIMER,
    ANALYTICS,
    APPS,
    ALERTS
}

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentTab by remember { mutableStateOf(AppNavigationTab.TIMER) }
    var subScreenOriginTab by remember { mutableStateOf(AppNavigationTab.TIMER) }

    var analyticsFocusedPackage by remember { mutableStateOf<String?>(null) }
    var appSelectionInitialTab by remember { mutableStateOf<Int?>(0) }

    var showUsageLogsScreen by remember { mutableStateOf(false) }
    var usageLogsInitialSector by remember { mutableStateOf<String?>(null) }
    var usageLogsInitialPackage by remember { mutableStateOf<String?>(null) }

    var showJournalScreen by remember { mutableStateOf(false) }

    // Pre-warm AppManager cache in background on startup (< 30ms warm-up)
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            AppManager(context).getLaunchableApps()
        }
    }

    // Android System Back Navigation Handler
    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (showUsageLogsScreen) {
            showUsageLogsScreen = false
            usageLogsInitialSector = null
            usageLogsInitialPackage = null
            currentTab = subScreenOriginTab
        } else if (showJournalScreen) {
            showJournalScreen = false
            currentTab = subScreenOriginTab
        } else if (analyticsFocusedPackage != null) {
            analyticsFocusedPackage = null
        } else if (currentTab != AppNavigationTab.TIMER) {
            currentTab = AppNavigationTab.TIMER
        } else {
            (context as? Activity)?.finish()
        }
    }

    val openDrawerAction: () -> Unit = {
        coroutineScope.launch {
            if (drawerState.isClosed) drawerState.open() else drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                onSelectLogs = {
                    coroutineScope.launch { drawerState.close() }
                    subScreenOriginTab = currentTab
                    usageLogsInitialSector = null
                    usageLogsInitialPackage = null
                    showUsageLogsScreen = true
                },
                onSelectJournal = {
                    coroutineScope.launch { drawerState.close() }
                    subScreenOriginTab = currentTab
                    showJournalScreen = true
                }
            )
        }
    ) {
        if (showUsageLogsScreen) {
            UsageLogsScreen(
                initialSectorId = usageLogsInitialSector,
                initialPackage = usageLogsInitialPackage,
                onBack = {
                    showUsageLogsScreen = false
                    usageLogsInitialSector = null
                    usageLogsInitialPackage = null
                    currentTab = subScreenOriginTab
                },
                modifier = modifier.fillMaxSize()
            )
        } else if (showJournalScreen) {
            JournalScreen(
                onBack = {
                    showJournalScreen = false
                    currentTab = subScreenOriginTab
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
                    // Screen Content
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
                                onOpenUsageLogs = {
                                    subScreenOriginTab = AppNavigationTab.TIMER
                                    usageLogsInitialSector = null
                                    usageLogsInitialPackage = null
                                    showUsageLogsScreen = true
                                },
                                onOpenDrawer = openDrawerAction,
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
                                    subScreenOriginTab = AppNavigationTab.ANALYTICS
                                    usageLogsInitialSector = sectorId
                                    usageLogsInitialPackage = pkg
                                    showUsageLogsScreen = true
                                },
                                onOpenDrawer = openDrawerAction
                            )
                            AppNavigationTab.APPS -> AppSelectionScreen(
                                initialTab = appSelectionInitialTab,
                                onNavigateToAppAnalytics = { pkg ->
                                    analyticsFocusedPackage = pkg
                                    currentTab = AppNavigationTab.ANALYTICS
                                },
                                onOpenDrawer = openDrawerAction
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
                                },
                                onOpenDrawer = openDrawerAction
                            )
                        }
                    }

                    // ================= NEUMORPHIC FLOATING BOTTOM NAVIGATION (4 CLEAN TABS) =================
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
