package com.example.merlinmedia.model

data class MediaEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val type: Kind,
    val country: String = "",
    val group: String = "",
    val logo: String? = null,
    val description: String = "",
    val source: String = ""
)

enum class Kind {
    LIVE,
    MOVIE,
    SERIES,
    FAVORITES
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit (16:9)"),
    ZOOM("Zoom (Fill)"),
    STRETCH("Stretch")
}

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val notes: String,
    val releaseDate: String = "",
    val releaseUrl: String = ""
)

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateState()
    data class ReadyToInstall(val file: java.io.File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}