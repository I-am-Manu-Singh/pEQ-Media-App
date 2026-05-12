package org.audiocular.peq.app

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.audiocular.peq.app.data.PersistenceManager
import org.audiocular.peq.app.data.SessionState
import org.audiocular.peq.app.data.pEQMediaItem
import java.io.File

class DesktopPersistenceManager : PersistenceManager {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val configFile = File(System.getProperty("user.home"), ".pEQ/config.json")
    private var data = mutableMapOf<String, String>()

    init {
        if (!configFile.parentFile.exists()) configFile.parentFile.mkdirs()
        if (configFile.exists()) {
            try {
                data = json.decodeFromString<Map<String, String>>(configFile.readText()).toMutableMap()
            } catch (e: Exception) {
                println("pEQControl: [PERSISTENCE] Error loading config: ${e.message}")
            }
        }
    }

    private fun save() {
        try {
            configFile.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            println("pEQControl: [PERSISTENCE] Error saving config: ${e.message}")
        }
    }

    override fun saveString(key: String, value: String) {
        data[key] = value
        save()
    }

    override fun getString(key: String, defaultValue: String?): String? = data[key] ?: defaultValue

    override fun saveLong(key: String, value: Long) {
        data[key] = value.toString()
        save()
    }

    override fun getLong(key: String, defaultValue: Long): Long = data[key]?.toLongOrNull() ?: defaultValue

    override fun remove(key: String) {
        data.remove(key)
        save()
    }

    override fun saveSession(state: SessionState) {
        saveString("last_session", json.encodeToString(state))
    }

    override fun loadSession(): SessionState? {
        return getString("last_session")?.let {
            try { json.decodeFromString<SessionState>(it) } catch (e: Exception) { null }
        }
    }
    
    override fun updateNowPlaying(item: pEQMediaItem?, isPlaying: Boolean) {}
    override fun updatePlaybackState(isShuffle: Boolean, repeatMode: String) {
        saveString("shuffle", isShuffle.toString())
        saveString("repeat_mode", repeatMode)
    }
    override fun updateHardwareState(settings: Map<String, String>) {}
    
    override fun saveFavorites(paths: List<String>) {
        saveString("favorites", json.encodeToString(paths))
    }

    override fun loadFavorites(): List<String> {
        return getString("favorites")?.let {
            try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    override fun savePlaylists(playlists: Map<String, List<String>>) {
        saveString("playlists", json.encodeToString(playlists))
    }

    override fun loadPlaylists(): Map<String, List<String>> {
        return getString("playlists")?.let {
            try { json.decodeFromString<Map<String, List<String>>>(it) } catch (e: Exception) { emptyMap() }
        } ?: emptyMap()
    }

    override fun saveScanFolders(paths: List<String>) {
        saveString("scan_folders", json.encodeToString(paths))
    }

    override fun loadScanFolders(): List<String> {
        return getString("scan_folders")?.let {
            try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }

    override fun saveRecentSessions(paths: List<String>) {
        saveString("recent_sessions", json.encodeToString(paths))
    }

    override fun loadRecentSessions(): List<String> {
        return getString("recent_sessions")?.let {
            try { json.decodeFromString<List<String>>(it) } catch (e: Exception) { emptyList() }
        } ?: emptyList()
    }
    
    override fun saveLibraryState(currentPath: String, showFolders: Boolean) {
        saveString("library_path", currentPath)
        saveString("library_show_folders", showFolders.toString())
    }

    override fun loadLibraryState(): Pair<String, Boolean> {
        val path = getString("library_path", "") ?: ""
        val show = getString("library_show_folders", "true")?.toBoolean() ?: true
        return Pair(path, show)
    }
    
    override fun saveMostPlayed(playCounts: Map<String, Int>) {
        saveString("most_played", json.encodeToString(playCounts))
    }

    override fun loadMostPlayed(): Map<String, Int> {
        return getString("most_played")?.let {
            try { json.decodeFromString<Map<String, Int>>(it) } catch (e: Exception) { emptyMap() }
        } ?: emptyMap()
    }
}