package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "zana_iptv_prefs")

data class PlaylistConfig(
    val type: String = "NONE", // URL, FILE, XTREAM, STALKER, NONE
    val sourceUri: String = "",
    val name: String = "",
    val expirationMillis: Long = 0L,
    val xtreamServer: String = "",
    val xtreamUser: String = "",
    val xtreamPass: String = "",
    val stalkerMac: String = "",
    val themeMode: String = THEME_DARK, // DARK, SYSTEM
    val userAgent: String = DEFAULT_USER_AGENT,
    val bufferSeconds: Int = 15,
    val parentalPin: String = "",
    val lockedCategories: Set<String> = setOf("Yetişkin", "+18", "Adult", "Erotik", "XXX"),
    val customEpgUrl: String = "",
    val subtitleSizeSp: Int = 16,
    val autoNextEpisode: Boolean = true,
    val resumePlaybackEnabled: Boolean = true
) {
    companion object {
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"
        const val DEFAULT_USER_AGENT = "ZANA-IPTV/2.0 (Linux; Android)"
    }

    fun isConfigured(): Boolean = type != "NONE" && (sourceUri.isNotBlank() || xtreamServer.isNotBlank() || stalkerMac.isNotBlank())

    val isParentalLockActive: Boolean
        get() = parentalPin.isNotBlank()

    fun isExpired(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        if (expirationMillis <= 0L) return false
        return currentTimeMillis > expirationMillis
    }

    fun daysLeft(currentTimeMillis: Long = System.currentTimeMillis()): Long? {
        if (expirationMillis <= 0L) return null
        val diff = expirationMillis - currentTimeMillis
        return if (diff <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun isNearExpiration(currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        val days = daysLeft(currentTimeMillis) ?: return false
        return !isExpired(currentTimeMillis) && days <= 3
    }
}

class PreferencesManager(private val context: Context) {

    private object Keys {
        val TYPE = stringPreferencesKey("playlist_type")
        val SOURCE_URI = stringPreferencesKey("source_uri")
        val NAME = stringPreferencesKey("playlist_name")
        val EXPIRATION_MILLIS = longPreferencesKey("expiration_millis")
        val XTREAM_SERVER = stringPreferencesKey("xtream_server")
        val XTREAM_USER = stringPreferencesKey("xtream_user")
        val XTREAM_PASS = stringPreferencesKey("xtream_pass")
        val STALKER_MAC = stringPreferencesKey("stalker_mac")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USER_AGENT = stringPreferencesKey("user_agent")
        val BUFFER_SECONDS = intPreferencesKey("buffer_seconds")
        val PARENTAL_PIN = stringPreferencesKey("parental_pin")
        val LOCKED_CATEGORIES = stringSetPreferencesKey("locked_categories")
        val CUSTOM_EPG_URL = stringPreferencesKey("custom_epg_url")
        val SUBTITLE_SIZE_SP = intPreferencesKey("subtitle_size_sp")
        val AUTO_NEXT_EPISODE = booleanPreferencesKey("auto_next_episode")
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
    }

    val configFlow: Flow<PlaylistConfig> = context.dataStore.data.map { prefs ->
        PlaylistConfig(
            type = prefs[Keys.TYPE] ?: "NONE",
            sourceUri = prefs[Keys.SOURCE_URI] ?: "",
            name = prefs[Keys.NAME] ?: "",
            expirationMillis = prefs[Keys.EXPIRATION_MILLIS] ?: 0L,
            xtreamServer = prefs[Keys.XTREAM_SERVER] ?: "",
            xtreamUser = prefs[Keys.XTREAM_USER] ?: "",
            xtreamPass = prefs[Keys.XTREAM_PASS] ?: "",
            stalkerMac = prefs[Keys.STALKER_MAC] ?: "",
            themeMode = prefs[Keys.THEME_MODE] ?: PlaylistConfig.THEME_DARK,
            userAgent = prefs[Keys.USER_AGENT] ?: PlaylistConfig.DEFAULT_USER_AGENT,
            bufferSeconds = prefs[Keys.BUFFER_SECONDS] ?: 15,
            parentalPin = prefs[Keys.PARENTAL_PIN] ?: "",
            lockedCategories = prefs[Keys.LOCKED_CATEGORIES] ?: setOf("Yetişkin", "+18", "Adult", "Erotik", "XXX"),
            customEpgUrl = prefs[Keys.CUSTOM_EPG_URL] ?: "",
            subtitleSizeSp = prefs[Keys.SUBTITLE_SIZE_SP] ?: 16,
            autoNextEpisode = prefs[Keys.AUTO_NEXT_EPISODE] ?: true,
            resumePlaybackEnabled = prefs[Keys.RESUME_PLAYBACK] ?: true
        )
    }

    suspend fun saveUrlPlaylist(url: String, name: String, expirationMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TYPE] = "URL"
            prefs[Keys.SOURCE_URI] = url
            prefs[Keys.NAME] = name.ifBlank { "M3U Playlist" }
            prefs[Keys.EXPIRATION_MILLIS] = expirationMillis
            prefs[Keys.XTREAM_SERVER] = ""
            prefs[Keys.XTREAM_USER] = ""
            prefs[Keys.XTREAM_PASS] = ""
            prefs[Keys.STALKER_MAC] = ""
        }
    }

    suspend fun saveFilePlaylist(fileName: String, expirationMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TYPE] = "FILE"
            prefs[Keys.SOURCE_URI] = fileName
            prefs[Keys.NAME] = fileName
            prefs[Keys.EXPIRATION_MILLIS] = expirationMillis
            prefs[Keys.XTREAM_SERVER] = ""
            prefs[Keys.XTREAM_USER] = ""
            prefs[Keys.XTREAM_PASS] = ""
            prefs[Keys.STALKER_MAC] = ""
        }
    }

    suspend fun saveXtreamAccount(
        server: String,
        user: String,
        pass: String,
        expirationMillis: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TYPE] = "XTREAM"
            prefs[Keys.SOURCE_URI] = server
            prefs[Keys.NAME] = "Xtream: $user"
            prefs[Keys.EXPIRATION_MILLIS] = expirationMillis
            prefs[Keys.XTREAM_SERVER] = server
            prefs[Keys.XTREAM_USER] = user
            prefs[Keys.XTREAM_PASS] = pass
            prefs[Keys.STALKER_MAC] = ""
        }
    }

    suspend fun saveStalkerPortal(
        portalUrl: String,
        macAddress: String,
        name: String,
        expirationMillis: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TYPE] = "STALKER"
            prefs[Keys.SOURCE_URI] = portalUrl
            prefs[Keys.NAME] = name.ifBlank { "Stalker Portal" }
            prefs[Keys.EXPIRATION_MILLIS] = expirationMillis
            prefs[Keys.XTREAM_SERVER] = ""
            prefs[Keys.XTREAM_USER] = ""
            prefs[Keys.XTREAM_PASS] = ""
            prefs[Keys.STALKER_MAC] = macAddress
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setUserAgent(ua: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_AGENT] = ua
        }
    }

    suspend fun setBufferSeconds(sec: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BUFFER_SECONDS] = sec
        }
    }

    suspend fun setParentalPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PARENTAL_PIN] = pin
        }
    }

    suspend fun setLockedCategories(categories: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCKED_CATEGORIES] = categories
        }
    }

    suspend fun setCustomEpgUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CUSTOM_EPG_URL] = url
        }
    }

    suspend fun setSubtitleSizeSp(sizeSp: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUBTITLE_SIZE_SP] = sizeSp
        }
    }

    suspend fun setAutoNextEpisode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_NEXT_EPISODE] = enabled
        }
    }

    suspend fun setResumePlaybackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RESUME_PLAYBACK] = enabled
        }
    }

    suspend fun updateExpiration(expirationMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXPIRATION_MILLIS] = expirationMillis
        }
    }

    suspend fun clearConfig() {
        context.dataStore.edit { prefs ->
            val currentTheme = prefs[Keys.THEME_MODE] ?: PlaylistConfig.THEME_DARK
            val pin = prefs[Keys.PARENTAL_PIN] ?: ""
            val ua = prefs[Keys.USER_AGENT] ?: PlaylistConfig.DEFAULT_USER_AGENT
            prefs.clear()
            prefs[Keys.THEME_MODE] = currentTheme
            prefs[Keys.PARENTAL_PIN] = pin
            prefs[Keys.USER_AGENT] = ua
        }
    }
}
