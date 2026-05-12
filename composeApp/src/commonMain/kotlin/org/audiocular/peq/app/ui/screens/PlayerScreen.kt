@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.data.MediaType
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.pEQOrientation
import org.audiocular.peq.app.pEQBackHandler
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.screens.player.*
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import org.audiocular.peq.app.ui.components.LyricsWebView
import org.audiocular.peq.app.ui.components.UnifiedMetadataEditor
import org.audiocular.peq.app.ui.components.MetadataEditorType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    viewModel: AppViewModel = LocalAppViewModel.current,
    item: pEQMediaItem? = viewModel.currentMediaItem.value
) {
    val currentItem = item
    
    if (viewModel.isPiPMode.value && currentItem != null) {
        PiPPlayerView(viewModel, currentItem)
        return
    }

    if (currentItem == null) {
        LaunchedEffect(Unit) {
            setSystemBarsVisibility(true)
            setScreenOrientation(pEQOrientation.USER)
        }
        EmptyPlayerState(onBack = { viewModel.currentTab.value = Tab.Dashboard })
        return
    }

    val showOverlay by remember {
        derivedStateOf {
            viewModel.showQueueDrawer.value ||
                    viewModel.isPlaylistPickerShown.value ||
                    viewModel.showTrackInfoSheet.value ||
                    viewModel.showSleepTimerSheet.value ||
                    viewModel.showStemSplitter.value ||
                    viewModel.isEditingLyrics.value ||
                    viewModel.isEditingChapters.value ||
                    viewModel.isEditingSubtitles.value ||
                    viewModel.showSubtitleConsole.value ||
                    viewModel.showAudioTrackConsole.value ||
                    viewModel.isChaptersVisible.value
        }
    }

    val showBufferingOverlay by remember {
        derivedStateOf {
            viewModel.isBuffering.value && (currentItem.type == MediaType.VIDEO || viewModel.isSeeking.value)
        }
    }

    pEQBackHandler(enabled = true) {
        when {
            viewModel.isEditingSubtitles.value -> viewModel.isEditingSubtitles.value = false
            viewModel.isEditingChapters.value -> viewModel.isEditingChapters.value = false
            viewModel.isEditingLyrics.value -> viewModel.isEditingLyrics.value = false
            viewModel.showSubtitleConsole.value -> viewModel.showSubtitleConsole.value = false
            viewModel.showAudioTrackConsole.value -> viewModel.showAudioTrackConsole.value = false
            viewModel.isLyricsVisible.value -> viewModel.isLyricsVisible.value = false
            viewModel.isChaptersVisible.value -> viewModel.isChaptersVisible.value = false
            viewModel.showQueueDrawer.value -> viewModel.showQueueDrawer.value = false
            viewModel.isPlaylistPickerShown.value -> viewModel.isPlaylistPickerShown.value = false
            viewModel.showTrackInfoSheet.value -> viewModel.showTrackInfoSheet.value = false
            viewModel.showSleepTimerSheet.value -> viewModel.showSleepTimerSheet.value = false
            viewModel.showStemSplitter.value -> viewModel.showStemSplitter.value = false
            viewModel.isVideoFullScreen.value -> {
                viewModel.isVideoFullScreen.value = false
                setSystemBarsVisibility(true)
                setScreenOrientation(pEQOrientation.USER)
            }
            else -> {
                setScreenOrientation(pEQOrientation.USER)
                setSystemBarsVisibility(true)
                viewModel.currentTab.value = Tab.Dashboard
            }
        }
    }

    DisposableEffect(currentItem.type, viewModel.isVideoFullScreen.value) {
        val immersiveVideo = currentItem.type == MediaType.VIDEO && viewModel.isVideoFullScreen.value
        setSystemBarsVisibility(!immersiveVideo)
        onDispose {
            setSystemBarsVisibility(true)
            setScreenOrientation(pEQOrientation.USER)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(targetState = currentItem.type, label = "player_mode_transition") { mediaType ->
            when (mediaType) {
                MediaType.VIDEO -> ImmersiveCinemaEngine(viewModel = viewModel, item = currentItem)
                else -> NeuralAudioWorkstation(viewModel = viewModel)
            }
        }

        AnimatedVisibility(visible = showOverlay, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(20f)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)))
        }

        AnimatedVisibility(visible = showBufferingOverlay, enter = fadeIn(animationSpec = tween(120)), exit = fadeOut(animationSpec = tween(180)), modifier = Modifier.zIndex(100f)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), tonalElevation = 8.dp) {
                    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(34.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        DiagnosticsOverlay(viewModel)

        AnimatedVisibility(visible = viewModel.showQueueDrawer.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(30f)) {
            PlayerQueueDrawer(viewModel) { viewModel.showQueueDrawer.value = false }
        }

        AnimatedVisibility(visible = viewModel.isPlaylistPickerShown.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(30f)) {
            PlaylistPicker(viewModel) { viewModel.isPlaylistPickerShown.value = false }
        }

        AnimatedVisibility(visible = viewModel.showTrackInfoSheet.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(40f)) {
            TrackInfoBottomSheet(item = currentItem, viewModel = viewModel, onDismiss = { viewModel.showTrackInfoSheet.value = false })
        }

        AnimatedVisibility(
            visible = viewModel.showSleepTimerSheet.value,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).zIndex(40f)
        ) {
            SleepTimerFloater(viewModel = viewModel, onDismiss = { viewModel.showSleepTimerSheet.value = false })
        }

        AnimatedVisibility(visible = viewModel.showStemSplitter.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(50f)) {
            StemSplitterDialog { viewModel.showStemSplitter.value = false }
        }

        AnimatedVisibility(visible = viewModel.isChaptersVisible.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(60f)) {
            ChapterOverlayConsole(viewModel = viewModel)
        }

        AnimatedVisibility(visible = viewModel.isEditingChapters.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(60f)) {
            UnifiedMetadataEditor(viewModel = viewModel, type = MetadataEditorType.CHAPTERS)
        }

        AnimatedVisibility(visible = viewModel.isEditingSubtitles.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(60f)) {
            UnifiedMetadataEditor(viewModel = viewModel, type = MetadataEditorType.SUBTITLES, settings = viewModel.subtitleSettings.value, onSettingsChange = { viewModel.subtitleSettings.value = it })
        }

        if (viewModel.isLyricsWebViewVisible.value) {
            LyricsWebView(url = viewModel.lyricsSearchUrl.value, onDismiss = { viewModel.isLyricsWebViewVisible.value = false })
        }

        AnimatedVisibility(visible = viewModel.showSubtitleConsole.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(70f)) {
            MediaSelectionSheet(
                title = "SUBTITLES",
                items = viewModel.availableSubtitleTracks,
                selectedItem = viewModel.selectedSubtitleTrack.value,
                emptyIcon = Icons.Default.Subtitles,
                emptyTitle = "No subtitles detected",
                emptyMessage = "Embedded subtitle tracks will appear here.",
                onClose = { viewModel.showSubtitleConsole.value = false },
                onItemClick = { viewModel.switchSubtitle(it) },
                onOnlineSearch = { viewModel.fetchSubtitlesOnline(autoOpenSheet = true) },
                onPickFile = { viewModel.requestSubtitlePicker() },
                itemKey = { it.id },
                itemTitle = { it.language.uppercase() },
                itemSubtitle = { it.codec ?: "Unknown Codec" }
            )
        }

        AnimatedVisibility(visible = viewModel.showAudioTrackConsole.value, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(70f)) {
            MediaSelectionSheet(
                title = "AUDIO TRACKS",
                items = viewModel.availableAudioTracks,
                selectedItem = viewModel.selectedAudioTrack.value,
                emptyIcon = Icons.Default.Audiotrack,
                emptyTitle = "No audio tracks detected",
                emptyMessage = "Alternative audio streams will appear here.",
                onClose = { viewModel.showAudioTrackConsole.value = false },
                onItemClick = { viewModel.switchAudioTrack(it) },
                itemKey = { it.id },
                itemTitle = { it.language },
                itemSubtitle = { it.codec ?: "Unknown Codec" }
            )
        }
    }
}

