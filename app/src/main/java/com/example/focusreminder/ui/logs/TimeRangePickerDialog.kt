package com.example.focusreminder.ui.logs

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuPill
import java.util.Locale

@Composable
fun TimeRangePickerDialog(
    initialStartHour: Int = 9,
    initialStartMinute: Int = 0,
    initialEndHour: Int = 17,
    initialEndMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) -> Unit
) {
    val context = LocalContext.current
    var startH by remember { mutableIntStateOf(initialStartHour) }
    var startM by remember { mutableIntStateOf(initialStartMinute) }
    var endH by remember { mutableIntStateOf(initialEndHour) }
    var endM by remember { mutableIntStateOf(initialEndMinute) }

    fun formatTime(hour: Int, min: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h12 = when (hour % 12) {
            0 -> 12
            else -> hour % 12
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", h12, min, amPm)
    }

    Dialog(onDismissRequest = onDismiss) {
        NeuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 24.dp,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "CUSTOM TIME RANGE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldDark,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Select Specific Time Window",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Filter the audit logbook strictly to sessions occurring within this exact window.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuPill(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                startH = h
                                startM = m
                            }, startH, startM, false).show()
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("START TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatTime(startH, startM),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                    }

                    Text("→", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMuted)

                    NeuPill(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                endH = h
                                endM = m
                            }, endH, endM, false).show()
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("END TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatTime(endH, endM),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeuPill(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }

                    NeuPill(
                        onClick = {
                            onConfirm(startH, startM, endH, endM)
                        },
                        backgroundColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Apply Filter", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
