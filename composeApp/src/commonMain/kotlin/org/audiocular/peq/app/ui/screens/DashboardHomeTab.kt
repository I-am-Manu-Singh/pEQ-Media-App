package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.MediaType
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*

@Composable
fun DashboardHome(viewModel: AppViewModel) {
    val items by viewModel.mediaLibrary.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 1000.dp
        
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (isWide) 32.dp else 16.dp),
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

            if (isWide) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            // 🧠 NEURAL MOOD ENGINE
                            NeuralMoodMakerItem(viewModel)
                            
                            SectionHeader("ARCHIVE COMMAND STATION")
                            ArchiveCommandCard(viewModel)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            StreamingIntakeCard(viewModel)
                            
                            SectionHeader("MOST PLAYED")
                            MostPlayedSection(viewModel)
                        }
                    }
                }
            } else {
                // 🧠 NEURAL MOOD ENGINE
                item {
                    NeuralMoodMakerItem(viewModel)
                }

                // 📂 ARCHIVE COMMAND STATION
                item {
                    SectionHeader("ARCHIVE COMMAND STATION")
                    Spacer(Modifier.height(12.dp))
                    ArchiveCommandCard(viewModel)
                    Spacer(Modifier.height(12.dp))
                    StreamingIntakeCard(viewModel)
                }

                // 🚀 MOST PLAYED
                item {
                    SectionHeader("MOST PLAYED")
                    Spacer(Modifier.height(12.dp))
                    MostPlayedSection(viewModel)
                }
            }
        }
    }
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
             .take(10)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            listOf("WEEK", "MONTH", "YEAR", "ALL").forEach { filter ->
                TextButton(
                    onClick = { timeFilter = filter },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        filter, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black,
                        color = if (timeFilter == filter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        
        if (filteredItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("NO RECENT PLAYBACK HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(filteredItems) { item ->
                    SquareMediaCard(
                        item = item,
                        onClick = { viewModel.playMediaItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun SquareMediaCard(item: org.audiocular.peq.app.data.pEQMediaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Box {
                pEQImage(
                    data = item.thumbnail,
                    fallbackPath = item.path,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Play count badge
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = item.playCount.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(Modifier.height(10.dp))
        
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.artist ?: "Unknown Artist",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
