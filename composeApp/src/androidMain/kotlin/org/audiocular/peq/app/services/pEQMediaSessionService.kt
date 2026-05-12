package org.audiocular.peq.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.*
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import org.audiocular.peq.app.receivers.pEQWidgetProvider
import org.audiocular.peq.app.widgets.NeuralGlanceWidget
import androidx.glance.appwidget.updateAll
import androidx.palette.graphics.Palette
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

class pEQMediaSessionService : MediaSessionService() {

    companion object {

        private const val CHANNEL_ID =
            "peq_media_session_channel"

        private const val NOTIFICATION_ID = 1001
    }

    // =========================================================
    // SCOPE
    // =========================================================

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

    private var widgetProgressJob: Job? = null

    // =========================================================
    // PLAYER / SESSION
    // =========================================================

    private lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate() {

        super.onCreate()

        createNotificationChannel()

        player = createPlayer()

        mediaSession =
            MediaSession.Builder(
                this,
                player
            ).build()

        updateWidgetMetadata()

        player.addListener(

            object : Player.Listener {

                override fun onIsPlayingChanged(
                    isPlaying: Boolean
                ) {

                    updateWidgetPlaybackState(
                        isPlaying
                    )

                    updateNotification()

                    if (isPlaying) {

                        startWidgetProgressUpdater()

                    } else {

                        stopWidgetProgressUpdater()
                    }
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int
                ) {

                    updateWidgetMetadata()

                    updateWidgetProgress()

                    updateNotification()
                }

                override fun onMediaMetadataChanged(
                    mediaMetadata: MediaMetadata
                ) {

                    updateWidgetMetadata()

                    refreshWidgets()
                }

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    updateWidgetProgress()

                    updateNotification()
                }
            }
        )

