package com.example.focusreminder.ui.squad

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.SquadInfo
import com.example.focusreminder.platform.auth.AuthManager
import com.example.focusreminder.platform.squad.SquadRepository
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.BackArrowVectorIcon
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuPill
import kotlinx.coroutines.launch

@Composable
fun SquadScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val squadRepo = remember { SquadRepository.getInstance(context) }
    val authManager = remember { AuthManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var squadInfo by remember { mutableStateOf<SquadInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showActionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val data = squadRepo.getSquadInfo()
        squadInfo = data
        isLoading = false
    }

    // Realtime listener: auto-refreshes squad members when friends join or update
    LaunchedEffect(squadInfo?.id) {
        val currentId = squadRepo.activeSquadId
        if (currentId != null) {
            val syncService = com.example.focusreminder.platform.squad.FirebaseSquadSyncService.getInstance(context)
            syncService.observeSquadMembers(currentId).collect {
                squadInfo = squadRepo.getSquadInfo()
            }
        }
    }

    if (showActionDialog) {
        SquadActionDialog(
            currentDisplayName = authManager.getDisplayName(),
            onDismiss = { showActionDialog = false },
            onJoin = { code, displayName ->
                coroutineScope.launch {
                    authManager.setDisplayName(displayName)
                    val result = squadRepo.joinSquad(code)
                    result.onSuccess { info ->
                        squadInfo = info
                        showActionDialog = false
                        com.example.focusreminder.platform.analytics.FirebaseAnalyticsHelper.logSquadJoined()
                        Toast.makeText(context, "Joined squad ${info.name}!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to join squad", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onCreate = { name, budget, displayName ->
                coroutineScope.launch {
                    authManager.setDisplayName(displayName)
                    val result = squadRepo.createSquad(name, budget)
                    result.onSuccess { info ->
                        squadInfo = info
                        showActionDialog = false
                        com.example.focusreminder.platform.analytics.FirebaseAnalyticsHelper.logSquadCreated(1)
                        Toast.makeText(context, "Squad created! Room code: ${info.inviteCode}", Toast.LENGTH_LONG).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to create squad", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onSaveProfile = { name ->
                authManager.setDisplayName(name)
                coroutineScope.launch {
                    squadInfo = squadRepo.getSquadInfo()
                    showActionDialog = false
                    Toast.makeText(context, "Display name updated to $name", Toast.LENGTH_SHORT).show()
                }
            }
        )
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
                    text = "Focus Squad",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            // Squad Code Pill
            squadInfo?.let { squad ->
                NeuPill(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(squad.inviteCode))
                        Toast.makeText(context, "Squad invite code ${squad.inviteCode} copied!", Toast.LENGTH_SHORT).show()
                    },
                    cornerRadius = 12.dp,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Code:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = squad.inviteCode,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = EmeraldDark
                        )
                    }
                }
            }
        }

        // Squad Selector Pill Row
        squadInfo?.let { squad ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuPill(
                    onClick = { showActionDialog = true },
                    cornerRadius = 12.dp,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${squad.name} (${squad.members.size}/5)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "▾",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }

                Text(
                    text = "Daily reset at midnight",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }

        // Scrollable Infographics List
        squadInfo?.let { squad ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
            ) {
                // 1. Squad Pact Halo
                item(key = "squad_halo") {
                    SquadPactHalo(members = squad.members)
                }

                // 2. Symmetrical Mountain Podium [4, 2, 1, 3, 5] & Baseline Overlap
                item(key = "squad_podium") {
                    SymmetricalPodiumView(members = squad.members)
                }

                // 3. 5-Friend Distracted vs. Total Time Chart
                item(key = "distracted_vs_total") {
                    DistractedVsTotalChart(members = squad.members)
                }

                // 4. 7-Day Consistency Pulse & Personal Goal Defense
                item(key = "squad_consistency_pulse") {
                    SquadConsistencyPulseChart(members = squad.members)
                }

                // 5. Architecture Footnote
                item(key = "squad_footnote") {
                    Text(
                        text = "Focus Squad • 5-Member Architecture • Local Offline Preview",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
