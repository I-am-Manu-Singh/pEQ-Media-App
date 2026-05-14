package org.audiocular.peq.app.ui.screens.player

import org.audiocular.peq.app.ui.viewmodel.requestChaptersPicker
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.audiocular.peq.app.data.RepeatMode
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.pEQBackHandler
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.viewmodel.cycleRotation
import kotlin.time.TimeSource

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun ImmersiveCinemaEngine(
    viewModel: AppViewModel,
    item: pEQMediaItem
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var gestureUIState by remember { mutableStateOf<Pair<String, Float>?>(null) }
    val overlayVisible = viewModel.isChaptersVisible.value || viewModel.showTrackInfoSheet.value
    val currentDuration = viewModel.currentDuration.value.coerceAtLeast(1L)

   LaunchedEffect(
    viewModel.isLandscapeMode.value
) {

    viewModel.isVideoFullScreen.value = true

    setSystemBarsVisibility(false)

    setScreenOrientation(

        if (viewModel.isLandscapeMode.value) {
            pEQOrientation.LANDSCAPE
        } else {
            pEQOrientation.PORTRAIT
        }
    )
}
LaunchedEffect(item.path) {

    viewModel.isLandscapeMode.value =
        shouldUseLandscape(
            item.videoWidth,
            item.videoHeight
        )
}

    pEQBackHandler {
        if (isLocked) return@pEQBackHandler
        if (overlayVisible) {
            viewModel.isChaptersVisible.value = false
            viewModel.showTrackInfoSheet.value = false
        } else {
            viewModel.isVideoFullScreen.value = false
            setSystemBarsVisibility(true)
            setScreenOrientation(pEQOrientation.USER)
            viewModel.currentTab.value = Tab.Dashboard
        }
    }

   Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)

        // Universal tap handling for KMP
        .pointerInput(
    isLocked,
    overlayVisible,
    gestureUIState
) {

    detectTapGestures(

        onTap = {

            if (
                isLocked ||
                overlayVisible ||
                gestureUIState != null
            ) {
                return@detectTapGestures
            }

            controlsVisible =
                !controlsVisible
        },

        onDoubleTap = { offset ->

            if (isLocked) {
                return@detectTapGestures
            }

            val seekAmount =
                if (offset.x < size.width / 2f) {
                    -10000L
                } else {
                    10000L
                }

            viewModel.seekRelative(seekAmount)

            gestureUIState =
                if (seekAmount > 0) {
                    "SEEK +00:10" to 1f
                } else {
                    "SEEK -00:10" to 0f
                }
        }
    )
}
){
        VideoSurface(modifier = Modifier.fillMaxSize(), contentScale = contentScale)
      AnimatedVisibility(
    visible = controlsVisible || isLocked,

    enter =
        fadeIn(
            animationSpec = tween(220)
        ) +
        slideInVertically {
            it / 12
        },

    exit =
        fadeOut(
            animationSpec = tween(180)
        ) +
        slideOutVertically {
            it / 18
        }
){

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.75f)
                    )
                )
            )
    )
}
        // Persistent Overlay Timeout
        LaunchedEffect(controlsVisible, isLocked, viewModel.isPlaying.value) {
            if (
    controlsVisible &&
    !isLocked &&
    viewModel.isPlaying.value &&
    !viewModel.isGestureInteracting.value
) {
                delay(4500)
                controlsVisible = false
            }
        }

        val brightnessAlpha = (1f - viewModel.videoBrightness.value).coerceIn(0f, 0.95f)
        if (brightnessAlpha > 0.01f) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = brightnessAlpha)))
        }

        var localBrightness by remember { mutableFloatStateOf(viewModel.videoBrightness.value) }
        var localVolume by remember { mutableFloatStateOf(viewModel.masterVolume.value) }
        var lastVolumeUpdate by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
        var lastBrightnessUpdate by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
        var pendingSeekPosition by remember { mutableLongStateOf(viewModel.currentPosition.value) }

        // Gesture Overlay Layer
        Box(
    modifier = Modifier
        .fillMaxSize()

        // Unified gesture system
        .pointerInput(currentDuration, isLocked) {

            if (isLocked) return@pointerInput

            var dragMode: GestureMode? = null
            var accumulatedSeek = 0f
            var accumulatedZoom = viewModel.videoZoom.value

            detectDragGestures(

                onDragStart = { offset ->

                    controlsVisible = false
                    viewModel.isGestureInteracting.value = true

                    dragMode = null
                    accumulatedSeek = 0f

                    localBrightness = viewModel.videoBrightness.value
                    localVolume = viewModel.masterVolume.value

                    pendingSeekPosition =
                        viewModel.currentPosition.value
                },

                onDragEnd = {

                    viewModel.isGestureInteracting.value = false

                    if (dragMode == GestureMode.SEEK) {

                        viewModel.seekTo(
                            pendingSeekPosition
                        )
                    }

                    dragMode = null
                    gestureUIState = null

                    controlsVisible = true
                },

                onDragCancel = {

                    viewModel.isGestureInteracting.value = false

                    dragMode = null
                    gestureUIState = null
                }

            ) { change, dragAmount ->

                change.consume()

                // Decide gesture direction once
                if (dragMode == null) {

                    dragMode =
                        if (
                            kotlin.math.abs(dragAmount.x) >
                            kotlin.math.abs(dragAmount.y)
                        ) {

                            GestureMode.SEEK

                        } else {

                            if (
                                change.position.x <
                                size.width / 2f
                            ) {
                                GestureMode.BRIGHTNESS
                            } else {
                                GestureMode.VOLUME
                            }
                        }
                }

                when (dragMode) {

                    GestureMode.SEEK -> {

                        accumulatedSeek += dragAmount.x

                        // smoother seek scaling
                        val seekDeltaMs =
                            (accumulatedSeek * 45L).toLong()

                        pendingSeekPosition =
                            (
                                viewModel.currentPosition.value +
                                seekDeltaMs
                            ).coerceIn(
                                0L,
                                currentDuration
                            )

                        val progress =
                            pendingSeekPosition.toFloat() /
                            currentDuration.toFloat()

                        val diff =

                                    pendingSeekPosition -

                                    viewModel.currentPosition.value

                                val seekLabel =

                                    if (diff >= 0) {

                                        "+${formatTime(diff)}"

                                    } else {

                                        "-${formatTime(kotlin.math.abs(diff))}"

                                    }

                                gestureUIState =

                                    "SEEK $seekLabel" to progress
                    }

                    GestureMode.BRIGHTNESS -> {

                        val delta =
                            (-dragAmount.y / 1600f)

                        localBrightness =
                            (
                                localBrightness + delta
                            ).coerceIn(
                                0.05f,
                                1f
                            )

                        if (
                            lastBrightnessUpdate
                                .elapsedNow()
                                .inWholeMilliseconds > 45L
                        ) {

                            lastBrightnessUpdate =
                                TimeSource.Monotonic.markNow()

                            viewModel.videoBrightness.value =
                                localBrightness

                            setBrightness(
                                localBrightness
                            )
                        }

                        gestureUIState =
                            "BRIGHTNESS" to localBrightness
                    }

                    GestureMode.VOLUME -> {

                        val delta =
                            (-dragAmount.y / 2500f)

                        localVolume =
                            (
                                localVolume + delta
                            ).coerceIn(
                                0f,
                                1f
                            )

                        if (
                            lastVolumeUpdate
                                .elapsedNow()
                                .inWholeMilliseconds > 45L
                        ) {

                            lastVolumeUpdate =
                                TimeSource.Monotonic.markNow()

                            viewModel.setMasterVolume(
                                localVolume
                            )
                        }

                        gestureUIState =
                            "VOLUME" to localVolume
                    }

                    null -> Unit
                }
            }
        }
)

        // Visual feedback for Gestures
         AnimatedVisibility(
            visible = gestureUIState != null,

            enter =
                fadeIn() +
                        scaleIn(
                            initialScale = 0.94f
                        ),

            exit =
                fadeOut() +
                        scaleOut(
                            targetScale = 0.94f
                        )
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),

                contentAlignment =
                    Alignment.Center
            ) {

                gestureUIState?.let {

                        (label, value) ->

                    Surface(
                        shape =
                            RoundedCornerShape(26.dp),

                        color =
    Color.Black.copy(alpha = 0.78f),

                        tonalElevation = 12.dp,

                        shadowElevation = 18.dp
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                horizontal = 22.dp,
                                vertical = 18.dp
                            ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = label,

                                color =
                                    Color.White,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize = 13.sp
                            )

                            Spacer(
                                Modifier.height(12.dp)
                            )

                            LinearProgressIndicator(
                                progress = {
                                    value.coerceIn(
                                        0f,
                                        1f
                                    )
                                },

                                modifier = Modifier
                                    .width(170.dp)
                                    .height(7.dp)
                                    .clip(CircleShape),

                                trackColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                            )

                            Spacer(
                                Modifier.height(10.dp)
                            )

                            Text(
                                text =

                                    if (
                                        label.startsWith(
                                            "SEEK"
                                        )
                                    ) {

                                       "${formatTime(pendingSeekPosition)} / ${formatTime(currentDuration)}"

                                    } else {

                                        "${(value * 100).toInt()}%"
                                    },

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface,

                                fontSize = 11.sp,

                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }


        // Controls Layer
        AnimatedVisibility(visible = controlsVisible || isLocked, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Lock Button (Persistent)
                Box(modifier = Modifier.fillMaxWidth().padding(top = 18.dp, end = 18.dp), contentAlignment = Alignment.TopEnd) {
                    FilledTonalIconButton(
                        onClick = { isLocked = !isLocked },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f))
                    ) {
                        Icon(imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }

                if (!isLocked) {
                    ImmersiveTopBar(
                        title = item.title,
                        isLocked = isLocked,
                        contentScale = contentScale,
                        onContentScaleChange = { contentScale = it },
                        onClose = {
                            viewModel.isVideoFullScreen.value = false
                            setSystemBarsVisibility(true)
                            setScreenOrientation(pEQOrientation.USER)
                            viewModel.currentTab.value = Tab.Dashboard
                        },
                        onRotate = { viewModel.cycleRotation() },
                        onLock = { isLocked = true },
                        onInfo = { controlsVisible = true; viewModel.showTrackInfoSheet.value = true }
                    )

                    // Playback Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilledTonalIconButton(onClick = { viewModel.seekRelative(-10000L) }, modifier = Modifier.size(50.dp)) { Icon(Icons.Default.Replay10, null) }
                        Spacer(Modifier.width(10.dp))
                        FilledTonalIconButton(onClick = { viewModel.previousTrack() }, modifier = Modifier.size(62.dp)) { Icon(Icons.Default.SkipPrevious, null) }
                        Spacer(Modifier.width(14.dp))
                        FilledIconButton(onClick = { viewModel.togglePlayback() }, modifier = Modifier
    .size(82.dp)
    .graphicsLayer {
        scaleX =
            if (viewModel.isPlaying.value) 1f
            else 1.08f

        scaleY =
            if (viewModel.isPlaying.value) 1f
            else 1.08f
    }) { 
                            Icon(if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(42.dp)) 
                        }
                        Spacer(Modifier.width(14.dp))
                        FilledTonalIconButton(onClick = { viewModel.nextTrack() }, modifier = Modifier.size(62.dp)) { Icon(Icons.Default.SkipNext, null) }
                        Spacer(Modifier.width(10.dp))
                        FilledTonalIconButton(onClick = { viewModel.seekRelative(10000L) }, modifier = Modifier.size(50.dp)) { Icon(Icons.Default.Forward10, null) }
                    }

                    // Bottom Panel
                    pEQGlassPanel(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).widthIn(max = 640.dp)) {
                        Column {
                            ImmersiveSeekSection(viewModel = viewModel)
                            Spacer(Modifier.height(8.dp))
                            ImmersiveControlsRow(viewModel = viewModel, item = item)
                        }
                    }
                }
            }
        }

        if (viewModel.isChaptersVisible.value) ChapterOverlayConsole(viewModel)
    }
}

