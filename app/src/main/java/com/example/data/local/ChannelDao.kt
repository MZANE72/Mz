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

    @Query("SELECT * FROM channels WHERE isDeleted = 0")
    suspend fun getAllActiveChannels(): List<ChannelEntity>

    @Query("SELECT COUNT(*) FROM channels WHERE isDeleted = 0")
    suspend fun getActiveCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE channels SET isDeleted = :isDeleted WHERE id = :id")
    suspend fun updateDeleted(id: String, isDeleted: Boolean)

    @Query("UPDATE channels SET sortOrder = :newOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, newOrder: Int)

    @Update
    suspend fun updateChannels(channels: List<ChannelEntity>)

    @Transaction
    suspend fun reorderChannels(reorderedList: List<ChannelEntity>) {
        val updated = reorderedList.mapIndexed { index, item ->
            item.copy(sortOrder = index)
        }
        updateChannels(updated)
    }

    @Query("UPDATE channels SET isDeleted = 0")
    suspend fun restoreAllDeleted()

    @Query("DELETE FROM channels")
    suspend fun clearAll()
}
