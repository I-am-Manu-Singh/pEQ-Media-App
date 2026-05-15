    // package org.audiocular.peq.app.audio

    // import androidx.compose.runtime.mutableStateOf
    // import org.audiocular.peq.app.data.*
    // import uk.co.caprica.vlcj.player.base.Equalizer
    // import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
    // import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
    // import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
    // import java.io.File
    // import java.util.concurrent.Executors
    // import java.util.concurrent.TimeUnit
    // import javax.sound.sampled.*
    // import kotlin.math.*
    // import kotlinx.coroutines.*

    // import uk.co.caprica.vlcj.player.base.MediaPlayer
    // import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
    // import uk.co.caprica.vlcj.factory.MediaPlayerFactory
    // import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface
    // import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapter
    // import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
    // import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
    // import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
    // import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
    // // import java.awt.image.BufferedImage
    // import java.nio.ByteBuffer
    // import androidx.compose.ui.graphics.toComposeImageBitmap
    // import org.jetbrains.skia.Image
    // import org.jetbrains.skia.Bitmap
    // import org.jetbrains.skia.ColorAlphaType
    // import org.jetbrains.skia.ColorType
    // import org.jetbrains.skia.ImageInfo

    // class DesktopAudioEngine : AudioEngine {
    //     private val executor = Executors.newSingleThreadScheduledExecutor()
    //     private val uiScope = CoroutineScope(
    //     Dispatchers.Main + SupervisorJob()
    // )   
    //     private var latestSpectrumData = FloatArray(32) { 0f }
    //     private var equalizer: Equalizer? = null
        
    //     // Video State for Callback
    //     val videoBitmap = mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    //     private var buffer: IntArray? = null
    //     private var width = 0
    //     private var height = 0

    //     /*
    //     * VLC FACTORY & PLAYER
    //     */
    //     private val factory = MediaPlayerFactory(

    //     // GPU decode
    //     "--avcodec-hw=any",

    //     // Performance
    //     "--drop-late-frames",
    //     "--skip-frames",

    //     // Lower latency
    //     "--file-caching=150",
    //     "--network-caching=300",
    //     "--live-caching=150",

    //     // Threading
    //     "--avcodec-threads=4",

    //     // Disable junk
    //     "--no-video-title-show",
    //     "--no-snapshot-preview",
    //     "--quiet",

    //     // Better sync
    //     "--clock-jitter=0",
    //     "--clock-synchro=0"
    // )
    //     private val mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer()

    //     /*
    //     * STATE
    //     */
    //     private var currentUrl: String? = null

    //     private var volume: Float = 1f

    //     private val isPlayingState =
    //         mutableStateOf(false)

    //     private var pixelBuffer: ByteArray? = null
    //     private val skiaBitmap = Bitmap()

    //     init {

    //         /*
    //         * VLC NATIVE DISCOVERY
    //         */
    //         println("pEQControl: Using bundled VLC")

    //         /*
    //         * CALLBACK VIDEO SURFACE
    //         * This allows us to render video frame-by-frame into a Compose Bitmap
    //         */
    //         val bufferFormatCallback = object : BufferFormatCallback {
    //             override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
    //                 // Cap resolution for UI rendering to ensure smoothness
    //                 val maxWidth = 640
    //                 if (sourceWidth > maxWidth) {
    //                     width = maxWidth
    //                     height = (maxWidth.toFloat() / sourceWidth * sourceHeight).toInt()
    //                 } else {
    //                     width = sourceWidth
    //                     height = sourceHeight
    //                 }
                    
    //                 buffer = IntArray(width * height)
    //                 return RV32BufferFormat(width, height)
    //             }

    //             override fun allocatedBuffers(buffers: Array<out ByteBuffer>?) { }
    //         }


    //         var lastFrameTime = 0L

    // var isRenderingFrame = false

    // val renderCallback = object : RenderCallback {

    //     override fun display(
    //         mediaPlayer: MediaPlayer?,
    //         nativeBuffers: Array<out ByteBuffer>?,
    //         bufferFormat: BufferFormat?
    //     ) {

    //         if (isRenderingFrame) return

    //         val now = System.currentTimeMillis()

    //         // 24 FPS UI rendering cap
    //         if (now - lastFrameTime < 41L) {
    //             return
    //         }

    //         val byteBuffer =
    //             nativeBuffers?.getOrNull(0)
    //                 ?: return

    //         val size =
    //             width * height * 4

    //         if (
    //             pixelBuffer == null ||
    //             pixelBuffer!!.size != size
    //         ) {

    //             pixelBuffer = ByteArray(size)

    //             skiaBitmap.allocPixels(
    //                 ImageInfo(
    //                     width,
    //                     height,
    //                     ColorType.BGRA_8888,
    //                     ColorAlphaType.PREMUL
    //                 )
    //             )
    //         }

    //         isRenderingFrame = true

    //         try {

    //             val localBuffer =
    //                 pixelBuffer ?: return

    //             byteBuffer.get(localBuffer)

    //             byteBuffer.rewind()

    //             skiaBitmap.installPixels(localBuffer)

    //             val skiaImage =
    //                 Image.makeFromBitmap(skiaBitmap)

    //             uiScope.launch {

    //                 try {

    //                     videoBitmap.value =
    //                         skiaImage.toComposeImageBitmap()

    //                 } finally {

    //                     skiaImage.close()
    //                 }
    //             }

    //             lastFrameTime = now

    //         } catch (_: Exception) {

    //         } finally {

    //             isRenderingFrame = false
    //         }
    //     }
    // }
    // }
    //         mediaPlayer.videoSurface().set(CallbackVideoSurface(bufferFormatCallback, renderCallback, true, null))
    //         mediaPlayer.video().setAspectRatio(null)
    //         /*
    //         * VLC EVENTS
    //         */
    //         mediaPlayer.events()
    //             .addMediaPlayerEventListener(

    //                 object : MediaPlayerEventAdapter() {

    //                     override fun playing(
    //                         mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
    //                     ) {
    //                         isPlayingState.value = true
                            
    //                         // Ensure volume is synced on play start
    //                         mediaPlayer?.audio()?.setVolume((volume * 100).toInt())

    //                         println(
    //                             "pEQControl: [VLC] PLAYING (Vol: ${(volume * 100).toInt()}%)"
    //                         )
    //                     }

    //                     override fun paused(
    //                         mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
    //                     ) {

    //                         isPlayingState.value = false

    //                         println(
    //                             "pEQControl: [VLC] PAUSED"
    //                         )
    //                     }

    //                     override fun stopped(
    //                         mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
    //                     ) {

    //                         isPlayingState.value = false

    //                         println(
    //                             "pEQControl: [VLC] STOPPED"
    //                         )
    //                     }

    //                     override fun finished(
    //                         mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
    //                     ) {

    //                         isPlayingState.value = false

    //                         println(
    //                             "pEQControl: [VLC] FINISHED"
    //                         )

    //                         onTrackEnded?.invoke()
    //                     }

    //                     override fun error(
    //                         mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
    //                     ) {

    //                         isPlayingState.value = false

    //                         println(
    //                             "pEQControl: [VLC] PLAYBACK ERROR"
    //                         )
    //                     }
    //                 }
    //             )

    //         // 📊 Start Spectrum Simulation / Polling
    //         executor.scheduleAtFixedRate({
    //             if (isPlaying()) {
    //                 // Simulate some movement in the bars based on volume and random noise
    //                 val base = (volume * 0.4f)
    //                 for (i in 0 until 32) {
    //                     val noise = (Math.random().toFloat() * 0.3f)
    //                     val freqBias = if (i < 8) 0.5f else if (i < 20) 0.3f else 0.1f
    //                     latestSpectrumData[i] = (latestSpectrumData[i] * 0.4f + (base + noise + freqBias) * 0.6f).coerceIn(0f, 1f)
    //                 }
    //             } else {
    //                 for (i in 0 until 32) {
    //                     latestSpectrumData[i] *= 0.8f
    //                 }
    //             }
    //         }, 0, 50, TimeUnit.MILLISECONDS)
    //     }

    //     /*
    //     * =========================================================
    //     * CALLBACKS
    //     * =========================================================
    //     */

    //     override var onFftDataReceived:
    //         ((FloatArray) -> Unit)? = null

    //     override var onTrackEnded:
    //         (() -> Unit)? = null

    //     override var onTrackChanged:
    //         (() -> Unit)? = null

    //     override var onBufferingChanged:
    //         ((Boolean) -> Unit)? = null

    //     /*
    //     * =========================================================
    //     * PLAYBACK
    //     * =========================================================
    //     */

    //     override fun initialize(
    //         audioSessionId: Int
    //     ) = Unit

    //     override fun playTrack(
    //         url: String,
    //         title: String?,
    //         artist: String?,
    //         thumbnail: String?
    //     ) {

    //         currentUrl = url

    //         val mediaPath =

    //             if (
    //                 url.startsWith("http")
    //             ) {

    //                 url

    //             } else {

    //                 File(
    //                     url.replace(
    //                         "file://",
    //                         ""
    //                     )
    //                 ).absolutePath
    //             }

    //         println(
    //             "pEQControl: [VLC] Loading -> $mediaPath"
    //         )

    //         /*
    //         * IMPORTANT:
    //         * USE controls().play()
    //         * NOT media().play()
    //         */

    //         mediaPlayer.media().play(mediaPath)

    //         mediaPlayer.audio().setVolume(
    //             (volume * 100).toInt()
    //         )

    //         onTrackChanged?.invoke()
    //     }

    //     override fun loadTrack(
    //         url: String,
    //         title: String?,
    //         artist: String?,
    //         thumbnail: String?
    //     ) {

    //         currentUrl = url
    //     }

    //     override fun pause() {
    //         mediaPlayer.controls().pause()
    //         isPlayingState.value = false
    //     }

    //     override fun resume() {
    //         mediaPlayer.controls().play()
    //         isPlayingState.value = true
    //     }

    //     override fun stop() {

    //         mediaPlayer.controls().stop()

    //         isPlayingState.value = false
    //     }

    //     /*
    //     * =========================================================
    //     * SEEK
    //     * =========================================================
    //     */

    //     override fun seekTo(
    //         positionMs: Long
    //     ) {

    //         mediaPlayer.controls()
    //             .setTime(positionMs)
    //     }

    //     override fun seekRelative(
    //         deltaMs: Long
    //     ) {

    //         val current =
    //             mediaPlayer.status().time()

    //         mediaPlayer.controls()
    //             .setTime(current + deltaMs)
    //     }

    //     /*
    //     * =========================================================
    //     * CONFIG
    //     * =========================================================
    //     */

    //     override fun setVolume(
    //         volume: Float
    //     ) {
    //         println("pEQControl: [VLC] Setting Volume -> ${(volume * 100).toInt()}%")
    //         this.volume = volume
    //         mediaPlayer.audio().setVolume(
    //             (volume * 100).toInt()
    //         )
    //     }

    //     override fun setPlaybackSpeed(
    //         speed: Float
    //     ) {

    //         mediaPlayer.controls()
    //             .setRate(speed)
    //     }

    //     override fun setPitch(
    //         pitch: Float
    //     ) = Unit

    //     override fun setRepeatMode(
    //         mode: RepeatMode
    //     ) = Unit

    //     override fun setShuffleMode(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun setLoopPoints(
    //         a: Long?,
    //         b: Long?
    //     ) = Unit

    //     override fun setVideoSurface(
    //         surface: Any?
    //     ) = Unit

    //     override fun setVideoScalingMode(
    //         mode: Int
    //     ) = Unit

    //     override fun setAudioDelay(
    //         ms: Long
    //     ) = Unit

    //     override fun setNeuralSaturation(
    //         enabled: Boolean
    //     ) = Unit

    //     /*
    //     * =========================================================
    //     * INFO
    //     * =========================================================
    //     */

    //     override fun isPlaying(): Boolean =
    //         mediaPlayer.status().isPlaying()

    //     override fun getDuration(): Long =
    //         mediaPlayer.status().length()

    //     override fun getCurrentPosition(): Long =
    //         mediaPlayer.status().time()

    //     override fun getCurrentTrackUri():
    //         String? = currentUrl

    //     override fun getDecoderInfo(): String =
    //         "VLCJ + FFmpeg"

    //     override fun getVideoWidth(): Int =
    //         mediaPlayer.video()
    //             .videoDimension()?.width ?: 0

    //     override fun getVideoHeight(): Int =
    //         mediaPlayer.video()
    //             .videoDimension()?.height ?: 0

    //     override fun getProcessingLatencyMs(): Int =
    //         15 + (Math.random() * 5).toInt()

    //     override fun getAudioDropouts(): Int =
    //         0

    //     override fun getJitterBufferMs(): Int =
    //         20

    //     override fun getCurrentSpectrum():
    //         FloatArray = latestSpectrumData

    //     override fun getPhaseCorrelation():
    //         Float = 0.5f + (sin(System.currentTimeMillis() / 500.0).toFloat() * 0.1f)

    //     override fun getBitrate(): Int = 320 // Default fallback

    //     override fun getSampleRate(): Int = 44100 // Default fallback

    //     override fun getBitDepth(): Int = 16

    //     override fun getCodec(): String = "FFmpeg"

    //     /*
    //     * =========================================================
    //     * RELEASE
    //     * =========================================================
    //     */

    // override fun release() {

    //     try {

    //         uiScope.cancel()

    //         mediaPlayer.release()

    //         factory.release()

    //     } catch (e: Exception) {

    //         println("pEQControl: [VLC] Release failed -> ${e.message}")
    //     }
    // }

    //     /*
    //     * =========================================================
    //     * MEDIA DATA
    //     * =========================================================
    //     */

    //     override suspend fun getEmbeddedMetadata(): pEQMediaMetadata {
    //         return pEQMediaMetadata(
    //             availableTracks = getAvailableTracks(),
    //             chapters = getChapters()
    //         )
    //     }

    //     override suspend fun getChapters(): List<MediaChapter> {
    //         return emptyList()
    //     }

    //     override fun getAvailableTracks(): List<MediaTrack> {
    //         val tracks = mutableListOf<MediaTrack>()
            
    //         mediaPlayer.video().trackDescriptions()?.forEach { desc ->
    //             tracks.add(MediaTrack(desc.id().toString(), desc.description() ?: "Video", TrackType.VIDEO))
    //         }
            
    //         mediaPlayer.audio().trackDescriptions()?.forEach { desc ->
    //             tracks.add(MediaTrack(desc.id().toString(), desc.description() ?: "Audio", TrackType.AUDIO))
    //         }
            
    //         // VLCJ 4 subtitles are often called subpictures
    //         mediaPlayer.subpictures().trackDescriptions()?.forEach { desc ->
    //             tracks.add(MediaTrack(desc.id().toString(), desc.description() ?: "Subtitle", TrackType.SUBTITLE))
    //         }
            
    //         return tracks
    //     }

    //     override fun switchTrack(
    //         track: MediaTrack?
    //     ) = Unit

    //     /*
    //     * =========================================================
    //     * AUDIO FX
    //     * =========================================================
    //     */

    //     override fun updateEqBand(
    //         bandIndex: Int,
    //         gainDb: Float
    //     ) {
    //         if (equalizer == null) {
    //             equalizer = factory.equalizer().newEqualizer()
    //             mediaPlayer.audio().setEqualizer(equalizer)
    //         }
    //         equalizer?.setAmp(bandIndex, gainDb)
    //         println("pEQControl: [VLC] EQ Band $bandIndex -> $gainDb dB")
    //     }

    //     override fun updateStemVolume(
    //         stem: String,
    //         volume: Float
    //     ) {
    //         println("pEQControl: [VLC] Stem $stem Volume -> $volume")
    //         // Simulation: Adjust EQ bands based on stem
    //         when (stem) {
    //             "vocals" -> (4..7).forEach { updateEqBand(it, (volume - 1f) * 12f) }
    //             "bass" -> (0..2).forEach { updateEqBand(it, (volume - 1f) * 12f) }
    //         }
    //     }

    //     override fun setCompressorThreshold(
    //         thresholdDb: Float
    //     ) {
    //         println("pEQControl: [VLC] Compressor Threshold -> $thresholdDb dB")
    //     }

    //     override fun setLimiterEnabled(
    //         enabled: Boolean
    //     ) {
    //         println("pEQControl: [VLC] Limiter -> $enabled")
    //     }

    //     override fun setClarityLevel(
    //         level: Float
    //     ) {
    //         // Boost highs
    //         (7..9).forEach { updateEqBand(it, level * 6f) }
    //     }

    //     override fun setDeepBassLevel(
    //         level: Float
    //     ) {
    //         // Boost lows
    //         (0..2).forEach { updateEqBand(it, level * 8f) }
    //     }

    //     override fun setExclusiveMode(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun setCrossfadeDuration(
    //         ms: Long
    //     ) = Unit

    //     override fun setInterferenceShielding(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun alignMicPhase(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun calibrateLatency():
    //         Int = 0

    //     override fun setOverflowProtection(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun applyMasteringChain(
    //         preset: String
    //     ) = Unit

    //     override fun startUpnpCasting(
    //         target: String
    //     ) = Unit

    //     override fun setOscBroadcasting(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun enableZeroLatencyMonitoring(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun forceBluetoothCodec(
    //         codec: String
    //     ) = Unit

    //     override fun togglePhaseInversion(
    //         enabled: Boolean
    //     ) = Unit

    //     override fun toggleVocalRemoval(
    //         enabled: Boolean
    //     ) {
    //         println("pEQControl: [VLC] Vocal Removal -> $enabled")
    //         if (enabled) {
    //             // Scoop the vocal frequencies (approx 500Hz - 4kHz)
    //             (4..7).forEach { updateEqBand(it, -15f) }
    //         } else {
    //             (4..7).forEach { updateEqBand(it, 0f) }
    //         }
    //     }

    //     override fun setOutputDevice(
    //         deviceId: Int?
    //     ) {
    //         println("pEQControl: [VLC] Output Device routing requested for ID: $deviceId")
    //     }

    //     /*
    //     * =========================================================
    //     * SAVE
    //     * =========================================================
    //     */

    //     override suspend fun saveEmbeddedLyrics(
    //         url: String,
    //         lyrics: List<SyncedLine>
    //     ) = Unit

    //     override suspend fun saveEmbeddedChapters(
    //         url: String,
    //         chapters: List<MediaChapter>
    //     ) = Unit

    //     override suspend fun saveEmbeddedSubtitles(
    //         url: String,
    //         subtitles: List<SyncedLine>
    //     ) = Unit

    //     override suspend fun saveSidecarFile(
    //         mediaPath: String,
    //         extension: String,
    //         content: String
    //     ) = Unit

    //     /*
    //     * =========================================================
    //     * DRUMS
    //     * =========================================================
    //     */

    //     override fun playDrumSample(
    //         label: String,
    //         index: Int
    //     ) {
    //         executor.execute {
    //             try {
    //                 val hz = when {
    //                     label.contains("KICK") -> 60.0
    //                     label.contains("SNARE") -> 200.0
    //                     label.contains("HAT") -> 8000.0
    //                     else -> 440.0
    //                 }
    //                 playTone(hz, 100)
    //             } catch (e: Exception) {
    //                 e.printStackTrace()
    //             }
    //         }
    //         println("pEQControl: Drum -> $label")
    //     }

    //     private fun playTone(hz: Double, msecs: Int) {
    //         val buf = ByteArray(1)
    //         val af = AudioFormat(8000f, 8, 1, true, false)
    //         val sdl = AudioSystem.getSourceDataLine(af)
    //         sdl.open(af)
    //         sdl.start()
    //         for (i in 0 until msecs * 8) {
    //             val angle = i / (8000f / hz) * 2.0 * Math.PI
    //             buf[0] = (Math.sin(angle) * 127.0).toInt().toByte()
    //             sdl.write(buf, 0, 1)
    //         }
    //         sdl.drain()
    //         sdl.stop()
    //         sdl.close()
    //     }

    //     override fun updateEqQ(
    //         bandIndex: Int,
    //         q: Float
    //     ) {
    //         println("pEQControl: [VLC] EQ Q $bandIndex -> $q")
    //     }

    //     /*
    //     * =========================================================
    //     * DEVICES
    //     * =========================================================
    //     */

    //     override fun getConnectedDevices(): List<Device> {
    //         val list = mutableListOf(Device.INTERNAL)
    //         val vlcDevices = mediaPlayer.audio().outputDevices()
    //         vlcDevices?.forEach {
    //             list.add(Device(name = it.toString(), chip = "VLC Bridge", mode = "Dynamic"))
    //         }
    //         return list
    //     }
    // override fun togglePlayback() {

    //     if (isPlaying()) {
    //         pause()
    //     } else {
    //         resume()
    //     }
    // }

    // override fun nextTrack() {
    //     // Handled by AppViewModel queue logic calling playTrack
    // }

    // override fun previousTrack() {
    //     // Handled by AppViewModel queue logic calling playTrack
    // }

    // // override fun addScanFolder(
    // //     path: String
    // // ) {
    // //     println("pEQControl: [DESKTOP] Scanning folder: $path")
    // // }
    // override fun updateStemPan(
    //     stem: String,
    //     pan: Float
    // ) = Unit

    // override fun updateCompressor(
    //     threshold: Float,
    //     ratio: Float,
    //     attack: Float,
    //     release: Float
    // ) = Unit

    // override fun setPhaseInversion(
    //     enabled: Boolean
    // ) = Unit

    // override fun setChannelBalance(
    //     balance: Float
    // ) = Unit

    // override fun setSubHarmonicGenerator(
    //     enabled: Boolean
    // ) = Unit

    // override fun addExternalSubtitle(
    //     path: String
    // ) {
    //     mediaPlayer.subpictures().setSubTitleFile(path)
    //     // mediaPlayer.subpictures().setSubTitleFile(File(path))
    //     // mediaPlayer.subpictures().setSubTitleFile(path, true)
    // }



    // override fun setSubtitlesEnabled(
    //     enabled: Boolean
    // ) = Unit
    //     /*
    //     * =========================================================
    //     * HELPERS
    //     * =========================================================
    //     */
    // }
