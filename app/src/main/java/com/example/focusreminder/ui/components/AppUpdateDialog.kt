package com.example.focusreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.focusreminder.platform.update.AppUpdateManager
import com.example.focusreminder.platform.update.UpdateStatus
import com.example.focusreminder.theme.*

@Composable
fun AppUpdateDialog() {
    val context = LocalContext.current
    val status by AppUpdateManager.updateStatus.collectAsState()

    when (val s = status) {
        is UpdateStatus.Available -> {
            Dialog(onDismissRequest = { AppUpdateManager.dismissUpdate() }) {
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
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(EmeraldPrimary, CircleShape)
                            )
                            Text(
                                text = "UPDATE AVAILABLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldDark,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Text(
                            text = s.info.tagName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )

                        Text(
                            text = s.info.releaseNotes,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeuPill(
                                onClick = { AppUpdateManager.dismissUpdate() },
                                cornerRadius = 12.dp,
                                elevation = 2.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Later",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                }
                            }

                            NeuPill(
                                onClick = {
                                    AppUpdateManager.openUpdateInBrowser(context, s.info.downloadUrl)
                                    AppUpdateManager.dismissUpdate()
                                },
                                cornerRadius = 12.dp,
                                elevation = 4.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Download Update",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        else -> {
            // Idle or Checking
        }
    }
}
