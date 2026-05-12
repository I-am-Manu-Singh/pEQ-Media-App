package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.data.MediaType
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import androidx.compose.ui.layout.ContentScale
import io.kamel.image.asyncPainterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputChange
import org.audiocular.peq.app.ui.theme.pEQStrings

@Composable
fun pEQWorkstationDashboard(viewModel: AppViewModel = LocalAppViewModel.current) {
    val selectedTab by viewModel.dashboardSubTab
    val tabs = listOf("HOME", "LIBRARY", "PLAYLISTS", "QUEUES", "APP SETTINGS")
    val _currentItem = viewModel.currentMediaItem.value

    Column(modifier = Modifier.fillMaxSize()) {
        // Integrated Workstation Search Header
        

        pEQTabs(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { viewModel.dashboardSubTab.value = it }
        )
            if (selectedTab != 0 && selectedTab != 2 && selectedTab != 3 && selectedTab != 7 && selectedTab != 8) {
            Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                            Icons.Default.Search,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                            value = viewModel.globalSearchQuery.value,
                            onValueChange = { viewModel.globalSearchQuery.value = it },
                            modifier = Modifier.weight(1f),
                            textStyle =
                                    LocalTextStyle.current.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                    ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                    if (viewModel.globalSearchQuery.value.isNotEmpty()) {
                        IconButton(
                                onClick = { viewModel.globalSearchQuery.value = "" },
                                modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                    Icons.Default.Close,
                                    null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                modifier = Modifier.fillMaxSize(),
                label = "dashboard_tabs"
        ) { target ->
            when (target) {
                0 -> DashboardHome(viewModel)
                1 -> UnifiedMediaExplorerTab(viewModel)
                2 -> PlaylistManagerTab(viewModel)
                3 -> QueueManagerTab(viewModel)
                4 -> SystemStatusTab(viewModel)
            }
        }

        // 📁 Industrial Archive Registry (Folder Manager)
        if (viewModel.showFolderManager.value) {
            PEQArchiveRegistryDialog(onDismiss = { viewModel.showFolderManager.value = false })
        }
        
        // 🧠 Neural Workstation Dialog
        // if (viewModel.showNeuralScan.value) {
        //     NeuralScanDialog(onDismiss = { viewModel.showNeuralScan.value = false })
        // }
    }
}

@Composable
private fun DashboardHome(viewModel: AppViewModel) {
    val items by viewModel.mediaLibrary.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 140.dp)
        ) {
            // ⭐ PRO STATS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "AUDIO",
                        value = items.count { it.type == MediaType.AUDIO }.toString(),
                        icon = Icons.Default.Audiotrack,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "VIDEO",
                        value = items.count { it.type == MediaType.VIDEO }.toString(),
                        icon = Icons.Default.VideoLibrary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "STORAGE",
                        value = "${items.sumOf { it.sizeBytes } / 1024 / 1024}MB",
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 🧠 NEURAL MOOD ENGINE
            item {
                NeuralMoodMakerItem(viewModel)
            }

            // 📂 ARCHIVE COMMAND STATION
            item {
                SectionHeader("ARCHIVE COMMAND STATION")
                Spacer(Modifier.height(12.dp))
                ArchiveCommandCard(viewModel)
            }

            // 🚀 MOST PLAYED
            item {
                SectionHeader("MOST PLAYED")
                Spacer(Modifier.height(12.dp))
                MostPlayedSection(viewModel)
            }

          

            // 📂 ARCHIVE MANAGEMENT

        }
    }
}

enum class LibraryViewMode {

    LIBRARY,
    FILES,
    ARTISTS,
    ALBUMS
}

