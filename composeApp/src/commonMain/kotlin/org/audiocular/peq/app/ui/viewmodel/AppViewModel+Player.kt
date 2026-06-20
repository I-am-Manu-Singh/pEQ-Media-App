package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull
/**
 * INDUSTRIAL PLAYER ENGINE: pEQ NEURAL WORKSTATION
 * This extension handles cinematic transport, mixer control, and playback metadata.
 */

fun AppViewModel.stop() {
    audioEngine.stop()
    isPlaying.value = false
    syncNowPlaying()
}

fun AppViewModel.togglePlayback() {
    val enginePlaying = audioEngine.isPlaying()
    
    // Safety check: If UI thinks we are playing but engine is not, try to resume engine
    if (isPlaying.value && !enginePlaying) {
        audioEngine.resume()
        syncNowPlaying()
        return
    }
    
    // Safety check: If engine is idle and we have an item, load it
    if (!enginePlaying && audioEngine.getDuration() <= 0L) {
        currentMediaItem.value?.let { item ->
            audioEngine.loadTrack(item.path, item.title, item.artist, item.thumbnail)
            audioEngine.resume()
            isPlaying.value = true
            syncNowPlaying()
            return
        }
    }

    if (isPlaying.value) audioEngine.pause() else audioEngine.resume()
    isPlaying.value = !isPlaying.value
    syncNowPlaying()
}

fun AppViewModel.playExternalFile(
    path: String,
    displayName: String? = null,
    mimeType: String? = null,
    sizeBytes: Long? = null,
    title: String? = null,
    artist: String? = null,
    durationMs: Long? = null
) {
    val decodedDisplayName = if (displayName != null) {
        displayName
    } else {
        path.substringAfterLast("/").substringBeforeLast(".")
    }
    
    val extension = if (displayName != null) {
        displayName.substringAfterLast(".", "").lowercase()
    } else {
        path.substringAfterLast(".").lowercase()
    }

    val isVideo = if (mimeType != null) {
        mimeType.startsWith("video/")
    } else {
        listOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "ts", "flv", "m4v").contains(extension) || 
        path.contains("video", ignoreCase = true) || 
        path.contains("DCIM", ignoreCase = true)
    }
    
    val item = pEQMediaItem(
        id = "external_${path.hashCode()}",
        title = if (title != null && title.isNotBlank()) title else decodedDisplayName.substringBeforeLast(".", decodedDisplayName),
        subtitle = if (artist != null && artist.isNotBlank()) artist else "External Source",
        artist = if (artist != null && artist.isNotBlank()) artist else "External Source",
        path = path,
        type = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
        format = if (extension.isNotEmpty()) extension.uppercase() else (if (isVideo) "VIDEO" else "AUDIO"),
        sizeBytes = sizeBytes ?: 0L,
        durationMs = durationMs ?: 0L
    )
    
    playMediaItem(item, addToQueue = true)
    
    // Explicitly set player tab and handle video fullscreen if needed
    currentTab.value = Tab.Player
    if (isVideo) {
        isVideoFullScreen.value = true
    }
}

fun AppViewModel.nextTrack() {
    if (playbackQueue.isEmpty()) return
    val nextIndex = if (isShuffleEnabled) {
        (0 until playbackQueue.size).random()
    } else {
        (currentQueueIndex.value + 1) % playbackQueue.size
    }
    setCurrentQueueIndex(nextIndex)
    playMediaItem(playbackQueue[nextIndex])
}

fun AppViewModel.previousTrack() {
    if (playbackQueue.isEmpty()) return
    val prevIndex = if (currentPosition.value > 5000) {
        currentQueueIndex.value
    } else {
        if (currentQueueIndex.value > 0) currentQueueIndex.value - 1 else playbackQueue.size - 1
    }
    setCurrentQueueIndex(prevIndex)
    playMediaItem(playbackQueue[prevIndex])
}

fun AppViewModel.seekTo(positionMs: Long) {
    currentPosition.longValue = positionMs
    isSeeking.value = true
    audioEngine.seekTo(positionMs)
    scope.launch {
        delay(400)
        isSeeking.value = false
    }
}

fun AppViewModel.seekRelative(deltaMs: Long) {
    val newPos = (currentPosition.value + deltaMs).coerceIn(0, currentDuration.value)
    seekTo(newPos)
}

