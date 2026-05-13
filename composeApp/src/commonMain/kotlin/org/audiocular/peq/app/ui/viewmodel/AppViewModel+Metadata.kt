package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.audiocular.peq.app.data.*

/**
 * Extension for AppViewModel to handle Metadata, Lyrics, and Subtitles logic.
 * This helps reduce the main AppViewModel file size for industrial maintenance.
 */

fun AppViewModel.openWebSearch(query: String, title: String = "SEARCH") {
    browserUrl.value = "https://www.google.com/search?q=${query.replace(" ", "+")}"
    browserTitle.value = title
    showBrowser.value = true
}

fun AppViewModel.searchLyricsWeb() {
    val item = currentMediaItem.value ?: return
    val query = "${item.artist ?: ""} ${item.title} lyrics genius"
    browserUrl.value = "https://www.google.com/search?q=${query.replace(" ", "+")}"
    browserTitle.value = "LYRICS SEARCH"
    showBrowser.value = true
}

fun AppViewModel.handleCapturedStream(url: String) {
    if (showBrowser.value) {
        showBrowser.value = false
        val item = org.audiocular.peq.app.data.pEQMediaItem(
            id = "stream_${url.hashCode()}",
            title = "Neural Web Stream",
            subtitle = "Captured from ${browserUrl.value.substringAfter("://").substringBefore("/")}",
            path = url,
            type = if (url.contains("video") || url.contains("mp4") || url.contains("m3u8") || url.contains("videoplayback")) org.audiocular.peq.app.data.MediaType.VIDEO else org.audiocular.peq.app.data.MediaType.AUDIO,
            format = "Network Stream",
            sizeBytes = 0,
            durationMs = 0
        )
        playMediaItem(item)
        
        // Force navigation to the native unified player
        currentTab.value = org.audiocular.peq.app.Tab.Player
        if (item.type == org.audiocular.peq.app.data.MediaType.VIDEO) {
            isVideoFullScreen.value = true
        }
    }
}

fun AppViewModel.fetchLyrics(autoOpenEditor: Boolean = true) {
    val item = currentMediaItem.value ?: return
    
    viewModelScope.launch {
        activeJob.value = SplitJob(
            id = "LYRICS_${Clock.System.now().toEpochMilliseconds()}",
            name = "Neural Lyrics Sync",
            progress = 0.3f,
            status = "SEARCHING REPOSITORY"
        )
        
        try {
            val results = lyricsRepository.fetchLyrics(
                artist = item.artist,
                title = item.title,
                durationMs = if (item.durationMs > 0) item.durationMs else null
            )
            
            if (!results.isNullOrEmpty()) {
                syncedLyrics.clear()
                syncedLyrics.addAll(results)
                showToast("Neural sync established")
                
                currentMediaItem.value?.let { current ->
                    val updated = current.copy(lyrics = results.joinToString("\n") { line -> "[${formatLrcTime(line.timestampMs)}] ${line.text}" })
                    currentMediaItem.value = updated
                    _mediaLibrary.value = _mediaLibrary.value.map { if (it.path == updated.path) updated else it }
                }
                
                activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "SYNC COMPLETE")
            } else {
                activeJob.value = activeJob.value?.copy(status = "NO MATCH FOUND", progress = 1.0f)
            }
        } catch (e: Exception) {
            activeJob.value = activeJob.value?.copy(status = "FETCH FAILED", progress = 1.0f)
        } finally {
            delay(1200)
            activeJob.value = null
            if (autoOpenEditor && syncedLyrics.isNotEmpty()) {
                isLyricsVisible.value = true
            }
        }
    }
}

fun AppViewModel.fetchSubtitles(autoOpenEditor: Boolean = true) {
    val item = currentMediaItem.value ?: return
    if (item.type != MediaType.VIDEO) return
    
    viewModelScope.launch {
        activeJob.value = SplitJob(
            id = "SUBS_${Clock.System.now().toEpochMilliseconds()}",
            name = "Subtitle Retrieval",
            progress = 0.2f,
            status = "SEARCHING OPENSUBS"
        )
        
        try {
            val matches = subtitleRepository.searchSubtitles(
                title = item.title,
                artist = item.artist,
                durationMs = item.durationMs
            )
            
            if (matches.isNotEmpty()) {
                val best = matches.minByOrNull { it.score }
                best?.let { match ->
                    activeJob.value = activeJob.value?.copy(status = "DOWNLOADING", progress = 0.6f)
                    val link = subtitleRepository.getDownloadLink(match.fileId)
                    if (link != null) {
                        showToast("Subtitles synchronized: ${match.fileName}")
                    }
                }
            } else {
                showToast("No subtitles found in archive")
            }
        } catch (e: Exception) {
            println("pEQControl: [SUBTITLES] Repository Exception: ${e.message}")
        } finally {
            activeJob.value = null
        }
    }
}

private fun formatLrcTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val millis = (ms % 1000) / 10
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${millis.toString().padStart(2, '0')}"
}
fun AppViewModel.parseLrcInternal(content: String): List<SyncedLine> {
    val lines = mutableListOf<SyncedLine>()
    val regex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
    content.lines().forEach { line ->
        regex.find(line)?.let { match ->
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong() * 10
            val text = match.groupValues[4].trim()
            lines.add(SyncedLine(timestampMs = (min * 60 + sec) * 1000 + ms, text = text))
        }
    }
    return lines
}
fun AppViewModel.fetchLyricsNative() {
    fetchLyrics()
}

fun AppViewModel.enqueueSplitJob(item: pEQMediaItem) {
    currentMediaItem.value = item
    startStemSplit()
}