package org.audiocular.peq.app.audio

import androidx.compose.runtime.mutableStateOf
import org.audiocular.peq.app.data.*
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.Equalizer
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.TrackDescription
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.awt.Color
import java.awt.Component

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

import kotlin.math.sin
import java.awt.Dimension
import javax.swing.SwingUtilities
class DesktopAudioEngine : AudioEngine {

    /*
     * VLC INIT
     */
    init {
        setupVlc()
    }

    private fun setupVlc() {

        try {

            val projectDir =
                System.getProperty("user.dir")

            val vlcRoot =
                "$projectDir/src/desktopMain/resources/vlc"

            val libPath =
                "$vlcRoot/lib"

            val pluginPath =
                "$vlcRoot/plugins"

            println("pEQControl: VLC ROOT -> $vlcRoot")
            println("pEQControl: VLC LIB -> $libPath")
            println("pEQControl: VLC PLUGINS -> $pluginPath")

            val libvlc =
                File("$libPath/libvlc.dylib")

            val libvlccore =
                File("$libPath/libvlccore.dylib")

            println(
                "pEQControl: libvlc exists -> ${libvlc.exists()}"
            )

            println(
                "pEQControl: libvlccore exists -> ${libvlccore.exists()}"
            )

            System.setProperty(
                "jna.library.path",
                libPath
            )

            System.setProperty(
                "VLC_PLUGIN_PATH",
                pluginPath
            )

            System.load(
                libvlccore.absolutePath
            )

            System.load(
                libvlc.absolutePath
            )

            println(
                "pEQControl: VLC LOADED SUCCESSFULLY"
            )

        } catch (e: Exception) {

            println(
                "pEQControl: VLC INIT ERROR -> ${e.message}"
            )

            e.printStackTrace()
        }
    }

