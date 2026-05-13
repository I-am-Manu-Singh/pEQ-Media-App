package org.audiocular.peq.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import org.audiocular.peq.app.widgets.NeuralGlanceWidget
import java.io.File
import java.io.FileOutputStream

class pEQMediaSessionService : MediaSessionService() {

    companion object {

        private const val CHANNEL_ID =
            "peq_media_session_channel"

        private const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "org.audiocular.peq.app.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "org.audiocular.peq.app.ACTION_NEXT"
        const val ACTION_PREV = "org.audiocular.peq.app.ACTION_PREV"
        const val ACTION_REPLAY_10 = "org.audiocular.peq.app.ACTION_REPLAY_10"
        const val ACTION_FORWARD_30 = "org.audiocular.peq.app.ACTION_FORWARD_30"
        const val ACTION_SHUFFLE = "org.audiocular.peq.app.ACTION_SHUFFLE"
        const val ACTION_FAVORITE = "org.audiocular.peq.app.ACTION_FAVORITE"
        const val ACTION_REPEAT = "org.audiocular.peq.app.ACTION_REPEAT"
    }

    /*
     * json
     */

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /*
     * scope
     */

    private val serviceScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

    private var widgetProgressJob: Job? = null

    /*
     * player/session
     */

    private lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    /*
     * create
     */

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

    /*
     * session
     */

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {

        return mediaSession
    }

