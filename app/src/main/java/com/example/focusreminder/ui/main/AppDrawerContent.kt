package com.example.focusreminder.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.ClockFaceIcon
import com.example.focusreminder.ui.components.NavJournalIcon
import com.example.focusreminder.ui.components.NavSquadIcon
import com.example.focusreminder.ui.components.NeuCard

@Composable
fun AppDrawerContent(
    onSelectLogs: () -> Unit,
    onSelectJournal: () -> Unit,
    onSelectSquad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = NeuBackground,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(modifier = Modifier.height(12.dp))

                // Drawer Header
                Column {
                    Text(
                        text = "FOCUS REMINDER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.8.sp
                    )
                    Text(
                        text = "Focus Tools",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Item 1: Logs (Clean, no description text as requested)
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectLogs),
                    cornerRadius = 16.dp,
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ClockFaceIcon(
                                color = EmeraldDark,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Logs",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "→",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                // Item 2: Reflective Journal (Clean, no description text as requested)
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectJournal),
                    cornerRadius = 16.dp,
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NavJournalIcon(
                                color = EmeraldDark,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Reflective Journal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "→",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                // Item 3: Focus Squad (Clean, no description text as requested)
                NeuCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectSquad),
                    cornerRadius = 16.dp,
                    elevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NavSquadIcon(
                                color = EmeraldDark,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Focus Squad",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "→",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Version Footer
            Text(
                text = "Focus Reminder v1.16.0",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
            )
        }
    }
}
