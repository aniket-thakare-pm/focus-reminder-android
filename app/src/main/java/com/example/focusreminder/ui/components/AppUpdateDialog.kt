package com.example.focusreminder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun AppUpdateDialog() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
                                    coroutineScope.launch {
                                        AppUpdateManager.downloadAndInstallApk(context, s.info)
                                    }
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
                                        text = "Update Now",
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

        is UpdateStatus.Downloading -> {
            Dialog(onDismissRequest = { /* Prevent dismiss during download */ }) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DOWNLOADING UPDATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldDark,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "${(s.progress * 100).toInt()}%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )

                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = EmeraldPrimary,
                            trackColor = NeuSunkenTrack
                        )

                        Text(
                            text = "Package will open automatically when download completes.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        is UpdateStatus.ReadyToInstall -> {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "DOWNLOAD COMPLETE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldDark,
                            letterSpacing = 1.2.sp
                        )

                        NeuPill(
                            onClick = { AppUpdateManager.installApk(context, s.apkFile) },
                            cornerRadius = 12.dp,
                            elevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap to Install Now",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldDark
                                )
                            }
                        }
                    }
                }
            }
        }

        is UpdateStatus.Error -> {
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
                        Text(
                            text = "UPDATE ERROR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = s.message,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        NeuPill(
                            onClick = { AppUpdateManager.dismissUpdate() },
                            cornerRadius = 12.dp,
                            elevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Close",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> {
            // Idle or Checking - show nothing
        }
    }
}
