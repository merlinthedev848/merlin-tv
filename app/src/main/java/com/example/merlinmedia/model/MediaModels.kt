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
    val source: String = "",
    val year: String = "",
    val duration: String = "",
    val genre: String = "",
    val rating: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val backdrop: String? = null,
    val isVod: Boolean = false,
    val quality: String = "1080p",
    val tvgId: String = ""
)

enum class Kind {
    LIVE,
    PLUTO,
    SKY,
    MOVIE,
    SERIES,
    RADIO,
    FAVORITES
}

enum class NavSection(val title: String) {
    HOME("Home"),
    HUB("Hubs"),
    LIVE("Live TV"),
    PLUTO("Pluto FAST"),
    SKY("Sky Network"),
    MOVIES("Movies"),
    SERIES("TV Series"),
    RADIO("Radio & Music"),
    FAVORITES("Favorites")
}

enum class SortMode(val label: String) {
    DEFAULT("Default"),
    ALPHABETICAL("A-Z"),
    QUALITY("Quality"),
    COUNTRY("Country")
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit (16:9)"),
    ZOOM("Zoom (Fill)"),
    STRETCH("Stretch")
}

data class EpgProgram(
    val title: String,
    val startFormatted: String = "",
    val stopFormatted: String = "",
    val description: String = "",
    val progress: Float = 0f
)

data class UpdateInfo(
    val version: String,
    val apkUrl: String,
    val notes: String,
    val releaseDate: String = "",
    val releaseUrl: String = ""
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object UpToDate : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateState()
    data class ReadyToInstall(val file: java.io.File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}