@Composable
fun EmptyPlayerState(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(40.dp)) {
            Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.GraphicEq, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(32.dp))
            Text(text = "NEURAL STATION IDLE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(text = "The audio buffer is currently empty. Orchestrate your next session by selecting a track from the library.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Spacer(Modifier.height(48.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(0.8f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(12.dp))
                Text("RETURN TO HUB", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun SleepTimerFloater(viewModel: AppViewModel, onDismiss: () -> Unit) {
    pEQGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Sleep Timer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            val options = listOf(15, 30, 45, 60, 90)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { minutes ->
                    val isSelected = viewModel.sleepTimerMinutes.value == minutes
                    Surface(
                        onClick = { viewModel.setSleepTimer(minutes); onDismiss() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = "${minutes}m",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            if (viewModel.sleepTimerActive.value) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.stopSleepTimer(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    elevation = null
                ) {
                    Text("STOP TIMER", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackInfoBottomSheet(
    item: pEQMediaItem,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {

    val isVideo =
        item.type == MediaType.VIDEO

    val isAudio =
        item.type == MediaType.AUDIO

    ModalBottomSheet(
        onDismissRequest = onDismiss,

        containerColor =
            MaterialTheme.colorScheme.surface,

        dragHandle = {

            BottomSheetDefaults.DragHandle(
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
                        .copy(alpha = 0.4f)
            )
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 14.dp
                )
        ) {

            // =========================
            // HEADER
            // =========================

            Surface(
                modifier = Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(28.dp),

                color =
                    MaterialTheme.colorScheme
                        .surfaceContainerHighest
                        .copy(alpha = 0.55f),

                tonalElevation = 4.dp
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .primary
                                    .copy(alpha = 0.14f)
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        pEQImage(data = item.thumbnail, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }

                    Spacer(
                        Modifier.width(16.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        item.title
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {

                        EnhancedInfoRow(
                            icon = Icons.Default.MusicNote,
                            label = "Title",
                            value = it
                        )
                    }

                        Spacer(
                            Modifier.height(4.dp)
                        )

                          item.artist
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {

                                EnhancedInfoRow(
                                    icon = Icons.Default.Person,
                                    label = "Artist",
                                    value = it
                                )
                            }
                    }
                }
            }

            Spacer(
                Modifier.height(20.dp)
            )

            // =========================
            // BASIC INFO
            // =========================

            EnhancedInfoSection(
                title =
                    if (isVideo)
                        "VIDEO INFO"
                    else
                        "SONG INFO",

                icon = Icons.Default.Info
            ) {

                EnhancedInfoRow(
                    icon = Icons.Default.Description,
                    label = "File Name",
                    value = item.fileName
                )

////                item.title
////                    .takeIf {
////                        it.isNotBlank()
////                    }
////                    ?.let {
////
////                        EnhancedInfoRow(
////                            icon = Icons.Default.MusicNote,
////                            label = "Title",
////                            value = it
////                        )
////                    }
//
//                item.artist
//                    ?.takeIf {
//                        it.isNotBlank()
//                    }
//                    ?.let {
//
//                        EnhancedInfoRow(
//                            icon = Icons.Default.Person,
//                            label = "Artist",
//                            value = it
//                        )
//                    }

                if (isAudio) {

                    item.album
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {

                            EnhancedInfoRow(
                                icon =
                                    Icons.Default
                                        .Album,

                                label = "Album",

                                value = it
                            )
                        }

                    item.genre
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {

                            EnhancedInfoRow(
                                icon =
                                    Icons.Default
                                        .LibraryMusic,

                                label = "Genre",

                                value = it
                            )
                        }

                    item.composer
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {

                            EnhancedInfoRow(
                                icon =
                                    Icons.Default
                                        .EditNote,

                                label = "Composer",

                                value = it
                            )
                        }

                    item.year
                        ?.takeIf {
                            it > 0
                        }
                        ?.let {

                            EnhancedInfoRow(
                                icon =
                                    Icons.Default
                                        .CalendarMonth,

                                label = "Year",

                                value =
                                    it.toString()
                            )
                        }
                }

                EnhancedInfoRow(
                    icon = Icons.Default.Folder,

                    label = "Folder",

                    value =
                        item.path.substringBeforeLast(
                            "/",
                            "Internal Storage"
                        )
                )
            }

            Spacer(
                Modifier.height(18.dp)
            )

            // =========================
            // TECHNICAL
            // =========================

            EnhancedInfoSection(
                title = "TECHNICAL DETAILS",

                icon =
                    Icons.Default.Memory
            ) {

                EnhancedInfoRow(
                    icon = Icons.Default.Timer,

                    label = "Duration",

                    value =
                        formatTime(
                            item.durationMs
                        )
                )

                EnhancedInfoRow(
                    icon = Icons.Default.AudioFile,

                    label = "Format",

                    value = item.format
                )

                item.codec
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {

                        EnhancedInfoRow(
                            icon =
                                Icons.Default
                                    .SettingsInputHdmi,

                            label = "Codec",

                            value = it
                        )
                    }

                if (item.bitrate > 0) {

                    EnhancedInfoRow(
                        icon =
                            Icons.Default
                                .Speed,

                        label = "Bitrate",

                        value =
                            "${item.bitrate} kbps"
                    )
                }

                if (isAudio) {

                    if (item.sampleRate > 0) {

                        EnhancedInfoRow(
                            icon =
                                Icons.Default
                                    .Equalizer,

                            label =
                                "Sample Rate",

                            value =
                                "${item.sampleRate} Hz"
                        )
                    }

                    if (item.bitDepth > 0) {

                        EnhancedInfoRow(
                            icon =
                                Icons.Default
                                    .DataObject,

                            label =
                                "Bit Depth",

                            value =
                                "${item.bitDepth} bit"
                        )
                    }

                    if (item.channels > 0) {

                        EnhancedInfoRow(
                            icon =
                                Icons.Default
                                    .SurroundSound,

                            label =
                                "Channels",

                            value =
                                item.channels
                                    .toString()
                        )
                    }
                }

                if (isVideo) {

                    item.resolution
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {

                            EnhancedInfoRow(
                                icon =
                                    Icons.Default
                                        .AspectRatio,

                                label =
                                    "Resolution",

                                value = it
                            )
                        }

                    if (item.frameRate > 0f) {

                        EnhancedInfoRow(
                            icon =
                                Icons.Default
                                    .SlowMotionVideo,

                            label =
                                "Frame Rate",

                            value =
                                "${item.frameRate.toInt()} FPS"
                        )
                    }
                }

                EnhancedInfoRow(
                    icon = Icons.Default.Storage,

                    label = "File Size",

                    value = item.displaySize
                )
            }

            Spacer(
                Modifier.height(18.dp)
            )

            // =========================
            // LIBRARY
            // =========================

            EnhancedInfoSection(
                title = "LIBRARY",
                icon = Icons.Default.LibraryBooks
            ) {

                EnhancedInfoRow(
                    icon = Icons.Default.AccessTime,
                    label = "Added",
                    value = formatDate(item.dateAdded * 1000L)
                )

                EnhancedInfoRow(
                    icon = Icons.Default.Update,
                    label = "Modified",
                    value = formatDate(item.dateModified * 1000L)
                )

                if (item.lastPlayed > 0) {

                    EnhancedInfoRow(
                        icon = Icons.Default.History,
                        label = "Last Played",
                        value = formatDate(item.lastPlayed * 1000L)
                    )
                }

                if (item.playCount > 0) {

                    EnhancedInfoRow(
                        icon = Icons.Default.PlayArrow,
                        label = "Play Count",
                        value = item.playCount.toString()
                    )
                }
            }

            Spacer(
                Modifier.height(28.dp)
            )

            // =========================
            // ACTION BUTTON
            // =========================

            FilledTonalButton(
                onClick = {

                    viewModel
                        .clearPlaybackHistory()

                    onDismiss()
                },

                modifier = Modifier
                    .height(56.dp),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    ButtonDefaults
                        .filledTonalButtonColors(

                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .errorContainer
                        )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.DeleteSweep,

                    contentDescription = null
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Text(
                    text =
                        "CLEAR PLAYBACK HISTORY",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(22.dp)
            )
        }
    }
}

// =====================================================
// ENHANCED SECTION
// =====================================================

@Composable
private fun EnhancedInfoSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(24.dp),

        color =
            MaterialTheme.colorScheme
                .surfaceContainerHighest
                .copy(alpha = 0.42f),

        tonalElevation = 2.dp
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = icon,

                    contentDescription = null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.size(18.dp)
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Text(
                    text = title,

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Black,

                    letterSpacing = 1.4.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            Spacer(
                Modifier.height(18.dp)
            )

            content()
        }
    }
}

// =====================================================
// ENHANCED ROW
// =====================================================

@Composable
private fun EnhancedInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.10f)
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector = icon,

                contentDescription = null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,

                modifier =
                    Modifier.size(16.dp)
            )
        }

        Spacer(
            Modifier.width(14.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = label,

                fontSize = 11.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                        .copy(alpha = 0.72f)
            )

            Spacer(
                Modifier.height(2.dp)
            )

            Text(
                text = value,

                fontSize = 14.sp,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }
    }
}


@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}