package com.example.focusreminder.ui.squad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuPill

enum class SquadDialogTab {
    JOIN,
    CREATE,
    PROFILE
}

@Composable
fun SquadActionDialog(
    currentDisplayName: String,
    onDismiss: () -> Unit,
    onJoin: (code: String, displayName: String) -> Unit,
    onCreate: (squadName: String, budgetMinutes: Int, displayName: String) -> Unit,
    onSaveProfile: (name: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(SquadDialogTab.CREATE) }
    var joinCodeInput by remember { mutableStateOf("") }
    var createNameInput by remember { mutableStateOf("Focus Sprint") }
    var createBudgetInput by remember { mutableStateOf("60") }
    var profileNameInput by remember { mutableStateOf(if (currentDisplayName == "You") "" else currentDisplayName) }

    Dialog(onDismissRequest = onDismiss) {
        NeuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            cornerRadius = 24.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Title
                Column {
                    Text(
                        text = "FOCUS SQUAD",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = when (selectedTab) {
                            SquadDialogTab.JOIN -> "Join a Squad"
                            SquadDialogTab.CREATE -> "Create New Squad"
                            SquadDialogTab.PROFILE -> "My Identity"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                // Full-Width Segmented Tab Row (Equal 1f weights so nothing is crunched)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabPill(
                        label = "Create",
                        isSelected = selectedTab == SquadDialogTab.CREATE,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = SquadDialogTab.CREATE }
                    )
                    TabPill(
                        label = "Join Code",
                        isSelected = selectedTab == SquadDialogTab.JOIN,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = SquadDialogTab.JOIN }
                    )
                    TabPill(
                        label = "Profile",
                        isSelected = selectedTab == SquadDialogTab.PROFILE,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = SquadDialogTab.PROFILE }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Tab Content Body
                when (selectedTab) {
                    SquadDialogTab.CREATE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Your Display Name:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            NeumorphicInput(
                                value = profileNameInput,
                                onValueChange = { profileNameInput = it },
                                placeholder = "e.g. Aatharv"
                            )

                            Text(
                                text = "Squad / Room Name:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            NeumorphicInput(
                                value = createNameInput,
                                onValueChange = { createNameInput = it },
                                placeholder = "e.g. Study Sprint"
                            )

                            Text(
                                text = "Daily Target Budget (Minutes):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            NeumorphicInput(
                                value = createBudgetInput,
                                onValueChange = { createBudgetInput = it.filter { ch -> ch.isDigit() } },
                                placeholder = "60"
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            NeuPill(
                                onClick = {
                                    val finalName = profileNameInput.trim().ifEmpty { "Aatharv" }
                                    val squad = createNameInput.trim().ifEmpty { "Focus Squad" }
                                    val budget = createBudgetInput.toIntOrNull() ?: 60
                                    onCreate(squad, budget, finalName)
                                },
                                cornerRadius = 14.dp,
                                elevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Create Squad & Get Code",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }

                    SquadDialogTab.JOIN -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Your Display Name:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            NeumorphicInput(
                                value = profileNameInput,
                                onValueChange = { profileNameInput = it },
                                placeholder = "e.g. Aatharv"
                            )

                            Text(
                                text = "Enter 6-Character Room Code:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                            NeumorphicInput(
                                value = joinCodeInput.uppercase(),
                                onValueChange = { if (it.length <= 6) joinCodeInput = it.uppercase() },
                                placeholder = "e.g. PACT5X",
                                isMonospace = true
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            NeuPill(
                                onClick = {
                                    if (joinCodeInput.trim().length >= 4) {
                                        val finalName = profileNameInput.trim().ifEmpty { "Aatharv" }
                                        onJoin(joinCodeInput.trim().uppercase(), finalName)
                                    }
                                },
                                cornerRadius = 14.dp,
                                elevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Join Squad Room",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }

                    SquadDialogTab.PROFILE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Your Display Name (Visible to Squad):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )

                            NeumorphicInput(
                                value = profileNameInput,
                                onValueChange = { profileNameInput = it },
                                placeholder = "e.g. Aatharv"
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            NeuPill(
                                onClick = {
                                    if (profileNameInput.isNotBlank()) {
                                        onSaveProfile(profileNameInput.trim())
                                    }
                                },
                                cornerRadius = 14.dp,
                                elevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Save Profile Name",
                                        fontSize = 13.sp,
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
}

@Composable
private fun TabPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = if (isSelected) EmeraldDark.copy(alpha = 0.16f) else NeuSurfaceSunken,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) EmeraldDark.copy(alpha = 0.5f) else NeuSunkenTrack,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) EmeraldDark else TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NeumorphicInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isMonospace: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(NeuSurfaceSunken, RoundedCornerShape(12.dp))
            .border(1.dp, NeuSunkenTrack, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                fontSize = 13.sp,
                color = TextMuted.copy(alpha = 0.6f),
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(EmeraldDark),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
