package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralMoodMakerItem(viewModel: AppViewModel) {
    var selectedMood by remember { mutableStateOf("Energetic") }
    var selectedType by remember { mutableStateOf("Music") }
    val moods = listOf("Energetic", "Chill", "Focused", "Melancholic")
    val types = listOf("Music", "Videos", "All")

    SectionHeader("NEURAL SYNC ENGINE")
    Spacer(Modifier.height(6.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = pEQDesign.GlassColor,
        border = pEQDesign.GlassBorder,
        tonalElevation = pEQDesign.GlassTonalElevation
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MOOD GENERATOR", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                
                // Target selection moved to top row
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.forEach { type ->
                        val selected = selectedType == type
                        Surface(
                            onClick = { selectedType = type },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(type.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(moods) { mood ->
                    val selected = selectedMood == mood
                    Surface(
                        onClick = { selectedMood = mood },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Text(mood, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.generateMoodPlaylist(selectedMood, selectedType) }, 
                modifier = Modifier.fillMaxWidth().height(48.dp), 
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Bolt, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("INITIATE NEURAL SYNC", fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
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
        StreamIntakeDialog(onDismiss = { showStreamDialog = false }, onConfirm = { url, type ->
            viewModel.addStreamUrl(url, type)
            showStreamDialog = false
        })
    }
}

@Composable
fun StreamIntakeDialog(onDismiss: () -> Unit, onConfirm: (String, MediaType) -> Unit) {
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
                Button(onClick = { onConfirm(url, MediaType.AUDIO) }) { Text("AUDIO") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onConfirm(url, MediaType.VIDEO) }) { Text("VIDEO") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
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
    onTabSelected: (Int) -> Unit
) {
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
                4 -> Icons.Default.Settings
                else -> Icons.Default.Radio
            }

            Surface(
                onClick = { onTabSelected(index) },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                tonalElevation = if (selected) 6.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier.height(44.dp).padding(horizontal = if (selected) 16.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
                    AnimatedVisibility(
                        visible = selected,
                        enter = fadeIn(tween(180)) + expandHorizontally(),
                        exit = fadeOut(tween(120)) + shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(6.dp))
                            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.2.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Visible)
                        }
                    }
                }
            }
        }
    }
}
