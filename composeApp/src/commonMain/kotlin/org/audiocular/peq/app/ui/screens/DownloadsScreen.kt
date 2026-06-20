package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.components.pEQDesign

@Composable
fun DownloadsScreen(viewModel: AppViewModel) {
    val downloads = viewModel.downloadQueue
    val activeJobs = remember(downloads) { downloads.filter { it.status != "COMPLETE" && !it.status.contains("ERROR") } }
    val completedJobs = remember(downloads) { downloads.filter { it.status == "COMPLETE" } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("CLOUD INTAKE & SYSTEM CACHE")
        Spacer(Modifier.height(16.dp))

        // ⭐ DOWNLOAD STATS CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = pEQDesign.GlassColor,
            border = pEQDesign.GlassBorder
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem("ACTIVE", activeJobs.size.toString(), modifier = Modifier.weight(1f))
                StatItem("COMPLETED", completedJobs.size.toString(), modifier = Modifier.weight(1f))
                val totalSizeMb = remember(completedJobs) {
                    completedJobs.sumOf {
                        val file = it.filePath?.let { p -> java.io.File(p) }
                        if (file != null && file.exists()) file.length() else 0L
                    } / 1024f / 1024f
                }
                StatItem("CACHED DATA", "${totalSizeMb.formatSize()} MB", modifier = Modifier.weight(1.2f))
            }
        }

        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 🔄 ACTIVE DOWNLOADS SECTION
            if (activeJobs.isNotEmpty()) {
                item {
                    Text("ACTIVE DISCOVERY JOBS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                items(activeJobs) { job ->
                    DownloadJobCard(job, viewModel)
                }
            }

            // 💾 COMPLETED DOWNLOADS SECTION
            if (completedJobs.isNotEmpty()) {
                item {
                    Text("LOCAL OFFLINE CACHE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                items(completedJobs) { job ->
                    CompletedJobCard(job, viewModel)
                }
            }

            // Empty state if absolutely nothing is present
            if (downloads.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudQueue, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                            Spacer(Modifier.height(16.dp))
                            Text("NO DISCOVERY HISTORIES FOUND", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DownloadJobCard(job: DownloadJob, viewModel: AppViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (job.type == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(job.status, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }
            
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { job.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(job.progress * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (job.etaSeconds > 0) {
                    Text("ETA: ${job.etaSeconds}s", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun CompletedJobCard(job: DownloadJob, viewModel: AppViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (job.type == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.Audiotrack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(job.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                val ext = job.filePath?.substringAfterLast(".")?.uppercase() ?: "WAV"
                val sizeStr = remember(job.filePath) {
                    val file = job.filePath?.let { p -> java.io.File(p) }
                    if (file != null && file.exists()) "${(file.length() / 1024f / 1024f).formatSize()} MB" else "unknown size"
                }
                Text("$ext • $sizeStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Play Button
                IconButton(
                    onClick = {
                        job.filePath?.let { path ->
                            val item = pEQMediaItem(
                                id = "dl_${job.id}",
                                title = job.title,
                                subtitle = "Downloaded Media",
                                path = path,
                                type = job.type,
                                format = path.substringAfterLast("."),
                                sizeBytes = 0,
                                durationMs = 0
                            )
                            viewModel.playMediaItem(item)
                        }
                    },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }

                // Delete Button
                IconButton(
                    onClick = { viewModel.deleteDownloadedJob(job) },
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun Float.formatSize(): String {
    return try {
        val parts = this.toString().split(".")
        if (parts.size == 2) {
            parts[0] + "." + parts[1].take(2)
        } else {
            this.toString()
        }
    } catch (_: Exception) {
        "0.00"
    }
}
