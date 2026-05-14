package org.audiocular.peq.app.data
import kotlin.collections.emptyMap
import kotlin.collections.emptyList

import kotlinx.serialization.Serializable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Serializable
enum class MediaType { AUDIO, VIDEO, STEM, PROJECT }
@Serializable
enum class RepeatMode { OFF, ALL, ONE, FOLDER }
@Serializable
data class pEQMediaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val path: String,
    val type: MediaType,
    val format: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val thumbnail: String? = null,
    val bitDepth: Int = 16,
    val sampleRate: Int = 44100,
    val isHiRes: Boolean = false,
    val isLossless: Boolean = false,
    val resolution: String? = null, // For Video
    val codec: String? = null,     // For Video
    val artist: String? = null,    // For Audio
    val album: String? = null,     // For Audio
    val albumArtist: String? = null, // ✅ New
    val composer: String? = null,    // ✅ New
    val genre: String? = null,       // ✅ New
    val trackNumber: Int? = null,    // ✅ New
    val discNumber: Int? = null,     // ✅ New
    val year: Int? = null,          // ✅ New
    val bitrate: Int = 0,           // ✅ New (kbps)
    val channels: Int = 2,          // ✅ New
    val dateAdded: Long = 0,        // ✅ New
    val lastPlayed: Long = 0,       // ✅ New
    val dateModified: Long = 0,
    val isFavorite: Boolean = false,
    val frameRate: Float = 0f,      // ✅ New
    val videoWidth: Int = 0,       // ✅ Required
    val videoHeight: Int = 0,      // ✅ Required
    val playCount: Int = 0,        // ✅ Tracking Popularity
    val isStream: Boolean = false,  // ✅ Web Streaming Support
    val lyrics: String? = null     // 📝 Embedded Lyrics
) {
    val fileName: String get() = path.substringAfterLast("/")
    val displaySize: String get() = "${sizeBytes / 1024 / 1024.0f}MB"
}

@Serializable
data class SplitJob(
    val id: String,
    val name: String,
    val progress: Float,
    val status: String,
    val resultPaths: Map<String, String> = emptyMap(),
    val fileName: String = name,
    val etaSeconds: Long = 0
)

@Serializable
data class SessionState(
    val lastTrackPath: String? = null,
    val lastTrackTitle: String? = null,
    val lastTrackArtist: String? = null,
    val lastTrackThumbnail: String? = null,
    val lastPosition: Long = 0,
    val isPlaying: Boolean = false,
    val queue: List<pEQMediaItem> = emptyList(),
    val isRotationLocked: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val favorites: List<String> = emptyList(),
    val accentColor: Int? = null,
    val eqGains: List<Float> = emptyList(),
    val eqQs: List<Float> = emptyList(),
    val lastIrPath: String? = null
)

@Serializable
data class Device(
    val name: String,
    val chip: String,
    val mode: String,
    val isFeatured: Boolean = true,
    val activeProfile: String = "Reference Profile",
    val sampleRate: Int = 44100,
    val isHighRes: Boolean = false
) {
    val deviceName: String get() = name
    
    // We can't easily serialize Color/ImageVector in commonMain without extra setup,
    // so we'll use a helper to get them based on name/type if needed, or just provide defaults.
    val color: Color get() = when(name) {
        "Pro Wired DAC" -> Color(0xFF3B82F6)
        "Wireless DAC Adapter" -> Color(0xFF22D3EE)
        "Universal USB DAC" -> Color(0xFF64748B)
        else -> Color(0xFFF59E0B)
    }
    
    val icon: ImageVector get() = when(name) {
        "Pro Wired DAC" -> Icons.Default.Headset
        "Wireless DAC Adapter" -> Icons.Default.BluetoothAudio
        "Universal USB DAC" -> Icons.Default.Usb
        else -> Icons.Default.Speaker
    }

    companion object {
        val HI_RES_DAC = Device("Pro Wired DAC", "Hi-Res Master", "Bit-Perfect", isHighRes = true, sampleRate = 192000)
        val WIRELESS_ADAPTER = Device("Wireless DAC Adapter", "Hi-Res 990kbps", "Wireless Mode", sampleRate = 96000)
        val GENERIC = Device("Universal USB DAC", "UAC 2.0 Driver", "High-Gain")
        val INTERNAL = Device("System Speakers", "Internal Audio", "Standard Mode")
        
        fun values() = listOf(INTERNAL, HI_RES_DAC, WIRELESS_ADAPTER, GENERIC)
    }
}

@Serializable
sealed class LyricsData {
    @Serializable
    data class Plain(val text: String) : LyricsData()
    
    @Serializable
    data class Synced(val lines: List<SyncedLine>) : LyricsData()
}

@Serializable
data class LrcLibResult(
    val id: Int? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Float? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)


@Serializable
data class AutoEqProfile(
    val name: String,
    val brand: String,
    val type: String,
    val parametricValues: Map<String, Float> = emptyMap()
)


@Serializable
data class BeatEvent(
    val soundId: String,
    val timestamp: Long
)

@Serializable
data class DrumKit(
    val name: String, 
    val labels: List<String>
)

@Serializable
data class DrumTutorial(
    val name: String, 
    val steps: List<List<String>> = emptyList()
)
