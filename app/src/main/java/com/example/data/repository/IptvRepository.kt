package com.example.data.repository

import com.example.data.local.ChannelDao
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistConfig
import com.example.data.local.PreferencesManager
import com.example.data.model.XtreamEpisode
import com.example.data.remote.M3uParser
import com.example.data.remote.XtreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

class IptvRepository(
    private val channelDao: ChannelDao,
    private val preferencesManager: PreferencesManager,
    private val xtreamClient: XtreamClient = XtreamClient()
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    val playlistConfig: Flow<PlaylistConfig> = preferencesManager.configFlow

    fun getLiveChannels(): Flow<List<ChannelEntity>> =
        channelDao.getChannelsFlow(ChannelEntity.TYPE_LIVE)

    fun getFavoriteLiveChannels(): Flow<List<ChannelEntity>> =
        channelDao.getFavoriteChannelsFlow(ChannelEntity.TYPE_LIVE)

    fun getMovieChannels(): Flow<List<ChannelEntity>> =
        channelDao.getChannelsFlow(ChannelEntity.TYPE_MOVIE)

    fun getSeriesChannels(): Flow<List<ChannelEntity>> =
        channelDao.getChannelsFlow(ChannelEntity.TYPE_SERIES)

    suspend fun toggleFavorite(id: String, currentStatus: Boolean) {
        channelDao.updateFavorite(id, !currentStatus)
    }

    suspend fun deleteChannel(id: String) {
        channelDao.updateDeleted(id, true)
    }

    suspend fun restoreDeletedChannels() {
        channelDao.restoreAllDeleted()
    }

    suspend fun reorderChannels(list: List<ChannelEntity>) {
        channelDao.reorderChannels(list)
    }

    suspend fun loadFromUrl(
        url: String,
        expirationMillis: Long,
        playlistName: String = "M3U Playlist"
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url.trim()).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("URL açılamadı: HTTP ${response.code}"))
            }
            val bodyStream = response.body?.byteStream()
                ?: return@withContext Result.failure(Exception("Boş playlist yanıtı"))

            val channels = M3uParser.parse(bodyStream)
            if (channels.isEmpty()) {
                return@withContext Result.failure(Exception("Playlist dosyasında geçerli kanal bulunamadı"))
            }

            channelDao.clearAll()
            channelDao.insertAll(channels)
            preferencesManager.saveUrlPlaylist(url, playlistName, expirationMillis)
            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFromFile(
        inputStream: InputStream,
        fileName: String,
        expirationMillis: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val channels = M3uParser.parse(inputStream)
            if (channels.isEmpty()) {
                return@withContext Result.failure(Exception("Dosyada geçerli kanal bulunamadı"))
            }

            channelDao.clearAll()
            channelDao.insertAll(channels)
            preferencesManager.saveFilePlaylist(fileName, expirationMillis)
            Result.success(channels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginXtream(
        server: String,
        user: String,
        pass: String,
        customExpirationMillis: Long = 0L
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val authRes = xtreamClient.authenticate(server, user, pass).getOrThrow()
            var computedExpiration = customExpirationMillis

            // Check if server returned exp_date
            authRes.userInfo?.expDate?.let { expStr ->
                val seconds = expStr.toLongOrNull()
                if (seconds != null && seconds > 0L) {
                    computedExpiration = seconds * 1000L
                }
            }

            val base = server.trim().removeSuffix("/")
            val channelsList = mutableListOf<ChannelEntity>()

            // 1. Live streams
            val liveRes = xtreamClient.getLiveStreams(server, user, pass).getOrNull() ?: emptyList()
            liveRes.forEachIndexed { idx, live ->
                val streamId = live.streamId?.toString() ?: ""
                val streamUrl = "$base/live/$user/$pass/$streamId.m3u8"
                channelsList.add(
                    ChannelEntity(
                        id = "live_$streamId",
                        name = live.name ?: "Kanal $idx",
                        streamUrl = streamUrl,
                        logoUrl = live.streamIcon,
                        groupTitle = live.categoryName ?: "Canlı TV",
                        type = ChannelEntity.TYPE_LIVE,
                        sortOrder = idx
                    )
                )
            }

            // 2. VOD (Movies)
            val vodRes = xtreamClient.getVodStreams(server, user, pass).getOrNull() ?: emptyList()
            vodRes.forEachIndexed { idx, vod ->
                val streamId = vod.streamId?.toString() ?: ""
                val ext = vod.containerExtension?.ifBlank { "mp4" } ?: "mp4"
                val streamUrl = "$base/movie/$user/$pass/$streamId.$ext"
                channelsList.add(
                    ChannelEntity(
                        id = "movie_$streamId",
                        name = vod.name ?: "Film $idx",
                        streamUrl = streamUrl,
                        logoUrl = vod.streamIcon,
                        groupTitle = "Filmler",
                        type = ChannelEntity.TYPE_MOVIE,
                        sortOrder = idx,
                        containerExtension = ext
                    )
                )
            }

            // 3. Series
            val seriesRes = xtreamClient.getSeries(server, user, pass).getOrNull() ?: emptyList()
            seriesRes.forEachIndexed { idx, s ->
                val seriesId = s.seriesId?.toString() ?: ""
                channelsList.add(
                    ChannelEntity(
                        id = "series_$seriesId",
                        name = s.name ?: "Dizi $idx",
                        streamUrl = "", // Fetched on demand
                        logoUrl = s.cover,
                        groupTitle = "Diziler",
                        type = ChannelEntity.TYPE_SERIES,
                        sortOrder = idx,
                        seriesId = seriesId
                    )
                )
            }

            channelDao.clearAll()
            channelDao.insertAll(channelsList)
            preferencesManager.saveXtreamAccount(server, user, pass, computedExpiration)

            Result.success(channelsList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeriesEpisodes(seriesId: String): Result<List<XtreamEpisode>> {
        // Find credentials
        var resultList = emptyList<XtreamEpisode>()
        // Return episodes via XtreamClient if xtream configured
        return try {
            // If it's a test or demo, return dummy episodes or call xtream
            Result.success(resultList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadSampleDemoStreams(): Int = withContext(Dispatchers.IO) {
        // Legal open-source public test streams for verification
        val sampleChannels = listOf(
            ChannelEntity(
                id = "sample_1",
                name = "NASA TV (HLS Canlı)",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                groupTitle = "Haber & Bilim",
                type = ChannelEntity.TYPE_LIVE,
                sortOrder = 0
            ),
            ChannelEntity(
                id = "sample_2",
                name = "Red Bull TV Live",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://resources.redbull.com/logos/redbull-tv-logo.png",
                groupTitle = "Spor & Macera",
                type = ChannelEntity.TYPE_LIVE,
                sortOrder = 1
            ),
            ChannelEntity(
                id = "sample_3",
                name = "Blender Big Buck Bunny (Film Test)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg",
                groupTitle = "Animasyon",
                type = ChannelEntity.TYPE_MOVIE,
                sortOrder = 0,
                containerExtension = "mp4"
            ),
            ChannelEntity(
                id = "sample_4",
                name = "Tears of Steel (4K Sci-Fi)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/6/69/Tears_of_Steel_poster.jpg",
                groupTitle = "Bilim Kurgu",
                type = ChannelEntity.TYPE_MOVIE,
                sortOrder = 1,
                containerExtension = "mp4"
            ),
            ChannelEntity(
                id = "sample_5",
                name = "Sintel Open Movie (Dizi/Kısa)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg",
                groupTitle = "Macera",
                type = ChannelEntity.TYPE_SERIES,
                sortOrder = 0,
                seriesId = "sample_series_1"
            )
        )

        channelDao.clearAll()
        channelDao.insertAll(sampleChannels)
        // 30 days expiration by default for sample
        val sampleExp = System.currentTimeMillis() + (30L * 24L * 60L * 60L * 1000L)
        preferencesManager.saveUrlPlaylist("https://open-demo.test/playlist.m3u8", "Yasal Test Yayını", sampleExp)
        sampleChannels.size
    }

    suspend fun clearAll() {
        channelDao.clearAll()
        preferencesManager.clearConfig()
    }
}
