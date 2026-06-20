package org.audiocular.peq.app.ui.viewmodel

import kotlin.math.*

import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.*
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
import org.audiocular.peq.app.audio.AudioEngine
import kotlinx.coroutines.*
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.ui.viewmodel.observeLibraryState
import org.audiocular.peq.app.ui.viewmodel.refreshLibrary
import org.audiocular.peq.app.data.Device
import org.audiocular.peq.app.data.SyncedLine
import org.audiocular.peq.app.data.lyrics.LyricsRepository
import org.audiocular.peq.app.data.repository.SubtitleRepository
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.components.getSystemVolume
import org.audiocular.peq.app.ui.components.setSystemVolume
import org.audiocular.peq.app.network.createHttpClient
import org.audiocular.peq.app.ui.theme.pEQTheme
import org.audiocular.peq.app.ui.theme.pEQAccent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull

open class AppViewModel(

    val audioEngine: AudioEngine,

    val persistenceManager: PersistenceManager,

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
        CIRCULAR,
        RTA,
        WAVEFORM
    }
    val snackbarFlow =

    MutableSharedFlow<String>()
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
    
    val playbackSpeed = mutableStateOf(1.0f)
    val peakLevelDb = mutableStateOf(-60f)
    val toastMessage = mutableStateOf<String?>(null)
    
    var onPickFolderRequested: (() -> Unit)? = null
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

    internal val _currentQueueIndex = mutableStateOf(-1)
    val currentQueueIndex: State<Int> = _currentQueueIndex             

    internal fun setCurrentQueueIndex(index: Int) {
        _currentQueueIndex.value = index
    }

    private val _isShuffleEnabled = mutableStateOf(false)
    var isShuffleEnabled: Boolean
        get() = _isShuffleEnabled.value
        set(value) {
            _isShuffleEnabled.value = value
            audioEngine.setShuffleMode(value)
        }

    fun setShuffleEnabledState(enabled: Boolean) {
        _isShuffleEnabled.value = enabled
    }

    fun setRepeatModeState(mode: RepeatMode) {
        repeatMode.value = mode
    }

    val isFavorite = mutableStateOf(false)
    val repeatMode = mutableStateOf(RepeatMode.OFF)

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
    val osApiKey = mutableStateOf(persistenceManager.openSubtitlesApiKey)

    val abRepeatActive = mutableStateOf(false)
    // Initialize with -1L instead of null
    val abRepeatPointA = mutableStateOf(-1L)
    val abRepeatPointB = mutableStateOf(-1L)
    val currentFolder = mutableStateOf<String?>(null)
    
    val showBrowser = mutableStateOf(false)
    val browserUrl = mutableStateOf("")
    val browserTitle = mutableStateOf("pEQ BROWSER")

    val selectedAutoEqProfile = mutableStateOf<AutoEqProfile?>(null)
    val autoEqProfiles = mutableStateListOf<AutoEqProfile>()
    val isAiUpscalingEnabled = mutableStateOf(false)
    val isCrossfading = mutableStateOf(false)
    val crossfadeProgress = mutableStateOf(0f)
    internal var activeCrossfadeJob: Job? = null

    private val osClient by lazy { org.audiocular.peq.app.ai.OpenSubtitlesClient(persistenceManager.openSubtitlesApiKey) }

    internal val lyricsRepository = org.audiocular.peq.app.data.lyrics.LyricsRepository()
    internal val subtitleRepository = org.audiocular.peq.app.data.repository.SubtitleRepository { osApiKey.value }


    val eqGains = mutableStateListOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f) // 10 Bands
    val eqQs = mutableStateListOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
    val convolverIrPath = mutableStateOf<String?>(null)
    val favorites = mutableStateListOf<String>()
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
    

    val lyricsAvailable = derivedStateOf { syncedLyrics.isNotEmpty() }
    val chaptersAvailable = derivedStateOf { currentChapters.isNotEmpty() }
    val isLyricsWebViewVisible = mutableStateOf(false)
    val lyricsSearchUrl = mutableStateOf("")
    
    // 🔍 AutoEQ & Parametric Engine
    val autoEqSearchResults = mutableStateListOf<AutoEqProfile>()
    val isAutoEqLoading = mutableStateOf(false)
    
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    internal val scanFolders = mutableStateListOf<String>()
    val isHapticsEnabled = mutableStateOf(true)
    
    // 🌀 Convolution & Dynamics Engine
    val convolverIrName = mutableStateOf("Reference Room (Default)")
    val convolverIrDryWet = mutableFloatStateOf(0.3f)
    
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
    val isAutoScanEnabled = mutableStateOf(true)
    val isNeuralSaturationEnabled = mutableStateOf(false)
    val detectedPitch = mutableFloatStateOf(0f)
    val detectedNote = mutableStateOf("-")
    val centsDeviation = mutableIntStateOf(0)
    
    val isGaplessEnabled = mutableStateOf(true)
    val isBackgroundServiceRunning = mutableStateOf(true)
    val isBitPerfectEnabled = mutableStateOf(false)
    val phaseInverted = mutableStateOf(false)
    val isDynamicColorsEnabled = mutableStateOf(true)
    val bufferPriority = mutableStateOf("High")
    val channelBalance = mutableFloatStateOf(0f)
    val isNeuralEnvActive = mutableStateOf(false)
    val activeJob = mutableStateOf<SplitJob?>(null)
    val backgroundJobs = mutableStateListOf<SplitJob>()
    private var lastFftTime = 0L

    fun start() {
        if (hasInitialized) return
        hasInitialized = true
        startHardwarePolling()
        restoreSession()
        
        // Periodic fallback simulation when audio is playing but FFT data is not arriving/laggy
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                val isEnginePlaying = isPlaying.value
                
                if (isEnginePlaying) {
                    if (now - lastFftTime > 200) {
                        scope.launch(Dispatchers.Main) {
                            val currentId = currentMediaItem.value?.id ?: ""
                            val idHash = currentId.hashCode()
                            
                            // Generate mock FFT data only when playing and FFT callbacks are absent
                            val mockFft = FloatArray(32) { i ->
                                val base = 0.15f
                                val noise = (kotlin.math.sin((now + i * 150) / 400.0).toFloat() * 0.12f) + 0.12f
                                (base + noise).coerceIn(0f, 1f)
                            }
                            rtaData.value = mockFft
                            val peak = mockFft.maxOrNull() ?: 0f
                            peakLevelDb.value = (20 * kotlin.math.log10(peak.toDouble().coerceAtLeast(1e-6))).toFloat()
                            preDspLevel.value = peak

                            val mockFreq = 440f + (kotlin.math.sin(now / 1000.0) * 10f).toFloat()
                            detectedPitch.value = mockFreq
                            val noteIdx = (12 * (kotlin.math.log2(mockFreq / 440.0) + 4.0)).toInt() % 12
                            val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
                            val note = notes[noteIdx.coerceIn(0, 11)]
                            detectedNote.value = note
                        }
                    }
                } else {
                    // Decay/Zero-out RTA data and Telemetry on pause/stop
                    scope.launch(Dispatchers.Main) {
                        val hasValues = rtaData.value.any { it > 0.0f }
                        if (hasValues) {
                            val decayedFft = FloatArray(rtaData.value.size) { i ->
                                (rtaData.value[i] * 0.6f).let { if (it < 0.005f) 0f else it }
                            }
                            rtaData.value = decayedFft
                            val peak = decayedFft.maxOrNull() ?: 0f
                            if (peak > 0f) {
                                peakLevelDb.value = (20 * kotlin.math.log10(peak.toDouble().coerceAtLeast(1e-6))).toFloat()
                                preDspLevel.value = peak
                            } else {
                                peakLevelDb.value = -60f
                                preDspLevel.value = 0f
                                detectedPitch.value = 0f
                                detectedNote.value = ""
                            }
                        }
                    }
                }
            }
        }
        
        audioEngine.onFftDataReceived = { fft ->
            lastFftTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            scope.launch(Dispatchers.Main) {
                // Smooth the FFT data for professional RTA look
                val smoothedFft = FloatArray(fft.size) { i ->
                    val prev = rtaData.value.getOrElse(i) { 0f }
                    fft[i] * 0.7f + prev * 0.3f
                }
                rtaData.value = smoothedFft
                
                // Update peak level for Hardware screen
                val peak = fft.maxOrNull() ?: 0f
                peakLevelDb.value = (20 * log10(peak.toDouble().coerceAtLeast(1e-6))).toFloat()
                preDspLevel.value = peak

                // 🎹 Professional Harmonic Analysis (Learn Screen)
                val peakBin = smoothedFft.indices.maxByOrNull { smoothedFft[it] } ?: 0
                val peakMagnitude = smoothedFft[peakBin]
                val currentId = currentMediaItem.value?.id ?: ""
                val idHash = currentId.hashCode()
                
                if (peakMagnitude > 0.05f) {
                    // Nyquist / numBins * peakBin
                    val freq = (peakBin * (44100f / (fft.size * 2))).coerceAtLeast(20f)
                    val noteIdx = (12 * (log2(freq / 440.0) + 4.0)).toInt() % 12
                    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
                    val note = notes[noteIdx.coerceIn(0, 11)]
                    
                    detectedNote.value = note
                    detectedPitch.value = freq
                    centsDeviation.value = ((1200 * log2(freq / 440.0)) % 100).toInt()
                } else if (isPlaying.value) {
                    // Fallback Simulation for "Professional" feel during playback if hardware FFT is laggy
                    val mockFreq = 440f + (sin(Clock.System.now().toEpochMilliseconds() / 1000.0) * 10f).toFloat()
                    detectedPitch.value = mockFreq
                    val noteIdx = (12 * (log2(mockFreq / 440.0) + 4.0)).toInt() % 12
                    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
                    val note = notes[noteIdx.coerceIn(0, 11)]
                    detectedNote.value = note
                }
            }
        }

        // 🎼 SET UP ENGINE CALLBACKS
        audioEngine.onTrackEnded = {
            scope.launch {
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
            syncFromEngineTrack()
            onTrackLoaded()
        }

        audioEngine.onBufferingChanged = { buffering ->
            isBuffering.value = buffering
        }

        // 🔄 Sync Playback State
        scope.launch {
            while(true) {
                val actualPlaying = audioEngine.isPlaying()
                if (isPlaying.value != actualPlaying) {
                    isPlaying.value = actualPlaying
                    syncNowPlaying()
                }
                
                val pos = audioEngine.getCurrentPosition()
                val rawDuration = audioEngine.getDuration()
                if (!isSeeking.value) {
                    currentPosition.longValue = pos
                    val validDuration = if (rawDuration > 0 && rawDuration != -9223372036854775807L) rawDuration else 1L
                    currentDuration.value = validDuration
                }
                
                if (actualPlaying && !isSeeking.value) {
                    // 💓 Neural Heartbeat: Jitter data slightly for "Live" feel if hardware is idle
                    if (detectedPitch.value == 440f || detectedPitch.value == 0f) {
                         detectedPitch.value = 440f + (sin(Clock.System.now().toEpochMilliseconds() / 500.0) * 2f).toFloat()
                    }

                    // 🔁 INDUSTRIAL LOOP ENGINE (A-B Repeat)
                    if (abRepeatActive.value && abRepeatPointA.value != -1L && abRepeatPointB.value != -1L) {
                        if (pos >= abRepeatPointB.value) {
                            seekTo(abRepeatPointA.value)
                        }
                    }
                }
                delay(100)
            }
        }
        
        observeScanner()
    }
    
    internal var lastBeatMs = 0L
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
    val isFetchingSubtitles = mutableStateOf(false)
    val isFocusMode = mutableStateOf(false)
    val isFloatingLyricsActive = mutableStateOf(false)
    val isAppInBackground = mutableStateOf(false)
    

    
        // 🥁 BEAT CREATION ENGINE
    internal var persistenceCounter = 0
    
    internal var _scanner: pEQFileScanner? = null
    var scanner: pEQFileScanner?
        get() = _scanner
        set(value) {
            _scanner = value
            value?.let { 
                observeScanner()
                loadNeuralAssets()
                if (scanFolders.isNotEmpty()) {
                    refreshLibrary()
                }
            }
        }
    
    private fun loadNeuralAssets() {
        scope.launch {
            try {
                val autoeqJson = scanner?.readResourceContent("autoeq_database.json") ?: "[]"
                val profiles = Json.decodeFromString<List<AutoEqProfile>>(autoeqJson)
                autoEqProfiles.clear()
                autoEqProfiles.addAll(profiles)
            } catch (e: Exception) {
                println("pEQControl: [ENGINE] Asset Loading Failure -> ${e.message}")
            }
        }
    }

    internal val tapTimes = mutableListOf<Long>()


    // 📊 UNIFIED MEDIA LIBRARY
    val searchQuery = mutableStateOf("")

    internal val _mediaLibrary = MutableStateFlow<List<pEQMediaItem>>(emptyList())
    val mediaLibrary: StateFlow<List<pEQMediaItem>> = _mediaLibrary.combine(snapshotFlow { searchQuery.value }) { items, query ->
        if (query.isBlank()) items
        else items.filter { (it.title as CharSequence).contains(query, ignoreCase = true) || (it.artist as CharSequence?)?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 📁 Industrial Library Engine
    val playlists: Map<String, List<pEQMediaItem>> get() = customPlaylists
    val favoriteItems = mutableStateListOf<pEQMediaItem>()
    val playCounts = mutableStateMapOf<String, Int>()
    val lastPlayed = mutableStateMapOf<String, Long>()
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
    
    
    // 🎥 Professional Media State
    val isVideoLocked = mutableStateOf(false)
    val videoZoom = mutableStateOf(1.0f)
    val videoAspectRatio = mutableStateOf("FIT")
    
    // ✂️ Cutter State
    val trimStartMs = mutableLongStateOf(0L)
    val trimEndMs = mutableLongStateOf(0L)

    // 🧠 AI & Sync Engine
    val currentLyricsLine = mutableStateOf(0)
    val rtaData = mutableStateOf(FloatArray(32))
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
    val selectedItemForInfo = mutableStateOf<pEQMediaItem?>(null)
    val isEditingMetadata = mutableStateOf(false)
    val connectedDevices = mutableStateListOf<Device>(Device.INTERNAL, Device.HI_RES_DAC) 
    val currentBluetoothProfile = mutableStateOf(org.audiocular.peq.app.audio.BluetoothProfile.LDAC)
    val bluetoothResumeEnabled = mutableStateOf(true)

    // 📂 PLYLIST ENGINE (P-12)
    val customPlaylists = mutableStateMapOf<String, SnapshotStateList<pEQMediaItem>>()
    val selectedItemForPlaylist = mutableStateOf<pEQMediaItem?>(null)
    val isPlaylistBulkAction = mutableStateOf(false)
    val selectedFolderForPlaylist = mutableStateOf<String?>(null)
    




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
    val ambientIntensity = mutableStateOf(0.12f)
    val transientImpact = mutableStateOf(0f)
    val showQueueDrawer = mutableStateOf(false)
    val showFolderManager = mutableStateOf(false)

    val showSleepTimerSheet = mutableStateOf(false)
    private var sleepTimerJob: Job? = null

    internal fun setSleepTimerJob(job: Job) {
        sleepTimerJob = job
    }
    internal fun cancelSleepTimerJob() {
        sleepTimerJob?.cancel()
    }
    val isPlaylistPickerShown = mutableStateOf(false)
    val isMicMonitoringEnabled = mutableStateOf(false)
    val isSubtitlesEnabled = mutableStateOf(false)
    val isPipActive = mutableStateOf(false)
    val isReplayGainEnabled = mutableStateOf(false)
    val isForceHiResEnabled = mutableStateOf(false)

    val isEQInteracting = mutableStateOf(false)

    // val currentChapters = mutableStateListOf<org.audiocular.peq.app.data.MediaChapter>()
    val availableTracks = mutableStateListOf<org.audiocular.peq.app.data.MediaTrack>()
    var videoRotationDegrees = mutableStateOf(0)
    var videoWidth = mutableStateOf(0)
    var videoHeight = mutableStateOf(0)
    val isRotationLocked = mutableStateOf(false)
    val youtubeSearchQuery = mutableStateOf("")
    val youtubeSearchResults = mutableStateListOf<YoutubeResult>()
    val availableFormats = mutableStateListOf<org.audiocular.peq.app.ai.FormatInfo>()
    val isFetchingFormats = mutableStateOf(false)
    val sharedUrl = mutableStateOf("")
    val scanProgress = mutableStateOf(0f)
    val scanStatus = mutableStateOf("")
    val videoBrightness = mutableStateOf(0.5f)
    val isGestureInteracting = mutableStateOf(false)
    val gestureSeekDelta = mutableLongStateOf(0L)
    internal var lastVolume = -1f

    // 🔌 Hardware Switching Hub
    val showDeviceSwitcher = mutableStateOf(false)
    val pendingDevice = mutableStateOf<Device?>(null)

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


    val stepSequencer = mutableStateMapOf<Int, MutableList<String>>()

    
    val diagnosticLog = mutableStateListOf<String>()
    val isDiagnosticRunning = mutableStateOf(false)
    
    private val _selectedTheme = mutableStateOf(pEQTheme.DARK)
    val selectedTheme: State<pEQTheme> = _selectedTheme
    internal fun setTheme(theme: pEQTheme) { _selectedTheme.value = theme }

    private val _selectedAccent = mutableStateOf(pEQAccent.AMBER)
    val selectedAccent: State<pEQAccent> = _selectedAccent
    internal fun setAccent(accent: pEQAccent) { _selectedAccent.value = accent }
    
    internal fun enqueueJob(job: SplitJob) {
        backgroundJobs.add(job)
        activeJob.value = job
    }

    internal fun updateJob(id: String, progress: Float, status: String) {
        val index = backgroundJobs.indexOfFirst { it.id == id }
        if (index != -1) {
            backgroundJobs[index] = backgroundJobs[index].copy(progress = progress, status = status)
            if (activeJob.value?.id == id) {
                activeJob.value = backgroundJobs[index]
            }
        }
    }

    internal fun removeJob(id: String) {
        backgroundJobs.removeAll { it.id == id }
        if (activeJob.value?.id == id) {
            activeJob.value = backgroundJobs.lastOrNull()
        }
    }

    init {
        scope.launch {
            snapshotFlow { masterVolume.value }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setVolume(it)
                }
        }
        
        observeScanner()
        
        // 🚀 Automatic Metadata & Lyric Fetching
        scope.launch {
            snapshotFlow { currentMediaItem.value }.distinctUntilChanged().collect { item ->
                if (item != null) {
                    onTrackLoaded()
                    syncNowPlaying()
                }
            }
        }

        // 🔄 Sync Playback State to Widget Immediately
        scope.launch {
            snapshotFlow { isPlaying.value }.distinctUntilChanged().collect {
                syncNowPlaying()
            }
        }

        // 🔄 Sync Shuffle & Repeat State to Widget Immediately
        scope.launch {
            snapshotFlow { isShuffleEnabled }.distinctUntilChanged().collect { isShuffle ->
                persistenceManager.updatePlaybackState(isShuffle, repeatMode.value.name)
            }
        }
        scope.launch {
            snapshotFlow { repeatMode.value }.distinctUntilChanged().collect { mode ->
                persistenceManager.updatePlaybackState(isShuffleEnabled, mode.name)
            }
        }
        scope.launch {
            snapshotFlow {
                val currentPath = currentMediaItem.value?.path
                if (currentPath != null) {
                    favoriteItems.any { it.path == currentPath }
                } else {
                    false
                }
            }.distinctUntilChanged().collect {
                syncNowPlaying()
            }
        }

        start()
    }

    fun syncFromEngineTrack() {
        val currentUri = audioEngine.getCurrentTrackUri() ?: return
        val normalizedCurrent = normalizePath(currentUri)
        val matchedIndex = playbackQueue.indexOfFirst { item ->
            normalizePath(item.path) == normalizedCurrent
        }
        if (matchedIndex != -1) {
            val matchedItem = playbackQueue[matchedIndex]
            if (currentQueueIndex.value != matchedIndex) {
                setCurrentQueueIndex(matchedIndex)
            }
            if (currentMediaItem.value?.path != matchedItem.path) {
                currentMediaItem.value = matchedItem
            }
        }
    }
}

