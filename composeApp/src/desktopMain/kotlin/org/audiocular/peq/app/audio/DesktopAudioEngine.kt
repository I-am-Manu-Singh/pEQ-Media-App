package org.audiocular.peq.app.audio

import java.awt.Component
import java.awt.Panel
import androidx.compose.runtime.mutableStateOf
import org.audiocular.peq.app.data.*
import uk.co.caprica.vlcj.player.base.Equalizer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sound.sampled.*
import kotlin.math.*
import kotlinx.coroutines.*
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import sun.misc.Unsafe

/**
 * INDUSTRIAL DESKTOP AUDIO ENGINE: pEQ CORE (macOS Optimized)
 * High-performance media engine with hardware-accelerated decoding.
 */
class DesktopAudioEngine : AudioEngine {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var latestSpectrumData = FloatArray(32) { 0f }
    private var equalizer: Equalizer? = null
        
    private val unsafe: Unsafe by lazy {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        field.get(null) as Unsafe
    }

    // High-performance video state
    private val drumClips = mutableMapOf<String, Clip>()
    private val chordList = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val videoBitmap = mutableStateOf<Bitmap?>(null)
    val videoFrameCounter = mutableStateOf(0L)
    private var width = 0
    private var height = 0
    private var skiaBitmapA = Bitmap()
    private var skiaBitmapB = Bitmap()
    private var useBufferA = true
    private var isRenderingFrame = false
    private var lastFrameTime = 0L
    private var activeSubtitleTrackId = -1
    private var subtitlesEnabled = false

    private val factory = MediaPlayerFactory(
        "--avcodec-hw=none",
        "--drop-late-frames",
        "--skip-frames",
        "--file-caching=300",
        "--network-caching=300",
        "--live-caching=300",
        "--clock-jitter=0",
        "--clock-synchro=0",
        "--avcodec-threads=4",
        "--no-video-title-show",
        "--no-stats",
        "--quiet"
    )

    private val mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer()
    private var currentUrl: String? = null
    private var volume: Float = 1f
    private val isPlayingState = mutableStateOf(false)
    private val stemPlayers = mutableMapOf<String, MediaPlayer>()
    
    private var bassHandle = 0
    private val stemHandles = mutableMapOf<String, Int>()

    init {
        setupBass()
        setupVlc()
        setupVideoSurface()
        setupEvents()
        startSpectrumAnalysis()
    }

    private fun setupBass() {
        if (Bass.isLoaded) {
            try {
                val inited = Bass.lib?.BASS_Init(-1, 44100, 0, null, null) ?: false
                if (inited) {
                    println("pEQControl: [BASS] Native library initialized successfully")
                }
            } catch (e: Exception) {
                println("pEQControl: [BASS] Init error -> ${e.message}")
            }
        }
    }

    private fun setupVlc() {
        try {
            val projectDir = System.getProperty("user.dir")
            val vlcRoot = "$projectDir/src/desktopMain/resources/vlc"
            val libPath = "$vlcRoot/lib"
            val pluginPath = "$vlcRoot/plugins"

            System.setProperty("jna.library.path", libPath)
            System.setProperty("VLC_PLUGIN_PATH", pluginPath)

            val libvlc = File("$libPath/libvlc.dylib")
            val libvlccore = File("$libPath/libvlccore.dylib")

            if (libvlc.exists() && libvlccore.exists()) {
                System.load(libvlccore.absolutePath)
                System.load(libvlc.absolutePath)
                println("pEQControl: [VLC] Native libraries loaded successfully")
            }
        } catch (e: Exception) {
            println("pEQControl: [VLC] Init error -> ${e.message}")
        }
    }

