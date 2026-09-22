package com.example.merlinmedia.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.merlinmedia.data.CatalogRepository
import com.example.merlinmedia.ui.theme.*
import com.example.merlinmedia.updater.UpdateManager

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onPlaylistChanged: () -> Unit,
    onCheckForUpdates: () -> Unit = {}
) {
    val context = LocalContext.current
    var customPlaylists by remember { mutableStateOf(CatalogRepository.getCustomPlaylists(context)) }
    var selectedCountries by remember { mutableStateOf(CatalogRepository.getSelectedCountryCodes(context)) }
    var newName by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .border(1.dp, CardSurface, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = PrimaryCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Merlin TV Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CardSurface)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Country Feeds Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "🌍 Live TV Country Feeds",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose which regional IPTV catalogs to load at startup:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        CatalogRepository.availableCountries.chunked(2).forEach { rowCountries ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowCountries.forEach { (country, _) ->
                                    val isChecked = selectedCountries.contains(country)
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChecked) Color(0xFF1F2A38) else CardSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        onClick = {
                                            val updated = selectedCountries.toMutableSet()
                                            if (isChecked) {
                                                if (updated.size > 1) updated.remove(country)
                                                else Toast.makeText(context, "At least one country must remain active", Toast.LENGTH_SHORT).show()
                                            } else {
                                                updated.add(country)
                                            }
                                            selectedCountries = updated
                                            CatalogRepository.setSelectedCountryCodes(context, updated)
                                            onPlaylistChanged()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(country, color = TextPrimary, fontWeight = FontWeight.Medium)
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(checkedColor = PrimaryCyan)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CardSurface)

                    // Custom Playlists Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📺 Custom M3U Playlists", style = MaterialTheme.typography.titleMedium, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { showAddForm = !showAddForm },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showAddForm) "Cancel" else "Add M3U")
                            }
                        }

                        if (showAddForm) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = { newName = it },
                                        label = { Text("Playlist Name (e.g. My Channels)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = newUrl,
                                        onValueChange = { newUrl = it },
                                        label = { Text("M3U / M3U8 URL") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            val trimmedUrl = newUrl.trim()
                                            val isValidUrl = trimmedUrl.startsWith("http://", ignoreCase = true) ||
                                                    trimmedUrl.startsWith("https://", ignoreCase = true)
                                            when {
                                                newName.isBlank() -> Toast.makeText(context, "Please enter a playlist name", Toast.LENGTH_SHORT).show()
                                                !isValidUrl -> Toast.makeText(context, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                                                else -> {
                                                    CatalogRepository.addCustomPlaylist(context, newName.trim(), trimmedUrl)
                                                    customPlaylists = CatalogRepository.getCustomPlaylists(context)
                                                    newName = ""
                                                    newUrl = ""
                                                    showAddForm = false
                                                    onPlaylistChanged()
                                                }
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                                    ) {
                                        Text("Save Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (customPlaylists.isEmpty()) {
                            Text("No custom playlists added yet.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        } else {
                            customPlaylists.forEach { (name, url) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(url, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                                        }
                                        IconButton(
                                            onClick = {
                                                CatalogRepository.removeCustomPlaylist(context, url)
                                                customPlaylists = CatalogRepository.getCustomPlaylists(context)
                                                onPlaylistChanged()
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = CardSurface)

                    // About Application & Repository
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ℹ️ Application Details", style = MaterialTheme.typography.titleMedium, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                        Text("Version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text("Repository: github.com/${UpdateManager.GITHUB_REPO}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onCheckForUpdates()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check for Updates", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/${UpdateManager.GITHUB_REPO}"))
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub Repository", color = PrimaryCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = CardSurface)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}