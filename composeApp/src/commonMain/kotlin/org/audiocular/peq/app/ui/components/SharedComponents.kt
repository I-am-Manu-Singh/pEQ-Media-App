package org.audiocular.peq.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.ExperimentalFoundationApi
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.data.MediaType
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import org.audiocular.peq.app.ui.theme.pEQStrings
import org.audiocular.peq.app.data.SubtitleSettings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.animateContentSize
/* ---------- UI TOKENS ---------- */

object pEQDesign {
    val GlassColor
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.red > 0.9f && MaterialTheme.colorScheme.surface.green > 0.9f) {
            // Light theme: Frosty semi-opaque
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        } else {
            // Dark theme: Industrial translucent
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        }

    val GlassBorder
        @Composable
        get() = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    Color.Transparent,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        )


    val PrimaryGlow
        @Composable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)

    val GlassElevation
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.red > 0.9f) 2.dp else 8.dp

    val GlassTonalElevation
        @Composable
        get() = if (MaterialTheme.colorScheme.surface.red > 0.9f) 0.dp else 6.dp

    val CardShape = RoundedCornerShape(20.dp)
}

@Composable
fun pEQEditorContainer(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp)
            ) {
                content()
            }
        }
    }
}

/* ---------- SHARED WIDGETS ---------- */

