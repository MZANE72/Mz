package com.example

import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistConfig
import com.example.data.remote.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ExampleUnitTest {

    @Test
    fun `parse sample m3u content correctly`() {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="TRT1" tvg-name="TRT 1 HD" tvg-logo="https://example.com/trt1.png" group-title="Ulusal",TRT 1 HD
            http://example.com/trt1/stream.m3u8
            #EXTINF:-1 group-title="Filmler",Interstellar (2014)
            http://example.com/movies/interstellar.mp4
        """.trimIndent()

        val parsed = M3uParser.parse(ByteArrayInputStream(m3uContent.toByteArray()))
        assertEquals(2, parsed.size)

        val liveChannel = parsed[0]
        assertEquals("TRT 1 HD", liveChannel.name)
        assertEquals("Ulusal", liveChannel.groupTitle)
        assertEquals("https://example.com/trt1.png", liveChannel.logoUrl)
        assertEquals(ChannelEntity.TYPE_LIVE, liveChannel.type)

        val movieChannel = parsed[1]
        assertEquals("Interstellar (2014)", movieChannel.name)
        assertEquals("Filmler", movieChannel.groupTitle)
        assertEquals(ChannelEntity.TYPE_MOVIE, movieChannel.type)
    }

    @Test
    fun `verify expiration detection logic`() {
        val now = 1000000000000L

        // Expired config
        val expiredConfig = PlaylistConfig(
            type = "URL",
            sourceUri = "http://test.m3u",
            expirationMillis = now - 50000L
        )
        assertTrue(expiredConfig.isExpired(now))

        // Active config (5 days left)
        val activeConfig = PlaylistConfig(
            type = "URL",
            sourceUri = "http://test.m3u",
            expirationMillis = now + (5L * 24L * 60L * 60L * 1000L)
        )
        assertFalse(activeConfig.isExpired(now))
        assertFalse(activeConfig.isNearExpiration(now))

        // Near expiration (2 days left)
        val nearExpiryConfig = PlaylistConfig(
            type = "URL",
            sourceUri = "http://test.m3u",
            expirationMillis = now + (2L * 24L * 60L * 60L * 1000L)
        )
        assertFalse(nearExpiryConfig.isExpired(now))
        assertTrue(nearExpiryConfig.isNearExpiration(now))
    }
}
