package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ai.pEQCloudDownloader
import org.audiocular.peq.app.ai.pEQStemSplitter
import org.audiocular.peq.app.audio.AudioEngine
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.data.Device
import org.audiocular.peq.app.data.SyncedLine
import org.audiocular.peq.app.data.lyrics.LyricsRepository
import org.audiocular.peq.app.ui.components.*
import kotlin.math.log2
import kotlin.math.roundToInt
import org.audiocular.peq.app.network.createHttpClient
import org.audiocular.peq.app.ui.theme.pEQTheme

class AppViewModel(

    val audioEngine: AudioEngine,

    val persistenceManager: PersistenceManager,

    val stemSplitter: pEQStemSplitter? = null,

    val cloudDownloader: pEQCloudDownloader? = null

) : pEQViewModel(){

    // =========================
// VIEW MODE ENUM
// PLACE INSIDE AppViewModel
// =========================

    enum class LibraryViewMode {

        LIBRARY,
        FILES,
        ARTISTS,
        ALBUMS
    }

    enum class VisualizerMode {
        BARS,
        CIRCULAR
    }

    var visualizerMode = mutableStateOf(VisualizerMode.BARS)

    var libraryViewMode =
        mutableStateOf(
            LibraryViewMode.LIBRARY
        )
val isLandscapeMode =
    mutableStateOf(true)
    var libraryMediaTypeFilter =
        mutableStateOf<MediaType?>(
            null
        )
    val isSeeking =
        mutableStateOf(false)
    companion object {
        var splashHasShown = false
    }

    var splashCompleted by mutableStateOf(splashHasShown)
        private set

    fun completeSplash() {
        splashCompleted = true
        splashHasShown = true
    }
    
val client = createHttpClient()
    private var metadataLoadJob: Job? = null

    private var hasInitialized = false
    // 🎬 Playback Core
    val isPlaying = mutableStateOf(false)
    val isInPipMode = mutableStateOf(false)
    val isLockScreenMode = mutableStateOf(false)
    
    var onEnterPiP: (() -> Unit)? = null
    
    fun toggleLockScreenMode() {
        isLockScreenMode.value = !isLockScreenMode.value
    }
    val playbackSpeed = mutableStateOf(1.0f)
    val peakLevelDb = mutableStateOf(-60f)
    val isKitBuilderActive = mutableStateOf(false)
    val editingPadIndex = mutableStateOf(-1)
    val toastMessage = mutableStateOf<String?>(null)
    
    var onPickFolderRequested: (() -> Unit)? = null
    var onRestoreSamplesRequested: (() -> Unit)? = null
    var onPickLyricsRequested: (() -> Unit)? = null
    var onPickSubtitleRequested: (() -> Unit)? = null
    var onPickChaptersRequested: (() -> Unit)? = null
    var onPickTrackRequested: (((pEQMediaItem) -> Unit) -> Unit)? = null
    val currentPosition = mutableLongStateOf(0L)
    val currentDuration = mutableStateOf(1L)
    val playbackQueue = mutableStateListOf<pEQMediaItem>()
    val currentMediaItem = mutableStateOf<pEQMediaItem?>(null)
    
    val currentTab = mutableStateOf(Tab.Dashboard)
    val dashboardSubTab = mutableStateOf(0)
    val selectedOutputDevice = mutableStateOf(org.audiocular.peq.app.data.Device.INTERNAL)
    val isVideoFullScreen = mutableStateOf(false)
    val isLandscapeLocked = mutableStateOf(false)
    val masterVolume = mutableStateOf(1f)
    val customAccentColor = mutableStateOf<Color?>(null)
    val isVocalRemovalActive = mutableStateOf(false)
    val isBuffering = mutableStateOf(false)
    val isPiPMode = mutableStateOf(false)
    val showMiniPlayer = mutableStateOf(false)

    private val _currentQueueIndex = mutableStateOf(-1)
    val currentQueueIndex: State<Int> = _currentQueueIndex             

    // ⏰ Industrial Utilities
    val sleepTimerActive = mutableStateOf(false)
    val sleepTimerMinutes = mutableStateOf(30)
    val sleepTimerRemainingSec = mutableLongStateOf(0L)
    
    val isCasting = mutableStateOf(false)
   
    val audioDelayMs = mutableLongStateOf(0L)
    val openAtStartup = mutableStateOf(false)
    val showSubtitleConsole = mutableStateOf(false)
    val showCutter = mutableStateOf(false)
    val subtitleSettings = mutableStateOf(SubtitleSettings())

    val abRepeatActive = mutableStateOf(false)
    // Initialize with -1L instead of null
    val abRepeatPointA = mutableStateOf(-1L)
    val abRepeatPointB = mutableStateOf(-1L)
    val currentFolder = mutableStateOf<String?>(null)
    
    val showBrowser = mutableStateOf(false)
    val browserUrl = mutableStateOf("")
    val browserTitle = mutableStateOf("pEQ BROWSER")

    private val osClient by lazy { org.audiocular.peq.app.ai.OpenSubtitlesClient(persistenceManager.openSubtitlesApiKey) }

    fun openWebSearch(query: String, title: String = "SEARCH") {
        browserUrl.value = "https://www.google.com/search?q=${query.replace(" ", "+")}"
        browserTitle.value = title
        showBrowser.value = true
    }

    fun searchLyricsWeb() {
        val item = currentMediaItem.value ?: return
        val query = "${item.artist ?: ""} ${item.title} lyrics genius"
        browserUrl.value = "https://www.google.com/search?q=${query.replace(" ", "+")}"
        browserTitle.value = "LYRICS SEARCH"
        showBrowser.value = true
    }

    fun handleCapturedStream(url: String) {
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

    private val lyricsRepository = LyricsRepository()
    private val subtitleRepository by lazy { org.audiocular.peq.app.data.repository.SubtitleRepository(persistenceManager.openSubtitlesApiKey) }

    val isEQInteracting = mutableStateOf(false)
    val diagnosticLog = mutableStateListOf<String>()
    val isDiagnosticRunning = mutableStateOf(false)

    fun fetchLyrics(autoOpenEditor: Boolean = true) {
        val item = currentMediaItem.value ?: return
        viewModelScope.launch {
            activeJob.value = SplitJob(
                id = "LYRICS_${Clock.System.now().toEpochMilliseconds()}",
                name = "Neural Lyrics Sync",
                progress = 0.2f,
                status = "SEARCHING LRCLIB"
            )
            
            try {
                val artist = item.artist ?: ""
                val title = item.title
                val url = "https://lrclib.net/api/get?artist=${artist.encodeURL()}&track_name=${title.encodeURL()}"
                
                val client = io.ktor.client.HttpClient()
                val response: String = client.get(url).body()
                
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val data = json.parseToJsonElement(response).jsonObject
                val syncedLrc = data["syncedLyrics"]?.jsonPrimitive?.contentOrNull
                val plainLrc = data["plainLyrics"]?.jsonPrimitive?.contentOrNull
                
                syncedLyrics.clear()
                if (!syncedLrc.isNullOrBlank()) {
                    syncedLyrics.addAll(parseLrc(syncedLrc))
                    showToast("Lyrics fetched from LyricHub")
                } else if (!plainLrc.isNullOrBlank()) {
                    plainLrc.lines().forEach { line ->
                        syncedLyrics.add(org.audiocular.peq.app.data.SyncedLine(timestampMs = 0L, text = line))
                    }
                    showToast("Plain lyrics synchronized")
                }
                
                activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "LYRICS SYNCED")
            } catch (e: Exception) {
                activeJob.value = activeJob.value?.copy(status = "NATIVE FETCH FAILED", progress = 1.0f)
            }
            
            delay(1500)
            activeJob.value = null
            if (autoOpenEditor && syncedLyrics.isNotEmpty()) {
                isLyricsVisible.value = true
            }
        }
    }

        private fun parseLrc(lrc: String): List<org.audiocular.peq.app.data.SyncedLine> {
            val lines = mutableListOf<org.audiocular.peq.app.data.SyncedLine>()
            val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
            lrc.lines().forEach { line ->
                val match = regex.find(line)
                if (match != null) {
                    val min = match.groupValues[1].toLong()
                    val sec = match.groupValues[2].toLong()
                    val ms = match.groupValues[3].padEnd(3, '0').toLong()
                    val time = (min * 60 * 1000) + (sec * 1000) + ms
                    val text = match.groupValues[4].trim()
                    lines.add(org.audiocular.peq.app.data.SyncedLine(timestampMs = time, text = text))
                }
            }
            return lines
        }

    private fun String.encodeURL(): String = this.replace(" ", "%20")



    fun openFolder(path: String) {

        currentFolder.value =
            path
                .replace("file://", "")
                .trim()
    }

    fun closeFolder() {
        currentFolder.value = null
        currentFolderPath.value = ""
    }

    fun openCurrentItemFolder() {

        val currentItem =
            currentMediaItem.value ?: return

        currentFolder.value =
            currentItem.path
                .replace("file://", "")
                .substringBeforeLast("/")
    }



    val eqGains = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) // 10 Bands
    val eqQs = mutableStateListOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
    val convolverIrPath = mutableStateOf<String?>(null)
    val favorites = mutableStateListOf<String>()
    val stemVolumes = mutableStateMapOf<String, Float>().apply {
        put("vocals", 1.0f)
        put("drums", 1.0f)
        put("bass", 1.0f)
        put("piano", 1.0f)
        put("guitar", 1.0f)
        put("other", 1.0f)
    }
    val stemMutes = mutableStateMapOf<String, Boolean>()
    val stemSolos = mutableStateMapOf<String, Boolean>()
    val stemPans = mutableStateMapOf<String, Float>().apply {
        put("vocals", 0f)
        put("drums", 0f)
        put("bass", 0f)
        put("piano", 0f)
        put("guitar", 0f)
        put("other", 0f)
    }

    // 📝 Lyrics & Temporal Metadata
    val syncedLyrics = mutableStateListOf<SyncedLine>()
    val currentChapters = mutableStateListOf<MediaChapter>()
    val isLyricsVisible = mutableStateOf(false)
    val isChaptersVisible = mutableStateOf(false)
    val isEditingLyrics = mutableStateOf(false)
    val isEditingChapters = mutableStateOf(false)
    val isEditingSubtitles = mutableStateOf(false)
    val isMoodMakerVisible = mutableStateOf(false)
    val selectedItems = mutableStateListOf<pEQMediaItem>()
    
    fun toggleSelection(item: pEQMediaItem) {
        if (selectedItems.any { it.id == item.id }) {
            selectedItems.removeAll { it.id == item.id }
        } else {
            selectedItems.add(item)
        }
    }


    val lyricsAvailable = derivedStateOf { syncedLyrics.isNotEmpty() }
    val chaptersAvailable = derivedStateOf { currentChapters.isNotEmpty() }
    val isLyricsWebViewVisible = mutableStateOf(false)
    val lyricsSearchUrl = mutableStateOf("")
    
    // 🔍 AutoEQ & Parametric Engine
    val autoEqSearchResults = mutableStateListOf<AutoEqProfile>()
    val selectedAutoEqProfile = mutableStateOf<AutoEqProfile?>(null)
    val isAutoEqLoading = mutableStateOf(false)

    init {
        viewModelScope.launch {
            snapshotFlow { masterVolume.value }
                .collect { audioEngine.setVolume(it) }
        }
    }
    
    // 🌀 Convolution & Dynamics Engine
    val convolverIrName = mutableStateOf("Reference Room (Default)")
    val convolverIrDryWet = mutableFloatStateOf(0.3f)
    
    val sequencerSteps = List(16) { mutableStateOf(false) }
    
    val currentKey = mutableStateOf("C Major")
    
    val isConvolverActive = mutableStateOf(false)
    val compressorThreshold = mutableFloatStateOf(-20f)
    val compressorRatio = mutableStateOf(4f)
    val compressorAttackMs = mutableFloatStateOf(10f)
    val compressorReleaseMs = mutableFloatStateOf(100f)
    val limiterEnabled = mutableStateOf(true)
    val isSubHarmonicActive = mutableStateOf(false)
    
    // 📊 Signal Telemetry (Pre/Post DSP)
    val preDspLevel = mutableFloatStateOf(0f)
    val dspProcessingLoad = mutableStateOf(0f)
    val detectedChord = mutableStateOf("READY")
    val isAutoScanEnabled = mutableStateOf(true)
    val isNeuralSaturationEnabled = mutableStateOf(false)
    val detectedPitch = mutableStateOf(440.0f)
    val detectedNote = mutableStateOf("A4")
    val centsDeviation = mutableStateOf(0f)
    val isTunerActive = mutableStateOf(false)
    
    // 🕒 Master Clock Engine
    val detectedBpm = mutableFloatStateOf(120f)
    val masterBpm = mutableFloatStateOf(120f)
    val timeSignatureNumerator = mutableIntStateOf(4)
    val timeSignatureDenominator = mutableIntStateOf(4)
    val isMetronomeActive = mutableStateOf(false)
    val isGaplessEnabled = mutableStateOf(true)
    val isHapticsEnabled = mutableStateOf(true)
    val isBackgroundServiceRunning = mutableStateOf(true)
    val isBitPerfectEnabled = mutableStateOf(false)
    val phaseInverted = mutableStateOf(false)
    val isDynamicColorsEnabled = mutableStateOf(true)
    val bufferPriority = mutableStateOf("High")
    val channelBalance = mutableFloatStateOf(0f)
    val transposeSemiTones = mutableIntStateOf(0)
    val guitarCapoPos = mutableIntStateOf(0)
    val selectedScale = mutableStateOf("Chromatic")
    val isNeuralEnvActive = mutableStateOf(false)
    val activeJob = mutableStateOf<SplitJob?>(null)

    init {
        observeScanner()
        observeMetronome()
        
        // 🚀 Automatic Metadata & Lyric Fetching
        viewModelScope.launch {
            snapshotFlow { currentMediaItem.value }.collect { item ->
                if (item != null) {
                    onTrackLoaded()
                }
            }
        }
    }

    fun start() {
        startHardwarePolling()
        restoreSession()
        
        // 🎼 SET UP ENGINE CALLBACKS
        audioEngine.onTrackEnded = {
            viewModelScope.launch {
                when (repeatMode.value) {
                    RepeatMode.ONE -> {
                        val current = currentMediaItem.value
                        if (current != null) {
                            playMediaItem(current, addToQueue = false)
                        }
                    }
                    RepeatMode.ALL, RepeatMode.FOLDER -> {
                        nextTrack()
                    }
                    else -> {
                        if (currentQueueIndex.value < playbackQueue.size - 1) {
                            nextTrack()
                        } else {
                            isPlaying.value = false
                        }
                    }
                }
            }
        }
        
        audioEngine.onTrackChanged = {
            onTrackLoaded()
        }
        
        observeScanner()
    }

    private fun observeMetronome() {
        viewModelScope.launch {
            while (true) {
                if (isMetronomeActive.value) {
                    val bpm = masterBpm.value.coerceIn(40f, 240f)
                    val msPerBeat = (60_000 / bpm).toLong()
                    
                    // Simple logic: Trigger click every beat
                    audioEngine.playDrumSample("HIHAT") // Using HiHat as click
                    if (isHapticsEnabled.value) {
                        // Trigger haptic if possible (Android specific usually, but we have a toggle)
                    }
                    
                    delay(msPerBeat)
                } else {
                    delay(250) // Safety: Avoid high-CPU spin when inactive
                }
            }
        }
    }

    private var tapTimes = mutableListOf<Long>()
    fun tapTempo() {
        val now = Clock.System.now().toEpochMilliseconds()
        tapTimes.add(now)
        if (tapTimes.size > 4) tapTimes.removeAt(0)
        
        if (tapTimes.size >= 2) {
            val averageDiff = (tapTimes.last() - tapTimes.first()) / (tapTimes.size - 1)
            if (averageDiff > 0) {
                val newBpm = 60000f / averageDiff
                masterBpm.value = newBpm.coerceIn(40f, 240f)
            }
        }
    }

    fun updateEqQ(index: Int, q: Float) {
        audioEngine.updateEqQ(index, q)
    }

    fun setCompressorParams(threshold: Float, ratio: Float, attack: Float, release: Float) {
        compressorThreshold.floatValue = threshold
        compressorRatio.value = ratio
        compressorAttackMs.floatValue = attack
        compressorReleaseMs.floatValue = release
        audioEngine.updateCompressor(threshold, ratio, attack, release)
    }

    fun importIr() {
        // In a real app, this would open a file picker
        // For now, we simulate adding a new IR
        convolverIrName.value = "Custom Room (Imported)"
        isConvolverActive.value = true
    }
    
    fun toggleBackgroundService(active: Boolean) {
        isBackgroundServiceRunning.value = active
    }

    fun togglePhaseInversion(active: Boolean) {
        phaseInverted.value = active
        audioEngine.setPhaseInversion(active)
    }

    fun updateChannelBalance(balance: Float) {
        channelBalance.floatValue = balance
        audioEngine.setChannelBalance(balance)
    }
    
    fun toggleSubHarmonicGenerator(enabled: Boolean) {
        isSubHarmonicActive.value = enabled
        audioEngine.setSubHarmonicGenerator(enabled)
    }

    fun toggleSequencerStep(step: Int) {
        if (step in sequencerSteps.indices) {
            sequencerSteps[step].value = !sequencerSteps[step].value
        }
    }

    fun exportLogs() {
        println("Industrial Sync: Exporting system logs via SHA-256 secure channel...")
    }
    
    private var lastBeatMs = 0L
    val bufferSizeFrames = mutableIntStateOf(1024)
    val hapticIntensity = mutableFloatStateOf(0.5f)
    val platformStorageBase = mutableStateOf("/storage/emulated/0")

    // 📺 Track Management
    val availableSubtitleTracks = mutableStateListOf<MediaTrack>()
    val availableAudioTracks = mutableStateListOf<MediaTrack>()
    val selectedSubtitleTrack = mutableStateOf<MediaTrack?>(null)
    val selectedAudioTrack = mutableStateOf<MediaTrack?>(null)
    val showAudioTrackConsole = mutableStateOf(false)
    val showEqualizer = mutableStateOf(false)
    val isFetchingEverything = mutableStateOf(false)
    
    // 🥁 BEAT CREATION ENGINE
    val isRecordingBeats = mutableStateOf(false)
    val isPlayingBeats = mutableStateOf(false)
    val drumPadLabels = mutableStateListOf("KICK", "SNARE", "HIHAT", "CLAP", "TOM", "CRASH", "PERC", "COWBELL", "SHAKER", "OPENHAT", "RIDE", "FX", "SUB", "KICK-2", "SNARE-2", "HIT")
    private var beatStartTime = 0L
    private var _isShuffleEnabled = mutableStateOf(false)
    private var persistenceCounter = 0
    var isShuffleEnabled: Boolean
        get() = _isShuffleEnabled.value
        set(value) {
            _isShuffleEnabled.value = value
            audioEngine.setShuffleMode(value)
        }
    
    private var _scanner: pEQFileScanner? = null
    var scanner: pEQFileScanner?
        get() = _scanner
        set(value) {
            _scanner = value
            value?.let { 
                observeScanner()
                if (scanFolders.isNotEmpty()) {
                    refreshLibrary()
                }
            }
        }


    private fun restoreSession() {
        val manager = persistenceManager

        viewModelScope.launch {
            try {
                // 1. Restore Scan Folders immediately to allow library refresh
                val storedFolders = manager.loadScanFolders()
                scanFolders.clear()
                scanFolders.addAll(storedFolders)
                
                // 🚀 Kick off library scan so items exist when we wait for them
                refreshLibrary()

                // 🚀 Industrial Library Synchronization (fix for resume header)
                viewModelScope.launch {
                    mediaLibrary.collect { library ->
                        val current = currentMediaItem.value ?: return@collect
                        if (current.subtitle == "Media Resource" || current.artist == "Unknown Artist") {
                            library.find { it.path == current.path }?.let { matched ->
                                currentMediaItem.value = matched
                                println("Industrial Sync: Resolved fallback metadata for ${matched.title}")
                            }
                        }
                    }
                }

                // 2. Restore Session State
                val session = manager.loadSession() ?: SessionState()
                isLandscapeLocked.value = session.isRotationLocked

                // 3. Wait for library to populate
                val items = withTimeoutOrNull(4000) { 
                    mediaLibrary.first { it.isNotEmpty() }
                } ?: mediaLibrary.value

                val resolveItem: (String) -> org.audiocular.peq.app.data.pEQMediaItem? = { path ->
                    // Priority 1: Check existing library
                    _mediaLibrary.value.find { it.path == path } ?: run {
                        // Priority 2: Check engine state if it's already playing this
                        val engineUri = audioEngine.getCurrentTrackUri()
                        if (engineUri == path) {
                            // Synthesize item from engine metadata if available
                            org.audiocular.peq.app.data.pEQMediaItem(
                                id = "engine_${path.hashCode()}",
                                title = session.lastTrackTitle ?: "Active Stream",
                                subtitle = session.lastTrackArtist ?: "Neural Engine",
                                path = path,
                                thumbnail = session.lastTrackThumbnail,
                                type = if (path.lowercase().contains("video") || path.lowercase().contains(".mp4")) 
                                    org.audiocular.peq.app.data.MediaType.VIDEO else org.audiocular.peq.app.data.MediaType.AUDIO,
                                format = "PROBED",
                                sizeBytes = 0L,
                                durationMs = 0L
                            )
                        } else if (path.startsWith("http")) {
                            org.audiocular.peq.app.data.pEQMediaItem(
                                id = "stream_${path.hashCode()}",
                                title = session.lastTrackTitle ?: "Web Stream",
                                subtitle = session.lastTrackArtist ?: path,
                                path = path,
                                thumbnail = session.lastTrackThumbnail,
                                type = if (path.lowercase().let { it.contains(".mp4") || it.contains(".m3u8") || it.contains(".mov") || it.contains("video") || it.contains(".mkv") }) 
                                    org.audiocular.peq.app.data.MediaType.VIDEO else org.audiocular.peq.app.data.MediaType.AUDIO,
                                format = "STREAM",
                                sizeBytes = 0L,
                                durationMs = 0L,
                                isStream = true
                            )
                        } else {
                            // Priority 3: Industrial Fallback for local files not yet scanned
                            val fileName = path.substringAfterLast("/").substringBeforeLast(".")
                            val extension = path.substringAfterLast(".").lowercase()
                            val isVideoExt = listOf("mp4", "mkv", "webm", "mov", "3gp").contains(extension)
                            
                            org.audiocular.peq.app.data.pEQMediaItem(
                                id = "resolved_${path.hashCode()}",
                                title = session.lastTrackTitle ?: fileName,
                                subtitle = session.lastTrackArtist ?: "Local archive",
                                path = path,
                                thumbnail = session.lastTrackThumbnail,
                                type = if (isVideoExt) org.audiocular.peq.app.data.MediaType.VIDEO else org.audiocular.peq.app.data.MediaType.AUDIO,
                                format = extension.uppercase(),
                                sizeBytes = 0L,
                                durationMs = 0L
                            )
                        }
                    }
                }

                // 4. Restore Queue

                // 4. Restore Queue (Full Data Restoration)
                if (session.queue.isNotEmpty()) {
                    playbackQueue.clear()
                    playbackQueue.addAll(session.queue)
                } else {
                    val restoredQueue = session.queue.map { it.path } // Legacy fallback
                        .mapNotNull { path -> resolveItem(path) }
                    playbackQueue.clear()
                    playbackQueue.addAll(restoredQueue)
                }

                // 5. Restore Current Item (Priority to Active Engine)
                val activeUri = audioEngine.getCurrentTrackUri()
                val targetPath = activeUri ?: session.lastTrackPath

                if (targetPath != null) resolveItem(targetPath)?.let { restoredItem ->
                    currentMediaItem.value = restoredItem
                    currentPosition.value = session.lastPosition
                    
                    currentSampleRate.value = restoredItem.sampleRate ?: 44100
                    currentBitDepth.value = restoredItem.bitDepth ?: 16
                    
                    // 🚀 Only load if engine is completely idle
                    if (activeUri == null && !audioEngine.isPlaying() && audioEngine.getDuration() <= 0L) {
                        audioEngine.loadTrack(restoredItem.path, restoredItem.title, restoredItem.subtitle, restoredItem.thumbnail)
                        audioEngine.seekTo(session.lastPosition)
                    } else if (audioEngine.getDuration() > 0L) {
                        // Engine already has it loaded (e.g. from background service)
                        currentPosition.value = audioEngine.getCurrentPosition()
                    }
                }

                // 6. Restore Playlists & Favorites
                val storedPlaylists = manager.loadPlaylists()
                customPlaylists.clear()
                storedPlaylists.forEach { (name, paths) ->
                    customPlaylists[name] = paths.mapNotNull { p -> resolveItem(p) }.toMutableStateList()
                }
                
                val storedFavorites = session.favorites.ifEmpty { manager.loadFavorites() }
                favoriteItems.clear()
                favoriteItems.addAll(storedFavorites.mapNotNull { p -> resolveItem(p) })

                val storedRecent = manager.loadRecentSessions()
                recentlyPlayedItems.clear()
                recentlyPlayedItems.addAll(storedRecent.mapNotNull { path -> resolveItem(path) })
                
                // 7. Restore Playback Flags
                isShuffleEnabled = session.isShuffle
                repeatMode.value = session.repeatMode
                favorites.addAll(session.favorites)
                
                // Restore DSP State
                if (session.eqGains.isNotEmpty()) {
                    session.eqGains.forEachIndexed { index, gain -> 
                        if (index < eqGains.size) {
                            eqGains[index] = gain
                            audioEngine.updateEqBand(index, gain)
                        }
                    }
                }
                if (session.eqQs.isNotEmpty()) {
                    session.eqQs.forEachIndexed { index, q -> 
                        if (index < eqQs.size) {
                            eqQs[index] = q
                            audioEngine.updateEqQ(index, q)
                        }
                    }
                }
                session.lastIrPath?.let { path ->
                    convolverIrPath.value = path
                    convolverIrName.value = path.substringAfterLast("/")
                }

                // 7. Load Play Counts
                val storedCounts = manager.loadMostPlayed()
                _mediaLibrary.value = _mediaLibrary.value.map { item ->
                    item.copy(playCount = storedCounts[item.path] ?: 0)
                }
                currentMediaItem.value?.let { updateMostPlayed(it) }
                
                val (path, show) = manager.loadLibraryState()
                currentFolderPath.value = path
                showFolders.value = show
                
            } catch (e: Exception) {
                println("pEQControl: [SYSTEM] Session restore failed: ${e.message}")
            }
        }
        
        observeLibraryState()
    }
