package com.example.merlinmedia.data

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    private const val DISK_CACHE_SIZE = 50L * 1024L * 1024L // 50 MB

    @Volatile
    private var clientInstance: OkHttpClient? = null

    fun getClient(context: Context? = null): OkHttpClient {
        val existing = clientInstance
        if (existing != null) {
            if (existing.cache != null || context == null) {
                return existing
            }
        }

        return synchronized(this) {
            val current = clientInstance
            if (current != null && (current.cache != null || context == null)) {
                current
            } else {
                buildClient(context).also { clientInstance = it }
            }
        }
    }

    private fun buildClient(context: Context?): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        if (context != null) {
            runCatching {
                val cacheDir = File(context.cacheDir, "http_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                builder.cache(Cache(cacheDir, DISK_CACHE_SIZE))
            }
        }

        return builder.build()
    }

    /**
     * Validates a URL string for use in network requests.
     * Returns true if the URL is a valid HTTP(S) URL.
     */
    fun isValidStreamUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.scheme in listOf("http", "https")
    }

    /**
     * Sanitizes a metadata string by removing potentially dangerous HTML tags.
     */
    fun sanitizeMetadata(raw: String): String {
        return raw
            .replace(Regex("<[^>]*>"), "")  // Strip HTML tags
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")  // Remove control chars
            .trim()
    }
}
