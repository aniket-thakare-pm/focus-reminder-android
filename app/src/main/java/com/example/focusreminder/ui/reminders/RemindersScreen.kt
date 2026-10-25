package com.example.focusreminder.ui.reminders

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.platform.ReminderScheduler
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*

data class LocalReminderItem(
    val id: Long,
    val title: String,
    val description: String?,
    val timeFormatted: String,
    val isP0: Boolean = false
)

@Composable
fun RemindersScreen(
    onPreviewSticky: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    var remindersList by remember {
        mutableStateOf(
            listOf(
                LocalReminderItem(101L, "Prepare Sprint Demo Deck", "Review the latest architecture slides and compile test metrics.", "09:45 AM", isP0 = true),
                LocalReminderItem(102L, "Review PR #42 Architecture", "Check rolling window boundary conditions.", "02:00 PM", isP0 = false),
                LocalReminderItem(103L, "Evening Walk & Offline Recharge", "Step away from screens and reset.", "07:30 PM", isP0 = false)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Header (Properly constrained so "+ New Alert" button never wraps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "FEATURE E & F: RICH ALERTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDark,
                    letterSpacing = 1.6.sp
                )
                Text(
                    text = "High-Attention Reminders",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
            NeuPill(
                onClick = { showCreateDialog = true }
            ) {
                Text(
                    text = "+ New Alert",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    maxLines = 1
                )
            }
        }

        // Reminders List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(remindersList, key = { it.id }) { reminder ->
                NeuCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    elevation = 5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (reminder.isP0) {
                                NeuInsetWell(
                                    cornerRadius = 8.dp
                                ) {
                                    Text(
                                        text = "P0 STICKY ALERT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldDark,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                NeuInsetWell(
                                    cornerRadius = 8.dp
                                ) {
                                    Text(
                                        text = "P1 TIMED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = reminder.timeFormatted,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = reminder.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )

                        if (!reminder.description.isNullOrBlank()) {
                            Text(
                                text = reminder.description,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (reminder.isP0) "Sticks on screen until acknowledged" else "Dismisses to notification tray",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )

                            if (reminder.isP0) {
                                NeuInsetWell(
                                    onClick = onPreviewSticky,
                                    cornerRadius = 10.dp
                                ) {
                                    Text(
                                        text = "Test Takeover",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDark,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet for New Reminder
    if (showCreateDialog) {
        CreateReminderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, delayMins, isP0 ->
                val id = System.currentTimeMillis()
                val triggerAt = System.currentTimeMillis() + (delayMins * 60 * 1000L)
                ReminderScheduler.scheduleReminder(
                    context = context,
                    reminderId = id,
                    title = title,
                    description = desc,
                    triggerAtMs = triggerAt,
                    isP0 = isP0
                )
                val newItem = LocalReminderItem(
                    id = id,
                    title = title,
                    description = desc,
                    timeFormatted = "In $delayMins mins",
                    isP0 = isP0
                )
                remindersList = listOf(newItem) + remindersList
                showCreateDialog = false
                Toast.makeText(context, "Reminder scheduled! It will fire in $delayMins minutes.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun CreateReminderDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, delayMins: Int, isP0: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var delayMins by remember { mutableIntStateOf(5) }
    var isP0 by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        title = {
            Text("Schedule New Reminder", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeuInsetWell(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Reminder Title (e.g. Stand up)", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }

                NeuInsetWell(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Optional description", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(80.dp).padding(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("P0 Sticky Takeover", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isP0,
                        onCheckedChange = { isP0 = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFD1D9E6)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, description, delayMins, isP0)
                    }
                }
            ) {
                Text("Schedule", color = EmeraldDark, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
