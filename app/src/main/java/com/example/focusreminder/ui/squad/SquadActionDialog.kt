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
    onJoin: (code: String) -> Unit,
    onCreate: (name: String, budgetMinutes: Int) -> Unit,
    onSaveProfile: (name: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(SquadDialogTab.JOIN) }
    var joinCodeInput by remember { mutableStateOf("") }
    var createNameInput by remember { mutableStateOf("") }
    var createBudgetInput by remember { mutableStateOf("60") }
    var profileNameInput by remember { mutableStateOf(currentDisplayName) }

    Dialog(onDismissRequest = onDismiss) {
        NeuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            cornerRadius = 24.dp,
            elevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                SquadDialogTab.JOIN -> "Join with Room Code"
                                SquadDialogTab.CREATE -> "Create 5-Person Squad"
                                SquadDialogTab.PROFILE -> "My Squad Profile"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    // Tab selector pills
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TabPill(
                            label = "Join",
                            isSelected = selectedTab == SquadDialogTab.JOIN,
                            onClick = { selectedTab = SquadDialogTab.JOIN }
                        )
                        TabPill(
                            label = "Create",
                            isSelected = selectedTab == SquadDialogTab.CREATE,
                            onClick = { selectedTab = SquadDialogTab.CREATE }
                        )
                        TabPill(
                            label = "Profile",
                            isSelected = selectedTab == SquadDialogTab.PROFILE,
                            onClick = { selectedTab = SquadDialogTab.PROFILE }
                        )
                    }
                }

                // Tab Content Body
                when (selectedTab) {
                    SquadDialogTab.JOIN -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Enter the 6-character room code shared by your friend:",
                                fontSize = 12.sp,
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
                                        onJoin(joinCodeInput.trim().uppercase())
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
                                        text = "Join Squad",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }

                    SquadDialogTab.CREATE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Squad Name:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )

                            NeumorphicInput(
                                value = createNameInput,
                                onValueChange = { createNameInput = it },
                                placeholder = "e.g. Sprint Pact"
                            )

                            Text(
                                text = "Your Daily Target Budget (Minutes):",
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
                                    val name = createNameInput.trim().ifEmpty { "Focus Squad" }
                                    val budget = createBudgetInput.toIntOrNull() ?: 60
                                    onCreate(name, budget)
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
                                        text = "Generate Room Code",
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
                                        text = "Save Profile",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Close Cancel button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) EmeraldDark.copy(alpha = 0.15f) else NeuSurfaceSunken,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) EmeraldDark.copy(alpha = 0.4f) else NeuSunkenTrack,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) EmeraldDark else TextMuted
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
            .height(44.dp)
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
