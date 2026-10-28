package com.example.focusreminder.ui.reminders

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusreminder.platform.ReminderRepository
import com.example.focusreminder.platform.ReminderScheduler
import com.example.focusreminder.platform.ScheduledReminder
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.*
import com.example.focusreminder.ui.reminder.StickyReminderActivity
import com.example.focusreminder.util.formatHoursMinutes
import com.example.focusreminder.util.formatRelativeCountdown
import com.example.focusreminder.util.formatReminderDateTime
import com.example.focusreminder.util.formatTimeOfDay
import java.util.Calendar

@Composable
fun RemindersScreen(
    onPreviewSticky: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = remember { ReminderRepository.getInstance(context) }
    val remindersList by repo.remindersFlow.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onOpenDrawer != null) {
                    NeuCard(
                        onClick = onOpenDrawer,
                        cornerRadius = 12.dp,
                        elevation = 3.dp
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HamburgerVectorIcon(
                                color = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Column {
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

        // Reminders List or Empty State
        if (remindersList.isEmpty()) {
            NeuCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                cornerRadius = 20.dp,
                elevation = 5.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NeuInsetWell(
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 28.dp
                    ) {
                        NavAlertsIcon(
                            color = EmeraldDark,
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.Center)
                        )
                    }

                    Text(
                        text = "No Alerts Scheduled",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Schedule a real-time reminder or P0 full-screen sticky alert to break focus trance at an exact moment.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    NeuPill(
                        onClick = { showCreateDialog = true }
                    ) {
                        Text(
                            text = "+ Schedule Your First Alert",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(remindersList, key = { it.id }) { reminder ->
                    val isPast = reminder.triggerTimeMs <= System.currentTimeMillis()

                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        elevation = 5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top Row: Tag & Exact Time
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
                                    text = formatTimeOfDay(reminder.triggerTimeMs),
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            // Title & Description
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

                            // Trigger Schedule & Countdown
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ClockFaceIcon(color = if (isPast) TextMuted else EmeraldDark, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "${formatReminderDateTime(reminder.triggerTimeMs)}  •  ${if (reminder.isCompleted) "Completed" else formatRelativeCountdown(reminder.triggerTimeMs)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPast) TextMuted else TextSecondary
                                )
                            }

                            // Actions Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (reminder.isP0) {
                                    NeuInsetWell(
                                        onClick = {
                                            val intent = StickyReminderActivity.createIntent(
                                                context = context,
                                                id = reminder.id,
                                                title = reminder.title,
                                                description = reminder.description,
                                                time = formatTimeOfDay(reminder.triggerTimeMs)
                                            )
                                            context.startActivity(intent)
                                        },
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
                                } else {
                                    Text(
                                        text = "Notification Tray alert",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                // Delete / Dismiss Button
                                NeuInsetWell(
                                    onClick = {
                                        ReminderScheduler.cancelReminder(context, reminder.id)
                                        repo.deleteReminder(reminder.id)
                                        Toast.makeText(context, "Reminder removed", Toast.LENGTH_SHORT).show()
                                    },
                                    cornerRadius = 10.dp
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        TrashCanIcon(color = TextMuted, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = "Delete",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted
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

    // Modal Sheet for New Reminder
    if (showCreateDialog) {
        CreateReminderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, desc, triggerAtMs, isP0 ->
                val id = System.currentTimeMillis()
                val newReminder = ScheduledReminder(
                    id = id,
                    title = title,
                    description = desc.takeIf { it.isNotBlank() },
                    triggerTimeMs = triggerAtMs,
                    isP0 = isP0
                )
                repo.addReminder(newReminder)
                ReminderScheduler.scheduleReminder(
                    context = context,
                    reminderId = id,
                    title = title,
                    description = desc,
                    triggerAtMs = triggerAtMs,
                    isP0 = isP0
                )
                showCreateDialog = false
                val dateFormatted = formatReminderDateTime(triggerAtMs)
                Toast.makeText(context, "Alert scheduled for $dateFormatted!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun CreateReminderDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, triggerAtMs: Long, isP0: Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isP0 by remember { mutableStateOf(true) }

    // Start with default trigger time: now + 15 minutes
    val triggerCalendar = remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                add(Calendar.MINUTE, 15)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        )
    }

    var isTomorrow by remember {
        mutableStateOf(false)
    }

    // Function to launch native Android TimePickerDialog
    fun showNativeTimePicker() {
        val currentHour = triggerCalendar.value.get(Calendar.HOUR_OF_DAY)
        val currentMinute = triggerCalendar.value.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val updatedCal = Calendar.getInstance()
                if (isTomorrow) {
                    updatedCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                updatedCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                updatedCal.set(Calendar.MINUTE, minute)
                updatedCal.set(Calendar.SECOND, 0)
                updatedCal.set(Calendar.MILLISECOND, 0)

                // If selected time on "Today" has already passed, automatically toggle to tomorrow
                if (!isTomorrow && updatedCal.timeInMillis <= System.currentTimeMillis()) {
                    updatedCal.add(Calendar.DAY_OF_YEAR, 1)
                    isTomorrow = true
                    Toast.makeText(context, "Time has passed today; set for Tomorrow.", Toast.LENGTH_SHORT).show()
                }

                triggerCalendar.value = updatedCal
            },
            currentHour,
            currentMinute,
            false // 12-hour AM/PM format
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeuSurface,
        title = {
            Text("Schedule Real Alert", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Field
                NeuInsetWell(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Alert title (e.g. Focus Sprint)", color = TextMuted, fontSize = 12.sp) },
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

                // Description Field
                NeuInsetWell(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Optional description", color = TextMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(65.dp).padding(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // P0 Sticky Takeover Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("P0 Sticky Takeover", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Full-screen lock until acknowledged", color = TextMuted, fontSize = 10.sp)
                    }
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

                HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), thickness = 0.8.dp)

                // Target Time Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TRIGGER TIME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.2.sp
                    )

                    // Big Tactile Debossed Time Display
                    NeuInsetWell(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNativeTimePicker() },
                        cornerRadius = 14.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formatTimeOfDay(triggerCalendar.value.timeInMillis),
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${if (isTomorrow) "Tomorrow" else "Today"} • ${formatRelativeCountdown(triggerCalendar.value.timeInMillis)}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            NeuPill(onClick = { showNativeTimePicker() }) {
                                Text(
                                    text = "Pick Time",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Day Selector: Today vs Tomorrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeuInsetWell(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (isTomorrow) {
                                        isTomorrow = false
                                        val cal = Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, triggerCalendar.value.get(Calendar.HOUR_OF_DAY))
                                            set(Calendar.MINUTE, triggerCalendar.value.get(Calendar.MINUTE))
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        if (cal.timeInMillis <= System.currentTimeMillis()) {
                                            // Reset to 15m from now if today's time has passed
                                            cal.timeInMillis = System.currentTimeMillis() + (15 * 60 * 1000L)
                                        }
                                        triggerCalendar.value = cal
                                    }
                                },
                            cornerRadius = 10.dp,
                            backgroundColor = if (!isTomorrow) EmeraldDark.copy(alpha = 0.15f) else null
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 11.sp,
                                fontWeight = if (!isTomorrow) FontWeight.Black else FontWeight.Normal,
                                color = if (!isTomorrow) EmeraldDark else TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp)
                            )
                        }

                        NeuInsetWell(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (!isTomorrow) {
                                        isTomorrow = true
                                        val cal = Calendar.getInstance().apply {
                                            add(Calendar.DAY_OF_YEAR, 1)
                                            set(Calendar.HOUR_OF_DAY, triggerCalendar.value.get(Calendar.HOUR_OF_DAY))
                                            set(Calendar.MINUTE, triggerCalendar.value.get(Calendar.MINUTE))
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        triggerCalendar.value = cal
                                    }
                                },
                            cornerRadius = 10.dp,
                            backgroundColor = if (isTomorrow) EmeraldDark.copy(alpha = 0.15f) else null
                        ) {
                            Text(
                                text = "Tomorrow",
                                fontSize = 11.sp,
                                fontWeight = if (isTomorrow) FontWeight.Black else FontWeight.Normal,
                                color = if (isTomorrow) EmeraldDark else TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp)
                            )
                        }
                    }

                    // Quick Interval Chips (+15m, +30m, +1h, +3h)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            15 to "+15m",
                            30 to "+30m",
                            60 to "+1h",
                            180 to "+3h"
                        ).forEach { (minutes, label) ->
                            NeuInsetWell(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance().apply {
                                            add(Calendar.MINUTE, minutes)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        val todayCal = Calendar.getInstance()
                                        isTomorrow = cal.get(Calendar.DAY_OF_YEAR) != todayCal.get(Calendar.DAY_OF_YEAR)
                                        triggerCalendar.value = cal
                                    },
                                cornerRadius = 8.dp
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title.trim(), description.trim(), triggerCalendar.value.timeInMillis, isP0)
                    }
                },
                enabled = title.isNotBlank() && triggerCalendar.value.timeInMillis > System.currentTimeMillis()
            ) {
                Text(
                    text = "Schedule Alert",
                    color = if (title.isNotBlank() && triggerCalendar.value.timeInMillis > System.currentTimeMillis()) EmeraldDark else TextMuted,
                    fontWeight = FontWeight.Black
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