@Composable
fun ImmersiveTopBar(
    title: String,
    isLocked: Boolean,
    contentScale: ContentScale,
    onContentScaleChange: (ContentScale) -> Unit,
    onClose: () -> Unit,
    onRotate: () -> Unit,
    onLock: () -> Unit,
    onInfo: () -> Unit
) {
    val viewModel = LocalAppViewModel.current
    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Spacer(Modifier.width(12.dp))
        Text(text = title, modifier = Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(12.dp))
        FilledTonalIconButton(onClick = {
            val next = when (contentScale) {
                ContentScale.Fit -> ContentScale.FillBounds
                ContentScale.FillBounds -> ContentScale.Crop
                else -> ContentScale.Fit
            }
            onContentScaleChange(next)
        }) { 
            Icon(imageVector = when (contentScale) { 
                ContentScale.Fit -> Icons.Default.AspectRatio
                ContentScale.FillBounds -> Icons.Default.Fullscreen
                else -> Icons.Default.FitScreen 
            }, contentDescription = "Resize") 
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = { viewModel.toggleSubtitles(!viewModel.isSubtitlesEnabled.value) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (viewModel.isSubtitlesEnabled.value) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) { Icon(Icons.Default.ClosedCaption, "Subs") }
        Spacer(Modifier.width(8.dp))
        var isLandscape by remember {
    mutableStateOf(true)
}

FilledTonalIconButton(
    onClick = {

        isLandscape = !isLandscape

        setScreenOrientation(
            if (isLandscape) {
                pEQOrientation.LANDSCAPE
            } else {
                pEQOrientation.PORTRAIT
            }
        )

        onRotate()
    }
) {

    Icon(
        imageVector = Icons.AutoMirrored.Filled.RotateRight,
        contentDescription = "Rotate"
    )
}
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(onClick = onInfo) { Icon(Icons.Default.Info, null) }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(onClick = {
            viewModel.onEnterPiP?.invoke()
        }) { Icon(Icons.Default.PictureInPicture, "PiP") }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(onClick = onLock) { Icon(if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, null) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImmersiveControlsRow(viewModel: AppViewModel, item: pEQMediaItem) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TransmissionTelemetry(viewModel = viewModel, item = item)
        MiniControlButton(icon = Icons.Default.Audiotrack, active = viewModel.showAudioTrackConsole.value) { viewModel.showAudioTrackConsole.value = true }
        MiniControlButton(icon = Icons.Default.Subtitles, active = viewModel.showSubtitleConsole.value) { viewModel.toggleSubtitleConsole() }
        MiniControlButton(icon = Icons.AutoMirrored.Filled.List, active = viewModel.isChaptersVisible.value) { viewModel.isChaptersVisible.value = true }
        MiniControlButton(icon = Icons.AutoMirrored.Filled.QueueMusic) { viewModel.showQueueDrawer.value = true }
        MiniControlButton(icon = Icons.AutoMirrored.Filled.PlaylistAdd) { 
            viewModel.selectedItemForPlaylist.value = item
            viewModel.isPlaylistPickerShown.value = true 
        }
        val isFav = viewModel.favoriteItems.any { it.path == item.path }
        MiniControlButton(icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, active = isFav) { 
            viewModel.toggleFavorite(item) 
        }
        Surface(
            onClick = { viewModel.cyclePlaybackSpeed() },
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
        ) {
            Box(contentAlignment = Alignment.Center) { 
                Text("${viewModel.playbackSpeed.value}x", fontSize = 10.sp, fontWeight = FontWeight.Black) 
            }
        }
        MiniControlButton(icon = Icons.Default.Shuffle, active = viewModel.isShuffleEnabled) { viewModel.toggleShuffle() }
        MiniControlButton(
            icon = if (viewModel.repeatMode.value == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, 
            active = viewModel.repeatMode.value != RepeatMode.OFF
        ) { viewModel.cycleRepeatMode() }
    }
}

@Composable
private fun ImmersiveSeekSection(viewModel: AppViewModel) {
    val haptics = LocalHapticFeedback.current
    val duration = viewModel.currentDuration.value.coerceAtLeast(1L)
    val currentPosition = viewModel.currentPosition.value.coerceIn(0L, duration)
    val chapters = viewModel.currentChapters
    val currentChapter = remember(currentPosition, chapters) { chapters.lastOrNull { currentPosition >= it.startMs } }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var lastHapticIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentPosition, duration, isDragging) { 
        if (!isDragging) dragProgress = currentPosition.toFloat() / duration.toFloat() 
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = currentChapter != null) {
            Surface(
                onClick = { viewModel.isChaptersVisible.value = true },
                modifier = Modifier.padding(horizontal = 14.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmarks, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(currentChapter?.title ?: "", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 14.dp)) {
            val widthPx = constraints.maxWidth.toFloat()
            Box(modifier = Modifier.fillMaxSize()
                .pointerInput(duration, chapters) {
                    detectDragGestures(
                        onDragStart = { offset -> isDragging = true; dragProgress = (offset.x / widthPx).coerceIn(0f, 1f) },
                        onDragEnd = { isDragging = false; viewModel.seekTo((dragProgress * duration).toLong()) }
                    ) { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val currentDragMs = (dragProgress * duration).toLong()
                        chapters.forEachIndexed { index, chapter ->
                            if (kotlin.math.abs(currentDragMs - chapter.startMs) <= 1200 && lastHapticIndex != index) {
                                lastHapticIndex = index
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
                .pointerInput(duration) { 
                    detectTapGestures { offset -> 
                        dragProgress = (offset.x / widthPx).coerceIn(0f, 1f)
                        viewModel.seekTo((dragProgress * duration).toLong()) 
                    } 
                }
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(5.dp).align(Alignment.CenterStart).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                Box(modifier = Modifier.fillMaxWidth(dragProgress).height(5.dp).align(Alignment.CenterStart).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    chapters.forEach { chapter ->
                        val x = (chapter.startMs.toFloat() / duration.toFloat()) * size.width
                        drawRoundRect(
                            color = Color.White, 
                            topLeft = Offset(x - 1.dp.toPx(), size.height / 2f - 4.dp.toPx()), 
                            size = Size(2.dp.toPx(), 8.dp.toPx()), 
                            cornerRadius = CornerRadius(100f)
                        )
                    }
                }
                Box(modifier = Modifier.offset { IntOffset((widthPx * dragProgress).toInt(), 0) }.align(Alignment.CenterStart).size(if (isDragging) 16.dp else 12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((dragProgress * duration).toLong()), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun ChapterOverlayConsole(
    viewModel: AppViewModel
) {

    val chapters =
        viewModel.currentChapters

    val currentPosition =
        viewModel.currentPosition.value

    val duration =
        viewModel.currentDuration.value
            .coerceAtLeast(1L)

    val listState =
        rememberLazyListState()

    // =========================
    // CURRENT INDEX
    // =========================

    val currentIndex =
        remember(
            currentPosition,
            chapters
        ) {

            chapters.indexOfLast {

                currentPosition >= it.startMs
            }
        }

    // =========================
    // AUTO SCROLL
    // =========================

    LaunchedEffect(currentIndex) {

        if (
            currentIndex >= 0 &&
            currentIndex < chapters.size
        ) {

            listState.animateScrollToItem(
                index = currentIndex
            )
        }
    }

    pEQEditorContainer(
        onDismiss = {

            viewModel.isChaptersVisible.value =
                false
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            // =========================
            // HEADER
            // =========================

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                FilledTonalIconButton(
                    onClick = {

                        viewModel.isChaptersVisible.value =
                            false
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Chapters",

                        fontSize = 22.sp,

                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text =
                            "${chapters.size} chapters",

                        fontSize = 11.sp,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    FilledTonalIconButton(
                        onClick = {
                            viewModel.fetchChapters()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Online")
                    }
                    
                    FilledTonalIconButton(
                        onClick = {
                            viewModel.requestChaptersPicker()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Import")
                    }

                    FilledIconButton(
                        onClick = {

                            viewModel.isChaptersVisible.value =
                                false

                            viewModel.isEditingChapters.value =
                                true
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Edit,

                            contentDescription = null
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            HorizontalDivider(
                color =
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
            )

            Spacer(Modifier.height(14.dp))

            // =========================
            // EMPTY
            // =========================

            if (chapters.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Bookmarks,

                            contentDescription = null,

                            modifier =
                                Modifier.size(42.dp),

                            tint =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                                    .copy(alpha = 0.5f)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text =
                                "No chapters detected",

                            fontSize = 13.sp,

                            fontWeight =
                                FontWeight.SemiBold,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }

            // =========================
            // LIST
            // =========================

            else {

                LazyColumn(
                    state = listState,

                    modifier = Modifier
                        .fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp),

                    contentPadding =
                        PaddingValues(
                            bottom = 40.dp
                        )
                ) {

                    itemsIndexed(

                        items = chapters,

                        key = { index, chapter ->

                            "${chapter.id}_$index"

                        }

                    ){ index, chapter ->

                        val nextChapter =
                            chapters.getOrNull(index + 1)

                        val endMs =

                            chapter.endMs
                                .takeIf {
                                    it > chapter.startMs
                                }

                                ?: nextChapter?.startMs

                                ?: duration

                        val chapterDuration =
                            (
                                    endMs -
                                            chapter.startMs
                                    ).coerceAtLeast(1L)

                        val isCurrent =
                            currentPosition in
                                    chapter.startMs until endMs

                        val progress =

                            if (isCurrent) {

                                (
                                        (
                                                currentPosition -
                                                        chapter.startMs
                                                ).toFloat() /

                                                chapterDuration.toFloat()
                                        ).coerceIn(0f, 1f)

                            } else {

                                0f
                            }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),

                            shape =
                                RoundedCornerShape(24.dp),

                            color =

                                if (isCurrent) {

                                    MaterialTheme.colorScheme
                                        .primary
                                        .copy(alpha = 0.14f)

                                } else {

                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh
                                },

                            border = BorderStroke(
                                width = 1.dp,

                                color =

                                    if (isCurrent) {

                                        MaterialTheme.colorScheme
                                            .primary
                                            .copy(alpha = 0.18f)

                                    } else {

                                        MaterialTheme.colorScheme
                                            .outline
                                            .copy(alpha = 0.05f)
                                    }
                            ),

                            onClick = {

                                viewModel.seekTo(
                                    chapter.startMs
                                )

                                viewModel.isChaptersVisible.value =
                                    false
                            }
                        ) {

                            Column {

                                AnimatedVisibility(
                                    visible = isCurrent
                                ) {

                                    LinearProgressIndicator(
                                        progress = {
                                            progress
                                        },

                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp),

                                        color =
                                            MaterialTheme.colorScheme
                                                .primary,

                                        trackColor =
                                            Color.Transparent
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 14.dp,
                                            vertical = 14.dp
                                        ),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Surface(
                                        modifier =
                                            Modifier.size(46.dp),

                                        shape =
                                            CircleShape,

                                        color =

                                            if (isCurrent) {

                                                MaterialTheme
                                                    .colorScheme
                                                    .primary

                                            } else {

                                                MaterialTheme
                                                    .colorScheme
                                                    .primary
                                                    .copy(alpha = 0.12f)
                                            }
                                    ) {

                                        Box(
                                            contentAlignment =
                                                Alignment.Center
                                        ) {

                                            if (isCurrent) {

                                                Icon(
                                                    imageVector =
                                                        Icons.Default.PlayArrow,

                                                    contentDescription = null,

                                                    tint =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .onPrimary
                                                )

                                            } else {

                                                Text(
                                                    text =
                                                        "${index + 1}",

                                                    fontSize = 12.sp,

                                                    fontWeight =
                                                        FontWeight.Bold,

                                                    color =
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary
                                                )
                                            }
                                        }
                                    }

                                    Spacer(
                                        Modifier.width(14.dp)
                                    )

                                    Column(
                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text =
                                                chapter.title,

                                            fontSize = 14.sp,

                                            fontWeight =

                                                if (isCurrent) {

                                                    FontWeight.Bold

                                                } else {

                                                    FontWeight.SemiBold
                                                },

                                            maxLines = 1,

                                            overflow =
                                                TextOverflow.Ellipsis
                                        )

                                        Spacer(
                                            Modifier.height(5.dp)
                                        )

                                        Row(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(
                                                    8.dp
                                                ),

                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {

                                            Text(
                                                text =
                                                    formatTime(
                                                        chapter.startMs
                                                    ),

                                                fontSize = 11.sp,

                                                fontWeight =
                                                    FontWeight.SemiBold,

                                                color =

                                                    if (isCurrent) {

                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary

                                                    } else {

                                                        MaterialTheme
                                                            .colorScheme
                                                            .onSurfaceVariant
                                                    }
                                            )

                                            Text(
                                                text = "•"
                                            )

                                            Text(
                                                text =
                                                    formatTime(
                                                        chapterDuration
                                                    ),

                                                fontSize = 11.sp,

                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(
                                        Modifier.width(10.dp)
                                    )

                                    Icon(
                                        imageVector =
                                            Icons.AutoMirrored
                                                .Filled
                                                .ArrowForward,

                                        contentDescription = null,

                                        tint =

                                            if (isCurrent) {

                                                MaterialTheme
                                                    .colorScheme
                                                    .primary

                                            } else {

                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant
                                                    .copy(alpha = 0.6f)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class GestureMode { BRIGHTNESS, VOLUME, SEEK }
private fun shouldUseLandscape(
    width: Int,
    height: Int
): Boolean {

    if (width <= 0 || height <= 0) {
        return true
    }

    return width >= height
}