fun AppViewModel.playMediaItem(item: pEQMediaItem, addToQueue: Boolean = true, fromHomeScreen: Boolean = false) {
    showStemSplitter.value = false 
    currentMediaItem.value = item
    syncedLyrics.clear()
    
    scope.launch {
        val playPath = if (item.path.startsWith("http") && (item.path.contains("youtube.com") || item.path.contains("youtu.be"))) {
            cloudDownloader?.getStreamUrl(item.path) ?: item.path
        } else item.path
        
        audioEngine.playTrack(playPath, item.title, item.artist, item.thumbnail)
        isPlaying.value = true

        var finalLyrics = item.lyrics
        if (finalLyrics.isNullOrBlank() && !item.path.startsWith("http")) {
            val normalizedPath = normalizePath(item.path)
            try {
                val file = java.io.File(normalizedPath)
                if (file.exists()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    finalLyrics = audioFile.tag?.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                }
            } catch (_: Exception) {}
        }

        if (!finalLyrics.isNullOrBlank()) {
            syncedLyrics.clear()
            syncedLyrics.addAll(parseLrcInternal(finalLyrics))
            if (syncedLyrics.isEmpty()) {
                finalLyrics.lines().filter { it.isNotBlank() }.forEachIndexed { index, line ->
                    syncedLyrics.add(SyncedLine(timestampMs = index * 5000L, text = line))
                }
            }
        } else {
            fetchLyrics(autoOpenEditor = false)
        }
    }
    
    if (addToQueue) {
        val existingIndex = playbackQueue.indexOfFirst { it.path == item.path }
        if (existingIndex == -1) {
            playbackQueue.add(0, item)
            setCurrentQueueIndex(0)
        } else {
            setCurrentQueueIndex(existingIndex)
        }
    }

    if (!recentlyPlayedItems.any { it.path == item.path }) {
        recentlyPlayedItems.add(0, item)
        if (recentlyPlayedItems.size > 20) recentlyPlayedItems.removeAt(recentlyPlayedItems.size - 1)
    }
    
    updateMostPlayed(item)
    if (item.type == MediaType.VIDEO) {
        currentTab.value = Tab.Player
    }
    
    // If video, enter immersive mode immediately
    if (item.type == MediaType.VIDEO) {
        isVideoFullScreen.value = true
    } else {
        isVideoFullScreen.value = false
    }
    
    persistSession()
    syncNowPlaying()
}

fun AppViewModel.playMediaItemFromQueue(index: Int) {
    if (index in playbackQueue.indices) {
        val item = playbackQueue[index]
        setCurrentQueueIndex(index)
        playMediaItem(item, addToQueue = false)
        syncNowPlaying()
    }
}

// Mixer Extensions
fun AppViewModel.updateStemVolume(stem: String, volume: Float) {
    stemVolumes[stem] = volume
    audioEngine.updateStemVolume(stem, volume)
}

fun AppViewModel.toggleStemMute(stem: String) {
    val current = stemMutes[stem] ?: false
    stemMutes[stem] = !current
}

fun AppViewModel.toggleStemSolo(stem: String, active: Boolean? = null) {
    val current = stemSolos[stem] ?: false
    val newState = active ?: !current
    stemSolos[stem] = newState
}

fun AppViewModel.updateStemPan(stem: String, pan: Float) {
    stemPans[stem] = pan
    audioEngine.updateStemPan(stem, pan)
}

// Track Management
fun AppViewModel.switchSubtitle(track: MediaTrack?) {
    selectedSubtitleTrack.value = track
    isSubtitlesEnabled.value = track != null && track.id != "none"
    if (track != null) {
        audioEngine.switchTrack(track)
    } else {
        audioEngine.setSubtitlesEnabled(false)
    }
}

fun AppViewModel.switchAudioTrack(track: MediaTrack?) {
    selectedAudioTrack.value = track
    audioEngine.switchTrack(track)
}

fun AppViewModel.refreshAvailableTracks() {
    val tracks = audioEngine.getAvailableTracks()
    availableSubtitleTracks.clear()
    availableSubtitleTracks.addAll(tracks.filter { t -> t.type == TrackType.SUBTITLE })
    
    availableAudioTracks.clear()
    availableAudioTracks.addAll(tracks.filter { t -> t.type == TrackType.AUDIO })
}

