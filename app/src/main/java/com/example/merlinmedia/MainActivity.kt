package com.example.merlinmedia

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class MediaEntry(
    val title: String,
    val url: String,
    val type: Kind,
    val country: String = "",
    val group: String = "",
    val artwork: String? = null,
    val source: String
)
enum class Kind { LIVE, MOVIE, SERIES }

data class UpdateInfo(val version: String, val apkUrl: String, val notes: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { MerlinTvApp() } }
        lifecycleScope.launch { UpdateManager.check(this@MainActivity, silent = true) }
    }
}

@Composable
fun MerlinTvApp() {
    var tab by remember { mutableStateOf(Kind.LIVE) }
    var selected by remember { mutableStateOf<MediaEntry?>(null) }
    val context = LocalContext.current
    val live = remember { mutableStateListOf<MediaEntry>() }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { CatalogRepository.loadLive() }
        live.clear(); live.addAll(result)
        loading = false
    }

    if (selected != null) {
        PlayerScreen(selected!!) { selected = null }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Merlin TV", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Kind.LIVE to "Live TV", Kind.MOVIE to "Movies", Kind.SERIES to "Series").forEach { (kind, label) ->
                Button(onClick = { tab = kind }, enabled = tab != kind) { Text(label) }
            }
            OutlinedButton(onClick = {
                (context as? ComponentActivity)?.lifecycleScope?.launch {
                    message = when (val info = UpdateManager.check(context, silent = false)) {
                        null -> "You're up to date, or the update feed is unavailable."
                        else -> "Update ${info.version} is available. Open Settings > Updates to install it."
                    }
                }
            }) { Text("Check updates") }
        }
        message?.let { Text(it, modifier = Modifier.padding(vertical = 8.dp)) }
        Spacer(Modifier.height(12.dp))

        val entries = when (tab) {
            Kind.LIVE -> live
            Kind.MOVIE -> CatalogRepository.movies
            Kind.SERIES -> CatalogRepository.series
        }
        if (loading && tab == Kind.LIVE) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { item ->
                Card(Modifier.fillMaxWidth().clickable { selected = item }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        val detail = listOf(item.country, item.group, item.source).filter { it.isNotBlank() }.joinToString(" · ")
                        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(item: MediaEntry, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(item.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.url)); prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onBack) { Text("Back") }
            Text(item.title, modifier = Modifier.padding(10.dp))
        }
        AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
    }
}

object CatalogRepository {
    private val client = OkHttpClient()
    private val playlists = listOf(
        "UK" to "https://iptv-org.github.io/iptv/countries/uk.m3u",
        "USA" to "https://iptv-org.github.io/iptv/countries/us.m3u"
    )

    suspend fun loadLive(): List<MediaEntry> = playlists.flatMap { (country, url) ->
        runCatching {
            val body = client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string().orEmpty() }
            parseM3u(body, country)
        }.getOrDefault(emptyList())
    }.distinctBy { it.title.lowercase() to it.url }

    private fun parseM3u(text: String, country: String): List<MediaEntry> {
        val out = mutableListOf<MediaEntry>()
        var title = ""; var group = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF")) {
                title = line.substringAfterLast(',').trim().ifBlank { "Channel" }
                group = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.getOrNull(1).orEmpty()
            } else if (line.startsWith("http://") || line.startsWith("https://")) {
                out += MediaEntry(title, line, Kind.LIVE, country, group, source = "iptv-org public playlist")
                title = ""; group = ""
            }
        }
        return out
    }

    val movies = listOf(
        MediaEntry("Big Buck Bunny", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", Kind.MOVIE, source = "Open movie / public sample stream"),
        MediaEntry("Sintel", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", Kind.MOVIE, source = "Blender open movie / public sample stream"),
        MediaEntry("Tears of Steel", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", Kind.MOVIE, source = "Blender open movie / public sample stream"),
        MediaEntry("Elephants Dream", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", Kind.MOVIE, source = "Blender open movie / public sample stream")
    )

    val series = listOf(
        MediaEntry("Open Shorts · Episode 1 · For Bigger Blazes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", Kind.SERIES, group = "Open Shorts", source = "Public Google sample stream"),
        MediaEntry("Open Shorts · Episode 2 · For Bigger Escapes", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", Kind.SERIES, group = "Open Shorts", source = "Public Google sample stream")
    )
}

object UpdateManager {
    private const val API = "https://api.github.com/repos/merlinthedev848/merlin-tv/releases/latest"
    private val client = OkHttpClient()

    suspend fun check(context: Context, silent: Boolean): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(API).header("Accept", "application/vnd.github+json").build()
            val json = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                JSONObject(response.body?.string().orEmpty())
            }
            val tag = json.optString("tag_name").removePrefix("v")
            if (!isNewer(tag, BuildConfig.VERSION_NAME)) return@runCatching null
            val assets = json.optJSONArray("assets") ?: return@runCatching null
            var apk = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", true)) { apk = asset.optString("browser_download_url"); break }
            }
            if (apk.isBlank()) return@runCatching null
            val info = UpdateInfo(tag, apk, json.optString("body"))
            if (silent) android.os.Handler(context.mainLooper).post {
                android.widget.Toast.makeText(context, "Merlin TV $tag available. Use Check updates to install.", android.widget.Toast.LENGTH_LONG).show()
            }
            info
        }.getOrNull()
    }

    suspend fun downloadAndInstall(context: Context, info: UpdateInfo) = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val file = File(dir, "merlin-tv-${info.version}.apk")
        client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
            require(response.isSuccessful) { "Download failed: ${response.code}" }
            file.outputStream().use { out -> response.body!!.byteStream().copyTo(out) }
        }
        android.os.Handler(context.mainLooper).post { install(context, file) }
    }

    private fun install(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            android.widget.Toast.makeText(context, "Allow installs from Merlin TV, then run Check updates again.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split('.', '-', '_').map { it.toIntOrNull() ?: 0 }
        val a = parts(remote); val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
