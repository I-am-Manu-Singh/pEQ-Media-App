package org.audiocular.peq.app

import androidx.compose.runtime.mutableStateOf
import org.audiocular.peq.app.audio.AudioEngine
import org.audiocular.peq.app.data.*
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File

class VlcAudioEngine : AudioEngine {

    val playerComponent =
        EmbeddedMediaPlayerComponent()

    private val mediaPlayer =
        playerComponent.mediaPlayer()

    private var currentUrl: String? = null

    private var volume: Float = 1f

    private val isPlayingState =
        mutableStateOf(false)

    init {

        mediaPlayer.events()
            .addMediaPlayerEventListener(

                object : MediaPlayerEventAdapter() {

                    override fun playing(
                        mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
                    ) {

                        isPlayingState.value = true

                        println(
                            "pEQControl: [VLC] PLAYING"
                        )
                    }

                    override fun paused(
                        mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
                    ) {

                        isPlayingState.value = false
                    }

                    override fun stopped(
                        mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
                    ) {

                        isPlayingState.value = false
                    }

                    override fun finished(
                        mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
                    ) {

                        isPlayingState.value = false

                        println(
                            "pEQControl: [VLC] FINISHED"
                        )

                        onTrackEnded?.invoke()
                    }

                    override fun error(
                        mediaPlayer: uk.co.caprica.vlcj.player.base.MediaPlayer?
                    ) {

                        isPlayingState.value = false

                        println(
                            "pEQControl: [VLC] PLAYBACK ERROR"
                        )
                    }
                }
            )
    }

    // =========================================================
    // CALLBACKS
    // =========================================================

    override var onFftDataReceived:
        ((FloatArray) -> Unit)? = null

    override var onTrackEnded:
        (() -> Unit)? = null

    override var onTrackChanged:
        (() -> Unit)? = null

    override var onBufferingChanged:
        ((Boolean) -> Unit)? = null

    // =========================================================
    // PLAYBACK
    // =========================================================

    override fun initialize(
        audioSessionId: Int
    ) = Unit

    override fun playTrack(
        url: String,
        title: String?,
        artist: String?,
        thumbnail: String?
    ) {

        currentUrl = url

        val mediaPath =

            if (
                url.startsWith("http")
            ) {

                url

            } else {

                File(
                    url.replace(
                        "file://",
                        ""
                    )
                ).absolutePath
            }

        println(
            "pEQControl: [VLC] Loading -> $mediaPath"
        )

        mediaPlayer.media().play(mediaPath)

        mediaPlayer.audio().setVolume(
            (volume * 100).toInt()
        )

        onTrackChanged?.invoke()
    }

    override fun loadTrack(
        url: String,
        title: String?,
        artist: String?,
        thumbnail: String?
    ) {

        currentUrl = url
    }

    override fun pause() {

        mediaPlayer.controls().pause()
    }

    override fun resume() {

        mediaPlayer.controls().play()
    }

    override fun stop() {

        mediaPlayer.controls().stop()

        isPlayingState.value = false
    }

    // =========================================================
    // SEEK
    // =========================================================

    override fun seekTo(
        positionMs: Long
    ) {

        mediaPlayer.controls()
            .setTime(positionMs)
    }

    override fun seekRelative(
        deltaMs: Long
    ) {

        val current =
            mediaPlayer.status().time()

        mediaPlayer.controls()
            .setTime(current + deltaMs)
    }

    // =========================================================
    // CONFIG
    // =========================================================

    override fun setVolume(
        volume: Float
    ) {

        this.volume = volume

        mediaPlayer.audio()
            .setVolume(
                (volume * 100).toInt()
            )
    }

    override fun setPlaybackSpeed(
        speed: Float
    ) {

        mediaPlayer.controls()
            .setRate(speed)
    }

    override fun setPitch(
        pitch: Float
    ) = Unit

    override fun setRepeatMode(
        mode: RepeatMode
    ) = Unit

    override fun setShuffleMode(
        enabled: Boolean
    ) = Unit

    override fun setLoopPoints(
        a: Long?,
        b: Long?
    ) = Unit

    override fun setVideoSurface(
        surface: Any?
    ) = Unit

