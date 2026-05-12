package org.audiocular.peq.app.cast

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.common.MediaItem
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.audiocular.peq.app.cast.pEQCastEngine as IpEQCastEngine

/**
 * pEQ CAST ENGINE: PRODUCTION BROADCAST HUB
 * Manages the physical handshake between the workstation and remote Cast monitors.
 */
class AndroidpEQCastEngine(private val context: Context) : IpEQCastEngine {
    private var castContext: CastContext? = null
    private var castPlayer: CastPlayer? = null
    
    private val _isCasting = MutableStateFlow(false)
    override val isCasting: StateFlow<Boolean> = _isCasting

    fun initialize() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castPlayer = CastPlayer(castContext!!)
            
            castPlayer?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isCasting.value = isPlaying
                }
            })
        } catch (e: Exception) {
            println("pEQControl: [CAST] Cast Initialization Failed: ${e.message}")
        }
    }

    override fun castMedia(url: String, title: String) {
        castPlayer?.let { player ->
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(title).build())
                .build()
            
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isCasting.value = true
        }
    }

    override fun stopCasting() {
        castPlayer?.stop()
        _isCasting.value = false
    }

    fun release() {
        castPlayer?.release()
    }
}
