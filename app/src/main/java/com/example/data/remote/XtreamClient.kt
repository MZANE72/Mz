package com.example.data.remote

import com.example.data.model.XtreamAuthResponse
import com.example.data.model.XtreamEpisode
import com.example.data.model.XtreamLiveStream
import com.example.data.model.XtreamSeriesItem
import com.example.data.model.XtreamVodStream
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class XtreamClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private fun normalizeServerUrl(rawServer: String): String {
        var url = rawServer.trim()
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            url = "http://$url"
        }
        return url.removeSuffix("/")
    }

    suspend fun authenticate(server: String, user: String, pass: String): Result<XtreamAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val base = normalizeServerUrl(server)
            val url = "$base/player_api.php?username=$user&password=$pass"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Sunucu hatası: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Boş sunucu yanıtı"))
                val adapter = moshi.adapter(XtreamAuthResponse::class.java)
                val parsed = adapter.fromJson(body)
                if (parsed?.userInfo != null) {
                    Result.success(parsed)
                } else {
                    Result.failure(Exception("Giriş bilgileri geçersiz veya sunucu yanıt vermiyor"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLiveStreams(server: String, user: String, pass: String): Result<List<XtreamLiveStream>> = withContext(Dispatchers.IO) {
        try {
            val base = normalizeServerUrl(server)
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Hata: ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val listType = Types.newParameterizedType(List::class.java, XtreamLiveStream::class.java)
                val adapter = moshi.adapter<List<XtreamLiveStream>>(listType)
                val list = adapter.fromJson(body) ?: emptyList()
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVodStreams(server: String, user: String, pass: String): Result<List<XtreamVodStream>> = withContext(Dispatchers.IO) {
        try {
            val base = normalizeServerUrl(server)
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_vod_streams"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Hata: ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val listType = Types.newParameterizedType(List::class.java, XtreamVodStream::class.java)
                val adapter = moshi.adapter<List<XtreamVodStream>>(listType)
                val list = adapter.fromJson(body) ?: emptyList()
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeries(server: String, user: String, pass: String): Result<List<XtreamSeriesItem>> = withContext(Dispatchers.IO) {
        try {
            val base = normalizeServerUrl(server)
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_series"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Hata: ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())
                val listType = Types.newParameterizedType(List::class.java, XtreamSeriesItem::class.java)
                val adapter = moshi.adapter<List<XtreamSeriesItem>>(listType)
                val list = adapter.fromJson(body) ?: emptyList()
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSeriesEpisodes(
        server: String,
        user: String,
        pass: String,
        seriesId: String
    ): Result<List<XtreamEpisode>> = withContext(Dispatchers.IO) {
        try {
            val base = normalizeServerUrl(server)
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_series_info&series_id=$seriesId"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Hata: ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.success(emptyList())

                val json = JSONObject(body)
                val episodesList = mutableListOf<XtreamEpisode>()

                if (json.has("episodes")) {
                    val episodesObj = json.getJSONObject("episodes")
                    val seasonKeys = episodesObj.keys()
                    while (seasonKeys.hasNext()) {
                        val seasonKey = seasonKeys.next()
                        val seasonNum = seasonKey.toIntOrNull() ?: 1
                        val epArray = episodesObj.getJSONArray(seasonKey)
                        for (i in 0 until epArray.length()) {
                            val epObj = epArray.getJSONObject(i)
                            val epId = epObj.optString("id", "")
                            val title = epObj.optString("title", "Bölüm ${i + 1}")
                            val epNum = epObj.optInt("episode_num", i + 1)
                            val ext = epObj.optString("container_extension", "mp4").ifBlank { "mp4" }
                            val streamUrl = "$base/series/$user/$pass/$epId.$ext"
                            episodesList.add(
                                XtreamEpisode(
                                    id = epId,
                                    title = title,
                                    seasonNum = seasonNum,
                                    episodeNum = epNum,
                                    containerExtension = ext,
                                    streamUrl = streamUrl
                                )
                            )
                        }
                    }
                }
                Result.success(episodesList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
