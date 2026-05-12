package org.audiocular.peq.app.receivers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.OptIn
import kotlin.math.min
import androidx.media3.common.util.UnstableApi
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import org.audiocular.peq.app.services.pEQMediaSessionService
import java.io.File

class pEQWidgetProvider : AppWidgetProvider() {

    companion object {

        const val ACTION_PLAY_PAUSE =
            "org.audiocular.peq.app.ACTION_PLAY_PAUSE"

        const val ACTION_NEXT =
            "org.audiocular.peq.app.ACTION_NEXT"

        const val ACTION_PREV =
            "org.audiocular.peq.app.ACTION_PREV"

        const val ACTION_REPLAY_10 =
            "org.audiocular.peq.app.ACTION_REPLAY_10"

        const val ACTION_FORWARD_30 =
            "org.audiocular.peq.app.ACTION_FORWARD_30"

        const val ACTION_SHUFFLE =
            "org.audiocular.peq.app.ACTION_SHUFFLE"

        const val ACTION_FAVORITE =
            "org.audiocular.peq.app.ACTION_FAVORITE"
    }

    // =========================================================
    // UPDATE
    // =========================================================

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        appWidgetIds.forEach { widgetId ->

            updateAppWidget(
                context,
                appWidgetManager,
                widgetId
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {

        super.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            appWidgetId,
            newOptions
        )

        updateAppWidget(
            context,
            appWidgetManager,
            appWidgetId
        )
    }

    // =========================================================
    // MAIN UPDATE
    // =========================================================

    @SuppressLint("RemoteViewLayout")
    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {

        val options = appWidgetManager
            .getAppWidgetOptions(appWidgetId)

        val minWidth = options.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        )

        val minHeight = options.getInt(
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        )
        val isUltraThin =
            minHeight < 70

        val isSquare =
            minWidth in 180..260 &&
                    minHeight in 180..260

        val isLarge =
            minWidth >= 260 &&
                    minHeight >= 260

        val isMedium =
            minWidth >= 180 &&
                    minHeight >= 110

        val layoutId = when {

            isUltraThin -> {
                R.layout.peq_widget_thin
            }

            isSquare -> {
                R.layout.peq_widget_square
            }

            isLarge -> {
                R.layout.peq_widget_large
            }

            isMedium -> {
                R.layout.peq_widget_medium
            }

            else -> {
                R.layout.peq_widget_small
            }
        }

        val views = RemoteViews(
            context.packageName,
            layoutId
        )

        val mediaPrefs = context.getSharedPreferences(
            "peq_widget_prefs",
            Context.MODE_PRIVATE
        )

        // =====================================================
        // MEDIA DATA
        // =====================================================

        val title = mediaPrefs.getString(
            "last_track",
            "pEQ Neural"
        ) ?: "pEQ Neural"

        val artist = mediaPrefs.getString(
            "last_artist",
            "Playback Ready"
        ) ?: "Playback Ready"

        val currentTime = mediaPrefs.getString(
            "current_time",
            "0:00"
        ) ?: "0:00"

        val duration = mediaPrefs.getString(
            "duration",
            "0:00"
        ) ?: "0:00"

        val progress = mediaPrefs.getInt(
            "progress",
            0
        ).coerceIn(0, 100)

        val isPlaying = mediaPrefs.getBoolean(
            "is_playing",
            false
        )

        val isFavorite = mediaPrefs.getBoolean(
            "is_favorite",
            false
        )

        val shuffleEnabled = mediaPrefs.getBoolean(
            "shuffle_enabled",
            false
        )

        val artPath = mediaPrefs.getString(
            "last_art_path",
            null
        )

        val bgColor1 = mediaPrefs.getInt("bg_color_1", 0xFF121212.toInt())
        val accentColor = mediaPrefs.getInt("bg_color_2", 0xFF1A1A1A.toInt())

        // Apply dynamic background color with transparency
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor1)
        views.setInt(R.id.widget_title, "setTextColor", accentColor)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(R.id.widget_progress, "setProgressTintList", android.content.res.ColorStateList.valueOf(accentColor))
        }

        // =====================================================
        // SETTINGS
        // =====================================================

        val settings = context.getSharedPreferences(
            "peq_widget_settings_$appWidgetId",
            Context.MODE_PRIVATE
        )

        val showTitle = settings.getBoolean(
            "show_title",
            true
        )

        val showArtist = settings.getBoolean(
            "show_artist",
            true
        )

        val showArt = settings.getBoolean(
            "show_art",
            true
        )

        val showFavorite = settings.getBoolean(
            "show_favorite",
            true
        )

        val showShuffle = settings.getBoolean(
            "show_shuffle",
            true
        )

        // =====================================================
        // VISIBILITY
        // =====================================================

        views.setViewVisibility(
            R.id.widget_title,
            if (showTitle) View.VISIBLE else View.GONE
        )

        views.setViewVisibility(
            R.id.widget_artist,
            if (showArtist) View.VISIBLE else View.GONE
        )

        views.setViewVisibility(
            R.id.art_container,
            if (showArt) View.VISIBLE else View.GONE
        )

        views.setViewVisibility(
            R.id.widget_favorite,
            if (showFavorite) View.VISIBLE else View.GONE
        )

        views.setViewVisibility(
            R.id.widget_shuffle,
            if (showShuffle) View.VISIBLE else View.GONE
        )

        // =====================================================
        // TEXT
        // =====================================================

        views.setTextViewText(
            R.id.widget_title,
            title
        )

        views.setTextViewText(
            R.id.widget_artist,
            artist
        )

        views.setTextViewText(
            R.id.widget_current_time,
            currentTime
        )

        views.setTextViewText(
            R.id.widget_duration,
            duration
        )

        // =====================================================
        // PROGRESS
        // =====================================================

        views.setProgressBar(
            R.id.widget_progress,
            100,
            progress,
            false
        )

        if (layoutId == R.layout.peq_widget_small) {

            try {
                views.setViewVisibility(
                    R.id.widget_artist,
                    View.GONE
                )
            } catch (_: Exception) {
            }

            try {
                views.setViewVisibility(
                    R.id.widget_shuffle,
                    View.GONE
                )
            } catch (_: Exception) {
            }

            try {
                views.setViewVisibility(
                    R.id.widget_favorite,
                    View.GONE
                )
            } catch (_: Exception) {
            }
        }

        // =====================================================
        // PLAY / PAUSE
        // =====================================================

        views.setImageViewResource(
            R.id.widget_play_pause,
            if (isPlaying) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
        )

        // =====================================================
        // FAVORITE
        // =====================================================

        views.setImageViewResource(
            R.id.widget_favorite,
            if (isFavorite) {
                R.drawable.ic_favorite
            } else {
                R.drawable.ic_favorite_outline
            }
        )

        // =====================================================
        // SHUFFLE COLOR
        // =====================================================

        views.setInt(
            R.id.widget_shuffle,
            "setColorFilter",
            if (shuffleEnabled) {
                Color.WHITE
            } else {
                Color.parseColor("#FFD600")
            }
        )

        // =====================================================
        // ALBUM ART
        // =====================================================

        val fallbackArt = R.drawable.ic_music

        if (showArt && !artPath.isNullOrBlank()) {

            try {

                val bitmap = when {

                    artPath.startsWith("content://") -> {

                        context.contentResolver
                            .openInputStream(Uri.parse(artPath))
                            ?.use { stream ->

                                val options = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.RGB_565
                                    inSampleSize = 2
                                }

                                BitmapFactory.decodeStream(
                                    stream,
                                    null,
                                    options
                                )
                            }
                    }

                    artPath.startsWith("file://") -> {

                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                            inSampleSize = 2
                        }

                        BitmapFactory.decodeFile(
                            artPath.removePrefix("file://"),
                            options
                        )
                    }

                    else -> {

                        val file = File(artPath)

                        if (file.exists()) {

                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.RGB_565
                                inSampleSize = 2
                            }

                            BitmapFactory.decodeFile(
                                artPath,
                                options
                            )

                        } else {

                            null
                        }
                    }
                }

                if (
                    bitmap != null &&
                    bitmap.width > 0 &&
                    bitmap.height > 0
                ) {

                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        320,
                        320,
                        true
                    )

                    val roundedBitmap = getRoundedBitmap(scaledBitmap)

                    if (
                        bitmap != scaledBitmap &&
                        !bitmap.isRecycled
                    ) {
                        bitmap.recycle()
                    }

                    views.setImageViewBitmap(
                        R.id.widget_album_art,
                        roundedBitmap
                    )

                } else {

                    views.setImageViewResource(
                        R.id.widget_album_art,
                        fallbackArt
                    )
                }

            } catch (_: Exception) {

                views.setImageViewResource(
                    R.id.widget_album_art,
                    fallbackArt
                )
            }

        } else {

            views.setImageViewResource(
                R.id.widget_album_art,
                fallbackArt
            )
        }

        // =====================================================
        // BUTTONS
        // =====================================================

        bindClick(
            views,
            R.id.widget_play_pause,
            context,
            ACTION_PLAY_PAUSE
        )

        bindClick(
            views,
            R.id.widget_next,
            context,
            ACTION_NEXT
        )

        bindClick(
            views,
            R.id.widget_prev,
            context,
            ACTION_PREV
        )

        bindClick(
            views,
            R.id.widget_shuffle,
            context,
            ACTION_SHUFFLE
        )

        bindClick(
            views,
            R.id.widget_favorite,
            context,
            ACTION_FAVORITE
        )

        // =====================================================
        // OPEN APP
        // =====================================================

        val launchIntent =
            getLaunchAppPendingIntent(context)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            launchIntent
        )

        views.setOnClickPendingIntent(
            R.id.widget_title,
            launchIntent
        )

        views.setOnClickPendingIntent(
            R.id.widget_album_art,
            launchIntent
        )

        // =====================================================
        // UPDATE
        // =====================================================

        try {

            appWidgetManager.partiallyUpdateAppWidget(
                appWidgetId,
                views
            )

        } catch (_: Exception) {
        }

        appWidgetManager.updateAppWidget(
            appWidgetId,
            views
        )
    }

    // =========================================================
    // BUTTON HELPER
    // =========================================================

    private fun bindClick(
        views: RemoteViews,
        viewId: Int,
        context: Context,
        action: String
    ) {

        views.setOnClickPendingIntent(
            viewId,
            getPendingServiceIntent(
                context,
                action
            )
        )
    }

    // =========================================================
    // OPEN APP
    // =========================================================

    private fun getLaunchAppPendingIntent(
        context: Context
    ): PendingIntent {

        val intent = Intent(
            context,
            MainActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

            `package` = context.packageName
        }

        return PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    @OptIn(UnstableApi::class)
    private fun getPendingServiceIntent(
        context: Context,
        action: String
    ): PendingIntent {

        val intent = Intent(
            context,
            pEQMediaSessionService::class.java
        ).apply {

            this.action = action

            `package` = context.packageName

            component = ComponentName(
                context,
                pEQMediaSessionService::class.java
            )
        }

        val requestCode = when (action) {

            ACTION_PLAY_PAUSE -> 1001
            ACTION_NEXT -> 1002
            ACTION_PREV -> 1003
            ACTION_SHUFFLE -> 1004
            ACTION_FAVORITE -> 1005
            ACTION_FORWARD_30 -> 1006
            ACTION_REPLAY_10 -> 1007

            else -> action.hashCode()
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            PendingIntent.getForegroundService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        } else {

            PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    // =========================================================
    // ROUNDED BITMAP
    // =========================================================

    private fun getRoundedBitmap(
        bitmap: Bitmap
    ): Bitmap {

        if (
            bitmap.width <= 0 ||
            bitmap.height <= 0
        ) {
            return bitmap
        }

        val size = min(
            bitmap.width,
            bitmap.height
        )

        val output = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val rect = Rect(
            0,
            0,
            size,
            size
        )

        val rectF = RectF(rect)

        val radius = size * 0.18f

        canvas.drawARGB(
            0,
            0,
            0,
            0
        )

        canvas.drawRoundRect(
            rectF,
            radius,
            radius,
            paint
        )

        paint.xfermode = PorterDuffXfermode(
            PorterDuff.Mode.SRC_IN
        )

        val left =
            (bitmap.width - size) / 2

        val top =
            (bitmap.height - size) / 2

        canvas.drawBitmap(
            bitmap,
            Rect(
                left,
                top,
                left + size,
                top + size
            ),
            rect,
            paint
        )

        if (
            bitmap != output &&
            !bitmap.isRecycled
        ) {
            bitmap.recycle()
        }

        return output
    }
}