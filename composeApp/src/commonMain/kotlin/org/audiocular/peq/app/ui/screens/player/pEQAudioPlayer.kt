package org.audiocular.peq.app.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.screens.*
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.viewmodel.toggleLyrics
import org.audiocular.peq.app.ui.viewmodel.toggleLockScreenMode
import org.audiocular.peq.app.ui.viewmodel.cyclePlaybackSpeed
import org.audiocular.peq.app.ui.viewmodel.toggleShuffle
import org.audiocular.peq.app.ui.viewmodel.cycleRepeatMode
import org.audiocular.peq.app.ui.components.pEQDesign
import org.audiocular.peq.app.data.RepeatMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import kotlin.math.pow
import kotlinx.coroutines.flow.first
import org.audiocular.peq.app.pEQBackHandler
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.snapshotFlow


@Composable
fun NeuralAudioWorkstation(
    viewModel: AppViewModel
) {
    val currentItem = remember(viewModel.currentMediaItem.value) {
        viewModel.currentMediaItem.value
    } ?: return

    LaunchedEffect(currentItem.id) {
        setSystemBarsVisibility(false)
    }

    DisposableEffect(Unit) {
        onDispose {
            setSystemBarsVisibility(true)
        }
    }

    val isFocusMode by viewModel.isFocusMode
    val isLockScreenMode by viewModel.isLockScreenMode
    val isPlaying by viewModel.isPlaying
    val isLyricsVisible by viewModel.isLyricsVisible
    val visualizerMode by viewModel.visualizerMode
    val rtaData by viewModel.rtaData
    val dominantColor by viewModel.dominantColor
    val ambientIntensity by viewModel.ambientIntensity

    Box(modifier = Modifier.fillMaxSize().background(if (isFocusMode) Color.Black else MaterialTheme.colorScheme.background)) {
        if (!isFocusMode) {
            DynamicAmbientBackdrop(dominantColor = dominantColor, intensity = ambientIntensity)

            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(dominantColor.copy(alpha = 0.11f), Color.Transparent),
                        radius = 1450f
                    )
                )
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(if (isFocusMode) 0.dp else 0.dp)) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // 🚀 FLOATING BACK BUTTON (Absolute Position)
                    FilledTonalIconButton(
                        onClick = { viewModel.currentTab.value = Tab.Dashboard },
                        modifier = Modifier.padding(24.dp).size(52.dp).zIndex(20f),
                        shape = RoundedCornerShape(18.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(24.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT PANEL
                        Surface(
                            modifier = Modifier.weight(1.1f).fillMaxHeight(),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.30f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                                val showLyrics by viewModel.isLyricsVisible
                                AnimatedContent(
                                    targetState = if (viewModel.isHarmonyVisible.value) "harmony" else if (showLyrics) "lyrics" else "vinyl",
                                    label = "playback_switch"
                                ) { mode ->
                                    when(mode) {
                                        "harmony" -> ChordVisualizerScreen(viewModel)
                                        "lyrics" -> LyricsVinylCard(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                        else -> AudioVinylEngine(item = currentItem, isPlaying = isPlaying)
                                    }
                                }
                            }
                        }

                        // RIGHT PANEL
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 500.dp)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.42f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                            tonalElevation = 10.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 22.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                NeuralHeaderPlayerControls(viewModel)
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(currentItem.title, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(currentItem.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PlayerTimeSlider(viewModel = viewModel)
                                    TransportControlsRestored(viewModel)
                                    NeuralBottomControlsRestored(viewModel)
                                }
                                
                                TransmissionTelemetry(viewModel = viewModel, item = currentItem)
                            }
                        }
                    }
                }
            }


 else {
                Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    NeuralPlayerHeaderSimple(viewModel = viewModel, isLandscape = false)

                    Column(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().weight(0.92f), contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = isLyricsVisible,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(260)) togetherWith fadeOut(animationSpec = tween(180))
                                },
                                label = "portrait_switch"
                            ) { showLyrics ->
                                if (showLyrics) {
                                        LyricsVinylCard(viewModel = viewModel, modifier = Modifier.fillMaxSize().padding(12.dp))
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
                                        when (visualizerMode) {
                                            AppViewModel.VisualizerMode.CIRCULAR -> {
                                                CircularSpectrumAnalyzer(magnitudes = rtaData)
                                            }
                                            AppViewModel.VisualizerMode.RTA -> {
                                                RtaSpectrumVisualizer(magnitudes = rtaData)
                                            }
                                            AppViewModel.VisualizerMode.WAVEFORM -> {
                                                WaveformVisualizer(viewModel = viewModel)
                                            }
                                            else -> {

    currentItem?.let {

        AudioVinylEngine(
            item = it,
            isPlaying = viewModel.isPlaying.value
        )

    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "No Media",
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}
                                            
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 10.dp,
                            shadowElevation = 18.dp
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                TransportControlsRestored(viewModel)
                                Spacer(modifier = Modifier.height(20.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = currentItem.title.ifBlank { "Unknown Track" }, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = currentItem.subtitle.ifBlank { "Unknown Artist" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                PlayerTimeSlider(viewModel)
                                Spacer(modifier = Modifier.height(20.dp))
                                NeuralBottomControlsRestored(viewModel)
                                Spacer(modifier = Modifier.weight(1f))
                                TransmissionTelemetry(viewModel = viewModel, item = currentItem)
                            }
                        }
                    }
                }
            }
        }

        // FLOATING PANELS (A-B Repeat, Cutter)
        AnimatedVisibility(
            visible = viewModel.abRepeatActive.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            DualPointControlConsole(
                title = "A-B Looper",
                startLabel = "POINT A",
                endLabel = "POINT B",
                startTime = viewModel.abRepeatPointA.value,
                endTime = viewModel.abRepeatPointB.value,
                onStartClick = { viewModel.setABPoint('A') },
                onEndClick = { viewModel.setABPoint('B') },
                onCloseClick = { viewModel.toggleAbRepeat() },
                mainActionIcon = Icons.Default.Refresh,
                mainActionLabel = "RESET",
                mainActionEnabled = viewModel.abRepeatPointA.value != -1L || viewModel.abRepeatPointB.value != -1L,
                onMainActionClick = {
                    viewModel.abRepeatPointA.value = -1L
                    viewModel.abRepeatPointB.value = -1L
                    viewModel.updateLoopPoints(null, null)
                }
            )
        }

        AnimatedVisibility(
            visible = viewModel.showCutter.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        ) {
            DualPointControlConsole(
                title = "Trimmer",
                startLabel = "START",
                endLabel = "END",
                startTime = viewModel.trimStartMs.value,
                endTime = viewModel.trimEndMs.value,
                onStartClick = { viewModel.trimStartMs.value = viewModel.currentPosition.value },
                onEndClick = { viewModel.trimEndMs.value = viewModel.currentPosition.value },
                onCloseClick = { viewModel.showCutter.value = false },
                mainActionIcon = Icons.Default.ContentCut,
                mainActionLabel = "EXPORT",
                mainActionEnabled = viewModel.trimEndMs.value > viewModel.trimStartMs.value && viewModel.trimStartMs.value != -1L,
                onMainActionClick = {
                    viewModel.trimMedia(viewModel.trimStartMs.value, viewModel.trimEndMs.value)
                }
            )
        }
        
    }
}

