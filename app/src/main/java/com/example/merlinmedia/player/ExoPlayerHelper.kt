package com.example.merlinmedia.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import timber.log.Timber

object ExoPlayerHelper {

    fun createPlayer(context: Context, lowLatencyMode: Boolean = true): ExoPlayer {
        val minBufferMs = if (lowLatencyMode) 3000 else 6000
        val maxBufferMs = if (lowLatencyMode) 20000 else 40000
        val playbackStartBufferMs = if (lowLatencyMode) 1500 else 2500
        val rebufferMs = if (lowLatencyMode) 2000 else 4000

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMs,
                maxBufferMs,
                playbackStartBufferMs,
                rebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android TV; MerlinTV; rv:120.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    fun setPlaybackSpeed(player: ExoPlayer, speed: Float) {
        runCatching {
            player.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 3.0f))
        }.onFailure { e ->
            Timber.w(e, "Failed to set playback speed")
        }
    }

    fun sanitizeStreamUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        return runCatching {
            // Replace raw spaces with %20 so HttpURLConnection and ExoPlayer do not throw IllegalArgumentException
            trimmed.replace(" ", "%20")
        }.getOrDefault(trimmed)
    }

    fun buildMediaItem(url: String, title: String): MediaItem {
        val sanitizedUrl = sanitizeStreamUrl(url)
        val safeTitle = title.ifBlank { "Channel" }

        val metadata = MediaMetadata.Builder()
            .setTitle(safeTitle)
            .setDisplayTitle(safeTitle)
            .build()

        val builder = MediaItem.Builder()
            .setUri(Uri.parse(sanitizedUrl))
            .setMediaId(sanitizedUrl)
            .setMediaMetadata(metadata)

        val lowerUrl = sanitizedUrl.lowercase()
        when {
            lowerUrl.endsWith(".m3u8") || lowerUrl.contains(".m3u8?") || lowerUrl.contains("/hls/") || lowerUrl.contains("format=m3u8") -> {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            lowerUrl.endsWith(".mpd") || lowerUrl.contains(".mpd?") || lowerUrl.contains("/dash/") -> {
                builder.setMimeType(MimeTypes.APPLICATION_MPD)
            }
            lowerUrl.endsWith(".mp4") || lowerUrl.contains(".mp4?") -> {
                builder.setMimeType(MimeTypes.APPLICATION_MP4)
            }
            lowerUrl.endsWith(".mkv") || lowerUrl.contains(".mkv?") -> {
                builder.setMimeType(MimeTypes.VIDEO_MATROSKA)
            }
            lowerUrl.endsWith(".ts") || lowerUrl.contains(".ts?") -> {
                builder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
        }

        return builder.build()
    }
}