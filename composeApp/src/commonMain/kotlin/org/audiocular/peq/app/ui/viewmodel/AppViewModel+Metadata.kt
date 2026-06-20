package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.audiocular.peq.app.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull

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


private var lyricsFetchJob: Job? = null

fun AppViewModel.fetchLyrics(
    autoOpenEditor: Boolean = true
) {

    val item =
        currentMediaItem.value ?: return

    /*
     * SKIP VIDEO FILES
     */
    if (item.type == MediaType.VIDEO) {

        println(
            "pEQControl: [LYRICS] Video detected -> skipping"
        )

        return
    }

    /*
     * UNIQUE REQUEST KEY
     */
    val requestKey =
        "${item.artist}_${item.title}"

    /*
     * PREVENT DUPLICATE FETCHES
     */
    if (
        lastLyricsRequest == requestKey &&
        syncedLyrics.isNotEmpty()
    ) {

        println(
            "pEQControl: [LYRICS] Already fetched"
        )

        return
    }

    /*
     * CANCEL OLD REQUEST
     */
    lyricsFetchJob?.cancel()

    lastLyricsRequest = requestKey

    scope.launch {
        /*
         * SKIP IF LYRICS ALREADY EXIST
         */
        if (!item.lyrics.isNullOrBlank()) {
            println("pEQControl: [LYRICS] Lyrics already present -> skipping fetch")
            return@launch
        }

        val jobId = "LYRICS_${Clock.System.now().toEpochMilliseconds()}"
        enqueueJob(SplitJob(
            id = jobId,
            name = "Neural Lyrics Sync",
            progress = 0.3f,
            status = ""
        ))

        try {
            // Add 10s timeout for network fetch
            val results = withTimeoutOrNull(10000L) {
                lyricsRepository.fetchLyrics(
                    artist = item.artist,
                    title = item.title,
                    durationMs = if (item.durationMs > 0) item.durationMs else null
                )
            }

            if (!results.isNullOrEmpty()) {

                syncedLyrics.clear()

                syncedLyrics.addAll(results)

                showToast(
                    "Neural sync established"
                )

                currentMediaItem.value
                    ?.let { current ->

                        val updated =
                            current.copy(

                                lyrics =
                                    results.joinToString("\n") { line ->

                                        "[${formatLrcTime(line.timestampMs)}] ${line.text}"
                                    }
                            )

                        currentMediaItem.value =
                            updated

                        _mediaLibrary.value =
                            _mediaLibrary.value.map {

                                if (it.path == updated.path) {
                                    updated
                                } else {
                                    it
                                }
                            }
                    }

                updateJob(jobId, 1.0f, "SYNC COMPLETE")

            } else {

                updateJob(jobId, 1.0f, "NO MATCH FOUND")
            }

        } catch (e: CancellationException) {
            updateJob(jobId, 1.0f, "REQUEST CANCELLED")
        } catch (e: Exception) {
            println("pEQControl: [LYRICS] Fetch failed -> ${e.message}")
            updateJob(jobId, 1.0f, "FETCH FAILED")
        } finally {
            if (lastLyricsRequest == requestKey && syncedLyrics.isEmpty()) {
                // If it timed out or failed, mark as failed to prevent immediate re-fetch
                // But allow re-fetch on next track play
            }
            delay(1200)
            removeJob(jobId)

            if (
                autoOpenEditor &&
                syncedLyrics.isNotEmpty()
            ) {

                isLyricsVisible.value = true
            }
        }
    }
}

