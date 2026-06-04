package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import org.audiocular.peq.app.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
/**
 * Extension for AppViewModel to handle Library scanning and File management.
 */

fun AppViewModel.refreshLibrary() {
    println("pEQControl: [LIBRARY] Refreshing local indices")
    scanner?.scan(scanFolders.toList(), emptyList())
}

fun AppViewModel.openFolder(path: String) {
    currentFolder.value = path.replace("file://", "").trim()
    currentFolderPath.value = currentFolder.value ?: ""
}

fun AppViewModel.closeFolder() {
    currentFolder.value = null
    currentFolderPath.value = ""
}

fun AppViewModel.openCurrentItemFolder() {
    val currentItem = currentMediaItem.value ?: return
    currentFolder.value = currentItem.path.replace("file://", "").substringBeforeLast("/")
}

fun AppViewModel.observeScanner() {
    val scanner = _scanner ?: return
    scope.launch {
        combine(
            scanner.audioFiles, 
            scanner.mediaFiles, 
            snapshotFlow { playCounts.toMap() },
            snapshotFlow { lastPlayed.toMap() }
        ) { audio, video, counts, times ->
            val audioItems = audio.map {
                pEQMediaItem(
                    id = it.id,
                    title = it.title ?: it.name,
                    subtitle = it.artist ?: "Unknown Artist",
                    path = it.path,
                    type = MediaType.AUDIO,
                    format = it.format,
                    sizeBytes = it.sizeBytes,
                    durationMs = it.duration,
                    artist = it.artist,
                    album = it.album,
                    bitDepth = it.bitDepth,
                    sampleRate = it.sampleRate,
                    bitrate = it.bitrate,
                    thumbnail = it.thumbnail,
                    lyrics = it.lyrics,
                    playCount = counts[it.path] ?: 0,
                    lastPlayed = times[it.path] ?: 0L
                )
            }
            val videoItems = video.map {
                pEQMediaItem(
                    id = it.id,
                    title = it.title ?: it.name,
                    subtitle = it.resolution,
                    path = it.path,
                    type = MediaType.VIDEO,
                    format = it.codec,
                    sizeBytes = it.sizeBytes,
                    durationMs = it.duration,
                    resolution = it.resolution,
                    codec = it.codec,
                    thumbnail = it.thumbnail,
                    playCount = counts[it.path] ?: 0,
                    lastPlayed = times[it.path] ?: 0L
                )
            }
            audioItems + videoItems
        }.collect { items ->
            _mediaLibrary.value = items
        }
    }

    scope.launch {
        scanner.scanProgress.collect { scanProgress.value = it }
    }
    scope.launch {
        scanner.scanStatus.collect { scanStatus.value = it }
    }
}

// 📁 Playlist & Archive Management
fun AppViewModel.createPlaylist(name: String) {
    if (!customPlaylists.containsKey(name)) {
        customPlaylists[name] = mutableStateListOf()
    }
    persistSession()
}

fun AppViewModel.addToPlaylist(playlist: String, item: pEQMediaItem) {
    val list = customPlaylists.getOrPut(playlist) { mutableStateListOf() }
    if (list.none { it.path == item.path }) {
        list.add(item)
        showToast("Added to $playlist")
    }
    persistSession()
}

fun AppViewModel.removeFromPlaylist(playlist: String, item: pEQMediaItem) {
    if (playlist == "FAVORITES") {
        toggleFavorite(item)
    } else {
        customPlaylists[playlist]?.remove(item)
        persistSession()
    }
}

fun AppViewModel.reorderPlaylist(playlist: String, from: Int, to: Int) {
    val list = if (playlist == "FAVORITES") favoriteItems else customPlaylists[playlist]
    list?.let {
        if (from in it.indices && to in it.indices) {
            val item = it.removeAt(from)
            it.add(to, item)
            persistSession()
        }
    }
}

fun AppViewModel.deletePlaylist(playlist: String) {
    customPlaylists.remove(playlist)
    persistSession()
}

fun AppViewModel.toggleFavorite(item: pEQMediaItem) {
    if (favoriteItems.any { it.path == item.path }) {
        favoriteItems.removeAll { it.path == item.path }
        favorites.remove(item.id)
    } else {
        favoriteItems.add(item)
        favorites.add(item.id)
    }
    persistSession()
}

fun AppViewModel.navigateToFolder(path: String) {
    if (currentFolderPath.value.isNotEmpty()) {
        folderHistory.add(currentFolderPath.value)
    }
    folderFuture.clear()
    currentFolderPath.value = path
    currentFolder.value = path
}

