package org.audiocular.peq.app.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.widgets.NeuralGlanceWidgetReceiver

class pEQWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId =
        AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {

            finish()
            return
        }

        setContent {

            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {

                WidgetConfigScreen()
            }
        }
    }

    // =========================================================
    // SCREEN
    // =========================================================

    @Composable
    private fun WidgetConfigScreen() {

        val context = this

        val prefs = remember {

            context.getSharedPreferences(
                "peq_widget_settings_$appWidgetId",
                Context.MODE_PRIVATE
            )
        }

        var showTitle by remember {
            mutableStateOf(
                prefs.getBoolean(
                    "show_title",
                    true
                )
            )
        }

        var showArtist by remember {
            mutableStateOf(
                prefs.getBoolean(
                    "show_artist",
                    true
                )
            )
        }

        var showArt by remember {
            mutableStateOf(
                prefs.getBoolean(
                    "show_art",
                    true
                )
            )
        }

        var showFavorite by remember {
            mutableStateOf(
                prefs.getBoolean(
                    "show_favorite",
                    true
                )
            )
        }

        var showShuffle by remember {
            mutableStateOf(
                prefs.getBoolean(
                    "show_shuffle",
                    true
                )
            )
        }

        var glassIntensity by remember {
            mutableFloatStateOf(
                prefs.getFloat(
                    "glass_intensity",
                    0.85f
                )
            )
        }

        val yellow = remember {
            Color(0xFFFFD600)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF050505),
                            Color(0xFF101010),
                            Color(0xFF151515)
                        )
                    )
                )
        ) {

            // =====================================================
            // BACKGROUND GLOW
            // =====================================================

            Box(
                modifier = Modifier
                    .size(260.dp)
                    .offset(
                        x = 120.dp,
                        y = (-80).dp
                    )
                    .background(
                        Brush.radialGradient(
                            listOf(
                                yellow.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp)
            ) {

                // =================================================
                // HEADER
                // =================================================

                Text(
                    text = "pEQ Widget",

                    color = Color.White,

                    fontSize = 30.sp,

                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Premium neural playback dashboard",

                    color = Color.White.copy(alpha = 0.55f),

                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // =================================================
                // PREVIEW
                // =================================================

                WidgetPreviewCard(
                    showTitle = showTitle,
                    showArtist = showArtist,
                    showArt = showArt,
                    showFavorite = showFavorite,
                    showShuffle = showShuffle,
                    glassIntensity = glassIntensity
                )

                Spacer(modifier = Modifier.height(28.dp))

                // =================================================
                // SETTINGS CARD
                // =================================================

                Surface(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(34.dp),

                    color = Color.White.copy(alpha = 0.04f),

                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.08f)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Widget Controls",

                            color = Color.White,

                            fontWeight = FontWeight.Bold,

                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ConfigToggleCard(
                            title = "Track Title",
                            subtitle = "Display song title",
                            icon = Icons.Default.Title,
                            checked = showTitle,
                            accent = yellow
                        ) {
                            showTitle = it
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ConfigToggleCard(
                            title = "Artist Name",
                            subtitle = "Display artist metadata",
                            icon = Icons.Default.Person,
                            checked = showArtist,
                            accent = yellow
                        ) {
                            showArtist = it
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ConfigToggleCard(
                            title = "Album Artwork",
                            subtitle = "Enable artwork preview",
                            icon = Icons.Default.Album,
                            checked = showArt,
                            accent = yellow
                        ) {
                            showArt = it
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ConfigToggleCard(
                            title = "Favorite Button",
                            subtitle = "Show favorite control",
                            icon = Icons.Default.Favorite,
                            checked = showFavorite,
                            accent = yellow
                        ) {
                            showFavorite = it
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ConfigToggleCard(
                            title = "Shuffle Button",
                            subtitle = "Show shuffle control",
                            icon = Icons.Default.Shuffle,
                            checked = showShuffle,
                            accent = yellow
                        ) {
                            showShuffle = it
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // =========================================
                        // GLASS INTENSITY
                        // =========================================

                        Text(
                            text = "Glass Intensity",

                            color = Color.White,

                            fontWeight = FontWeight.Bold,

                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Slider(
                            value = glassIntensity,

                            onValueChange = {
                                glassIntensity = it
                            },

                            colors = SliderDefaults.colors(
                                thumbColor = yellow,
                                activeTrackColor = yellow,
                                inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${(glassIntensity * 100).toInt()}%",

                            color = Color.White.copy(alpha = 0.5f),

                            modifier = Modifier.fillMaxWidth(),

                            textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // =================================================
                // APPLY BUTTON
                // =================================================

                Button(
                    onClick = {

                        prefs.edit()
                            .putBoolean("show_title", showTitle)
                            .putBoolean("show_artist", showArtist)
                            .putBoolean("show_art", showArt)
                            .putBoolean("show_favorite", showFavorite)
                            .putBoolean("show_shuffle", showShuffle)
                            .putFloat("glass_intensity", glassIntensity)
                            .apply()

                        val manager =
                            AppWidgetManager.getInstance(context)

                        val ids =
                            manager.getAppWidgetIds(
                                ComponentName(
                                    context,
                                    NeuralGlanceWidgetReceiver::class.java
                                )
                            )

                        val updateIntent =
                            Intent(
                                context,
                                NeuralGlanceWidgetReceiver::class.java
                            ).apply {

                                action =
                                    AppWidgetManager.ACTION_APPWIDGET_UPDATE

                                putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                    ids
                                )
                            }

                        context.sendBroadcast(updateIntent)

                        val result =
                            Intent().apply {

                                putExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    appWidgetId
                                )
                            }

                        setResult(
                            Activity.RESULT_OK,
                            result
                        )

                        finish()
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),

                    shape = RoundedCornerShape(24.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = yellow
                    )
                ) {

                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = Color.Black
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "INITIALIZE WIDGET",

                        color = Color.Black,

                        fontWeight = FontWeight.Black,

                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // =========================================================
    // PREVIEW
    // =========================================================

    @Composable
    private fun WidgetPreviewCard(
        showTitle: Boolean,
        showArtist: Boolean,
        showArt: Boolean,
        showFavorite: Boolean,
        showShuffle: Boolean,
        glassIntensity: Float
    ) {

        val yellow = remember {
            Color(0xFFFFD600)
        }

        val alpha by animateFloatAsState(
            targetValue = glassIntensity,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            ),
            label = ""
        )

        Box {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),

                shape = RoundedCornerShape(32.dp),

                color = Color(0xFF111111).copy(
                    alpha = 0.82f + (alpha * 0.08f)
                ),

                border = BorderStroke(
                    1.dp,
                    yellow.copy(alpha = 0.14f)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),

                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (showArt) {

                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(
                                    RoundedCornerShape(28.dp)
                                )
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            yellow,
                                            Color(0xFFFFB300)
                                        )
                                    )
                                )
                        )
                    }

                    if (showArt) {
                        Spacer(modifier = Modifier.width(18.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        if (showTitle) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (showArtist) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.48f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        yellow.copy(alpha = 0.75f)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(
                                rememberScrollState()
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (showShuffle) {

                                CircleButton(
                                    size = 36.dp,
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            CircleButton(
                                size = 36.dp,
                                color = Color.White.copy(alpha = 0.08f)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            CircleButton(
                                size = 52.dp,
                                color = yellow
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            CircleButton(
                                size = 36.dp,
                                color = Color.White.copy(alpha = 0.08f)
                            )

                            if (showFavorite) {

                                Spacer(modifier = Modifier.width(10.dp))

                                CircleButton(
                                    size = 36.dp,
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }

            // =====================================================
            // WATERMARK
            // =====================================================

            Text(
                text = "pEQ",

                color = yellow.copy(alpha = 0.05f),

                fontSize = 64.sp,

                fontWeight = FontWeight.Black,

                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        end = 12.dp,
                        top = 4.dp
                    )
            )

            // =====================================================
            // BOTTOM PROGRESS GLOW
            // =====================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp
                        )
                    )
                    .background(
                        yellow.copy(alpha = 0.18f)
                    )
            )
        }
    }

    @Composable
    private fun CircleButton(
        size: Dp,
        color: Color
    ) {

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }

    // =========================================================
    // TOGGLE CARD
    // =========================================================

    @Composable
    private fun ConfigToggleCard(
        title: String,
        subtitle: String,
        icon: ImageVector,
        checked: Boolean,
        accent: Color,
        onCheckedChange: (Boolean) -> Unit
    ) {

        val background by animateColorAsState(
            targetValue =
                if (checked) {
                    accent.copy(alpha = 0.10f)
                } else {
                    Color.White.copy(alpha = 0.03f)
                },
            label = ""
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            color = background,

            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.06f)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCheckedChange(!checked)
                    }
                    .padding(
                        horizontal = 16.dp,
                        vertical = 16.dp
                    ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            accent.copy(alpha = 0.14f)
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,

                        color = Color.White,

                        fontWeight = FontWeight.Bold,

                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,

                        color = Color.White.copy(alpha = 0.55f),

                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = checked,

                    onCheckedChange = onCheckedChange,

                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}