@Composable
fun PlayerTimeSlider(viewModel: AppViewModel) {
    val currentPos = viewModel.currentPosition.longValue
    val totalDur = viewModel.currentDuration.value
    val showHours = totalDur >= 3600_000L
    val rtaData by viewModel.rtaData
    var localValue by remember { mutableFloatStateOf(currentPos.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderWidth by remember { mutableIntStateOf(0) }
    val barCount = 56

    val animatedLevels = remember { List(barCount) { Animatable(0.12f) } }

    LaunchedEffect(rtaData, currentPos) {
        val halfBars = barCount / 2
        val playedBars = (progressFraction(currentPos, totalDur) * barCount).toInt()
        animatedLevels.forEachIndexed { index, anim ->
            if (index > playedBars) {
                launch { anim.animateTo(0.16f, tween(260)) }
                return@forEachIndexed
            }
            val mirroredIndex = if (index < halfBars) halfBars - index else index - halfBars
            val raw = rtaData.getOrNull(mirroredIndex) ?: 0f
            val processed = (raw.pow(0.52f) * 1.15f).coerceIn(0.12f, 0.92f)
            launch { anim.animateTo(processed, tween(150, easing = LinearOutSlowInEasing)) }
        }
    }

    LaunchedEffect(currentPos) { if (!isDragging) localValue = currentPos.toFloat() }

    val progress = (localValue / totalDur.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(120), label = "progress_animation")

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(16.dp))
                .onSizeChanged { sliderWidth = it.width }
                .pointerInput(totalDur) {
                    detectTapGestures { offset ->
                        val tapProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        localValue = tapProgress * totalDur
                        viewModel.seekTo(localValue.toLong())
                    }
                }
                .pointerInput(totalDur) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            localValue = dragProgress * totalDur
                            // Don't seek here to avoid engine lag, only update local UI
                        },
                        onDragEnd = { 
                            isDragging = false
                            viewModel.seekTo(localValue.toLong())
                        },
                        onDragCancel = { isDragging = false }
                    )
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(modifier = Modifier.matchParentSize().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)))
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
                animatedLevels.forEachIndexed { index, anim ->
                    val normalizedIndex = index / barCount.toFloat()
                    val isPlayed = normalizedIndex <= animatedProgress
                    val frequencyWeight = when {
                        index < 10 -> 0.7f
                        index < 22 -> 0.9f
                        index < 38 -> 1f
                        else -> 0.78f
                    }
                    val finalHeight = (anim.value * frequencyWeight).coerceIn(0.14f, 1f)
                    val activeColor = if (isPlayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(finalHeight).clip(CircleShape).background(Brush.verticalGradient(listOf(activeColor, activeColor.copy(alpha = 0.75f), activeColor.copy(alpha = 0.18f)))))
                }
            }
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)))))
            Box(modifier = Modifier.align(Alignment.BottomStart).offset { IntOffset(((sliderWidth - 14) * animatedProgress).toInt(), 0) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(2.dp).height(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.height(2.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).border(1.5.dp, Color.White.copy(alpha = 0.92f), CircleShape))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(localValue.toLong(), showHours), fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Text(text = formatTime(totalDur, showHours), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f))
        }
    }
}

