package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "Genel",
    val type: String = TYPE_LIVE, // LIVE, MOVIE, SERIES
    val sortOrder: Int = 0,
    val channelNumber: Int = 0,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val isWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val resumePositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastWatchedAt: Long = 0L,
    val customName: String? = null,
    val isLocked: Boolean = false,
    val tvgId: String? = null,
    val currentProgram: String? = null,
    val nextProgram: String? = null,
    val seriesId: String? = null,
    val seasonNum: Int? = null,
    val episodeNum: Int? = null,
    val containerExtension: String? = null,
    val accountId: String = "default",
    val addedAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = customName?.takeIf { it.isNotBlank() } ?: name

    val hasResumePosition: Boolean
        get() = resumePositionMs > 10000L && (durationMs <= 0L || resumePositionMs < durationMs - 15000L)

    companion object {
        const val TYPE_LIVE = "LIVE"
        const val TYPE_MOVIE = "MOVIE"
        const val TYPE_SERIES = "SERIES"
    }
}
