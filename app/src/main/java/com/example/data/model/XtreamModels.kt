package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username") val username: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "exp_date") val expDate: String? = null,
    @Json(name = "is_trial") val isTrial: String? = null,
    @Json(name = "active_cons") val activeCons: String? = null,
    @Json(name = "max_connections") val maxConnections: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "port") val port: String? = null,
    @Json(name = "server_protocol") val serverProtocol: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "category_name") val categoryName: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStream(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_type") val streamType: String? = null,
    @Json(name = "stream_id") val streamId: Any? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "category_name") val categoryName: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamVodStream(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_id") val streamId: Any? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "container_extension") val containerExtension: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "rating") val rating: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamSeriesItem(
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "series_id") val seriesId: Any? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "plot") val plot: String? = null,
    @Json(name = "cast") val cast: String? = null,
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "category_id") val categoryId: String? = null
)

data class XtreamEpisode(
    val id: String,
    val title: String,
    val seasonNum: Int,
    val episodeNum: Int,
    val containerExtension: String = "mp4",
    val streamUrl: String
)