private fun progressFraction(current: Long, total: Long): Float = (current / total.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)

@Composable
private fun EmptyLyricsState(
    viewModel: AppViewModel
) {
    val lyricJob = viewModel.backgroundJobs.find { it.name == "Neural Lyrics Sync" }
    val isSearching = lyricJob != null && lyricJob.status != "NO MATCH FOUND" && lyricJob.status != "FETCH FAILED"

    val infiniteTransition = rememberInfiniteTransition(label = "lyrics_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSearching) {
            Box(
                modifier = Modifier.size(84.dp).border(width = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), shape = CircleShape)
                    .graphicsLayer { rotationZ = rotation },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            }
        } else {
            Surface(modifier = Modifier.size(84.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = if (lyricJob?.status == "NO MATCH FOUND") Icons.Default.SearchOff else Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(38.dp))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = if (lyricJob?.status == "NO MATCH FOUND") "No Lyrics Found" else if (isSearching) "Searching Lyrics" else "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = when {
                lyricJob?.status == "NO MATCH FOUND" -> "We couldn't find synchronized lyrics for this track in our neural archive."
                isSearching -> lyricJob?.status ?: "Neural sync engine is analyzing temporal vocal data..."
                else -> "Establishing connection to the pEQ metadata repository..."
            },
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        if (!isSearching && lyricJob?.status == "NO MATCH FOUND") {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.webSearchLyrics() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Language, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("SEARCH ONLINE", fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LyricsVinylCard(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val lyrics = viewModel.syncedLyrics
    val listState = rememberLazyListState()
    val currentPosition = viewModel.currentPosition.value

    var controlsVisible by remember {
        mutableStateOf(true)
    }

    val currentLineIndex by remember(lyrics, currentPosition) {
        derivedStateOf {
            lyrics.indexOfLast {
                it.timestampMs <= currentPosition
            }.coerceAtLeast(0)
        }
    }

    val currentLine = lyrics.getOrNull(currentLineIndex)
    val nextLine = lyrics.getOrNull(currentLineIndex + 1)

    val progressToNext by remember(
        currentPosition,
        currentLine,
        nextLine
    ) {
        derivedStateOf {
            if (currentLine == null || nextLine == null) {
                0f
            } else {
                (
                    (currentPosition - currentLine.timestampMs).toFloat() /
                        (nextLine.timestampMs - currentLine.timestampMs)
                            .toFloat()
                            .coerceAtLeast(1f)
                    ).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(
        controlsVisible,
        viewModel.isEditingLyrics.value
    ) {
        if (
            controlsVisible &&
            !viewModel.isEditingLyrics.value
        ) {
            delay(4000)
            controlsVisible = false
        }
    }

    /*
     * FIXED AUTO SCROLL
     */
  LaunchedEffect(currentLineIndex) {

    if (
        lyrics.isNotEmpty() &&
        currentLineIndex >= 0
    ) {

        val targetIndex =
            (currentLineIndex - 1)
                .coerceAtLeast(0)

        listState.scrollToItem(
            index = targetIndex,
            scrollOffset = 45
        )
    }
}

    Box(
    modifier = modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(32.dp))
        .background(Color.Transparent)
        .border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.08f
            ),
            shape = RoundedCornerShape(32.dp)
        )
  
        .pointerInput(Unit) {
            detectTapGestures {
                controlsVisible = !controlsVisible
            }
        }
){

        if (viewModel.isEditingLyrics.value) {

            MetadataEditorContent(
                viewModel = viewModel,
                type = MetadataEditorType.LYRICS,
                isCompact = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )

        } else if (lyrics.isEmpty()) {

            EmptyLyricsState(viewModel)

        } else {
LazyColumn(

    state = listState,

    modifier = Modifier.fillMaxSize()
    .graphicsLayer {

    compositingStrategy =

        CompositingStrategy.Offscreen},

    verticalArrangement = Arrangement.spacedBy(18.dp),

    contentPadding = PaddingValues(

        top = 48.dp,

        bottom = 180.dp,

        start = 28.dp,

        end = 28.dp

    )

){

                itemsIndexed(
                    items = lyrics,
                    key = { index, line ->
                        "${line.text}_${line.timestampMs}_$index"
                    }
                ) { index, line ->

                    val isActive = index == currentLineIndex

                    LyricLineItem(
                        line = line,
                        isActive = isActive,
                        progressToNext = progressToNext,
                        onClick = {
                            viewModel.seekTo(line.timestampMs)
                        }
                    )
                }
            }

           
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            visible = controlsVisible &&
                !viewModel.isEditingLyrics.value,
            enter = fadeIn() +
                slideInVertically {
                    -it / 2
                },
            exit = fadeOut() +
                slideOutVertically {
                    -it / 2
                }
        ) {

            Surface(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
                    .copy(alpha = 0.72f),
                tonalElevation = 4.dp,
                shadowElevation = 2.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    SmallActionButton(
                        icon = Icons.Default.Search
                    ) {
                        viewModel.webSearchLyrics()
                    }

                    SmallActionButton(
                        icon = Icons.Default.Language
                    ) {
                        viewModel.fetchLyrics(
                            autoOpenEditor = false
                        )
                    }

                    SmallActionButton(
                        icon = Icons.Default.FolderOpen
                    ) {
                        viewModel.requestLyricsPicker { }
                    }

                    SmallActionButton(
                        icon = Icons.Default.Edit
                    ) {
                        viewModel.isEditingLyrics.value = true
                    }
                }
            }
        }
    }
}

@Composable
fun TransportControlsRestored(viewModel: AppViewModel) {
    val item = viewModel.currentMediaItem.value ?: return
    val isFavorite = viewModel.favoriteItems.any { it.path == item.path }
    val _repeatMode by viewModel.repeatMode
    var showVolumePopup by remember { mutableStateOf(false) }
    val volumeIcon = when {
        viewModel.masterVolume.value > 0.5f -> Icons.AutoMirrored.Filled.VolumeUp
        viewModel.masterVolume.value > 0f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeMute
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SmallTransportButton(icon = volumeIcon, active = showVolumePopup) { showVolumePopup = !showVolumePopup }
        if (showVolumePopup) {
            Popup(alignment = Alignment.TopCenter, offset = IntOffset(0, -280), onDismissRequest = { showVolumePopup = false }) {
                Surface(modifier = Modifier.padding(horizontal = 16.dp).width(280.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, tonalElevation = 12.dp) {
                    Box(Modifier.padding(24.dp)) { VolumeControlSlider(viewModel = viewModel) }
                }
            }
        }
        FilledTonalIconButton(onClick = { viewModel.previousTrack() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(30.dp)) }
        Surface(onClick = { viewModel.togglePlayback() }, modifier = Modifier.size(78.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp) {
            Box(contentAlignment = Alignment.Center) { Icon(if (viewModel.isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        }
        FilledTonalIconButton(onClick = { viewModel.nextTrack() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(30.dp)) }
        SmallTransportButton(icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, active = isFavorite) { viewModel.toggleFavorite(item) }
        SmallTransportButton(icon = Icons.Default.AutoAwesome, active = viewModel.isAiUpscalingEnabled.value) { viewModel.toggleAiUpscaling() }
    }
}

@Composable
fun NeuralPlayerHeaderSimple(viewModel: AppViewModel, isLandscape: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp), verticalAlignment = Alignment.Top) {
            FilledTonalIconButton(onClick = { viewModel.currentTab.value = Tab.Dashboard }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(top = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "pEQ NEURAL INTERFACE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.6.sp, maxLines = 1)
            }
            FilledTonalIconButton(onClick = { viewModel.showTrackInfoSheet.value = true }, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp)) }
        }
        if (!isLandscape) NeuralHeaderPlayerControls(viewModel)
    }
}

@Composable
fun NeuralBottomControlsRestored(viewModel: AppViewModel) {
    val repeatMode by viewModel.repeatMode
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        SmallTransportButton(icon = Icons.Default.Shuffle, active = viewModel.isShuffleEnabled) { viewModel.toggleShuffle() }
        SmallTransportButton(icon = when (repeatMode) { RepeatMode.ONE -> Icons.Default.RepeatOne; RepeatMode.FOLDER -> Icons.Default.RepeatOn; else -> Icons.Default.Repeat }, active = repeatMode != RepeatMode.OFF) {
            viewModel.cycleRepeatMode()
            if (viewModel.repeatMode.value != RepeatMode.OFF) viewModel.abRepeatActive.value = false
        }
        SmallTransportButton(icon = Icons.AutoMirrored.Filled.PlaylistAdd, active = viewModel.isPlaylistPickerShown.value) { viewModel.isPlaylistPickerShown.value = true }
        SmallTransportButton(icon = Icons.AutoMirrored.Filled.QueueMusic, active = viewModel.showQueueDrawer.value) { viewModel.showQueueDrawer.value = true }
        if (viewModel.currentMediaItem.value?.type != MediaType.VIDEO) {
        SmallTransportButton(
    icon = Icons.Default.Lyrics,
    active = viewModel.isLyricsVisible.value
) {

    val wasVisible =
        viewModel.isLyricsVisible.value

    viewModel.toggleLyrics()

    /*
     * FETCH ONLY WHEN OPENING
     */
    if (
        !wasVisible &&
        viewModel.isLyricsVisible.value &&
        viewModel.syncedLyrics.isEmpty()
    ) {

        viewModel.fetchLyrics()
    }
}
        }
        SmallTransportButton(icon = Icons.Default.Speed, active = viewModel.playbackSpeed.value != 1f) { viewModel.cyclePlaybackSpeed() }
        SmallTransportButton(icon = Icons.Default.VpnKey, active = viewModel.isFocusMode.value) {
            viewModel.toggleFocusMode()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeuralHeaderPlayerControls(viewModel: AppViewModel) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PlayerActionButton(icon = if (viewModel.abRepeatActive.value) Icons.Default.AllInclusive else Icons.Default.Sync, active = viewModel.abRepeatActive.value) {
            viewModel.toggleAbRepeat()
            if (!viewModel.abRepeatActive.value) { viewModel.abRepeatPointA.value = -1L; viewModel.abRepeatPointB.value = -1L; viewModel.updateLoopPoints(null, null) }
        }
        // PlayerActionButton(icon = Icons.Default.AutoAwesome, active = viewModel.visualizerMode.value != AppViewModel.VisualizerMode.BARS) {
        //     val modes = AppViewModel.VisualizerMode.entries
        //     viewModel.visualizerMode.value = modes[(viewModel.visualizerMode.value.ordinal + 1) % modes.size]
        // }
        PlayerActionButton(icon = Icons.Default.Timer, active = viewModel.sleepTimerActive.value) { viewModel.showSleepTimerSheet.value = true }
        PlayerActionButton(icon = Icons.Default.ContentCut, active = viewModel.showCutter.value) { viewModel.showCutter.value = !viewModel.showCutter.value }
        PlayerActionButton(icon = if (viewModel.isHarmonyVisible.value) Icons.Default.MusicNote else Icons.Default.Audiotrack, active = viewModel.isHarmonyVisible.value) { viewModel.isHarmonyVisible.value = !viewModel.isHarmonyVisible.value }
        PlayerActionButton(icon = if (viewModel.isMicMonitoringEnabled.value) Icons.Default.MicExternalOn else Icons.Default.MicExternalOff, active = viewModel.isMicMonitoringEnabled.value) { viewModel.toggleMicMonitoring() }
        
    }
}

@Composable
fun DynamicAmbientBackdrop(dominantColor: Color, intensity: Float = 0f) {
    val animatedDominant by animateColorAsState(targetValue = dominantColor, animationSpec = tween(1200))
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(animatedDominant.copy(alpha = 0.45f + (intensity * 0.1f)), MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f), MaterialTheme.colorScheme.background.copy(alpha = 0.95f), MaterialTheme.colorScheme.background))))
    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(animatedDominant.copy(alpha = 0.35f + (intensity * 0.25f)), animatedDominant.copy(alpha = 0.15f + (intensity * 0.1f)), Color.Transparent), center = Offset(0f, 0f), radius = 1600f + (intensity * 800f))))
    Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(colors = listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f + (intensity * 0.18f)), Color.Transparent), center = Offset(1200f, 1200f), radius = 1200f + (intensity * 600f))))
    Box(modifier = Modifier.fillMaxSize().alpha(0.04f).background(Color.Black))
}