@Composable
fun UnifiedMediaExplorerTab(

    viewModel: AppViewModel

) {

    val allItems by

    viewModel.mediaLibrary.collectAsState()

    val searchResults by

    viewModel.searchResults.collectAsState()

    val currentPath =

        viewModel.currentFolderPath.value

    val mediaFilter by

    viewModel.libraryMediaTypeFilter

    val viewMode by

    viewModel.libraryViewMode
    val _isLibraryMode =

        viewMode ==
                AppViewModel.LibraryViewMode.LIBRARY

    val isArtistsMode =

        viewMode ==
                AppViewModel.LibraryViewMode.ARTISTS

    val isAlbumsMode =

        viewMode ==
                AppViewModel.LibraryViewMode.ALBUMS
    val isFilesMode =

        viewMode ==
                AppViewModel.LibraryViewMode.FILES
    val selectedItems =

        viewModel.selectedItems

    val isMultiSelectMode =

        selectedItems.isNotEmpty()

    var activeArtist by remember {

        mutableStateOf<String?>(null)

    }

    var activeAlbum by remember {

        mutableStateOf<String?>(null)

    }

    var sortExpanded by remember {

        mutableStateOf(false)

    }

    var viewExpanded by remember {

        mutableStateOf(false)

    }

    // =========================

    // SOURCE ITEMS

    // =========================

    val sourceItems =

        if (

            viewModel.globalSearchQuery.value

                .isNotEmpty()

        ) {

            searchResults

        } else {

            allItems

        }

    // =========================

    // ROOT PATH

    // =========================

    val baseArchive =

        remember(allItems) {

            if (allItems.isEmpty()) ""

            else {

                val paths =

                    allItems.map { it.path }

                var prefix =

                    paths.first()

                        .substringBeforeLast("/")

                for (i in 1 until paths.size) {

                    while (

                        !paths[i].startsWith(prefix)

                    ) {

                        prefix =

                            prefix.substringBeforeLast(

                                "/",

                                ""

                            )

                        if (prefix.isEmpty()) {

                            break

                        }

                    }

                }

                prefix

            }

        }

    val effectivePath =

        currentPath.ifEmpty {

            baseArchive

        }

    // =========================

    // AVAILABLE VIEW MODES

    // =========================

    val availableViewModes =

        remember(mediaFilter) {

            when (mediaFilter) {

                MediaType.AUDIO -> listOf<AppViewModel.LibraryViewMode>(

                    AppViewModel.LibraryViewMode.LIBRARY,

                    AppViewModel.LibraryViewMode.FILES,

                    AppViewModel.LibraryViewMode.ARTISTS,

                    AppViewModel.LibraryViewMode.ALBUMS

                )

                MediaType.VIDEO -> listOf<AppViewModel.LibraryViewMode>(

                    AppViewModel.LibraryViewMode.LIBRARY,

                    AppViewModel.LibraryViewMode.FILES

                )

                else -> listOf<AppViewModel.LibraryViewMode>(

                    AppViewModel.LibraryViewMode.LIBRARY,

                    AppViewModel.LibraryViewMode.FILES

                )

            }

        }

    // =========================

    // FILTERED ITEMS

    // =========================

    val filteredItems =

        remember(

            sourceItems,

            effectivePath,

            mediaFilter,

            activeArtist,

            activeAlbum,

            viewMode,

            viewModel.sortBy.value

        ) {

            sourceItems

                .filter { item ->

                    val typeMatch =

                        mediaFilter == null ||

                                item.type == mediaFilter

                    val pathMatch =

                        when (viewMode) {

                            AppViewModel.LibraryViewMode.FILES -> {

                                val itemDir =

                                    item.path.substringBeforeLast("/")

                                itemDir.equals(

                                    effectivePath,

                                    ignoreCase = true

                                )

                            }

                            else -> true

                        }

                    val metadataMatch =

                        when (viewMode) {

                            AppViewModel.LibraryViewMode.ARTISTS -> {

                                activeArtist == null ||

                                        (

                                                item.artist

                                                    ?: "Unknown Artist"

                                                ).equals(

                                                activeArtist,

                                                ignoreCase = true

                                            )

                            }

                            AppViewModel.LibraryViewMode.ALBUMS -> {

                                activeAlbum == null ||

                                        (

                                                item.album

                                                    ?: "Unknown Album"

                                                ).equals(

                                                activeAlbum,

                                                ignoreCase = true

                                            )

                            }

                            else -> true

                        }

                    typeMatch &&

                            pathMatch &&

                            metadataMatch

                }

                .sortedWith { a, b ->

                    when (viewModel.sortBy.value) {

                        "SIZE" -> {

                            b.sizeBytes.compareTo(

                                a.sizeBytes

                            )

                        }

                        "DATE" -> {

                            b.dateModified.compareTo(

                                a.dateModified

                            )

                        }

                        else -> {

                            a.title.compareTo(

                                b.title,

                                ignoreCase = true

                            )

                        }

                    }

                }

        }

    // =========================

    // FOLDERS

    // =========================

    val currentFolders =

        remember(

            sourceItems,

            effectivePath,

            mediaFilter,

            viewMode

        ) {

            if (

                viewMode !=

                AppViewModel.LibraryViewMode.FILES

            ) {

                emptyList()

            } else {

                sourceItems

                    .filter {

                        mediaFilter == null ||

                                it.type == mediaFilter

                    }

                    .map { it.path }

                    .filter { path ->

                        val dir =

                            path.substringBeforeLast("/")

                        dir.startsWith(

                            effectivePath,

                            ignoreCase = true

                        ) &&

                                dir.length >

                                effectivePath.length

                    }

                    .map { path ->

                        val relative =

                            path.substring(

                                effectivePath.length

                            ).trimStart('/')

                        relative.substringBefore("/")

                    }

                    .filter {

                        it.isNotEmpty() &&

                                !it.contains(".")

                    }

                    .distinct()

                    .sorted()

            }

        }

    // =========================

    // GRID STATES

    // =========================

    val showArtistGrid =

        viewMode ==

                AppViewModel.LibraryViewMode.ARTISTS &&

                activeArtist == null

    val showAlbumGrid =

        viewMode ==

                AppViewModel.LibraryViewMode.ALBUMS &&

                activeAlbum == null

    val showGrid =

        showArtistGrid || showAlbumGrid

    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(horizontal = 14.dp)

    ) {

        // =========================

        // TOP BAR

        // =========================

        LazyRow(

            modifier = Modifier

                .fillMaxWidth()

                .padding(

                    top = 10.dp,

                    bottom = 10.dp

                ),

            horizontalArrangement =

                Arrangement.spacedBy(10.dp),

            verticalAlignment =

                Alignment.CenterVertically

        ) {

            // =========================

            // HOME

            // =========================

            item {

                FilledTonalIconButton(

                    onClick = {

                        viewModel

                            .currentFolderPath

                            .value = ""

                        activeArtist = null

                        activeAlbum = null

                    },

                    modifier =

                        Modifier.size(38.dp)

                ) {

                    Icon(

                        Icons.Default.Home,

                        null,

                        modifier =

                            Modifier.size(18.dp)

                    )

                }

            }

            // =========================

            // ALL

            // =========================

            item {

                ModernLibraryChip(

                    selected =

                        mediaFilter == null,

                    icon =

                        Icons.Default.LibraryMusic,

                    text = "All",

                    onClick = {

                        viewModel

                            .libraryMediaTypeFilter

                            .value = null

                        viewModel

                            .libraryViewMode

                            .value =

                            AppViewModel

                                .LibraryViewMode

                                .LIBRARY

                        activeArtist = null

                        activeAlbum = null

                    }

                )

            }

            // =========================

            // AUDIO

            // =========================

            item {

                ModernLibraryChip(

                    selected =

                        mediaFilter ==

                                MediaType.AUDIO,

                    icon =

                        Icons.Default.MusicNote,

                    text = "Audio",

                    onClick = {

                        viewModel

                            .libraryMediaTypeFilter

                            .value =

                            MediaType.AUDIO

                        viewModel

                            .libraryViewMode

                            .value =

                            AppViewModel

                                .LibraryViewMode

                                .LIBRARY

                    }

                )

            }

            // =========================

            // VIDEO

            // =========================

            item {

                ModernLibraryChip(

                    selected =

                        mediaFilter ==

                                MediaType.VIDEO,

                    icon =

                        Icons.Default.Videocam,

                    text = "Video",

                    onClick = {

                        viewModel

                            .libraryMediaTypeFilter

                            .value =

                            MediaType.VIDEO

                        viewModel

                            .libraryViewMode

                            .value =

                            AppViewModel

                                .LibraryViewMode

                                .LIBRARY

                        activeArtist = null

                        activeAlbum = null

                    }

                )

            }

            // =========================

            // VIEW MODE DROPDOWN

            // =========================

            item {

                Box {

                    ModernLibraryChip(

                        selected = false,

                        icon =

                            when (viewMode) {

                                AppViewModel.LibraryViewMode.LIBRARY ->

                                    Icons.Default.GridView

                                AppViewModel.LibraryViewMode.FILES ->

                                    Icons.Default.Folder

                                AppViewModel.LibraryViewMode.ARTISTS ->

                                    Icons.Default.Person

                                AppViewModel.LibraryViewMode.ALBUMS ->

                                    Icons.Default.Album

                            },

                        text =

                            when (viewMode) {

                                AppViewModel.LibraryViewMode.LIBRARY ->

                                    "Library"

                                AppViewModel.LibraryViewMode.FILES ->

                                    "Files"

                                AppViewModel.LibraryViewMode.ARTISTS ->

                                    "Artists"

                                AppViewModel.LibraryViewMode.ALBUMS ->

                                    "Albums"

                            },

                        onClick = {

                            viewExpanded = true

                        }

                    )

                    DropdownMenu(

                        expanded =

                            viewExpanded,

                        onDismissRequest = {

                            viewExpanded = false

                        }

                    ) {

                        availableViewModes.forEach { mode ->

                            DropdownMenuItem(

                                text = {

                                    Text(

                                        when (mode) {

                                            AppViewModel.LibraryViewMode.LIBRARY ->

                                                "Library"

                                            AppViewModel.LibraryViewMode.FILES ->

                                                "Files"

                                            AppViewModel.LibraryViewMode.ARTISTS ->

                                                "Artists"

                                            AppViewModel.LibraryViewMode.ALBUMS ->

                                                "Albums"

                                        }

                                    )

                                },

                                leadingIcon = {

                                    Icon(

                                        when (mode) {

                                            AppViewModel.LibraryViewMode.LIBRARY ->

                                                Icons.Default.GridView

                                            AppViewModel.LibraryViewMode.FILES ->

                                                Icons.Default.Folder

                                            AppViewModel.LibraryViewMode.ARTISTS ->

                                                Icons.Default.Person

                                            AppViewModel.LibraryViewMode.ALBUMS ->

                                                Icons.Default.Album

                                        },

                                        contentDescription = null

                                    )

                                },

                                onClick = {

                                    viewModel

                                        .libraryViewMode

                                        .value = mode

                                    activeArtist = null

                                    activeAlbum = null

                                    viewExpanded = false

                                }

                            )

                        }

                    }

                }

            }

            // =========================

            // SORT

            // =========================

            item {

                Box {

                    ModernLibraryChip(

                        selected = false,

                        icon =

                            Icons.AutoMirrored.Filled.Sort,

                        text =

                            when (

                                viewModel

                                    .sortBy

                                    .value

                            ) {

                                "SIZE" ->

                                    "Size"

                                "DATE" ->

                                    "Recent"

                                else ->

                                    "Name"

                            },

                        onClick = {

                            sortExpanded = true

                        }

                    )

                    DropdownMenu(

                        expanded =

                            sortExpanded,

                        onDismissRequest = {

                            sortExpanded = false

                        }

                    ) {

                        DropdownMenuItem(

                            text = {

                                Text("Name")

                            },

                            onClick = {

                                viewModel

                                    .sortBy

                                    .value =

                                    "NAME"

                                sortExpanded = false

                            }

                        )

                        DropdownMenuItem(

                            text = {

                                Text("Recent")

                            },

                            onClick = {

                                viewModel

                                    .sortBy

                                    .value =

                                    "DATE"

                                sortExpanded = false

                            }

                        )

                        DropdownMenuItem(

                            text = {

                                Text("Size")

                            },

                            onClick = {

                                viewModel

                                    .sortBy

                                    .value =

                                    "SIZE"

                                sortExpanded = false

                            }

                        )

                    }

                }

            }

            // =========================

            // REFRESH

            // =========================

            item {

                FilledTonalIconButton(

                    onClick = {

                        viewModel.refreshLibrary()

                    },

                    modifier =

                        Modifier.size(38.dp)

                ) {

                    Icon(

                        Icons.Default.Refresh,

                        null,

                        modifier =

                            Modifier.size(18.dp)

                    )

                }
            }
        }

        // =========================
        // BREADCRUMB ROW
        // =========================

        val breadcrumbVisible =

            (

                    isFilesMode &&

                            currentPath.isNotEmpty()

                    ) ||

                    (

                            isArtistsMode &&

                                    activeArtist != null

                            ) ||

                    (

                            isAlbumsMode &&

                                    activeAlbum != null

                            )

        AnimatedVisibility(
            visible = breadcrumbVisible
        ) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),

                shape = RoundedCornerShape(22.dp),

                color =
                    MaterialTheme.colorScheme
                        .surfaceContainerLow
            ) {

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 6.dp
                        ),

                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    // BACK

                    item {

                        FilledTonalIconButton(
                            onClick = {

                                when {

                                    activeArtist != null -> {
                                        activeArtist = null
                                    }

                                    activeAlbum != null -> {
                                        activeAlbum = null
                                    }

                                    else -> {
                                        viewModel.navigateBackFolder()
                                    }
                                }
                            },

                            modifier =
                                Modifier.size(34.dp)
                        ) {

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                null,

                                modifier =
                                    Modifier.size(18.dp)
                            )
                        }
                    }

                    // FORWARD

                    item {

                        FilledTonalIconButton(
                            onClick = {
                                viewModel.navigateForwardFolder()
                            },

                            modifier =
                                Modifier.size(34.dp)
                        ) {

                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,

                                modifier =
                                    Modifier.size(18.dp)
                            )
                        }
                    }

                    val segments: List<String> =

                        when {

                            activeArtist != null ->

                                listOf(activeArtist!!)

                            activeAlbum != null ->

                                listOf(activeAlbum!!)

                            else ->

                                currentPath

                                    .split("/")

                                    .filter {

                                        it.isNotEmpty()

                                    }

                        }

                    segments.forEachIndexed {
                            index,
                            segment ->

                        item {

                            if (index > 0) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,

                                    contentDescription = null,

                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                            .copy(alpha = 0.45f),

                                    modifier =
                                        Modifier.size(18.dp)
                                )
                            }

                            TextButton(
                                onClick = {

                                    if (
                                        activeArtist != null
                                    ) {

                                        activeArtist = null

                                    } else if (
                                        activeAlbum != null
                                    ) {

                                        activeAlbum = null

                                    } else {

                                        val newPath =
                                            "/" +
                                                    segments
                                                        .take(index + 1)
                                                        .joinToString("/")

                                        viewModel
                                            .currentFolderPath
                                            .value =
                                            newPath
                                    }
                                }
                            ) {

                                Text(
                                    text = segment,

                                    fontSize = 11.sp,

                                    maxLines = 1,

                                    overflow =
                                        TextOverflow.Ellipsis,

                                    fontWeight =

                                        if (
                                            index ==
                                            segments.lastIndex
                                        ) {

                                            FontWeight.Bold

                                        } else {

                                            FontWeight.Medium
                                        },

                                    color =

                                        if (
                                            index ==
                                            segments.lastIndex
                                        ) {

                                            MaterialTheme
                                                .colorScheme
                                                .primary

                                        } else {

                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================
        // CONTENT
        // =========================

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            if (
                currentFolders.isEmpty() &&
                filteredItems.isEmpty()
            ) {

                EmptyLibraryState(false)
            }

            else {

                if (showGrid) {

                    LazyVerticalGrid(
                        columns =
                            GridCells.Adaptive(160.dp),

                        modifier =
                            Modifier.fillMaxSize(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp),

                        contentPadding =
                            PaddingValues(bottom = 120.dp)
                    ) {

                        val grouped =

                            if (
                                viewMode ==
                                AppViewModel.LibraryViewMode.ARTISTS
                            ) {

                                sourceItems
                                    .filter {

                                        mediaFilter == null ||
                                                it.type == mediaFilter
                                    }

                                    .groupBy {

                                        it.artist
                                            ?: "Unknown Artist"
                                    }

                            } else {

                                sourceItems
                                    .filter {

                                        mediaFilter == null ||
                                                it.type == mediaFilter
                                    }

                                    .groupBy {

                                        it.album
                                            ?: "Unknown Album"
                                    }
                            }

                        items(
                            grouped.keys
                                .toList()
                                .sorted()
                        ) { key ->

                            val representative =
                                grouped[key]
                                    ?.firstOrNull()

                            if (representative != null) {

                                CompactMediaCard(
                                    item =
                                        representative.copy(

                                            title = key,

                                            subtitle =

                                                if (
                                                    viewMode ==
                                                    AppViewModel.LibraryViewMode.ARTISTS
                                                ) {

                                                    "${grouped[key]?.size} tracks"

                                                } else {

                                                    representative.artist
                                                        ?: "Various Artists"
                                                }
                                        ),

                                    onClick = {

                                        if (
                                            viewMode ==
                                            AppViewModel.LibraryViewMode.ARTISTS
                                        ) {

                                            activeArtist = key

                                        } else {

                                            activeAlbum = key
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                else {

                    LazyColumn(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp),

                        contentPadding =
                            PaddingValues(bottom = 120.dp)
                    ) {

                        // =========================
                        // FOLDERS
                        // =========================

                        if (isFilesMode) {

                            items(currentFolders) { folder ->

                                val fullPath =

                                    if (
                                        effectivePath.endsWith("/")
                                    ) {

                                        "$effectivePath$folder"

                                    } else {

                                        "$effectivePath/$folder"
                                    }

                                val children = sourceItems.filter { it.path.startsWith(fullPath) }
                                MediaFolderCard(
                                    name = folder,

                                    audioCount =
                                        children.count {
                                            it.type ==
                                                    MediaType.AUDIO
                                        },

                                    videoCount =
                                        children.count {
                                            it.type ==
                                                    MediaType.VIDEO
                                        },

                                    totalSize =
                                        children.sumOf {
                                            it.sizeBytes
                                        },

                                    onClick = {

                                        viewModel
                                            .navigateToFolder(
                                                fullPath
                                            )
                                    },
                                    onAddQueue = { viewModel.addFolderToQueue(fullPath) },
                                    onAddPlaylist = { viewModel.addFolderToPlaylist(fullPath, "Default") } // Just a placeholder for now
                                )
                            }
                        }

                        // =========================
                        // FILES
                        // =========================

                        items(
                            filteredItems,
                            key = { it.path }
                        ) { item ->

                            MediaItemCard(
                                item = item,

                                isActive =

                                    viewModel
                                        .currentMediaItem
                                        .value
                                        ?.path ==
                                            item.path,

                                onClick = {

                                    if (
                                        isMultiSelectMode
                                    ) {

                                        viewModel
                                            .toggleSelection(
                                                item
                                            )

                                    } else {

                                        viewModel
                                            .playMediaItem(
                                                item
                                            )
                                    }
                                },

                                onLongClick = {

                                    viewModel
                                        .toggleSelection(
                                            item
                                        )
                                }
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
    onClick: () -> Unit
) {

    FilterChip(
        selected = selected,

        onClick = onClick,

        shape = RoundedCornerShape(16.dp),

        border = null,

        leadingIcon = {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },

        label = {

            Text(
                text = text,

                style =
                    MaterialTheme.typography.labelMedium,

                fontWeight =
                    if (selected)
                        FontWeight.SemiBold
                    else
                        FontWeight.Medium
            )
        },

        colors = FilterChipDefaults.filterChipColors(

            containerColor =
                MaterialTheme.colorScheme.surfaceContainerLow,

            selectedContainerColor =
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.14f),

            labelColor =
                MaterialTheme.colorScheme.onSurfaceVariant,

            selectedLabelColor =
                MaterialTheme.colorScheme.primary,

            iconColor =
                MaterialTheme.colorScheme.onSurfaceVariant,

            selectedLeadingIconColor =
                MaterialTheme.colorScheme.primary
        )
    )
}




@Composable
private fun MostPlayedSection(viewModel: AppViewModel) {
    var timeFilter by remember { mutableStateOf("ALL") }
    val items by viewModel.mediaLibrary.collectAsState()
    
    val filteredItems = remember(items, timeFilter) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val threshold = when (timeFilter) {
            "WEEK" -> 7 * 24 * 3600 * 1000L
            "MONTH" -> 30 * 24 * 3600 * 1000L
            "YEAR" -> 365 * 24 * 3600 * 1000L
            else -> Long.MAX_VALUE
        }
        items.filter { it.playCount > 0 && (now - it.lastPlayed) < threshold }
             .sortedByDescending { it.playCount }
             .take(5)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            listOf("WEEK", "MONTH", "YEAR", "ALL").forEach { filter ->
                TextButton(onClick = { timeFilter = filter }) {
                    Text(filter, fontSize = 10.sp, color = if (timeFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        if (filteredItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("NO RECENT PLAYBACK HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        } else {
            filteredItems.forEach { item ->
                MediaItemCard(
                    item = item,
                    showPlayCount = true,
                    onClick = { viewModel.playMediaItem(item) }
                )
            }
        }
    }
}
@Composable
private fun SortHeader(viewModel: AppViewModel) {
    var showSortMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("MEDIA ASSETS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
        Box {
            FilterChip(
                selected = viewModel.sortBy.value != "NAME",
                onClick = { showSortMenu = true },
                label = { Text(viewModel.sortBy.value, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(14.dp)) },
                shape = RoundedCornerShape(12.dp)
            )
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                listOf("NAME", "SIZE", "DATE").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { viewModel.sortBy.value = option; showSortMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(isInsideFolder: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isInsideFolder) Icons.Default.FolderOff else Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("NO MEDIA DETECTED", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(if (isInsideFolder) "This directory appears to be empty." else "Check your archive paths in settings.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class
)
@Composable
fun QueueManagerTab(
    viewModel: AppViewModel
) {
    QueueContent(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
    )
}
@Composable
fun FavoritesTab(
    viewModel: AppViewModel
) {

    val favorites = viewModel.favoriteItems

    val searchQuery =
        viewModel.globalSearchQuery.value

    val filtered =
        remember(favorites, searchQuery) {

            favorites.filter {

                it.title.contains(searchQuery, true) ||
                        it.subtitle.contains(searchQuery, true)
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        SectionHeader("Pinned Media Assets")

        Spacer(Modifier.height(16.dp))

        if (filtered.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Surface(
                        modifier = Modifier.size(82.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "No Favorites Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Pinned media will appear here.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {

                items(
                    items = filtered,
                    key = { it.path }
                ) { item ->

                    MediaItemCard(
                        item = item,
                        isActive =
                            viewModel.currentMediaItem.value?.path == item.path,
                        onClick = {
                            viewModel.playMediaItem(item)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistManagerTab(
    viewModel: AppViewModel
) {

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var selectedPlaylist by remember {
        mutableStateOf<String?>(null)
    }

    // =========================
    // PLAYLIST DETAIL SCREEN
    // =========================

    if (selectedPlaylist != null) {

        val playlistName =
            selectedPlaylist!!

        val playlistItems =
            if (playlistName == "FAVORITES")
                viewModel.favoriteItems
            else
                viewModel.customPlaylists[playlistName]
                    ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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
                        selectedPlaylist = null
                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Filled.ArrowBack,

                        contentDescription = null
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            playlistName.uppercase(),

                        fontWeight =
                            FontWeight.Black,

                        fontSize = 20.sp,

                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text =
                            "${playlistItems.size} TRACKS",

                        fontSize = 10.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing = 1.sp,

                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = { viewModel.shufflePlaylistAndPlay(playlistName) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(20.dp))
                    }
                    
                    FilledTonalIconButton(
                        onClick = { viewModel.openPlaylistAsQueue(playlistName) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.QueueMusic, null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // =========================
// EMPTY STATE
// =========================

            if (playlistItems.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Surface(
                            modifier = Modifier.size(82.dp),

                            shape = CircleShape,

                            color =
                                MaterialTheme.colorScheme
                                    .surfaceContainerHigh
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.PlaylistPlay,

                                    contentDescription = null,

                                    tint =
                                        MaterialTheme.colorScheme
                                            .primary
                                            .copy(alpha = 0.65f),

                                    modifier =
                                        Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = "No Tracks Yet",

                            fontSize = 18.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text =
                                "Add media to start building your collection.",

                            fontSize = 12.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }

// =========================
// PLAYLIST ITEMS
// =========================

            else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp),

                    contentPadding =
                        PaddingValues(bottom = 120.dp)
                ) {

                    itemsIndexed(
                        items = playlistItems,
                        key = { _, item -> item.path }
                    ) { index, item ->

                        var accumulatedDrag by remember {
                            mutableFloatStateOf(0f)
                        }

                        val dismissState =
                            rememberSwipeToDismissBoxState(

                                confirmValueChange = { value ->

                                    if (
                                        value ==
                                        SwipeToDismissBoxValue.EndToStart
                                    ) {

                                        viewModel.removeFromPlaylist(
                                            playlistName,
                                            item
                                        )

                                        true

                                    } else {

                                        false
                                    }
                                }
                            )

                        SwipeToDismissBox(
                            state = dismissState,

                            enableDismissFromStartToEnd = false,

                            backgroundContent = {

                                Surface(
                                    modifier =
                                        Modifier.fillMaxSize(),

                                    shape =
                                        RoundedCornerShape(16.dp),

                                    color =
                                        MaterialTheme.colorScheme
                                            .error
                                            .copy(alpha = 0.10f)
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(end = 16.dp),

                                        contentAlignment =
                                            Alignment.CenterEnd
                                    ) {

                                        Icon(
                                            Icons.Default.Delete,
                                            null,

                                            tint =
                                                MaterialTheme.colorScheme
                                                    .error
                                        )
                                    }
                                }
                            }
                        ) {

                            MediaItemCard(
                                item = item,

                                showIndex = true,

                                index = index + 1,

                                onClick = {
                                    viewModel.playMediaItem(item)
                                },

                                trailing = {

                                    Icon(
                                        Icons.Default.DragHandle,
                                        null,

                                        modifier = Modifier
                                            .pointerInput(Unit) {

                                                detectVerticalDragGestures(

                                                    onDragEnd = {

                                                        accumulatedDrag = 0f
                                                    },

                                                    onVerticalDrag = {
                                                            change,
                                                            dragAmount ->

                                                        change.consume()

                                                        accumulatedDrag +=
                                                            dragAmount

                                                        val threshold = 60f

                                                        if (
                                                            accumulatedDrag >
                                                            threshold &&

                                                            index <
                                                            playlistItems.lastIndex
                                                        ) {

                                                            viewModel
                                                                .reorderPlaylist(
                                                                    playlistName,
                                                                    index,
                                                                    index + 1
                                                                )

                                                            accumulatedDrag = 0f
                                                        }

                                                        else if (
                                                            accumulatedDrag <
                                                            -threshold &&

                                                            index > 0
                                                        ) {

                                                            viewModel
                                                                .reorderPlaylist(
                                                                    playlistName,
                                                                    index,
                                                                    index - 1
                                                                )

                                                            accumulatedDrag = 0f
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            )
                        }
                    }
                }

                }

        }

    } else {

        // =========================
        // PLAYLIST GRID
        // =========================

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                SectionHeader(
                    "USER COLLECTIONS"
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { viewModel.isMoodMakerVisible.value = true },
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("MOOD MAKER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }


            Spacer(Modifier.height(18.dp))

            // =========================
            // LIST OF COLLECTIONS
            // =========================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // NEW COLLECTION CARD
                item {
                    pEQItemCard(
                        title = "CREATE NEW COLLECTION",
                        subtitle = "Organize your neural assets",
                        icon = Icons.Default.PlaylistAdd,
                        onClick = {
                            showCreateDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }

                // SPECIAL: FAVORITES CARD
                item {
                    pEQItemCard(
                        title = "FAVORITES",
                        subtitle = "${viewModel.favoriteItems.size} pinned items",
                        icon = Icons.Default.Favorite,
                        onClick = {
                            selectedPlaylist = "FAVORITES"
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("YOUR COLLECTIONS")
                    Spacer(Modifier.height(4.dp))
                }

                // CUSTOM PLAYLISTS
                if (viewModel.customPlaylists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.LibraryMusic,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No Custom Playlists",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }


                    // CUSTOM PLAYLISTS
                    items(viewModel.customPlaylists.toList()) { (name, list) ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        
                        pEQItemCard(
                            title = name.uppercase(),
                            subtitle = "${list.size} tracks",
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            onClick = {
                                selectedPlaylist = name
                            },
                            onLongClick = {
                                showDeleteConfirm = true
                            }
                        )

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Delete Collection", fontWeight = FontWeight.Black) },
                                text = { Text("Are you sure you want to permanently delete '$name'?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deletePlaylist(name)
                                        showDeleteConfirm = false
                                    }) {
                                        Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("CANCEL")
                                    }
                                }
                            )
                        }
                    }
                }
        }
    }

    // =========================
    // CREATE PLAYLIST DIALOG
    // =========================

    if (showCreateDialog) {

        var playlistName by remember {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
            },

            shape = RoundedCornerShape(24.dp),

            title = {

                Text(
                    text = "NEW COLLECTION",

                    fontWeight =
                        FontWeight.Black
                )
            },

            text = {

                OutlinedTextField(
                    value = playlistName,

                    onValueChange = {
                        playlistName = it
                    },

                    placeholder = {
                        Text("Playlist Name")
                    },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(16.dp)
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        val cleaned =
                            playlistName.trim()

                        if (cleaned.isNotEmpty()) {

                            viewModel.createPlaylist(
                                cleaned
                            )

                            showCreateDialog =
                                false
                        }
                    }
                ) {

                    Text(
                        text = "CREATE",

                        color =
                            MaterialTheme.colorScheme
                                .primary,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showCreateDialog = false
                    }
                ) {

                    Text("CANCEL")
                }
            }
        )
    }
}



@Composable
fun SystemStatusTab(
    viewModel: AppViewModel
) {

    val scrollState =
        rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp),

        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Spacer(Modifier.height(6.dp))

        // =========================
        // PROFILE
        // =========================

        ProfileSection(viewModel)

        // =========================
        // HEADER CARD
        // =========================

        Column(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 16.dp
                )
            ) {

                Text(
                    text = "SYSTEM CONTROL CENTER",

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Black,

                    color =
                        MaterialTheme.colorScheme.primary,

                    letterSpacing = 2.2.sp
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text =
                        "Configure playback behavior, interface tuning and library services.",

                    fontSize = 13.sp,

                    lineHeight = 18.sp,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.74f),

                    fontWeight = FontWeight.Medium
                )
            
        }

        // =========================
        // USER INTERFACE
        // =========================

        SystemSectionBlock(
            title = "USER INTERFACE",
            icon = Icons.Default.Palette
        ) {

            ThemePicker(viewModel)

            Spacer(Modifier.height(10.dp))

            HardwareControlItem(
                label = "HAPTIC FEEDBACK",

                value =
                    if (
                        viewModel.isHapticsEnabled.value
                    ) {
                        "Enabled"
                    } else {
                        "Disabled"
                    },

                desc =
                    "Vibrate on gestures and controls",

                enabled =
                    viewModel.isHapticsEnabled.value,

                onToggle = {
                    viewModel
                        .isHapticsEnabled
                        .value = it
                }
            )

            Spacer(Modifier.height(10.dp))

            HardwareControlItem(
                label = "DYNAMIC COLORS",
                value = if (viewModel.isDynamicColorsEnabled.value) "Adaptive" else "Static",
                desc = "Generate palette from artwork",
                enabled = viewModel.isDynamicColorsEnabled.value,
                onToggle = { viewModel.isDynamicColorsEnabled.value = it }
            )
        }

        // =========================
        // PLAYBACK
        // =========================

        SystemSectionBlock(
            title = "PLAYBACK TOOLS",
            icon = Icons.Default.Timer
        ) {

            SleepTimer(viewModel)

            Spacer(Modifier.height(10.dp))

            HardwareControlItem(
                label = "GAPLESS PLAYBACK",

                value =
                    if (
                        viewModel
                            .isGaplessEnabled
                            .value
                    ) {
                        "Enabled"
                    } else {
                        "Disabled"
                    },

                desc =
                    "Remove silence between tracks",

                enabled =
                    viewModel
                        .isGaplessEnabled
                        .value,

                onToggle = {
                    viewModel
                        .isGaplessEnabled
                        .value = it
                }
            )

            HardwareControlItem(
                label = "NEURAL SATURATION",
                value = if (viewModel.isNeuralSaturationEnabled.value) "Active" else "Bypass",
                desc = "Industrial harmonics & gain boost",
                enabled = viewModel.isNeuralSaturationEnabled.value,
                onToggle = { viewModel.toggleNeuralSaturation(it) }
            )

            HardwareControlItem(
                label = "BUFFER PRIORITY",
                value = viewModel.bufferPriority.value,
                desc = "Reduce latency on high-end devices",
                enabled = viewModel.bufferPriority.value == "High",
                onToggle = { viewModel.bufferPriority.value = if (it) "High" else "Low" }
            )
        }

        // =========================
        // LIBRARY
        // =========================

        SystemSectionBlock(
            title = "LIBRARY SERVICES",
            icon = Icons.Default.LibraryMusic
        ) {

            HardwareControlItem(
                label = "AUTOMATIC SCAN",

                value = if (viewModel.isAutoScanEnabled.value) "Enabled" else "Disabled",

                desc =
                    "Refresh library on startup",

                enabled = viewModel.isAutoScanEnabled.value, onToggle = { viewModel.isAutoScanEnabled.value = it }
            )
        }

        SystemSectionBlock(
            title = "CLOUD INTEGRATION",
            icon = Icons.Default.Cloud
        ) {
            OutlinedTextField(
                value = viewModel.persistenceManager.openSubtitlesApiKey,
                onValueChange = { viewModel.persistenceManager.openSubtitlesApiKey = it },
                label = { Text("OpenSubtitles.com API Key", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            Text(
                "Required for cloud subtitle fetching (v1 API)",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                modifier = Modifier.padding(bottom = 14.dp, start = 14.dp)
            )
        }

        SystemSectionBlock(
            title = "DIAGNOSTICS",
            icon = Icons.Default.BugReport
        ) {
            Button(
                onClick = { viewModel.exportLogs() },
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(12.dp))
                Text("EXPORT SYSTEM LOGS (SHA-256)", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        // =========================
        // FOOTER SPACE
        // =========================

        Spacer(Modifier.height(90.dp))
    }
}

@Composable
private fun SystemSectionBlock(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {

    Column {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape = CircleShape,

                color =
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.12f)
            ) {

                Box(
                    modifier = Modifier.padding(8.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector = icon,

                        contentDescription = null,

                        tint =
                            MaterialTheme.colorScheme.primary,

                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = title,

                fontSize = 11.sp,

                fontWeight = FontWeight.Black,

                letterSpacing = 1.4.sp,

                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(24.dp),

            color =
                MaterialTheme.colorScheme.surfaceContainerHigh
                    .copy(alpha = 0.58f),

            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.08f)
            )
        ) {

            Column(
                modifier = Modifier.padding(10.dp),

                verticalArrangement =
                    Arrangement.spacedBy(4.dp),

                content = content
            )
        }
    }
}
/* ---------- UI COMPONENTS ---------- */

@OptIn(

    ExperimentalMaterial3Api::class,

    ExperimentalFoundationApi::class

)


@Composable
private fun pEQFilterChip(
    selected: Boolean,
    label: @Composable () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        shape = shape,
        color =
            if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        border = BorderStroke(
            1.dp,
            if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(6.dp))
            }

            label()
        }
    }
}
@Composable
fun pEQTabs(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 10.dp
            ),

        horizontalArrangement =
            Arrangement.spacedBy(10.dp),

        contentPadding = PaddingValues(
            horizontal = 4.dp
        )
    ) {

        itemsIndexed(tabs) { index, title ->

            val selected =
                selectedTab == index

            val icon =
                when (index) {

                    0 -> Icons.Default.Dashboard
                    1 -> Icons.Default.LibraryMusic
                    2 -> Icons.AutoMirrored.Filled.QueueMusic
                    3 -> Icons.AutoMirrored.Filled.PlaylistPlay
                    4 -> Icons.Default.Settings

                    else -> Icons.Default.Radio
                }

            val containerColor =
                if (selected) {

                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.14f)

                } else {

                    MaterialTheme.colorScheme.surfaceContainer
                        .copy(alpha = 0.82f)
                }

            val borderColor =
                if (selected) {

                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.24f)

                } else {

                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
                }

            Surface(
                onClick = {
                    onTabSelected(index)
                },

                shape = RoundedCornerShape(20.dp),

                color = containerColor,

                border = BorderStroke(
                    1.dp,
                    borderColor
                ),

                tonalElevation =
                    if (selected) 6.dp
                    else 1.dp,

                shadowElevation =
                    if (selected) 6.dp
                    else 0.dp
            ) {

                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .padding(
                            start =
                                if (selected) 14.dp
                                else 12.dp,

                            end =
                                if (selected) 18.dp
                                else 12.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    Icon(
                        imageVector = icon,

                        contentDescription = title,

                        tint =
                            if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                                    .copy(alpha = 0.72f),

                        modifier = Modifier.size(18.dp)
                    )

                    AnimatedVisibility(
                        visible = selected,

                        enter =
                            fadeIn(
                                animationSpec = tween(180)
                            ) + expandHorizontally(),

                        exit =
                            fadeOut(
                                animationSpec = tween(120)
                            ) + shrinkHorizontally()
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Spacer(
                                Modifier.width(6.dp)
                            )

                            Text(
                                text = title,

                                fontSize = 10.sp,

                                fontWeight = FontWeight.Black,

                                letterSpacing = 0.2.sp,

                                color =
                                    MaterialTheme.colorScheme.primary,
                                
                                textAlign = TextAlign.Center,

                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun CompactMediaCard(
    item: pEQMediaItem,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {

    val isVideo =
        item.type == MediaType.VIDEO

    Surface(
        onClick = onClick,

        modifier = Modifier.width(116.dp),

        shape = RoundedCornerShape(20.dp),

        color =
            if (isPlaying)
                MaterialTheme.colorScheme.surfaceContainerHighest
            else
                MaterialTheme.colorScheme.surfaceContainerLow,

        border = BorderStroke(
            1.dp,

            if (isPlaying)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else
                MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),

        tonalElevation =
            if (isPlaying) 3.dp
            else 1.dp,

        shadowElevation = 0.dp
    ) {

        Column(
            modifier = Modifier.padding(7.dp)
        ) {

            // =========================
            // ARTWORK
            // =========================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
            ) {

                pEQImage(
                    data = item.thumbnail,

                    modifier = Modifier.fillMaxSize(),

                    contentScale = ContentScale.Crop
                )

                // SIMPLE CINEMATIC OVERLAY

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.22f)
                                )
                            )
                        )
                )

                // PLAYING STATE

                if (isPlaying) {

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            )
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(38.dp),

                        shape = CircleShape,

                        color =
                            MaterialTheme.colorScheme.primary
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.GraphicEq,

                                contentDescription = null,

                                tint =
                                    MaterialTheme.colorScheme.onPrimary,

                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // VIDEO PLAY ICON

                else if (isVideo) {

                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp),

                        shape = CircleShape,

                        color = Color.Black.copy(alpha = 0.48f)
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.PlayArrow,

                                contentDescription = null,

                                tint = Color.White,

                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // TOP FORMAT CHIP

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val label1 = if (isVideo) mapResolutionToLabel(item.resolution) else (item.format ?: "PCM").uppercase()
                    val label2 = if (isVideo) item.codec else if (item.sampleRate > 0) "${item.sampleRate / 1000}k" else null

                    listOfNotNull(label1, label2).forEach { text ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = text,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                fontSize = 6.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                // LOSSLESS BADGE

                if (!isVideo && item.isLossless) {

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),

                        shape = RoundedCornerShape(8.dp),

                        color =
                            MaterialTheme.colorScheme.primary
                    ) {

                        Text(
                            text = "LOSSLESS",

                            modifier = Modifier.padding(
                                horizontal = 6.dp,
                                vertical = 3.dp
                            ),

                            fontSize = 6.sp,

                            fontWeight = FontWeight.Black,

                            color =
                                MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // =========================
            // TITLE
            // =========================

            Text(
                text = item.title,

                fontSize = 10.sp,

                lineHeight = 12.sp,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis,

                fontWeight =
                    if (isPlaying)
                        FontWeight.Bold
                    else
                        FontWeight.SemiBold,

                color =
                    if (isPlaying)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
            )

            // =========================
            // SUBTITLE
            // =========================

            if (item.subtitle.isNotBlank()) {

                Spacer(Modifier.height(2.dp))

                Text(
                    text = item.subtitle,

                    fontSize = 8.sp,

                    lineHeight = 10.sp,

                    maxLines = 1,

                    overflow = TextOverflow.Ellipsis,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun ProfileSection(viewModel: AppViewModel) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("NEURAL IDENTITY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text("Workstation Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThemePicker(
    viewModel: AppViewModel
) {

    val selectedTheme =
        viewModel.selectedTheme.value

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        // =========================
        // OLED
        // =========================

        pEQThemePaletteCard(
            title = "OLED",

            palette = listOf(
                Color(0xFF000000),
                Color(0xFF121212),
                Color(0xFF2A2A2A)
            ),

            isSelected =
                selectedTheme.name == "OLED",

            modifier = Modifier.weight(1f),

            onClick = {
                viewModel.updateTheme(
                    org.audiocular.peq.app.ui.theme
                        .pEQTheme.OLED
                )
            }
        )

        // =========================
        // LIGHT
        // =========================

        pEQThemePaletteCard(
            title = "LIGHT",

            palette = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFEAEAEA),
                Color(0xFFCFCFCF)
            ),

            isSelected =
                selectedTheme.name == "LIGHT",

            modifier = Modifier.weight(1f),

            onClick = {
                viewModel.updateTheme(
                    org.audiocular.peq.app.ui.theme
                        .pEQTheme.LIGHT
                )
            }
        )

       // =========================

// DARK

// =========================

pEQThemePaletteCard(

    title = "DARK",

    palette = listOf(

        Color(0xFF121212),

        Color(0xFF1E1E1E),

        Color(0xFF3A3A3A)

    ),

    isSelected =

        selectedTheme.name == "DARK",

    modifier = Modifier.weight(1f),

    onClick = {

        viewModel.updateTheme(

            org.audiocular.peq.app.ui.theme

                .pEQTheme.DARK

        )

    }

)
    }
}

@Composable
private fun pEQThemePaletteCard(
    title: String,
    palette: List<Color>,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,

        modifier = modifier,

        shape = RoundedCornerShape(22.dp),

        color =

            if (isSelected) {

                MaterialTheme.colorScheme
                    .primary
                    .copy(alpha = 0.12f)

            } else {

                MaterialTheme.colorScheme
                    .surfaceContainer
            },

        tonalElevation =
            if (isSelected) 6.dp
            else 2.dp,

        shadowElevation =
            if (isSelected) 10.dp
            else 4.dp,

        border = BorderStroke(

            width =
                if (isSelected) 1.4.dp
                else 1.dp,

            color =

                if (isSelected) {

                    MaterialTheme.colorScheme
                        .primary
                        .copy(alpha = 0.35f)

                } else {

                    MaterialTheme.colorScheme
                        .outline
                        .copy(alpha = 0.08f)
                }
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 14.dp
            ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // =========================
            // PALETTE ROW
            // =========================

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {

                palette.forEach { color ->

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                1.dp,
                                Color.White.copy(
                                    alpha = 0.12f
                                ),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                text = title,

                fontSize = 10.sp,

                fontWeight = FontWeight.Black,

                letterSpacing = 1.sp,

                color =

                    if (isSelected) {

                        MaterialTheme.colorScheme
                            .primary

                    } else {

                        MaterialTheme.colorScheme
                            .onSurface
                    }
            )
        }
    }
}

@Composable
fun HapticSlider(viewModel: AppViewModel) {
    HardwareControlItem(
        "HAPTIC INTENSITY",
        "${(viewModel.hapticIntensity.floatValue * 100).toInt()}%",
        "Industrial vibration sync",
        true
    ) {
        viewModel.hapticIntensity.floatValue = if (it) 1f else 0f
    }
}

@Composable
fun SleepTimer(viewModel: AppViewModel) {
    HardwareControlItem(
        "SLEEP TIMER",
        if (viewModel.sleepTimerActive.value) "${viewModel.sleepTimerMinutes.value}m remaining" else "Off",
        "Automated session shutdown",
        viewModel.sleepTimerActive.value
    ) {
        viewModel.toggleSleepTimer(it)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralMoodMakerItem(viewModel: AppViewModel) {

    var selectedMood by remember {
        mutableStateOf("Energetic")
    }

    var selectedType by remember {
        mutableStateOf("Music")
    }

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
        tonalElevation = pEQDesign.GlassTonalElevation,
        shadowElevation = pEQDesign.GlassElevation
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "MOOD GENERATOR",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "VIBE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(5.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                items(moods) { mood ->

                    pEQFilterChip(
                        selected = selectedMood == mood,
                        onClick = {
                            selectedMood = mood
                        },
                        label = {
                            Text(
                                text = mood,
                                fontSize = 10.sp
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "TARGET",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(5.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                items(types) { type ->

                    pEQFilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                        },
                        label = {
                            Text(
                                text = type,
                                fontSize = 10.sp
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    viewModel.generateMoodPlaylist(
                        selectedMood,
                        selectedType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {

                Text(
                    text = "GENERATE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
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
                    Icon(Icons.Default.FolderSpecial, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                            onClick = { viewModel.openFolder(folder); viewModel.dashboardSubTab.value = 2 },
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
    var showStreamDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = pEQDesign.GlassColor,
        border = pEQDesign.GlassBorder
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("STREAM INTAKE", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("Initialize HLS/MP4/MP3 network feeds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showStreamDialog = true }, shape = RoundedCornerShape(12.dp)) {
                        Text("INTAKE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { viewModel.openWebSearch("youtube", "YOUTUBE SEARCH") }, shape = RoundedCornerShape(12.dp)) {
                        Text("YT SEARCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    
    if (showStreamDialog) {
        StreamIntakeDialog(onDismiss = { showStreamDialog = false }, onConfirm = { url: String, type: org.audiocular.peq.app.data.MediaType ->
            viewModel.addStreamUrl(url, type)
            showStreamDialog = false
        })
    }
}

@Composable
fun StreamIntakeDialog(onDismiss: () -> Unit, onConfirm: (String, org.audiocular.peq.app.data.MediaType) -> Unit) {
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stream Intake Manifest", fontWeight = FontWeight.Black) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Network URL (HLS / MP4 / MP3 / FLAC)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Select Intake Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Row {
                Button(onClick = { onConfirm(url, org.audiocular.peq.app.data.MediaType.AUDIO) }) { Text("AUDIO") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onConfirm(url, org.audiocular.peq.app.data.MediaType.VIDEO) }) { Text("VIDEO") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}
