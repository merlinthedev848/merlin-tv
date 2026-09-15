package com.example.merlinmedia.ui.dialogs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.merlinmedia.BuildConfig
import com.example.merlinmedia.model.UpdateInfo
import com.example.merlinmedia.model.UpdateState
import com.example.merlinmedia.ui.theme.*
import com.example.merlinmedia.updater.UpdateManager
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    initialState: UpdateState = UpdateState.Idle,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(initialState) }

    fun runUpdateCheck() {
        state = UpdateState.Checking
        coroutineScope.launch {
            val result = UpdateManager.checkForUpdates()
            result.onSuccess { info ->
                state = if (info != null) {
                    UpdateState.Available(info)
                } else {
                    UpdateState.UpToDate
                }
            }.onFailure { error ->
                state = UpdateState.Error(error.localizedMessage ?: "Failed to check for updates")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state is UpdateState.Idle) {
            runUpdateCheck()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, CardSurface, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Software Updates",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Current version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardSurface)
                Spacer(modifier = Modifier.height(16.dp))

                // Content based on State
                when (val current = state) {
                    is UpdateState.Checking -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterVertically,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryCyan)
                            Text("Checking GitHub repository for latest release...", color = TextSecondary)
                        }
                    }

                    is UpdateState.UpToDate -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterVertically,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🎉 You're running the latest version!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                            Text(
                                text = "Repository: github.com/${UpdateManager.GITHUB_REPO}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    is UpdateState.Available -> {
                        val info = current.info
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "New Release Available: v${info.version}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCyan
                                )
                                if (info.releaseDate.isNotBlank()) {
                                    Text(
                                        text = info.releaseDate.take(10),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "Changelog & Notes:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CardSurface)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = info.notes.ifBlank { "No detailed release notes provided." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    is UpdateState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Downloading Update...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )

                            if (current.progress >= 0) {
                                LinearProgressIndicator(
                                    progress = { current.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PrimaryCyan,
                                    trackColor = CardSurface
                                )
                                val downloadedMb = current.bytesDownloaded / (1024f * 1024f)
                                val totalMb = current.totalBytes / (1024f * 1024f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${current.progress}%", color = PrimaryCyan, fontSize = 12.sp)
                                    Text(
                                        String.format("%.1f MB / %.1f MB", downloadedMb, totalMb),
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PrimaryCyan
                                )
                            }
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterVertically,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "✅ Download Complete!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryCyan
                            )
                            Text(
                                text = "Click 'Install Now' to launch the Android package installer.",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is UpdateState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Update Check Failed",
                                color = ErrorRed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = current.message,
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is UpdateState.Idle -> {}
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (val current = state) {
                        is UpdateState.Available -> {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(current.info.releaseUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GitHub")
                            }

                            Button(
                                onClick = {
                                    state = UpdateState.Downloading(0, 0, 0)
                                    coroutineScope.launch {
                                        val downloadResult = UpdateManager.downloadApk(context, current.info) { progress, downloaded, total ->
                                            state = UpdateState.Downloading(progress, downloaded, total)
                                        }
                                        downloadResult.onSuccess { apkFile ->
                                            state = UpdateState.ReadyToInstall(apkFile)
                                            UpdateManager.installApk(context, apkFile)
                                        }.onFailure { err ->
                                            state = UpdateState.Error(err.localizedMessage ?: "Failed to download APK")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download & Install", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        is UpdateState.ReadyToInstall -> {
                            Button(
                                onClick = { UpdateManager.installApk(context, current.file) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                            ) {
                                Text("Install Now", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        is UpdateState.Error -> {
                            Button(
                                onClick = { runUpdateCheck() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        is UpdateState.UpToDate -> {
                            OutlinedButton(onClick = { runUpdateCheck() }, modifier = Modifier.padding(end = 8.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Again")
                            }
                        }

                        else -> {}
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = onDismiss) {
                        Text("Close", color = TextSecondary)
                    }
                }
            }
        }
    }
}