fun AppViewModel.fetchSubtitles(autoOpenEditor: Boolean = true) {
    val item = currentMediaItem.value ?: return
    if (item.type != MediaType.VIDEO) return
    
    if (osApiKey.value.isBlank()) {
        showToast("OpenSubtitles API Key required in Settings")
        return
    }
    
    isFetchingSubtitles.value = true
    scope.launch {
        val jobId = "SUBS_${Clock.System.now().toEpochMilliseconds()}"
        enqueueJob(SplitJob(
            id = jobId,
            name = "Subtitle Retrieval",
            progress = 0.2f,
            status = "SEARCHING OPENSUBS"
        ))

        try {
            val matches = withTimeoutOrNull(15000L) {
                subtitleRepository.searchSubtitles(
                    title = item.title,
                    artist = item.artist
                )
            }
            
            if (!matches.isNullOrEmpty()) {
                val best = matches.minByOrNull { it.score }
                best?.let { match ->
                    updateJob(jobId, 0.6f, "DOWNLOADING")
                    val link = subtitleRepository.getDownloadLink(match.fileId)
                    if (link != null) {
                        showToast("Subtitles synchronized: ${match.fileName}")
                    }
                }
            } else {
                showToast("No subtitles found in archive")
                updateJob(jobId, 1.0f, "NOT FOUND")
            }
        } catch (e: Exception) {
            println("pEQControl: [SUBTITLES] Repository Exception: ${e.message}")
            updateJob(jobId, 1.0f, "ERROR")
        } finally {
            isFetchingSubtitles.value = false
            delay(1000)
            removeJob(jobId)
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
    // More flexible regex: [mm:ss.xx] or [mm:ss:xx] or [mm:ss]
    val regex = Regex("\\[(\\d+):(\\d+)([\\.:](\\d+))?\\](.*)")
    content.lines().forEach { line ->
        regex.find(line)?.let { match ->
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val msPart = match.groupValues[4]
            val ms = if (msPart.isNotEmpty()) {
                if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
            } else 0L
            val text = match.groupValues[5].trim()
            lines.add(SyncedLine(timestampMs = (min * 60 + sec) * 1000 + ms, text = text))
        }
    }
    
    // If empty, try SRT format
    if (lines.isEmpty() && content.contains("-->")) {
        return parseSrtInternal(content)
    }
    
    return lines.sortedBy { it.timestampMs }
}

fun AppViewModel.parseSrtInternal(content: String): List<SyncedLine> {
    val lines = mutableListOf<SyncedLine>()
    val blocks = content.split(Regex("\\n\\s*\\n"))
    val timestampRegex = Regex("(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2},\\d{3})")
    
    blocks.forEach { block ->
        val parts = block.lines().filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val tsLine = parts.find { it.contains("-->") } ?: ""
            timestampRegex.find(tsLine)?.let { match ->
                val startStr = match.groupValues[1]
                val startTime = parseSrtTimeToMs(startStr)
                val text = parts.drop(parts.indexOf(tsLine) + 1).joinToString(" ")
                lines.add(SyncedLine(timestampMs = startTime, text = text))
            }
        }
    }
    return lines
}

private fun parseSrtTimeToMs(time: String): Long {
    val parts = time.replace(',', ':').split(':').map { it.toLongOrNull() ?: 0L }
    return if (parts.size == 4) {
        (parts[0] * 3600000) + (parts[1] * 60000) + (parts[2] * 1000) + parts[3]
    } else 0L
}
private var lastLyricsRequest: String? = null

fun AppViewModel.fetchLyricsNative() {

    val item =
        currentMediaItem.value ?: return

    /*
     * SKIP VIDEO FILES
     */
    if (item.type == MediaType.VIDEO) {

        println(
            "pEQControl: [LYRICS] Video detected -> skipping"
        )

        return
    }

    val requestKey =
        "${item.artist}_${item.title}"

    /*
     * PREVENT DUPLICATE REQUESTS
     */
    if (lastLyricsRequest == requestKey) {

        println(
            "pEQControl: [LYRICS] Duplicate fetch prevented"
        )

        return
    }

    lastLyricsRequest = requestKey

    fetchLyrics()
}

fun AppViewModel.importChapterFile(path: String) {
    scope.launch {
        try {
            val content = readFileContent(path) ?: return@launch
            val parsedChapters = parseChaptersInternal(content)
            if (parsedChapters.isNotEmpty()) {
                currentChapters.clear()
                currentChapters.addAll(parsedChapters)
                showToast("Imported ${parsedChapters.size} chapters")
            } else {
                showToast("Failed to parse chapter metadata")
            }
        } catch (e: Exception) {
            showToast("Chapter import failed")
        }
    }
}

fun AppViewModel.importSubtitleFile(path: String) {
    audioEngine.addExternalSubtitle(path)
    showToast("External subtitle synchronized")
}
fun AppViewModel.requestChaptersPicker() {

    onPickChaptersRequested?.invoke()

}
fun AppViewModel.parseChaptersInternal(content: String): List<MediaChapter> {
    val chapters = mutableListOf<MediaChapter>()

    // More robust regex for various timestamp formats: [00:00:00.000], 00:00:00, 00:00, [00:00]
    val timeRegex =
        Regex("(\\[?(\\d{1,2}:)?\\d{1,2}:\\d{1,2}([\\.,:]\\d{1,3})?\\]?|\\d{1,2}:\\d{1,2})")

    content.lines().forEach { line ->
        val match = timeRegex.find(line)

        if (match != null) {
            val timeStr = match.value
                .replace("[", "")
                .replace("]", "")

            val title = line
                .replace(match.value, "")
                .trim()
                .trim('-')
                .trim()

            val ms = parseTimeToMs(timeStr)

            chapters.add(
                MediaChapter(
                    title = if (title.isBlank()) {
                        "Chapter ${chapters.size + 1}"
                    } else {
                        title
                    },
                    startMs = ms,
                    endMs = Long.MAX_VALUE
                )
            )
        }
    }

    val sorted = chapters.sortedBy { it.startMs }

    return sorted.mapIndexed { index, chapter ->
        val nextStart =
            sorted.getOrNull(index + 1)?.startMs ?: Long.MAX_VALUE

        chapter.copy(
            endMs = nextStart - 1
        )
    }
}

private fun parseTimeToMs(time: String): Long {
    val parts = time.split(':', '.').map { it.toLongOrNull() ?: 0L }
    return when (parts.size) {
        4 -> (parts[0] * 3600000) + (parts[1] * 60000) + (parts[2] * 1000) + parts[3]
        3 -> (parts[0] * 60000) + (parts[1] * 1000) + parts[2]
        2 -> (parts[0] * 60000) + (parts[1] * 1000)
        else -> 0L
    }
}

fun AppViewModel.readFileContent(path: String): String? {
    return scanner?.readFileContent(path)
}
