package org.audiocular.peq.app.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import androidx.compose.ui.graphics.Color as ComposeColor

private const val ACTION_PLAY_PAUSE = "org.audiocular.peq.app.ACTION_PLAY_PAUSE"
private const val ACTION_NEXT = "org.audiocular.peq.app.ACTION_NEXT"
private const val ACTION_PREV = "org.audiocular.peq.app.ACTION_PREV"
private const val ACTION_SHUFFLE = "org.audiocular.peq.app.ACTION_SHUFFLE"
private const val ACTION_FAVORITE = "org.audiocular.peq.app.ACTION_FAVORITE"
private const val ACTION_REPEAT = "org.audiocular.peq.app.ACTION_REPEAT"
private const val ACTION_REPLAY_10 = "org.audiocular.peq.app.ACTION_REPLAY_10"
private const val ACTION_FORWARD_30 = "org.audiocular.peq.app.ACTION_FORWARD_30"

class NeuralGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),
            DpSize(200.dp, 100.dp),
            DpSize(300.dp, 120.dp),
            DpSize(300.dp, 240.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val size = LocalSize.current
            NeuralWorkstationWidget(context, size)
        }
    }

    @Composable
    private fun NeuralWorkstationWidget(context: Context, size: DpSize) {
        val prefs = context.getSharedPreferences("peq_widget_prefs", Context.MODE_PRIVATE)
        val track = prefs.getString("last_track", "pEQ Neural") ?: "pEQ Neural"
        val artist = prefs.getString("last_artist", "Neural Workstation") ?: "Neural Workstation"
        val isPlaying = prefs.getBoolean("is_playing", false)
        val progress = prefs.getInt("progress", 0)
        val currentTime = prefs.getString("current_time", "0:00") ?: "0:00"
        val duration = prefs.getString("duration", "0:00") ?: "0:00"
        val bgColor1 = prefs.getInt("bg_color_1", 0xFF121212.toInt())
        val accentColor = prefs.getInt("bg_color_2", 0xFF1A1A1A.toInt())
        val queueJson = prefs.getString("queue_json", "[]") ?: "[]"
        val repeatMode = prefs.getInt("repeat_mode", 0)
        val queue = try { Json.decodeFromString<List<String>>(queueJson) } catch (e: Exception) { emptyList() }

        val isWide = size.width >= 250.dp
        val isTall = size.height >= 200.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ComposeColor(bgColor1))
                .cornerRadius(24.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            // Background Branding Logo (Subtle Watermark)
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Image(
                    provider = ImageProvider(R.drawable.app_icon),
                    contentDescription = null,
                    modifier = GlanceModifier
                        .size(64.dp)
                        .padding(12.dp)
                        ,
                    contentScale = ContentScale.Fit
                )
            }

            val artFile = java.io.File(context.filesDir, "widget_art.png")
            val artBitmap = if (artFile.exists()) android.graphics.BitmapFactory.decodeFile(artFile.absolutePath) else null

            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWide && artBitmap != null) {
                    Image(
                        provider = ImageProvider(artBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier
                            .size(if (isTall) 80.dp else 60.dp)
                            .cornerRadius(12.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(GlanceModifier.width(16.dp))
                }
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title/Artist Section
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = track,
                                style = TextStyle(
                                    color = ColorProvider(ComposeColor.White),
                                    fontSize = if (isTall) 20.sp else 16.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = artist,
                                style = TextStyle(
                                    color = ColorProvider(ComposeColor(accentColor).copy(alpha = 0.8f)),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(GlanceModifier.height(8.dp))

                    // Progress Bar
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(currentTime, style = TextStyle(color = ColorProvider(ComposeColor.White.copy(alpha = 0.5f)), fontSize = 9.sp))
                        Spacer(GlanceModifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = GlanceModifier.defaultWeight().height(4.dp),
                            color = ColorProvider(ComposeColor(accentColor)),
                            backgroundColor = ColorProvider(ComposeColor.White.copy(alpha = 0.1f))
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Text(duration, style = TextStyle(color = ColorProvider(ComposeColor.White.copy(alpha = 0.5f)), fontSize = 9.sp))
                    }

                    Spacer(GlanceModifier.height(12.dp))

                    // Transport Controls
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isShuffle = prefs.getBoolean("shuffle_enabled", false)
                        val isFav = prefs.getBoolean("is_favorite", false)

                        NeuralTransportButton(
                            context,
                            if (repeatMode == 2) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                            ACTION_REPEAT,
                            size = 18.dp,
                            alpha = if (repeatMode > 0) 1f else 0.4f
                        )
 
                        NeuralTransportButton(context, R.drawable.ic_prev, ACTION_PREV, size = 26.dp)
 
                        NeuralTransportButton(
                            context,
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                            ACTION_PLAY_PAUSE,
                            size = 40.dp
                        )
 
                        NeuralTransportButton(context, R.drawable.ic_next, ACTION_NEXT, size = 26.dp)
 
                        NeuralTransportButton(
                            context,
                            R.drawable.ic_favorite,
                            ACTION_FAVORITE,
                            size = 18.dp,
                            alpha = if (isFav) 1f else 0.4f
                        )
                    }

                    if (isTall && queue.isNotEmpty()) {
                        Spacer(GlanceModifier.height(16.dp))
                        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ComposeColor.White.copy(alpha = 0.1f))) {}
                        Spacer(GlanceModifier.height(8.dp))
                        Text(
                            "UPCOMING NEURAL SESSIONS",
                            style = TextStyle(color = ColorProvider(ComposeColor(accentColor)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        LazyColumn(modifier = GlanceModifier.fillMaxWidth().height(60.dp)) {
                            items(queue) { item ->
                                Text(
                                    "• $item",
                                    style = TextStyle(color = ColorProvider(ComposeColor.White.copy(alpha = 0.7f)), fontSize = 11.sp),
                                    maxLines = 1,
                                    modifier = GlanceModifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NeuralTransportButton(context: Context, icon: Int, action: String, size: androidx.compose.ui.unit.Dp = 32.dp, alpha: Float = 1f) {
        androidx.glance.Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier
                .size(size)
                .clickable(androidx.glance.appwidget.action.actionSendBroadcast(
                    android.content.Intent(action).setComponent(
                        android.content.ComponentName(
                            context.packageName,
                            "org.audiocular.peq.app.widgets.NeuralGlanceWidgetReceiver"
                        )
                    )
                ))
        )
    }
}
