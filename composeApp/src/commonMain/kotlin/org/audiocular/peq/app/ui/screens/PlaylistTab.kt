package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistManagerTab(viewModel: AppViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }

    if (selectedPlaylist != null) {
        PlaylistDetailScreen(
            viewModel = viewModel,
            playlistName = selectedPlaylist!!,
            onBack = { selectedPlaylist = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            SectionHeader("USER COLLECTIONS")
            Spacer(Modifier.height(18.dp))

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
                        onClick = { showCreateDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                }

                // SPECIAL: FAVORITES CARD
                item {
                    pEQItemCard(
                        title = "FAVORITES",
                        subtitle = "${viewModel.favoriteItems.size} pinned items",
                        icon = Icons.Default.Favorite,
                        onClick = { selectedPlaylist = "FAVORITES" },
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader("YOUR COLLECTIONS")
                    Spacer(Modifier.height(4.dp))
                }

                if (viewModel.customPlaylists.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(Modifier.height(12.dp))
                                Text("No Custom Playlists", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }

                items(viewModel.customPlaylists.toList()) { (name, list) ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    pEQItemCard(
                        title = name.uppercase(),
                        subtitle = "${list.size} tracks",
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        onClick = { selectedPlaylist = name },
                        onLongClick = { showDeleteConfirm = true }
                    )

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Collection", fontWeight = FontWeight.Black) },
                            text = { Text("Are you sure you want to permanently delete '$name'?") },
                            confirmButton = {
                                TextButton(onClick = { viewModel.deletePlaylist(name); showDeleteConfirm = false }) {
                                    Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) { Text("CANCEL") }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("NEW COLLECTION", fontWeight = FontWeight.Black) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleaned = playlistName.trim()
                    if (cleaned.isNotEmpty()) {
                        viewModel.createPlaylist(cleaned)
                        showCreateDialog = false
                    }
                }) {
                    Text("CREATE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("CANCEL") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailScreen(viewModel: AppViewModel, playlistName: String, onBack: () -> Unit) {
    val items = if (playlistName == "FAVORITES") viewModel.favoriteItems else viewModel.customPlaylists[playlistName] ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlistName.uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                Text("${items.size} TRACKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { viewModel.shufflePlaylistAndPlay(playlistName) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                }
                
                Spacer(Modifier.width(8.dp))
                
                Button(
                    onClick = { viewModel.openPlaylistAsQueue(playlistName) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("START AS QUEUE", fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Tracks Yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Add media to start building your collection.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 120.dp)) {
                itemsIndexed(items, key = { _, item -> item.path + item.id }) { index, item ->
                    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                if (playlistName == "FAVORITES") viewModel.toggleFavorite(item)
                                else viewModel.removeFromPlaylist(playlistName, item)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)).padding(end = 16.dp), contentAlignment = Alignment.CenterEnd) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    ) {
                        MediaItemCard(
                            item = item,
                            showIndex = true,
                            index = index + 1,
                            isActive = viewModel.currentMediaItem.value?.path == item.path,
                            onClick = { viewModel.playMediaItem(item) },
                            trailing = {
                                Icon(
                                    Icons.Default.DragHandle,
                                    null,
                                    modifier = Modifier.pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragEnd = { accumulatedDrag = 0f },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                accumulatedDrag += dragAmount
                                                if (accumulatedDrag > 60f && index < items.lastIndex) {
                                                    viewModel.reorderPlaylist(playlistName, index, index + 1)
                                                    accumulatedDrag = 0f
                                                } else if (accumulatedDrag < -60f && index > 0) {
                                                    viewModel.reorderPlaylist(playlistName, index, index - 1)
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun pEQItemCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, containerColor: androidx.compose.ui.graphics.Color? = null) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor ?: MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        }
    }
}
