package com.example.data.remote

import com.example.data.local.ChannelEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.regex.Pattern

object M3uParser {

    private val TVG_NAME_PATTERN = Pattern.compile("""tvg-name=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_PATTERN = Pattern.compile("""tvg-logo=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_PATTERN = Pattern.compile("""group-title=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)
    private val TVG_ID_PATTERN = Pattern.compile("""tvg-id=["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)

    fun parse(inputStream: InputStream): List<ChannelEntity> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val channels = mutableListOf<ChannelEntity>()
        var currentLine: String?
        var tempName: String? = null
        var tempLogo: String? = null
        var tempGroup: String? = null
        var index = 0

        while (reader.readLine().also { currentLine = it } != null) {
            val line = currentLine?.trim() ?: continue
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse attributes
                tempLogo = extractAttribute(line, TVG_LOGO_PATTERN)
                tempGroup = extractAttribute(line, GROUP_TITLE_PATTERN) ?: "Genel Kanallar"
                val tvgName = extractAttribute(line, TVG_NAME_PATTERN)

                // The title is everything after the last comma
                val commaIndex = line.lastIndexOf(',')
                val displayTitle = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    tvgName ?: "Kanal ${index + 1}"
                }
                tempName = if (displayTitle.isNotBlank()) displayTitle else (tvgName ?: "Kanal ${index + 1}")
            } else if (!line.startsWith("#")) {
                // This is a stream URL
                val streamUrl = line
                if (streamUrl.startsWith("http://", ignoreCase = true) ||
                    streamUrl.startsWith("https://", ignoreCase = true) ||
                    streamUrl.startsWith("rtmp://", ignoreCase = true) ||
                    streamUrl.startsWith("rtsp://", ignoreCase = true)
                ) {
                    val name = tempName ?: "Kanal ${index + 1}"
                    val group = tempGroup ?: "Genel Kanallar"
                    val type = detectType(name, group, streamUrl)
                    val id = generateHash(streamUrl + "_" + name)

                    channels.add(
                        ChannelEntity(
                            id = id,
                            name = name,
                            streamUrl = streamUrl,
                            logoUrl = tempLogo,
                            groupTitle = group,
                            type = type,
                            sortOrder = index
                        )
                    )
                    index++
                }
                // Reset temporary fields
                tempName = null
                tempLogo = null
                tempGroup = null
            }
        }
        return channels
    }

    private fun extractAttribute(line: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(line)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }

    private fun detectType(name: String, group: String, url: String): String {
        val lowerGroup = group.lowercase()
        val lowerName = name.lowercase()
        val lowerUrl = url.lowercase()

        return when {
            lowerGroup.contains("film") || lowerGroup.contains("movie") || lowerGroup.contains("vod") || lowerGroup.contains("sinema") ||
                    lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".avi") -> {
                ChannelEntity.TYPE_MOVIE
            }
            lowerGroup.contains("dizi") || lowerGroup.contains("series") || lowerGroup.contains("season") || lowerGroup.contains("sezon") -> {
                ChannelEntity.TYPE_SERIES
            }
            else -> ChannelEntity.TYPE_LIVE
        }
    }

    private fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