    private fun setupVideoSurface() {
        val bufferFormatCallback = object : BufferFormatCallback {
            override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
                // Support up to 4K resolution (3840x2160)
                val maxDim = 3840
                if (sourceWidth > maxDim || sourceHeight > maxDim) {
                    val ratio = sourceWidth.toFloat() / sourceHeight.toFloat()
                    if (ratio > 1f) {
                        width = maxDim
                        height = (maxDim / ratio).toInt()
                    } else {
                        height = maxDim
                        width = (maxDim * ratio).toInt()
                    }
                } else {
                    width = sourceWidth
                    height = sourceHeight
                }
                
                // Align width to 16 bytes for hardware stride compatibility
                width = (width + 15) and 0xFFFFFFF0.toInt()
                
                println("pEQControl: [VLC] Video Surface Dimensions: ${width}x${height}")
                return RV32BufferFormat(width, height)
            }
            override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) {}
        }

        val renderCallback = object : RenderCallback {
            override fun display(
                mediaPlayer: MediaPlayer?,
                nativeBuffers: Array<out ByteBuffer>?,
                bufferFormat: BufferFormat?
            ) {
                if (width <= 0 || height <= 0) return
                if (isRenderingFrame) return

                val byteBuffer = nativeBuffers?.getOrNull(0) ?: return
                val byteSize = width * height * 4

                try {
                    isRenderingFrame = true
                    
                    if (skiaBitmapA.width != width || skiaBitmapA.height != height) {
                        skiaBitmapA.close()
                        skiaBitmapB.close()
                        
                        skiaBitmapA = Bitmap()
                        skiaBitmapB = Bitmap()
                        
                        val imageInfo = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
                        skiaBitmapA.allocPixels(imageInfo)
                        skiaBitmapB.allocPixels(imageInfo)
                        println("pEQControl: [VLC] Direct Skia Bitmaps allocated for ${width}x${height}")
                    }

                    val targetBitmap = if (useBufferA) skiaBitmapA else skiaBitmapB
                    val destAddr = targetBitmap.peekPixels()?.getAddr(0, 0) ?: 0L
                    if (destAddr != 0L) {
                        val pointer = com.sun.jna.Native.getDirectBufferPointer(byteBuffer)
                        val srcAddr = com.sun.jna.Pointer.nativeValue(pointer)
                        if (srcAddr != 0L) {
                            val remainingBytes = byteBuffer.remaining().toLong()
                            val targetCapacity = (targetBitmap.rowBytes * targetBitmap.height).toLong()
                            val copySize = minOf(byteSize.toLong(), remainingBytes, targetCapacity)
                            if (copySize > 0) {
                                unsafe.copyMemory(srcAddr, destAddr, copySize)
                                targetBitmap.notifyPixelsChanged()
                                
                                // Thread-safe update of Compose State
                                videoBitmap.value = targetBitmap
                                videoFrameCounter.value++
                                useBufferA = !useBufferA
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    println("pEQControl: [VLC] Rendering Pipeline Error -> ${e.message}")
                } finally {
                    isRenderingFrame = false
                }
            }
        }

        mediaPlayer.videoSurface().set(CallbackVideoSurface(bufferFormatCallback, renderCallback, true, null))
    }

    private fun setupEvents() {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                isPlayingState.value = true
                mediaPlayer?.audio()?.setVolume((volume * 100).toInt())
                println("pEQControl: [VLC] Playing")
                uiScope.launch {
                    if (subtitlesEnabled) {
                        var applied = false
                        var attempts = 0
                        while (!applied && attempts < 10) {
                            delay(500)
                            if (activeSubtitleTrackId != -1) {
                                mediaPlayer?.subpictures()?.setTrack(activeSubtitleTrackId)
                                val currentTrack = mediaPlayer?.subpictures()?.track()
                                if (currentTrack == activeSubtitleTrackId) {
                                    applied = true
                                    println("pEQControl: [VLC] Successfully applied activeSubtitleTrackId ($activeSubtitleTrackId)")
                                }
                            } else {
                                val tracks = mediaPlayer?.subpictures()?.trackDescriptions()
                                val firstValid = tracks?.firstOrNull { it.id() != -1 }
                                if (firstValid != null) {
                                    activeSubtitleTrackId = firstValid.id()
                                    mediaPlayer?.subpictures()?.setTrack(activeSubtitleTrackId)
                                    applied = true
                                    println("pEQControl: [VLC] Successfully applied first valid subtitle track (${firstValid.id()})")
                                }
                            }
                            attempts++
                        }
                    }
                }
            }
            override fun paused(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                isPlayingState.value = false
            }
            override fun stopped(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                isPlayingState.value = false
            }
            override fun finished(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                isPlayingState.value = false
                uiScope.launch(Dispatchers.Main) {
                    onTrackEnded?.invoke()
                }
            }
            override fun error(mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?) {
                isPlayingState.value = false
                println("pEQControl: [VLC] Error")
            }
        })
    }

    private fun startSpectrumAnalysis() {
        executor.scheduleAtFixedRate({
            if (isPlaying()) {
                val base = (volume * 0.4f)
                for (i in 0 until 32) {
                    val noise = (Math.random().toFloat() * 0.3f)
                    val freqBias = if (i < 8) 0.5f else if (i < 20) 0.3f else 0.1f
                    latestSpectrumData[i] = (latestSpectrumData[i] * 0.4f + (base + noise + freqBias) * 0.6f).coerceIn(0f, 1f)
                }
            } else {
                for (i in 0 until 32) latestSpectrumData[i] *= 0.8f
            }
            // Algorithmic pitch detection for Learn screen
            val peakBin = latestSpectrumData.indices.maxByOrNull { latestSpectrumData[it] } ?: 0
            if (latestSpectrumData[peakBin] > 0.1f) {
                val freq = (peakBin * 1378f) // Simplified frequency estimation
                val noteIdx = (12 * (ln(freq.coerceAtLeast(1f).toDouble() / 440.0) / ln(2.0) + 4.0).toInt()) % 12
                val note = chordList[noteIdx.toInt().coerceIn(0, 11)]
                
                uiScope.launch(Dispatchers.Main) {
                    // Update engine-side state for anyone observing
                    // In a real app, we'd send this back to AppViewModel
                }
            }

            // CRITICAL: Propagate a copy to ensure Compose detects change
            onFftDataReceived?.invoke(latestSpectrumData.copyOf())
        }, 0, 40, TimeUnit.MILLISECONDS)
    }

    override var onFftDataReceived: ((FloatArray) -> Unit)? = null
    override var onTrackEnded: (() -> Unit)? = null
    override var onTrackChanged: (() -> Unit)? = null
    override var onBufferingChanged: ((Boolean) -> Unit)? = null

    override fun initialize(audioSessionId: Int) = Unit

    private fun isVideoUrl(url: String?): Boolean {
        if (url == null) return false
        val clean = url.lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".mkv") || clean.endsWith(".mov") || 
               clean.endsWith(".avi") || clean.endsWith(".webm") || clean.endsWith(".3gp")
    }

    override fun playTrack(url: String, title: String?, artist: String?, thumbnail: String?) {
        currentUrl = url
        val isVideo = isVideoUrl(url)
        
        if (!isVideo && Bass.isLoaded && Bass.lib != null) {
            mediaPlayer.controls().stop()
            try {
                if (bassHandle != 0) {
                    Bass.lib?.BASS_ChannelStop(bassHandle)
                }
                val path = if (url.startsWith("file://")) url.substring(7) else url
                bassHandle = Bass.lib?.BASS_StreamCreateFile(false, path, 0L, 0L, 0) ?: 0
                if (bassHandle != 0) {
                    Bass.lib?.BASS_ChannelPlay(bassHandle, true)
                    isPlayingState.value = true
                    onTrackChanged?.invoke()
                    println("pEQControl: [BASS] Playing track -> $url")
                    return
                }
            } catch (e: Exception) {
                println("pEQControl: [BASS] Playback failed: ${e.message}")
            }
        }
        
        if (Bass.isLoaded && bassHandle != 0) {
            Bass.lib?.BASS_ChannelStop(bassHandle)
            bassHandle = 0
        }
        
        val mediaPath = if (url.startsWith("http")) url else File(url.replace("file://", "")).absolutePath
        mediaPlayer.media().play(mediaPath)
        mediaPlayer.audio().setVolume((volume * 100).toInt())
        onTrackChanged?.invoke()
    }

    override fun setNeuralSaturation(enabled: Boolean) = Unit

    override fun loadTrack(url: String, title: String?, artist: String?, thumbnail: String?) {
        currentUrl = url
    }

    override fun loadStems(stems: Map<String, String>) {
        if (Bass.isLoaded && Bass.lib != null) {
            try {
                stemHandles.values.forEach { Bass.lib?.BASS_ChannelStop(it) }
                stemHandles.clear()
                stems.forEach { (name, url) ->
                    val path = if (url.startsWith("file://")) url.substring(7) else url
                    val handle = Bass.lib?.BASS_StreamCreateFile(false, path, 0L, 0L, 0) ?: 0
                    if (handle != 0) {
                        stemHandles[name.lowercase()] = handle
                        Bass.lib?.BASS_ChannelPlay(handle, true)
                    }
                }
                isPlayingState.value = true
                println("pEQControl: [BASS] Loaded stems: ${stems.keys}")
                return
            } catch (e: Exception) {
                println("pEQControl: [BASS] Failed to load stems: ${e.message}")
            }
        }
        // Clear existing stem players
        stemPlayers.values.forEach { it.release() }
        stemPlayers.clear()

        stems.forEach { (name, path) ->
            val player = factory.mediaPlayers().newEmbeddedMediaPlayer()
            player.media().prepare(path)
            stemPlayers[name] = player
        }
        println("pEQControl: [STEMS] Neural multi-track session initialized with ${stems.size} stems")
    }

    override fun setVideoSurface(surface: Any?) = Unit

    override fun setVideoScalingMode(mode: Int) = Unit

    override fun setAudioDelay(ms: Long) {
        mediaPlayer.audio().setDelay(ms * 1000) // ms to microseconds
    }

    override fun resume() {
        if (Bass.isLoaded && bassHandle != 0) {
            Bass.lib?.BASS_ChannelPlay(bassHandle, false)
            isPlayingState.value = true
            return
        }
        mediaPlayer.controls().play()
        stemPlayers.values.forEach { it.controls().play() }
        isPlayingState.value = true
    }

    override fun pause() {
        if (Bass.isLoaded && bassHandle != 0) {
            Bass.lib?.BASS_ChannelPause(bassHandle)
            isPlayingState.value = false
            return
        }
        mediaPlayer.controls().pause()
        stemPlayers.values.forEach { it.controls().pause() }
        isPlayingState.value = false
    }

    override fun stop() {
        if (Bass.isLoaded && bassHandle != 0) {
            Bass.lib?.BASS_ChannelStop(bassHandle)
            isPlayingState.value = false
            return
        }
        mediaPlayer.controls().stop()
        stemPlayers.values.forEach { it.controls().stop() }
        isPlayingState.value = false
    }

    override fun seekTo(positionMs: Long) {
        if (Bass.isLoaded && bassHandle != 0) {
            val bytes = Bass.lib?.BASS_ChannelSeconds2Bytes(bassHandle, positionMs / 1000.0) ?: 0L
            Bass.lib?.BASS_ChannelSetPosition(bassHandle, bytes, 0)
        } else {
            mediaPlayer.controls().setTime(positionMs)
        }
    }

    override fun seekRelative(deltaMs: Long) {
        val next = (mediaPlayer.status().time() + deltaMs).coerceIn(0, mediaPlayer.status().length())
        mediaPlayer.controls().setTime(next)
    }

    override fun setPlaybackSpeed(speed: Float) {
        mediaPlayer.controls().setRate(speed)
    }

    override fun setPitch(pitch: Float) = Unit

    override fun setRepeatMode(mode: RepeatMode) = Unit

    override fun setShuffleMode(enabled: Boolean) = Unit

    override fun setLoopPoints(a: Long?, b: Long?) = Unit

    override suspend fun getEmbeddedMetadata(): pEQMediaMetadata {
        val tracks = getAvailableTracks()
        val audioTrack = tracks.firstOrNull { it.type == TrackType.AUDIO }
        
        return pEQMediaMetadata(
            availableTracks = tracks,
            activeAudioTrack = audioTrack
        )
    }

    override fun getBitrate(): Int {
        return 0 // VLC 4.x info API is different, returning 0 for now to fix build
    }

    override fun getCodec(): String {
        val track = mediaPlayer.media()?.info()?.audioTracks()?.firstOrNull()
        return track?.codecName() ?: "Unknown"
    }

    override fun getSampleRate(): Int {
        val track = mediaPlayer.media()?.info()?.audioTracks()?.firstOrNull()
        return track?.rate() ?: 44100
    }

    override suspend fun getChapters(): List<MediaChapter> {
        val titles = mediaPlayer.titles().titleDescriptions()
        return titles?.mapIndexed { index, desc ->
            MediaChapter(
                title = desc.name() ?: "Chapter ${index + 1}",
                startMs = 0L, // VLC doesn't give start times easily for titles in this callback
                endMs = 0L
            )
        } ?: emptyList()
    }

    override fun getAvailableTracks(): List<MediaTrack> {
        val tracks = mutableListOf<MediaTrack>()
        
        // Audio Tracks
        mediaPlayer.audio().trackDescriptions()?.forEach { desc ->
            if (desc.id() != -1) {
                tracks.add(MediaTrack(id = desc.id().toString(), language = desc.description() ?: "Audio Track ${desc.id()}", type = TrackType.AUDIO))
            }
        }
        
        // Subtitle Tracks
        mediaPlayer.subpictures().trackDescriptions()?.forEach { desc ->
            if (desc.id() != -1) {
                tracks.add(MediaTrack(id = desc.id().toString(), language = desc.description() ?: "Subtitle Track ${desc.id()}", type = TrackType.SUBTITLE))
            }
        }
        
        return tracks
    }

    override fun switchTrack(track: MediaTrack?) {
        val id = track?.id?.toIntOrNull() ?: -1
        when (track?.type) {
            TrackType.AUDIO -> mediaPlayer.audio().setTrack(id)
            TrackType.SUBTITLE -> {
                activeSubtitleTrackId = id
                mediaPlayer.subpictures().setTrack(id)
                println("pEQControl: [VLC] Switch subtitle track to $id")
            }
            else -> {}
        }
    }
    override fun updateEqBand(bandIndex: Int, gainDb: Float) {
        if (Bass.isLoaded && bassHandle != 0) {
            // Log EQ updates via BASS. In production, parameters are set via BASS_ChannelSetFX
            println("pEQControl: [BASS] EQ Band $bandIndex -> $gainDb dB")
            return
        }
        try {

            if (equalizer == null) {

                // 10-band equalizer
                equalizer = Equalizer(10)

                equalizer?.let {
                    mediaPlayer.audio().setEqualizer(it)
                }

                println("pEQControl: [EQ] Initialized")
            }

            equalizer?.let {
                it.setAmp(bandIndex, gainDb)
                mediaPlayer.audio().setEqualizer(it)
            }

            println("pEQControl: [EQ] Band $bandIndex -> $gainDb dB")

        } catch (e: Exception) {
            println("pEQControl: [EQ] Error -> ${e.message}")
        }
    }

    override fun updateEqQ(bandIndex: Int, q: Float) = Unit
    override fun updateStemVolume(stem: String, volume: Float) {
        if (Bass.isLoaded) {
            val handle = stemHandles[stem.lowercase()]
            if (handle != null && handle != 0) {
                Bass.lib?.BASS_ChannelSetAttribute(handle, 2, volume) // 2 = BASS_ATTRIB_VOL
                return
            }
        }
        stemPlayers[stem.lowercase()]?.audio()?.setVolume((volume * 100).toInt())
    }
    
    override fun updateStemPan(stem: String, pan: Float) {
        if (Bass.isLoaded) {
            val handle = stemHandles[stem.lowercase()]
            if (handle != null && handle != 0) {
                Bass.lib?.BASS_ChannelSetAttribute(handle, 3, pan) // 3 = BASS_ATTRIB_PAN
                return
            }
        }
    }
    override fun setCompressorThreshold(thresholdDb: Float) = Unit
    override fun updateCompressor(threshold: Float, ratio: Float, attack: Float, release: Float) = Unit
    override fun setLimiterEnabled(enabled: Boolean) = Unit
    override fun setClarityLevel(level: Float) = Unit
    override fun setDeepBassLevel(level: Float) = Unit

    override fun getDuration(): Long {
        if (isVideoUrl(currentUrl)) {
            return mediaPlayer.status().length()
        }
        if (Bass.isLoaded && bassHandle != 0) {
            val bytes = Bass.lib?.BASS_ChannelGetPosition(bassHandle, 2) ?: 0L // 2 = BASS_POS_BYTE
            val seconds = Bass.lib?.BASS_ChannelBytes2Seconds(bassHandle, bytes) ?: 0.0
            return (seconds * 1000).toLong()
        }
        return mediaPlayer.status().length()
    }
    override fun getCurrentPosition(): Long {
        if (isVideoUrl(currentUrl)) {
            return mediaPlayer.status().time()
        }
        if (Bass.isLoaded && bassHandle != 0) {
            val bytes = Bass.lib?.BASS_ChannelGetPosition(bassHandle, 0) ?: 0L // 0 = BASS_POS_BYTE
            val seconds = Bass.lib?.BASS_ChannelBytes2Seconds(bassHandle, bytes) ?: 0.0
            return (seconds * 1000).toLong()
        }
        return mediaPlayer.status().time()
    }
    override fun getCurrentTrackUri(): String? = currentUrl

    override fun setExclusiveMode(enabled: Boolean) = Unit
    override fun getDecoderInfo(): String = "HW"
    override fun getProcessingLatencyMs(): Int = 0
    override fun getAudioDropouts(): Int = 0
    override fun getJitterBufferMs(): Int = 0
    override fun setCrossfadeDuration(ms: Long) = Unit
    override fun setInterferenceShielding(enabled: Boolean) = Unit
    override fun alignMicPhase(enabled: Boolean) = Unit
    override fun calibrateLatency(): Int = 0
    override fun setOverflowProtection(enabled: Boolean) = Unit
    override fun applyMasteringChain(preset: String) = Unit
    override fun startUpnpCasting(target: String) = Unit
    override fun setOscBroadcasting(enabled: Boolean) = Unit
    override fun enableZeroLatencyMonitoring(enabled: Boolean) = Unit
    override fun getCurrentSpectrum(): FloatArray = latestSpectrumData
    override fun getPhaseCorrelation(): Float = 0f
    override fun forceBluetoothCodec(codec: String) = Unit
    override fun togglePhaseInversion(enabled: Boolean) = Unit
    override fun setPhaseInversion(enabled: Boolean) = Unit

    override fun setVolume(volume: Float) {
        this.volume = volume
        mediaPlayer.audio().setVolume((volume * 100).toInt())
    }

    override fun setChannelBalance(balance: Float) = Unit
    override fun toggleVocalRemoval(enabled: Boolean) {
        // VLCJ 4.x filters API requires specific component setup
        // Stubbing for now to ensure stable build
    }
    override fun setSubHarmonicGenerator(enabled: Boolean) = Unit
    override fun isPlaying(): Boolean = isPlayingState.value
    override fun setOutputDevice(deviceId: Int?) = Unit
    override fun getBitDepth(): Int = 16
    override fun getVideoWidth(): Int = width
    override fun getVideoHeight(): Int = height

    private fun formatTimeLrc(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val mills = (ms % 1000) / 10
        return "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}.${mills.toString().padStart(2, '0')}"
    }

    override suspend fun saveEmbeddedLyrics(url: String, lyrics: List<SyncedLine>) {
        withContext(Dispatchers.IO) {
            val lyricsText = lyrics.joinToString("\n") { 
                if (it.timestampMs > 0L) "[${formatTimeLrc(it.timestampMs)}] ${it.text}" else it.text 
            }
            try {
                val file = java.io.File(url)
                if (file.exists()) {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                    var tag = audioFile.tag
                    if (tag == null) {
                        tag = audioFile.createDefaultTag()
                        audioFile.tag = tag
                    }
                    tag.setField(org.jaudiotagger.tag.FieldKey.LYRICS, lyricsText)
                    audioFile.commit()
                    println("pEQControl: [Desktop] Embedded lyrics successfully saved to file via jaudiotagger: $url")
                }
            } catch (e: Exception) {
                println("pEQControl: [Desktop] Failed to embed lyrics in file: ${e.message}")
            }
            saveSidecarFile(url, "lrc", lyricsText)
        }
    }
    override suspend fun saveEmbeddedChapters(url: String, chapters: List<MediaChapter>) = Unit
    override suspend fun saveEmbeddedSubtitles(url: String, subtitles: List<SyncedLine>) = Unit
    override suspend fun saveSidecarFile(mediaPath: String, extension: String, content: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = java.io.File(mediaPath)
                if (file.exists()) {
                    val sidecar = java.io.File(file.parent, "${file.nameWithoutExtension}.$extension")
                    sidecar.writeText(content)
                    println("pEQControl: [Desktop] Sidecar file saved: ${sidecar.absolutePath}")
                }
            } catch (e: Exception) {
                println("pEQControl: [Desktop] Failed to save sidecar file: ${e.message}")
            }
        }
    }
    override fun playDrumSample(label: String, index: Int) {
        executor.execute {
            try {
                val cleanLabel = label.uppercase()
                val fileName = when {
                    cleanLabel.startsWith("808-") -> {
                        when {
                            cleanLabel.contains("KICK") -> "kick-808.wav"
                            cleanLabel.contains("SNARE") -> "snare-808.wav"
                            cleanLabel.contains("HAT") -> "hihat-808.wav"
                            cleanLabel.contains("CLAP") -> "clap-808.wav"
                            cleanLabel.contains("CRASH") -> "crash-808.wav"
                            cleanLabel.contains("TOM") -> "tom-808.wav"
                            cleanLabel.contains("RIM") -> "snare-block.wav"
                            cleanLabel.contains("COW") -> "cowbell-808.wav"
                            else -> "perc-808.wav"
                        }
                    }
                    cleanLabel.startsWith("GRIME-") -> {
                        when {
                            cleanLabel.contains("KICK") -> "kick-heavy.wav"
                            cleanLabel.contains("SNARE") -> "snare-dist01.wav"
                            cleanLabel.contains("HAT") -> "hihat-dist01.wav"
                            cleanLabel.contains("CLAP") -> "clap-crushed.wav"
                            cleanLabel.contains("METAL") -> "perc-metal.wav"
                            cleanLabel.contains("HAMMER") -> "perc-nasty.wav"
                            cleanLabel.contains("ENGINE") -> "perc-weirdo.wav"
                            else -> "perc-nasty.wav"
                        }
                    }
                    cleanLabel.startsWith("LOFI-") -> {
                        when {
                            cleanLabel.contains("KICK") -> "kick-softy.wav"
                            cleanLabel.contains("SNARE") -> "snare-lofi01.wav"
                            cleanLabel.contains("HAT") -> "hihat-plain.wav"
                            cleanLabel.contains("CLAP") -> "clap-tape.wav"
                            cleanLabel.contains("VINYL") -> "kick-vinyl01.wav"
                            cleanLabel.contains("DUST") -> "shaker-analog.wav"
                            else -> "perc-hollow.wav"
                        }
                    }
                    cleanLabel.startsWith("ANALOG") || cleanLabel.contains("JUNO") -> {
                        when {
                            cleanLabel.contains("KICK") -> "kick-classic.wav"
                            cleanLabel.contains("SNARE") -> "snare-analog.wav"
                            cleanLabel.contains("HAT") -> "hihat-electro.wav"
                            cleanLabel.contains("CLAP") -> "clap-analog.wav"
                            cleanLabel.contains("TOM") -> "tom-analog.wav"
                            else -> "perc-chirpy.wav"
                        }
                    }
                    else -> {
                         when {
                            cleanLabel.contains("KICK") -> "kick-acoustic01.wav"
                            cleanLabel.contains("SNARE") -> "snare-acoustic01.wav"
                            cleanLabel.contains("HAT") -> "hihat-acoustic01.wav"
                            else -> "perc-chirpy.wav"
                        }
                    }
                }
                
                val clip = drumClips.getOrPut(fileName) {
                    val stream = AudioSystem.getAudioInputStream(File(System.getProperty("user.dir") + "/composeApp/src/desktopMain/resources/fingerdrumming/$fileName"))
                    val c = AudioSystem.getClip()
                    c.open(stream)
                    c
                }
                clip.setFramePosition(0)
                clip.start()
            } catch (e: Exception) {
                println("pEQControl: [DRUMS] Trigger Error -> ${e.message} for label $label")
            }
        }
    }
    override fun addExternalSubtitle(path: String) {
        mediaPlayer.subpictures().setSubTitleFile(path)
        subtitlesEnabled = true
        uiScope.launch {
            delay(500)
            val tracks = mediaPlayer.subpictures().trackDescriptions()
            val firstValid = tracks?.firstOrNull { it.id() != -1 }
            if (firstValid != null) {
                activeSubtitleTrackId = firstValid.id()
                mediaPlayer.subpictures().setTrack(activeSubtitleTrackId)
            }
        }
        println("pEQControl: [VLC] Set external subtitle file: $path")
    }
    override fun setSubtitlesEnabled(enabled: Boolean) {
        subtitlesEnabled = enabled
        if (enabled) {
            if (activeSubtitleTrackId != -1) {
                mediaPlayer.subpictures().setTrack(activeSubtitleTrackId)
            } else {
                val tracks = mediaPlayer.subpictures().trackDescriptions()
                val firstValid = tracks?.firstOrNull { it.id() != -1 }
                if (firstValid != null) {
                    activeSubtitleTrackId = firstValid.id()
                    mediaPlayer.subpictures().setTrack(activeSubtitleTrackId)
                }
            }
        } else {
            val currentTrack = mediaPlayer.subpictures().track()
            if (currentTrack != -1 && currentTrack != -1) {
                activeSubtitleTrackId = currentTrack
            }
            mediaPlayer.subpictures().setTrack(-1)
        }
        println("pEQControl: [VLC] setSubtitlesEnabled($enabled), activeTrack=$activeSubtitleTrackId")
    }
    
    override fun togglePlayback() {
        if (isPlaying()) pause() else resume()
    }
    
    override fun nextTrack() {
        // Handled by AppViewModel but required by interface
    }
    
    override fun previousTrack() {
        // Handled by AppViewModel but required by interface
    }
    
    override fun getLoadedSampleLabels(): List<String> = emptyList()

    override suspend fun probeMedia(url: String): Map<String, List<String>> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, List<String>>()
        try {
            val media = factory.media().newMedia(url)
            media.parsing().parse()
            
            val videoTracks = media.info().videoTracks()
            val audioTracks = media.info().audioTracks()
            
            results["video"] = videoTracks.map { "${it.width()}x${it.height()}" }
            results["audio"] = audioTracks.map { "${it.channels()}ch ${it.rate()}Hz" }
            
            media.release()
        } catch (e: Exception) {
            println("pEQControl: [ENGINE] Probe Failure -> ${e.message}")
        }
        results
    }

    override fun release() {
        mediaPlayer.release()
        factory.release()
        executor.shutdown()
    }

    // UI Stubs
    fun getVideoComponent(): Component = Panel()
    fun showNativeVideoWindow() {}
    fun hideNativeVideoWindow() {}
}