@Composable
fun rememberArtworkGradient(item: pEQMediaItem): List<Color> {
    val primary = MaterialTheme.colorScheme.primary
    return remember(item.thumbnail, item.id, primary, item.title, item.artist) {
        // Derive hue from metadata for pseudo-relevant coloring
        val seed = (item.title + item.artist + (item.thumbnail ?: "")).hashCode()
        val hue = (seed % 360).let { if (it < 0) it + 360 else it }.toFloat()
        
        // Dynamic saturations based on track energy (simulated)
        val dominant = Color.hsl(hue, 0.55f, 0.18f)
        val secondary = Color.hsl((hue + 45) % 360, 0.42f, 0.12f)
        val tertiary = Color.hsl((hue + 90) % 360, 0.35f, 0.06f)
        
        listOf(dominant, secondary, tertiary, Color.Black)
    }
}

@Composable
fun LyricLineItem(
    line: SyncedLine,
    isActive: Boolean,
    progressToNext: Float,
    onClick: () -> Unit
) {

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = 0.92f,
            stiffness = 400f
        ),
        label = "lyrics_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.42f,
        animationSpec = tween(250),
        label = "lyrics_alpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) progressToNext else 0f,
        animationSpec = tween(140),
        label = "lyrics_progress"
    )

    val animatedColor by animateColorAsState(
        targetValue =
            if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = tween(220),
        label = "lyrics_color"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
          
            .clickable {
                onClick()
            }
            .padding(vertical = 4.dp)
    ) {

        /*
         * BASE TEXT
         */
        Text(
            text = line.text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isActive) 0.22f else 0.55f
            )
        )

        /*
         * PROGRESS FILL TEXT
         */
        if (isActive) {

            Text(
                text = line.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {

                        val progressWidth =
                            size.width * animatedProgress

                        clipRect(
                            left = 0f,
                            top = 0f,
                            right = progressWidth,
                            bottom = size.height
                        ) {
                            this@drawWithContent.drawContent()
                        }
                    },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp
                ),
                color = animatedColor
            )
        }
    }
}
@Composable
fun LargeTrackInfo(item: org.audiocular.peq.app.data.pEQMediaItem) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      
        
        Spacer(Modifier.height(28.dp))
        
        Text(
            text = item.title,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 42.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            ),
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = item.subtitle,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        )
    }
}
