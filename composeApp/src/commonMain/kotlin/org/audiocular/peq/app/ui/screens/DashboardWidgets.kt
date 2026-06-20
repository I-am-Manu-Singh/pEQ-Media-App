package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.theme.*
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.ai.FormatInfo
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralMoodMakerItem(viewModel: AppViewModel) {
    var selectedMood by remember { mutableStateOf("Energetic") }
    var selectedType by remember { mutableStateOf("Music") }

    val moods = listOf(
        "Energetic",
        "Chill",
        "Focused",
        "Melancholic"
    )

    val types = listOf(
        "Music",
        "Videos",
        "All"
    )

    SectionHeader("NEURAL SYNC ENGINE")

    Spacer(Modifier.height(6.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = pEQDesign.GlassColor,
        border = pEQDesign.GlassBorder,
        tonalElevation = pEQDesign.GlassTonalElevation
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp)
                    ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    }
                }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "MOOD GENERATOR",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    types.forEach { type ->
                        val selected = selectedType == type

                        Surface(
                            onClick = { selectedType = type },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Text(
                                text = type.uppercase(),
                                modifier = Modifier.padding(
                                    horizontal = 2.dp,
                                    vertical = 0.dp
                                ),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moods) { mood ->
                    val selected = selectedMood == mood

                    Surface(
                        onClick = { selectedMood = mood },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Text(
                            text = mood,
                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 2.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AssistChip(
                    onClick = {
                        viewModel.generateMoodPlaylist(
                            selectedMood,
                            selectedType
                        )
                    },
                    label = {
                        Text(
                            text = "Generate",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ArchiveCommandCard(viewModel: AppViewModel) {
    val scanProgress by viewModel.scanProgress
    val scanStatus by viewModel.scanStatus
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = pEQDesign.GlassColor,
        border = pEQDesign.GlassBorder,
        tonalElevation = pEQDesign.GlassTonalElevation
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp)
                    ) {
                    Icon(Icons.Default.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ACTIVE ARCHIVES", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text("${viewModel.scanFolders.size} paths registered", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                    }
                }
                Row {
                    FilledTonalIconButton(onClick = { viewModel.refreshLibrary() }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = { viewModel.showFolderManager.value = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (scanProgress in 0.01f..0.99f) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(progress = { scanProgress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape))
                Text(scanStatus, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (viewModel.scanFolders.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.scanFolders) { folder ->
                        AssistChip(
                            onClick = { viewModel.openFolder(folder); viewModel.dashboardSubTab.value = 1 },
                            label = { Text(folder.substringAfterLast("/"), fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun StreamingIntakeCard(viewModel: AppViewModel) {
    var streamUrl by remember { mutableStateOf("") }
    val isFetchingFormats by viewModel.isFetchingFormats
    val availableFormats = viewModel.availableFormats
    var isDownloadMode by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf<org.audiocular.peq.app.ai.FormatInfo?>(null) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    var streamStatus by remember { mutableStateOf("READY FOR LINK INPUT") }

    // Auto-fill shared URL if present
    LaunchedEffect(viewModel.sharedUrl.value) {
        if (viewModel.sharedUrl.value.isNotEmpty()) {
            streamUrl = viewModel.sharedUrl.value
            viewModel.loadLinkFormats(streamUrl)
        }
    }

    // Auto-select first format when list changes
    LaunchedEffect(availableFormats.size) {
        if (availableFormats.isNotEmpty()) {
            selectedFormat = availableFormats.firstOrNull()
            streamStatus = "RESOLVED ${availableFormats.size} FORMATS"
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = pEQDesign.GlassColor,
        border = pEQDesign.GlassBorder
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(modifier = Modifier.padding(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        "STREAM INTAKE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "DYNAMIC QUALITY & FORMAT PROBING",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // URL input field
            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                label = { Text("Stream or Download Link") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (streamUrl.isNotEmpty()) {
                        IconButton(onClick = { streamUrl = ""; availableFormats.clear(); selectedFormat = null; streamStatus = "CLEARED" }) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // Action Row: YT/SC Shortcuts & Probe Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SuggestionChip(
                        onClick = { streamUrl = "https://www.youtube.com/watch?v=" },
                        label = { Text("YOUTUBE", fontSize = 9.sp) }
                    )
                    SuggestionChip(
                        onClick = { streamUrl = "https://" },
                        label = { Text("STREAM", fontSize = 9.sp) }
                    )
                }

                Button(
                    onClick = {
                        if (streamUrl.isNotBlank()) {
                            viewModel.loadLinkFormats(streamUrl)
                        } else {
                            streamStatus = "PLEASE ENTER LINK FIRST"
                        }
                    },
                    enabled = !isFetchingFormats && streamUrl.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isFetchingFormats) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ANALYZE STREAM", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Expandable format options and Mode Switch when formats are available
            if (availableFormats.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                // Mode switch chip Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isDownloadMode,
                        onClick = { isDownloadMode = false },
                        label = { Text("STREAM PLAYBACK", fontSize = 9.sp, fontWeight = FontWeight.Black) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp)) }
                    )

                    FilterChip(
                        selected = isDownloadMode,
                        onClick = { isDownloadMode = true },
                        label = { Text("LOCAL DOWNLOAD", fontSize = 9.sp, fontWeight = FontWeight.Black) },
                        leadingIcon = { Icon(Icons.Default.Download, null, modifier = Modifier.size(12.dp)) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Dynamic quality drop-down selector
                Text("CHOOSE QUALITY / CODEC:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Spacer(Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { showFormatDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val current = selectedFormat
                                if (current != null) {
                                    Text(
                                        text = "${current.ext.uppercase()} - ${current.note}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val sizeStr = if (current.sizeBytes > 0) " (${(current.sizeBytes / 1024f / 1024f).formatSize()} MB)" else ""
                                    Text(
                                        text = "Audio: ${current.acodec ?: "unknown"} | Video: ${current.vcodec ?: "none"}$sizeStr",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                } else {
                                    Text("Select quality format", fontSize = 12.sp)
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    if (showFormatDropdown) {
                        ProbedFormatsDialog(
                            formats = availableFormats,
                            onSelected = { format: FormatInfo ->
                                selectedFormat = format
                                showFormatDropdown = false
                            },
                            onDismiss = { showFormatDropdown = false }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Trigger Button
                Button(
                    onClick = {
                        val format = selectedFormat
                        if (isDownloadMode) {
                            viewModel.startMediaDownload(
                                url = streamUrl,
                                audioOnly = format?.isVideo == false,
                                videoEnabled = format?.isVideo == true,
                                highQuality = true,
                                autoQueue = true,
                                resolution = format?.note ?: "",
                                audioQuality = format?.note ?: "",
                                formatId = format?.formatId
                            )
                        } else {
                            viewModel.addStreamUrl(
                                url = streamUrl,
                                type = if (format?.isVideo == true) MediaType.VIDEO else MediaType.AUDIO
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = if (isDownloadMode) Icons.Default.Download else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isDownloadMode) "START LOCAL INTAKE" else "PLAY LIVE STREAM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Status indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (availableFormats.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        streamStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

// Float formatting extension helper
private fun Float.formatSize(): String {
    val rounded = (this * 100).toInt() / 100f
    return rounded.toString()
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), letterSpacing = 1.sp)
        }
    }
}

@Composable
fun pEQTabs(
    tabs: List<String>,
    selectedTab: Int,
    viewModel: AppViewModel,
    onTabSelected: (Int) -> Unit
) {
    val mediaLibrary by viewModel.mediaLibrary.collectAsState()
    val playlists = viewModel.customPlaylists
    val queue = viewModel.playbackQueue
    val downloads = viewModel.downloadQueue

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(items = tabs) { index, title ->
            val selected = selectedTab == index
            val icon = when (index) {
                0 -> Icons.Default.Dashboard
                1 -> Icons.Default.LibraryMusic
                2 -> Icons.AutoMirrored.Filled.PlaylistPlay
                3 -> Icons.AutoMirrored.Filled.QueueMusic
                4 -> Icons.Default.Download
                5 -> Icons.Default.Settings
                else -> Icons.Default.Radio
            }

            val bubbleText = when (index) {
                1 -> mediaLibrary.size.toString()
                2 -> playlists.size.toString()
                3 -> queue.size.toString()
                4 -> downloads.size.toString()
                else -> null
            }

            Surface(
                onClick = { onTabSelected(index) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                tonalElevation = if (selected) 6.dp else 1.dp
            ) {
                Box(
                    modifier = Modifier.height(44.dp).padding(horizontal = if (selected) 16.dp else 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = title, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
                        AnimatedVisibility(
                            visible = selected,
                            enter = fadeIn(tween(180)) + expandHorizontally(),
                            exit = fadeOut(tween(120)) + shrinkHorizontally()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Spacer(Modifier.width(6.dp))
                                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.2.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Visible)
                            }
                        }

                        // Display Bubble Badge count if positive
                        if (bubbleText != null && bubbleText != "0") {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = bubbleText,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (selected) Color.Black else MaterialTheme.colorScheme.primary
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbedFormatsDialog(
    formats:List<FormatInfo>,
    onSelected:(FormatInfo)->Unit,
    onDismiss:()->Unit
){
    var selectedTab by remember { mutableStateOf(0)}

    val audioFormats = remember(formats){
        formats.filter { !it.isVideo }
    }

    val videoFormats = remember(formats){
        formats.filter { it.isVideo }
    }

    val activeFormats =
        if(selectedTab==0) audioFormats
        else videoFormats

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ){

        Surface(
            modifier = Modifier
                .fillMaxWidth(.96f)
                .widthIn(max = 620.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        ){

            Column(
                modifier = Modifier.padding(24.dp)
            ){

                //-------------------------------------
                // TITLE
                //-------------------------------------

                Text(
                    text = "Probed Formats",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Choose the quality you want to stream or download.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                //-------------------------------------
                // Tabs
                //-------------------------------------

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ){

                    SegmentedButton(
                        selected = selectedTab==0,
                        onClick = { selectedTab=0 },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 0,
                            count = 2
                        ),
                        icon = {
                            Icon(
                                Icons.Default.Audiotrack,
                                null,
                                Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                "Audio (${audioFormats.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )

                    SegmentedButton(
                        selected = selectedTab==1,
                        onClick = { selectedTab=1 },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = 1,
                            count = 2
                        ),
                        icon = {
                            Icon(
                                Icons.Default.VideoLibrary,
                                null,
                                Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                "Video (${videoFormats.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                if(activeFormats.isEmpty()){

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ){

                        Text(
                            "No formats available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }else{

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ){

                        items(
                            activeFormats,
                            key = { it.formatId ?: "${it.ext}${it.note}" }
                        ){

                            FormatCard(
                                format = it,
                                onClick = {
                                    onSelected(it)
                                }
                            )
                        }

                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ){

                    FilledTonalButton(
                        onClick = onDismiss
                    ){
                        Text("Close")
                    }
                }
            }
        }
    }
}
@Composable
private fun FormatCard(
    format: FormatInfo,
    onClick:()->Unit
){

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ){

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){

            Icon(
                imageVector = if(format.isVideo)
                    Icons.Default.VideoLibrary
                else
                    Icons.Default.Audiotrack,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ){

                Text(
                    text = buildString{

                        append(format.ext.uppercase())

                        if(format.note.isNotBlank()){
                            append(" • ")
                            append(format.note)
                        }

                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                val codec = buildString{

                    if(format.acodec!="none" && !format.acodec.isNullOrBlank()){
                        append("Audio: ${format.acodec}")
                    }

                    if(format.vcodec!="none" && !format.vcodec.isNullOrBlank()){

                        if(isNotEmpty()) append(" • ")

                        append("Video: ${format.vcodec}")
                    }

                }

                if(codec.isNotBlank()){

                    Text(
                        codec,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(6.dp))
                if(format.sizeBytes>0){

                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {

                        val mb =
                            format.sizeBytes /1024f/1024f

                        Text("${mb.formatSize()} MB")
                    }
                )
            }
            }
        }
    }
}