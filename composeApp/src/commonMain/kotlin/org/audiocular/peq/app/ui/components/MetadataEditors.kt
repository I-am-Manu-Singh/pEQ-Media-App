@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)

package org.audiocular.peq.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.MediaChapter
import org.audiocular.peq.app.data.SubtitleSettings
import org.audiocular.peq.app.data.SyncedLine
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow

enum class MetadataEditorType {
    LYRICS,
    SUBTITLES,
    CHAPTERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedMetadataEditor(
    viewModel: AppViewModel,
    type: MetadataEditorType,
    settings: SubtitleSettings? = null,
    onSettingsChange: ((SubtitleSettings) -> Unit)? = null
) {

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    ModalBottomSheet(
        onDismissRequest = {

            when (type) {

                MetadataEditorType.LYRICS ->
                    viewModel.isEditingLyrics.value = false

                MetadataEditorType.SUBTITLES ->
                    viewModel.isEditingSubtitles.value = false

                MetadataEditorType.CHAPTERS ->
                    viewModel.isEditingChapters.value = false
            }
        },

        sheetState = sheetState,

        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },

        containerColor =
            MaterialTheme.colorScheme
                .surfaceContainerLow,

        tonalElevation = 0.dp
    ) {

        MetadataEditorContent(
            viewModel = viewModel,
            type = type,
            settings = settings,
            onSettingsChange = onSettingsChange,

            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun MetadataEditorContent(
    viewModel: AppViewModel,
    type: MetadataEditorType,
    settings: SubtitleSettings? = null,
    isCompact: Boolean = false,
    onSettingsChange: ((SubtitleSettings) -> Unit)? = null,
    modifier: Modifier = Modifier
) {

    // =========================
    // LOCAL EDIT STATE
    // =========================

    var lyrics by remember(type) {

        mutableStateOf(
            viewModel.syncedLyrics.toList()
        )
    }

    var chapters by remember(type) {

        mutableStateOf(
            viewModel.currentChapters.toList()
        )
    }

    var isPlainMode by remember {
        mutableStateOf(false)
    }

    var plainText by remember {
        mutableStateOf("")
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var isSearchVisible by remember {
        mutableStateOf(false)
    }

    val currentLyric = viewModel.syncedLyrics.getOrNull(viewModel.currentLyricsLine.value)

    val currentPosition =
        viewModel.currentPosition.value

    // =========================
    // PLAIN TEXT GENERATION
    // =========================

    LaunchedEffect(
        isPlainMode,
        lyrics,
        chapters
    ) {

        if (isPlainMode) {

            plainText =

                when (type) {

                    MetadataEditorType.CHAPTERS -> {

                        chapters.joinToString("\n") {

                            "${formatTime(it.startMs)} ${it.title}"
                        }
                    }

                    else -> {

                        lyrics.joinToString("\n") {

                            "[${formatTime(it.timestampMs)}] ${it.text}"
                        }
                    }
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {

        // =========================
        // TOP BAR
        // =========================

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),

            shape = RoundedCornerShape(
                if (isCompact) 20.dp
                else 24.dp
            ),

            color =
                MaterialTheme.colorScheme
                    .surfaceContainerLow
        ) {

            Column {

                Row(
                    modifier = Modifier.padding(
                        horizontal =
                            if (isCompact) 12.dp
                            else 14.dp,

                        vertical =
                            if (isCompact) 10.dp
                            else 12.dp
                    ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    // =========================
                    // TITLE
                    // =========================

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = when (type) {

                                MetadataEditorType.LYRICS ->
                                    "Lyrics"

                                MetadataEditorType.SUBTITLES ->
                                    "Subtitles"

                                MetadataEditorType.CHAPTERS ->
                                    "Chapters"
                            },

                            style =
                                MaterialTheme.typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Spacer(
                            Modifier.height(2.dp)
                        )

                        Text(
                            text = when (type) {

                                MetadataEditorType.CHAPTERS ->
                                    "${chapters.size} Chapters"

                                else ->
                                    "${lyrics.size} Lines"
                            },

                            fontSize =
                                if (isCompact) 10.sp
                                else 11.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    // =========================
                    // ACTIONS
                    // =========================

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                if (isCompact) 6.dp
                                else 8.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        // =========================
                        // FETCH
                        // =========================

                        if (
                            type ==
                            MetadataEditorType.LYRICS
                        ) {

                            SmallActionButton(
                                icon =
                                    Icons.Default.AutoAwesome
                            ) {

                                viewModel.fetchLyrics()
                            }

                            SmallActionButton(
                                icon =
                                    Icons.Default.Language
                            ) {

                                viewModel.searchLyricsInWebView()
                            }
                        }

                        if (
                            type ==
                            MetadataEditorType.SUBTITLES
                        ) {

                            SmallActionButton(
                                icon =
                                    Icons.Default.Download
                            ) {

                                viewModel.fetchSubtitlesOnline()
                            }
                        }

                        // =========================
                        // ADD
                        // =========================

                        if (!isPlainMode) {

                            SmallActionButton(
                                icon =
                                    Icons.Default.Add
                            ) {

                                when (type) {

                                    MetadataEditorType.CHAPTERS -> {

                                        chapters =
                                            (
                                                    chapters +

                                                            MediaChapter(
                                                                title = "New Chapter",

                                                                startMs =
                                                                    currentPosition,

                                                                endMs =
                                                                    currentPosition +
                                                                            10_000L
                                                            )
                                                    ).sortedBy {
                                                    it.startMs
                                                }

                                        viewModel.currentChapters.clear()

                                        viewModel.currentChapters.addAll(
                                            chapters
                                        )
                                    }

                                    else -> {

                                        lyrics =
                                            (
                                                    lyrics +

                                                            SyncedLine(
                                                                timestampMs =
                                                                    currentPosition,

                                                                text = ""
                                                            )
                                                    ).sortedBy {
                                                    it.timestampMs
                                                }

                                        viewModel.syncedLyrics.clear()
                                        viewModel.syncedLyrics.addAll(
                                            lyrics
                                        )
                                    }
                                }
                            }
                        }

                        // =========================
                        // CLEAR
                        // =========================

                        SmallActionButton(
                            icon =
                                Icons.Default.DeleteOutline,

                            destructive = true
                        ) {

                            when (type) {

                                MetadataEditorType.CHAPTERS -> {

                                    chapters = emptyList()

                                    viewModel.currentChapters.clear()
                                }

                                else -> {

                                    lyrics = emptyList()

                                    viewModel.syncedLyrics.clear()
                                }
                            }
                        }

                        // =========================
                        // SEARCH TOGGLE
                        // =========================

                        SmallActionButton(
                            icon =

                                if (isSearchVisible)
                                    Icons.Default.Close

                                else
                                    Icons.Default.Search
                        ) {

                            isSearchVisible =
                                !isSearchVisible

                            if (!isSearchVisible) {

                                searchQuery = ""
                            }
                        }

                        // =========================
                        // MODE
                        // =========================

                        SmallActionButton(
                            icon =

                                if (isPlainMode)
                                    Icons.Default.ViewList

                                else
                                    Icons.Default.Edit
                        ) {

                            isPlainMode =
                                !isPlainMode
                        }

                        // =========================
                        // SAVE
                        // =========================

                        FilledIconButton(
                            onClick = {

                                when (type) {

                                    MetadataEditorType.LYRICS -> {

                                        viewModel.saveLyrics(
                                            lyrics
                                        )

                                        viewModel.isEditingLyrics.value =
                                            false
                                    }

                                    MetadataEditorType.SUBTITLES -> {

                                        viewModel.saveSubtitles(
                                            lyrics
                                        )

                                        viewModel.isEditingSubtitles.value =
                                            false
                                    }

                                    MetadataEditorType.CHAPTERS -> {

                                        viewModel.saveChapters(
                                            chapters
                                        )

                                        viewModel.isEditingChapters.value =
                                            false
                                    }
                                }
                            },

                            modifier =
                                Modifier.size(

                                    if (isCompact)
                                        34.dp

                                    else
                                        38.dp
                                )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Check,

                                contentDescription = null,

                                modifier =
                                    Modifier.size(

                                        if (isCompact)
                                            16.dp

                                        else
                                            18.dp
                                    )
                            )
                        }
                    }
                }

                // =========================
                // SEARCH
                // =========================

                AnimatedVisibility(
                    visible =
                        !isPlainMode &&
                                isSearchVisible
                ) {

                    Column {

                        OutlinedTextField(
                            value = searchQuery,

                            onValueChange = {
                                searchQuery = it
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp
                                ),

                            singleLine = true,

                            shape =
                                RoundedCornerShape(16.dp),

                            leadingIcon = {

                                Icon(
                                    imageVector =
                                        Icons.Default.Search,

                                    contentDescription = null,

                                    modifier =
                                        Modifier.size(18.dp)
                                )
                            },

                            placeholder = {

                                Text(
                                    text =

                                        when (type) {

                                            MetadataEditorType.CHAPTERS ->
                                                "Search chapters..."

                                            else ->
                                                "Search lyrics..."
                                        }
                                )
                            },

                            textStyle =
                                TextStyle(
                                    fontSize =

                                        if (isCompact)
                                            12.sp

                                        else
                                            13.sp
                                )
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )
                    }
                }
            }
        }

        // =========================
        // CONTENT
        // =========================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {

            // =========================
            // PLAIN MODE
            // =========================

            if (isPlainMode) {

                PlainModeEditor(
                    modifier = Modifier.fillMaxSize(),

                    type = type,

                    text = plainText,

                    onTextChange = {
                        plainText = it
                    },

                    onConvert = {

                        when (type) {

                            MetadataEditorType.CHAPTERS -> {

                                chapters =
                                    parseChapters(
                                        plainText
                                    )

                                viewModel.currentChapters.clear()

                                viewModel.currentChapters.addAll(
                                    chapters
                                )
                            }

                            else -> {

                                lyrics =
                                    parsePlainLyrics(
                                        plainText
                                    )

                                viewModel.syncedLyrics.clear()

                                viewModel.syncedLyrics.addAll(
                                    lyrics
                                )
                            }
                        }

                        isPlainMode = false
                    }
                )
            }

            // =========================
            // VISUAL MODE
            // =========================

            else {

                when (type) {

                    MetadataEditorType.LYRICS,
                    MetadataEditorType.SUBTITLES -> {

                        val filteredLines =

                            lyrics.filter {

                                if (searchQuery.isBlank()) {

                                    true

                                } else {

                                    it.text.contains(
                                        searchQuery,
                                        ignoreCase = true
                                    )
                                }
                            }

                        LyricsEditorList(
                            modifier = Modifier.fillMaxSize(),

                            lines = filteredLines,

                            onLinesChange = {

                                lyrics = it

                                viewModel.syncedLyrics.clear()

                                viewModel.syncedLyrics.addAll(
                                    it
                                )
                            },

                            currentTime =
                                currentPosition
                        )
                    }

                    MetadataEditorType.CHAPTERS -> {

                        val filteredChapters =

                            chapters.filter {

                                if (searchQuery.isBlank()) {

                                    true

                                } else {

                                    it.title.contains(
                                        searchQuery,
                                        ignoreCase = true
                                    )
                                }
                            }

                        ChaptersEditorList(
                            modifier =
                                Modifier.fillMaxSize(),

                            chapters =
                                filteredChapters,

                            onChaptersChange = {

                                chapters = it

                                viewModel.currentChapters.clear()

                                viewModel.currentChapters.addAll(
                                    it
                                )
                            }
                        )
                    }
                }
            }
        }

        // =========================
        // SUBTITLE SETTINGS
        // =========================

        if (
            type ==
            MetadataEditorType.SUBTITLES &&

            settings != null &&

            onSettingsChange != null
        ) {

            Spacer(
                Modifier.height(

                    if (isCompact)
                        8.dp

                    else
                        12.dp
                )
            )

            SubtitleStylingBar(
                settings = settings,

                onSettingsChange =
                    onSettingsChange
            )
        }
    }
}

@Composable
private fun EditorToolbar(
    type: MetadataEditorType,
    isPlainMode: Boolean,
    onFetch: () -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit
) {

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        if (type == MetadataEditorType.LYRICS) {

            EditorChip(
                icon = Icons.Default.AutoAwesome,
                label = "Fetch",
                onClick = onFetch
            )

            EditorChip(
                icon = Icons.Default.Language,
                label = "Search",
                onClick = onSearch
            )

            if (!isPlainMode) {

                EditorChip(
                    icon = Icons.Default.Add,
                    label = "Add",
                    onClick = onAdd
                )
            }

            EditorChip(
                icon = Icons.Default.DeleteOutline,
                label = "Clear",
                isDestructive = true,
                onClick = onClear
            )
        }
    }
}

@Composable
fun EditorChip(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,

        shape = CircleShape,

        color =
            if (isDestructive)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),

            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun PlainModeEditor(
    modifier: Modifier = Modifier,
    type: MetadataEditorType,
    text: String,
    onTextChange: (String) -> Unit,
    onConvert: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        OutlinedTextField(
            value = text,

            onValueChange = onTextChange,

            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),

            shape = RoundedCornerShape(24.dp),

            placeholder = {

                Text(
                    text =
                        if (type == MetadataEditorType.CHAPTERS)
                            "Title|Start|End"
                        else
                            "Paste lyrics here"
                )
            }
        )

        Spacer(Modifier.height(14.dp))

        FilledTonalButton(
            onClick = onConvert,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Generate synced list")
        }
    }
}
@Composable
fun EditableSubtitleCard(
    line: SyncedLine,
    currentTime: Long,
    isSubtitle: Boolean,
    onDelete: () -> Unit,
    onUpdate: (SyncedLine) -> Unit,
    onSeek: () -> Unit
) {

    var text by remember(line.text) {
        mutableStateOf(line.text)
    }

    var timestamp by remember(line.timestampMs) {
        mutableStateOf(line.timestampMs.toString())
    }

    val isActive =
        kotlin.math.abs(
            currentTime - line.timestampMs
        ) < 1200L

    Surface(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(24.dp),

        color =

            if (isActive) {

                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.12f)

            } else {

                MaterialTheme.colorScheme.surfaceContainerHigh
            },

        border = BorderStroke(
            1.dp,

            if (isActive) {

                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.35f)

            } else {

                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.08f)
            }
        )
    ) {

        Column(
            modifier = Modifier.padding(6.dp)
        ) {

            // =========================
            // TOP ROW
            // =========================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                FilledTonalIconButton(
                    onClick = onSeek,

                    modifier = Modifier.size(38.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.PlayArrow,

                        contentDescription = null,

                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(
                    Modifier.width(10.dp)
                )

                OutlinedTextField(
                    value = timestamp,

                    onValueChange = {
                        timestamp = it
                    },

                    modifier = Modifier.width(120.dp),

                    singleLine = true,

                    shape =
                        RoundedCornerShape(16.dp),

                    textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),

                    label = {

                        Text(
                            "Time"
                        )
                    },

                    placeholder = {

                        Text(
                            "00:00"
                        )
                    }
                )

                Spacer(
                    Modifier.weight(1f)
                )

                FilledTonalIconButton(
                    onClick = {

                        onUpdate(
                            line.copy(
                                timestampMs =
                                    currentTime
                            )
                        )

                        timestamp =
                            currentTime.toString()
                    },

                    modifier = Modifier.size(38.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.MyLocation,

                        contentDescription = null,

                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                FilledTonalIconButton(
                    onClick = onDelete,

                    modifier = Modifier.size(38.dp),

                    colors =
                        IconButtonDefaults
                            .filledTonalIconButtonColors(

                                containerColor =
                                    MaterialTheme.colorScheme
                                        .errorContainer
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.DeleteOutline,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.error,

                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            // =========================
            // TEXT FIELD
            // =========================

            OutlinedTextField(
                value = text,

                onValueChange = {

                    text = it

                    val parsedTime =
                        parseTimeToMs(timestamp)

                    onUpdate(
                        line.copy(
                            timestampMs =
                                parsedTime,

                            text = it
                        )
                    )
                },

                modifier = Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp),

                textStyle =
                    TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    ),

                placeholder = {

                    Text(

                        if (isSubtitle) {

                            "Enter subtitle text..."

                        } else {

                            "Enter lyric line..."
                        }
                    )
                }
            )

            Spacer(
                Modifier.height(10.dp)
            )

            // =========================
            // FOOTER
            // =========================

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,

                    color =

                        if (isActive) {

                            MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.18f)

                        } else {

                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                ) {

                    Text(
                        text =
                            formatTime(
                                line.timestampMs
                            ),

                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        ),

                        fontSize = 11.sp,

                        fontWeight =
                            FontWeight.SemiBold,

                        color =

                            if (isActive) {

                                MaterialTheme.colorScheme.primary

                            } else {

                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            }
                    )
                }

                Spacer(
                    Modifier.weight(1f)
                )

                if (isActive) {

                    Text(
                        text = "LIVE",

                        color =
                            MaterialTheme.colorScheme.primary,

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
private fun parseTimeToMs(
    input: String
): Long {

    return try {

        val clean =
            input.replace("[", "")
                .replace("]", "")
                .trim()

        val parts =
            clean.split(":")

        if (parts.size < 2) {
            return 0L
        }

        val minutes =
            parts[0].toLong()

        val secParts =
            parts[1].split(".")

        val seconds =
            secParts[0].toLong()

        val millis =

            if (secParts.size > 1) {

                secParts[1]
                    .padEnd(3, '0')
                    .take(3)
                    .toLong()

            } else {
                0L
            }

        (minutes * 60_000L) +
                (seconds * 1000L) +
                millis

    } catch (_: Exception) {

        0L
    }
}
@Composable
fun EditableChapterCard(
    chapter: MediaChapter,
    currentTime: Long,
    onDelete: () -> Unit,
    onUpdate: (MediaChapter) -> Unit,
    onSeek: () -> Unit
) {

    var title by remember(chapter.title) {
        mutableStateOf(chapter.title)
    }

    var timestamp by remember(chapter.startMs) {
        mutableStateOf(
            formatTime(chapter.startMs)
        )
    }

    val isActive =
        currentTime >= chapter.startMs &&
                currentTime <
                (chapter.startMs + 15_000L)

    Surface(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(26.dp),

        color =

            if (isActive) {

                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.12f)

            } else {

                MaterialTheme.colorScheme.surfaceContainerHigh
            },

        border = BorderStroke(
            1.dp,

            if (isActive) {

                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.30f)

            } else {

                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.08f)
            }
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            // =========================
            // TOP ROW
            // =========================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier = Modifier.size(42.dp),

                    shape = CircleShape,

                    color =

                        if (isActive) {

                            MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.18f)

                        } else {

                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Bookmarks,

                            contentDescription = null,

                            tint =

                                if (isActive) {

                                    MaterialTheme.colorScheme.primary

                                } else {

                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                },

                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(
                    Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =

                            if (isActive) {

                                "Current Chapter"

                            } else {

                                "Chapter"
                            },

                        fontSize = 11.sp,

                        fontWeight = FontWeight.SemiBold,

                        color =

                            if (isActive) {

                                MaterialTheme.colorScheme.primary

                            } else {

                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                            }
                    )

                    Spacer(
                        Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            formatTime(
                                chapter.startMs
                            ),

                        fontSize = 13.sp,

                        fontWeight = FontWeight.Bold
                    )
                }

                FilledTonalIconButton(
                    onClick = onSeek,

                    modifier = Modifier.size(38.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.PlayArrow,

                        contentDescription = null,

                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                FilledTonalIconButton(
                    onClick = {

                        val updated =
                            chapter.copy(
                                startMs = currentTime
                            )

                        timestamp =
                            formatTime(currentTime)

                        onUpdate(updated)
                    },

                    modifier = Modifier.size(38.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.MyLocation,

                        contentDescription = null,

                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(
                    Modifier.width(8.dp)
                )

                FilledTonalIconButton(
                    onClick = onDelete,

                    modifier = Modifier.size(38.dp),

                    colors =
                        IconButtonDefaults
                            .filledTonalIconButtonColors(

                                containerColor =
                                    MaterialTheme.colorScheme
                                        .errorContainer
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.DeleteOutline,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.error,

                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(
                Modifier.height(14.dp)
            )

            // =========================
            // TITLE FIELD
            // =========================

            OutlinedTextField(
                value = title,

                onValueChange = {

                    title = it

                    onUpdate(
                        chapter.copy(
                            title = it,
                            startMs =
                                parseTimeToMs(
                                    timestamp
                                )
                        )
                    )
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                shape =
                    RoundedCornerShape(18.dp),

                textStyle =
                    TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Title,

                        contentDescription = null
                    )
                },

                placeholder = {

                    Text(
                        "Chapter title..."
                    )
                }
            )

            Spacer(
                Modifier.height(12.dp)
            )

            // =========================
            // TIMESTAMP FIELD
            // =========================

            OutlinedTextField(
                value = timestamp,

                onValueChange = {

                    timestamp = it

                    onUpdate(
                        chapter.copy(
                            title = title,
                            startMs =
                                parseTimeToMs(it)
                        )
                    )
                },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true,

                shape =
                    RoundedCornerShape(18.dp),

                textStyle =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Schedule,

                        contentDescription = null
                    )
                },

                placeholder = {

                    Text(
                        "00:00"
                    )
                },

                supportingText = {

                    Text(
                        "Format: mm:ss or mm:ss.ms"
                    )
                }
            )

            // =========================
            // ACTIVE FOOTER
            // =========================

            AnimatedVisibility(
                visible = isActive
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.primary
                            )
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text = "Playing now",

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
@Composable
private fun LyricsEditorList(
    modifier: Modifier = Modifier,

    lines: List<SyncedLine>,

    onLinesChange: (List<SyncedLine>) -> Unit,

    currentTime: Long
) {

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
        contentPadding = PaddingValues(
            bottom = 24.dp
        )
    ) {

        if (lines.isEmpty()) {

            item {

                EmptyMetadataState(
                    title = "No lyrics yet",
                    subtitle = "Fetch, paste, or add synced lines."
                )
            }
        }

        itemsIndexed(

            items = lines,

            key = { index, line ->

                "${line.id}_$index"

            }

        ) { index, line ->

            SyncedLineItem(
                line = line,

                isActive =
                    currentTime >= line.timestampMs &&
                            (
                                    index == lines.lastIndex ||
                                            currentTime < lines[index + 1].timestampMs
                                    ),

                onUpdate = { updated ->

                    onLinesChange(
                        lines.toMutableList().apply {
                            set(index, updated)
                        }
                    )
                },

                onDelete = {

                    onLinesChange(
                        lines.toMutableList().apply {
                            removeAt(index)
                        }
                    )
                },

                onSync = {

                    onLinesChange(
                        lines.toMutableList().apply {

                            set(
                                index,
                                line.copy(
                                    timestampMs = currentTime
                                )
                            )
                        }
                    )
                },

                onMove = { from, to ->

                    val mutable =
                        lines.toMutableList()

                    val item =
                        mutable.removeAt(from)

                    mutable.add(to, item)

                    onLinesChange(mutable)
                },

                index = index,
                totalCount = lines.size
            )
        }
    }
}

@Composable
private fun SyncedLineItem(
    line: SyncedLine,
    isActive: Boolean,
    onUpdate: (SyncedLine) -> Unit,
    onDelete: () -> Unit,
    onSync: () -> Unit,
    onMove: (Int, Int) -> Unit,
    index: Int,
    totalCount: Int
) {

    var accumulatedDrag by remember {
        mutableFloatStateOf(0f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(22.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                if (isActive)
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surface
        ),

        border = BorderStroke(
            width =
                if (isActive) 1.dp
                else 0.dp,

            color =
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.22f)
        ),

        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {

            Row(
                verticalAlignment = Alignment.Top
            ) {

                Surface(
                    shape = RoundedCornerShape(10.dp),

                    color =
                        MaterialTheme.colorScheme
                            .primaryContainer
                ) {

                    Text(
                        text = formatTime(line.timestampMs),

                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),

                        style = MaterialTheme.typography.labelSmall,

                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.width(12.dp))

                BasicTextField(
                    value = line.text,

                    onValueChange = {

                        onUpdate(
                            line.copy(text = it)
                        )
                    },

                    modifier = Modifier.weight(1f),

                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(

                            color =
                                MaterialTheme.colorScheme
                                    .onSurface,

                            fontSize = 15.sp,

                            lineHeight = 22.sp
                        ),

                    cursorBrush =
                        SolidColor(
                            MaterialTheme.colorScheme.primary
                        ),

                    decorationBox = { innerTextField ->

                        Box {

                            if (line.text.isBlank()) {

                                Text(
                                    text = "Lyrics line",

                                    style =
                                        MaterialTheme.typography
                                            .bodyLarge,

                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                            .copy(alpha = 0.6f)
                                )
                            }

                            innerTextField()
                        }
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                SmallActionButton(
                    icon = Icons.Default.Sync,
                    onClick = onSync
                )

                Spacer(Modifier.width(8.dp))

                SmallActionButton(
                    icon = Icons.Default.DeleteOutline,
                    destructive = true,
                    onClick = onDelete
                )

                Spacer(Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            MaterialTheme.colorScheme
                                .surfaceContainerHigh
                        )

                        .pointerInput(Unit) {

                            detectVerticalDragGestures(

                                onDragEnd = {
                                    accumulatedDrag = 0f
                                },

                                onVerticalDrag = {
                                        change,
                                        dragAmount ->

                                    change.consume()

                                    accumulatedDrag += dragAmount

                                    val threshold = 80f

                                    if (
                                        accumulatedDrag > threshold &&
                                        index < totalCount - 1
                                    ) {

                                        onMove(
                                            index,
                                            index + 1
                                        )

                                        accumulatedDrag = 0f
                                    }

                                    else if (
                                        accumulatedDrag < -threshold &&
                                        index > 0
                                    ) {

                                        onMove(
                                            index,
                                            index - 1
                                        )

                                        accumulatedDrag = 0f
                                    }
                                }
                            )
                        }

                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        )
                ) {

                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}
@Composable
private fun ChaptersEditorList(
    modifier: Modifier = Modifier,
    chapters: List<MediaChapter>,
    onChaptersChange: (List<MediaChapter>) -> Unit
) {
    LazyColumn(
        modifier = modifier,

        verticalArrangement =
            Arrangement.spacedBy(14.dp),

        contentPadding =
            PaddingValues(
                start = 2.dp,
                top = 0.dp,
                end = 2.dp,
                bottom = 120.dp
            )
    ){

        itemsIndexed(
            items = chapters,

            key = { index, chapter ->
                "${chapter.id}_$index"
            }
        ) { index, chapter ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()

                    .border(
                        width = 1.dp,

                        color =
                            MaterialTheme.colorScheme
                                .outlineVariant
                                .copy(alpha = 0.32f),

                        shape =
                            RoundedCornerShape(22.dp)
                    ),

                shape =
                    RoundedCornerShape(22.dp),

                colors =
                    CardDefaults.elevatedCardColors(

                        containerColor =
                            MaterialTheme.colorScheme
                                .surfaceContainer
                    ),

                elevation =
                    CardDefaults.elevatedCardElevation(
                        defaultElevation = 2.dp
                    )
            ){

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    // =========================
                    // TOP STRIP
                    // =========================

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        // =========================
                        // INDEX BADGE
                        // =========================

                        Surface(
                            modifier =
                                Modifier.size(34.dp),

                            shape = CircleShape,

                            color =
                                MaterialTheme.colorScheme
                                    .primary
                                    .copy(alpha = 0.12f)
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text =
                                        "${index + 1}",

                                    fontSize = 11.sp,

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        MaterialTheme.colorScheme
                                            .primary
                                )
                            }
                        }

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        // =========================
                        // TITLE
                        // =========================

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            BasicTextField(
                                value = chapter.title,

                                onValueChange = { updated ->

                                    onChaptersChange(
                                        chapters.toMutableList()
                                            .apply {

                                                set(
                                                    index,

                                                    chapter.copy(
                                                        title = updated
                                                    )
                                                )
                                            }
                                    )
                                },

                                singleLine = true,

                                textStyle =
                                    LocalTextStyle.current.copy(

                                        fontSize = 14.sp,

                                        fontWeight =
                                            FontWeight.SemiBold,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurface
                                    ),

                                decorationBox = { inner ->

                                    Box {

                                        if (
                                            chapter.title.isBlank()
                                        ) {

                                            Text(
                                                text =
                                                    "Chapter title",

                                                fontSize = 14.sp,

                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant
                                            )
                                        }

                                        inner()
                                    }
                                }
                            )

                            Spacer(
                                Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    formatTime(
                                        chapter.startMs
                                    ),

                                fontSize = 11.sp,

                                fontWeight =
                                    FontWeight.Medium,

                                color =
                                    MaterialTheme.colorScheme
                                        .primary
                            )
                        }

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        // =========================
                        // PLAY
                        // =========================

                        FilledTonalIconButton(
                            onClick = {

                                // hook seek here
                            },

                            modifier =
                                Modifier.size(34.dp)
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.PlayArrow,

                                contentDescription = null,

                                modifier =
                                    Modifier.size(18.dp)
                            )
                        }
                    }

                    // =========================
                    // DIVIDER
                    // =========================

                    HorizontalDivider(
                        thickness = 1.dp,

                        color =
                            MaterialTheme.colorScheme
                                .outlineVariant
                                .copy(alpha = 0.18f)
                    )

                    // =========================
                    // BOTTOM SECTION
                    // =========================

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 10.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        // =========================
                        // RANGE CHIP
                        // =========================

                        Surface(
                            shape =
                                RoundedCornerShape(14.dp),

                            color =
                                MaterialTheme.colorScheme
                                    .surfaceContainerHighest
                        ) {

                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                ),

                                verticalAlignment =
                                    Alignment.CenterVertically,

                                horizontalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Schedule,

                                    contentDescription = null,

                                    modifier =
                                        Modifier.size(13.dp),

                                    tint =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )

                                Text(
                                    text =
                                        "${formatTime(chapter.startMs)}  •  ${
                                            formatTime(chapter.endMs)
                                        }",

                                    fontSize = 11.sp,

                                    color =
                                        MaterialTheme.colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }

                        Spacer(
                            Modifier.weight(1f)
                        )

                        // =========================
                        // DELETE
                        // =========================

                        IconButton(
                            onClick = {

                                onChaptersChange(
                                    chapters.toMutableList()
                                        .apply {
                                            removeAt(index)
                                        }
                                )
                            },

                            modifier =
                                Modifier.size(32.dp)
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.DeleteOutline,

                                contentDescription = null,

                                tint =
                                    MaterialTheme.colorScheme
                                        .error
                                        .copy(alpha = 0.85f),

                                modifier =
                                    Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmallActionButton(
    icon: ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit
) {

    FilledTonalIconButton(
        onClick = onClick,

        modifier = Modifier.size(36.dp),

        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor =
                if (destructive)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyMetadataState(
    title: String,
    subtitle: String
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,

            modifier = Modifier.size(42.dp),

            tint =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
                    .copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = title,

            style = MaterialTheme.typography.titleMedium,

            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = subtitle,

            style = MaterialTheme.typography.bodyMedium,

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
fun SubtitleStylingBar(
    settings: SubtitleSettings,
    onSettingsChange: (SubtitleSettings) -> Unit
) {

    Card(
        shape = RoundedCornerShape(22.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainer
        ),

        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                SmallActionButton(
                    icon = Icons.Default.Remove
                ) {

                    onSettingsChange(
                        settings.copy(
                            fontSize =
                                (
                                        settings.fontSize - 1
                                        ).coerceAtLeast(8f)
                        )
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "${settings.fontSize.toInt()}sp",

                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.width(8.dp))

                SmallActionButton(
                    icon = Icons.Default.Add
                ) {

                    onSettingsChange(
                        settings.copy(
                            fontSize =
                                (
                                        settings.fontSize + 1
                                        ).coerceAtMost(32f)
                        )
                    )
                }
            }

            SmallActionButton(
                icon = Icons.Default.FormatBold
            ) {

                onSettingsChange(
                    settings.copy(
                        isBold = !settings.isBold
                    )
                )
            }
        }
    }
}


private fun parseChapters(
    text: String
): List<MediaChapter> {

    return text
        .lines()
        .filter {
            it.contains("|")
        }

        .map { line ->

            val parts =
                line.split("|")

            MediaChapter(
                title =
                    parts.getOrNull(0)
                        ?: "Untitled",

                startMs =
                    parts.getOrNull(1)
                        ?.toLongOrNull()
                        ?: 0L,

                endMs =
                    parts.getOrNull(2)
                        ?.toLongOrNull()
                        ?: 0L
            )
        }
}

private fun parsePlainLyrics(
    text: String
): List<SyncedLine> {

    return text
        .lines()
        .filter {
            it.isNotBlank()
        }

        .mapIndexed { index, value ->

            SyncedLine(
                timestampMs = index * 2000L,
                text = value
            )
        }
}
@Composable
fun LyricsWebView(
    url: String,
    onDismiss: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = onDismiss,

        modifier =
            Modifier.fillMaxHeight(0.90f),

        containerColor =
            MaterialTheme.colorScheme
                .surfaceContainerLow,

        dragHandle = {
            BottomSheetDefaults.DragHandle()
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            // =========================
            // TOP BAR
            // =========================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 4.dp,
                        bottom = 14.dp
                    ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    modifier =
                        Modifier.size(38.dp),

                    shape = CircleShape,

                    color =
                        MaterialTheme.colorScheme
                            .primary
                            .copy(alpha = 0.12f)
                ) {

                    Box(
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Language,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme
                                    .primary,

                            modifier =
                                Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(
                    Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Lyrics Search",

                        fontSize = 16.sp,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Spacer(
                        Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "External lyrics source",

                        fontSize = 11.sp,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = onDismiss,

                    modifier =
                        Modifier.size(36.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Close,

                        contentDescription = null,

                        modifier =
                            Modifier.size(18.dp)
                    )
                }
            }

            // =========================
            // CONTENT CARD
            // =========================

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth(),

                shape =
                    RoundedCornerShape(28.dp),

                colors =
                    CardDefaults.elevatedCardColors(

                        containerColor =
                            MaterialTheme.colorScheme
                                .surfaceContainer
                    ),

                elevation =
                    CardDefaults.elevatedCardElevation(
                        defaultElevation = 2.dp
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {

                    // =========================
                    // URL BOX
                    // =========================

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp),

                        color =
                            MaterialTheme.colorScheme
                                .surfaceContainerHighest
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 12.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Link,

                                contentDescription = null,

                                tint =
                                    MaterialTheme.colorScheme
                                        .primary,

                                modifier =
                                    Modifier.size(16.dp)
                            )

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Text(
                                text = url,

                                modifier =
                                    Modifier.weight(1f),

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis,

                                fontSize = 12.sp,

                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    // =========================
                    // INFO
                    // =========================

                    Row(
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Surface(
                            modifier =
                                Modifier.size(34.dp),

                            shape =
                                RoundedCornerShape(12.dp),

                            color =
                                MaterialTheme.colorScheme
                                    .primary
                                    .copy(alpha = 0.10f)
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ContentPaste,

                                    contentDescription = null,

                                    tint =
                                        MaterialTheme.colorScheme
                                            .primary,

                                    modifier =
                                        Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column {

                            Text(
                                text =
                                    "Paste into plain mode",

                                fontSize = 13.sp,

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Text(
                                text =
                                    "Copy synced or plain lyrics from the website and paste them into the editor.",

                                fontSize = 11.sp,

                                lineHeight = 16.sp,

                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    // =========================
                    // ACTION BUTTON
                    // =========================

                    FilledTonalButton(
                        onClick = {

                            // open url externally
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(18.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.OpenInBrowser,

                            contentDescription = null,

                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Text(
                            text = "Open Website"
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(24.dp)
            )
        }
    }
}