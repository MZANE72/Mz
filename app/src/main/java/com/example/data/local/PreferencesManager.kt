package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "nova_iptv_prefs")

data class PlaylistConfig(
    val type: String = "NONE", // URL, FILE, XTREAM, NONE
    val sourceUri: String = "",
    val name: String = "",
    val expirationMillis: Long = 0L,
    val xtreamServer: String = "",
    val xtreamUser: String = "",
    val xtreamPass: String = ""
) {
    fun isConfigured(): Boolean = type != "NONE" && (sourceUri.isNotBlank() || xtreamServer.isNotBlank())

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
    }

    val configFlow: Flow<PlaylistConfig> = context.dataStore.data.map { prefs ->
        PlaylistConfig(
            type = prefs[Keys.TYPE] ?: "NONE",
            sourceUri = prefs[Keys.SOURCE_URI] ?: "",
            name = prefs[Keys.NAME] ?: "",
            expirationMillis = prefs[Keys.EXPIRATION_MILLIS] ?: 0L,
            xtreamServer = prefs[Keys.XTREAM_SERVER] ?: "",
            xtreamUser = prefs[Keys.XTREAM_USER] ?: "",
            xtreamPass = prefs[Keys.XTREAM_PASS] ?: ""
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
        }
    }

    suspend fun updateExpiration(expirationMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EXPIRATION_MILLIS] = expirationMillis
        }
    }

    suspend fun clearConfig() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
