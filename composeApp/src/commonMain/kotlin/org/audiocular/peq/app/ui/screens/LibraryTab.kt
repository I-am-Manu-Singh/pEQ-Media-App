package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.MediaType
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedMediaExplorerTab(viewModel: AppViewModel) {
    val allItems by viewModel.mediaLibrary.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val currentPath = viewModel.currentFolderPath.value
    val mediaFilter by viewModel.libraryMediaTypeFilter
    val viewMode by viewModel.libraryViewMode
    
    val selectedItems = viewModel.selectedItems
    val isMultiSelectMode = selectedItems.isNotEmpty()

    var activeArtist by remember { mutableStateOf<String?>(null) }
    var activeAlbum by remember { mutableStateOf<String?>(null) }
    var viewExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    val sourceItems = if (viewModel.globalSearchQuery.value.isNotEmpty()) searchResults else allItems

    val baseArchive = remember(allItems) {
        if (allItems.isEmpty()) ""
        else {
            val paths = allItems.map { it.path }
            var prefix = paths.first().substringBeforeLast("/")
            for (i in 1 until paths.size) {
                while (!paths[i].startsWith(prefix)) {
                    prefix = prefix.substringBeforeLast("/", "")
                    if (prefix.isEmpty()) break
                }
            }
            prefix
        }
    }

    val effectivePath = currentPath.ifEmpty { baseArchive }

    val filteredItems = remember(sourceItems, effectivePath, mediaFilter, activeArtist, activeAlbum, viewMode, viewModel.sortBy.value) {
        sourceItems.filter { item ->
            val typeMatch = mediaFilter == null || item.type == mediaFilter
            val pathMatch = when (viewMode) {
                AppViewModel.LibraryViewMode.FILES -> item.path.substringBeforeLast("/").equals(effectivePath, ignoreCase = true)
                else -> true
            }
            val metadataMatch = when (viewMode) {
                AppViewModel.LibraryViewMode.ARTISTS -> activeArtist == null || (item.artist ?: "Unknown Artist").equals(activeArtist, ignoreCase = true)
                AppViewModel.LibraryViewMode.ALBUMS -> activeAlbum == null || (item.album ?: "Unknown Album").equals(activeAlbum, ignoreCase = true)
                else -> true
            }
            typeMatch && pathMatch && metadataMatch
        }.sortedWith { a, b ->
            when (viewModel.sortBy.value) {
                "SIZE" -> b.sizeBytes.compareTo(a.sizeBytes)
                "DATE" -> b.dateModified.compareTo(a.dateModified)
                else -> a.title.compareTo(b.title, ignoreCase = true)
            }
        }
    }

    val currentFolders = remember(sourceItems, effectivePath, mediaFilter, viewMode) {
        if (viewMode != AppViewModel.LibraryViewMode.FILES) emptyList()
        else {
            sourceItems.filter { mediaFilter == null || it.type == mediaFilter }
                .map { it.path }
                .filter { path ->
                    val dir = path.substringBeforeLast("/")
                    dir.startsWith(effectivePath, ignoreCase = true) && dir.length > effectivePath.length
                }
                .map { path ->
                    val relative = path.substring(effectivePath.length).trimStart('/')
                    relative.substringBefore("/")
                }
                .filter { it.isNotEmpty() && !it.contains(".") }
                .distinct().sorted()
        }
    }

    val showGrid = (viewMode == AppViewModel.LibraryViewMode.ARTISTS && activeArtist == null) || (viewMode == AppViewModel.LibraryViewMode.ALBUMS && activeAlbum == null)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        // 🚀 NAVIGATION & FILTER HUB
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilledTonalIconButton(
    onClick = {
        viewModel.currentFolderPath.value = ""
        activeArtist = null
        activeAlbum = null

        viewModel.libraryViewMode.value =
            AppViewModel.LibraryViewMode.LIBRARY
    },

    modifier = Modifier.size(38.dp),

    colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = MaterialTheme.colorScheme.primary
    )
) {

    Icon(
        imageVector = Icons.Default.Home,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onPrimary
    )
}
            }

            // Media Type Filters
            item {
                ModernLibraryChip(selected = mediaFilter == null, icon = Icons.Default.LibraryMusic, text = "All") {
                    viewModel.libraryMediaTypeFilter.value = null
                    activeArtist = null
                    activeAlbum = null
                }
            }

            item {
                ModernLibraryChip(selected = mediaFilter == MediaType.AUDIO, icon = Icons.Default.MusicNote, text = "Audio") {
                    viewModel.libraryMediaTypeFilter.value = MediaType.AUDIO
                }
            }

            item {
                ModernLibraryChip(selected = mediaFilter == MediaType.VIDEO, icon = Icons.Default.Videocam, text = "Video") {
                    viewModel.libraryMediaTypeFilter.value = MediaType.VIDEO
                }
            }

            // View Mode Dropdown (Library, Files, Artists, Albums)
            item {
                Box {
                    ModernLibraryChip(
                        selected = true,
                        icon = when (viewMode) {
                            AppViewModel.LibraryViewMode.LIBRARY -> Icons.Default.GridView
                            AppViewModel.LibraryViewMode.FILES -> Icons.Default.Folder
                            AppViewModel.LibraryViewMode.ARTISTS -> Icons.Default.Person
                            AppViewModel.LibraryViewMode.ALBUMS -> Icons.Default.Album
                        },
                        text = when (viewMode) {
                            AppViewModel.LibraryViewMode.LIBRARY -> "Library"
                            AppViewModel.LibraryViewMode.FILES -> "Files"
                            AppViewModel.LibraryViewMode.ARTISTS -> "Artists"
                            AppViewModel.LibraryViewMode.ALBUMS -> "Albums"
                        },
                        showDropdown = true
                    ) { viewExpanded = true }

                    pEQDropdownMenu(expanded = viewExpanded, onDismissRequest = { viewExpanded = false }) {
                        AppViewModel.LibraryViewMode.values().forEach { mode ->
                            pEQDropdownItem(
                                text = mode.name,
                                icon = when (mode) {
                                    AppViewModel.LibraryViewMode.LIBRARY -> Icons.Default.GridView
                                    AppViewModel.LibraryViewMode.FILES -> Icons.Default.Folder
                                    AppViewModel.LibraryViewMode.ARTISTS -> Icons.Default.Person
                                    AppViewModel.LibraryViewMode.ALBUMS -> Icons.Default.Album
                                },
                                isSelected = viewMode == mode,
                                onClick = { 
                                    viewModel.libraryViewMode.value = mode
                                    activeArtist = null
                                    activeAlbum = null
                                    viewExpanded = false 
                                }
                            )
                        }
                    }
                }
            }

            // Sort Dropdown (Moved to same row)
            item {
                Box {
                    ModernLibraryChip(
                        selected = viewModel.sortBy.value != "NAME",
                        icon = Icons.AutoMirrored.Filled.Sort,
                        text = viewModel.sortBy.value,
                        showDropdown = true
                    ) { sortExpanded = true }

                    pEQDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        listOf("NAME", "SIZE", "DATE").forEach { option ->
                            pEQDropdownItem(
                                text = option,
                                icon = when (option) {
                                    "NAME" -> Icons.Default.SortByAlpha
                                    "SIZE" -> Icons.Default.Storage
                                    "DATE" -> Icons.Default.CalendarToday
                                    else -> Icons.AutoMirrored.Filled.Sort
                                },
                                isSelected = viewModel.sortBy.value == option,
                                onClick = { viewModel.sortBy.value = option; sortExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        // 🔗 DYNAMIC BREADCRUMBS & DIRECTIONAL NAV
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.8f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                // Directional Navigation
                Row(
    modifier = Modifier.padding(end = 8.dp),
    verticalAlignment = Alignment.CenterVertically
) {

    val canBack =
        viewModel.folderHistory.isNotEmpty() ||
                activeArtist != null ||
                activeAlbum != null

    val navButtonColors =
        IconButtonDefaults.filledTonalIconButtonColors(

            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.72f),

            contentColor =
                MaterialTheme.colorScheme.onSurfaceVariant,

            disabledContainerColor =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.22f),

            disabledContentColor =
                MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = 0.28f)
        )

    FilledTonalIconButton(
        onClick = {

            if (activeArtist != null) {
                activeArtist = null
            } else if (activeAlbum != null) {
                activeAlbum = null
            } else {
                viewModel.navigateBackFolder()
            }
        },

        modifier = Modifier.size(34.dp),

        enabled = canBack,

        colors = navButtonColors
    ) {

        Icon(
            imageVector =
                Icons.AutoMirrored.Filled.ArrowBack,

            contentDescription = null,

            modifier = Modifier.size(16.dp)
        )
    }

    Spacer(Modifier.width(6.dp))

    FilledTonalIconButton(
        onClick = {
            viewModel.navigateForwardFolder()
        },

        modifier = Modifier.size(34.dp),

        enabled =
            viewModel.folderFuture.isNotEmpty() &&
                    activeArtist == null &&
                    activeAlbum == null,

        colors = navButtonColors
    ) {

        Icon(
            imageVector =
                Icons.AutoMirrored.Filled.ArrowForward,

            contentDescription = null,

            modifier = Modifier.size(16.dp)
        )
    }
}

                LazyRow(
    modifier = Modifier.weight(1f),

    horizontalArrangement =
        Arrangement.spacedBy(2.dp),

    verticalAlignment =
        Alignment.CenterVertically
) {

    val segments = when {

        activeArtist != null -> {

            listOf(
                viewMode.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },

                activeArtist!!
            )
        }

        activeAlbum != null -> {

            listOf(
                viewMode.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },

                activeAlbum!!
            )
        }

        viewMode ==
                AppViewModel.LibraryViewMode.FILES -> {

            effectivePath
                .split("/")
                .filter { it.isNotEmpty() }
        }

        else -> listOf("Library")
    }

    itemsIndexed(segments) { index, segment ->

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (index > 0) {

                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,

                    contentDescription = null,

                    modifier = Modifier.size(13.dp),

                    tint =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.38f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {

                        if (
                            viewMode ==
                            AppViewModel.LibraryViewMode.ARTISTS ||

                            viewMode ==
                            AppViewModel.LibraryViewMode.ALBUMS
                        ) {

                            if (index == 0) {

                                activeArtist = null
                                activeAlbum = null
                            }

                        } else if (
                            viewMode ==
                            AppViewModel.LibraryViewMode.FILES
                        ) {

                            val newPath =
                                "/" + segments
                                    .take(index + 1)
                                    .joinToString("/")

                            viewModel.currentFolderPath.value =
                                newPath
                        }
                    }
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            ) {

                Text(
                    text = segment,

                    fontSize = 11.sp,

                    fontWeight =
                        if (index == segments.lastIndex)
                            FontWeight.Black
                        else
                            FontWeight.Medium,

                    color =
                        if (index == segments.lastIndex)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
            }
        }

        // 📋 ASSET LISTING
        Box(modifier = Modifier.fillMaxSize()) {
            if (currentFolders.isEmpty() && filteredItems.isEmpty()) {
                EmptyLibraryState(viewMode == AppViewModel.LibraryViewMode.FILES)
            } else {
                if (showGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(160.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        val grouped = if (viewMode == AppViewModel.LibraryViewMode.ARTISTS) {
                            sourceItems.filter { mediaFilter == null || it.type == mediaFilter }.groupBy { it.artist ?: "Unknown Artist" }
                        } else {
                            sourceItems.filter { mediaFilter == null || it.type == mediaFilter }.groupBy { it.album ?: "Unknown Album" }
                        }

                        items(grouped.keys.toList().sorted()) { key ->
                            val representative = grouped[key]?.firstOrNull()
                            if (representative != null) {
                                CompactMediaCard(
                                    item = representative.copy(
                                        title = key,
                                        subtitle = if (viewMode == AppViewModel.LibraryViewMode.ARTISTS) "${grouped[key]?.size} tracks" else representative.artist ?: "Various Artists"
                                    ),
                                    onClick = { if (viewMode == AppViewModel.LibraryViewMode.ARTISTS) activeArtist = key else activeAlbum = key }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                        // Removed SortHeader as it's now in the top row

                        if (viewMode == AppViewModel.LibraryViewMode.FILES) {
                            items(currentFolders) { folder ->
                                val fullPath = if (effectivePath.endsWith("/")) "$effectivePath$folder" else "$effectivePath/$folder"
                                val children = sourceItems.filter { it.path.startsWith(fullPath) }
                                MediaFolderCard(
                                    name = folder,
                                    audioCount = children.count { it.type == MediaType.AUDIO },
                                    videoCount = children.count { it.type == MediaType.VIDEO },
                                    totalSize = children.sumOf { it.sizeBytes },
                                    onClick = { viewModel.navigateToFolder(fullPath) },
                                    onAddQueue = { viewModel.addFolderToQueue(fullPath) },
                                    onAddPlaylist = { viewModel.addFolderToPlaylist(fullPath, "Default") }
                                )
                            }
                        }

                        items(filteredItems, key = { it.path + it.id }) { item ->
                            MediaItemCard(
                                item = item,
                                isActive = viewModel.currentMediaItem.value?.path == item.path,
                                onClick = {
                                    if (isMultiSelectMode) viewModel.toggleSelection(item)
                                    else viewModel.playMediaItem(item)
                                },
                                onLongClick = { viewModel.toggleSelection(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernLibraryChip(
    selected: Boolean, 
    icon: ImageVector, 
    text: String, 
    showDropdown: Boolean = false,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        label = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                if (showDropdown) {
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    )
}

@Composable
private fun pEQDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
    ) {
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = onDismissRequest,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            content = content
        )
    }
}

@Composable
private fun pEQDropdownItem(text: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { 
            Text(
                text, 
                fontSize = 12.sp, 
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        trailingIcon = { if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun EmptyLibraryState(isInsideFolder: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = if (isInsideFolder) Icons.Default.FolderOff else Icons.Default.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("NO MEDIA DETECTED", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(if (isInsideFolder) "This directory appears to be empty." else "Check your archive paths in settings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactMediaCard(item: pEQMediaItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Surface(modifier = Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                pEQImage(
                    data = item.thumbnail,
                    fallbackPath = item.path,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    mediaType = item.type
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(item.title, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
