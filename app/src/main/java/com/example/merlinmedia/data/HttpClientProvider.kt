package com.example.merlinmedia.data

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
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
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
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
}
