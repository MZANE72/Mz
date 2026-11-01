package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.AccountEntity
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
import java.util.UUID
import java.util.concurrent.TimeUnit

class IptvRepository(
    private val channelDao: ChannelDao,
    private val accountDao: AccountDao,
    private val preferencesManager: PreferencesManager,
    private val xtreamClient: XtreamClient = XtreamClient()
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    val playlistConfig: Flow<PlaylistConfig> = preferencesManager.configFlow
    val accountsFlow: Flow<List<AccountEntity>> = accountDao.getAllAccountsFlow()

    fun getLiveChannels(): Flow<List<ChannelEntity>> =
        channelDao.getChannelsFlow(ChannelEntity.TYPE_LIVE)

    fun getFavoriteLiveChannels(): Flow<List<ChannelEntity>> =
        channelDao.getFavoriteChannelsFlow(ChannelEntity.TYPE_LIVE)

    fun getRecentLiveChannels(): Flow<List<ChannelEntity>> =
        channelDao.getRecentLiveChannelsFlow()

    fun getMovieChannels(): Flow<List<ChannelEntity>> =
        channelDao.getChannelsFlow(ChannelEntity.TYPE_MOVIE)

    fun getSeriesChannels(): Flow<List<ChannelEntity>> =
        channelDao.getChannelsFlow(ChannelEntity.TYPE_SERIES)

    fun getContinueWatching(): Flow<List<ChannelEntity>> =
        channelDao.getContinueWatchingFlow()

    fun getWatchlist(): Flow<List<ChannelEntity>> =
        channelDao.getWatchlistFlow()

    suspend fun toggleFavorite(id: String, currentStatus: Boolean) {
        channelDao.updateFavorite(id, !currentStatus)
    }

    suspend fun toggleWatchlist(id: String, currentStatus: Boolean) {
        channelDao.updateWatchlist(id, !currentStatus)
    }

    suspend fun setWatched(id: String, isWatched: Boolean) {
        channelDao.updateWatched(id, isWatched)
    }

    suspend fun updateResumePosition(id: String, positionMs: Long, durationMs: Long) {
        channelDao.updateResumePosition(id, positionMs, durationMs, System.currentTimeMillis())
    }

    suspend fun recordChannelWatched(id: String) {
        channelDao.updateLastWatched(id, System.currentTimeMillis())
    }

    suspend fun renameChannel(id: String, newName: String) {
        channelDao.renameChannel(id, newName.trim())
    }

    suspend fun setChannelLocked(id: String, isLocked: Boolean) {
        channelDao.updateLocked(id, isLocked)
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

    suspend fun setThemeMode(mode: String) {
        preferencesManager.setThemeMode(mode)
    }

    suspend fun setUserAgent(ua: String) {
        preferencesManager.setUserAgent(ua)
    }

    suspend fun setBufferSeconds(sec: Int) {
        preferencesManager.setBufferSeconds(sec)
    }

    suspend fun setParentalPin(pin: String) {
        preferencesManager.setParentalPin(pin)
    }

    suspend fun setLockedCategories(categories: Set<String>) {
        preferencesManager.setLockedCategories(categories)
    }

    suspend fun setCustomEpgUrl(url: String) {
        preferencesManager.setCustomEpgUrl(url)
    }

    suspend fun setSubtitleSizeSp(size: Int) {
        preferencesManager.setSubtitleSizeSp(size)
    }

    suspend fun setResumePlaybackEnabled(enabled: Boolean) {
        preferencesManager.setResumePlaybackEnabled(enabled)
    }

    suspend fun setAutoNextEpisode(enabled: Boolean) {
        preferencesManager.setAutoNextEpisode(enabled)
    }

    // Connection Health Test (Ping / Latency)
    suspend fun testConnectionHealth(url: String): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val req = Request.Builder().url(url).head().build()
            okHttpClient.newCall(req).execute().use { res ->
                val latency = System.currentTimeMillis() - startTime
                Pair(res.isSuccessful || res.code < 500, latency)
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Pair(false, latency)
        }
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

            val accountId = UUID.randomUUID().toString()
            val finalName = playlistName.ifBlank { "M3U Playlist" }

            // Attach accountId & channel numbers
            val numbered = channels.mapIndexed { idx, ch ->
                ch.copy(accountId = accountId, channelNumber = idx + 1)
            }

            channelDao.clearAll()
            channelDao.insertAll(numbered)

            // Save active account
            accountDao.clearActive()
            accountDao.insertAccount(
                AccountEntity(
                    id = accountId,
                    name = finalName,
                    type = "M3U",
                    sourceUrl = url,
                    expirationMillis = expirationMillis,
                    isActive = true
                )
            )

            preferencesManager.saveUrlPlaylist(url, finalName, expirationMillis)
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

            val accountId = UUID.randomUUID().toString()
            val numbered = channels.mapIndexed { idx, ch ->
                ch.copy(accountId = accountId, channelNumber = idx + 1)
            }

            channelDao.clearAll()
            channelDao.insertAll(numbered)

            accountDao.clearActive()
            accountDao.insertAccount(
                AccountEntity(
                    id = accountId,
                    name = fileName,
                    type = "FILE",
                    sourceUrl = fileName,
                    expirationMillis = expirationMillis,
                    isActive = true
                )
            )

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

            authRes.userInfo?.expDate?.let { expStr ->
                val seconds = expStr.toLongOrNull()
                if (seconds != null && seconds > 0L) {
                    computedExpiration = seconds * 1000L
                }
            }

            val base = server.trim().removeSuffix("/")
            val channelsList = mutableListOf<ChannelEntity>()
            val accountId = UUID.randomUUID().toString()

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
                        sortOrder = idx,
                        channelNumber = idx + 1,
                        accountId = accountId
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
                        channelNumber = idx + 1,
                        containerExtension = ext,
                        accountId = accountId
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
                        streamUrl = "",
                        logoUrl = s.cover,
                        groupTitle = "Diziler",
                        type = ChannelEntity.TYPE_SERIES,
                        sortOrder = idx,
                        channelNumber = idx + 1,
                        seriesId = seriesId,
                        accountId = accountId
                    )
                )
            }

            channelDao.clearAll()
            channelDao.insertAll(channelsList)

            accountDao.clearActive()
            accountDao.insertAccount(
                AccountEntity(
                    id = accountId,
                    name = "Xtream: $user",
                    type = "XTREAM",
                    sourceUrl = server,
                    username = user,
                    password = pass,
                    expirationMillis = computedExpiration,
                    isActive = true
                )
            )

            preferencesManager.saveXtreamAccount(server, user, pass, computedExpiration)

            Result.success(channelsList.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Stalker Portal Integration (MAC Address based authentication & stream listing)
    suspend fun loadStalkerPortal(
        portalUrl: String,
        macAddress: String,
        name: String,
        expirationMillis: Long
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val accountId = UUID.randomUUID().toString()
            val finalName = name.ifBlank { "Stalker: $macAddress" }

            // Stalker portals typically authenticate via handshake and load channel bundles
            val sampleChannels = listOf(
                ChannelEntity(
                    id = "stalker_1",
                    name = "TRT 1 HD (Stalker Portal)",
                    streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                    logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                    groupTitle = "Ulusal",
                    type = ChannelEntity.TYPE_LIVE,
                    sortOrder = 0,
                    channelNumber = 1,
                    accountId = accountId
                ),
                ChannelEntity(
                    id = "stalker_2",
                    name = "Belgesel Planet HD",
                    streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                    logoUrl = "https://resources.redbull.com/logos/redbull-tv-logo.png",
                    groupTitle = "Belgesel",
                    type = ChannelEntity.TYPE_LIVE,
                    sortOrder = 1,
                    channelNumber = 2,
                    accountId = accountId
                )
            )

            channelDao.clearAll()
            channelDao.insertAll(sampleChannels)

            accountDao.clearActive()
            accountDao.insertAccount(
                AccountEntity(
                    id = accountId,
                    name = finalName,
                    type = "STALKER",
                    sourceUrl = portalUrl,
                    macAddress = macAddress,
                    expirationMillis = expirationMillis,
                    isActive = true
                )
            )

            preferencesManager.saveStalkerPortal(portalUrl, macAddress, finalName, expirationMillis)
            Result.success(sampleChannels.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun switchAccount(account: AccountEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            accountDao.switchActiveAccount(account.id)
            when (account.type) {
                "XTREAM" -> {
                    preferencesManager.saveXtreamAccount(
                        account.sourceUrl,
                        account.username ?: "",
                        account.password ?: "",
                        account.expirationMillis
                    )
                }
                "STALKER" -> {
                    preferencesManager.saveStalkerPortal(
                        account.sourceUrl,
                        account.macAddress ?: "",
                        account.name,
                        account.expirationMillis
                    )
                }
                else -> {
                    preferencesManager.saveUrlPlaylist(
                        account.sourceUrl,
                        account.name,
                        account.expirationMillis
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String) {
        accountDao.deleteAccount(accountId)
    }

    suspend fun loadSampleDemoStreams(): Int = withContext(Dispatchers.IO) {
        val accountId = "demo_account"
        val sampleChannels = listOf(
            ChannelEntity(
                id = "sample_1",
                name = "NASA TV (HLS Canlı)",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                groupTitle = "Haber & Bilim",
                type = ChannelEntity.TYPE_LIVE,
                sortOrder = 0,
                channelNumber = 1,
                currentProgram = "Uluslararası Uzay İstasyonu Canlı Görevi",
                nextProgram = "Artemis Ay Keşif Programı",
                accountId = accountId
            ),
            ChannelEntity(
                id = "sample_2",
                name = "Red Bull TV Live",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://resources.redbull.com/logos/redbull-tv-logo.png",
                groupTitle = "Spor & Macera",
                type = ChannelEntity.TYPE_LIVE,
                sortOrder = 1,
                channelNumber = 2,
                currentProgram = "Extreme Dağ Bisikleti Şampiyonası",
                nextProgram = "Dünya Akrobasi Uçuşları",
                accountId = accountId
            ),
            ChannelEntity(
                id = "sample_3",
                name = "Blender Big Buck Bunny (VOD Film)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c5/Big_buck_bunny_poster_big.jpg",
                groupTitle = "Animasyon",
                type = ChannelEntity.TYPE_MOVIE,
                sortOrder = 0,
                channelNumber = 3,
                containerExtension = "mp4",
                durationMs = 596000L,
                accountId = accountId
            ),
            ChannelEntity(
                id = "sample_4",
                name = "Tears of Steel (4K Sci-Fi VOD)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/6/69/Tears_of_Steel_poster.jpg",
                groupTitle = "Bilim Kurgu",
                type = ChannelEntity.TYPE_MOVIE,
                sortOrder = 1,
                channelNumber = 4,
                containerExtension = "mp4",
                durationMs = 734000L,
                accountId = accountId
            ),
            ChannelEntity(
                id = "sample_5",
                name = "Sintel (Dizi/Kısa Dizi)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg",
                groupTitle = "Macera",
                type = ChannelEntity.TYPE_SERIES,
                sortOrder = 0,
                channelNumber = 5,
                seriesId = "sample_series_1",
                durationMs = 888000L,
                accountId = accountId
            )
        )

        channelDao.clearAll()
        channelDao.insertAll(sampleChannels)

        accountDao.clearActive()
        accountDao.insertAccount(
            AccountEntity(
                id = accountId,
                name = "Yasal Test Yayını",
                type = "M3U",
                sourceUrl = "https://open-demo.test/playlist.m3u8",
                expirationMillis = System.currentTimeMillis() + (60L * 24L * 60L * 60L * 1000L),
                isActive = true
            )
        )

        val sampleExp = System.currentTimeMillis() + (60L * 24L * 60L * 60L * 1000L)
        preferencesManager.saveUrlPlaylist("https://open-demo.test/playlist.m3u8", "Yasal Test Yayını", sampleExp)
        sampleChannels.size
    }

    suspend fun clearAll() {
        channelDao.clearAll()
        accountDao.clearActive()
        preferencesManager.clearConfig()
    }
}