    /*
     * commands
     */

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }

            ACTION_NEXT -> {
                player.seekToNext()
            }

            ACTION_PREV -> {
                player.seekToPrevious()
            }

            ACTION_REPLAY_10 -> {
                player.seekTo(maxOf(0L, player.currentPosition - 10000L))
            }

            ACTION_FORWARD_30 -> {
                player.seekTo(minOf(player.duration, player.currentPosition + 30000L))
            }

            ACTION_SHUFFLE -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                updateWidgetMetadata()
            }

            ACTION_FAVORITE -> {
                val prefs = getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
                val isFav = prefs.getBoolean("is_favorite", false)
                prefs.edit().putBoolean("is_favorite", !isFav).apply()
                updateWidgetMetadata()
            }

            ACTION_REPEAT -> {
                val next = (player.repeatMode + 1) % 4 // OFF, ALL, ONE, FOLDER
                player.repeatMode = next
                updateWidgetMetadata()
            }
        }

        refreshWidgets()

        return super.onStartCommand(
            intent,
            flags,
            startId
        )
    }

    /*
     * notification
     */

    private fun updateNotification() {

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    @OptIn(UnstableApi::class)
    private fun createNotification(): Notification {

        val launchIntent =
            Intent(
                this,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val prevIntent =
            Intent(
                this,
                pEQMediaSessionService::class.java
            ).apply {

                action =
            ACTION_PREV
            }

        val prevPending =
            PendingIntent.getService(
                this,
                10,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val playIntent =
            Intent(
                this,
                pEQMediaSessionService::class.java
            ).apply {

                action =
                    ACTION_PLAY_PAUSE
            }

        val playPending =
            PendingIntent.getService(
                this,
                11,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val nextIntent =
            Intent(
                this,
                pEQMediaSessionService::class.java
            ).apply {

                action =
                    ACTION_NEXT
            }

        val nextPending =
            PendingIntent.getService(
                this,
                12,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                R.drawable.ic_music
            )
            .setContentTitle(
                player.currentMediaItem
                    ?.mediaMetadata
                    ?.title
                    ?: "pEQ Neural"
            )
            .setContentText(
                player.currentMediaItem
                    ?.mediaMetadata
                    ?.artist
                    ?: "Neural Workstation"
            )
            .setContentIntent(
                contentIntent
            )
            .setOnlyAlertOnce(true)
            .setOngoing(player.isPlaying)
            .setCategory(
                NotificationCompat.CATEGORY_TRANSPORT
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .setVisibility(
                NotificationCompat.VISIBILITY_PUBLIC
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_replay_10,
                    "Replay 10s",
                    PendingIntent.getService(this, 13, Intent(this, pEQMediaSessionService::class.java).apply { action = ACTION_REPLAY_10 }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_prev,
                    "Previous",
                    prevPending
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (player.isPlaying) "Pause" else "Play",
                    playPending
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_next,
                    "Next",
                    nextPending
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_forward_30,
                    "Forward 30s",
                    PendingIntent.getService(this, 14, Intent(this, pEQMediaSessionService::class.java).apply { action = ACTION_FORWARD_30 }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_shuffle,
                    "Shuffle",
                    PendingIntent.getService(this, 15, Intent(this, pEQMediaSessionService::class.java).apply { action = ACTION_SHUFFLE }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_favorite,
                    "Favorite",
                    PendingIntent.getService(this, 16, Intent(this, pEQMediaSessionService::class.java).apply { action = ACTION_FAVORITE }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )
            )
            .setStyle(
                androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(mediaSession!!)
                    .setShowActionsInCompactView(1, 2, 3)
            )
            .build()
    }

    /*
     * channel
     */

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

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

    /*
     * progress updater
     */

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

    /*
     * metadata
     */

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

        val artworkData =
            metadata?.artworkData

        val artworkUri =
            metadata?.artworkUri

        var color1 =
            0xFF121212.toInt()

        var color2 =
            0xFF1A1A1A.toInt()

        val bitmap =
            artworkData?.let {

                BitmapFactory.decodeByteArray(
                    it,
                    0,
                    it.size
                )

            } ?: artworkUri?.let { uri ->

                try {

                    if (
                        uri.scheme == "content" ||
                        uri.scheme == "file"
                    ) {

                        contentResolver
                            .openInputStream(uri)
                            ?.use { stream ->

                                BitmapFactory.decodeStream(
                                    stream
                                )
                            }

                    } else {

                        null
                    }

                } catch (_: Exception) {

                    null
                }
            }

        bitmap?.let { bmp ->

            val palette =
                Palette.from(bmp)
                    .generate()

            color1 =
                palette.getDominantColor(
                    0xFF121212.toInt()
                )

            color2 =
                palette.getVibrantColor(
                    palette.getMutedColor(
                        color1
                    )
                )

            /*
             * save artwork
             */

            serviceScope.launch(
                Dispatchers.IO
            ) {

                try {

                    val file =
                        File(
                            filesDir,
                            "widget_art.png"
                        )

                    FileOutputStream(file).use { out ->

                        bmp.compress(
                            Bitmap.CompressFormat.PNG,
                            90,
                            out
                        )
                    }

                } catch (e: Exception) {

                    println(
                        "pEQControl: Widget Art Sync Failed: ${e.message}"
                    )
                }
            }
        }

        /*
         * queue serialization
         */

        val queueList =
            mutableListOf<String>()

        for (i in 1..3) {

            val nextIndex =
                player.currentMediaItemIndex + i

            if (
                nextIndex < player.mediaItemCount
            ) {

                val nextItem =
                    player.getMediaItemAt(nextIndex)

                queueList.add(
                    nextItem.mediaMetadata.title
                        ?.toString()
                        ?: "Unknown"
                )
            }
        }

        val queueJson =
            json.encodeToString(queueList)

        getSharedPreferences(
            "peq_widget_prefs",
            MODE_PRIVATE
        )
            .edit()
            .putString("last_track", title)
            .putString("last_artist", artist)
            .putInt("bg_color_1", color1)
            .putInt("bg_color_2", color2)
            .putBoolean("shuffle_enabled", player.shuffleModeEnabled)
            .putInt("repeat_mode", player.repeatMode)
            .putString("queue_json", queueJson)
            .apply()

        refreshWidgets()
    }

    /*
     * progress
     */

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
        )
            .edit()
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

    /*
     * playback state
     */

    private fun updateWidgetPlaybackState(
        isPlaying: Boolean
    ) {

        getSharedPreferences(
            "peq_widget_prefs",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "is_playing",
                isPlaying
            )
            .apply()

        refreshWidgets()
    }

    /*
     * widgets
     */

    private fun refreshWidgets() {
        serviceScope.launch {
            try {
                NeuralGlanceWidget()
                    .updateAll(
                        applicationContext
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /*
     * formatter
     */

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

    /*
     * player
     */

    private fun createPlayer(): ExoPlayer {

        return org.audiocular.peq.app.audio
            .AudioEngineProvider
            .getExoPlayer(this)
    }

    /*
     * destroy
     */

    override fun onDestroy() {

        stopWidgetProgressUpdater()

        mediaSession?.release()

        mediaSession = null

        serviceScope.cancel()

        stopForeground(
            STOP_FOREGROUND_REMOVE
        )

        super.onDestroy()
    }
}