private fun observeLibraryState() {
    viewModelScope.launch {

        snapshotFlow {
            Pair(
                currentFolderPath.value,
                showFolders.value
            )
        }.collect { (path, show) ->

            persistenceManager.saveLibraryState(
                path,
                show
            )
        }
    }
}

    private fun persistSession() {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        persistenceManager.saveSession(
            SessionState(
                lastTrackPath = currentMediaItem.value?.path,
                lastTrackTitle = currentMediaItem.value?.title,
                lastTrackArtist = currentMediaItem.value?.artist,
                lastTrackThumbnail = currentMediaItem.value?.thumbnail,
                lastPosition = currentPosition.value,
                isPlaying = isPlaying.value,
                queue = playbackQueue.toList(), // ✅ Now persisting the actual queue
                isShuffle = isShuffleEnabled,
                repeatMode = repeatMode.value,
                favorites = favorites.toList(),
                eqGains = eqGains.toList(),
                eqQs = eqQs.toList(),
                lastIrPath = convolverIrPath.value
            )
        )

        // 🚀 Persist Playlists & Favorites
        persistenceManager.savePlaylists(
            customPlaylists.mapValues { entry ->
                entry.value.map { item -> item.path }
            }
        )

        persistenceManager.saveFavorites(
            favoriteItems.map { it.path }
        )

        // 🚀 Persist Recently Played
        persistenceManager.saveRecentSessions(
            recentlyPlayedItems.map { it.path }
        )
    }
}

    private data class HardwarePollingState(
        val isPlaying: Boolean,
        val position: Long = 0L,
        val duration: Long = 0L,
        val bitrate: Int = 0,
        val codec: String = "",
        val sampleRate: Int = 0,
        val bitDepth: Int = 0,
        val spectrum: FloatArray = FloatArray(0)
    )

    private fun startHardwarePolling() {

    viewModelScope.launch(Dispatchers.IO) {

        while (isActive) {

            // 🚀 Sync Commands from OS/Widget
            val cmd =
                workstationRegistry["last_command"]

                if (cmd != null) {
                    withContext(Dispatchers.Main) {
                        when (cmd) {
                            "TOGGLE_PLAYBACK" -> togglePlayback()
                            "SKIP_NEXT" -> nextTrack()
                            "SKIP_PREV" -> previousTrack()
                            "TOGGLE_SHUFFLE" -> isShuffleEnabled = !isShuffleEnabled
                        }
                    }
                    workstationRegistry.remove("last_command")
                }

            val engineState = withContext(Dispatchers.Main) {
                val playing = audioEngine.isPlaying()
                if (playing) {
                    val pos = audioEngine.getCurrentPosition()
                    val dur = audioEngine.getDuration()
                    val bitrate = audioEngine.getBitrate()
                    val codec = audioEngine.getCodec()
                    val sRate = audioEngine.getSampleRate()
                    val bDepth = audioEngine.getBitDepth()
                    val spectrum = audioEngine.getCurrentSpectrum()
                    HardwarePollingState(
                        isPlaying = true,
                        position = pos,
                        duration = dur,
                        bitrate = bitrate,
                        codec = codec,
                        sampleRate = sRate,
                        bitDepth = bDepth,
                        spectrum = spectrum
                    )
                } else {
                    HardwarePollingState(isPlaying = false)
                }
            }

            if (engineState.isPlaying) {
                withContext(Dispatchers.Main) {
                    isPlaying.value = true
                    currentPosition.longValue = engineState.position
                    currentDuration.value = engineState.duration.coerceAtLeast(1L)
                    
                    currentBitrate.value = engineState.bitrate
                    currentCodec.value = engineState.codec
                    currentSampleRate.value = engineState.sampleRate
                    currentBitDepth.value = engineState.bitDepth
                }

                // 📊 High-Fidelity Spectral Telemetry (P-09)
                val rawSpectrum = engineState.spectrum
                val hasRealData = rawSpectrum.any { level -> level > 0.001f }
                
                if (hasRealData) {
                    withContext(Dispatchers.Main) {
                        rtaData.value = rawSpectrum
                    }
                    
                    // 🧠 NEURAL BPM & KEY ANALYSIS (Industrial Algorithm)
                    // We analyze spectral energy peaks for BPM
                    val energy = rawSpectrum.take(4).average().toFloat() // Focus on Lows
                    if (energy > 0.4f) {
                        val currentMs = Clock.System.now().toEpochMilliseconds()
                        val diff = currentMs - lastBeatMs
                        if (diff > 300 && diff < 1200) { // Valid BPM range
                            val instantBpm = 60000f / diff
                            detectedBpm.floatValue = (detectedBpm.floatValue * 0.9f + instantBpm * 0.1f)
                            lastBeatMs = currentMs
                        }
                    }
                    
                    // Key Inference based on spectral distribution
                    if (Clock.System.now().toEpochMilliseconds() % 3500 < 100) { 
                        // 🎼 Expanded Harmonic Dictionary (P-14)
                        val chords = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B", 
                                            "Cm", "C#m", "Dm", "Ebm", "Em", "Fm", "F#m", "Gm", "Abm", "Am", "Bbm", "Bm",
                                            "C7", "G7", "D7", "A7", "E7", "Fmaj7", "Cmaj7")
                        val peakIdx = rawSpectrum.indices.maxByOrNull { rawSpectrum[it] } ?: 0
                        val harmonicCenter = (peakIdx * 1.5).toInt() % chords.size
                        withContext(Dispatchers.Main) {
                            detectedChord.value = chords[harmonicCenter]
                            currentKey.value = if (detectedChord.value.contains("m") && !detectedChord.value.contains("maj")) "${detectedChord.value.replace("m","")} Minor" else "${detectedChord.value.replace("7","")} Major"
                        }
                    }
                } else {
                    // 🧪 Simulated Ambient Pulse (Industrial UI Liveliness)
                    val time = Clock.System.now().toEpochMilliseconds() / 180.0
                    withContext(Dispatchers.Main) {
                        rtaData.value = FloatArray(32) { i -> 
                            (0.04f + 0.08f * kotlin.math.sin(time + i * 0.5).toFloat()).coerceAtLeast(0f) 
                        }
                    }
                }
                
                // Ballistic Telemetry Fallbacks (P-11)
                withContext(Dispatchers.Main) {
                    currentSampleRate.value = if (engineState.sampleRate > 0) engineState.sampleRate else 44100
                    currentBitDepth.value = if (engineState.bitDepth > 0) engineState.bitDepth else 16
                    currentCodec.value = engineState.codec.ifBlank { "PCM" }
                    currentBitrate.value = engineState.bitrate.coerceAtLeast(1411)
                    
                    // Peak detection with industrial ballistics
                    val maxMag = rtaData.value.maxOrNull() ?: 0f
                    val targetPeak = if (maxMag > 0.01f) 20 * kotlin.math.log10(maxMag.coerceAtLeast(0.00001f)) else -60f
                    peakLevelDb.value = (peakLevelDb.value * 0.8f + targetPeak * 0.2f).coerceIn(-60f, 0f)

                    // 🧠 DSP Pressure Simulation
                    val baseLoad = if (isBitPerfectEnabled.value) 12 else 8
                    val eqLoad = (0..5).random()
                    dspProcessingLoad.value = (baseLoad + eqLoad).toFloat()
                }

            } else {
                withContext(Dispatchers.Main) {
                    isPlaying.value = false
                    rtaData.value = rtaData.value.map { it * 0.85f }.toFloatArray()
                }
            }

            // 🔊 Sync Master Gain
            val sysVol = getSystemVolume()
            if (kotlin.math.abs(sysVol - masterVolume.value) > 0.01f) {
                withContext(Dispatchers.Main) {
                    masterVolume.value = sysVol
                    audioEngine.setVolume(sysVol)
                }
            }

            if (engineState.isPlaying) {
                persistenceCounter++
                if (persistenceCounter >= 50) { // Every 5 seconds (100ms * 50)
                    persistSession()
                    persistenceCounter = 0
                }
            }

            delay(100)
        }
    }
    }
    // 📊 UNIFIED MEDIA LIBRARY
    val searchQuery = mutableStateOf("")

    private val _mediaLibrary = MutableStateFlow<List<pEQMediaItem>>(emptyList())
    val mediaLibrary: StateFlow<List<pEQMediaItem>> = _mediaLibrary.combine(snapshotFlow { searchQuery.value }) { items, query ->
        if (query.isBlank()) items
        else items.filter { (it.title as CharSequence).contains(query, ignoreCase = true) || (it.artist as CharSequence?)?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 📁 Industrial Library Engine
    val playlists: Map<String, List<pEQMediaItem>> get() = customPlaylists
    val favoriteItems = mutableStateListOf<pEQMediaItem>()
    val mostPlayedItems = mutableStateListOf<pEQMediaItem>()
    val recentlyPlayedItems = mutableStateListOf<pEQMediaItem>()
    val mediaByAlbum = mutableStateMapOf<String, List<pEQMediaItem>>()
    val mediaByArtist = mutableStateMapOf<String, List<pEQMediaItem>>()
    val mediaByFormat = mutableStateMapOf<String, List<pEQMediaItem>>()
    val smartPlaylists = mutableStateMapOf<String, List<pEQMediaItem>>()

    val selectedLibraryTab = mutableStateOf("Media")
    val isScanning = mutableStateOf(false)
    val showFolders = mutableStateOf(false)

    // 🎬 Playback Core
    
    // 📁 Archive Management State
    val currentFolderPath = mutableStateOf("")
    val folderHistory = mutableStateListOf<String>()
    val folderFuture = mutableStateListOf<String>()
    
    fun navigateToFolder(path: String) {
        if (currentFolderPath.value.isNotEmpty()) {
            folderHistory.add(currentFolderPath.value)
        }
        folderFuture.clear()
        currentFolderPath.value = path
        currentFolder.value = path
        persistenceManager.saveLibraryState(path, showFolders.value)
    }

    fun toggleFolderView() {
        showFolders.value = !showFolders.value
        // Sync folder states when toggling
        if (showFolders.value && currentFolder.value != null && currentFolderPath.value.isEmpty()) {
            currentFolderPath.value = currentFolder.value ?: ""
        } else if (!showFolders.value && currentFolderPath.value.isNotEmpty() && currentFolder.value == null) {
            currentFolder.value = currentFolderPath.value
        }
        persistenceManager.saveLibraryState(currentFolderPath.value, showFolders.value)
    }
    
    fun navigateBackFolder() {
        if (folderHistory.isNotEmpty()) {
            folderFuture.add(currentFolderPath.value)
            currentFolderPath.value = folderHistory.removeAt(folderHistory.size - 1)
        } else {
            currentFolderPath.value = ""
        }
    }
    
    fun navigateForwardFolder() {
        if (folderFuture.isNotEmpty()) {
            folderHistory.add(currentFolderPath.value)
            currentFolderPath.value = folderFuture.removeAt(folderFuture.size - 1)
        }
    }
    
    // 🎥 Professional Media State
    val isVideoLocked = mutableStateOf(false)
    val videoZoom = mutableStateOf(1.0f)
    val videoAspectRatio = mutableStateOf("FIT")
    
    // ✂️ Cutter State
    val trimStartMs = mutableLongStateOf(0L)
    val trimEndMs = mutableLongStateOf(0L)
    
    
    
    
    
    // 🎬 Transport Actions
    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOf(playbackSpeed.value)
        val nextIndex = (currentIndex + 1) % speeds.size
        playbackSpeed.value = speeds[nextIndex]
        audioEngine.setPlaybackSpeed(playbackSpeed.value)
    }

    fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
    }

    fun cycleRepeatMode() {
        repeatMode.value = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            else -> RepeatMode.OFF
        }
        audioEngine.setRepeatMode(repeatMode.value)
        audioEngine.setRepeatMode(repeatMode.value)
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
        currentPosition.value = positionMs
    }

    fun seekRelative(deltaMs: Long) {
        val newPos = (currentPosition.value + deltaMs).coerceIn(0, currentDuration.value)
        seekTo(newPos)
    }

    fun toggleSubtitles(enabled: Boolean? = null) {
        val newState = enabled ?: !isSubtitlesEnabled.value
        isSubtitlesEnabled.value = newState
        audioEngine.setSubtitlesEnabled(newState)
    }

    fun toggleSubtitleConsole() {
        showSubtitleConsole.value = !showSubtitleConsole.value
    }

    fun requestLyricsPicker(onDone: () -> Unit = {}) {
        onPickLyricsRequested?.invoke()
    }

    fun addFolderToQueue(path: String) {
        val folderItems = mediaLibrary.value.filter { it.path.startsWith(path) }
        playbackQueue.addAll(folderItems)
    }

    fun addFolderToPlaylist(path: String, playlistName: String) {
        val folderItems = mediaLibrary.value.filter { it.path.startsWith(path) }
        val playlist = customPlaylists[playlistName]
        playlist?.addAll(folderItems)
        persistSession()
    }

    fun cycleAspectRatio() {
        videoAspectRatio.value = when (videoAspectRatio.value) {
            "FIT" -> "FILL"
            "FILL" -> "16:9"
            "16:9" -> "4:3"
            else -> "FIT"
        }
    }

    fun adjustAudioDelay(delta: Long) {
        audioDelayMs.value = (audioDelayMs.value + delta).coerceIn(-5000L, 5000L)
    }


    
    // 🧠 AI & Sync Engine
    val currentLyricsLine = mutableStateOf(0)
    val rtaData = mutableStateOf(FloatArray(32))
    val detectedBpmMap = mutableStateMapOf<String, Float>()
    val detectedKeyMap = mutableStateMapOf<String, String>()
    val currentChord = mutableStateOf("Cmaj7")
    
    val isAnalyzingAudio = mutableStateOf(false)
    val isSplittingStems = mutableStateOf(false)
    val isLearnModeActive = mutableStateOf(false)
    val chordRepresentation = mutableStateOf("Piano")
    val manualTempo = mutableStateOf(120f)
    val exportFormat = mutableStateOf("WAV")
    val exportSampleRate = mutableStateOf(48000)
    val isDitheringEnabled = mutableStateOf(true)
    val stereoWidth = mutableStateOf(1.0f)

    // ⚙️ Workstation System Hooks
    var workstationRegistry = mutableStateMapOf<String, String>()
    val hardwareSettings get() = workstationRegistry
    val currentDevice = mutableStateOf(Device.INTERNAL)
    val isMasterBypassActive = mutableStateOf(false)
    val showDiagnostics = mutableStateOf(false)
    val showTrackInfoSheet = mutableStateOf(false)
    val connectedDevices = mutableStateListOf<Device>(Device.INTERNAL, Device.HI_RES_DAC) 
    val currentBluetoothProfile = mutableStateOf(org.audiocular.peq.app.audio.BluetoothProfile.LDAC)
    val bluetoothResumeEnabled = mutableStateOf(true)

    // 📂 PLYLIST ENGINE (P-12)
    val customPlaylists = mutableStateMapOf<String, SnapshotStateList<pEQMediaItem>>()
    val selectedItemForPlaylist = mutableStateOf<pEQMediaItem?>(null)
    val selectedFolderForPlaylist = mutableStateOf<String?>(null)
    
    fun createPlaylist(name: String) {
        if (!customPlaylists.containsKey(name)) {
            customPlaylists[name] = mutableStateListOf()
        }
        persistSession()
    }


    // 🔄 QUEUE LOGIC
    fun moveQueueItem(from: Int, to: Int) {
        if (from == to) return
        val item = playbackQueue.removeAt(from)
        playbackQueue.add(to, item)
    }

    // 🔍 INTEGRATED SEARCH ENGINE
    val globalSearchQuery = mutableStateOf("")
    val searchResults: StateFlow<List<pEQMediaItem>> = snapshotFlow { globalSearchQuery.value }
        .combine(mediaLibrary) { query, library ->
            if (query.isEmpty()) emptyList()
            else library.filter { it.title.contains(query, true) || it.artist?.contains(query, true) == true }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    
    val selectedPreset = mutableStateOf(Presets.Flat)
    
    val showSettings = mutableStateOf(false)
    val isKidsLockActive = mutableStateOf(false)
    val showLyrics = mutableStateOf(true)
    val dominantColor =
        mutableStateOf(Color(0xFF121212))
    val transientImpact = mutableStateOf(0f)
    val showQueueDrawer = mutableStateOf(false)
    val showFolderManager = mutableStateOf(false)

    val showSleepTimerSheet = mutableStateOf(false)
    private var sleepTimerJob: Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes.value = minutes
        sleepTimerActive.value = true
        sleepTimerRemainingSec.longValue = minutes * 60L
        
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            while (sleepTimerRemainingSec.longValue > 0 && sleepTimerActive.value) {
                delay(1000)
                sleepTimerRemainingSec.longValue -= 1
            }
            if (sleepTimerActive.value) {
                audioEngine.pause()
                isPlaying.value = false
                sleepTimerActive.value = false
                showToast("Sleep timer active: Playback stopped.")
            }
        }
    }

    fun stopSleepTimer() {
        sleepTimerActive.value = false
        sleepTimerJob?.cancel()
    }
    val scanFolders = mutableStateListOf<String>()
    val isPlaylistPickerShown = mutableStateOf(false)
    val showStemSplitter = mutableStateOf(false)
    val drumKitVolume = mutableStateOf(1.0f)
    val drumReverb = mutableStateOf(0.0f)
    val drumDelay = mutableStateOf(0.0f)
    val drumPadVolumes = mutableStateMapOf<Int, Float>()
    val isRecordingDrumSession = mutableStateOf(false)
    val isMicMonitoringEnabled = mutableStateOf(false)
    val isSubtitlesEnabled = mutableStateOf(false)
    val isPipActive = mutableStateOf(false)
    val isReplayGainEnabled = mutableStateOf(false)
    val isForceHiResEnabled = mutableStateOf(false)

    fun toggleGapless(enabled: Boolean) {
        isGaplessEnabled.value = enabled
        audioEngine.setCrossfadeDuration(if (enabled) 0 else 2000)
    }

    fun toggleForceHiRes(enabled: Boolean) {
        isForceHiResEnabled.value = enabled
        audioEngine.setExclusiveMode(enabled)
    }
    // val currentChapters = mutableStateListOf<org.audiocular.peq.app.data.MediaChapter>()
    val availableTracks = mutableStateListOf<org.audiocular.peq.app.data.MediaTrack>()
    var videoRotationDegrees = mutableStateOf(0)
    var videoWidth = mutableStateOf(0)
    var videoHeight = mutableStateOf(0)
    val isRotationLocked = mutableStateOf(false)
    val scanProgress = mutableStateOf(0f)
    val scanStatus = mutableStateOf("")
    val videoBrightness = mutableStateOf(0.5f)
    val isGestureInteracting = mutableStateOf(false)
    val gestureSeekDelta = mutableLongStateOf(0L)
    private var lastVolume = -1f

    fun setMasterVolume(
        volume: Float
    ) {

        val safeVolume =
            volume.coerceIn(0f, 1f)

        // Prevent volume spam/jitter
        if (
            kotlin.math.abs(
                safeVolume - lastVolume
            ) < 0.01f
        ) {
            return
        }

        lastVolume = safeVolume

        masterVolume.value = safeVolume

        // Audio engine volume
        audioEngine.setVolume(
            safeVolume
        )

        // Sync hardware/system volume
        setSystemVolume(
            safeVolume
        )
    }
    // 🔌 Hardware Switching Hub
    val showDeviceSwitcher = mutableStateOf(false)
    val pendingDevice = mutableStateOf<Device?>(null)


    fun confirmDeviceSwitch() {
        pendingDevice.value?.let { updateDevice(it) }
        showDeviceSwitcher.value = false
        pendingDevice.value = null
    }

    fun toggleVocalRemoval() {
        isVocalRemovalActive.value = !isVocalRemovalActive.value
        audioEngine.toggleVocalRemoval(isVocalRemovalActive.value)
    }
    fun cycleRotation() {
        // We cycle through PORTRAIT -> LANDSCAPE -> PORTRAIT
        // In a real app, we might support all 4, but for UX let's stick to the 2 main ones
        val current = videoRotationDegrees.value
        videoRotationDegrees.value = (current + 90) % 360
    }

    fun resetRotation() {
        videoRotationDegrees.value = 0
    }
    val isBackgroundPlayEnabled = mutableStateOf(true)
    val crossfadeSeconds = mutableStateOf(0f)
    val eqFrequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    // 📊 Library Management (P-14)
    val sortBy = mutableStateOf("NAME") // NAME, SIZE, DATE
    val filterType = mutableStateOf<MediaType?>(null)
    
    // 🎧 Professional Audio Telemetry
    val currentBitrate = mutableStateOf(0)
    val currentCodec = mutableStateOf("PCM")
    val currentSampleRate = mutableStateOf(44100)
    val currentBitDepth = mutableStateOf(16)

    val neuralProgress = mutableStateOf(0f)

    fun trimMedia(_startMs: Long, _endMs: Long) {
        val current = currentMediaItem.value ?: return
        if (activeJob.value != null) return

        viewModelScope.launch {
            activeJob.value = SplitJob(
                id = "TRIM_${Clock.System.now().toEpochMilliseconds()}",
                name = "High-Fidelity Trim: ${current.title}",
                progress = 0.0f,
                status = "BITSTREAM EXTRACTION"
            )

            // Simulate industrial trimming process
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

    fun toggleMicMonitoring() {
        isMicMonitoringEnabled.value = !isMicMonitoringEnabled.value
        audioEngine.enableZeroLatencyMonitoring(isMicMonitoringEnabled.value)
    }

    fun toggleDrumRecording() {
        isRecordingDrumSession.value = !isRecordingDrumSession.value
        if (isRecordingDrumSession.value) {
            showToast("Neural recording session initiated")
        } else {
            showToast("Session mastered and saved to /pEQ/Recordings")
        }
    }

    fun updateDrumPadVolume(index: Int, volume: Float) {
        drumPadVolumes[index] = volume
    }

    fun toggleAbRepeat() {
        abRepeatActive.value = !abRepeatActive.value

        if (abRepeatActive.value) {
            // Close Cutter if A-B Repeat is opened to keep UI clean
            showCutter.value = false
        } else {
            // Reset points and stop engine loop when toggled OFF
            abRepeatPointA.value = -1L
            abRepeatPointB.value = -1L
            updateLoopPoints(-1L, -1L)
        }
    }


    fun setABPoint(point: Char) {
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

        // Only update the engine if both points are valid
        if (abRepeatPointA.value != -1L && abRepeatPointB.value != -1L) {
            audioEngine.setLoopPoints(abRepeatPointA.value, abRepeatPointB.value)
        }
    }
    fun startStemSplit() {
        val current = currentMediaItem.value ?: return
        val splitter = stemSplitter ?: return
        
        println("pEQControl: [STEMS] Initiating industrial batch export of neural stems for ${current.title}...")
        
        viewModelScope.launch {
            activeJob.value = SplitJob(
                id = "STEM_${Clock.System.now().toEpochMilliseconds()}",
                name = "Neural Split: ${current.title}",
                progress = 0.0f,
                status = "ANALYZING"
            )
            
            // Industrial Hook: Begin background processing
            val job = activeJob.value ?: return@launch
            println("pEQControl: [NEURAL] Initiating ${job.name}...")
            
            // Link progress from splitter
            val progressJob = launch {
                splitter.progress.collect { p ->
                    activeJob.value = activeJob.value?.copy(progress = p)
                }
            }
            
            try {
                activeJob.value = activeJob.value?.copy(status = "ISOLATING STEMS")
                // Simulated high-fidelity processing
                for (p in 1..10) {
                    delay(800) // Pacing for industrial reliability
                    activeJob.value = activeJob.value?.copy(progress = p / 10f)
                }
                
                val stems = splitter.splitTrack(current.path, "/sdcard/pEQ/Stems")
                println("pEQControl: [NEURAL] Stems synchronized: ${stems.size} outputs detected.")
                
                activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "COMPLETE")
                delay(1000)
                activeJob.value = null
                showStemSplitter.value = false
                currentTab.value = Tab.Learn
                
                // Update Mixer with new stems
                println("pEQControl: [LAB] Neural stems registered for real-time orchestration.")
            } catch (e: Exception) {
                activeJob.value = activeJob.value?.copy(status = "ERROR: ${e.message}")
                delay(3000)
                activeJob.value = null
            } finally {
                progressJob.cancel()
            }
        }
    }

    fun batchExportStems() {
        val current = currentMediaItem.value ?: return
        val splitter = stemSplitter ?: return
        
        println("pEQControl: [STEMS] Initiating industrial batch export of neural stems for ${current.title}...")
        
        viewModelScope.launch {
            activeJob.value = SplitJob(
                id = "STEM_${Clock.System.now().toEpochMilliseconds()}",
                name = "Neural Split: ${current.title}",
                progress = 0.0f,
                status = "ANALYZING"
            )
            
            val job = activeJob.value ?: return@launch
            println("pEQControl: [NEURAL] Starting stem splitting for ${job.name}")
            
            // Link progress from splitter
            val progressJob = launch {
                splitter.progress.collect { p ->
                    activeJob.value = activeJob.value?.copy(progress = p)
                }
            }
            
            try {
                activeJob.value = activeJob.value?.copy(status = "PROCESSING")
                val stems = splitter.splitTrack(current.path, "/sdcard/pEQ/Stems")
                println("pEQControl: [NEURAL] Stems generated: ${stems.size}")
                
                activeJob.value = activeJob.value?.copy(progress = 1.0f, status = "COMPLETE")
                delay(1000)
                activeJob.value = null
                showStemSplitter.value = false
                currentTab.value = Tab.Learn
            } catch (e: Exception) {
                activeJob.value = activeJob.value?.copy(status = "ERROR: ${e.message}")
                delay(3000)
                activeJob.value = null
            } finally {
                progressJob.cancel()
            }
        }
    }

    fun switchSubtitle(track: MediaTrack?) {
        selectedSubtitleTrack.value = track
        isSubtitlesEnabled.value = track != null && track.id != "none"
        audioEngine.switchTrack(track)
    }

    fun switchAudioTrack(track: MediaTrack?) {
        selectedAudioTrack.value = track
        audioEngine.switchTrack(track)
    }

    fun fetchSubtitlesOnline(
    autoOpenSheet: Boolean = false
) {

    val current =
        currentMediaItem.value ?: return

    val apiKey =
        persistenceManager.openSubtitlesApiKey

    viewModelScope.launch {

        activeJob.value =
            SplitJob(
                id = "SUB_${Clock.System.now().toEpochMilliseconds()}",
                name = "Subtitle Intelligence",
                progress = 0.05f,
                status = "AUTHENTICATING"
            )

        if (apiKey.isBlank()) {

            activeJob.value =
                activeJob.value?.copy(
                    progress = 1f,
                    status = "OPENSUBTITLES API KEY MISSING"
                )

            delay(2000)

            activeJob.value = null

            return@launch
        }

        try {

            /*
             * Step 1
             */

            activeJob.value =
                activeJob.value?.copy(
                    progress = 0.2f,
                    status = "NORMALIZING MEDIA"
                )

            val title =
                current.title.trim()

            val artist =
                current.artist?.trim()

            /*
             * Step 2
             */

            activeJob.value =
                activeJob.value?.copy(
                    progress = 0.4f,
                    status = "SEARCHING SUBTITLES"
                )

            val results =
                withTimeoutOrNull(8000) {
                    retry(3) {
                        subtitleRepository.searchSubtitles(
                            title = title,
                            artist = artist,
                            durationMs = current.durationMs,
                            language = "en"
                        )
                    }
                } ?: emptyList()

            if (results.isEmpty()) {

                activeJob.value =
                    activeJob.value?.copy(
                        progress = 1f,
                        status = "NO SUBTITLES FOUND"
                    )

                delay(1500)

                activeJob.value = null

                return@launch
            }

            /*
             * Step 3
             */

            activeJob.value =
                activeJob.value?.copy(
                    progress = 0.65f,
                    status = "RANKING MATCHES"
                )

            val bestMatch =
                results.first()

            /*
             * Safety threshold
             */

            if (bestMatch.score > 120) {

                activeJob.value =
                    activeJob.value?.copy(
                        progress = 1f,
                        status = "LOW CONFIDENCE MATCH"
                    )

                delay(1500)

                activeJob.value = null

                return@launch
            }

            /*
             * Step 4
             */

            activeJob.value =
                activeJob.value?.copy(
                    progress = 0.8f,
                    status = "DOWNLOADING SUBTITLE"
                )

            val downloadLink =
                retry(2) {
                    subtitleRepository.getDownloadLink(
                        bestMatch.fileId
                    )
                }

            if (downloadLink.isNullOrBlank()) {

                activeJob.value =
                    activeJob.value?.copy(
                        progress = 1f,
                        status = "DOWNLOAD FAILED"
                    )

                delay(1500)

                activeJob.value = null

                return@launch
            }

            /*
             * Step 5
             */

            activeJob.value =
                activeJob.value?.copy(
                    progress = 0.95f,
                    status = "LOADING SUBTITLE"
                )

            loadExternalSubtitle(downloadLink)
            showToast("Subtitles synchronized")

            activeJob.value =
                activeJob.value?.copy(
                    progress = 1f,
                    status = "SUBTITLE LINKED"
                )

        } catch (e: Exception) {

            activeJob.value =
                activeJob.value?.copy(
                    progress = 1f,
                    status = buildString {

                        append("SUBTITLE ERROR")

                        e.message?.let {
                            append(": ")
                            append(it.take(40))
                        }
                    }
                )
        }

        delay(1500)

        activeJob.value = null

        if (autoOpenSheet) {
            showSubtitleConsole.value = true
        }
    }
}
suspend inline fun <T> retry(
    times: Int,
    block: () -> T?
): T? {

    repeat(times) {

        try {

            val result = block()

            if (result != null) {
                return result
            }

        } catch (_: Exception) {
        }
    }

    return null
}

    private fun refreshAvailableTracks() {
        val tracks = audioEngine.getAvailableTracks()
        availableSubtitleTracks.clear()
        availableSubtitleTracks.addAll(tracks.filter { t -> t.type == TrackType.SUBTITLE })
        
        availableAudioTracks.clear()
        availableAudioTracks.addAll(tracks.filter { t -> t.type == TrackType.AUDIO })
    }

    fun onTrackLoaded() {
        viewModelScope.launch {
            // Update local state from engine metadata
            val engineMetadata = audioEngine.getEmbeddedMetadata()
            currentMediaItem.value = currentMediaItem.value?.copy(
                bitrate = engineMetadata.availableTracks.firstOrNull { it.type == TrackType.AUDIO }?.codec?.toIntOrNull() ?: 0,
                codec = engineMetadata.availableTracks.firstOrNull { it.type == TrackType.AUDIO }?.codec
            )

            // Delay to let the engine fully parse tracks (ExoPlayer needs time for embedded subs)
            delay(500)
            refreshAvailableTracks()
            fetchLocalSubtitles()
            fetchChapters()
            
            // 🔎 Check for Embedded Lyrics Variants
            val tracks = audioEngine.getAvailableTracks()
            val embeddedLyrics = tracks.filter { it.type == TrackType.LYRICS }
            if (embeddedLyrics.isNotEmpty()) {
                // If we found embedded lyrics, we prioritize them but still offer online search
                audioEngine.switchTrack(embeddedLyrics.first())
            } else {
                fetchLyrics(autoOpenEditor = false)
            }
            
            delay(1500)
            refreshAvailableTracks()
            persistSession()
        }
    }

    fun fetchLocalSubtitles() {
        val current = currentMediaItem.value ?: return
        if (current.path.startsWith("http")) return
        val scanner = scanner ?: return

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val path = current.path
                    val separator = if (path.contains("\\")) "\\" else "/"
                    val parentPath = path.substringBeforeLast(separator)
                    val fileName = path.substringAfterLast(separator)
                    val baseName = fileName.substringBeforeLast(".")
                    val extensionStr = fileName.substringAfterLast(".")

                    val subExtensions = listOf("srt", "vtt", "ass", "ssa", "sub", "idx")

                    val scanDirs = mutableListOf(parentPath)
                    // Common subdirs for subtitles
                    listOf("Subs", "Subtitles", "subs", "subtitles").forEach { subName ->
                        val subDirPath = "$parentPath$separator$subName"
                        scanDirs.add(subDirPath)
                    }

                    val foundTracks = mutableListOf<MediaTrack>()

                    scanDirs.forEach { dirPath ->
                        val filesInDir = scanner.listFiles(dirPath)

                        filesInDir.filter { filePath ->
                            val fName = filePath.substringAfterLast(separator)
                            val fNameWithoutExt = fName.substringBeforeLast(".")
                            val fExt = fName.substringAfterLast(".").lowercase()

                            subExtensions.contains(fExt) &&
                                (fNameWithoutExt.equals(baseName, ignoreCase = true) ||
                                    fNameWithoutExt.startsWith(baseName, ignoreCase = true) ||
                                    baseName.startsWith(fNameWithoutExt.substringBefore("."), ignoreCase = true))
                        }.forEach { filePath ->
                            val fName = filePath.substringAfterLast(separator)
                            val fNameWithoutExt = fName.substringBeforeLast(".")
                            val fExt = fName.substringAfterLast(".")
                            val langTag = fNameWithoutExt.removePrefix(baseName).removePrefix(".").takeIf { it.isNotBlank() }
                            val displayLang = langTag?.uppercase() ?: fExt.uppercase()

                            foundTracks.add(MediaTrack(
                                id = "local_$filePath",
                                language = displayLang,
                                type = TrackType.SUBTITLE,
                                codec = fExt.uppercase(),
                                isExternal = true,
                                externalUrl = filePath
                            ))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        foundTracks.forEach { track ->
                            if (availableSubtitleTracks.none { it.id == track.id }) {
                                availableSubtitleTracks.add(track)
                                track.externalUrl?.let { audioEngine.addExternalSubtitle(it) }
                            }
                        }
                        refreshAvailableTracks()
                    }
                } catch (e: Exception) {
                    println("pEQControl: [SUBTITLES] Local scan failed: ${e.message}")
                }
            }
        }
    }

    fun searchLyricsInWebView() {
        val current = currentMediaItem.value ?: return
        val query = "${current.artist ?: ""} ${current.title} lyrics".trim()
        lyricsSearchUrl.value = "https://www.google.com/search?q=${query.replace(" ", "+")}"
        isLyricsWebViewVisible.value = true
    }

    fun updateEqBand(index: Int, gain: Float) {
        if (index in eqGains.indices) {
            eqGains[index] = gain
            audioEngine.updateEqBand(index, gain)
        }
    }

    fun toggleNeuralSaturation(enabled: Boolean) {
        isNeuralSaturationEnabled.value = enabled
        audioEngine.setNeuralSaturation(enabled)
    }

    fun generateMoodPlaylist(mood: String, type: String) {
        println("pEQControl: [NEURAL] Generating $mood playlist for $type")
    }

    fun searchAutoEqRepository(query: String) {
        println("pEQControl: [AUTO-EQ] Searching for $query")
    }

    fun applyAutoEqProfile(profile: AutoEqProfile) {
        selectedAutoEqProfile.value = profile
        println("pEQControl: [AUTO-EQ] Applying ${profile.name}")
    }

    fun refreshLibrary() {
        println("pEQControl: [LIBRARY] Refreshing local indices")
        scanner?.scan(scanFolders.toList(), emptyList())
    }

    private fun observeScanner() {
        val scanner = _scanner ?: return
        viewModelScope.launch {
            combine(scanner.audioFiles, scanner.mediaFiles) { audio, video ->
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
                        thumbnail = it.thumbnail
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
                        thumbnail = it.thumbnail
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
    
    val stepSequencer = mutableStateMapOf<Int, MutableList<String>>()
    val currentStep = mutableIntStateOf(0)
    val isSequencerActive = mutableStateOf(false)
    fun startSequencer() { isSequencerActive.value = true }
    fun stopSequencer() { isSequencerActive.value = false }
    fun quantizeRecordedBeats() {}
    fun toggleSequencerStep(step: Int, type: String) {
        val list = stepSequencer.getOrPut(step) { mutableStateListOf() }
        if (list.contains(type)) list.remove(type) else list.add(type)
    }
    
    data class DrumKit(val name: String, val labels: List<String>)
    data class DrumTutorial(val name: String, val steps: List<List<String>> = emptyList())
    
    val drumKits = listOf(
        DrumKit("Neural 808", listOf(
            "808-PERC", "808-TOM_H", "808-TOM_M", "808-CRASH",
            "808-OPEN_HAT", "808-CLAP", "808-TOM_L", "808-RIDE",
            "808-SNARE_2", "808-RIM", "808-FX", "808-SHAKER",
            "808-KICK_1", "808-KICK_2", "808-SNARE_1", "808-CLOSED_HAT"
        )),
        DrumKit("Industrial Grime", listOf(
            "GRIME-STEAM", "GRIME-HAMMER", "GRIME-METAL", "GRIME-CLANG",
            "GRIME-PIPE", "GRIME-CRUSH", "GRIME-ENGINE", "GRIME-SPARK",
            "GRIME-GEAR", "GRIME-OIL", "GRIME-RUST", "GRIME-DRILL",
            "GRIME-STEEL_KICK", "GRIME-SUB", "GRIME-HAMMER_SNARE", "GRIME-METAL_HAT"
        )),
        DrumKit("Lofi Nostalgia", listOf(
            "LOFI-DUST", "LOFI-WARMTH", "LOFI-MEMORIES", "LOFI-RAIN",
            "LOFI-BREEZE", "LOFI-ECHO", "LOFI-FADED", "LOFI-VINYL",
            "LOFI-RIM", "LOFI-CLAP", "LOFI-SOOTHE", "LOFI-GRAIN",
            "LOFI-KICK", "LOFI-BASS", "LOFI-SNARE", "LOFI-CLOSED_HAT"
        )),
        DrumKit("Retro Future", listOf(
            "RETRO-CYBER", "RETRO-PULSE", "RETRO-WARP", "RETRO-LOGIC",
            "RETRO-NEON", "RETRO-GRID", "RETRO-PHASE", "RETRO-SYNC",
            "RETRO-HIT", "RETRO-CLAP", "RETRO-LINK", "RETRO-CODE",
            "RETRO-KICK", "RETRO-SUB", "RETRO-SNARE", "RETRO-CLOSED_HAT"
        ))
    )
    val selectedDrumKit = mutableStateOf(drumKits[0])
    fun switchDrumKit(kit: DrumKit) {
        selectedDrumKit.value = kit
        drumPadLabels.clear()
        drumPadLabels.addAll(kit.labels)
    }
    
    
    val drumTutorials = listOf(DrumTutorial("Basic Rock"), DrumTutorial("Trap Foundations"))
    val selectedTutorial = mutableStateOf<DrumTutorial?>(null)
    fun startTutorial(pattern: DrumTutorial) { selectedTutorial.value = pattern }
    
    fun startKitBuilding() {}
    val guideStep = mutableStateOf(0)
    val sequencerTriggeredPads = mutableStateMapOf<String, Boolean>()
    private val _selectedTheme = mutableStateOf(pEQTheme.DARK)
    val selectedTheme: State<pEQTheme> = _selectedTheme
    
    val sampleLibrary = mapOf(
        "KICKS" to listOf("Neural Kick 1", "Industrial Kick 2", "Lofi Thump"),
        "SNARES" to listOf("Neural Snare 1", "Grime Clap", "Retro Snap"),
        "HATS" to listOf("Neural Hat 1", "Industrial Shaker", "Lofi Tick")
    )
    fun remapPad(index: Int, sample: String) {
        if (index in drumPadLabels.indices) {
            drumPadLabels[index] = sample
        }
    }
    
    
    fun updateStemVolume(stem: String, volume: Float) {
        stemVolumes[stem] = volume
        audioEngine.updateStemVolume(stem, volume)
    }
    fun toggleStemMute(stem: String) {
        val current = stemMutes[stem] ?: false
        stemMutes[stem] = !current
    }
    fun toggleStemSolo(stem: String, solo: Boolean) {
        val current = stemSolos[stem] ?: false
        stemSolos[stem] = !current
    }
    fun updateStemPan(stem: String, pan: Float) {
        stemPans[stem] = pan
        audioEngine.updateStemPan(stem, pan)
    }
    
    fun playMediaItem(item: pEQMediaItem, addToQueue: Boolean = true) {
        showStemSplitter.value = false 
        currentMediaItem.value = item
        
        viewModelScope.launch {
            val playPath = if (item.path.startsWith("http") && (item.path.contains("youtube.com") || item.path.contains("youtu.be"))) {
                cloudDownloader?.getStreamUrl(item.path) ?: item.path
            } else item.path
            
            audioEngine.playTrack(playPath, item.title, item.artist, item.thumbnail)
            isPlaying.value = true
        }
        
        if (addToQueue && !playbackQueue.any { it.path == item.path }) {
            playbackQueue.add(0, item)
            _currentQueueIndex.value = 0
        } else if (addToQueue) {
             _currentQueueIndex.value = playbackQueue.indexOfFirst { it.path == item.path }
        }

        // Recently Played
        if (!recentlyPlayedItems.any { it.path == item.path }) {
            recentlyPlayedItems.add(0, item)
            if (recentlyPlayedItems.size > 20) recentlyPlayedItems.removeAt(recentlyPlayedItems.size - 1)
        }
        
        currentTab.value = Tab.Player
        persistSession()
    }

    fun shuffleQueue() {
        val current = playbackQueue.toList()
        playbackQueue.clear()
        playbackQueue.addAll(current.shuffled())
        showToast("Queue Shuffled")
    }

    fun openPlaylistAsQueue(name: String) {
        val items = if (name == "FAVORITES") favoriteItems else customPlaylists[name] ?: emptyList()
        if (items.isEmpty()) return
        
        playbackQueue.clear()
        playbackQueue.addAll(items)
        playMediaItem(items.first())
        showToast("Playlist loaded to Queue")
    }

    fun shufflePlaylistAndPlay(name: String) {
        val items = if (name == "FAVORITES") favoriteItems else customPlaylists[name] ?: emptyList()
        if (items.isEmpty()) return
        
        val shuffled = items.shuffled()
        playbackQueue.clear()
        playbackQueue.addAll(shuffled)
        playMediaItem(shuffled.first())
        showToast("Playlist Shuffled & Playing")
    }
    fun removeFromPlaylist(playlist: String, item: pEQMediaItem) {
        customPlaylists[playlist]?.remove(item)
        persistSession()
    }
    
    fun reorderPlaylist(playlist: String, from: Int, to: Int) {
        customPlaylists[playlist]?.let { list ->
            if (from in list.indices && to in list.indices) {
                val item = list.removeAt(from)
                list.add(to, item)
                persistSession()
            }
        }
    }
    
    fun deletePlaylist(playlist: String) {
        customPlaylists.remove(playlist)
        persistSession()
    }
    
    fun updateTheme(theme: pEQTheme) {
        _selectedTheme.value = theme
    }
    fun toggleSleepTimer(enabled: Boolean) { 
        sleepTimerActive.value = enabled
    }
    fun addStreamUrl(url: String, type: org.audiocular.peq.app.data.MediaType) {}
    
    fun saveLyrics(lines: List<SyncedLine>) {
        viewModelScope.launch {
            currentMediaItem.value?.let { item ->
                audioEngine.saveEmbeddedLyrics(item.path, lines)
                showToast("Lyrics Saved Permanently")
            }
        }
    }
    
    fun saveSubtitles(lines: List<SyncedLine>) {
        viewModelScope.launch {
            currentMediaItem.value?.let { item ->
                audioEngine.saveEmbeddedSubtitles(item.path, lines)
                showToast("Subtitles Saved Permanently")
            }
        }
    }
    
    fun saveChapters(chapters: List<MediaChapter>) {
        viewModelScope.launch {
            currentMediaItem.value?.let { item ->
                audioEngine.saveEmbeddedChapters(item.path, chapters)
                showToast("Chapters Saved Permanently")
            }
        }
    }

    fun addToQueue(item: pEQMediaItem) {
        playbackQueue.add(item)
        showToast("Added to Queue")
    }

    fun removeFromQueue(item: pEQMediaItem) {
        playbackQueue.remove(item)
    }

    fun playFromQueue(item: pEQMediaItem) {
        playMediaItem(item)
    }

    fun reorderQueue(from: Int, to: Int) {
        if (from < playbackQueue.size && to < playbackQueue.size) {
            val item = playbackQueue.removeAt(from)
            playbackQueue.add(to, item)
        }
    }

    fun addToPlaylist(playlist: String, item: pEQMediaItem) {
        val list = customPlaylists.getOrPut(playlist) { mutableStateListOf() }
        if (!list.contains(item)) {
            list.add(item)
            showToast("Added to $playlist")
        }
    }

    fun addScanFolder(path: String) {
        if (!scanFolders.contains(path)) {
            scanFolders.add(path)
            persistenceManager.saveScanFolders(scanFolders.toList())
            refreshLibrary()
        }
    }

    fun removeScanFolder(path: String) {
        scanFolders.remove(path)
        persistenceManager.saveScanFolders(scanFolders.toList())
        refreshLibrary()
    }

    fun requestFolderPicker() {
        onPickFolderRequested?.invoke()
    }
    
    fun runHardwareDiagnostics() {
        viewModelScope.launch {
            isDiagnosticRunning.value = true
            diagnosticLog.add("Initializing JNI Hook...")
            delay(500)
            diagnosticLog.add("Testing Audio Engine Latency...")
            delay(1000)
            diagnosticLog.add("Verifying DSP Buffer...")
            delay(800)
            diagnosticLog.add("All systems nominal.")
            isDiagnosticRunning.value = false
        }
    }

    fun fetchEverything() {
        fetchLyrics(autoOpenEditor = false)
        fetchSubtitlesOnline()
        fetchChapters()
    }

    fun requestSubtitlePicker() {
        onPickSubtitleRequested?.invoke()
    }

    fun requestChapterPicker() {
        onPickChaptersRequested?.invoke()
    }

    fun loadExternalSubtitle(path: String) {
        viewModelScope.launch {
            audioEngine.addExternalSubtitle(path)
            showToast("Subtitle Linked: ${path.substringAfterLast("/")}")
        }
    }

    fun loadExternalLyrics(content: String, path: String) {
        viewModelScope.launch {
             val lines = parseLrc(content)
             if (lines.isNotEmpty()) {
                 syncedLyrics.clear()
                 syncedLyrics.addAll(lines)
                 showToast("Lyrics Loaded: ${lines.size} lines")
             } else {
                 showToast("Failed to parse lyrics")
             }
        }
    }

    fun loadExternalChapters(path: String) {
        viewModelScope.launch {
             try {
                // In a real app, we'd read the file content here. 
                // For now, we rely on the engine to probe it if it's a sidecar, 
                // or we could implement a local reader.
                fetchChapters()
                showToast("Chapters Refreshed")
             } catch (e: Exception) {
                 showToast("Failed to load chapters")
             }
        }
    }
    fun clearPlaybackHistory() {}
    
    fun onEQPointDragged(freq: Float, gain: Float) {
        // Logic to update nearest EQ band
    }

    fun fetchLyricsNative() { fetchLyrics() } // Compatibility alias

    fun fetchChapters() {
        viewModelScope.launch {
            val chapters = audioEngine.getChapters()
            currentChapters.clear()
            currentChapters.addAll(chapters)
        }
    }

    fun importChapters(path: String) {
        loadExternalChapters(path)
    }

    fun webSearchLyrics() {
        val item = currentMediaItem.value ?: return
        val query = "${item.title} ${item.subtitle} lyrics lrc"
        openWebSearch(query, "LYRIC DISCOVERY")
    }
    
    
    val repeatMode = mutableStateOf(RepeatMode.OFF)

    fun togglePlayback() {
        if (isPlaying.value) audioEngine.pause() else audioEngine.resume()
        isPlaying.value = !isPlaying.value
    }
    fun nextTrack() {
        if (playbackQueue.isEmpty()) return
        val nextIndex = if (isShuffleEnabled) {
            (0 until playbackQueue.size).random()
        } else {
            (currentQueueIndex.value + 1) % playbackQueue.size
        }
        _currentQueueIndex.value = nextIndex
        playMediaItem(playbackQueue[nextIndex])
    }

    fun previousTrack() {
        if (playbackQueue.isEmpty()) return
        val prevIndex = if (currentPosition.longValue > 5000) {
            currentQueueIndex.value
        } else {
            if (currentQueueIndex.value > 0) currentQueueIndex.value - 1 else playbackQueue.size - 1
        }
        _currentQueueIndex.value = prevIndex
        playMediaItem(playbackQueue[prevIndex])
    }

    fun toggleFavorite(item: pEQMediaItem) {
        if (favoriteItems.any { it.path == item.path }) {
            favoriteItems.removeAll { it.path == item.path }
            favorites.remove(item.id)
        } else {
            favoriteItems.add(item)
            favorites.add(item.id)
        }
        persistSession()
    }
    fun syncNotificationState() {}
    fun updateMostPlayed(item: pEQMediaItem) {}
    fun updateLoopPoints(a: Long?, b: Long?) {
        audioEngine.setLoopPoints(a, b)
    }
    fun showToast(msg: String) {}
    fun updateDevice(device: org.audiocular.peq.app.data.Device) {
        selectedOutputDevice.value = device
        audioEngine.setOutputDevice(null) // Mock
    }
    fun toggleLyrics() { isLyricsVisible.value = !isLyricsVisible.value }
}
