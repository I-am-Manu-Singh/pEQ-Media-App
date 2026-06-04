package org.audiocular.peq.app.audio

/**
 * INDUSTRIAL AUDIO INTERFACE: pEQ CORE
 * The primary contract for low-latency DSP and cinematic transport.
 */
interface AudioEngine {
    fun initialize(audioSessionId: Int)
    fun playTrack(url: String, title: String? = null, artist: String? = null, thumbnail: String? = null)
    fun setNeuralSaturation(enabled: Boolean)
    fun loadTrack(url: String, title: String? = null, artist: String? = null, thumbnail: String? = null)
    fun loadStems(stems: Map<String, String>)
    fun setVideoSurface(surface: Any?)
    fun setVideoScalingMode(mode: Int)
    fun setAudioDelay(ms: Long)
    var onFftDataReceived: ((FloatArray) -> Unit)?
    var onTrackEnded: (() -> Unit)?
    var onTrackChanged: (() -> Unit)?
    var onBufferingChanged: ((Boolean) -> Unit)?
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(positionMs: Long)
    fun seekRelative(deltaMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setPitch(pitch: Float)
    fun setRepeatMode(mode: org.audiocular.peq.app.data.RepeatMode)
    fun setShuffleMode(enabled: Boolean)
    fun setLoopPoints(a: Long?, b: Long?)
    suspend fun getEmbeddedMetadata(): org.audiocular.peq.app.data.pEQMediaMetadata
    suspend fun getChapters(): List<org.audiocular.peq.app.data.MediaChapter>
    fun getAvailableTracks(): List<org.audiocular.peq.app.data.MediaTrack>
    fun switchTrack(track: org.audiocular.peq.app.data.MediaTrack?)
    fun updateEqBand(bandIndex: Int, gainDb: Float)
    fun updateEqQ(bandIndex: Int, q: Float)
    fun updateStemVolume(stem: String, volume: Float)
    fun updateStemPan(stem: String, pan: Float)
    fun setCompressorThreshold(thresholdDb: Float)
    fun updateCompressor(threshold: Float, ratio: Float, attack: Float, release: Float)
    fun setLimiterEnabled(enabled: Boolean)
    fun setClarityLevel(level: Float)
    fun setDeepBassLevel(level: Float)
    fun getDuration(): Long
    fun getCurrentPosition(): Long
    fun getCurrentTrackUri(): String?
    fun setExclusiveMode(enabled: Boolean)
    fun getDecoderInfo(): String // Returns "HW", "SW", or "Unknown"
    
    // Performance Telemetry (P-09)
    fun getProcessingLatencyMs(): Int
    fun getAudioDropouts(): Int
    fun getJitterBufferMs(): Int
    
    fun setCrossfadeDuration(ms: Long)
    fun setInterferenceShielding(enabled: Boolean)
    fun alignMicPhase(enabled: Boolean)
    
    fun calibrateLatency(): Int
    fun setOverflowProtection(enabled: Boolean)
    
    fun applyMasteringChain(preset: String)
    fun startUpnpCasting(target: String)
    fun setOscBroadcasting(enabled: Boolean)
    fun enableZeroLatencyMonitoring(enabled: Boolean)
    
    fun getCurrentSpectrum(): FloatArray
    fun getPhaseCorrelation(): Float
    
    fun forceBluetoothCodec(codec: String)
    fun togglePhaseInversion(enabled: Boolean)
    fun setPhaseInversion(enabled: Boolean)
    fun setVolume(volume: Float)
    fun setChannelBalance(balance: Float)
    fun toggleVocalRemoval(enabled: Boolean)
    fun setSubHarmonicGenerator(enabled: Boolean)
    fun isPlaying(): Boolean
    fun setOutputDevice(deviceId: Int?) // New: Routing Control
    fun getBitrate(): Int
    fun getSampleRate(): Int
    fun getBitDepth(): Int
    fun getCodec(): String
    fun getVideoWidth(): Int
    fun getVideoHeight(): Int
    suspend fun saveEmbeddedLyrics(url: String, lyrics: List<org.audiocular.peq.app.data.SyncedLine>)
    suspend fun saveEmbeddedChapters(url: String, chapters: List<org.audiocular.peq.app.data.MediaChapter>)
    suspend fun saveEmbeddedSubtitles(url: String, subtitles: List<org.audiocular.peq.app.data.SyncedLine>)
    suspend fun saveSidecarFile(mediaPath: String, extension: String, content: String)
    fun playDrumSample(label: String, index: Int = -1)
    fun getLoadedSampleLabels(): List<String> = emptyList()

    fun addExternalSubtitle(path: String)
    fun getConnectedDevices(): List<org.audiocular.peq.app.data.Device> = emptyList()
    fun setSubtitlesEnabled(enabled: Boolean)

    // 🎓 Learning & Calibration Engine
    fun startTuner() {}
    fun stopTuner() {}
    fun setMetronomeEnabled(enabled: Boolean) {}
    fun setBpm(bpm: Float) {}
fun togglePlayback() {}

fun nextTrack() {}

fun previousTrack() {}

    fun getDspLoad(): Float = 0f
    fun isInitialized(): Boolean = true
    fun getLatency(): Int = 0
    fun getRegistry(): Map<String, String> = emptyMap()
    fun release()
    suspend fun probeMedia(url: String): Map<String, List<String>>
}

expect fun getAudioEngine(): AudioEngine