        startForeground(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    // =========================================================
    // SESSION
    // =========================================================

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    // =========================================================
    // FOREGROUND
    // =========================================================

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            pEQWidgetProvider.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            pEQWidgetProvider.ACTION_NEXT -> {
                player.seekToNext()
            }
            pEQWidgetProvider.ACTION_PREV -> {
                player.seekToPrevious()
            }
            pEQWidgetProvider.ACTION_FAVORITE -> {
                val prefs = getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
                val isFav = prefs.getBoolean("is_favorite", false)
                prefs.edit().putBoolean("is_favorite", !isFav).apply()
            }
        }
        refreshWidgets()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification() {

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    @OptIn(UnstableApi::class)
    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, pEQMediaSessionService::class.java).apply { action = pEQWidgetProvider.ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playIntent = Intent(this, pEQMediaSessionService::class.java).apply { action = pEQWidgetProvider.ACTION_PLAY_PAUSE }
        val playPending = PendingIntent.getService(this, 11, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, pEQMediaSessionService::class.java).apply { action = pEQWidgetProvider.ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 12, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(player.currentMediaItem?.mediaMetadata?.title ?: "pEQ Neural")
            .setContentText(player.currentMediaItem?.mediaMetadata?.artist ?: "Neural Workstation")
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(player.isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(NotificationCompat.Action(R.drawable.ic_prev, "Previous", prevPending))
            .addAction(
                NotificationCompat.Action(
                    if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (player.isPlaying) "Pause" else "Play",
                    playPending
                )
            )
            .addAction(NotificationCompat.Action(R.drawable.ic_next, "Next", nextPending))
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    // =========================================================
    // CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "pEQ Media Session",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    // =========================================================
    // LIVE WIDGET UPDATER
    // =========================================================

    private fun startWidgetProgressUpdater() {

        if (
            widgetProgressJob?.isActive == true
        ) return

        widgetProgressJob =
            serviceScope.launch {

                while (isActive) {

                    updateWidgetProgress()

                    delay(750)
                }
            }
    }

    private fun stopWidgetProgressUpdater() {

        widgetProgressJob?.cancel()

        widgetProgressJob = null
    }

    // =========================================================
    // UPDATE METADATA
    // =========================================================

    private fun updateWidgetMetadata() {

        val mediaItem =
            player.currentMediaItem

        val metadata =
            mediaItem?.mediaMetadata

        val title =
            metadata?.title
                ?.toString()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "pEQ Neural"

        val artist =
            metadata?.artist
                ?.toString()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Playback Ready"

        val artworkData = metadata?.artworkData
        val artworkUri = metadata?.artworkUri
        
        var color1 = 0xFF121212.toInt() // Default Dark
        var color2 = 0xFF1A1A1A.toInt() // Default Slightly Lighter

        val bitmap = artworkData?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        } ?: artworkUri?.let { uri ->
            try {
                if (uri.scheme == "content" || uri.scheme == "file") {
                    val stream = contentResolver.openInputStream(uri)
                    val b = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    b
                } else null
            } catch (e: Exception) { null }
        }

        bitmap?.let {
            val palette = Palette.from(it).generate()
            color1 = palette.getDominantColor(0xFF121212.toInt())
            color2 = palette.getVibrantColor(palette.getMutedColor(color1))
            
            // Apply slight darkening for readability if needed
            // (Optional: can add logic here)
        }

        getSharedPreferences(
            "peq_widget_prefs",
            MODE_PRIVATE
        ).edit()
            .putString("last_track", title)
            .putString("last_artist", artist)
            .putInt("bg_color_1", color1)
            .putInt("bg_color_2", color2)
            .apply()

        refreshWidgets()
    }

    // =========================================================
    // UPDATE PROGRESS
    // =========================================================

    private fun updateWidgetProgress() {

        val durationMs =
            if (
                player.duration == C.TIME_UNSET
            ) {
                0L
            } else {
                player.duration
            }

        val currentMs =
            player.currentPosition

        val progress =
            if (durationMs > 0) {

                (
                        (currentMs * 100L) /
                                durationMs
                        ).toInt()

            } else {

                0
            }.coerceIn(0, 100)

        getSharedPreferences(
            "peq_widget_prefs",
            MODE_PRIVATE
        ).edit()
            .putInt(
                "progress",
                progress
            )
            .putString(
                "current_time",
                formatTime(currentMs)
            )
            .putString(
                "duration",
                formatTime(durationMs)
            )
            .putBoolean(
                "is_playing",
                player.isPlaying
            )
            .apply()

        refreshWidgets()
    }

    // =========================================================
    // PLAYBACK STATE
    // =========================================================

    private fun updateWidgetPlaybackState(
        isPlaying: Boolean
    ) {

        getSharedPreferences(
            "peq_widget_prefs",
            MODE_PRIVATE
        ).edit()
            .putBoolean(
                "is_playing",
                isPlaying
            )
            .apply()

        refreshWidgets()
    }

    // =========================================================
    // REFRESH WIDGETS
    // =========================================================

    private fun refreshWidgets() {
        // 🚀 Legacy XML Widgets Refresh
        val manager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, pEQWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(componentName)
        
        if (ids.isNotEmpty()) {
            val provider = pEQWidgetProvider()
            ids.forEach { id ->
                provider.updateAppWidget(this, manager, id)
            }
        }

        // 🚀 Modern Glance Widgets Refresh
        serviceScope.launch {
            try {
                NeuralGlanceWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =========================================================
    // FORMAT
    // =========================================================

    private fun formatTime(
        ms: Long
    ): String {

        if (ms <= 0L) {
            return "0:00"
        }

        val totalSeconds =
            ms / 1000

        val minutes =
            totalSeconds / 60

        val seconds =
            totalSeconds % 60

        return String.format(
            "%d:%02d",
            minutes,
            seconds
        )
    }

    // =========================================================
    // PLAYER
    // =========================================================

    private fun createPlayer(): ExoPlayer {
        return org.audiocular.peq.app.audio.AudioEngineProvider.getExoPlayer(this)
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        stopWidgetProgressUpdater()

        mediaSession?.release()

        mediaSession = null

        // player.release() // Managed by Provider
        serviceScope.cancel()

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        super.onDestroy()
    }
}