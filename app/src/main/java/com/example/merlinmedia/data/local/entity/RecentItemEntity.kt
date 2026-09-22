package com.example.merlinmedia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.merlinmedia.model.Kind
import com.example.merlinmedia.model.MediaEntry

@Entity(tableName = "recent_history")
data class RecentItemEntity(
    @PrimaryKey
    val url: String,
    val id: String,
    val title: String,
    val type: String,
    val country: String,
    val group: String,
    val logo: String?,
    val description: String,
    val source: String,
    val year: String,
    val duration: String,
    val genre: String,
    val rating: String,
    val season: Int?,
    val episode: Int?,
    val backdrop: String?,
    val isVod: Boolean,
    val quality: String,
    val tvgId: String,
    val watchedAt: Long = System.currentTimeMillis()
) {
    fun toMediaEntry(): MediaEntry {
        return MediaEntry(
            id = id,
            title = title,
            url = url,
            type = runCatching { Kind.valueOf(type) }.getOrDefault(Kind.LIVE),
            country = country,
            group = group,
            logo = logo?.ifBlank { null },
            description = description,
            source = source,
            year = year,
            duration = duration,
            genre = genre,
            rating = rating,
            season = season,
            episode = episode,
            backdrop = backdrop?.ifBlank { null },
            isVod = isVod,
            quality = quality.ifBlank { "1080p" },
            tvgId = tvgId
        )
    }

    companion object {
        fun fromMediaEntry(entry: MediaEntry): RecentItemEntity {
            return RecentItemEntity(
                url = entry.url,
                id = entry.id,
                title = entry.title,
                type = entry.type.name,
                country = entry.country,
                group = entry.group,
                logo = entry.logo,
                description = entry.description,
                source = entry.source,
                year = entry.year,
                duration = entry.duration,
                genre = entry.genre,
                rating = entry.rating,
                season = entry.season,
                episode = entry.episode,
                backdrop = entry.backdrop,
                isVod = entry.isVod,
                quality = entry.quality,
                tvgId = entry.tvgId,
                watchedAt = System.currentTimeMillis()
            )
        }
    }
}