fun AppViewModel.toggleFolderView() {
    showFolders.value = !showFolders.value
    if (showFolders.value && currentFolder.value != null && currentFolderPath.value.isEmpty()) {
        currentFolderPath.value = currentFolder.value ?: ""
    }
}

fun AppViewModel.navigateBackFolder() {
    if (folderHistory.isNotEmpty()) {
        folderFuture.add(currentFolderPath.value)
        currentFolderPath.value = folderHistory.removeAt(folderHistory.size - 1)
    } else {
        currentFolderPath.value = ""
    }
}

fun AppViewModel.navigateForwardFolder() {
    if (folderFuture.isNotEmpty()) {
        folderHistory.add(currentFolderPath.value)
        currentFolderPath.value = folderFuture.removeAt(folderFuture.size - 1)
    }
}

fun AppViewModel.addScanFolder(path: String) {
    if (!scanFolders.contains(path)) {
        scanFolders.add(path)
        persistenceManager.saveScanFolders(scanFolders.toList())
        refreshLibrary()
    }
}

fun AppViewModel.removeScanFolder(path: String) {
    scanFolders.remove(path)
    persistenceManager.saveScanFolders(scanFolders.toList())
    refreshLibrary()
}

fun AppViewModel.requestFolderPicker() {
    onPickFolderRequested?.invoke()
}

fun AppViewModel.observeLibraryState() {
    scope.launch {
        snapshotFlow {
            Pair(currentFolderPath.value, showFolders.value)
        }.collect { (path, show) ->
            persistenceManager.saveLibraryState(path, show)
        }
    }
}

fun AppViewModel.clearPlaybackHistory() {
    recentlyPlayedItems.clear()
    persistenceManager.saveRecentSessions(emptyList())
}

fun AppViewModel.toggleSelection(item: pEQMediaItem) {
    if (selectedItems.contains(item)) {
        selectedItems.remove(item)
    } else {
        selectedItems.add(item)
    }
}

fun AppViewModel.updateMostPlayed(item: pEQMediaItem) {
    val count = (playCounts[item.path] ?: 0) + 1
    playCounts[item.path] = count
    lastPlayed[item.path] = Clock.System.now().toEpochMilliseconds()
    
    // Refresh most played list
    val sorted = mediaLibrary.value
        .filter { (playCounts[it.path] ?: 0) > 0 }
        .sortedByDescending { playCounts[it.path] ?: 0 }
        .take(10)
    
    mostPlayedItems.clear()
    mostPlayedItems.addAll(sorted)
}
fun AppViewModel.generateMoodPlaylist(mood: String, type: String) {
    scope.launch {
        val allItems = mediaLibrary.value
        val filtered = allItems.filter { item ->
            // Simple keyword-based mood mapping for now
            val matchesType = when (type) {
                "Music" -> item.type == MediaType.AUDIO
                "Videos" -> item.type == MediaType.VIDEO
                else -> true
            }
            
            val matchesMood = when (mood.uppercase()) {
                "ENERGETIC" -> item.title.contains("remix", true) || item.title.contains("dance", true) || item.title.contains("rock", true) || item.title.contains("fast", true) || item.bitrate > 256
                "CHILL" -> item.title.contains("lofi", true) || item.title.contains("acoustic", true) || item.title.contains("ambient", true) || item.title.contains("relax", true) || item.bitrate < 192
                "FOCUSED" -> item.title.contains("piano", true) || item.title.contains("instrumental", true) || item.title.contains("study", true) || item.title.contains("coding", true)
                "MELANCHOLIC" -> item.title.contains("sad", true) || item.title.contains("blues", true) || item.title.contains("slow", true) || item.title.contains("emotional", true)
                else -> true
            }
            
            matchesType && matchesMood
        }
        
        if (filtered.isNotEmpty()) {
            val playlistName = "MOOD: ${mood.uppercase()}"
            val sortedList = filtered.shuffled().take(20)
            
            // 1. Update Playback Queue
            playbackQueue.clear()
            playbackQueue.addAll(sortedList)
            playMediaItem(playbackQueue.first())
            
            // 2. Create Persistent Playlist
            customPlaylists[playlistName] = mutableStateListOf(*sortedList.toTypedArray())
            
            showToast("Neural sync established for $mood mood")
            persistSession()
        } else {
            showToast("No matching assets found for $mood")
        }
    }
}
