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
import org.audiocular.peq.app.data.normalizePath

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
        const val ACTION_PLAY_INDEX = "org.audiocular.peq.app.ACTION_PLAY_INDEX"
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
        org.audiocular.peq.app.data.initPersistenceManager(applicationContext)
        createNotificationChannel()
        player = createPlayer()

        // Restore player state from saved session if empty
        try {
            val persistence = org.audiocular.peq.app.data.getPersistenceManager()
            val session = persistence.loadSession()
            if (session != null && player.mediaItemCount == 0) {
                val queue = session.queue
                if (queue.isNotEmpty()) {
                    val mediaItems = queue.map { item ->
                        val artBytes = if (item.thumbnail != null) {
                            try {
                                val uri = android.net.Uri.parse(item.thumbnail)
                                if (uri.scheme == "content" || uri.scheme == "file") {
                                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                } else {
                                    null
                                }
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        MediaItem.Builder()
                            .setUri(item.path)
                            .setMediaId(item.path)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(item.title)
                                    .setArtist(item.subtitle)
                                    .setArtworkUri(item.thumbnail?.let { android.net.Uri.parse(it) })
                                    .setArtworkData(artBytes)
                                    .build()
                            )
                            .build()
                    }
                    player.setMediaItems(mediaItems)
                    
                    val lastIndex = queue.indexOfFirst { normalizePath(it.path) == normalizePath(session.lastTrackPath) }.coerceAtLeast(0)
                    player.prepare()
                    player.seekTo(lastIndex, session.lastPosition)
                    player.shuffleModeEnabled = session.isShuffle
                    player.repeatMode = when (session.repeatMode) {
                        org.audiocular.peq.app.data.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                        org.audiocular.peq.app.data.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                } else {
                    val path = session.lastTrackPath
                    if (path != null) {
                        val artBytes = if (session.lastTrackThumbnail != null) {
                            try {
                                val uri = android.net.Uri.parse(session.lastTrackThumbnail)
                                if (uri.scheme == "content" || uri.scheme == "file") {
                                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                } else {
                                    null
                                }
                            } catch (_: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        val mediaItem = MediaItem.Builder()
                            .setUri(path)
                            .setMediaId(path)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(session.lastTrackTitle ?: "pEQ Track")
                                    .setArtist(session.lastTrackArtist ?: "Ready for Neural Input")
                                    .setArtworkUri(session.lastTrackThumbnail?.let { android.net.Uri.parse(it) })
                                    .setArtworkData(artBytes)
                                    .build()
                            )
                            .build()
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.seekTo(session.lastPosition)
                        player.shuffleModeEnabled = session.isShuffle
                        player.repeatMode = when (session.repeatMode) {
                            org.audiocular.peq.app.data.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                            org.audiocular.peq.app.data.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return command == Player.COMMAND_SEEK_TO_NEXT ||
                       command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                       command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                       command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                       super.isCommandAvailable(command)
            }

            override fun seekToNext() {
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_NEXT"))
            }

            override fun seekToNextMediaItem() {
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_NEXT"))
            }

            override fun seekToPrevious() {
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_PREV"))
            }

            override fun seekToPreviousMediaItem() {
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_PREV"))
            }
        }

        mediaSession =
            MediaSession.Builder(
                this,
                forwardingPlayer
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
                    persistSessionFromService()
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int
                ) {

                    updateWidgetMetadata()

                    updateWidgetProgress()

                    updateNotification()
                    persistSessionFromService()
                }

                override fun onMediaMetadataChanged(
                    mediaMetadata: MediaMetadata
                ) {

                    updateWidgetMetadata()

                    refreshWidgets()
                    persistSessionFromService()
                }

                override fun onPlaybackStateChanged(
                    playbackState: Int
                ) {

                    updateWidgetProgress()

                    updateNotification()
                    persistSessionFromService()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_shuffle", shuffleModeEnabled)
                        .apply()
                    refreshWidgets()
                    persistSessionFromService()
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
                        .edit()
                        .putInt("repeat_mode", repeatMode)
                        .apply()
                    refreshWidgets()
                    persistSessionFromService()
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
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else {
                    player.seekToNext()
                }
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_NEXT").apply { setPackage(packageName) })
            }

            ACTION_PREV -> {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else {
                    player.seekToPrevious()
                }
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_PREV").apply { setPackage(packageName) })
            }

            ACTION_REPLAY_10 -> {
                player.seekTo(maxOf(0L, player.currentPosition - 10000L))
            }

            ACTION_FORWARD_30 -> {
                player.seekTo(minOf(player.duration, player.currentPosition + 30000L))
            }

            ACTION_SHUFFLE -> {
                val newShuffle = !player.shuffleModeEnabled
                player.shuffleModeEnabled = newShuffle
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_SHUFFLE").apply {
                    setPackage(packageName)
                    putExtra("shuffle_enabled", newShuffle)
                })
            }

            ACTION_FAVORITE -> {
                val currentMediaItem = player.currentMediaItem
                val path = currentMediaItem?.localConfiguration?.uri?.toString()
                if (path != null) {
                    val normalizedPath = normalizePath(path)
                    val persistence = org.audiocular.peq.app.data.getPersistenceManager()
                    val favoritesList = persistence.loadFavorites().toMutableList()
                    val existing = favoritesList.find { normalizePath(it) == normalizedPath }
                    val newFavoriteStatus = if (existing != null) {
                        favoritesList.remove(existing)
                        false
                    } else {
                        favoritesList.add(path)
                        true
                    }
                    persistence.saveFavorites(favoritesList)
                    getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_favorite", newFavoriteStatus)
                        .apply()
                }
                updateWidgetMetadata()
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_FAVORITE").apply { setPackage(packageName) })
            }

            ACTION_REPEAT -> {
                val newRepeat = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                player.repeatMode = newRepeat
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_REPEAT").apply {
                    setPackage(packageName)
                    putExtra("repeat_mode", newRepeat)
                })
            }
            
            ACTION_PLAY_INDEX -> {
                val index = intent.getIntExtra("play_index", -1)
                if (index >= 0 && index < player.mediaItemCount) {
                    player.seekTo(index, 0L)
                    player.play()
                }
                sendBroadcast(Intent("org.audiocular.peq.app.ACTION_SYSTEM_PLAY_INDEX").apply {
                    setPackage(packageName)
                    putExtra("play_index", index)
                })
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

        val metadata = player.currentMediaItem?.mediaMetadata
        val artworkData = metadata?.artworkData
        val artworkUri = metadata?.artworkUri

        val artworkBitmap = artworkData?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } catch (_: Exception) { null }
        } ?: artworkUri?.let { uri ->
            try {
                if (uri.scheme == "content" || uri.scheme == "file") {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } else null
            } catch (_: Exception) { null }
        }

        return NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(
                R.drawable.ic_music
            )
            .setLargeIcon(artworkBitmap)
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
            .setOngoing(true)
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
                    delay(1500)
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

        if (bitmap != null) {
            val bmp = bitmap

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
                    refreshWidgets()

                } catch (e: Exception) {

                    println(
                        "pEQControl: Widget Art Sync Failed: ${e.message}"
                    )
                }
            }
        } else {
            serviceScope.launch(
                Dispatchers.IO
            ) {
                try {
                    val file = File(filesDir, "widget_art.png")
                    if (file.exists()) {
                        file.delete()
                    }
                    refreshWidgets()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        /*
         * queue serialization
         */

        val queueList = mutableListOf<String>()
        for (i in 0 until player.mediaItemCount) {
            val item = player.getMediaItemAt(i)
            val title = item.mediaMetadata.title?.toString() ?: "Unknown"
            queueList.add("$title|$i")
        }

        val queueJson =
            json.encodeToString(queueList)

        val path = mediaItem?.localConfiguration?.uri?.toString() ?: ""
        val isFavorite = if (path.isNotEmpty()) {
            val normalizedPath = normalizePath(path)
            org.audiocular.peq.app.data.getPersistenceManager().loadFavorites().any {
                normalizePath(it) == normalizedPath
            }
        } else {
            false
        }

        loadLyricsForCurrentTrack()
        val pos = player.currentPosition
        val idx = currentTrackLyrics.indexOfLast { it.first <= pos }.coerceAtLeast(0)
        val currentLyric = currentTrackLyrics.getOrNull(idx)?.second ?: ""
        val nextLyric = currentTrackLyrics.getOrNull(idx + 1)?.second ?: ""

        getSharedPreferences(
            "peq_widget_prefs",
            MODE_PRIVATE
        )
            .edit()
            .putString("last_track", title)
            .putString("last_artist", artist)
            .putString("last_path", path)
            .putBoolean("is_favorite", isFavorite)
            .putInt("bg_color_1", color1)
            .putInt("bg_color_2", color2)
            .putBoolean("shuffle_enabled", player.shuffleModeEnabled)
            .putBoolean("is_shuffle", player.shuffleModeEnabled)
            .putInt("repeat_mode", player.repeatMode)
            .putString("queue_json", queueJson)
            .putString("current_lyric", currentLyric)
            .putString("next_lyric", nextLyric)
            .apply()

        refreshWidgets()
    }

    /*
     * progress
     */

    private fun updateWidgetProgress() {
        val durationMs = if (player.duration == C.TIME_UNSET) 0L else player.duration
        val currentMs = player.currentPosition

        val progress = if (durationMs > 0) {
            ((currentMs * 100L) / durationMs).toInt()
        } else {
            0
        }.coerceIn(0, 100)

        loadLyricsForCurrentTrack()
        val idx = currentTrackLyrics.indexOfLast { it.first <= currentMs }.coerceAtLeast(0)
        val currentLyric = currentTrackLyrics.getOrNull(idx)?.second ?: ""
        val nextLyric = currentTrackLyrics.getOrNull(idx + 1)?.second ?: ""

        val prefs = getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
        val oldCurrentLyric = prefs.getString("current_lyric", "") ?: ""
        val oldNextLyric = prefs.getString("next_lyric", "") ?: ""
        val oldPlaying = prefs.getBoolean("is_playing", false)
        val oldProgress = prefs.getInt("progress", 0)

        val shouldRefresh = oldCurrentLyric != currentLyric || 
                            oldNextLyric != nextLyric || 
                            oldPlaying != player.isPlaying ||
                            Math.abs(oldProgress - progress) >= 5

        prefs.edit()
            .putInt("progress", progress)
            .putString("current_time", formatTime(currentMs))
            .putString("duration", formatTime(durationMs))
            .putBoolean("is_playing", player.isPlaying)
            .putString("current_lyric", currentLyric)
            .putString("next_lyric", nextLyric)
            .apply()

        // Sync with Glance state immediately
        val track = prefs.getString("last_track", "pEQ Station") ?: "pEQ Station"
        val artist = prefs.getString("last_artist", "Ready for Neural Input") ?: "Ready for Neural Input"
        val repeatMode = prefs.getInt("repeat_mode", 0)
        val favorite = prefs.getBoolean("is_favorite", false)
        val isShuffle = prefs.getBoolean("is_shuffle", false)
        val queueJson = prefs.getString("queue_json", "[]") ?: "[]"
        val queue = try { json.decodeFromString<List<String>>(queueJson) } catch (_: Exception) { emptyList() }
        val bgColor1 = prefs.getInt("bg_color_1", 0xFF08090D.toInt())
        val bgColor2 = prefs.getInt("bg_color_2", 0xFF8A7DFF.toInt())

        if (shouldRefresh) {
            serviceScope.launch {
                try {
                    org.audiocular.peq.app.widgets.NeuralWidgetStateSync.update(
                        context = applicationContext,
                        track = track,
                        artist = artist,
                        isPlaying = player.isPlaying,
                        progress = progress,
                        currentTime = formatTime(currentMs),
                        duration = formatTime(durationMs),
                        repeatMode = repeatMode,
                        favorite = favorite,
                        isShuffle = isShuffle,
                        queue = queue,
                        bgColor1 = bgColor1,
                        bgColor2 = bgColor2,
                        currentLyric = currentLyric,
                        nextLyric = nextLyric
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /*
     * playback state
     */

    private fun updateWidgetPlaybackState(
        isPlaying: Boolean
    ) {
        println("pEQControl: [WIDGET_SERVICE] updateWidgetPlaybackState: isPlaying=$isPlaying")
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
        val prefs = getSharedPreferences("peq_widget_prefs", MODE_PRIVATE)
        val track = prefs.getString("last_track", "pEQ Station") ?: "pEQ Station"
        val artist = prefs.getString("last_artist", "Ready for Neural Input") ?: "Ready for Neural Input"
        val isPlaying = prefs.getBoolean("is_playing", false)
        val progress = prefs.getInt("progress", 0)
        val currentTime = prefs.getString("current_time", "0:00") ?: "0:00"
        val duration = prefs.getString("duration", "0:00") ?: "0:00"
        val repeatMode = prefs.getInt("repeat_mode", 0)
        val favorite = prefs.getBoolean("is_favorite", false)
        val isShuffle = prefs.getBoolean("is_shuffle", false)
        val queueJson = prefs.getString("queue_json", "[]") ?: "[]"
        val queue = try { json.decodeFromString<List<String>>(queueJson) } catch (_: Exception) { emptyList() }
        val bgColor1 = prefs.getInt("bg_color_1", 0xFF08090D.toInt())
        val bgColor2 = prefs.getInt("bg_color_2", 0xFF8A7DFF.toInt())
        val currentLyric = prefs.getString("current_lyric", "") ?: ""
        val nextLyric = prefs.getString("next_lyric", "") ?: ""

        serviceScope.launch {
            try {
                org.audiocular.peq.app.widgets.NeuralWidgetStateSync.update(
                    context = applicationContext,
                    track = track,
                    artist = artist,
                    isPlaying = isPlaying,
                    progress = progress,
                    currentTime = currentTime,
                    duration = duration,
                    repeatMode = repeatMode,
                    favorite = favorite,
                    isShuffle = isShuffle,
                    queue = queue,
                    bgColor1 = bgColor1,
                    bgColor2 = bgColor2,
                    currentLyric = currentLyric,
                    nextLyric = nextLyric
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

    private var currentTrackLyrics: List<Pair<Long, String>> = emptyList()
    private var lastLoadedLyricsPath: String? = null

    private fun loadLyricsForCurrentTrack() {
        val path = player.currentMediaItem?.localConfiguration?.uri?.toString() ?: return
        if (path == lastLoadedLyricsPath) return
        lastLoadedLyricsPath = path
        currentTrackLyrics = emptyList()
        
        try {
            val persistence = org.audiocular.peq.app.data.getPersistenceManager()
            val session = persistence.loadSession()
            val item = session?.queue?.find { it.path == path } 
                ?: persistence.loadFavorites().find { it == path }?.let { session?.queue?.find { q -> q.path == it } }
            
            var lyricsText = item?.lyrics
            if (lyricsText.isNullOrBlank() && !path.startsWith("http")) {
                // FALLBACK: Read directly from the file path
                val normalizedPath = normalizePath(path)
                try {
                    val file = java.io.File(normalizedPath)
                    if (file.exists()) {
                        val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                        lyricsText = audioFile.tag?.getFirst(org.jaudiotagger.tag.FieldKey.LYRICS)
                    }
                } catch (_: Exception) {}

                if (lyricsText.isNullOrBlank()) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(normalizedPath)
                        lyricsText = retriever.extractMetadata(31) // METADATA_KEY_LYRICS
                            ?: retriever.extractMetadata(23)
                            ?: retriever.extractMetadata(101)
                        retriever.release()
                    } catch (_: Exception) {}
                }
            }
            if (!lyricsText.isNullOrBlank()) {
                val lines = mutableListOf<Pair<Long, String>>()
                for (line in lyricsText.lines()) {
                    if (line.isBlank()) continue
                    // Match [mm:ss.xx] or [mm:ss:xx] or [mm:ss]
                    val regex = "\\[(\\d+):(\\d+)(?:[\\.:](\\d+))?\\](.*)".toRegex()
                    val match = regex.find(line)
                    if (match != null) {
                        val min = match.groupValues[1].toLongOrNull() ?: 0L
                        val sec = match.groupValues[2].toLongOrNull() ?: 0L
                        val ms = match.groupValues[3].toLongOrNull() ?: 0L
                        val text = match.groupValues[4].trim()
                        val timestamp = (min * 60 * 1000) + (sec * 1000) + (if (ms < 100) ms * 10 else ms)
                        lines.add(Pair(timestamp, text))
                    } else if (line.trim().isNotEmpty() && !line.startsWith("[")) {
                        lines.add(Pair(lines.size * 5000L, line.trim()))
                    }
                }
                currentTrackLyrics = lines.sortedBy { it.first }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistSessionFromService() {
        try {
            val persistence = org.audiocular.peq.app.data.getPersistenceManager()
            val session = persistence.loadSession() ?: org.audiocular.peq.app.data.SessionState()
            
            val mediaItem = player.currentMediaItem
            val path = mediaItem?.localConfiguration?.uri?.toString() ?: session.lastTrackPath
            val title = mediaItem?.mediaMetadata?.title?.toString() ?: session.lastTrackTitle
            val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: session.lastTrackArtist
            val thumbnail = mediaItem?.mediaMetadata?.artworkUri?.toString() ?: session.lastTrackThumbnail
            
            val updatedSession = session.copy(
                lastTrackPath = path,
                lastTrackTitle = title,
                lastTrackArtist = artist,
                lastTrackThumbnail = thumbnail,
                lastPosition = player.currentPosition,
                isPlaying = player.isPlaying,
                isShuffle = player.shuffleModeEnabled,
                repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_ONE -> org.audiocular.peq.app.data.RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> org.audiocular.peq.app.data.RepeatMode.ALL
                    else -> org.audiocular.peq.app.data.RepeatMode.OFF
                }
            )
            persistence.saveSession(updatedSession)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
     * destroy
     */

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_ENDED || player.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        persistSessionFromService()
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