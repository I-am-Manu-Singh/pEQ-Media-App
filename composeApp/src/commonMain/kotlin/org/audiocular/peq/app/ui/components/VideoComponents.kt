package org.audiocular.peq.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.viewmodel.*

@Composable
fun VideoControlOverlay(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val isLocked by viewModel.isVideoLocked
    val isPlaying by viewModel.isPlaying
    val item by viewModel.currentMediaItem
    val aspectRatio by viewModel.videoAspectRatio

    var showGestureUI by remember { mutableStateOf(false) }
    var gestureLabel by remember { mutableStateOf("") }
    var gestureValue by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { showGestureUI = true },
                    onDragEnd = { showGestureUI = false },
                    onVerticalDrag = { change, dragAmount ->
                        val isLeft = change.position.x < size.width / 2
                        if (isLeft) {
                            // 💡 Brightness
                            viewModel.videoBrightness.value = (viewModel.videoBrightness.value - dragAmount / 1000f).coerceIn(0f, 1f)
                            gestureLabel = "BRIGHTNESS"
                            gestureValue = viewModel.videoBrightness.value
                        } else {
                            // 🔊 Volume
                            viewModel.setMasterVolume((viewModel.masterVolume.value - dragAmount / 1000f).coerceIn(0f, 1f))
                            gestureLabel = "VOLUME"
                            gestureValue = viewModel.masterVolume.value
                        }
                    }
                )
            }
    ) {
        // 🎬 GESTURE OVERLAY UI
        AnimatedVisibility(
            visible = showGestureUI,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(gestureLabel, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { gestureValue },
                        modifier = Modifier.width(120.dp).height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // 🔒 LOCK SCREEN LAYER
        if (isLocked) {
            // ... (rest of the code remains same)
            Box(
                modifier = Modifier.fillMaxSize().clickable(enabled = true) { viewModel.isVideoLocked.value = false },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(40.dp).padding(20.dp))
                }
            }
        } else {
            // 🎬 ACTIVE CONTROLS LAYER
            Column(modifier = Modifier.fillMaxSize()) {
                
                // TOP BAR
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Text(
                        item?.title ?: "VIDEO PLAYER",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { 
                            viewModel.isLandscapeLocked.value = !viewModel.isLandscapeLocked.value 
                        }) {
                            Icon(
                                if (viewModel.isLandscapeLocked.value) Icons.Default.ScreenLockLandscape else Icons.Default.ScreenRotation, 
                                null, 
                                tint = if (viewModel.isLandscapeLocked.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.isChaptersVisible.value = true }) {
                            Icon(Icons.Default.List, null, tint = if (viewModel.currentChapters.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { 
                            if (viewModel.isSubtitlesEnabled.value) {
                                viewModel.toggleSubtitles(false)
                            } else {
                                if (viewModel.availableSubtitleTracks.isNotEmpty()) {
                                    viewModel.toggleSubtitles(true)
                                } else {
                                    viewModel.fetchSubtitles(autoOpenEditor = true)
                                }
                            }
                        }) {
                            Icon(Icons.Default.ClosedCaption, null, tint = if (viewModel.isSubtitlesEnabled.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { viewModel.showAudioTrackConsole.value = true }) {
                            Icon(Icons.Default.Audiotrack, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { 
                            viewModel.videoAspectRatio.value = when(aspectRatio) {
                                "FIT" -> "STRETCH"
                                "STRETCH" -> "FILL"
                                "FILL" -> "16:9"
                                "16:9" -> "4:3"
                                else -> "FIT"
                            }
                        }) {
                            Icon(
                                when(aspectRatio) {
                                    "STRETCH" -> Icons.Default.OpenInFull
                                    "FILL" -> Icons.Default.Fullscreen
                                    else -> Icons.Default.AspectRatio
                                }, 
                                null, 
                                tint = if (aspectRatio != "FIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { 
                            viewModel.videoZoom.value = if (viewModel.videoZoom.value == 1f) 1.5f else 1f
                        }) {
                            Icon(Icons.Default.CropFree, null, tint = if (viewModel.videoZoom.value > 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        // REMOVED: Redundant Subtitle Edit Button (Consolidated to one button)
                        IconButton(onClick = { 
                            viewModel.fetchEverything()
                        }) {
                            Icon(Icons.Default.AutoAwesome, null, tint = if (viewModel.isFetchingEverything.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                     IconButton(
                            onClick = { viewModel.cyclePlaybackSpeed() }
                        ) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        "${viewModel.playbackSpeed.value}x",
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = "Playback Speed",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                        IconButton(onClick = { viewModel.isVideoLocked.value = true }) {
                            Icon(Icons.Default.LockOpen, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // CENTER CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousTrack() }) {
                        Icon(Icons.Default.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(36.dp))
                    }

                    IconButton(onClick = { viewModel.seekRelative(-10000L) }) {
                        Icon(Icons.Default.Replay10, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(42.dp))
                    }
                    
                    Surface(
                        onClick = { viewModel.togglePlayback() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.seekRelative(30000L) }) {
                        Icon(Icons.Default.Forward30, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(42.dp))
                    }

                    IconButton(onClick = { viewModel.nextTrack() }) {
                        Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(Modifier.weight(1f))

                // BOTTOM TELEMETRY & SEEK
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).background(pEQDesign.GlassColor, RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                    VideoSeekBar(viewModel)
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TelemetryChip("RESOLUTION", item?.resolution ?: "1080P")
                        TelemetryChip("CODEC", item?.format ?: "H.264")
                        TelemetryChip("SYNC", "0ms")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoSeekBar(viewModel: AppViewModel) {
    val position = viewModel.currentPosition.value
    val duration = viewModel.currentDuration.value.coerceAtLeast(1L)
    val progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    Column {
        Slider(
            value = progress,
            onValueChange = { viewModel.seekTo((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
            Text(formatTime(duration), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TelemetryChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 7.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
