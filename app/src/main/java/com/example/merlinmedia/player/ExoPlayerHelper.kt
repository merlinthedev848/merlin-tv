package com.example.merlinmedia.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

object ExoPlayerHelper {

    fun createPlayer(context: Context, lowLatencyMode: Boolean = true): ExoPlayer {
        val minBufferMs = if (lowLatencyMode) 2500 else 5000
        val maxBufferMs = if (lowLatencyMode) 15000 else 30000
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

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
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
        player.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 3.0f))
    }

    fun buildMediaItem(url: String, title: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setDisplayTitle(title)
            .build()

        // Hint the MIME type so ExoPlayer doesn't have to probe the network
        // to determine the container format — critical for .mp4 VOD files.
        val mimeType = when {
            url.contains(".mp4", ignoreCase = true) -> androidx.media3.common.MimeTypes.VIDEO_MP4
            url.contains(".m3u8", ignoreCase = true) || url.contains(".m3u", ignoreCase = true) -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
            else -> null
        }

        val builder = MediaItem.Builder()
            .setUri(url)
            .setMediaId(url)
            .setMediaMetadata(metadata)

        if (mimeType != null) {
            builder.setMimeType(mimeType)
        }

        return builder.build()
    }
}