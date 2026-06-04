package org.audiocular.peq.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AndroidPersistenceManager(val context: Context) : PersistenceManager {
    private val prefs: android.content.SharedPreferences by lazy {
        context.getSharedPreferences("pEQ_production_vault", android.content.Context.MODE_PRIVATE)
    }
    private val json = Json { ignoreUnknownKeys = true }

  override var openSubtitlesApiKey: String

    get() = prefs.getString("os_api_key", "") ?: ""

    set(value) = prefs.edit().putString("os_api_key", value).apply()

override fun saveOpenSubtitlesApiKey(key: String) {

    openSubtitlesApiKey = key

}
    override fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }

    override fun saveLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun saveSession(state: SessionState) {
        try {
            val serialized = json.encodeToString(state)
            saveString("neural_session", serialized)
            
            // Sync to widget prefs for large widget queue view
            val widgetPrefs = context.getSharedPreferences("peq_widget_prefs", Context.MODE_PRIVATE)
            val queueTitles = state.queue.map { it.title }
            widgetPrefs.edit().putString("queue_json", json.encodeToString(queueTitles)).apply()
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving session: ${e.message}")
        }
    }

    override fun loadSession(): SessionState? {
        val serialized = getString("neural_session") ?: return null
        return try {
            json.decodeFromString<SessionState>(serialized)
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error loading session: ${e.message}")
            null
        }
    }

    override fun updateNowPlaying(item: pEQMediaItem?, isPlaying: Boolean) {
        val widgetPrefs = context.getSharedPreferences("peq_widget_prefs", android.content.Context.MODE_PRIVATE)
        val registryPrefs = context.getSharedPreferences("pEQ_workstation_registry", android.content.Context.MODE_PRIVATE)
        
        widgetPrefs.edit().apply {
            putBoolean("is_playing", isPlaying)
            if (item != null) {
                putString("last_track", item.title)
                putString("last_artist", item.artist ?: item.subtitle)
                putString("last_art", item.thumbnail)
                putString("last_codec", item.format)
                putString("last_resolution", item.resolution ?: "${item.sampleRate}Hz")
                putString("last_bitrate", "${item.bitDepth}-bit")
                // Path to cached artwork
                val file = File(context.cacheDir, "widget_art.png")
                if (file.exists()) {
                    putString("last_art_path", file.absolutePath)
                } else {
                    remove("last_art_path")
                }
            }
            apply()
        }

        registryPrefs.edit().apply {
             putBoolean("is_playing", isPlaying)
             if (item != null) {
                 putString("last_track", item.title)
                 putString("last_artist", item.artist ?: item.subtitle)
                 putString("last_art", item.thumbnail)
             }
             apply()
        }
    }

    override fun updatePlaybackState(isShuffle: Boolean, repeatMode: String) {
        val widgetPrefs = context.getSharedPreferences("peq_widget_prefs", android.content.Context.MODE_PRIVATE)
        widgetPrefs.edit().apply {
            putBoolean("is_shuffle", isShuffle)
            putString("repeat_mode", repeatMode)
            apply()
        }
    }

    override fun updateHardwareState(settings: Map<String, String>) {
        val registryPrefs = context.getSharedPreferences("pEQ_workstation_registry", android.content.Context.MODE_PRIVATE)
        registryPrefs.edit().apply {
            settings.forEach { (k, v) -> putString(k, v) }
            apply()
        }
    }

    override fun saveFavorites(paths: List<String>) {
        try {
            val serialized = json.encodeToString(paths)
            saveString("pEQ_favorites_paths", serialized)
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving favorites: ${e.message}")
        }
    }

    override fun loadFavorites(): List<String> {
        val serialized = getString("pEQ_favorites_paths") ?: return emptyList()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun savePlaylists(playlists: Map<String, List<String>>) {
        try {
            val serialized = json.encodeToString(playlists)
            saveString("pEQ_playlists", serialized)
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving playlists: ${e.message}")
        }
    }

    override fun loadPlaylists(): Map<String, List<String>> {
        val serialized = getString("pEQ_playlists") ?: return emptyMap()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override fun saveScanFolders(paths: List<String>) {
        try {
            val serialized = json.encodeToString(paths)
            saveString("pEQ_scan_folders", serialized)
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving scan folders: ${e.message}")
        }
    }

    override fun loadScanFolders(): List<String> {
        val serialized = getString("pEQ_scan_folders") ?: return emptyList()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun saveRecentSessions(paths: List<String>) {
        try {
            val serialized = json.encodeToString(paths)
            saveString("pEQ_recent_sessions", serialized)
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving recent sessions: ${e.message}")
        }
    }

    override fun loadRecentSessions(): List<String> {
        val serialized = getString("pEQ_recent_sessions") ?: return emptyList()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun saveLibraryState(currentPath: String, showFolders: Boolean) {
        saveString("pEQ_last_folder_path", currentPath)
        saveString("pEQ_show_folders", showFolders.toString())
    }

    override fun loadLibraryState(): Pair<String, Boolean> {
        val path = getString("pEQ_last_folder_path", "") ?: ""
        val show = getString("pEQ_show_folders", "false") == "true"
        return Pair(path, show)
    }

    override fun saveMostPlayed(playCounts: Map<String, Int>) {
        try {
            val serialized = json.encodeToString(playCounts)
            saveString("pEQ_play_counts", serialized)
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving play counts: ${e.message}")
        }
    }

    override fun loadMostPlayed(): Map<String, Int> {
        val serialized = getString("pEQ_play_counts") ?: return emptyMap()
        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

private var _instance: PersistenceManager? = null

actual fun getPersistenceManager(): PersistenceManager {
    return _instance ?: error("PersistenceManager not initialized. Call initPersistenceManager() first.")
}

fun initPersistenceManager(context: Context) {
    _instance = AndroidPersistenceManager(context)
}

