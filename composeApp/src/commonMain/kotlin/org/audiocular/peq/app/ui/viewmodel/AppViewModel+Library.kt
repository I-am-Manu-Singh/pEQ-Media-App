package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import org.audiocular.peq.app.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
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
    viewModelScope.launch {
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

    viewModelScope.launch {
        scanner.scanProgress.collect { scanProgress.value = it }
    }
    viewModelScope.launch {
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
    if (!list.contains(item)) {
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
    viewModelScope.launch {
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
    // Placeholder for actual play count increment logic if handled elsewhere
}
