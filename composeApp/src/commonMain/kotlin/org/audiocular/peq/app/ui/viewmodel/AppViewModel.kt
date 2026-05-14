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

    private val osClient by lazy { org.audiocular.peq.app.ai.OpenSubtitlesClient(persistenceManager.openSubtitlesApiKey) }

    internal val lyricsRepository = org.audiocular.peq.app.data.lyrics.LyricsRepository()
    internal val subtitleRepository = org.audiocular.peq.app.data.repository.SubtitleRepository { osApiKey.value }


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
    

    val lyricsAvailable = derivedStateOf { syncedLyrics.isNotEmpty() }
    val chaptersAvailable = derivedStateOf { currentChapters.isNotEmpty() }
    val isLyricsWebViewVisible = mutableStateOf(false)
    val lyricsSearchUrl = mutableStateOf("")
    
    // 🔍 AutoEQ & Parametric Engine
    val autoEqSearchResults = mutableStateListOf<AutoEqProfile>()
    val selectedAutoEqProfile = mutableStateOf<AutoEqProfile?>(null)
    val isAutoEqLoading = mutableStateOf(false)
    
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    internal val scanFolders = mutableStateListOf<String>()
    internal val currentStep = mutableIntStateOf(0)
    internal val isSequencerActive = mutableStateOf(false)
    internal val drumTutorials = listOf(DrumTutorial("Basic Rock"), DrumTutorial("Trap Foundations"))
    internal val selectedTutorial = mutableStateOf<DrumTutorial?>(null)
    
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
    val detectedPitch = mutableFloatStateOf(440.0f)
    val detectedNote = mutableStateOf("A4")
    val centsDeviation = mutableIntStateOf(0)
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


    fun start() {
        startHardwarePolling()
        restoreSession()
        
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
            onTrackLoaded()
        }

        // 🔄 Sync Playback State
        scope.launch {
            while(true) {
                val actualPlaying = audioEngine.isPlaying()
                if (isPlaying.value != actualPlaying) {
                    isPlaying.value = actualPlaying
                }
                
                if (actualPlaying) {
                    currentPosition.longValue = audioEngine.getCurrentPosition()
                    currentDuration.value = audioEngine.getDuration().coerceAtLeast(1L)
                }
                delay(500)
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
    

    
    val drumKits = listOf(
        DrumKit("Neural 808", listOf(
            "808-CYMBAL", "808-CONGA", "808-MARACA", "808-CLAVE",
            "808-TOM-L", "808-TOM-M", "808-TOM-H", "808-COWBELL",
            "808-OPEN-HAT", "808-CLAP", "808-RIM", "808-CRASH",
            "808-KICK-1", "808-KICK-2", "808-SNARE-1", "808-CLOSED-HAT"
        )),
        DrumKit("Industrial Grime", listOf(
            "GRIME-STEAM", "GRIME-HAMMER", "GRIME-METAL", "GRIME-CLANG",
            "GRIME-PIPE", "GRIME-CRUSH", "GRIME-ENGINE", "GRIME-SPARK",
            "GRIME-GEAR", "GRIME-OIL", "GRIME-RUST", "GRIME-DRILL",
            "GRIME-KICK", "GRIME-SUB", "GRIME-SNARE", "GRIME-HAT"
        )),
        DrumKit("Lofi Nostalgia", listOf(
            "LOFI-DUST", "LOFI-WARMTH", "LOFI-MEMORIES", "LOFI-RAIN",
            "LOFI-BREEZE", "LOFI-ECHO", "LOFI-FADED", "LOFI-VINYL",
            "LOFI-RIM", "LOFI-CLAP", "LOFI-SOOTHE", "LOFI-GRAIN",
            "LOFI-KICK", "LOFI-BASS", "LOFI-SNARE", "LOFI-HAT"
        )),
        DrumKit("Analog Future", listOf(
            "Juno-Pad", "Warp-Hit", "Pulse-Lead", "Cyber-Perc",
            "Analog-Tom-L", "Analog-Tom-M", "Analog-Tom-H", "Digital-Noise",
            "Tight-Open-Hat", "Slapper-Clap", "Rim-Verbotron", "Tape-Crash",
            "Kick-Juno", "Kick-1985", "Snare-Analog", "Hats-Springwater"
        ))
    )
    
    // 🥁 BEAT CREATION ENGINE
    val isRecordingBeats = mutableStateOf(false)
    val isPlayingBeats = mutableStateOf(false)
    val drumPadLabels = mutableStateListOf<String>().apply { addAll(drumKits[0].labels) }
    private var beatStartTime = 0L
    internal var persistenceCounter = 0
    
    internal var _scanner: pEQFileScanner? = null
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

    internal fun setSleepTimerJob(job: Job) {
        sleepTimerJob = job
    }
    internal fun cancelSleepTimerJob() {
        sleepTimerJob?.cancel()
    }
    internal fun quantizeRecordedBeats() {}
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

    val isEQInteracting = mutableStateOf(false)

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
    
    val selectedDrumKit = mutableStateOf(drumKits[0])

    
    val guideStep = mutableStateOf(0)
    val sequencerTriggeredPads = mutableStateMapOf<String, Boolean>()
    private val _selectedTheme = mutableStateOf(pEQTheme.DARK)
    val selectedTheme: State<pEQTheme> = _selectedTheme
    internal fun setTheme(theme: pEQTheme) { _selectedTheme.value = theme }

    private val _selectedAccent = mutableStateOf(pEQAccent.AMBER)
    val selectedAccent: State<pEQAccent> = _selectedAccent
    internal fun setAccent(accent: pEQAccent) { _selectedAccent.value = accent }
    
    val sampleLibrary = mapOf(
        "KICKS" to listOf("Neural Kick 1", "Industrial Kick 2", "Lofi Thump"),
        "SNARES" to listOf("Neural Snare 1", "Grime Clap", "Retro Snap"),
        "HATS" to listOf("Neural Hat 1", "Industrial Shaker", "Lofi Tick")
    )

    fun addStreamUrl(url: String, type: org.audiocular.peq.app.data.MediaType) {}

    init {
        scope.launch {
            snapshotFlow { masterVolume.value }
                .distinctUntilChanged()
                .collect { 
                    audioEngine.setVolume(it)
                }
        }
        
        observeScanner()
        observeMetronome()
        
        // 🚀 Automatic Metadata & Lyric Fetching
        scope.launch {
            snapshotFlow { currentMediaItem.value }.collect { item ->
                if (item != null) {
                    onTrackLoaded()
                }
            }
        }

        start()
    }
}



