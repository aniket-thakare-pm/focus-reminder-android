package com.example.focusreminder.ui.logs

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.example.focusreminder.domain.model.TimeSector
import com.example.focusreminder.platform.analytics.SectorConfigStore
import com.example.focusreminder.theme.*
import com.example.focusreminder.ui.components.NeuCard
import com.example.focusreminder.ui.components.NeuInsetWell
import com.example.focusreminder.ui.components.NeuPill
import java.util.Locale

import com.example.focusreminder.util.formatHourMinute12

@Composable
fun SectorEditorDialog(
    initialSectors: List<TimeSector>,
    onDismiss: () -> Unit,
    onSave: (List<TimeSector>) -> Unit
) {
    val context = LocalContext.current
    var sectors by remember { mutableStateOf(initialSectors.toMutableList()) }

    Dialog(onDismissRequest = onDismiss) {
        NeuCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            cornerRadius = 24.dp,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CUSTOMIZE SECTORS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldDark,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Configure Day Sectors",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${sectors.size}/6 Sectors",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }

                Text(
                    text = "Partition the 24-hour day into 2 to 6 custom productivity sectors. Changes automatically update Logbook and Analytics.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                // Sectors List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(sectors) { index, sector ->
                        NeuCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 14.dp,
                            elevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color(sector.colorLong), CircleShape)
                                        )

                                        Text(
                                            text = sector.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    if (sectors.size > 2) {
                                        Text(
                                            text = "Remove",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .clickable {
                                                    sectors = sectors.toMutableList().also { it.removeAt(index) }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Start Time Button
                                    NeuPill(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    sectors = sectors.toMutableList().also {
                                                        it[index] = it[index].copy(startHour = h, startMinute = m)
                                                    }
                                                },
                                                sector.startHour,
                                                sector.startMinute,
                                                false
                                            ).show()
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("START", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                            Text(
                                                formatHourMinute12(sector.startHour, sector.startMinute),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    Text("→", fontSize = 12.sp, color = TextMuted)

                                    // End Time Button
                                    NeuPill(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    sectors = sectors.toMutableList().also {
                                                        it[index] = it[index].copy(endHour = h, endMinute = m)
                                                    }
                                                },
                                                sector.endHour,
                                                sector.endMinute,
                                                false
                                            ).show()
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("END", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                            Text(
                                                formatHourMinute12(sector.endHour, sector.endMinute),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Sector button (if < 6)
                if (sectors.size < 6) {
                    NeuPill(
                        onClick = {
                            val nextIndex = sectors.size
                            val color = SectorConfigStore.DEFAULT_SECTOR_COLORS.getOrElse(nextIndex) { 0xFF06B6D4L }
                            val newSector = TimeSector(
                                id = "sector_custom_${System.currentTimeMillis()}",
                                name = "Sector ${nextIndex + 1}",
                                startHour = (nextIndex * 4) % 24,
                                startMinute = 0,
                                endHour = ((nextIndex + 1) * 4) % 24,
                                endMinute = 0,
                                colorLong = color
                            )
                            sectors = sectors.toMutableList().also { it.add(newSector) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Add Sector (${sectors.size}/6)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                        }
                    }
                }

                // Bottom Actions: Reset, Cancel, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val store = SectorConfigStore.getInstance(context)
                            store.resetToDefaultSectors()
                            sectors = store.getDefaultSectors().toMutableList()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Defaults", fontSize = 11.sp, color = TextMuted)
                    }

                    NeuPill(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }

                    NeuPill(
                        onClick = {
                            onSave(sectors)
                        },
                        backgroundColor = EmeraldPrimary,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