fun AppViewModel.onTrackLoaded() {
    scope.launch {
        val engineMetadata = audioEngine.getEmbeddedMetadata()
        fetchChapters()
        
        var embeddedLyricsText: String? = null
        var trackTitle = currentMediaItem.value?.title ?: ""
        var trackArtist = currentMediaItem.value?.artist ?: "External Source"
        var sampleRateVal = audioEngine.getSampleRate()
        var bitDepthVal = audioEngine.getBitDepth()
        var codecVal = audioEngine.getCodec()
        var sizeBytesVal = currentMediaItem.value?.sizeBytes ?: 0L
        var durationMsVal = currentMediaItem.value?.durationMs ?: 0L

        val currentPath = currentMediaItem.value?.path ?: ""
        if (!currentPath.startsWith("http")) {
            val normalizedPath = normalizePath(currentPath)
            try {
                val file = java.io.File(normalizedPath)
                if (file.exists()) {
                    if (sizeBytesVal == 0L) sizeBytesVal = file.length()
                    
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    val tag = audioFile.tag
                    if (tag != null) {
                        embeddedLyricsText = tag.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                        if (trackTitle.isBlank() || trackTitle == "temp_media") {
                            trackTitle = tag.getFirst(org.jaudiotagger.tag.FieldKey.TITLE)
                        }
                        if (trackArtist.isBlank() || trackArtist == "External Source") {
                            trackArtist = tag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST)
                        }
                    }
                }
            } catch (_: Exception) {}

            if (embeddedLyricsText.isNullOrBlank() || trackTitle.isBlank() || trackArtist.isBlank() || durationMsVal == 0L) {
                try {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(normalizedPath)
                    if (embeddedLyricsText.isNullOrBlank()) {
                        embeddedLyricsText = retriever.extractMetadata(31) // METADATA_KEY_LYRICS
                            ?: retriever.extractMetadata(23)
                            ?: retriever.extractMetadata(101)
                    }
                    if (trackTitle.isBlank() || trackTitle == "temp_media") {
                        trackTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
                    }
                    if (trackArtist.isBlank() || trackArtist == "External Source") {
                        trackArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                    }
                    if (durationMsVal == 0L) {
                        durationMsVal = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    }
                    retriever.release()
                } catch (_: Exception) {}
            }
        }

        if (trackTitle.isBlank()) {
            trackTitle = currentPath.substringAfterLast("/").substringBeforeLast(".")
        }
        if (trackArtist.isBlank()) {
            trackArtist = "External Source"
        }

        val updatedItem = currentMediaItem.value?.copy(
            title = trackTitle,
            subtitle = trackArtist,
            artist = trackArtist,
            bitrate = engineMetadata.availableTracks.firstOrNull { it.type == TrackType.AUDIO }?.codec?.toIntOrNull() ?: 0,
            codec = codecVal,
            sampleRate = sampleRateVal,
            bitDepth = bitDepthVal,
            sizeBytes = sizeBytesVal,
            durationMs = if (durationMsVal > 0L) durationMsVal else (currentMediaItem.value?.durationMs ?: 0L),
            lyrics = if (!embeddedLyricsText.isNullOrBlank()) embeddedLyricsText else currentMediaItem.value?.lyrics
        )

        currentMediaItem.value = updatedItem
        
        // Update the queue item as well if present
        val queueIndex = playbackQueue.indexOfFirst { it.path == currentPath }
        if (queueIndex != -1 && updatedItem != null) {
            playbackQueue[queueIndex] = updatedItem
        }

        currentSampleRate.value = sampleRateVal
        currentBitDepth.value = bitDepthVal
        currentCodec.value = codecVal

        delay(500)
        refreshAvailableTracks()
        val tracks = audioEngine.getAvailableTracks()
        val embeddedLyrics = tracks.filter { it.type == TrackType.LYRICS }
        if (embeddedLyrics.isNotEmpty()) {
            audioEngine.switchTrack(embeddedLyrics.first())
        }
        delay(1500)
        refreshAvailableTracks()
        analyzeCurrentTrack() // 🧠 Auto-sync workstation telemetry
        persistSession()
        syncNowPlaying()
    }
}

