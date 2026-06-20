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
    import androidx.glance.appwidget.lazy.LazyColumn
    import androidx.glance.appwidget.lazy.itemsIndexed
    import kotlinx.serialization.decodeFromString
    import kotlinx.serialization.encodeToString
    import kotlinx.serialization.json.Json
    import android.graphics.Bitmap
    import android.graphics.BitmapFactory
    import java.io.File
    import org.audiocular.peq.app.MainActivity
    import org.audiocular.peq.app.R
    import androidx.compose.ui.graphics.Color as ComposeColor
    import androidx.glance.state.PreferencesGlanceStateDefinition
    import androidx.datastore.preferences.core.Preferences
    import androidx.datastore.preferences.core.booleanPreferencesKey
    import androidx.datastore.preferences.core.stringPreferencesKey
    import androidx.datastore.preferences.core.intPreferencesKey

    private const val ACTION_PLAY_PAUSE = "org.audiocular.peq.app.ACTION_PLAY_PAUSE"
    private const val ACTION_NEXT = "org.audiocular.peq.app.ACTION_NEXT"
    private const val ACTION_PREV = "org.audiocular.peq.app.ACTION_PREV"
    private const val ACTION_REPEAT = "org.audiocular.peq.app.ACTION_REPEAT"
    private const val ACTION_FAVORITE = "org.audiocular.peq.app.ACTION_FAVORITE"
    private const val ACTION_SHUFFLE = "org.audiocular.peq.app.ACTION_SHUFFLE"
    private const val ACTION_PLAY_INDEX = "org.audiocular.peq.app.ACTION_PLAY_INDEX"

    class NeuralGlanceWidget : GlanceAppWidget() {

        override val sizeMode = SizeMode.Exact
        override val stateDefinition = PreferencesGlanceStateDefinition

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideContent {
                NeuralWidgetUI(context)
            }
        }

        @Composable
        private fun NeuralWidgetUI(context: Context) {
            val size = LocalSize.current
            val compact = size.width < 90.dp || size.height < 90.dp
            val expanded = size.width >= 230.dp
            val tall = size.height >= 140.dp && size.width >= 160.dp

            val prefs = currentState<Preferences>()
            val track = prefs[stringPreferencesKey("last_track")] ?: "pEQ Station"
            val artist = prefs[stringPreferencesKey("last_artist")] ?: "Ready for Neural Input"
            val isPlaying = prefs[booleanPreferencesKey("is_playing")] ?: false
            println("pEQControl: [WIDGET_UI] read isPlaying=$isPlaying, track=$track")
            val progress = prefs[intPreferencesKey("progress")] ?: 0
            val currentTime = prefs[stringPreferencesKey("current_time")] ?: "0:00"
            val duration = prefs[stringPreferencesKey("duration")] ?: "0:00"
            val repeatMode = prefs[intPreferencesKey("repeat_mode")] ?: 0
            val favorite = prefs[booleanPreferencesKey("is_favorite")] ?: false
            val isShuffle = prefs[booleanPreferencesKey("is_shuffle")] ?: false
            val queueJson = prefs[stringPreferencesKey("queue_json")] ?: "[]"
            val queue = try { Json.decodeFromString<List<String>>(queueJson) } catch (_: Exception) { emptyList() }
            val currentLyric = prefs[stringPreferencesKey("current_lyric")] ?: ""
            val nextLyric = prefs[stringPreferencesKey("next_lyric")] ?: ""

            // Read configurator settings
            val showTitle = prefs[booleanPreferencesKey("show_title")] ?: true
            val showArtist = prefs[booleanPreferencesKey("show_artist")] ?: true
            val showArt = prefs[booleanPreferencesKey("show_art")] ?: true
            val showFavoriteConfig = prefs[booleanPreferencesKey("show_favorite")] ?: true
            val showShuffleConfig = prefs[booleanPreferencesKey("show_shuffle")] ?: true
            val showRepeatConfig = prefs[booleanPreferencesKey("show_repeat")] ?: true
            val showQueueConfig = prefs[booleanPreferencesKey("show_queue")] ?: true
            val albumArtColorEnabled = prefs[booleanPreferencesKey("album_art_color_enabled")] ?: true
            val glassIntensity = prefs[androidx.datastore.preferences.core.floatPreferencesKey("glass_intensity")] ?: 0.75f
            val widgetTheme = prefs[stringPreferencesKey("widget_theme")] ?: "system"

            val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val isWidgetDark = when (widgetTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemDark
            }

            val rawBg = if (albumArtColorEnabled) {
                ComposeColor(prefs[intPreferencesKey("bg_color_1")] ?: (if (isWidgetDark) 0xFF08090D.toInt() else 0xFFF1F5F9.toInt()))
            } else {
                ComposeColor(if (isWidgetDark) 0xFF08090D.toInt() else 0xFFF1F5F9.toInt())
            }

            val bg = if (isWidgetDark) {
                if (isColorLight(rawBg)) ComposeColor(0xFF08090D.toInt()) else rawBg
            } else {
                if (!isColorLight(rawBg)) ComposeColor(0xFFF1F5F9.toInt()) else rawBg
            }

            val accent = if (albumArtColorEnabled) {
                ComposeColor(prefs[intPreferencesKey("bg_color_2")] ?: 0xFF8A7DFF.toInt())
            } else {
                ComposeColor(0xFF8A7DFF.toInt())
            }

            val isLightBg = isColorLight(bg)
            val primaryText = if (isLightBg) ComposeColor(0xFF0F111A) else ComposeColor.White
            val secondaryText = if (isLightBg) ComposeColor(0xFF555B6E) else ComposeColor.White.copy(alpha = 0.65f)
            val titleText = if (isLightBg) {
                if (isColorLight(accent)) ComposeColor(0xFF0F111A) else accent
            } else {
                if (!isColorLight(accent)) ComposeColor.White else accent
            }
            val buttonColor = if (isLightBg) {
                if (isColorLight(accent)) ComposeColor(0xFF0F111A) else accent
            } else {
                if (!isColorLight(accent)) ComposeColor.White else accent
            }

            val artFile = File(context.filesDir, "widget_art.png")
            val artBitmap: Bitmap? = if (artFile.exists()) {
                try {
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    BitmapFactory.decodeFile(artFile.absolutePath, opts)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    if (compact) {
                        CompactLayout(
                            context = context,
                            track = track,
                            artist = artist,
                            progress = progress,
                            isPlaying = isPlaying,
                            accent = buttonColor,
                            bg = bg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            artBitmap = artBitmap,
                            showTitle = showTitle,
                            showArtist = showArtist,
                            showArt = showArt,
                            glassIntensity = glassIntensity
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
                            accent = buttonColor,
                            bg = bg,
                            repeatMode = repeatMode,
                            favorite = favorite,
                            isShuffle = isShuffle,
                            expanded = expanded,
                            tall = tall,
                            queue = queue,
                            artBitmap = artBitmap,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            titleText = titleText,
                            showTitle = showTitle,
                            showArtist = showArtist,
                            showArt = showArt,
                            showFavoriteConfig = showFavoriteConfig,
                            showShuffleConfig = showShuffleConfig,
                            showRepeatConfig = showRepeatConfig,
                            showQueueConfig = showQueueConfig,
                            glassIntensity = glassIntensity,
                            currentLyric = currentLyric,
                            nextLyric = nextLyric
                        )
                    }
                }
            }
        }

        @Suppress("UNUSED_PARAMETER")
        @Composable
        private fun CompactLayout(
            context: Context,
            track: String,
            artist: String,
            progress: Int,
            isPlaying: Boolean,
            accent: ComposeColor,
            bg: ComposeColor,
            primaryText: ComposeColor,
            secondaryText: ComposeColor,
            artBitmap: Bitmap?,
            showTitle: Boolean,
            showArtist: Boolean,
            showArt: Boolean,
            glassIntensity: Float
        ) {
            val size = LocalSize.current
            val showPrevNext = size.width >= 110.dp
            val isWide = size.width >= 180.dp

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bg.copy(alpha = glassIntensity))
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(MainActivity::class.java))
            ) {
                Image(
                    provider = ImageProvider(if (isColorLight(bg)) R.drawable.peq_glass_border_light else R.drawable.peq_glass_border_dark),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(20.dp),
                    contentScale = ContentScale.FillBounds
                )

                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp)
                ) {
                    if (isWide) {
                        Row(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showArt) {
                                Box(
                                    modifier = GlanceModifier
                                        .size(40.dp)
                                        .background(primaryText.copy(alpha = 0.08f))
                                        .cornerRadius(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artBitmap != null) {
                                        Image(
                                            provider = ImageProvider(artBitmap),
                                            contentDescription = null,
                                            modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            provider = ImageProvider(R.drawable.app_icon),
                                            contentDescription = null,
                                            modifier = GlanceModifier.size(24.dp),
                                            contentScale = ContentScale.Fit,
                                            colorFilter = ColorFilter.tint(ColorProvider(primaryText.copy(alpha = 0.2f)))
                                        )
                                    }
                                }
                                Spacer(GlanceModifier.width(10.dp))
                            }
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                if (showTitle) {
                                    Text(text = track, maxLines = 1, style = TextStyle(color = ColorProvider(primaryText), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                                if (showArtist) {
                                    Text(text = artist, maxLines = 1, style = TextStyle(color = ColorProvider(secondaryText), fontSize = 9.sp))
                                }
                            }
                            Spacer(GlanceModifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (showPrevNext) {
                                    NeuralButton(context, R.drawable.ic_prev, ACTION_PREV, 24.dp, tint = accent)
                                    Spacer(GlanceModifier.width(8.dp))
                                }
                                Box(modifier = GlanceModifier.size(36.dp).background(accent.copy(alpha = 0.15f)).cornerRadius(100.dp), contentAlignment = Alignment.Center) {
                                    NeuralButton(context, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, ACTION_PLAY_PAUSE, 22.dp, tint = accent)
                                }
                                if (showPrevNext) {
                                    Spacer(GlanceModifier.width(8.dp))
                                    NeuralButton(context, R.drawable.ic_next, ACTION_NEXT, 24.dp, tint = accent)
                                }
                            }
                        }
                    } else {
                        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showTitle) {
                                Text(text = track, maxLines = 1, style = TextStyle(color = ColorProvider(primaryText), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                            }
                            if (showArtist) {
                                Text(text = artist, maxLines = 1, style = TextStyle(color = ColorProvider(secondaryText), fontSize = 9.sp))
                            }
                            Spacer(GlanceModifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (showPrevNext) {
                                    NeuralButton(context, R.drawable.ic_prev, ACTION_PREV, 24.dp, tint = accent)
                                    Spacer(GlanceModifier.width(14.dp))
                                }
                                Box(modifier = GlanceModifier.size(48.dp).background(accent.copy(alpha = 0.15f)).cornerRadius(100.dp), contentAlignment = Alignment.Center) {
                                    NeuralButton(context, if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, ACTION_PLAY_PAUSE, 28.dp, tint = accent)
                                }
                                if (showPrevNext) {
                                    Spacer(GlanceModifier.width(14.dp))
                                    NeuralButton(context, R.drawable.ic_next, ACTION_NEXT, 24.dp, tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        @Suppress("UNUSED_PARAMETER")
        @Composable
        private fun ExpandedLayout(
            context: Context,
            track: String,
            artist: String,
            progress: Int,
            currentTime: String,
            duration: String,
            isPlaying: Boolean,
            accent: ComposeColor,
            bg: ComposeColor,
            repeatMode: Int,
            favorite: Boolean,
            isShuffle: Boolean,
            expanded: Boolean,
            tall: Boolean,
            queue: List<String>,
            artBitmap: Bitmap?,
            primaryText: ComposeColor,
            secondaryText: ComposeColor,
            titleText: ComposeColor,
            showTitle: Boolean,
            showArtist: Boolean,
            showArt: Boolean,
            showFavoriteConfig: Boolean,
            showShuffleConfig: Boolean,
            showRepeatConfig: Boolean,
            showQueueConfig: Boolean,
            glassIntensity: Float,
            currentLyric: String = "",
            nextLyric: String = ""
        ) {
            val size = LocalSize.current
            val showQueue = queue.isNotEmpty() && showQueueConfig
            val tallLayout = size.height >= 140.dp
            val wideLayout = size.width >= 230.dp && size.height >= 100.dp

            if (tallLayout) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    PlayerCard(
                        context = context,
                        track = track,
                        artist = artist,
                        isPlaying = isPlaying,
                        accent = accent,
                        bg = bg,
                        repeatMode = repeatMode,
                        favorite = favorite,
                        isShuffle = isShuffle,
                        artBitmap = artBitmap,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        titleText = titleText,
                        showTitle = showTitle,
                        showArtist = showArtist,
                        showArt = showArt,
                        showFavoriteConfig = showFavoriteConfig,
                        showShuffleConfig = showShuffleConfig,
                        showRepeatConfig = showRepeatConfig,
                        glassIntensity = glassIntensity,
                        size = size,
                        modifier = GlanceModifier.fillMaxWidth().height(if (size.width >= 280.dp && size.height >= 280.dp) 96.dp else 68.dp)
                    )
                    
                    if (showQueue) {
                        Spacer(GlanceModifier.height(8.dp))
                        QueueCard(
                            context = context,
                            track = track,
                            queue = queue,
                            accent = accent,
                            bg = bg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            glassIntensity = glassIntensity,
                            size = size,
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            currentLyric = currentLyric,
                            nextLyric = nextLyric
                        )
                    }
                }
            } else if (wideLayout) {
                Row(modifier = GlanceModifier.fillMaxSize()) {
                    PlayerCard(
                        context = context,
                        track = track,
                        artist = artist,
                        isPlaying = isPlaying,
                        accent = accent,
                        bg = bg,
                        repeatMode = repeatMode,
                        favorite = favorite,
                        isShuffle = isShuffle,
                        artBitmap = artBitmap,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        titleText = titleText,
                        showTitle = showTitle,
                        showArtist = showArtist,
                        showArt = showArt,
                        showFavoriteConfig = showFavoriteConfig,
                        showShuffleConfig = showShuffleConfig,
                        showRepeatConfig = showRepeatConfig,
                        glassIntensity = glassIntensity,
                        size = size,
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    )
                    
                    if (showQueue) {
                        Spacer(GlanceModifier.width(8.dp))
                        QueueCard(
                            context = context,
                            track = track,
                            queue = queue,
                            accent = accent,
                            bg = bg,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            glassIntensity = glassIntensity,
                            size = size,
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            currentLyric = currentLyric,
                            nextLyric = nextLyric
                        )
                    }
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    PlayerCard(
                        context = context,
                        track = track,
                        artist = artist,
                        isPlaying = isPlaying,
                        accent = accent,
                        bg = bg,
                        repeatMode = repeatMode,
                        favorite = favorite,
                        isShuffle = isShuffle,
                        artBitmap = artBitmap,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        titleText = titleText,
                        showTitle = showTitle,
                        showArtist = showArtist,
                        showArt = showArt,
                        showFavoriteConfig = showFavoriteConfig,
                        showShuffleConfig = showShuffleConfig,
                        showRepeatConfig = showRepeatConfig,
                        glassIntensity = glassIntensity,
                        size = size,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }

        @Composable
        private fun PlayerCard(
            context: Context,
            track: String,
            artist: String,
            isPlaying: Boolean,
            accent: ComposeColor,
            bg: ComposeColor,
            repeatMode: Int,
            favorite: Boolean,
            isShuffle: Boolean,
            artBitmap: Bitmap?,
            primaryText: ComposeColor,
            secondaryText: ComposeColor,
            titleText: ComposeColor,
            showTitle: Boolean,
            showArtist: Boolean,
            showArt: Boolean,
            showFavoriteConfig: Boolean,
            showShuffleConfig: Boolean,
            showRepeatConfig: Boolean,
            glassIntensity: Float,
            size: DpSize,
            modifier: GlanceModifier
        ) {
            val isExtraLarge = size.width >= 280.dp && size.height >= 280.dp
            val artworkSize = if (isExtraLarge) 80.dp else 54.dp
            val titleFontSize = if (isExtraLarge) 15.sp else 12.sp
            val artistFontSize = if (isExtraLarge) 11.sp else 9.sp
            val controlsSpacing = if (isExtraLarge) 6.dp else 3.dp
            val playButtonCircleSize = if (isExtraLarge) 46.dp else 34.dp
            val playButtonSize = if (isExtraLarge) 28.dp else 20.dp
            val skipButtonSize = if (isExtraLarge) 34.dp else 26.dp
           val optionButtonSize = if (isExtraLarge) 28.dp else 20.dp

            Box(
                modifier = modifier
                    .background(bg.copy(alpha = glassIntensity))
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(MainActivity::class.java))
            ) {
                Image(
                    provider = ImageProvider(if (isColorLight(bg)) R.drawable.peq_glass_border_light else R.drawable.peq_glass_border_dark),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(20.dp),
                    contentScale = ContentScale.FillBounds
                )

                Row(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (isExtraLarge) 14.dp else 10.dp,
                            vertical = if (isExtraLarge) 8.dp else 7.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showArt) {
                        Box(
                            modifier = GlanceModifier
                                .size(artworkSize)
                                .background(primaryText.copy(alpha = 0.08f))
                                .cornerRadius(10.dp), 
                            contentAlignment = Alignment.Center
                        ) {
                            if (artBitmap != null) {
                                Image(
                                    provider = ImageProvider(artBitmap),
                                    contentDescription = null,
                                    modifier = GlanceModifier.fillMaxSize().cornerRadius(10.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    provider = ImageProvider(R.drawable.app_icon), 
                                    contentDescription = null, 
                                    modifier = GlanceModifier.size(if (isExtraLarge) 40.dp else 28.dp), 
                                    contentScale = ContentScale.Fit, 
                                    colorFilter = ColorFilter.tint(ColorProvider(primaryText.copy(alpha = 0.2f)))
                                )
                            }
                        }
                        Spacer(GlanceModifier.width(if (isExtraLarge) 14.dp else 10.dp))
                    }
                    
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (showTitle) {
                                Text(
                                    text = track, 
                                    maxLines = 1, 
                                    style = TextStyle(
                                        color = ColorProvider(titleText), 
                                        fontSize = titleFontSize, 
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            if (showArtist) {
                                if (showTitle) {
                                    Spacer(GlanceModifier.height(2.dp))
                                }
                                Text(
                                    text = artist, 
                                    maxLines = 1, 
                                    style = TextStyle(
                                        color = ColorProvider(secondaryText), 
                                        fontSize = artistFontSize,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                        
                        Spacer(GlanceModifier.height(if (isExtraLarge) 8.dp else 4.dp))
                        
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val showFavorite = size.width >= 180.dp && showFavoriteConfig
                            val showPrevNext = size.width >= 110.dp
                            val showRepeat = size.width >= 180.dp && showRepeatConfig
                            val showShuffle = size.width >= 240.dp && showShuffleConfig

                            Spacer(GlanceModifier.defaultWeight())
                            if (showFavorite) {
                            NeuralButton(
    context = context,
    icon = if (favorite)
        R.drawable.ic_favorite
    else
        R.drawable.ic_favorite_outline,
    action = ACTION_FAVORITE,
    size = if (favorite) {
        optionButtonSize
    } else {
        optionButtonSize - 2.dp
    },
    tint = if (favorite) accent else secondaryText,
    modifier = GlanceModifier.padding(horizontal = controlsSpacing)
)
                            }

                            if (showPrevNext) {
                                NeuralButton(
                                    context = context,
                                    icon = R.drawable.ic_prev,
                                    action = ACTION_PREV,
                                    size = skipButtonSize,
                                    tint = accent,
                                    modifier = GlanceModifier.padding(horizontal = controlsSpacing + 2.dp)
                                )
                            }

                            Box(
                                modifier = GlanceModifier
                                    .size(playButtonCircleSize)
                                    // .background(accent.copy(alpha = 0.15f))
                                    .cornerRadius(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                NeuralButton(
                                    context = context,
                                    icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                                    action = ACTION_PLAY_PAUSE,
                                    size = playButtonSize,
                                    tint = accent
                                )
                            }

                            if (showPrevNext) {
                                NeuralButton(
                                    context = context,
                                    icon = R.drawable.ic_next,
                                    action = ACTION_NEXT,
                                    size = skipButtonSize,
                                    tint = accent,
                                    modifier = GlanceModifier.padding(horizontal = controlsSpacing + 2.dp)
                                )
                            }

                            if (showRepeat) {
                                NeuralButton(
                                    context = context,
                                    icon = if (repeatMode == 1) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                                    action = ACTION_REPEAT,
                                    size = optionButtonSize,
                                    tint = if (repeatMode > 0) accent else secondaryText,
                                    modifier = GlanceModifier.padding(horizontal = controlsSpacing)
                                )
                            }

                            if (showShuffle) {
                                NeuralButton(
                                    context = context,
                                    icon = R.drawable.ic_shuffle,
                                    action = ACTION_SHUFFLE,
                                    size = optionButtonSize,
                                    tint = if (isShuffle) accent else secondaryText,
                                    modifier = GlanceModifier.padding(horizontal = controlsSpacing)
                                )
                            }
                            Spacer(GlanceModifier.defaultWeight())
                        }
                    }
                }
            }
        }

        @Composable
        private fun QueueCard(
            context: Context,
            track: String,
            queue: List<String>,
            accent: ComposeColor,
            bg: ComposeColor,
            primaryText: ComposeColor,
            secondaryText: ComposeColor,
            glassIntensity: Float,
            size: DpSize,
            modifier: GlanceModifier,
            currentLyric: String = "",
            nextLyric: String = ""
        ) {
            val isExtraLarge = size.width >= 280.dp && size.height >= 280.dp

            Box(
                modifier = modifier
                    .background(bg.copy(alpha = glassIntensity))
                    .cornerRadius(20.dp)
            ) {
                Image(
                    provider = ImageProvider(if (isColorLight(bg)) R.drawable.peq_glass_border_light else R.drawable.peq_glass_border_dark),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(20.dp),
                    contentScale = ContentScale.FillBounds
                )

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(if (isExtraLarge) 16.dp else 12.dp)
                ) {
                    if (currentLyric.isNotBlank()) {
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Lyrics",
                                style = TextStyle(
                                    color = ColorProvider(accent),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = currentLyric,
                                maxLines = 1,
                                style = TextStyle(
                                    color = ColorProvider(primaryText),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            if (nextLyric.isNotBlank()) {
                                Text(
                                    text = nextLyric,
                                    maxLines = 1,
                                    style = TextStyle(
                                        color = ColorProvider(secondaryText),
                                        fontSize = 9.sp
                                    )
                                )
                            }
                            Spacer(GlanceModifier.height(4.dp))
                        }
                    }

                    Text(
                        text = "Current Queue (${queue.size})", 
                        style = TextStyle(
                            color = ColorProvider(secondaryText), 
                            fontSize = if (isExtraLarge) 13.sp else 12.sp, 
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                    
                    Spacer(GlanceModifier.height(8.dp))

                    LazyColumn(
                        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()
                    ) {
                        itemsIndexed(queue) { index, rawItem ->
                            val parts = rawItem.split("|")
                            val queueItem = parts.getOrNull(0) ?: rawItem
                            val globalIndex = parts.getOrNull(1)?.toIntOrNull() ?: index
                            val isActive = queueItem.equals(track, ignoreCase = true)
                            
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(if (isActive) primaryText.copy(alpha = 0.08f) else ComposeColor.Transparent)
                                    .cornerRadius(8.dp)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .clickable(
                                        actionStartService(
                                            Intent(ACTION_PLAY_INDEX).apply {
                                                component = ComponentName(context.packageName, "org.audiocular.peq.app.services.pEQMediaSessionService")
                                                putExtra("play_index", globalIndex)
                                            },
                                            isForegroundService = true
                                        )
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier.width(24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (isActive) {
                                        Image(
                                            provider = ImageProvider(R.drawable.ic_play),
                                            contentDescription = "Playing",
                                            colorFilter = ColorFilter.tint(ColorProvider(accent)),
                                            modifier = GlanceModifier.size(12.dp)
                                        )
                                    } else {
                                        Text(
                                            text = (globalIndex + 1).toString(),
                                            style = TextStyle(
                                                color = ColorProvider(secondaryText),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }

                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        text = queueItem,
                                        maxLines = 1,
                                        style = TextStyle(
                                            color = ColorProvider(if (isActive) accent else primaryText),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        @Composable
        private fun NeuralButton(context: Context, icon: Int, action: String, size: Dp, tint: ComposeColor = ComposeColor.White, modifier: GlanceModifier = GlanceModifier) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ColorProvider(tint)),
                modifier = GlanceModifier.size(size).then(modifier).clickable(
                    actionStartService(
                        Intent(action).apply {
                            component = ComponentName(context.packageName, "org.audiocular.peq.app.services.pEQMediaSessionService")
                        },
                        isForegroundService = true
                    )
                )
            )
        }

        private fun isColorLight(color: ComposeColor): Boolean {
            val red = color.red
            val green = color.green
            val blue = color.blue
            val luminance = 0.2126f * red + 0.7152f * green + 0.0722f * blue
            return luminance > 0.5f
        }
    }

    object NeuralWidgetStateSync {
        suspend fun update(
            context: Context,
            track: String,
            artist: String,
            isPlaying: Boolean,
            progress: Int,
            currentTime: String,
            duration: String,
            repeatMode: Int,
            favorite: Boolean,
            isShuffle: Boolean,
            queue: List<String>,
            bgColor1: Int,
            bgColor2: Int,
            currentLyric: String = "",
            nextLyric: String = ""
        ) {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(NeuralGlanceWidget::class.java)
                for (glanceId in glanceIds) {
                    androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("last_track")] = track
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("last_artist")] = artist
                        prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_playing")] = isPlaying
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("progress")] = progress
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("current_time")] = currentTime
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("duration")] = duration
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("repeat_mode")] = repeatMode
                        prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_favorite")] = favorite
                        prefs[androidx.datastore.preferences.core.booleanPreferencesKey("is_shuffle")] = isShuffle
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("queue_json")] = Json.encodeToString(queue)
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("bg_color_1")] = bgColor1
                        prefs[androidx.datastore.preferences.core.intPreferencesKey("bg_color_2")] = bgColor2
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("current_lyric")] = currentLyric
                        prefs[androidx.datastore.preferences.core.stringPreferencesKey("next_lyric")] = nextLyric
                    }
                    NeuralGlanceWidget().update(context, glanceId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }