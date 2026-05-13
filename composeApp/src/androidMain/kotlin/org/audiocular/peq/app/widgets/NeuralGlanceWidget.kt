/*
 * pEQ Neural Widget
 * Humans kept shrinking music players until they became decorative jewelry.
 * So here. Tiny cyberpunk altar for your audio obsession.
 */

package org.audiocular.peq.app.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.glance.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.ColorFilter
import androidx.glance.ImageProvider
import androidx.glance.action.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.*
import androidx.glance.appwidget.lazy.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import androidx.compose.ui.graphics.Color as ComposeColor

private const val ACTION_PLAY_PAUSE =
    "org.audiocular.peq.app.ACTION_PLAY_PAUSE"

private const val ACTION_NEXT =
    "org.audiocular.peq.app.ACTION_NEXT"

private const val ACTION_PREV =
    "org.audiocular.peq.app.ACTION_PREV"

private const val ACTION_REPEAT =
    "org.audiocular.peq.app.ACTION_REPEAT"

private const val ACTION_FAVORITE =
    "org.audiocular.peq.app.ACTION_FAVORITE"

class NeuralGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 120.dp),
            DpSize(240.dp, 120.dp),
            DpSize(300.dp, 160.dp),
            DpSize(320.dp, 260.dp)
        )
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        provideContent {
            NeuralWidgetUI(context)
        }
    }

    @Composable
    private fun NeuralWidgetUI(
        context: Context
    ) {

        val size = LocalSize.current

        val compact = size.width < 180.dp
        val expanded = size.width > 260.dp
        val tall = size.height > 180.dp

        val prefs =
            context.getSharedPreferences(
                "peq_widget_prefs",
                Context.MODE_PRIVATE
            )

        val track =
            prefs.getString(
                "last_track",
                "Nothing Playing"
            ) ?: "Nothing Playing"

        val artist =
            prefs.getString(
                "last_artist",
                "pEQ Neural"
            ) ?: "pEQ Neural"

        val isPlaying =
            prefs.getBoolean(
                "is_playing",
                false
            )

        val progress =
            prefs.getInt(
                "progress",
                0
            )

        val currentTime =
            prefs.getString(
                "current_time",
                "0:00"
            ) ?: "0:00"

        val duration =
            prefs.getString(
                "duration",
                "0:00"
            ) ?: "0:00"

        val repeatMode =
            prefs.getInt(
                "repeat_mode",
                0
            )

        val favorite =
            prefs.getBoolean(
                "is_favorite",
                false
            )

        val queueJson =
            prefs.getString(
                "queue_json",
                "[]"
            ) ?: "[]"

        val queue =
            try {
                Json.decodeFromString<List<String>>(queueJson)
            } catch (_: Exception) {
                emptyList()
            }

        val bg =
            ComposeColor(
                prefs.getInt(
                    "bg_color_1",
                    0xFF08090D.toInt()
                )
            )

        val accent =
            ComposeColor(
                prefs.getInt(
                    "bg_color_2",
                    0xFF8A7DFF.toInt()
                )
            )

        val artFile =
            java.io.File(
                context.filesDir,
                "widget_art.png"
            )

        val artBitmap =
            if (artFile.exists()) {
                BitmapFactory.decodeFile(
                    artFile.absolutePath
                )
            } else null

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .cornerRadius(30.dp)
                .clickable(
                    actionStartActivity(
                        MainActivity::class.java
                    )
                )
                .padding(16.dp)
        ) {

            /*
             * GLOW LAYER
             */

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(
                        accent.copy(alpha = 0.06f)
                    )
                    .cornerRadius(30.dp)
            ) {}

            /*
             * HUGE WATERMARK TEXT
             * Because subtlety died around the same time
             * people started putting RGB in kitchen appliances.
             */

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {

                Text(
                    text = "pEQ\nPlayer",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.White.copy(alpha = 0.05f)
                        ),
                        fontSize =
                            if (expanded) 34.sp
                            else 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(
                        end = 6.dp,
                        bottom = 2.dp
                    )
                )
            }

            /*
             * TOP SHINE BAR
             */

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        accent.copy(alpha = 0.55f)
                    )
                    .cornerRadius(100.dp)
            ) {}

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
            ) {

                if (compact) {

                    CompactLayout(
                        context = context,
                        track = track,
                        artist = artist,
                        progress = progress,
                        currentTime = currentTime,
                        duration = duration,
                        isPlaying = isPlaying,
                        accent = accent
                    )

                } else {

                    ExpandedLayout(
                        context = context,
                        track = track,
                        artist = artist,
                        progress = progress,
                        currentTime = currentTime,
                        duration = duration,
                        isPlaying = isPlaying,
                        accent = accent,
                        repeatMode = repeatMode,
                        favorite = favorite,
                        artBitmap = artBitmap,
                        expanded = expanded,
                        tall = tall,
                        queue = queue
                    )
                }
            }
        }
    }

    @Composable
    private fun CompactLayout(
        context: Context,
        track: String,
        artist: String,
        progress: Int,
        currentTime: String,
        duration: String,
        isPlaying: Boolean,
        accent: Color
    ) {

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = track,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(4.dp))

            Text(
                text = artist,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(
                        Color.White.copy(alpha = 0.6f)
                    ),
                    fontSize = 10.sp
                )
            )

            Spacer(GlanceModifier.height(12.dp))

            LinearProgressIndicator(
                progress = progress / 100f,
                color = ColorProvider(accent),
                backgroundColor = ColorProvider(
                    Color.White.copy(alpha = 0.08f)
                ),
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .cornerRadius(100.dp)
            )

            Spacer(GlanceModifier.height(14.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {

                NeuralButton(
                    context,
                    R.drawable.ic_prev,
                    ACTION_PREV,
                    24.dp,
                    0.82f
                )

                Spacer(GlanceModifier.width(18.dp))

                Box(
                    modifier = GlanceModifier
                        .size(52.dp)
                        .background(
                            accent.copy(alpha = 0.18f)
                        )
                        .cornerRadius(100.dp),
                    contentAlignment = Alignment.Center
                ) {

                    NeuralButton(
                        context,
                        if (isPlaying)
                            R.drawable.ic_pause
                        else
                            R.drawable.ic_play,
                        ACTION_PLAY_PAUSE,
                        28.dp
                    )
                }

                Spacer(GlanceModifier.width(18.dp))

                NeuralButton(
                    context,
                    R.drawable.ic_next,
                    ACTION_NEXT,
                    24.dp,
                    0.82f
                )
            }

            Spacer(GlanceModifier.height(10.dp))

            Text(
                text = "$currentTime  •  $duration",
                style = TextStyle(
                    color = ColorProvider(
                        Color.White.copy(alpha = 0.38f)
                    ),
                    fontSize = 9.sp
                )
            )
        }
    }

    @Composable
    private fun ExpandedLayout(
        context: Context,
        track: String,
        artist: String,
        progress: Int,
        currentTime: String,
        duration: String,
        isPlaying: Boolean,
        accent: Color,
        repeatMode: Int,
        favorite: Boolean,
        artBitmap: android.graphics.Bitmap?,
        expanded: Boolean,
        tall: Boolean,
        queue: List<String>
    ) {

        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (artBitmap != null) {

                    Box {

                        Image(
                            provider = ImageProvider(artBitmap),
                            contentDescription = null,
                            modifier = GlanceModifier
                                .size(
                                    if (expanded)
                                        88.dp
                                    else
                                        66.dp
                                )
                                .cornerRadius(24.dp),
                            contentScale = androidx.glance.layout.ContentScale.Crop
                        )

                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(
                                    Color.Black.copy(alpha = 0.08f)
                                )
                                .cornerRadius(24.dp)
                        ) {}
                    }

                    Spacer(GlanceModifier.width(16.dp))
                }

                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {

                    Text(
                        text = track,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize =
                                if (expanded)
                                    20.sp
                                else
                                    16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(GlanceModifier.height(4.dp))

                    Text(
                        text = artist,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(
                                Color.White.copy(alpha = 0.64f)
                            ),
                            fontSize = 11.sp
                        )
                    )

                    Spacer(GlanceModifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = progress / 100f,
                        color = ColorProvider(accent),
                        backgroundColor = ColorProvider(
                            Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .cornerRadius(100.dp)
                    )

                    Spacer(GlanceModifier.height(8.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {

                        Text(
                            text = currentTime,
                            style = TextStyle(
                                color = ColorProvider(
                                    Color.White.copy(alpha = 0.42f)
                                ),
                                fontSize = 9.sp
                            )
                        )

                        Spacer(
                            modifier = GlanceModifier.defaultWeight()
                        )

                        Text(
                            text = duration,
                            style = TextStyle(
                                color = ColorProvider(
                                    Color.White.copy(alpha = 0.42f)
                                ),
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            Spacer(GlanceModifier.height(18.dp))

            /*
             * FLOATING CONTROLS
             */

            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(
                        Color.White.copy(alpha = 0.04f)
                    )
                    .cornerRadius(100.dp)
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    )
            ) {

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    NeuralButton(
                        context,
                        if (repeatMode == 2)
                            R.drawable.ic_repeat_one
                        else
                            R.drawable.ic_repeat,
                        ACTION_REPEAT,
                        18.dp,
                        if (repeatMode > 0) 1f else 0.35f
                    )

                    Spacer(GlanceModifier.width(16.dp))

                    NeuralButton(
                        context,
                        R.drawable.ic_prev,
                        ACTION_PREV,
                        28.dp
                    )

                    Spacer(GlanceModifier.width(18.dp))

                    Box(
                        modifier = GlanceModifier
                            .size(60.dp)
                            .background(accent)
                            .cornerRadius(100.dp),
                        contentAlignment = Alignment.Center
                    ) {

                        NeuralButton(
                            context,
                            if (isPlaying)
                                R.drawable.ic_pause
                            else
                                R.drawable.ic_play,
                            ACTION_PLAY_PAUSE,
                            30.dp
                        )
                    }

                    Spacer(GlanceModifier.width(18.dp))

                    NeuralButton(
                        context,
                        R.drawable.ic_next,
                        ACTION_NEXT,
                        28.dp
                    )

                    Spacer(GlanceModifier.width(16.dp))

                    NeuralButton(
    context = context,
    icon = R.drawable.ic_favorite,
    action = ACTION_FAVORITE,
    size = 18.dp,
    alpha = if (favorite) 1f else 0.35f,
    tint = if (favorite) accent else Color.White
)
                }
            }

            if (tall && queue.isNotEmpty()) {

                Spacer(GlanceModifier.height(16.dp))

                Text(
                    text = "UP NEXT",
                    style = TextStyle(
                        color = ColorProvider(accent),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(GlanceModifier.height(8.dp))

                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(76.dp)
                ) {

                    items(queue.take(4)) { item ->

                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    Color.White.copy(alpha = 0.04f)
                                )
                                .cornerRadius(14.dp)
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 7.dp
                                )
                        ) {

                            Text(
                                text = item,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(
                                        Color.White.copy(alpha = 0.78f)
                                    ),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
            
@Composable
private fun NeuralButton(
    context: Context,
    icon: Int,
    action: String,
    size: Dp,
    alpha: Float = 1f,
    tint: Color = Color.White
) {

    Image(
        provider = ImageProvider(icon),
        contentDescription = null,
colorFilter = ColorFilter.tint(
    ColorProvider(
        tint.copy(alpha = alpha)
    )
),
        modifier = GlanceModifier
            .size(size)
            .clickable(
                actionSendBroadcast(
                    Intent(action).apply {
                        component = ComponentName(
                            context.packageName,
                            "org.audiocular.peq.app.widgets.NeuralGlanceWidgetReceiver"
                        )
                    }
                )
            )
    )
                }
}