package com.example.focusreminder.ui.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.domain.model.JournalReflectionRecord
import com.example.focusreminder.platform.journal.JournalRepository
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.BackArrowVectorIcon
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuInsetWell
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val REFLECTION_QUESTIONS = listOf(
    "Specific task or objective:",
    "Can this wait or be done on desktop?",
    "How will you feel in 30 minutes?",
    "Single highest-priority task today:",
    "Genuinely needed emergency minutes:"
)

@Composable
fun JournalScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val journalRepo = remember { JournalRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<JournalReflectionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshEntries() {
        scope.launch {
            isLoading = true
            entries = journalRepo.getAllReflections()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshEntries()
    }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeuBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
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
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "INTENTIONAL REFLECTIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Reflective Journal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        Text(
            text = "Past reflections recorded during intentional emergency overrides. Retained 100% locally.",
            fontSize = 12.sp,
            color = TextSecondary
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary, strokeWidth = 2.dp)
            }
        } else if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "No emergency reflections recorded yet.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "When an app blocker is active, completing the 5 reflective questions before unlocking will save your answers here.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    var isExpanded by remember { mutableStateOf(false) }

                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${entry.appName} • ${entry.requestedMinutes}m Override",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldDark
                                )

                                Text(
                                    text = "Delete",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            journalRepo.deleteReflection(entry.id)
                                            refreshEntries()
                                        }
                                    }
                                )
                            }

                            Text(
                                text = dateFormat.format(Date(entry.timestampMs)),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )

                            // Primary Task Stated (Question 1)
                            val firstAnswer = entry.answers.firstOrNull() ?: "No task specified"
                            Text(
                                text = "Stated Objective:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            NeuInsetWell(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 12.dp
                            ) {
                                Text(
                                    text = "\"$firstAnswer\"",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 19.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            // Expandable view for all 5 questions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isExpanded) "Hide all 5 answers" else "View all 5 answers (${entry.answers.size} answered)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = if (isExpanded) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = EmeraldDark
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    for (i in entry.answers.indices) {
                                        val qTitle = REFLECTION_QUESTIONS.getOrElse(i) { "Question ${i + 1}:" }
                                        val ans = entry.answers[i]
                                        Column {
                                            Text(
                                                text = qTitle,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = ans,
                                                fontSize = 12.sp,
                                                color = TextPrimary,
                                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
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
    }
}
