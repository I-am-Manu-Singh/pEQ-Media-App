package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.ui.components.*

@Composable
fun DownloadsScreen(viewModel: AppViewModel) {
    val downloads = viewModel.downloadQueue

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader("INTAKE QUEUE")
        Spacer(Modifier.height(16.dp))

        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudQueue, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                    Spacer(Modifier.height(16.dp))
                    Text("NO ACTIVE DOWNLOADS", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(0.3f))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(downloads) { job ->
                    DownloadJobCard(job, viewModel)
                }
            }
        }
    }
}

@Composable
fun DownloadJobCard(job: DownloadJob, viewModel: AppViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = if (job.status == "COMPLETE") BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f)) else null
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
                    Text(job.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(job.status, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
                
                if (job.status == "COMPLETE") {
                    IconButton(onClick = { 
                        job.filePath?.let { path ->
                            // Find in library or create item
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
                    }) {
                        Icon(Icons.Default.PlayCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            if (job.status != "COMPLETE" && !job.status.contains("ERROR")) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = job.progress,
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(job.progress * 100).toInt()}%", fontSize = 10.sp)
                    if (job.etaSeconds > 0) {
                        Text("ETA: ${job.etaSeconds}s", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
