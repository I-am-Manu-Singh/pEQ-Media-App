package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.ai.pEQStemSplitter
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull
/**
 * Extension for AppViewModel to handle AI and Neural Stem Splitter logic.
 */

fun AppViewModel.startStemSplit() {
    val item = currentMediaItem.value ?: return
    val splitter = stemSplitter ?: run {
        showToast("Neural Splitter not available on this platform")
        return
    }

    scope.launch {
        activeJob.value = SplitJob(
            id = "STEM_${Clock.System.now().toEpochMilliseconds()}",
            name = "Neural Stem Extraction",
            progress = 0f,
            status = "ANALYZING SPECTRUM"
        )

        try {
            val results = splitter.splitTrack(inputPath = item.path, outputDir = "/stems")
            // Update stem volumes map
            results.forEach { (type, path) ->
                println("pEQControl: [AI] Stem extracted: $type -> $path")
            }
            activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "EXTRACTION COMPLETE")
            showToast("Stems isolated successfully")
        } catch (e: Exception) {
            activeJob.value = activeJob.value?.copy(status = "FAILED: ${e.message}", progress = 1.0f)
        } finally {
            delay(2000)
            activeJob.value = null
        }
    }
}

fun AppViewModel.batchExportStems(items: List<pEQMediaItem>) {
    val splitter = stemSplitter ?: return
    scope.launch {
        activeJob.value = SplitJob(
            id = "BATCH_${Clock.System.now().toEpochMilliseconds()}",
            name = "Neural Batch Export",
            progress = 0f,
            status = "INITIALIZING BATCH"
        )
        try {
            splitter.splitBatch(items.map { it.path }, "/stems")
            activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "BATCH COMPLETE")
        } catch (e: Exception) {
             activeJob.value = activeJob.value?.copy(status = "BATCH FAILED", progress = 1.0f)
        } finally {
            delay(2000)
            activeJob.value = null
        }
    }
}


fun AppViewModel.generateMoodPlaylist(mood: String, type: String) {
    val filterType = when (type) {
        "Music" -> MediaType.AUDIO
        "Videos" -> MediaType.VIDEO
        else -> null
    }
    
    val items = _mediaLibrary.value.filter { 
        (filterType == null || it.type == filterType) && it.playCount > 0 
    }.shuffled().take(10)
    
    if (items.isNotEmpty()) {
        playbackQueue.clear()
        playbackQueue.addAll(items)
        playMediaItem(items.first())
        showToast("Generated $mood $type mix")
    } else {
        showToast("Not enough playback history for $mood mix")
    }
}
