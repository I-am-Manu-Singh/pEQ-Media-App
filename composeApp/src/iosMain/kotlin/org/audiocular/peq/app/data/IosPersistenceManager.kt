package org.audiocular.peq.app.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class IosPersistenceManager : PersistenceManager {
    private val json = Json { ignoreUnknownKeys = true }
    private val data = mutableMapOf<String, String>()

    override var openSubtitlesApiKey: String
        get() = getString("os_api_key", "") ?: ""
        set(value) {
            saveString("os_api_key", value)
        }

    override fun saveOpenSubtitlesApiKey(key: String) {
        openSubtitlesApiKey = key
    }

    override fun saveString(key: String, value: String) {
        data[key] = value
    }

    override fun getString(key: String, defaultValue: String?): String? = data[key] ?: defaultValue

    override fun saveLong(key: String, value: Long) {
        data[key] = value.toString()
    }

    override fun getLong(key: String, defaultValue: Long): Long = data[key]?.toLongOrNull() ?: defaultValue

    override fun remove(key: String) {
        data.remove(key)
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

actual fun getPersistenceManager(): PersistenceManager = IosPersistenceManager()