    override fun setVideoScalingMode(
        mode: Int
    ) = Unit

    override fun setAudioDelay(
        ms: Long
    ) = Unit

    // =========================================================
    // INFO
    // =========================================================

    override fun isPlaying(): Boolean =
        isPlayingState.value

    override fun getDuration(): Long =
        mediaPlayer.status().length()

    override fun getCurrentPosition(): Long =
        mediaPlayer.status().time()

    override fun getCurrentTrackUri():
        String? = currentUrl

    override fun getDecoderInfo(): String =
        "VLCJ + FFmpeg"

    override fun getVideoWidth(): Int =
        mediaPlayer.video()
            .videoDimension()?.width ?: 0

    override fun getVideoHeight(): Int =
        mediaPlayer.video()
            .videoDimension()?.height ?: 0

    override fun getProcessingLatencyMs(): Int =
        15

    override fun getAudioDropouts(): Int = 0

    override fun getJitterBufferMs(): Int = 0

    override fun getCurrentSpectrum():
        FloatArray =
            FloatArray(64)

    override fun getPhaseCorrelation():
        Float = 0.5f

    override fun getBitrate(): Int = 0

    override fun getSampleRate(): Int = 0

    override fun getBitDepth(): Int = 0

    override fun getCodec(): String =
        "FFmpeg"

    // =========================================================
    // RELEASE
    // =========================================================

    override fun release() {

        try {

            mediaPlayer.release()

            playerComponent.release()

        } catch (e: Exception) {

            println(
                "pEQControl: [VLC] Release failed -> ${e.message}"
            )
        }
    }

    // =========================================================
    // STUBS
    // =========================================================

    override suspend fun getEmbeddedMetadata():
        pEQMediaMetadata =
            pEQMediaMetadata()

    override suspend fun getChapters():
        List<MediaChapter> =
            emptyList()

    override fun getAvailableTracks():
        List<MediaTrack> =
            emptyList()

    override fun switchTrack(
        track: MediaTrack?
    ) = Unit

    override fun updateEqBand(
        bandIndex: Int,
        gainDb: Float
    ) = Unit

    override fun updateStemVolume(
        stem: String,
        volume: Float
    ) = Unit

    override fun setCompressorThreshold(thresholdDb: Float) {}
    override fun setLimiterEnabled(enabled: Boolean) {}

    override fun setClarityLevel(
        level: Float
    ) = Unit

    override fun setDeepBassLevel(
        level: Float
    ) = Unit

    override fun setExclusiveMode(
        enabled: Boolean
    ) = Unit

    override fun setCrossfadeDuration(
        ms: Long
    ) = Unit

    override fun setInterferenceShielding(
        enabled: Boolean
    ) = Unit

    override fun alignMicPhase(
        enabled: Boolean
    ) = Unit

    override fun calibrateLatency():
        Int = 0

    override fun setOverflowProtection(
        enabled: Boolean
    ) = Unit

    override fun applyMasteringChain(
        preset: String
    ) = Unit

    override fun startUpnpCasting(
        target: String
    ) = Unit

    override fun setOscBroadcasting(
        enabled: Boolean
    ) = Unit

    override fun enableZeroLatencyMonitoring(
        enabled: Boolean
    ) = Unit

    override fun forceBluetoothCodec(
        codec: String
    ) = Unit

    override fun togglePhaseInversion(
        enabled: Boolean
    ) = Unit

    override fun toggleVocalRemoval(
        enabled: Boolean
    ) = Unit

    override fun setOutputDevice(
        deviceId: Int?
    ) = Unit

    override suspend fun saveEmbeddedLyrics(
        url: String,
        lyrics: List<SyncedLine>
    ) = Unit

    override suspend fun saveEmbeddedChapters(
        url: String,
        chapters: List<MediaChapter>
    ) = Unit

    override suspend fun saveEmbeddedSubtitles(
        url: String,
        subtitles: List<SyncedLine>
    ) = Unit

    override suspend fun saveSidecarFile(
        mediaPath: String,
        extension: String,
        content: String
    ) = Unit

    override fun playDrumSample(
        label: String
    ) = Unit

    override fun getConnectedDevices():
        List<Device> =
            listOf(Device.INTERNAL)
}