    /*
     * VLC FACTORY
     */
    private val factory by lazy {

        MediaPlayerFactory(

            "--quiet",

            "--no-video-title-show",

            "--avcodec-hw=any",

            "--aout=macosx",

        )
    }

    /*
     * AUDIO PLAYER
     */
    private val audioPlayer by lazy {

        factory.mediaPlayers()
            .newMediaPlayer()
    }

    /*
     * VIDEO PLAYER
     */
    private var videoPlayerComponent:
        EmbeddedMediaPlayerComponent? = null

    private var videoPlayer:
        EmbeddedMediaPlayer? = null

    /*
     * STATE
     */
    val isVideoReady =
        mutableStateOf(false)

    private var isVideoMedia = false

    private var currentUrl: String? = null

    private var volume = 1f

    @Volatile
    private var videoSurfaceReady = false

    private var latestSpectrumData =
        FloatArray(32) { 0f }

    /*
     * THREADS
     */
    private val executor =
        Executors.newSingleThreadScheduledExecutor()

    /*
     * EQ
     */
    private var equalizer: Equalizer? = null

    /*
     * INIT
     */
    init {

        setupListeners(audioPlayer)

        executor.scheduleAtFixedRate({

            if (isPlaying()) {

                val base =
                    volume * 0.4f

                for (i in 0 until 32) {

                    val noise =
                        Math.random().toFloat() * 0.3f

                    val freqBias =
                        when {

                            i < 8 -> 0.5f
                            i < 20 -> 0.3f
                            else -> 0.1f
                        }

                    latestSpectrumData[i] = (

                        latestSpectrumData[i] * 0.4f +

                        (base + noise + freqBias) * 0.6f

                    ).coerceIn(0f, 1f)
                }

            } else {

                for (i in 0 until 32) {

                    latestSpectrumData[i] *= 0.8f
                }
            }

        }, 0, 50, TimeUnit.MILLISECONDS)
    }

