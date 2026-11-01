package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE type = :type AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getChannelsFlow(type: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE type = :type AND isFavorite = 1 AND isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun getFavoriteChannelsFlow(type: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE type = 'LIVE' AND isDeleted = 0 AND lastWatchedAt > 0 ORDER BY lastWatchedAt DESC LIMIT 25")
    fun getRecentLiveChannelsFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE (type = 'MOVIE' OR type = 'SERIES') AND isDeleted = 0 AND resumePositionMs > 5000 ORDER BY lastWatchedAt DESC LIMIT 25")
    fun getContinueWatchingFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isWatchlist = 1 AND isDeleted = 0 ORDER BY addedAt DESC")
    fun getWatchlistFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isDeleted = 0")
    suspend fun getAllActiveChannels(): List<ChannelEntity>

    @Query("SELECT COUNT(*) FROM channels WHERE isDeleted = 0")
    suspend fun getActiveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE channels SET isWatchlist = :isWatchlist WHERE id = :id")
    suspend fun updateWatchlist(id: String, isWatchlist: Boolean)

    @Query("UPDATE channels SET isWatched = :isWatched WHERE id = :id")
    suspend fun updateWatched(id: String, isWatched: Boolean)

    @Query("UPDATE channels SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun updateDeleted(id: String, isDeleted: Boolean)

    @Query("UPDATE channels SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, newOrder: Int)

    @Query("UPDATE channels SET customName = :newName WHERE id = :id")
    suspend fun renameChannel(id: String, newName: String)

    @Query("UPDATE channels SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateLocked(id: String, isLocked: Boolean)

    @Query("UPDATE channels SET lastWatchedAt = :watchedAt WHERE id = :id")
    suspend fun updateLastWatched(id: String, watchedAt: Long)

    @Query("UPDATE channels SET resumePositionMs = :positionMs, durationMs = :durationMs, lastWatchedAt = :watchedAt WHERE id = :id")
    suspend fun updateResumePosition(id: String, positionMs: Long, durationMs: Long, watchedAt: Long)

    @Query("UPDATE channels SET currentProgram = :currentProg, nextProgram = :nextProg WHERE id = :id")
    suspend fun updateEpg(id: String, currentProg: String?, nextProg: String?)

    @Update
    suspend fun updateChannels(channels: List<ChannelEntity>)

    @Transaction
    suspend fun reorderChannels(reorderedList: List<ChannelEntity>) {
        val updated = reorderedList.mapIndexed { index, item ->
            item.copy(sortOrder = index, channelNumber = index + 1)
        }
        updateChannels(updated)
    }

    @Query("UPDATE channels SET isDeleted = 0")
    suspend fun restoreAllDeleted()

    @Query("DELETE FROM channels")
    suspend fun clearAll()
}
