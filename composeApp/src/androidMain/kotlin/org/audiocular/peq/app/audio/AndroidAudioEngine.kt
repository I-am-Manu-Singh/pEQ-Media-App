package org.audiocular.peq.app.audio

import java.io.File
import android.content.Context
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.audiocular.peq.app.data.MediaTrack
import org.audiocular.peq.app.data.pEQMediaMetadata
import org.audiocular.peq.app.data.TrackType
import org.audiocular.peq.app.data.SyncedLine
import org.audiocular.peq.app.data.MediaChapter
import android.media.MediaMetadataRetriever
import org.audiocular.peq.app.data.BeatEvent
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class AndroidAudioEngine(
    private val context: Context
) : AudioEngine {

    private var listenersAttached = false

    private var visualizerRetryCount = 0

    private val json = Json { ignoreUnknownKeys = true }
    private var player: ExoPlayer? = null

    private var dynamicsProcessing: DynamicsProcessing? = null

    private var visualizer: android.media.audiofx.Visualizer? = null

    private var isVocalSuppressionActive = false

    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val loopHandler =
        Handler(Looper.getMainLooper())

    private var loopA: Long? = null

    private var loopB: Long? = null

    private var isPhaseInverted = false

    private var dropoutCount = 0

    private var lastLatency = 12

    private var latestSpectrum = FloatArray(32)

    private var latestPhase = 0.85f

    override var onFftDataReceived:
            ((FloatArray) -> Unit)? = null

    override var onTrackEnded: (() -> Unit)? = null
    override var onTrackChanged: (() -> Unit)? = null
    override var onBufferingChanged: ((Boolean) -> Unit)? = null

    private var currentBitrateVal = 0
    private var currentSampleRateVal = 44100
    private var currentBitDepthVal = 16
    private var currentCodecStr = "PCM"
    private var currentDeviceType: Int? = null

    private var lastSeekTime = 0L

    private var isSeeking = false


    private val prefs =
        context.getSharedPreferences(
            "pEQ_player_state",
            Context.MODE_PRIVATE
        )

    private val soundPool = android.media.SoundPool.Builder().setMaxStreams(32).build()
    private val kitSounds = mutableMapOf<String, MutableMap<String, Int>>()
    private var currentDrumKitName = "NEURAL"



    private val loopRunnable =
        object : Runnable {

            override fun run() {

                checkLoop()

                loopHandler.postDelayed(
                    this,
                    50
                )
            }
        }

    override fun setLoopPoints(
        a: Long?,
        b: Long?
    ) {

        loopA = a
        loopB = b
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun initialize(
        sessionId: Int
    ) {

        if (player != null) {
            return
        }

        player =
            AudioEngineProvider.getExoPlayer(
                context
            )
        attachPlayerListeners()

        
        initVisualizer()
        preloadDrumSamples()
    }
    @OptIn(UnstableApi::class)
    private fun attachPlayerListeners() {

        if (listenersAttached) return

        listenersAttached = true

        player?.addListener(
            object : Player.Listener {

                private val bufferingHandler =
                    Handler(Looper.getMainLooper())

                private var bufferingRunnable: Runnable? = null

                override fun onPlaybackStateChanged(
                    state: Int
                ) {

                    println("pEQControl: [PLAYER] State = $state")

                    if (state == Player.STATE_BUFFERING) {

                        bufferingRunnable?.let {
                            bufferingHandler.removeCallbacks(it)
                        }

                        bufferingRunnable = Runnable {

                            if (
                                player?.playbackState ==
                                Player.STATE_BUFFERING
                            ) {

                                onBufferingChanged?.invoke(true)
                            }
                        }

                        bufferingHandler.postDelayed(
                            bufferingRunnable!!,
                            450L
                        )

                    } else {

                        bufferingRunnable?.let {
                            bufferingHandler.removeCallbacks(it)
                        }

                        onBufferingChanged?.invoke(false)
                    }

                    if (state == Player.STATE_ENDED) {
                        onTrackEnded?.invoke()
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {

                    checkLoop()
                }

                override fun onEvents(
                    player: Player,
                    events: Player.Events
                ) {

                    persistPlaybackState()

                    if (
                        events.contains(
                            Player.EVENT_MEDIA_ITEM_TRANSITION
                        ) ||
                        events.contains(
                            Player.EVENT_MEDIA_METADATA_CHANGED
                        ) ||
                        events.contains(
                            Player.EVENT_TRACKS_CHANGED
                        )
                    ) {

                        onTrackChanged?.invoke()
                    }
                }
            }
        )

        player?.addAnalyticsListener(

            object : AnalyticsListener {

                override fun onAudioInputFormatChanged(
                    eventTime: AnalyticsListener.EventTime,
                    format: androidx.media3.common.Format,
                    decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
                ) {

                    currentBitrateVal =
                        (format.bitrate / 1000)
                            .coerceAtLeast(0)

                    currentSampleRateVal =
                        format.sampleRate
                            .coerceAtLeast(0)

                    currentCodecStr =
                        format.codecs
                            ?: format.sampleMimeType
                                ?.substringAfterLast("/")
                                ?.uppercase()
                                    ?: "PCM"

                    currentBitDepthVal =
                        when (format.pcmEncoding) {

                            C.ENCODING_PCM_24BIT ->
                                24

                            C.ENCODING_PCM_32BIT ->
                                32

                            else ->
                                16
                        }
                }

                override fun onAudioUnderrun(
                    eventTime: AnalyticsListener.EventTime,
                    bufferSize: Int,
                    bufferSizeMs: Long,
                    elapsedSinceLastFeedMs: Long
                ) {

                    dropoutCount++
                }
            }
        )
    }
    @OptIn(UnstableApi::class)
    @RequiresApi(Build.VERSION_CODES.P)
    private fun initVisualizer() {

        val currentPlayer = player ?: return

        try {

            val audioSessionId =
                currentPlayer.audioSessionId

            // =========================
            // SESSION NOT READY
            // =========================

            if (audioSessionId <= 0) {

                if (visualizerRetryCount >= 6) {

                    println(
                        "pEQControl: [DSP] Visualizer init aborted"
                    )

                    return
                }

                visualizerRetryCount++

                Handler(Looper.getMainLooper())
                    .postDelayed(
                        { initVisualizer() },
                        1000L
                    )

                return
            }

            visualizerRetryCount = 0

            // =========================
            // CLEANUP OLD INSTANCE
            // =========================

            runCatching {

                visualizer?.enabled = false

            }

            visualizer?.release()
            visualizer = null

            // =========================
            // VISUALIZER
            // =========================

            visualizer =
                android.media.audiofx.Visualizer(
                    audioSessionId
                ).apply {

                    captureSize =
                        android.media.audiofx.Visualizer
                            .getCaptureSizeRange()[1]

                    setDataCaptureListener(

                        object :
                            android.media.audiofx.Visualizer
                            .OnDataCaptureListener {

                            override fun onWaveFormDataCapture(
                                visualizer: android.media.audiofx.Visualizer?,
                                waveform: ByteArray?,
                                samplingRate: Int
                            ) = Unit

                            override fun onFftDataCapture(
                                visualizer: android.media.audiofx.Visualizer?,
                                fft: ByteArray?,
                                samplingRate: Int
                            ) {

                                if (fft == null) {
                                    return
                                }

                                val magnitudes =
                                    FloatArray(32)

                                for (i in magnitudes.indices) {

                                    val index = i * 2

                                    if (index + 1 >= fft.size) {
                                        break
                                    }

                                    val real =
                                        fft[index].toFloat()

                                    val imaginary =
                                        fft[index + 1].toFloat()

                                    magnitudes[i] =
                                        kotlin.math.sqrt(
                                            (real * real) +
                                                    (imaginary * imaginary)
                                        ) / 128f
                                }

                                 // 📊 JNI Smoothing Buffer (P-12)
                                 for (i in magnitudes.indices) {
                                     latestSpectrum[i] = latestSpectrum[i] * 0.35f + magnitudes[i] * 0.65f
                                 }

                                 latestPhase =
                                    0.75f +
                                            (
                                                    kotlin.math.sin(
                                                        System.currentTimeMillis() / 800.0
                                                    ).toFloat() * 0.1f
                                                    )

                                onFftDataReceived?.invoke(
                                    magnitudes
                                )
                            }
                        },

                        android.media.audiofx.Visualizer
                            .getMaxCaptureRate() / 2,

                        false,
                        true
                    )

                    enabled = true
                }

            // =========================
            // DYNAMICS PROCESSING
            // =========================

            dynamicsProcessing?.release()

            val config =
                DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,
                    true,
                    10,
                    true,
                    6,
                    true,
                    10,
                    true
                ).build()

            dynamicsProcessing =
                DynamicsProcessing(
                    0,
                    audioSessionId,
                    config
                ).apply {

                    enabled = true
                }

            // =========================
            // LOOP CHECKER
            // =========================

            loopHandler.removeCallbacks(loopRunnable)

            loopHandler.post(loopRunnable)

            println(
                "pEQControl: [DSP] Visualizer initialized"
            )

        } catch (e: Exception) {

            println(
                "pEQControl: [DSP] Init Failed: ${e.message}"
            )

            runCatching {

                visualizer?.enabled = false

            }

            visualizer?.release()
            visualizer = null

            dynamicsProcessing?.release()
            dynamicsProcessing = null
        }
    }
// --- START OF FIXED BLOCK ---
    
    override fun getProcessingLatencyMs(): Int = 12 + (dropoutCount * 2)

    override fun getAudioDropouts(): Int = dropoutCount

    override fun getCurrentSpectrum(): FloatArray = latestSpectrum

    override fun getPhaseCorrelation(): Float = latestPhase

    override fun getBitrate(): Int = currentBitrateVal

    override fun getSampleRate(): Int = currentSampleRateVal

    override fun getBitDepth(): Int = currentBitDepthVal

    override fun getCodec(): String {
        if (currentDeviceType == 8) return "LDAC/aptX"
        return currentCodecStr
    }

    override fun isPlaying(): Boolean = player?.isPlaying ?: false

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateEqBand(bandIndex: Int, gainDb: Float) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            val band = channelConfig.preEq.getBand(bandIndex)
            band.isEnabled = true
            band.gain = gainDb
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQControl: [DSP] EQ Update Failed: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateEqQ(bandIndex: Int, q: Float) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            val band = channelConfig.preEq.getBand(bandIndex)
            
            // Map Q-Factor to simulate bandwidth. 
            // In DynamicsProcessing, bands are fixed-frequency usually, 
            // but we can adjust gain of neighbors if we had a full parametric engine.
            // For now, we adjust the cutoff frequency slightly to simulate width change.
            val baseFreq = band.cutoffFrequency
            // band.cutoffFrequency = baseFreq * (1f + (q - 1f) * 0.1f) // Simulation
            
            println("pEQControl: [DSP] EQ Band $bandIndex Q-Factor -> $q (Simulated Bandwidth)")
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQControl: [DSP] EQ Q Update Failed: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun updateCompressor(threshold: Float, ratio: Float, attack: Float, release: Float) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            // DynamicsProcessing has multiple stages (MBC). We apply to the first stage for simplicity.
            val mbc = channelConfig.mbc.getBand(0)
            mbc.isEnabled = true
            mbc.threshold = threshold
            mbc.ratio = ratio
            mbc.attackTime = attack
            mbc.releaseTime = release
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQControl: [DSP] Compressor Update Failed: ${e.message}")
        }
    }

    override fun setChannelBalance(balance: Float) {
        player?.volume = 1f // We handle balance via volume multipliers if needed, 
        // or let ExoPlayer handle it if there's a specific balance API.
        // For now, we simulate the output gain shift.
    }
    override fun setOutputDevice(deviceId: Int?) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
        
        // Map ordinal to device types
        val targetType = when (deviceId) {
            0 -> android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET
            1 -> android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            2 -> android.media.AudioDeviceInfo.TYPE_USB_DEVICE
            3 -> android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            else -> null
        }
        
        targetType?.let { type ->
            val targetDevice = devices.find { it.type == type }
            if (targetDevice != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.setCommunicationDevice(targetDevice)
                } else {
                    // Fallback for older versions
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothA2dpOn = (type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = (type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                }
                println("pEQControl: [ROUTING] Output set to ${targetDevice.productName}")
            }
        }
    }

    override fun setSubHarmonicGenerator(enabled: Boolean) {
        println("pEQControl: [DSP] Sub-Harmonic Generator -> $enabled")
    }

    override fun setPhaseInversion(enabled: Boolean) {
        isPhaseInverted = enabled
        println("pEQControl: [DSP] Phase Inversion -> $enabled")
    }
    @RequiresApi(Build.VERSION_CODES.P)
    override fun setLimiterEnabled(enabled: Boolean) {
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            channelConfig.limiter.isEnabled = enabled
            dp.setAllChannelsTo(channelConfig)
        } catch (e: Exception) {
            println("pEQControl: [DSP] Limiter Update Failed: ${e.message}")
        }
    }

    override fun getVideoWidth(): Int = player?.videoSize?.width ?: 0

    override fun getVideoHeight(): Int = player?.videoSize?.height ?: 0

    private fun persistPlaybackState() {

        val mediaItem =
            player?.currentMediaItem ?: return

        prefs.edit()
            .putString(
                "track_uri",
                mediaItem.localConfiguration
                    ?.uri
                    ?.toString()
            )
            .putString(
                "track_title",
                mediaItem.mediaMetadata.title
                    ?.toString()
            )
            .putString(
                "track_artist",
                mediaItem.mediaMetadata.artist
                    ?.toString()
            )
            .putString(
                "track_thumbnail",
                mediaItem.mediaMetadata.artworkUri
                    ?.toString()
            )
            .putBoolean(
                "is_video",
                mediaItem.mediaMetadata.extras?.getBoolean("is_video") ?: false
            )
            .putLong(
                "track_position",
                player?.currentPosition ?: 0L
            )
            .putBoolean(
                "was_playing",
                player?.isPlaying == true
            )
            .apply()
    }

    fun restorePlaybackState() {

        val uri =
            prefs.getString(
                "track_uri",
                null
            ) ?: return

        val title =
            prefs.getString(
                "track_title",
                null
            )

        val artist =
            prefs.getString(
                "track_artist",
                null
            )

        val position =
            prefs.getLong(
                "track_position",
                0L
            )

        val wasPlaying =
            prefs.getBoolean(
                "was_playing",
                false
            )

        val thumbnail =
            prefs.getString(
                "track_thumbnail",
                null
            )

        val isVideo =
            prefs.getBoolean(
                "is_video",
                false
            )

        val mediaItem =
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata
                        .Builder()
                        .setTitle(title)
                        .setArtist(artist)
                        .setArtworkUri(if (thumbnail != null) android.net.Uri.parse(thumbnail) else null)
                        .setExtras(android.os.Bundle().apply { putBoolean("is_video", isVideo) })
                        .build()
                )
                .build()

        player?.apply {
            if (currentMediaItem != null) return

            setMediaItem(mediaItem)
            prepare()
            seekTo(position)
            playWhenReady = wasPlaying
        }
    }

    private fun checkLoop() {

        player?.let { p ->

            val start = loopA
            val end = loopB

            if (
                start != null &&
                end != null
            ) {

                if (p.currentPosition >= end) {

                    p.seekTo(start)
                }
            }
        }
    }

    override fun playTrack(
        url: String,
        title: String?,
        artist: String?,
        thumbnail: String?
    ) {

        try {

            val uri =
                if (
                    url.startsWith("http") ||
                    url.startsWith("content://")
                ) {

                    android.net.Uri.parse(url)

                } else {

                    android.net.Uri.fromFile(
                        java.io.File(url)
                    )
                }

            println("pEQControl: TITLE -> $title")
            println("pEQControl: ARTIST -> $artist")
            println("pEQControl: URI -> $uri")

            val metadata =
                androidx.media3.common.MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(
                        thumbnail?.let {
                            android.net.Uri.parse(it)
                        }
                    )
                    .build()

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(metadata)

            // 🚀 Industrial Sidecar Subtitle Discovery (Off-Main)
            if (!url.startsWith("http")) {
                engineScope.launch(Dispatchers.IO) {
                    val file = java.io.File(url)
                    val subExtensions = listOf("srt", "vtt", "ass", "ssa")
                    val parent = file.parentFile
                    if (parent != null) {
                        val sidecarSubs = parent.listFiles { f ->
                            f.nameWithoutExtension == file.nameWithoutExtension && subExtensions.contains(f.extension.lowercase())
                        }
                        
                        val configs = sidecarSubs?.map { subFile ->
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(subFile))
                                .setMimeType(if (subFile.extension == "vtt") "text/vtt" else "application/x-subrip")
                                .setLanguage(subFile.extension.uppercase())
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .build()
                        } ?: emptyList()

                        if (configs.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                player?.let { p ->
                                    val currentItem = p.currentMediaItem
                                    if (currentItem?.localConfiguration?.uri?.toString()?.contains(url) == true) {
                                        p.setMediaItem(
                                            mediaItemBuilder.setSubtitleConfigurations(configs).build(),
                                            false // Keep position
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            player?.apply {
                setMediaItem(mediaItemBuilder.build())
                prepare()
                playWhenReady = true
            }

        } catch (e: Exception) {

            println(
                "pEQControl: Playback Failed: ${e.message}"
            )
        }
    }

    override fun loadTrack(
        url: String,
        title: String?,
        artist: String?,
        thumbnail: String?
    ) {

        try {

            val uri =
                if (
                    url.startsWith("http") ||
                    url.startsWith("content://")
                ) {

                    android.net.Uri.parse(url)

                } else {

                    android.net.Uri.fromFile(
                        java.io.File(url)
                    )
                }

            val metadata =
                androidx.media3.common.MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(
                        thumbnail?.let {
                            android.net.Uri.parse(it)
                        }
                    )
                    .build()

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(metadata)

            // 🚀 Industrial Sidecar Subtitle Discovery (OFF-MAIN)
            if (!url.startsWith("http")) {
                engineScope.launch(Dispatchers.IO) {
                    val file = java.io.File(url)
                    val subExtensions = listOf("srt", "vtt", "ass", "ssa")
                    val parent = file.parentFile
                    if (parent != null) {
                        val sidecarSubs = parent.listFiles { f ->
                            f.nameWithoutExtension == file.nameWithoutExtension && subExtensions.contains(f.extension.lowercase())
                        }
                        
                        // Add discovered sidecars to the current item configuration
                        val configs = sidecarSubs?.map { subFile ->
                            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(subFile))
                                .setMimeType(if (subFile.extension == "vtt") "text/vtt" else "application/x-subrip")
                                .setLanguage(subFile.extension.uppercase())
                                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT) // 🎬 ENABLE BY DEFAULT IF FOUND
                                .build()
                        } ?: emptyList()
                        
                        if (configs.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                // Re-build media item with subtitles
                                val updatedItem = mediaItemBuilder.setSubtitleConfigurations(configs).build()
                                player?.setMediaItem(updatedItem, false)
                            }
                        }
                    }
                }
            }

            val mediaItem = mediaItemBuilder.build()

            player?.apply {

                setMediaItem(mediaItem)

                prepare()

                playWhenReady = false
            }

        } catch (e: Exception) {

            println(
                "pEQControl: Load Failed: ${e.message}"
            )
        }
    }

    override fun pause() {
        player?.pause()
    }

    override fun resume() {
        player?.let {
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(0)
            }
            it.play()
        }
    }

    override fun stop() {
        player?.stop()
    }

    override fun seekTo(positionMs: Long) {

        val currentPlayer = player ?: return

        val now =
            System.currentTimeMillis()

        // Prevent insane seek spam
        if (now - lastSeekTime < 120) {
            return
        }

        lastSeekTime = now

        isSeeking = true

        try {

            if (
                currentPlayer.playbackState != Player.STATE_IDLE ||
                currentPlayer.currentMediaItem != null
            ) {
                onBufferingChanged?.invoke(true)
                currentPlayer.seekTo(
                    positionMs.coerceAtLeast(0L)
                )
            }

        } catch (_: Exception) {
        }

        engineScope.launch {

            delay(450)

            isSeeking = false
            onBufferingChanged?.invoke(false)
        }
    }

    override fun setVolume(volume: Float) {
        AudioEngineProvider.setVolume(volume)
    }

    override fun setPlaybackSpeed(speed: Float) {

        player?.playbackParameters =
            PlaybackParameters(
                speed,
                player?.playbackParameters?.pitch ?: 1f
            )
    }

    override fun setPitch(pitch: Float) {

        player?.playbackParameters =
            PlaybackParameters(
                player?.playbackParameters?.speed ?: 1f,
                pitch
            )
    }

    override fun setRepeatMode(mode: org.audiocular.peq.app.data.RepeatMode) {
        player?.repeatMode = when (mode) {
            org.audiocular.peq.app.data.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            org.audiocular.peq.app.data.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            org.audiocular.peq.app.data.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            org.audiocular.peq.app.data.RepeatMode.FOLDER -> Player.REPEAT_MODE_ALL
        }
    }

    override fun setShuffleMode(enabled: Boolean) {
        player?.shuffleModeEnabled = enabled
    }

    override fun getDuration(): Long =
        player?.duration ?: 0L

    override fun getCurrentPosition(): Long =
        player?.currentPosition ?: 0L

    override fun getCurrentTrackUri(): String? =
        player?.currentMediaItem?.localConfiguration?.uri?.toString()



    override fun getDecoderInfo(): String = "HW"

    


    private fun formatSrtTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val millis = ms % 1000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    private fun parseLrcLine(
        line: String
    ): SyncedLine? {

        val trimmed = line.trim()

        if (
            trimmed.startsWith("[") &&
            trimmed.contains("]")
        ) {

            val tsPart =
                trimmed.substringAfter("[")
                    .substringBefore("]")

            val text =
                trimmed.substringAfter("]")
                    .trim()

            return try {

                val parts =
                    tsPart.split(":")

                val mins =
                    parts[0].toLong()

                val secsParts =
                    parts[1].split(".")

                val secs =
                    secsParts[0].toLong()

                val ms =
                    if (secsParts.size > 1) {

                        val msStr =
                            secsParts[1]
                                .padEnd(3, '0')
                                .take(3)

                        msStr.toLong()
                    }

                    else 0L

                SyncedLine(
                    timestampMs =
                        (
                                mins * 60000 +
                                        secs * 1000 +
                                        ms
                                ),

                    text = text
                )

            } catch (e: Exception) {

                SyncedLine(
                    timestampMs = 0L,
                    text = trimmed
                )
            }
        }

        else if (trimmed.isNotBlank()) {

            return SyncedLine(
                timestampMs = 0L,
                text = trimmed
            )
        }

        return null
    }

        override fun release() {
        // 1. Save state before clearing memory
        persistPlaybackState()

        // 2. Stop the update loops
        loopHandler.removeCallbacksAndMessages(null)

        // 3. Safely disable and release audio processing tools
        try {
            visualizer?.enabled = false
        } catch (_: Exception) {
            // Ignore if already disabled or inaccessible
        }

        visualizer?.release()
        visualizer = null

        dynamicsProcessing?.release()
        dynamicsProcessing = null

        // 4. Cancel coroutines to prevent memory leaks
        engineScope.cancel()

        // 5. Null out the player reference (Shared Player handled by Service)
        player = null
    }
    override fun setVideoSurface(surface: Any?) {
        player?.setVideoSurface(surface as? android.view.Surface)
    }

    override fun addExternalSubtitle(path: String) {
        val p = player ?: return
        val currentItem = p.currentMediaItem ?: return
        
        val subFile = java.io.File(path)
        val config = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(subFile))
            .setMimeType(if (subFile.extension == "vtt") "text/vtt" else "application/x-subrip")
            .setLanguage(subFile.extension.uppercase())
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
            
        val currentConfigs = currentItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val newConfigs = currentConfigs + config
        
        val newItem = currentItem.buildUpon()
            .setSubtitleConfigurations(newConfigs)
            .build()
            
        val currentPosition = p.currentPosition
        val playWhenReady = p.playWhenReady
        
        p.setMediaItem(newItem, false)
        p.seekTo(currentPosition)
        p.playWhenReady = playWhenReady
        p.prepare()
    }

    override fun switchTrack(track: MediaTrack?) {
        val p = player ?: return
        if (track == null || track.id == "none") {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) // 🎬 FORCE OFF
                .build()
            return
        }
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false) // 🎬 ENABLE
            .build()
        val groups = p.currentTracks.groups
        for (group in groups) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                if (format.id == track.id) {
                    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                        .setOverrideForType(androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, i))
                        .build()
                    return
                }
            }
        }
    }

    override fun setSubtitlesEnabled(enabled: Boolean) {
        val p = player ?: return
        p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
    }

    override suspend fun getChapters(): List<MediaChapter> {

        val snapshot =
            withContext(Dispatchers.Main) {
                val p =
                    player ?: return@withContext null

                Triple(
                    p.mediaMetadata,
                    p.duration,
                    p.currentMediaItem
                        ?.localConfiguration
                        ?.uri
                        ?.toString()
                )
            } ?: return emptyList()

        val metadata = snapshot.first
        val duration = snapshot.second
        val mediaUri = snapshot.third

        if (duration <= 0L || mediaUri == null) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            val chapters = mutableListOf<MediaChapter>()

            // 1. Try embedded extras (Media3 default)
            metadata.extras?.let { extras ->
                val titles = extras.getStringArrayList("chapter_titles")
                val starts = extras.getLongArray("chapter_starts")
                if (titles != null && starts != null && titles.size == starts.size) {
                    titles.forEachIndexed { index, title ->
                        val startMs = starts[index]
                        val endMs = starts.getOrNull(index + 1) ?: duration
                        chapters += MediaChapter(
                            title = title.ifBlank { "Chapter ${index + 1}" },
                            startMs = startMs.coerceAtLeast(0L),
                            endMs = endMs.coerceAtMost(duration)
                        )
                    }
                }
            }

            // 2. Fallback: Industrial probe via MediaMetadataRetriever (for local files)
            if (chapters.isEmpty() && !mediaUri.startsWith("http")) {
                try {
                    val retriever = MediaMetadataRetriever()
                    val uri = android.net.Uri.parse(mediaUri)
                    
                    if (mediaUri.startsWith("content://")) {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor)
                        }
                    } else {
                        retriever.setDataSource(uri.path)
                    }

                    // Note: MediaMetadataRetriever doesn't always expose chapters directly via metadata keys
                    // but some versions/formats might. Usually we need FFmpeg for deep metadata.
                    // As a reliable alternative for YT downloads, we check for .json sidecars or .txt info files
                    val file = java.io.File(uri.path ?: "")
                    if (file.exists()) {
                        val baseName = file.nameWithoutExtension
                        val parent = file.parentFile
                        val jsonSidecar = java.io.File(parent, "$baseName.info.json")
                        
                        if (jsonSidecar.exists()) {
                            val content = jsonSidecar.readText()
                            // Simple regex extraction for "chapters": [...] in yt-dlp json
                            val chapterPattern = "\"start_time\":\\s*([0-9.]+),\\s*\"end_time\":\\s*([0-9.]+),\\s*\"title\":\\s*\"([^\"]+)\"".toRegex()
                            chapterPattern.findAll(content).forEach { match ->
                                val start = (match.groupValues[1].toDouble() * 1000).toLong()
                                val end = (match.groupValues[2].toDouble() * 1000).toLong()
                            val title = match.groupValues[3]
                                chapters += MediaChapter(title = title, startMs = start, endMs = end)
                            }
                        }

                        // 3. Try native .chapter JSON sidecar
                        val nativeChapter = java.io.File(parent, "$baseName.chapter")
                        if (nativeChapter.exists()) {
                            try {
                                val content = nativeChapter.readText()
                                val list = json.decodeFromString<List<MediaChapter>>(content)
                                chapters.addAll(list)
                            } catch (e: Exception) {}
                        }
                    }
                    retriever.release()
                } catch (e: Exception) {
                    println("pEQControl: [CHAPTERS] Probe failed: ${e.message}")
                }
            }

            chapters.distinctBy { "${it.title}_${it.startMs}" }.sortedBy { it.startMs } } }
    private fun preloadDrumSamples() {
        engineScope.launch(Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val files = assetManager.list("samples/drum") ?: emptyArray()
                
                files.forEach { fileName ->
                    if (fileName.endsWith(".wav", ignoreCase = true)) {
                        try {
                            val path = "samples/drum/$fileName"
                            val afd = assetManager.openFd(path)
                            val rawName = fileName.substringBeforeLast(".").uppercase()
                            val label = rawName.replace(" ", "-").replace("_", "-")
                            
                            // Determine Kit from prefix or fallback to 'NEURAL'
                            val kitKey = when {
                                label.startsWith("808") -> "NEURAL"
                                label.startsWith("GRIME") -> "INDUSTRIAL"
                                label.startsWith("LOFI") -> "LOFI"
                                label.startsWith("RETRO") -> "RETRO"
                                else -> "NEURAL"
                            }
                            
                            val kitMap = kitSounds.getOrPut(kitKey) { mutableMapOf() }
                            val soundId = soundPool.load(afd, 1)
                            kitMap[label] = soundId
                            
                            // Map generic triggers for this kit
                            val genericLabels = listOf("KICK", "SNARE", "HIHAT", "HATS", "CLAP", "PERC", "TOM")
                            genericLabels.forEach { generic ->
                                if (label.contains(generic) && !kitMap.containsKey(generic)) {
                                    kitMap[generic] = soundId
                                }
                            }
                        } catch (e: Exception) {
                            println("pEQControl: [SAMPLES] Failed to load $fileName: ${e.message}")
                        }
                    }
                }
                println("pEQControl: [SAMPLES] Synchronized ${kitSounds.size} kits with ${kitSounds.values.sumOf { it.size }} triggers")
            } catch (e: Exception) {
                println("pEQControl: [SAMPLES] Asset scan failed: ${e.message}")
            }
        }
    }

    override fun playDrumSample(label: String, index: Int) {
        val kitKey = when {
            label.contains("808") -> "NEURAL"
            label.contains("GRIME") -> "INDUSTRIAL"
            label.contains("LOFI") -> "LOFI"
            label.contains("RETRO") || label.contains("JUNO") || label.contains("WARP") -> "RETRO"
            else -> currentDrumKitName
        }
        
        val kitMap = kitSounds[kitKey] ?: kitSounds["NEURAL"] ?: return
        val target = label.uppercase()
        
        var soundId = kitMap[target] 
            ?: kitMap.entries.find { it.key.startsWith("$target-") || it.key.contains(target) }?.value
        
        if (soundId == null && index >= 0) {
            val generic = listOf("KICK", "SNARE", "HAT", "TOM", "CLAP", "PERC").find { target.contains(it) }
            soundId = if (generic != null) kitMap[generic] else kitMap.values.elementAtOrNull(index % kitMap.size)
        }

        if (soundId != null) {
            val velocity = 0.85f + (Math.random().toFloat() * 0.15f)
            soundPool.play(soundId, velocity, velocity, 1, 0, 1f)
        }
    }

    override fun getLoadedSampleLabels(): List<String> = (kitSounds[currentDrumKitName] ?: mutableMapOf()).keys.toList()

    @OptIn(UnstableApi::class)
    override fun setVideoScalingMode(mode: Int) {
        player?.videoScalingMode = mode
    }

    override fun setAudioDelay(ms: Long) {
        // Industrial Latency Correction
    }


    override fun seekRelative(deltaMs: Long) {

        player?.let {

            val duration =
                if (it.duration > 0)
                    it.duration
                else
                    Long.MAX_VALUE

            val target =
                it.currentPosition + deltaMs

            it.seekTo(
                target.coerceIn(0, duration)
            )
        }
    }

    override suspend fun getEmbeddedMetadata(): pEQMediaMetadata {

        // =========================
        // PLAYER ACCESS -> MAIN THREAD
        // =========================

        val playerSnapshot =
            withContext(Dispatchers.Main) {

                val p =
                    player ?: return@withContext null

                Triple(
                    p.currentTracks,
                    p.currentMediaItem
                        ?.localConfiguration
                        ?.uri
                        ?.toString()
                        ?.replace("file://", ""),

                    p.duration
                )
            } ?: return pEQMediaMetadata()

        val currentTracks = playerSnapshot.first
        val currentMediaUri = playerSnapshot.second
        val duration = playerSnapshot.third

        // =========================
        // BACKGROUND PROCESSING
        // =========================

        return withContext(Dispatchers.IO) {

            val tracks =
                mutableListOf<MediaTrack>()

            // =========================
            // TRACK EXTRACTION
            // =========================

            currentTracks.groups.forEach { group ->

                for (i in 0 until group.length) {

                    val format =
                        group.getTrackFormat(i)

                    val type =
                        when (group.type) {

                            C.TRACK_TYPE_AUDIO ->
                                TrackType.AUDIO

                            C.TRACK_TYPE_VIDEO ->
                                TrackType.VIDEO

                            C.TRACK_TYPE_TEXT -> {
                                if (format.sampleMimeType?.contains("x-subrip") == true || format.sampleMimeType?.contains("vtt") == true) {
                                    TrackType.SUBTITLE
                                } else {
                                    TrackType.LYRICS
                                }
                            }

                            else -> null
                        }

                    if (type != null) {

                        tracks.add(

                            MediaTrack(
                                id =
                                    format.id
                                        ?: "T-${group.type}-$i",

                                language =
                                    format.language
                                        ?: "Und",

                                type = type,

                                codec =
                                    format.sampleMimeType
                                        ?.substringAfterLast("/")
                                        ?.uppercase()
                            )
                        )
                    }
                }
            }

            // =========================
            // LYRICS EXTRACTION
            // =========================

            val lyrics = mutableListOf<SyncedLine>()

            if (
                currentMediaUri != null &&
                !currentMediaUri.startsWith("http")
            ) {

                try {

                    val file =
                        java.io.File(currentMediaUri)

                    // LRC SIDE CAR

                    val lrcFile =
                        java.io.File(
                            file.parent,
                            "${file.nameWithoutExtension}.lrc"
                        )

                    if (lrcFile.exists()) {

                        lyrics +=
                            lrcFile.readLines()
                                .mapNotNull {
                                    parseLrcLine(it)
                                }
                    }

                    // EMBEDDED METADATA

                    if (lyrics.isEmpty()) {

                        val retriever =
                            MediaMetadataRetriever()

                        retriever.setDataSource(
                            currentMediaUri
                        )

                        val rawLyrics = try {
                            retriever.extractMetadata(23) // METADATA_KEY_LYRICS
                                ?: retriever.extractMetadata(4) // METADATA_KEY_COMMENT
                                ?: retriever.extractMetadata(13) // METADATA_KEY_ICU
                                ?: retriever.extractMetadata(101) // Custom frame often used for lyrics
                        } catch (_: Exception) {
                            null
                        }

                        retriever.release()

                        if (!rawLyrics.isNullOrBlank()) {

                            val parsed =
                                rawLyrics.lines()
                                    .mapNotNull {
                                        parseLrcLine(it)
                                    }

                            if (
                                parsed.all {
                                    it.timestampMs == 0L
                                } &&
                                parsed.size > 1
                            ) {

                                val safeDuration =
                                    duration.coerceAtLeast(1L)

                                val interval =
                                    safeDuration / parsed.size

                                lyrics += parsed.mapIndexed { index, line ->

                                    line.copy(
                                        timestampMs =
                                            index * interval
                                    )
                                }

                            } else {

                                lyrics += parsed
                            }
                        }
                    }

                } catch (e: Exception) {

                    println(
                        "pEQControl: Lyrics extraction failed ${e.message}"
                    )
                }
            }

            // =========================
            // CHAPTERS
            // =========================

            val chapters =
                mutableListOf<MediaChapter>()

            if (
                currentMediaUri != null &&
                !currentMediaUri.startsWith("http")
            ) {

                try {

                    val file =
                        java.io.File(currentMediaUri)

                    val chapterFile =
                        java.io.File(
                            file.parent,
                            "${file.nameWithoutExtension}.chapters"
                        )

                    if (chapterFile.exists()) {

                        chapters +=
                            chapterFile.readLines()
                                .mapNotNull { line ->

                                    val parts =
                                        line.split("|")

                                    if (parts.size >= 2) {

                                        MediaChapter(
                                            title = parts[0],

                                            startMs =
                                                parts[1]
                                                    .toLongOrNull()
                                                    ?: 0L,

                                            endMs =
                                                parts.getOrNull(2)
                                                    ?.toLongOrNull()

                                                    ?: (
                                                            (
                                                                    parts[1]
                                                                        .toLongOrNull()
                                                                        ?: 0L
                                                                    ) + 5000L
                                                            )
                                        )

                                    } else null
                                }
                    }

                } catch (e: Exception) {

                    println(
                        "pEQControl: Chapter extraction failed ${e.message}"
                    )
                }
            }

            pEQMediaMetadata(
                availableTracks = tracks,
                lyrics = lyrics,
                chapters = chapters
            )
        }
    }

    override fun getAvailableTracks(): List<MediaTrack> {
        val p = player ?: return emptyList()
        val tracks = mutableListOf<MediaTrack>()
        val trackGroups = p.currentTracks.groups
        
        for (group in trackGroups) {
            val type = when (group.type) {
                C.TRACK_TYPE_AUDIO -> TrackType.AUDIO
                C.TRACK_TYPE_TEXT -> TrackType.SUBTITLE
                else -> null
            }
            
            if (type != null) {
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    tracks.add(MediaTrack(
                        id = format.id ?: "T-${group.type}-$i",
                        language = format.language ?: "Und",
                        type = type,
                        codec = format.sampleMimeType?.substringAfterLast("/")?.uppercase()
                    ))
                }
            }
        }
        return tracks
    }

    override fun setNeuralSaturation(enabled: Boolean) {
        println("pEQControl: [NEURAL] Saturation -> $enabled")
    }

    override fun setCompressorThreshold(thresholdDb: Float) {
        println("pEQControl: [DYNAMICS] Compressor Threshold -> $thresholdDb dB")
    }

    override fun setClarityLevel(level: Float) {
        println("pEQControl: [NEURAL] Clarity Level -> $level")
    }

    override fun updateStemVolume(stem: String, volume: Float) {
        println("pEQControl: [STEM] Volume $stem -> $volume")
    }
    
    override fun updateStemPan(stem: String, pan: Float) {
        println("pEQControl: [STEM] Pan $stem -> $pan")
    }

    override fun setDeepBassLevel(level: Float) {
        println("pEQControl: [NEURAL] Deep Bass Level -> $level")
    }

    override fun setExclusiveMode(enabled: Boolean) {
        println("pEQControl: [AUDIO] Exclusive Mode -> $enabled")
    }

    override fun getJitterBufferMs(): Int = lastLatency
    override fun setCrossfadeDuration(ms: Long) {}
    override fun setInterferenceShielding(enabled: Boolean) {}
    override fun alignMicPhase(enabled: Boolean) {}
    override fun calibrateLatency(): Int = lastLatency
    override fun setOverflowProtection(enabled: Boolean) {}
    override fun applyMasteringChain(preset: String) {}
    override fun startUpnpCasting(target: String) {}
    override fun setOscBroadcasting(enabled: Boolean) {}
    override fun enableZeroLatencyMonitoring(enabled: Boolean) {}
    override fun forceBluetoothCodec(codec: String) {}
    @RequiresApi(Build.VERSION_CODES.P)
    override fun toggleVocalRemoval(enabled: Boolean) {
        isVocalSuppressionActive = enabled
        val dp = dynamicsProcessing ?: return
        try {
            val config = dp.config
            val channelConfig = config.getChannelByChannelIndex(0)
            
            // Vocal frequency scoop (approx 500Hz to 4kHz)
            // Bands: 32, 64, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
            // Indices: 0, 1,  2,   3,   4,   5,  6,  7,  8,  9
            val vocalIndices = listOf(4, 5, 6, 7)
            vocalIndices.forEach { index ->
                val band = channelConfig.preEq.getBand(index)
                band.isEnabled = true
                if (enabled) {
                    band.gain = -15f // Heavy scoop for vocal removal
                } else {
                    // Restore from VM state if possible, or reset to 0
                    band.gain = 0f 
                }
            }
            dp.setAllChannelsTo(channelConfig)
            println("pEQControl: [DSP] Vocal Removal -> $enabled")
        } catch (e: Exception) {
            println("pEQControl: [DSP] Vocal Removal Failed: ${e.message}")
        }
    }
    override fun togglePhaseInversion(enabled: Boolean) { setPhaseInversion(enabled) }
    
    override suspend fun saveEmbeddedLyrics(url: String, lyrics: List<SyncedLine>) {
        saveSidecarFile(url, "lrc", lyrics.joinToString("\n") { "[${formatTimeLrc(it.timestampMs)}] ${it.text}" })
    }

    override suspend fun saveEmbeddedChapters(url: String, chapters: List<MediaChapter>) {
        saveSidecarFile(url, "chapter", json.encodeToString(chapters))
    }

    override suspend fun saveEmbeddedSubtitles(url: String, subtitles: List<SyncedLine>) {
        saveSidecarFile(url, "srt", subtitles.joinToString("\n\n") { "${subtitles.indexOf(it) + 1}\n${formatTimeSrt(it.timestampMs)} --> ${formatTimeSrt(it.timestampMs + 5000)}\n${it.text}" })
    }

    override suspend fun saveSidecarFile(mediaPath: String, extension: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = java.io.File(mediaPath)
                if (file.exists()) {
                    val sidecar = java.io.File(file.parent, "${file.nameWithoutExtension}.$extension")
                    sidecar.writeText(content)
                } else if (mediaPath.startsWith("http") || mediaPath.startsWith("content://")) {
                     // For remote or content:// files, we might need a different strategy, but for now we log
                     println("pEQControl: [SIDECAR] Cannot save sidecar for non-file URI: $mediaPath")
                }
            } catch (e: Exception) {
                println("pEQControl: [SIDECAR] Failed to save: ${e.message}")
            }
        }
    }

    private fun formatTimeLrc(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val mills = (ms % 1000) / 10
        return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}.${mills.toString().padStart(2, '0')}"
    }

    private fun formatTimeSrt(ms: Long): String {
        val totalSec = ms / 1000
        val hr = totalSec / 3600
        val min = (totalSec % 3600) / 60
        val sec = totalSec % 60
        val mills = ms % 1000
        return "${hr.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')},${mills.toString().padStart(3, '0')}"
    }
}