    /*
     * VIDEO PLAYER
     */
  private fun ensureVideoPlayer() {

    if (
        videoPlayerComponent != null &&
        videoPlayer != null
    ) {
        return
    }

    println(
        "pEQControl: CREATING VIDEO SURFACE"
    )

    val createPlayer = {

        val component =
            EmbeddedMediaPlayerComponent()

        component.isVisible = true
        component.isFocusable = true
        component.isOpaque = true

        component.background =
            Color.BLACK

        component.preferredSize =
            Dimension(1920, 1080)

        component.minimumSize =
            Dimension(320, 180)

        videoPlayerComponent =
            component

        videoPlayer =
            component.mediaPlayer()

        setupListeners(videoPlayer!!)
    }

    if (SwingUtilities.isEventDispatchThread()) {

        createPlayer()

    } else {

        SwingUtilities.invokeLater {

            createPlayer()
        }
    }
}

    /*
     * ACTIVE PLAYER
     */
    private fun activePlayer(): MediaPlayer {

        return if (isVideoMedia) {

            videoPlayer ?: audioPlayer

        } else {

            audioPlayer
        }
    }

    /*
     * VLC LISTENERS
     */
    private fun setupListeners(
        player: MediaPlayer
    ) {

        player.events()
            .addMediaPlayerEventListener(

                object : MediaPlayerEventAdapter() {

                    override fun playing(
                        mediaPlayer: MediaPlayer?
                    ) {

                        isVideoReady.value =
                            true

                        mediaPlayer?.audio()
                            ?.setVolume(
                                (volume * 100).toInt()
                            )

                        println(
                            "pEQControl: [VLC] PLAYING"
                        )

                        println(
                            "pEQControl: VIDEO SIZE -> ${
                                mediaPlayer
                                    ?.video()
                                    ?.videoDimension()
                            }"
                        )
                    }

                    override fun paused(
                        mediaPlayer: MediaPlayer?
                    ) {

                        println(
                            "pEQControl: [VLC] PAUSED"
                        )
                    }

                    override fun stopped(
                        mediaPlayer: MediaPlayer?
                    ) {

                        isVideoReady.value = false

                        println(
                            "pEQControl: [VLC] STOPPED"
                        )
                    }

                    override fun finished(
                        mediaPlayer: MediaPlayer?
                    ) {

                        isVideoReady.value = false

                        onTrackEnded?.invoke()
                    }

                    override fun error(
                        mediaPlayer: MediaPlayer?
                    ) {

                        isVideoReady.value = false

                        println(
                            "pEQControl: [VLC] ERROR"
                        )
                    }
                }
            )
    }

    /*
     * VIDEO SURFACE
     */
    fun getVideoComponent(): Component {

        ensureVideoPlayer()

        return videoPlayerComponent!!
    }

    fun notifyVideoSurfaceReady() {

        val component =
            videoPlayerComponent

        videoSurfaceReady =

            component != null &&
            component.isDisplayable &&
            component.isShowing

        println(
            "pEQControl: VIDEO SURFACE READY -> $videoSurfaceReady"
        )
    }

    fun isCurrentMediaVideo(): Boolean {

        return isVideoMedia
    }

    /*
     * CALLBACKS
     */
    override var onFftDataReceived:
        ((FloatArray) -> Unit)? = null

    override var onTrackEnded:
        (() -> Unit)? = null

    override var onTrackChanged:
        (() -> Unit)? = null

    override var onBufferingChanged:
        ((Boolean) -> Unit)? = null

    override fun initialize(
        audioSessionId: Int
    ) = Unit

    /*
     * PLAY MEDIA
     */
    override fun playTrack(
        url: String,
        title: String?,
        artist: String?,
        thumbnail: String?
    ) {

        val mediaPath =

            if (url.startsWith("http")) {

                url

            } else {

                File(
                    url.replace("file://", "")
                ).absolutePath
            }

        audioPlayer.controls().stop()

        videoPlayer?.controls()?.stop()

        currentUrl = mediaPath

        isVideoMedia =
            mediaPath.endsWith(".mp4", true) ||
            mediaPath.endsWith(".mkv", true) ||
            mediaPath.endsWith(".avi", true) ||
            mediaPath.endsWith(".mov", true) ||
            mediaPath.endsWith(".webm", true)

        /*
         * VIDEO
         */
        if (isVideoMedia) {

            ensureVideoPlayer()

            println(
                "pEQControl: WAITING FOR VIDEO SURFACE"
            )

            Executors
                .newSingleThreadExecutor()
                .execute {

                    repeat(80) {

                        try {

                            val component =
                                videoPlayerComponent

                            if (
                                videoSurfaceReady &&
                                component != null &&
                                component.isDisplayable &&
                                component.isShowing
                            ) {

                                println(
                                    "pEQControl: STARTING VIDEO"
                                )

                                SwingUtilities.invokeLater {

                                    component.isVisible = true

                                    component.revalidate()

                                    component.repaint()

                                    val result =
                                        videoPlayer
                                            ?.media()
                                            ?.play(mediaPath)

                                    println(
                                        "pEQControl: VIDEO START RESULT -> $result"
                                    )

                                    videoPlayer
                                        ?.audio()
                                        ?.setVolume(
                                            (volume * 100).toInt()
                                        )

                                    onTrackChanged?.invoke()
                                }

                                return@execute
                            }

                            Thread.sleep(100)

                        } catch (e: Exception) {

                            e.printStackTrace()
                        }
                    }

                    println(
                        "pEQControl: VIDEO SURFACE TIMEOUT"
                    )
                }

            return
        }

        /*
         * AUDIO
         */
        println(
            "pEQControl: [VLC] Loading -> $mediaPath"
        )

        audioPlayer
            .media()
            .play(mediaPath)

        audioPlayer
            .audio()
            .setVolume(
                (volume * 100).toInt()
            )

        onTrackChanged?.invoke()
    }

    /*
     * BASIC CONTROLS
     */
    override fun pause() {

        activePlayer()
            .controls()
            .setPause(true)
    }

    override fun resume() {

        activePlayer()
            .controls()
            .setPause(false)
    }

    override fun stop() {

        audioPlayer.controls().stop()

        videoPlayer?.controls()?.stop()
    }

    override fun seekTo(
        positionMs: Long
    ) {

        activePlayer()
            .controls()
            .setTime(positionMs)
    }

    override fun seekRelative(
        deltaMs: Long
    ) {

        val current =
            activePlayer()
                .status()
                .time()

        activePlayer()
            .controls()
            .setTime(current + deltaMs)
    }

    override fun setVolume(
        volume: Float
    ) {

        this.volume = volume

        audioPlayer.audio()
            .setVolume(
                (volume * 100).toInt()
            )

        videoPlayer?.audio()
            ?.setVolume(
                (volume * 100).toInt()
            )
    }

    override fun setPlaybackSpeed(
        speed: Float
    ) {

        activePlayer()
            .controls()
            .setRate(speed)
    }

    override fun isPlaying(): Boolean =
        activePlayer()
            .status()
            .isPlaying()

    override fun getDuration(): Long =
        activePlayer()
            .status()
            .length()

    override fun getCurrentPosition(): Long =
        activePlayer()
            .status()
            .time()

    override fun getCurrentTrackUri(): String? =
        currentUrl

    override fun getVideoWidth(): Int =
        if (isVideoMedia)
            videoPlayer?.video()
                ?.videoDimension()
                ?.width ?: 0
        else 0

    override fun getVideoHeight(): Int =
        if (isVideoMedia)
            videoPlayer?.video()
                ?.videoDimension()
                ?.height ?: 0
        else 0

    /*
     * RELEASE
     */
    override fun release() {

        try {

            audioPlayer.release()

            videoPlayer?.release()

            videoPlayerComponent?.release()

            factory.release()

            executor.shutdownNow()

        } catch (e: Exception) {

            println(
                "pEQControl: [VLC] Release failed -> ${e.message}"
            )
        }
    }

    /*
     * STUBS
     */
    override fun loadTrack(url: String, title: String?, artist: String?, thumbnail: String?) = Unit
    override fun updateEqBand(bandIndex: Int, gainDb: Float) = Unit
    override fun getDecoderInfo(): String = "VLCJ"
    override fun getProcessingLatencyMs(): Int = 0
    override fun getAudioDropouts(): Int = 0
    override fun getJitterBufferMs(): Int = 0
    override fun getCurrentSpectrum(): FloatArray = latestSpectrumData
    override fun getPhaseCorrelation(): Float = 0f
    override fun getBitrate(): Int = 320
    override fun getSampleRate(): Int = 44100
    override fun getBitDepth(): Int = 16
    override fun getCodec(): String = "FFmpeg"
    override suspend fun getEmbeddedMetadata(): pEQMediaMetadata = pEQMediaMetadata()
    override suspend fun getChapters(): List<MediaChapter> = emptyList()
    override fun getAvailableTracks(): List<MediaTrack> = emptyList()
    override fun switchTrack(track: MediaTrack?) = Unit
    override fun togglePlayback() = Unit
    override fun nextTrack() = Unit
    override fun previousTrack() = Unit
    override fun addExternalSubtitle(path: String) = Unit
    override fun getConnectedDevices(): List<Device> = listOf(Device.INTERNAL)
    override fun playDrumSample(label: String, index: Int) = Unit
    override fun setSubtitlesEnabled(enabled: Boolean) = Unit
    override fun setPitch(pitch: Float) = Unit
    override fun setRepeatMode(mode: RepeatMode) = Unit
    override fun setShuffleMode(enabled: Boolean) = Unit
    override fun setLoopPoints(a: Long?, b: Long?) = Unit
    override fun setVideoSurface(surface: Any?) = Unit
    override fun setVideoScalingMode(mode: Int) = Unit
    override fun setAudioDelay(ms: Long) = Unit
    override fun setNeuralSaturation(enabled: Boolean) = Unit
    override fun updateStemVolume(stem: String, volume: Float) = Unit
    override fun setCompressorThreshold(thresholdDb: Float) = Unit
    override fun setLimiterEnabled(enabled: Boolean) = Unit
    override fun setClarityLevel(level: Float) = Unit
    override fun setDeepBassLevel(level: Float) = Unit
    override fun setExclusiveMode(enabled: Boolean) = Unit
    override fun setCrossfadeDuration(ms: Long) = Unit
    override fun setInterferenceShielding(enabled: Boolean) = Unit
    override fun alignMicPhase(enabled: Boolean) = Unit
    override fun calibrateLatency(): Int = 0
    override fun setOverflowProtection(enabled: Boolean) = Unit
    override fun applyMasteringChain(preset: String) = Unit
    override fun startUpnpCasting(target: String) = Unit
    override fun setOscBroadcasting(enabled: Boolean) = Unit
    override fun enableZeroLatencyMonitoring(enabled: Boolean) = Unit
    override fun forceBluetoothCodec(codec: String) = Unit
    override fun togglePhaseInversion(enabled: Boolean) = Unit
    override fun toggleVocalRemoval(enabled: Boolean) = Unit
    override fun setOutputDevice(deviceId: Int?) = Unit
    override suspend fun saveEmbeddedLyrics(url: String, lyrics: List<SyncedLine>) = Unit
    override suspend fun saveEmbeddedChapters(url: String, chapters: List<MediaChapter>) = Unit
    override suspend fun saveEmbeddedSubtitles(url: String, subtitles: List<SyncedLine>) = Unit
    override suspend fun saveSidecarFile(mediaPath: String, extension: String, content: String) = Unit
    override fun updateEqQ(bandIndex: Int, q: Float) = Unit
    override fun updateStemPan(stem: String, pan: Float) = Unit
    override fun updateCompressor(threshold: Float, ratio: Float, attack: Float, release: Float) = Unit
    override fun setPhaseInversion(enabled: Boolean) = Unit
    override fun setChannelBalance(balance: Float) = Unit
    override fun setSubHarmonicGenerator(enabled: Boolean) = Unit
}