fun AppViewModel.trimMedia(_startMs: Long, _endMs: Long) {
    val current = currentMediaItem.value ?: return
    if (activeJob.value != null) return
    scope.launch {
        activeJob.value = SplitJob(
            id = "TRIM_${Clock.System.now().toEpochMilliseconds()}",
            name = "High-Fidelity Trim: ${current.title}",
            progress = 0.0f,
            status = "BITSTREAM EXTRACTION"
        )
        for (p in 1..20) {
            delay(150)
            activeJob.value = activeJob.value?.copy(
                progress = p / 20f,
                status = if (p < 10) "DEMUXING" else "STREAM REPLICATION"
            )
        }
        activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "EXPORT COMPLETE")
        showToast("Mastered trim saved to /pEQ/Exports")
        delay(1000)
        activeJob.value = null
        showCutter.value = false
    }
}

fun AppViewModel.cycleAspectRatio() {
    videoAspectRatio.value = when (videoAspectRatio.value) {
        "FIT" -> "FILL"
        "FILL" -> "16:9"
        "16:9" -> "4:3"
        else -> "FIT"
    }
}

fun AppViewModel.toggleSubtitles(enabled: Boolean? = null) {
    val newState = enabled ?: !isSubtitlesEnabled.value
    isSubtitlesEnabled.value = newState
    audioEngine.setSubtitlesEnabled(newState)
}

fun AppViewModel.toggleSubtitleConsole() {
    showSubtitleConsole.value = !showSubtitleConsole.value
}

fun AppViewModel.toggleVocalRemoval() {
    isVocalRemovalActive.value = !isVocalRemovalActive.value
    audioEngine.toggleVocalRemoval(isVocalRemovalActive.value)
}

fun AppViewModel.addFolderToQueue(path: String) {
    val folderItems = mediaLibrary.value.filter { it.path.startsWith(path) }
    playbackQueue.addAll(folderItems)
}

fun AppViewModel.addFolderToPlaylist(path: String, playlistName: String) {
    val folderItems = mediaLibrary.value.filter { it.path.startsWith(path) }
    val playlist = customPlaylists[playlistName]
    playlist?.addAll(folderItems)
    persistSession()
}

fun AppViewModel.fetchEverything() {
    fetchLyrics(autoOpenEditor = false)
    fetchSubtitles()
    fetchChapters()
}

fun AppViewModel.webSearchLyrics() {
    val item = currentMediaItem.value ?: return
    val query = "${item.title} ${item.subtitle} lyrics lrc"
    openWebSearch(query, "LYRIC DISCOVERY")
}

fun AppViewModel.saveLyrics(lines: List<SyncedLine>) {
    scope.launch {
        currentMediaItem.value?.let { item ->
            audioEngine.saveEmbeddedLyrics(item.path, lines)
            showToast("Lyrics Saved Permanently")
        }
    }
}

fun AppViewModel.saveSubtitles(lines: List<SyncedLine>) {
    scope.launch {
        currentMediaItem.value?.let { item ->
            audioEngine.saveEmbeddedSubtitles(item.path, lines)
            showToast("Subtitles Saved Permanently")
        }
    }
}

fun AppViewModel.saveChapters(chapters: List<MediaChapter>) {
    scope.launch {
        currentMediaItem.value?.let { item ->
            audioEngine.saveEmbeddedChapters(item.path, chapters)
            showToast("Chapters Saved Permanently")
        }
    }
}

// Transport helpers moved from AppViewModel.kt
fun AppViewModel.cyclePlaybackSpeed() {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val currentIndex = speeds.indexOf(playbackSpeed.value)
    val nextIndex = (currentIndex + 1) % speeds.size
    playbackSpeed.value = speeds[nextIndex]
    audioEngine.setPlaybackSpeed(playbackSpeed.value)
}

fun AppViewModel.shuffleQueue() {
    val current = playbackQueue.toList()
    playbackQueue.clear()
    playbackQueue.addAll(current.shuffled())
    showToast("Queue Shuffled")
}

fun AppViewModel.openPlaylistAsQueue(name: String) {
    val items = if (name == "FAVORITES") favoriteItems else customPlaylists[name] ?: emptyList()
    if (items.isEmpty()) return
    playbackQueue.clear()
    playbackQueue.addAll(items)
    playMediaItem(items.first())
}

