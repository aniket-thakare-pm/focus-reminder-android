package com.example.focusreminder.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun LogbookCalendarDialog(
    selectedDateOffset: Int,
    onDismiss: () -> Unit,
    onSelectDateOffset: (Int) -> Unit
) {
    val todayCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var displayMonthCal by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, selectedDateOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        mutableStateOf(cal)
    }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◀",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable {
                                displayMonthCal = (displayMonthCal.clone() as Calendar).apply {
                                    add(Calendar.MONTH, -1)
                                }
                            }
                            .padding(8.dp)
                    )

                    Text(
                        text = monthFormat.format(displayMonthCal.time),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )

                    Text(
                        text = "▶",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable {
                                displayMonthCal = (displayMonthCal.clone() as Calendar).apply {
                                    add(Calendar.MONTH, 1)
                                }
                            }
                            .padding(8.dp)
                    )
                }

                // Days of week header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    for (d in days) {
                        Text(
                            text = d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Days Grid
                val firstDayOfWeek = (displayMonthCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                val offsetEmpty = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
                val maxDaysInMonth = displayMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val targetSelectedCal = remember(selectedDateOffset) {
                    (todayCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, selectedDateOffset)
                    }
                }

                val totalCells = offsetEmpty + maxDaysInMonth
                val totalRows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - offsetEmpty + 1

                                if (dayNum in 1..maxDaysInMonth) {
                                    val cellCal = (displayMonthCal.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }

                                    val diffMs = cellCal.timeInMillis - todayCal.timeInMillis
                                    val offsetDays = (diffMs / (24 * 60 * 60 * 1000L)).toInt()

                                    val isFuture = offsetDays > 0
                                    val isAvailable = offsetDays in -14..0
                                    val isSelected = cellCal.get(Calendar.YEAR) == targetSelectedCal.get(Calendar.YEAR) &&
                                            cellCal.get(Calendar.DAY_OF_YEAR) == targetSelectedCal.get(Calendar.DAY_OF_YEAR)

                                    val cellModifier = Modifier
                                        .size(36.dp)
                                        .then(
                                            when {
                                                isSelected -> Modifier.background(EmeraldPrimary, CircleShape)
                                                isAvailable -> Modifier.border(1.dp, EmeraldDark.copy(alpha = 0.4f), CircleShape)
                                                else -> Modifier
                                            }
                                        )
                                        .then(
                                            if (isAvailable && !isFuture) {
                                                Modifier.clickable {
                                                    onSelectDateOffset(offsetDays)
                                                    onDismiss()
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )

                                    Box(
                                        modifier = cellModifier,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            color = when {
                                                isSelected -> Color.White
                                                isFuture -> TextMuted.copy(alpha = 0.3f)
                                                isAvailable -> TextPrimary
                                                else -> TextMuted
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(EmeraldPrimary, CircleShape))
                        Text("Selected", fontSize = 10.sp, color = TextMuted)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).border(1.dp, EmeraldDark, CircleShape))
                        Text("Available Logs (14d)", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}
