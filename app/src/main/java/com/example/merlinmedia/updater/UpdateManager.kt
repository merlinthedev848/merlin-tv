package com.example.merlinmedia.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.merlinmedia.BuildConfig
import com.example.merlinmedia.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object UpdateManager {
    const val GITHUB_REPO = "merlinthedev848/merlin-tv"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MerlinTV/${BuildConfig.VERSION_NAME} (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.code == 404) {
                // No releases yet in repository
                return@runCatching null
            }
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub API error: HTTP ${response.code} ${response.message}")
            }

            val bodyString = response.body?.string().orEmpty()
            if (bodyString.isBlank()) return@runCatching null

            val json = JSONObject(bodyString)
            val rawTag = json.optString("tag_name", "").trim()
            val cleanVersion = rawTag.removePrefix("v").removePrefix("V")
            val releaseNotes = json.optString("body", "No release notes provided.")
            val releaseDate = json.optString("published_at", "")
            val releaseHtmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")

            val assets = json.optJSONArray("assets") ?: return@runCatching null
            var apkDownloadUrl = ""

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = asset.optString("browser_download_url", "")
                    break
                }
            }

            if (apkDownloadUrl.isBlank()) {
                return@runCatching null
            }

            val isNew = isNewerVersion(cleanVersion, BuildConfig.VERSION_NAME)
            if (isNew) {
                UpdateInfo(
                    version = cleanVersion,
                    apkUrl = apkDownloadUrl,
                    notes = releaseNotes,
                    releaseDate = releaseDate,
                    releaseUrl = releaseHtmlUrl
                )
            } else {
                null
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, "merlin-tv-${info.version}.apk")

            val request = Request.Builder()
                .url(info.apkUrl)
                .header("User-Agent", "MerlinTV/${BuildConfig.VERSION_NAME} (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IllegalStateException("Download failed with HTTP ${response.code}")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body from download server")
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (contentLength > 0) {
                            ((totalRead * 100) / contentLength).toInt()
                        } else {
                            -1
                        }
                        onProgress(progress, totalRead, contentLength)
                    }
                    output.flush()
                }
            }
            targetFile
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
        if (remoteVersion.isBlank() || localVersion.isBlank()) return false
        val cleanRemote = remoteVersion.split('-', '_', '+')[0]
        val cleanLocal = localVersion.split('-', '_', '+')[0]

        val remoteParts = cleanRemote.split('.').mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split('.').mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}