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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.widgets.NeuralGlanceWidgetReceiver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.GlanceAppWidgetManager
import org.audiocular.peq.app.widgets.NeuralGlanceWidget
import org.audiocular.peq.app.ui.theme.pEQTheme
import org.audiocular.peq.app.ui.theme.pEQTheme as AppThemeEnum

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

        if (appWidgetId ==
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            finish()
            return
        }

        setContent {
            pEQTheme(theme = org.audiocular.peq.app.ui.theme.pEQTheme.SYSTEM) {
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

        val configuration = LocalConfiguration.current
        val compactMode = configuration.screenWidthDp < 390

        val prefs = remember {
            context.getSharedPreferences(
                "peq_widget_settings_$appWidgetId",
                Context.MODE_PRIVATE
            )
        }

        // =====================================================
        // STATES
        // =====================================================

        var showTitle by remember {
            mutableStateOf(
                prefs.getBoolean("show_title", true)
            )
        }

        var showArtist by remember {
            mutableStateOf(
                prefs.getBoolean("show_artist", true)
            )
        }

        var showArt by remember {
            mutableStateOf(
                prefs.getBoolean("show_art", true)
            )
        }

        var showFavorite by remember {
            mutableStateOf(
                prefs.getBoolean("show_favorite", true)
            )
        }

        var showShuffle by remember {
            mutableStateOf(
                prefs.getBoolean("show_shuffle", true)
            )
        }

        var showRepeat by remember {
            mutableStateOf(
                prefs.getBoolean("show_repeat", true)
            )
        }

        var showQueue by remember {
            mutableStateOf(
                prefs.getBoolean("show_queue", true)
            )
        }

        var albumArtColorEnabled by remember {
            mutableStateOf(
                prefs.getBoolean("album_art_color_enabled", true)
            )
        }

        var glassIntensity by remember {
            mutableFloatStateOf(
                prefs.getFloat("glass_intensity", 0.85f)
            )
        }

        var widgetTheme by remember {
            mutableStateOf(
                prefs.getString("widget_theme", "system") ?: "system"
            )
        }

        val yellow = MaterialTheme.colorScheme.primary

        // =====================================================
        // ROOT
        // =====================================================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {

            // =================================================
            // GLOW
            // =================================================

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
                                yellow.copy(alpha = 0.14f),
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
                    .padding(
                        horizontal =
                            if (compactMode) 16.dp else 20.dp,
                        vertical = 20.dp
                    )
            ) {

                // =============================================
                // HEADER
                // =============================================

                Text(
                    text = "pEQ Widget Configurator",

                    color = MaterialTheme.colorScheme.onBackground,

                    fontSize =
                        if (compactMode) 26.sp else 32.sp,

                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

          

                // =============================================
                // PREVIEW
                // =============================================

                // WidgetPreviewCard(
                //     showTitle = showTitle,
                //     showArtist = showArtist,
                //     showArt = showArt,
                //     showFavorite = showFavorite,
                //     showShuffle = showShuffle,
                //     glassIntensity = glassIntensity
                // )

                // Spacer(modifier = Modifier.height(24.dp))

                // =============================================
                // SETTINGS CARD
                // =============================================

                Surface(
                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(30.dp),

                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),

                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(
                            if (compactMode) 16.dp else 20.dp
                        )
                    ) {

                        Text(
                            text = "Widget Controls",

                            color = MaterialTheme.colorScheme.onSurface,

                            fontWeight = FontWeight.Bold,

                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ConfigToggleCard(
                            title = "Favorite Button",
                            subtitle = "Show favorite shortcut",
                            icon = Icons.Default.Favorite,
                            checked = showFavorite,
                            accent = yellow
                        ) {
                            showFavorite = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ConfigToggleCard(
                            title = "Shuffle Button",
                            subtitle = "Enable shuffle shortcut",
                            icon = Icons.Default.Shuffle,
                            checked = showShuffle,
                            accent = yellow
                        ) {
                            showShuffle = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ConfigToggleCard(
                            title = "Repeat Button",
                            subtitle = "Enable repeat shortcut",
                            icon = Icons.Default.Repeat,
                            checked = showRepeat,
                            accent = yellow
                        ) {
                            showRepeat = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ConfigToggleCard(
                            title = "Queue Section",
                            subtitle = "Show current queue list",
                            icon = Icons.Default.QueueMusic,
                            checked = showQueue,
                            accent = yellow
                        ) {
                            showQueue = it
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        ConfigToggleCard(
                            title = "Album Art Color",
                            subtitle = "Extract color from album art",
                            icon = Icons.Default.ColorLens,
                            checked = albumArtColorEnabled,
                            accent = yellow
                        ) {
                            albumArtColorEnabled = it
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // =====================================
                        // WIDGET THEME
                        // =====================================

                        Text(
                            text = "Widget Theme",

                            color = MaterialTheme.colorScheme.onSurface,

                            fontWeight = FontWeight.Bold,

                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf("system" to "System", "dark" to "Dark", "light" to "Light")
                            themes.forEach { (key, label) ->
                                val selected = widgetTheme == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) yellow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                        )
                                        .clickable { widgetTheme = key }
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(26.dp))

                        // =====================================
                        // GLASS INTENSITY
                        // =====================================

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "Glass Intensity",

                                color = MaterialTheme.colorScheme.onSurface,

                                fontWeight = FontWeight.Bold,

                                fontSize = 14.sp
                            )

                            Text(
                                text =
                                    "${(glassIntensity * 100).toInt()}%",

                                color = yellow,

                                fontSize = 12.sp,

                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value = glassIntensity,

                            onValueChange = {
                                glassIntensity = it
                            },

                            valueRange = 0.25f..1f,

                            colors = SliderDefaults.colors(
                                thumbColor = yellow,
                                activeTrackColor = yellow,
                                inactiveTrackColor =
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // =============================================
                // APPLY BUTTON
                // =============================================

                Button(
                    onClick = {

                         prefs.edit()
                            .putBoolean(
                                "show_title",
                                showTitle
                            )
                            .putBoolean(
                                "show_artist",
                                showArtist
                            )
                            .putBoolean(
                                "show_art",
                                showArt
                            )
                            .putBoolean(
                                "show_favorite",
                                showFavorite
                            )
                            .putBoolean(
                                "show_shuffle",
                                showShuffle
                            )
                            .putBoolean(
                                "show_repeat",
                                showRepeat
                            )
                            .putBoolean(
                                "show_queue",
                                showQueue
                            )
                            .putBoolean(
                                "album_art_color_enabled",
                                albumArtColorEnabled
                            )
                            .putFloat(
                                "glass_intensity",
                                glassIntensity
                            )
                            .putString(
                                "widget_theme",
                                widgetTheme
                            )
                            .apply()

                        // =====================================
                        // FORCE WIDGET UPDATE
                        // =====================================

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

                        lifecycleScope.launch {
                            try {
                                val glanceManager = GlanceAppWidgetManager(context)
                                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { glancePrefs ->
                                    glancePrefs.toMutablePreferences().apply {
                                        set(booleanPreferencesKey("show_title"), showTitle)
                                        set(booleanPreferencesKey("show_artist"), showArtist)
                                        set(booleanPreferencesKey("show_art"), showArt)
                                        set(booleanPreferencesKey("show_favorite"), showFavorite)
                                        set(booleanPreferencesKey("show_shuffle"), showShuffle)
                                        set(booleanPreferencesKey("show_repeat"), showRepeat)
                                        set(booleanPreferencesKey("show_queue"), showQueue)
                                        set(booleanPreferencesKey("album_art_color_enabled"), albumArtColorEnabled)
                                        set(floatPreferencesKey("glass_intensity"), glassIntensity)
                                        set(stringPreferencesKey("widget_theme"), widgetTheme)
                                    }
                                }
                                NeuralGlanceWidget().update(context, glanceId)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // =====================================
                            // RESULT
                            // =====================================

                            val resultIntent =
                                Intent().apply {

                                    putExtra(
                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                        appWidgetId
                                    )
                                }

                            setResult(
                                Activity.RESULT_OK,
                                resultIntent
                            )

                            finish()
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (compactMode) 58.dp else 64.dp
                        ),

                    shape = RoundedCornerShape(22.dp),

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
                        text = "APPLY WIDGET",

                        color = Color.Black,

                        fontWeight = FontWeight.Black,

                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
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

        val configuration = LocalConfiguration.current
        val compactMode = configuration.screenWidthDp < 390

        val yellow = Color(0xFFFFD54F)

        val alpha by animateFloatAsState(
            targetValue = glassIntensity,
            animationSpec = spring(
                dampingRatio =
                    Spring.DampingRatioMediumBouncy
            ),
            label = "preview_alpha"
        )

        Box {

            Surface(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(30.dp),

                color = Color(0xFF111111).copy(
                    alpha = 0.82f + (alpha * 0.08f)
                ),

                border = BorderStroke(
                    1.dp,
                    yellow.copy(alpha = 0.14f)
                )
            ) {

                Column(
                    modifier = Modifier.padding(
                        if (compactMode) 16.dp else 20.dp
                    )
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        if (showArt) {

                            Box(
                                modifier = Modifier
                                    .size(
                                        if (compactMode) {
                                            70.dp
                                        } else {
                                            86.dp
                                        }
                                    )
                                    .clip(
                                        RoundedCornerShape(24.dp)
                                    )
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                yellow,
                                                Color(0xFFFFB300)
                                            )
                                        )
                                    ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text = "pEQ",

                                    color = Color.Black,

                                    fontWeight = FontWeight.Black,

                                    fontSize =
                                        if (compactMode) 14.sp
                                        else 18.sp
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(
                                    if (compactMode) 12.dp
                                    else 16.dp
                                )
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            if (showTitle) {

                                Text(
                                    text =
                                        "Back To December",

                                    color = Color.White,

                                    maxLines = 1,

                                    overflow =
                                        TextOverflow.Ellipsis,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        if (compactMode) 14.sp
                                        else 16.sp
                                )
                            }

                            if (showArtist) {

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "Taylor Swift",

                                    color = yellow,

                                    fontSize =
                                        if (compactMode) 11.sp
                                        else 13.sp,

                                    maxLines = 1
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(
                                    if (compactMode) 12.dp
                                    else 18.dp
                                )
                            )

                            // =============================
                            // CONTROLS
                            // =============================

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    if (showShuffle) {

                                        CircleButton(
                                            size =
                                                if (compactMode) {
                                                    30.dp
                                                } else {
                                                    34.dp
                                                },

                                            color =
                                                Color.White.copy(
                                                    alpha = 0.08f
                                                )
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )

                                    CircleButton(
                                        size =
                                            if (compactMode) {
                                                30.dp
                                            } else {
                                                34.dp
                                            },

                                        color =
                                            Color.White.copy(
                                                alpha = 0.08f
                                            )
                                    )
                                }

                                CircleButton(
                                    size =
                                        if (compactMode) {
                                            46.dp
                                        } else {
                                            54.dp
                                        },

                                    color = yellow
                                )

                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    CircleButton(
                                        size =
                                            if (compactMode) {
                                                30.dp
                                            } else {
                                                34.dp
                                            },

                                        color =
                                            Color.White.copy(
                                                alpha = 0.08f
                                            )
                                    )

                                    if (showFavorite) {

                                        Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )

                                        CircleButton(
                                            size =
                                                if (compactMode) {
                                                    30.dp
                                                } else {
                                                    34.dp
                                                },

                                            color =
                                                Color.White.copy(
                                                    alpha = 0.08f
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // =============================
                    // PROGRESS
                    // =============================

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(alpha = 0.06f)
                            )
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            yellow,
                                            Color(0xFFFFB300)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // =============================================
            // WATERMARK
            // =============================================

            Text(
                text = "pEQ",

                color = yellow.copy(alpha = 0.04f),

                fontSize =
                    if (compactMode) 52.sp else 68.sp,

                fontWeight = FontWeight.Black,

                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        end = 10.dp,
                        top = 6.dp
                    )
            )
        }
    }

    // =========================================================
    // BUTTON
    // =========================================================

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
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                },
            label = "toggle_background"
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(22.dp),

            color = background,

            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
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
                        .size(44.dp)
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

                        color = MaterialTheme.colorScheme.onSurface,

                        fontWeight = FontWeight.Bold,

                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,

                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),

                        fontSize = 12.sp,

                        maxLines = 2,

                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Switch(
                    checked = checked,

                    onCheckedChange = onCheckedChange,

                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor =
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )
            }
        }
    }
}