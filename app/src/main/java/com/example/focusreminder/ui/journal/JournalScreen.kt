package com.example.focusreminder.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuInsetWell

data class JournalEntry(
    val id: Long,
    val appName: String,
    val dateFormatted: String,
    val requestedMinutes: Int,
    val firstQuestionAnswer: String
)

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier
) {
    val entries = remember {
        listOf(
            JournalEntry(1L, "Instagram", "Yesterday 4:12 PM", 5, "Reply to Sarah regarding the flight tickets booking for next week."),
            JournalEntry(2L, "YouTube", "2 days ago", 10, "Watch the official Kotlin 2.0 migration walkthrough for work.")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        Column {
            Text(
                text = "FEATURE D: LOCAL REFLECTIONS",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldDark,
                letterSpacing = 1.8.sp
            )
            Text(
                text = "Emergency Journal",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Past reflections recorded during intentional emergency overrides. Retained 100% locally.",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${entry.appName} • ${entry.requestedMinutes}m Override",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = entry.dateFormatted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }

                        Text(
                            text = "Task Stated:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )

                        NeuInsetWell(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp
                        ) {
                            Text(
                                text = "\"${entry.firstQuestionAnswer}\"",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 19.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Text(
                            text = "Completed all 5 reflective questions before temporary override was granted.",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
