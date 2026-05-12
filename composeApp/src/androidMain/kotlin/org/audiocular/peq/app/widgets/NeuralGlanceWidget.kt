package org.audiocular.peq.app.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.action.clickable
import androidx.glance.unit.ColorProvider
import org.audiocular.peq.app.MainActivity
import org.audiocular.peq.app.R
import org.audiocular.peq.app.services.pEQMediaSessionService
import org.audiocular.peq.app.receivers.pEQWidgetProvider

/**
 * 🧠 NEURAL GLANCE WORKSTATION (P-22)
 * High-fidelity adaptive widget for pEQ Neural Workstation.
 * Implements Industrial Glassmorphism and Dynamic Density Scaling.
 */
class NeuralGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(100.dp, 50.dp),   // Compact
            DpSize(180.dp, 100.dp),  // Standard
            DpSize(300.dp, 150.dp)   // Neural Console
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = context.getSharedPreferences("peq_widget_prefs", Context.MODE_PRIVATE)
            val title = prefs.getString("last_track", "pEQ Neural") ?: "pEQ Neural"
            val artist = prefs.getString("last_artist", "Ready for Input") ?: "Ready for Input"
            val isPlaying = prefs.getBoolean("is_playing", false)
            val progress = prefs.getInt("progress", 0)
            val isFavorite = prefs.getBoolean("is_favorite", false)

            val color1 = prefs.getInt("bg_color_1", 0xFF121212.toInt())
            val color2 = prefs.getInt("bg_color_2", 0xFF1A1A1A.toInt())

            NeuralWidgetTheme {
                NeuralWidgetContent(title, artist, isPlaying, progress, isFavorite, color1, color2)
            }
        }
    }

    @Composable
    private fun NeuralWidgetTheme(content: @Composable () -> Unit) {
        // We use a custom theme wrapper for Glance
        content()
    }

    @Composable
    private fun NeuralWidgetContent(
        title: String,
        artist: String,
        isPlaying: Boolean,
        progress: Int,
        isFavorite: Boolean,
        color1: Int,
        color2: Int
    ) {
        val size = LocalSize.current
        
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(ImageProvider(R.drawable.peq_widget_background))
                .padding(2.dp) // Border simulation from XML stroke
        ) {
            // Internal Content
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(color1).copy(alpha = 0.82f)))
                    .cornerRadius(22.dp)
                    .padding(14.dp)
            ) {
                if (size.height < 80.dp) {
                    CompactConsole(title, isPlaying, color2)
                } else if (size.width < 250.dp) {
                    StandardConsole(title, artist, isPlaying, progress, isFavorite, color2)
                } else {
                    NeuralCommandConsole(title, artist, isPlaying, progress, isFavorite, color2)
                }
            }
        }
    }

    @Composable
    private fun CompactConsole(title: String, isPlaying: Boolean, accentColor: Int) {
        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuralTransportButton(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, pEQWidgetProvider.ACTION_PLAY_PAUSE)
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title.uppercase(),
                    style = TextStyle(
                        color = ColorProvider(Color(accentColor)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            NeuralTransportButton(R.drawable.ic_next, pEQWidgetProvider.ACTION_NEXT)
        }
    }

    @Composable
    private fun StandardConsole(title: String, artist: String, isPlaying: Boolean, progress: Int, isFavorite: Boolean, accentColor: Int) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                text = "NEURAL TELEMETRY",
                style = TextStyle(color = ColorProvider(Color(accentColor)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = artist,
                        style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp),
                        maxLines = 1
                    )
                }
                NeuralTransportButton(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, pEQWidgetProvider.ACTION_PLAY_PAUSE)
            }
            
            Spacer(GlanceModifier.defaultWeight())
            
            // Progress Bar
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                color = ColorProvider(Color(accentColor)),
                backgroundColor = ColorProvider(Color.Gray.copy(alpha = 0.2f))
            )
            
            Spacer(GlanceModifier.height(12.dp))
            
            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                NeuralTransportButton(R.drawable.ic_prev, pEQWidgetProvider.ACTION_PREV)
                Spacer(GlanceModifier.width(20.dp))
                NeuralTransportButton(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_outline, pEQWidgetProvider.ACTION_FAVORITE)
                Spacer(GlanceModifier.width(20.dp))
                NeuralTransportButton(R.drawable.ic_next, pEQWidgetProvider.ACTION_NEXT)
            }
        }
    }

    @Composable
    private fun NeuralCommandConsole(title: String, artist: String, isPlaying: Boolean, progress: Int, isFavorite: Boolean, accentColor: Int) {
        Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "NEURAL TELEMETRY",
                    style = TextStyle(color = ColorProvider(Color(accentColor)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    text = title,
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = artist,
                    style = TextStyle(color = ColorProvider(Color.LightGray), fontSize = 14.sp),
                    maxLines = 1
                )
                
                Spacer(GlanceModifier.height(12.dp))
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                    color = ColorProvider(Color(accentColor)),
                    backgroundColor = ColorProvider(Color.Gray.copy(alpha = 0.2f))
                )
            }
            
            Spacer(GlanceModifier.width(16.dp))
            
            Column(
                modifier = GlanceModifier.width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NeuralTransportButton(R.drawable.ic_prev, pEQWidgetProvider.ACTION_PREV)
                    Spacer(GlanceModifier.width(12.dp))
                    NeuralTransportButton(R.drawable.ic_next, pEQWidgetProvider.ACTION_NEXT)
                }
                Spacer(GlanceModifier.height(12.dp))
                Box(
                    modifier = GlanceModifier.size(56.dp).cornerRadius(28.dp)
                        .background(ColorProvider(Color(accentColor).copy(alpha = 0.12f)))
                        .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(ActionParamKey to pEQWidgetProvider.ACTION_PLAY_PAUSE))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = null,
                        modifier = GlanceModifier.size(32.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(accentColor)))
                    )
                }
                Spacer(GlanceModifier.height(12.dp))
                NeuralTransportButton(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_outline, pEQWidgetProvider.ACTION_FAVORITE)
            }
        }
    }

    @Composable
    private fun IndustrialTag(text: String, accentColor: Int) {
        Box(
            modifier = GlanceModifier
                .background(ColorProvider(Color(accentColor).copy(alpha = 0.1f)))
                .cornerRadius(4.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = text.uppercase(),
                style = TextStyle(color = ColorProvider(Color(accentColor)), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    private fun NeuralTransportButton(resId: Int, action: String) {
        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .cornerRadius(20.dp)
                .background(ColorProvider(Color.White.copy(alpha = 0.08f)))
                .clickable(actionRunCallback<WidgetActionCallback>(actionParametersOf(ActionParamKey to action))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(resId),
                contentDescription = null,
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(ColorProvider(Color.White))
            )
        }
    }
}

val ActionParamKey = ActionParameters.Key<String>("action_key")

class WidgetActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val action = parameters[ActionParamKey] ?: return
        val intent = Intent(context, pEQMediaSessionService::class.java).apply {
            this.action = action
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