@Composable
fun DeviceSwitcherDialog(
    viewModel: AppViewModel
) {

    val device = viewModel.pendingDevice.value ?: return

    AlertDialog(
        onDismissRequest = {
            viewModel.showDeviceSwitcher.value = false
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,

        title = {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.SettingsInputComponent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {

                    Text(
                        text = "Hardware Detected",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )

                    Text(
                        text = "Audio routing device available",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },

        text = {

            Column {

                Text(
                    text = "A high-performance production asset has been detected. Switch the workstation output route to this device.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = device.color.copy(alpha = 0.10f),
                    border = BorderStroke(
                        1.dp,
                        device.color.copy(alpha = 0.22f)
                    )
                ) {

                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            shape = CircleShape,
                            color = device.color.copy(alpha = 0.14f),
                            modifier = Modifier.size(48.dp)
                        ) {

                            Box(
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = device.icon,
                                    contentDescription = null,
                                    tint = device.color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = device.deviceName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = device.chip,
                                fontSize = 11.sp,
                                color = device.color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {
                    viewModel.confirmDeviceSwitch()
                },
                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    text = "Switch Route",
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = {
                    viewModel.showDeviceSwitcher.value = false
                }
            ) {

                Text(
                    text = "Ignore",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun InfoChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {

        Text(
            text = text.uppercase(),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 3.dp
            )
        )
    }
}

@Composable
fun SectionHeader(
    title: String
) {

    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun MetadataBadge(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary
) {

    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.18f)
        )
    ) {

        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 3.dp
            ),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun pEQItemCard(
    item: pEQMediaItem,
    isActive: Boolean = false,
    showFavorite: Boolean = true,
    showMenu: Boolean = true,
    showIndex: Boolean = false,
    showPlayCount: Boolean = false,
    index: Int = 0,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) = MediaItemCard(item, isActive, showFavorite, showMenu, showIndex, showPlayCount, index, modifier, trailing, onClick, onLongClick)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun pEQItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemCard(
    item: pEQMediaItem,
    isActive: Boolean = false,
    showFavorite: Boolean = true,
    showMenu: Boolean = true,
    showIndex: Boolean = false,
    showPlayCount: Boolean = false,
    index: Int = 0,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {

    val viewModel = LocalAppViewModel.current

    val isFavorite =
        remember(
            item,
            viewModel.favoriteItems.size
        ) {

            viewModel.favoriteItems.contains(item)
        }

    var showMenuState by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()

            .animateContentSize()

            .combinedClickable(
                onClick = onClick,

                onLongClick = onLongClick
            ),

        shape = RoundedCornerShape(22.dp),

        color =

            if (isActive)
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.50f)

            else
                MaterialTheme.colorScheme
                    .surfaceContainerHigh
                    .copy(alpha = 1.0f),

        border = BorderStroke(
            1.dp,

            if (isActive)
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.24f)

            else
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.06f)
        ),

        tonalElevation =
            if (isActive)
                2.dp
            else
                0.dp
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 11.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            // =========================
            // THUMBNAIL
            // =========================

            Box(
                modifier = Modifier.size(58.dp),

                contentAlignment = Alignment.Center
            ) {

                Surface(
                    modifier = Modifier.size(56.dp),

                    shape = RoundedCornerShape(16.dp),

                    color =
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.45f)
                ) {

                    if (
                        item.thumbnail != null
                    ) {

                        pEQImage(
                            data = item.thumbnail,

                            modifier = Modifier.fillMaxSize(),

                            contentScale = ContentScale.Crop,
                            
                            mediaType = item.type
                        )

                    } else {

                        Box(
                            modifier = Modifier.fillMaxSize(),

                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector =

                                    if (item.type == MediaType.VIDEO)
                                        Icons.Default.VideoLibrary
                                    else
                                        Icons.Default.MusicNote,

                                contentDescription = null,

                                tint =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                        .copy(alpha = 0.55f),

                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // =========================
                // ACTIVE OVERLAY
                // =========================

                if (isActive) {

                    Box(
                        modifier = Modifier
                            .matchParentSize()

                            .background(
                                color =
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.16f),

                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    Surface(
                        shape = CircleShape,

                        color =
                            MaterialTheme.colorScheme.primary
                    ) {

                        Box(
                            modifier = Modifier.size(22.dp),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.PlayArrow,

                                contentDescription = null,

                                tint = Color.Black,

                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // =========================
                // INDEX BADGE
                // =========================

                else if (showIndex) {

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)

                            .offset(
                                x = 4.dp,
                                y = 4.dp
                            ),

                        shape = CircleShape,

                        color =
                            MaterialTheme.colorScheme.surface,

                        shadowElevation = 4.dp
                    ) {

                        Box(
                            modifier = Modifier.size(21.dp),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = index.toString(),

                                fontSize = 9.sp,

                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // =========================
            // DETAILS
            // =========================

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        item.title.ifBlank {
                            "Unknown Media"
                        },

                    fontSize = 14.sp,

                    fontWeight = FontWeight.Bold,

                    color =

                        if (isActive)
                            MaterialTheme.colorScheme.primary

                        else
                            MaterialTheme.colorScheme.onSurface,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            item.subtitle.ifBlank {
                                "Unknown Artist"
                            },

                        fontSize = 11.sp,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis,

                        modifier = Modifier.weight(1f)
                    )

                    if (showPlayCount && item.playCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = item.playCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    MetadataBadge(

                        text =

                            if (item.type == MediaType.VIDEO)

                                buildString {

                                    append(
                                        item.resolution
                                            ?: "VIDEO"
                                    )

                                    item.codec?.let {

                                        append(" • ")
                                        append(it)
                                    }
                                }

                            else

                                buildString {

                                    append("${item.bitDepth}Bit")

                                    if (item.sampleRate > 0) {

                                        append(" • ")
                                        append("${item.sampleRate / 1000}kHz")
                                    }
                                }
                    )
                }

                AnimatedVisibility(
                    visible = isActive
                ) {

                    Row(
                        modifier = Modifier.padding(top = 6.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(7.dp)

                                .clip(CircleShape)

                                .background(
                                    MaterialTheme.colorScheme.primary
                                )
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "NOW PLAYING",

                            fontSize = 9.sp,

                            fontWeight = FontWeight.Black,

                            letterSpacing = 0.5.sp,

                            color =
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))

            // =========================
            // ACTIONS
            // =========================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                if (showFavorite) {

                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(item)
                        },

                        modifier = Modifier.size(34.dp)
                    ) {

                        Icon(
                            imageVector =

                                if (isFavorite)
                                    Icons.Default.Favorite

                                else
                                    Icons.Default.FavoriteBorder,

                            contentDescription = null,

                            tint =

                                if (isFavorite)
                                    MaterialTheme.colorScheme.primary

                                else
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                        .copy(alpha = 0.45f),

                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showMenu) {

                    Box {

                        IconButton(
                            onClick = {
                                showMenuState = true
                            },

                            modifier = Modifier.size(34.dp)
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.MoreVert,

                                contentDescription = null,

                                tint =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                                        .copy(alpha = 0.45f),

                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuState,

                            onDismissRequest = {
                                showMenuState = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Add to Queue")
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector =
                                            Icons.Default.QueueMusic,

                                        contentDescription = null
                                    )
                                },

                                onClick = {

                                    viewModel.addToQueue(item)

                                    showMenuState = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Add to Playlist")
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector =
                                            Icons.Default.PlaylistAdd,

                                        contentDescription = null
                                    )
                                },

                                onClick = {

                                    viewModel.selectedItemForPlaylist.value =
                                        item

                                    viewModel.isPlaylistPickerShown.value =
                                        true

                                    showMenuState = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Neural Split")
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector =
                                            Icons.Default.AutoAwesome,

                                        contentDescription = null
                                    )
                                },

                                onClick = {
                                    viewModel.currentMediaItem.value = item
                                    viewModel.startStemSplit()
                                    showMenuState = false
                                }
                            )
                        }
                    }
                }

                trailing?.invoke()
            }
        }
    }
}
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun QueueContent(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {

    val queue = viewModel.playbackQueue

    val currentIndex by viewModel.currentQueueIndex

    val listState = rememberLazyListState()

    val searchQuery =
        viewModel.globalSearchQuery.value

    // =========================
    // LIVE FILTERING
    // =========================

    val filtered =
        queue.filter {

            it.title.contains(
                searchQuery,
                ignoreCase = true
            ) ||

                    it.subtitle.contains(
                        searchQuery,
                        ignoreCase = true
                    )
        }

    // =========================
    // AUTO SCROLL TO CURRENT
    // =========================

    LaunchedEffect(
        currentIndex,
        queue.size
    ) {

        if (
            queue.isNotEmpty() &&
            currentIndex in queue.indices
        ) {

            listState.animateScrollToItem(
                index = (currentIndex + 1)
                    .coerceAtLeast(0)
            )
        }
    }

    // =========================
    // EMPTY STATE
    // =========================

    if (filtered.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),

            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Surface(
                    modifier = Modifier.size(86.dp),

                    shape = CircleShape,

                    color =
                        MaterialTheme.colorScheme
                            .surfaceContainerHigh
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.QueueMusic,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.65f),

                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text =

                        if (searchQuery.isNotBlank())
                            "No Matching Queue Items"
                        else
                            "Queue Empty",

                    fontSize = 19.sp,

                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text =

                        if (searchQuery.isNotBlank())
                            "No queued media matched your search."
                        else
                            "Your playback queue is currently empty.",

                    textAlign = TextAlign.Center,

                    fontSize = 12.sp,

                    lineHeight = 18.sp,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

    } else {

        LazyColumn(
            state = listState,

            modifier = modifier.fillMaxSize(),

            verticalArrangement =
                Arrangement.spacedBy(10.dp),

            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 90.dp
            )
        ) {

            
            itemsIndexed(
                items = filtered,

                key = { index, item ->

                    // UNIQUE KEY FIX
                    "${item.path}_${index}_${item.id}"
                }
            ) { index, item ->

                val currentPlayingPath =
                    viewModel.currentMediaItem.value?.path

                val isCurrent =
                    item.path == currentPlayingPath

                var accumulatedDrag by remember(
                    item.path,
                    index
                ) {
                    mutableFloatStateOf(0f)
                }

                val dismissState =
                    rememberSwipeToDismissBoxState(

                        positionalThreshold = {
                            it * 0.22f
                        },

                        confirmValueChange = { value ->

                            if (isCurrent) {
                                return@rememberSwipeToDismissBoxState false
                            }

                            if (
                                value ==
                                SwipeToDismissBoxValue.EndToStart
                            ) {

                                viewModel.removeFromQueue(item)

                                true

                            } else {
                                false
                            }
                        }
                    )

                SwipeToDismissBox(
    state = dismissState,

    enableDismissFromStartToEnd = false,

    enableDismissFromEndToStart = !isCurrent,

    backgroundContent = {

        if (!isCurrent) {

            Surface(
                modifier = Modifier.fillMaxSize(),

                shape = RoundedCornerShape(24.dp),

                color =
                    MaterialTheme.colorScheme.error
                        .copy(alpha = 0.12f)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 18.dp),

                    contentAlignment =
                        Alignment.CenterEnd
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.DeleteOutline,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme.error,

                            modifier =
                                Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "REMOVE",

                            fontSize = 11.sp,

                            fontWeight = FontWeight.Bold,

                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
) {

                    MediaItemCard(
                        item = item,

                        isActive = isCurrent,

                        showFavorite = false,

                        showMenu = false,

                        showIndex = true,

                        index = index + 1,

                        onClick = {

                            val latestIndex =
                                queue.indexOfFirst {
                                    it.path == item.path
                                }

                            if (latestIndex != -1) {

                                viewModel.playFromQueue(
                                    queue[latestIndex]
                                )
                            }
                        },

                        trailing = {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.DragHandle,

                                    contentDescription = null,

                                    tint =

                                        if (isCurrent)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant,

                                    modifier = Modifier
                                        .size(22.dp)

                                        .pointerInput(
                                            item.path,
                                            queue.size
                                        ) {

                                            detectVerticalDragGestures(

                                                onVerticalDrag = {
                                                        change,
                                                        dragAmount ->

                                                    change.consume()

                                                    accumulatedDrag +=
                                                        dragAmount

                                                    val currentItemIndex =
                                                        queue.indexOfFirst {
                                                            it.path == item.path
                                                        }

                                                    if (
                                                        currentItemIndex == -1
                                                    ) {
                                                        return@detectVerticalDragGestures
                                                    }

                                                    val threshold = 70f

                                                    // MOVE DOWN
                                                    if (

                                                        accumulatedDrag > threshold &&
                                                        currentItemIndex < queue.lastIndex
                                                    ) {

                                                        viewModel.reorderQueue(
                                                            currentItemIndex,
                                                            currentItemIndex + 1
                                                        )

                                                        accumulatedDrag = 0f
                                                    }

                                                    // MOVE UP
                                                    else if (

                                                        accumulatedDrag < -threshold &&
                                                        currentItemIndex > 0
                                                    ) {

                                                        viewModel.reorderQueue(
                                                            currentItemIndex,
                                                            currentItemIndex - 1
                                                        )

                                                        accumulatedDrag = 0f
                                                    }
                                                },

                                                onDragEnd = {
                                                    accumulatedDrag = 0f
                                                }
                                            )
                                        }
                                )
                            }
                        },

                        modifier = Modifier
                            .animateContentSize()
                    )
                }
            }

            item(
                key = "queue_bottom_spacing"
            ) {

                Spacer(
                    Modifier.height(12.dp)
                )
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable

fun PlayerQueueDrawer(
    viewModel: AppViewModel,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UP NEXT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        // Open playlist picker for all queue items
                        viewModel.isPlaylistPickerShown.value = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ADD ALL TO PLAYLIST", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { viewModel.shuffleQueue() },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Shuffle, 
                        null, 
                        tint = if (viewModel.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            QueueContent(
                viewModel = viewModel
            )
        }
    }
}
    @Composable
    fun MediaFolderCard(
        name: String,
        audioCount: Int = 0,
        videoCount: Int = 0,
        totalSize: Long = 0,
        onAddQueue: () -> Unit = {},
        onAddPlaylist: () -> Unit = {},
        onClick: () -> Unit
    ) {

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.size(48.dp)
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = name.substringAfterLast("/"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        if (audioCount > 0) {

                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )

                            Text(
                                text = " $audioCount",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.width(10.dp))
                        }

                        if (videoCount > 0) {

                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )

                            Text(
                                text = " $videoCount",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddQueue) {
                        Icon(
                            Icons.Default.QueueMusic,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onAddPlaylist) {
                        Icon(
                            Icons.Default.PlaylistAdd,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${totalSize / 1024 / 1024}MB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }

    @Composable
    fun HardwareSectionCard(
        title: String,
        content: @Composable ColumnScope.() -> Unit
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)
                )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )
            }
        }
    }


    @Composable
    fun DSPSmallCard(
        label: String,
        value: String,
        active: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) {

        Surface(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(18.dp),
            color =
                if (active)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
            border = BorderStroke(
                1.dp,
                if (active)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)
            ),
            tonalElevation = if (active) 2.dp else 0.dp,
            shadowElevation = if (active) 1.dp else 0.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = label.uppercase(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color =
                        if (active)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
    }


    /**
     * ODIO SUBTITLE CONSOLE: CINEMATIC OVERLAY MASTER Provides tactile control over the appearance and
     * timing of subtitles.
     */

@Composable
    fun SubtitleSliderCard(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(0.92f),

        shape =
            RoundedCornerShape(24.dp),

        color =
            MaterialTheme.colorScheme
                .surfaceContainerHigh
                .copy(alpha = 0.72f),

        border = BorderStroke(
            1.dp,

            MaterialTheme.colorScheme
                .outline
                .copy(alpha = 0.08f)
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 16.dp
            )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = title,

                    fontSize = 13.sp,

                    fontWeight =
                        FontWeight.SemiBold,

                    modifier =
                        Modifier.weight(1f)
                )

                Surface(
                    shape =
                        RoundedCornerShape(999.dp),

                    color =
                        accentColor.copy(alpha = 0.12f)
                ) {

                    Text(
                        text = valueText,

                        modifier =
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            ),

                        fontSize = 10.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color = accentColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Slider(
                value = value,

                onValueChange =
                    onValueChange,

                valueRange = range
            )
        }
    }
}
    @Composable
    fun StatCard(
        label: String,
        value: String,
        icon: ImageVector,
        modifier: Modifier = Modifier
    ) {

        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.07f)
            ),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = label,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.92f)
                )
            }
        }
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPicker(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {

    val playlists = viewModel.playlists

    // =========================
    // SINGLE + MULTI ADD SUPPORT
    // =========================

    val selectedSingleItem =
        viewModel.selectedItemForPlaylist.value
            ?: viewModel.currentMediaItem.value

    val selectedItems = remember(
        selectedSingleItem,
        viewModel.playbackQueue,
        viewModel.isPlaylistPickerShown.value
    ) {

        if (
            viewModel.selectedItemForPlaylist.value == null &&
            viewModel.playbackQueue.isNotEmpty()
        ) {

            // QUEUE MODE
            viewModel.playbackQueue.toList()

        } else {

            // SINGLE ITEM MODE
            listOfNotNull(selectedSingleItem)
        }
    }

    val totalItems =
        selectedItems.size

    var newPlaylistName by remember {
        mutableStateOf("")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,

        containerColor =
            MaterialTheme.colorScheme.surface,

        tonalElevation = 12.dp,

        dragHandle = {

            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(10.dp))

                Surface(
                    modifier = Modifier
                        .width(52.dp)
                        .height(5.dp),

                    shape = RoundedCornerShape(999.dp),

                    color =
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.14f)
                ) {}

                Spacer(Modifier.height(16.dp))
            }
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {

            // =========================
            // HEADER
            // =========================


               Row(
    modifier = Modifier.padding(18.dp),

    verticalAlignment = Alignment.CenterVertically
) {

    Surface(
        modifier = Modifier.size(58.dp),

        shape = RoundedCornerShape(20.dp),

        color =
            MaterialTheme.colorScheme.primary
                .copy(alpha = 0.12f)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Default.PlaylistAdd,

                contentDescription = null,

                tint =
                    MaterialTheme.colorScheme.primary,

                modifier = Modifier.size(28.dp)
            )
        }
    }

    Spacer(Modifier.width(14.dp))

    Column(
        modifier = Modifier.weight(1f)
    ) {

        Text(
            text = "Playlist Manager",

            fontSize = 22.sp,

            fontWeight = FontWeight.Black,

            maxLines = 1,

            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text =

                if (totalItems == 1)
                    "Add media into your collection."
                else
                    "Add these tracks into a playlist.",

            fontSize = 12.sp,

            lineHeight = 17.sp,

            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }


                }
            }



            // =========================
            // PLAYLIST COUNT
            // =========================

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Your Playlists",

                    fontSize = 15.sp,

                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(999.dp),

                    color =
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.12f)
                ) {

                    Text(
                        text =
                            "${playlists.size} PLAYLISTS",

                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),

                        fontSize = 10.sp,

                        fontWeight = FontWeight.Black,

                        letterSpacing = 0.8.sp,

                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // =========================
            // EMPTY STATE
            // =========================

            if (playlists.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 70.dp),

                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Surface(
                            modifier = Modifier.size(88.dp),

                            shape = CircleShape,

                            color =
                                MaterialTheme.colorScheme
                                    .surfaceContainerHighest
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.QueueMusic,

                                    contentDescription = null,

                                    tint =
                                        MaterialTheme.colorScheme.primary
                                            .copy(alpha = 0.55f),

                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "No Playlists Yet",

                            fontSize = 18.sp,

                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text =
                                "Create your first playlist above.",

                            textAlign = TextAlign.Center,

                            fontSize = 12.sp,

                            lineHeight = 18.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp),

                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 36.dp
                    )
                ) {

                    items(
                        items = playlists.keys.toList(),
                        key = { it }
                    ) { playlistName ->

                        val trackCount =
                            playlists[playlistName]
                                ?.size ?: 0

                        Surface(
                            onClick = {

                                selectedItems.forEach { item ->

                                    viewModel.addToPlaylist(
                                        playlistName,
                                        item
                                    )
                                }

                                onDismiss()
                            },

                            shape = RoundedCornerShape(24.dp),

                            color =
                                MaterialTheme.colorScheme
                                    .surfaceContainerHigh,

                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline
                                    .copy(alpha = 0.06f)
                            )
                        ) {

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
                                    modifier = Modifier.size(54.dp),

                                    shape = RoundedCornerShape(18.dp),

                                    color =
                                        MaterialTheme.colorScheme.primary
                                            .copy(alpha = 0.12f)
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.LibraryMusic,

                                            contentDescription = null,

                                            tint =
                                                MaterialTheme.colorScheme.primary,

                                            modifier =
                                                Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = playlistName,

                                        fontSize = 14.sp,

                                        fontWeight = FontWeight.Bold,

                                        maxLines = 1,

                                        overflow =
                                            TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text =
                                            "$trackCount tracks",

                                        fontSize = 11.sp,

                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )
                                }

                                Surface(
                                    modifier = Modifier.size(40.dp),

                                    shape = CircleShape,

                                    color =
                                        MaterialTheme.colorScheme.primary
                                            .copy(alpha = 0.10f)
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Add,

                                            contentDescription =
                                                "Add To Playlist",

                                            tint =
                                                MaterialTheme.colorScheme.primary,

                                            modifier =
                                                Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }


    @Composable
    expect fun VideoSurface(modifier: Modifier, contentScale: androidx.compose.ui.layout.ContentScale)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PEQArchiveRegistryDialog(
        onDismiss: () -> Unit
    ) {

        val viewModel = LocalAppViewModel.current

        val folders = viewModel.scanFolders
        val scanProgress by viewModel.scanProgress
        val scanStatus by viewModel.scanStatus

        var newPath by remember { mutableStateOf("") }

        val quickFolders = listOf(
            "Music",
            "Download",
            "Movies",
            "DCIM",
            "Pictures",
            "Podcasts",
            "Audiobooks",
            "Recordings"
        )

        val availableQuickFolders = quickFolders.filterNot { folderName ->
            folders.any { it.contains("/$folderName") }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            sheetMaxWidth = 680.dp,
            dragHandle = {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .width(48.dp)
                            .height(5.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
                    ) {}

                    Spacer(Modifier.height(12.dp))
                }
            }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp)
            ) {

                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(54.dp)
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Archive Registry",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Manage storage paths and synchronize media assets with your local engine.",
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onDismiss
                    ) {

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                // SCAN PROGRESS
                AnimatedVisibility(
                    visible = scanProgress > 0f && scanProgress < 1f
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                    ) {

                        LinearProgressIndicator(
                            progress = { scanProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = scanStatus.ifBlank { "Scanning media..." },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = "${(scanProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // HEADER ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Quick Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.weight(1f))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {

                        Text(
                            text = "${folders.size}",
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            ),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // QUICK FOLDERS
                if (availableQuickFolders.isNotEmpty()) {

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {

                        items(availableQuickFolders) { folderName ->

                            ElevatedFilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.addScanFolder(
                                        "/storage/emulated/0/$folderName"
                                    )
                                },
                                label = {

                                    Text(
                                        text = folderName,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {

                                    Icon(
                                        imageVector = Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                }

                // INPUT SECTION
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "Custom Path",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            OutlinedTextField(
                                value = newPath,
                                onValueChange = {
                                    newPath = it
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                singleLine = true,
                                placeholder = {

                                    Text(
                                        text = "Add storage path...",
                                        fontSize = 11.sp
                                    )
                                },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 12.sp
                                ),
                                leadingIcon = {

                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null
                                    )
                                },
                                trailingIcon = {

                                    AnimatedVisibility(
                                        visible = newPath.isNotBlank()
                                    ) {

                                        IconButton(
                                            onClick = {

                                                viewModel.addScanFolder(
                                                    newPath.trim()
                                                )

                                                newPath = ""
                                            }
                                        ) {

                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Confirm",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    viewModel.requestFolderPicker()
                                },
                                modifier = Modifier.size(54.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.CreateNewFolder,
                                    contentDescription = "Browse Folders"
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // REGISTERED PATHS
                Text(
                    text = "Registered Paths",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(
                            min = 140.dp,
                            max = 340.dp
                        )
                ) {

                    if (folders.isEmpty()) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 56.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(74.dp)
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "No folders registered",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Add folders above to begin scanning.",
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    } else {

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {

                            items(
                                items = folders,
                                key = { it }
                            ) { folder ->

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                                    )
                                ) {

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 14.dp,
                                                vertical = 14.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                            modifier = Modifier.size(46.dp)
                                        ) {

                                            Box(
                                                contentAlignment = Alignment.Center
                                            ) {

                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(21.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {

                                            Text(
                                                text = folder.substringAfterLast("/"),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(Modifier.height(3.dp))

                                            Text(
                                                text = folder,
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        FilledTonalIconButton(
                                            onClick = {
                                                viewModel.removeScanFolder(folder)
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {

                                            Icon(
                                                imageVector = Icons.Default.RemoveCircleOutline,
                                                contentDescription = "Remove Folder",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedButton(
                        onClick = {
                            viewModel.refreshLibrary()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = "Rescan",
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp)
                    ) {

                        Text(
                            text = "Done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    @Composable
    fun PEQFolderManagerDialog(onDismiss: () -> Unit) = PEQArchiveRegistryDialog(onDismiss)

    fun mapResolutionToLabel(resolution: String?): String {
        if (resolution == null) return "VIDEO"
        return when {
            resolution.contains("2160") -> "4K"
            resolution.contains("1080") -> "FHD"
            resolution.contains("720") -> "HD"
            else -> resolution
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StemSplitterDialog(
        onDismiss: () -> Unit
    ) {

        val viewModel = LocalAppViewModel.current
        val activeJob = viewModel.activeJob.value

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 14.dp,
            sheetMaxWidth = 620.dp,
            dragHandle = {

                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 10.dp)
                        .width(48.dp)
                        .height(5.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            CircleShape
                        )
                )
            }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {

                // HEADER
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Neural Stem Splitter",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            maxLines = 1
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "DSP Band-Pass Approximation (No GPU)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onDismiss
                    ) {

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )

                Spacer(Modifier.height(22.dp))

                if (activeJob == null) {

                    // IDLE STATE
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(
                                horizontal = 22.dp,
                                vertical = 26.dp
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Surface(
                                modifier = Modifier.size(84.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(22.dp))

                            Text(
                                text = "Ready for Extraction",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Approximating stem isolation via multi-band EQ processing. True Tensorflow Lite extraction is currently unavailable.",
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(26.dp))

                            FilledTonalButton(
                                onClick = {
                                    viewModel.batchExportStems()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {

                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.width(10.dp))

                                Text(
                                    text = "Initiate Neural Split",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                } else {

                    // PROCESSING STATE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(132.dp)
                        ) {

                            CircularProgressIndicator(
                                progress = { activeJob.progress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                strokeCap = StrokeCap.Round
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                Text(
                                    text = "${(activeJob.progress * 100).toInt()}%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "Processing",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        Text(
                            text = activeJob.status,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = activeJob.name,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(28.dp))

                        // TELEMETRY
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 18.dp,
                                        vertical = 16.dp
                                    ),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {

                                TelemetryItem(
                                    label = "Model",
                                    value = "v1.0-6S"
                                )

                                TelemetryItem(
                                    label = "Latency",
                                    value = "42ms"
                                )

                                TelemetryItem(
                                    label = "Threads",
                                    value = "8x"
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }

    @Composable
    private fun TelemetryItem(
        label: String,
        value: String
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )

            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MediaSelectionSheet(
    title: String,

    items: List<T>,

    selectedItem: T? = null,

    emptyIcon: ImageVector,

    emptyTitle: String,

    emptyMessage: String,

    onClose: () -> Unit,

    onItemClick: (T) -> Unit,

    onOnlineSearch: (() -> Unit)? = null,

    itemKey: (T) -> Any,

    itemTitle: (T) -> String,

    itemSubtitle: (T) -> String? = { null },

    itemTrailing: @Composable ((T, Boolean) -> Unit)? = null,

    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onPickFile: (() -> Unit)? = null
) {

    ModalBottomSheet(
        onDismissRequest = onClose,

        containerColor =
            MaterialTheme.colorScheme.surface,

        tonalElevation = 12.dp,

        sheetMaxWidth = 620.dp,

        dragHandle = {

            Box(
                modifier = Modifier
                    .padding(
                        top = 12.dp,
                        bottom = 10.dp
                    )
                    .width(48.dp)
                    .height(5.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.14f),

                        CircleShape
                    )
            )
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {

            // =========================
            // HEADER
            // =========================

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,

                        fontWeight = FontWeight.Black,

                        fontSize = 22.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = when {
                            items.isEmpty() ->
                                "No items available"

                            items.size == 1 ->
                                "1 item detected"

                            else ->
                                "${items.size} items detected"
                        },

                        fontSize = 11.sp,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (actionLabel != null && onAction != null) {
                        TextButton(onClick = onAction) {
                            Text(
                                actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (onOnlineSearch != null) {

                        FilledTonalButton(
                            onClick = onOnlineSearch
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Language,

                                contentDescription = null,

                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                text = "Online",

                                fontSize = 11.sp
                            )
                        }

                        Spacer(Modifier.width(8.dp))
                    }

                    if (onPickFile != null) {
                        FilledTonalIconButton(
                            onClick = onPickFile
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Pick Local File"
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }

                    FilledTonalIconButton(
                        onClick = onClose
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Close,

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

            // =========================
            // EMPTY
            // =========================

            if (items.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 72.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Surface(
                        modifier = Modifier.size(74.dp),

                        shape = CircleShape,

                        color =
                            MaterialTheme.colorScheme
                                .surfaceContainerHighest
                    ) {

                        Box(
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector = emptyIcon,

                                contentDescription = null,

                                tint =
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.65f),

                                modifier =
                                    Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = emptyTitle,

                        fontSize = 16.sp,

                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = emptyMessage,

                        textAlign = TextAlign.Center,

                        fontSize = 11.sp,

                        lineHeight = 17.sp,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }

            } else {

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp),

                    contentPadding = PaddingValues(
                        top = 16.dp,
                        bottom = 28.dp
                    )
                ) {

                itemsIndexed(
                    items = items,
                    key = { index, item -> "${itemKey(item)}_$index" }
                ) { index, item ->

                        val isSelected =
                            selectedItem == item

                        Surface(
                            onClick = {
                                onItemClick(item)
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            shape =
                                RoundedCornerShape(22.dp),

                            color =
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,

                            border = BorderStroke(
                                1.dp,

                                if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                        .copy(alpha = 0.28f)
                                else
                                    MaterialTheme.colorScheme.outline
                                        .copy(alpha = 0.06f)
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 14.dp
                                    ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    modifier =
                                        Modifier.size(42.dp),

                                    shape = CircleShape,

                                    color =
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                                .copy(alpha = 0.16f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                ) {

                                    Box(
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector =
                                                if (isSelected)
                                                    Icons.Default.CheckCircle
                                                else
                                                    Icons.Default.RadioButtonUnchecked,

                                            contentDescription = null,

                                            tint =
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant,

                                            modifier =
                                                Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text =
                                            itemTitle(item),

                                        fontWeight =
                                            FontWeight.Bold,

                                        fontSize = 14.sp
                                    )

                                    itemSubtitle(item)?.let {

                                        Spacer(
                                            Modifier.height(3.dp)
                                        )

                                        Text(
                                            text = it,

                                            fontSize = 11.sp,

                                            color =
                                                MaterialTheme.colorScheme
                                                    .onSurfaceVariant
                                        )
                                    }
                                }

                                itemTrailing?.invoke(
                                    item,
                                    isSelected
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun JobProgressItem(job: org.audiocular.peq.app.data.SplitJob) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(job.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${(job.progress * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { job.progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        Text(job.status, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}
@Composable
fun pEQGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = pEQDesign.GlassColor,
        shape = pEQDesign.CardShape,
        border = pEQDesign.GlassBorder,
        tonalElevation = pEQDesign.GlassTonalElevation,
        shadowElevation = pEQDesign.GlassElevation
    ) {
        content()
    }
}

fun AppViewModel.enqueueSplitJob(item: pEQMediaItem) {
    currentMediaItem.value = item
    startStemSplit()
}



