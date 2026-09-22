package com.example.merlinmedia.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.merlinmedia.BuildConfig
import com.example.merlinmedia.data.HttpClientProvider
import com.example.merlinmedia.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import timber.log.Timber

object UpdateManager {
    const val GITHUB_REPO = "merlinthedev848/merlin-tv"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val RAW_VERSION_URL = "https://raw.githubusercontent.com/$GITHUB_REPO/main/version.json"

    suspend fun checkForUpdates(context: Context? = null): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val client = HttpClientProvider.getClient(context)

            // 1. Try GitHub Releases API first
            val apiUpdate = runCatching {
                val request = Request.Builder()
                    .url(API_URL)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "MerlinTV/${BuildConfig.VERSION_NAME} (Android)")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bodyString = response.body?.string().orEmpty()
                    if (bodyString.isBlank()) return@use null

                    val json = JSONObject(bodyString)
                    val rawTag = json.optString("tag_name", "").trim()
                    val cleanVersion = rawTag.removePrefix("v").removePrefix("V")
                    val releaseNotes = json.optString("body", "No release notes provided.")
                    val releaseDate = json.optString("published_at", "")
                    val releaseHtmlUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases")

                    val assets = json.optJSONArray("assets") ?: return@use null
                    var apkDownloadUrl = ""

                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }

                    if (apkDownloadUrl.isNotBlank() && isNewerVersion(cleanVersion, BuildConfig.VERSION_NAME)) {
                        UpdateInfo(
                            version = cleanVersion,
                            apkUrl = apkDownloadUrl,
                            notes = releaseNotes,
                            releaseDate = releaseDate,
                            releaseUrl = releaseHtmlUrl
                        )
                    } else null
                }
            }.getOrNull()

            if (apiUpdate != null) {
                return@runCatching apiUpdate
            }

            // 2. Fallback to raw version.json on main branch (immune to API rate-limiting)
            runCatching {
                val req = Request.Builder()
                    .url(RAW_VERSION_URL)
                    .header("User-Agent", "MerlinTV/${BuildConfig.VERSION_NAME} (Android)")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val body = resp.body?.string().orEmpty()
                    if (body.isBlank()) return@use null
                    val json = JSONObject(body)
                    val ver = json.optString("version", "").trim()
                    val downloadUrl = json.optString("downloadUrl", "")
                    val relUrl = json.optString("releaseUrl", "https://github.com/$GITHUB_REPO/releases")
                    val notes = json.optString("releaseNotes", "Bug fixes and performance improvements.")
                    val date = json.optString("publishedAt", "")

                    if (ver.isNotBlank() && downloadUrl.isNotBlank() && isNewerVersion(ver, BuildConfig.VERSION_NAME)) {
                        UpdateInfo(
                            version = ver,
                            apkUrl = downloadUrl,
                            notes = notes,
                            releaseDate = date,
                            releaseUrl = relUrl
                        )
                    } else null
                }
            }.getOrNull()
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

            val client = HttpClientProvider.getClient(context)
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                throw IllegalStateException("Download failed with HTTP ${response.code}")
            }

            val body = response.body ?: run {
                response.close()
                throw IllegalStateException("Empty response body from download server")
            }
            val contentLength = body.contentLength()

            response.use {
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
            }
            targetFile
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        apkFile.setReadable(true, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            runCatching {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
            }.onFailure { error ->
                Timber.w(error, "Failed to launch ACTION_MANAGE_UNKNOWN_APP_SOURCES intent")
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            clipData = android.content.ClipData.newRawUri("package", apkUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val resolveList = context.packageManager.queryIntentActivities(installIntent, 0)
        for (resolveInfo in resolveList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(
                packageName,
                apkUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
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