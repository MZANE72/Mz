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
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val seriesId: String? = null,
    val seasonNum: Int? = null,
    val episodeNum: Int? = null,
    val containerExtension: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_LIVE = "LIVE"
        const val TYPE_MOVIE = "MOVIE"
        const val TYPE_SERIES = "SERIES"
    }
}
