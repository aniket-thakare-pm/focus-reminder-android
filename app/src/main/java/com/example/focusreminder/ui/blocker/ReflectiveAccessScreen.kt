package com.example.focusreminder.ui.blocker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

private val REFLECTIVE_QUESTIONS = listOf(
    "What specific task or objective requires using this application right now?",
    "Can this action wait until your next scheduled window or be done on desktop?",
    "How will you feel in 30 minutes if you spend this time scrolling?",
    "What is the single highest-priority task on your agenda today?",
    "How many minutes of emergency access do you genuinely need to finish your goal?"
)

@Composable
fun ReflectiveAccessScreen(
    appName: String = "Instagram",
    onCancel: () -> Unit,
    onGrantOverride: (answers: List<String>, requestedMinutes: Int) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf("", "", "", "", "") }
    var requestedMinutes by remember { mutableIntStateOf(5) }

    val currentAnswer = answers.getOrElse(currentStep) { "" }
    val wordCount = remember(currentAnswer) {
        val trimmed = currentAnswer.trim()
        if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val isAnswerValid = wordCount >= 10

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Step Indicator & Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = TextSecondary, fontSize = 14.sp)
                }

                NeuPill(
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "Step ${currentStep + 1} of 5",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Question Section
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "EMERGENCY ACCESS REFLECTION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = EmeraldDark
                )
                Text(
                    text = REFLECTIVE_QUESTIONS[currentStep],
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                )
            }

            // Input Well (Sunken Neumorphic Well)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                NeuInsetWell(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp
                ) {
                    OutlinedTextField(
                        value = currentAnswer,
                        onValueChange = { answers[currentStep] = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .padding(8.dp),
                        placeholder = {
                            Text(
                                text = "Take a moment to reflect honestly. Type your answer...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = EmeraldPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAnswerValid) "Requirement met ($wordCount words)" else "Minimum 10 words required ($wordCount/10)",
                        fontSize = 11.sp,
                        fontWeight = if (isAnswerValid) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAnswerValid) EmeraldDark else Color(0xFFEF4444)
                    )
                    Text(
                        text = if (isAnswerValid) "Unlocked" else "Locked",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnswerValid) EmeraldDark else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // On final step: Ask user to set their needed access window duration
                if (currentStep == 4) {
                    Text(
                        text = "Requested Access Duration:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(3, 5, 10).forEach { mins ->
                            val isSelected = requestedMinutes == mins
                            NeuPill(
                                modifier = Modifier.weight(1f),
                                onClick = { requestedMinutes = mins }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$mins Mins",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "App automatically locks when elapsed. Extensions require repeating all 5 questions.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    NeuPill(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier
                            .weight(0.8f)
                            .height(52.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Back", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }
                }

                NeuPill(
                    onClick = {
                        if (isAnswerValid) {
                            if (currentStep < 4) {
                                currentStep += 1
                            } else {
                                onGrantOverride(answers.toList(), requestedMinutes)
                            }
                        }
                    },
                    backgroundColor = if (isAnswerValid) EmeraldPrimary else Color(0xFFD5DBE5),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (currentStep < 4) "Next Question" else "Unlock for $requestedMinutes Mins",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = if (isAnswerValid) Color.White else TextMuted
                        )
                    }
                }
            }
        }
    }
}
