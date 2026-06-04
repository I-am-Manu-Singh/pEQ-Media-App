/*
 * pEQ Neural Widget
 * Humans kept shrinking music players until they became decorative jewelry.
 * So here. Tiny cyberpunk altar for your audio obsession.
 */

package org.audiocular.peq.app.widgets

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import androidx.compose.ui.graphics.Color as ComposeColor

private const val ACTION_PLAY_PAUSE = "org.audiocular.peq.app.ACTION_PLAY_PAUSE"
private const val ACTION_NEXT = "org.audiocular.peq.app.ACTION_NEXT"
private const val ACTION_PREV = "org.audiocular.peq.app.ACTION_PREV"
private const val ACTION_REPEAT = "org.audiocular.peq.app.ACTION_REPEAT"
private const val ACTION_FAVORITE = "org.audiocular.peq.app.ACTION_FAVORITE"
private const val ACTION_SHUFFLE = "org.audiocular.peq.app.ACTION_SHUFFLE"

class NeuralGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 100.dp),
            DpSize(160.dp, 100.dp),
            DpSize(240.dp, 100.dp),
            DpSize(240.dp, 160.dp),
            DpSize(300.dp, 200.dp),
            DpSize(320.dp, 280.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            NeuralWidgetUI(context)
        }
    }

    @Composable
    private fun NeuralWidgetUI(context: Context) {
        val size = LocalSize.current
        val compact = size.width < 150.dp
        val expanded = size.width >= 230.dp
        val tall = size.height >= 120.dp

        val prefs = context.getSharedPreferences("peq_widget_prefs", Context.MODE_PRIVATE)
        val track = prefs.getString("last_track", "pEQ Station") ?: "pEQ Station"
        val artist = prefs.getString("last_artist", "Ready for Neural Input") ?: "Ready for Neural Input"
        val isPlaying = prefs.getBoolean("is_playing", false)
        val progress = prefs.getInt("progress", 0)
        val currentTime = prefs.getString("current_time", "0:00") ?: "0:00"
        val duration = prefs.getString("duration", "0:00") ?: "0:00"
        val repeatMode = prefs.getInt("repeat_mode", 0)
        val favorite = prefs.getBoolean("is_favorite", false)
        val queueJson = prefs.getString("queue_json", "[]") ?: "[]"
        val queue = try { Json.decodeFromString<List<String>>(queueJson) } catch (_: Exception) { emptyList() }

        val bg = ComposeColor(prefs.getInt("bg_color_1", 0xFF08090D.toInt()))
        val accent = ComposeColor(prefs.getInt("bg_color_2", 0xFF8A7DFF.toInt()))

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(MainActivity::class.java))
                .padding(12.dp)
        ) {
            // Glow and Branding
            Box(modifier = GlanceModifier.fillMaxSize().background(accent.copy(alpha = 0.05f)).cornerRadius(24.dp)) {}
            
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Text(
                    text = "pEQ",
                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.04f)), fontSize = if (expanded) 42.sp else 28.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(end = 4.dp, bottom = 0.dp)
                )
            }

            Column(modifier = GlanceModifier.fillMaxSize()) {
                if (compact) {
                    CompactLayout(context, track, artist, progress, isPlaying, accent)
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
                        expanded = expanded,
                        tall = tall,
                        queue = queue
                    )
                }
            }
        }
    }

    @Composable
    private fun CompactLayout(context: Context, track: String, artist: String, progress: Int, isPlaying: Boolean, accent: Color) {
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = track, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Text(text = artist, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.6f)), fontSize = 9.sp))
            Spacer(GlanceModifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeuralButton(context, R.drawable.ic_prev, ACTION_PREV, 20.dp, 0.7f)
                Spacer(GlanceModifier.width(10.dp))
                Box(modifier = GlanceModifier.size(40.dp).background(accent.copy(alpha = 0.2f)).cornerRadius(100.dp), contentAlignment = Alignment.Center) {
                    NeuralButton(context, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, ACTION_PLAY_PAUSE, 22.dp)
                }
                Spacer(GlanceModifier.width(10.dp))
                NeuralButton(context, R.drawable.ic_next, ACTION_NEXT, 20.dp, 0.7f)
            }
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
        expanded: Boolean,
        tall: Boolean,
        queue: List<String>
    ) {
        val size = LocalSize.current
        
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Artwork Thumbnail - Clickable to open app
                Box(
                    modifier = GlanceModifier
                        .size(if (expanded) 64.dp else 48.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(MainActivity::class.java)), 
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.app_icon), 
                        contentDescription = null, 
                        modifier = GlanceModifier.size(if (expanded) 32.dp else 24.dp), 
                        contentScale = ContentScale.Fit, 
                        colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.15f)))
                    )
                }
                
                Spacer(GlanceModifier.width(12.dp))
                
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = track, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = if (expanded) 17.sp else 14.sp, fontWeight = FontWeight.Bold))
                    Text(text = artist, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.5f)), fontSize = 10.sp))
                    
                    if (size.height > 100.dp) {
                        Spacer(GlanceModifier.height(8.dp))
                        LinearProgressIndicator(progress = progress / 100f, color = ColorProvider(accent), backgroundColor = ColorProvider(Color.White.copy(alpha = 0.08f)), modifier = GlanceModifier.fillMaxWidth().height(3.dp).cornerRadius(100.dp))
                        Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp)) {
                            Text(text = currentTime, style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.4f)), fontSize = 8.sp))
                            Spacer(GlanceModifier.defaultWeight())
                            Text(text = duration, style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.4f)), fontSize = 8.sp))
                        }
                    }
                }
            }

            Spacer(GlanceModifier.height(if (tall) 10.dp else 6.dp))

            // Transport Control Strip
            Row(
                modifier = GlanceModifier.fillMaxWidth().background(Color.White.copy(alpha = 0.03f)).cornerRadius(100.dp).padding(vertical = if (tall) 8.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuralButton(context, if (repeatMode == 1) R.drawable.ic_repeat_one else R.drawable.ic_repeat, ACTION_REPEAT, 16.dp, if (repeatMode > 0) 1f else 0.3f)
                Spacer(GlanceModifier.width(if (expanded) 16.dp else 8.dp))
                NeuralButton(context, R.drawable.ic_prev, ACTION_PREV, 22.dp)
                Spacer(GlanceModifier.width(if (expanded) 16.dp else 12.dp))
                Box(modifier = GlanceModifier.size(if (tall) 44.dp else 36.dp).background(accent).cornerRadius(100.dp), contentAlignment = Alignment.Center) {
                    NeuralButton(context, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, ACTION_PLAY_PAUSE, if (tall) 24.dp else 20.dp, tint = Color.Black)
                }
                Spacer(GlanceModifier.width(if (expanded) 16.dp else 12.dp))
                NeuralButton(context, R.drawable.ic_next, ACTION_NEXT, 22.dp)
                Spacer(GlanceModifier.width(if (expanded) 16.dp else 8.dp))
                NeuralButton(context, R.drawable.ic_shuffle, ACTION_SHUFFLE, 16.dp, alpha = 0.6f)
                Spacer(GlanceModifier.width(if (expanded) 16.dp else 8.dp))
                NeuralButton(context, if (favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_outline, ACTION_FAVORITE, 16.dp, tint = if (favorite) accent else Color.White)
            }

            if (tall && queue.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Text(text = "NEURAL QUEUE", style = TextStyle(color = ColorProvider(accent), fontSize = 8.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    queue.take(if (size.height > 200.dp) 3 else 1).forEach { item ->
                        Box(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp).background(Color.White.copy(alpha = 0.04f)).cornerRadius(10.dp).padding(6.dp)) {
                            Text(text = item, maxLines = 1, style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 10.sp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NeuralButton(context: Context, icon: Int, action: String, size: Dp, alpha: Float = 1f, tint: Color = Color.White) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(tint.copy(alpha = alpha))),
            modifier = GlanceModifier.size(size).clickable(
                actionSendBroadcast(
                    Intent(action).apply {
                        component = ComponentName(context.packageName, "org.audiocular.peq.app.widgets.NeuralGlanceWidgetReceiver")
                    }
                )
            )
        )
    }
}