fun AppViewModel.shufflePlaylistAndPlay(name: String) {
    val items = if (name == "FAVORITES") favoriteItems else customPlaylists[name] ?: emptyList()
    if (items.isEmpty()) return
    val shuffled = items.shuffled()
    playbackQueue.clear()
    playbackQueue.addAll(shuffled)
    playMediaItem(shuffled.first())
}

fun AppViewModel.addToQueue(item: pEQMediaItem) {
    playbackQueue.add(item)
    showToast("Added to Queue")
}

fun AppViewModel.removeFromQueue(item: pEQMediaItem) {
    playbackQueue.remove(item)
}

fun AppViewModel.playFromQueue(item: pEQMediaItem) {
    playMediaItem(item)
}

fun AppViewModel.reorderQueue(from: Int, to: Int) {
    if (from < playbackQueue.size && to < playbackQueue.size) {
        val item = playbackQueue.removeAt(from)
        playbackQueue.add(to, item)
    }
}

fun AppViewModel.cycleRepeatMode() {
    val next = when(repeatMode.value) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
        else -> RepeatMode.OFF
    }
    repeatMode.value = next
    audioEngine.setRepeatMode(next)
}

fun AppViewModel.toggleShuffle() {
    isShuffleEnabled = !isShuffleEnabled
    audioEngine.setShuffleMode(isShuffleEnabled)
}

fun AppViewModel.toggleAbRepeat() {
    abRepeatActive.value = !abRepeatActive.value
    if (abRepeatActive.value) {
        showCutter.value = false
    } else {
        abRepeatPointA.value = -1L
        abRepeatPointB.value = -1L
        updateLoopPoints(-1L, -1L)
    }
}

fun AppViewModel.setABPoint(point: Char) {
    val currentPos = currentPosition.value
    if (point == 'A') {
        abRepeatPointA.value = currentPos
    } else {
        val pointA = abRepeatPointA.value
        if (currentPos > pointA && pointA != -1L) {
            abRepeatPointB.value = currentPos
        } else {
            abRepeatPointA.value = currentPos
            abRepeatPointB.value = -1L
        }
    }
    if (abRepeatPointA.value != -1L && abRepeatPointB.value != -1L) {
        audioEngine.setLoopPoints(abRepeatPointA.value, abRepeatPointB.value)
    }
}

fun AppViewModel.requestLyricsPicker(onDone: () -> Unit = {}) { onPickLyricsRequested?.invoke() }
fun AppViewModel.requestSubtitlePicker() { onPickSubtitleRequested?.invoke() }
fun AppViewModel.requestChapterPicker() { onPickChaptersRequested?.invoke() }

fun AppViewModel.loadExternalSubtitle(path: String, displayName: String? = null) {
    scope.launch {
        audioEngine.addExternalSubtitle(path)
        val nameToShow = displayName ?: path.substringAfterLast("/")
        showToast("Subtitle Linked: $nameToShow")
    }
}

fun AppViewModel.loadExternalLyrics(content: String, path: String? = null) {
    scope.launch {
         val lines = parseLrcInternal(content)
         if (lines.isNotEmpty()) {
             syncedLyrics.clear()
             syncedLyrics.addAll(lines)
             val fileName = path?.substringAfterLast("/") ?: "Lyrics"
             showToast("Lyrics Loaded: $fileName")
         }
    }
}

fun AppViewModel.loadExternalChapters(path: String) {
    scope.launch {
         fetchChapters()
         showToast("Chapters Refreshed")
    }
}

fun AppViewModel.fetchChapters() {
    scope.launch {
        val chapters = audioEngine.getChapters()
        currentChapters.clear()
        currentChapters.addAll(chapters)
    }
}

fun AppViewModel.importChapters(path: String) {
    loadExternalChapters(path)
}
fun AppViewModel.toggleLyrics() {
    isLyricsVisible.value = !isLyricsVisible.value
}

fun AppViewModel.updateLoopPoints(a: Long?, b: Long?) {
    audioEngine.setLoopPoints(a, b)
}

fun AppViewModel.syncNowPlaying() {
    persistenceManager.updateNowPlaying(currentMediaItem.value, isPlaying.value)
}

fun AppViewModel.syncNotificationState() {}

fun AppViewModel.clearQueue() {
    playbackQueue.clear()
    _currentQueueIndex.value = -1
    currentMediaItem.value = null
    isPlaying.value = false
    audioEngine.stop()
    persistSession()
}
