package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.AppViewModel

/** pEQ NEURAL MIXER: TACTILE STEM CONSOLE High-fidelity 6-track vertical mixing surface. */
@Composable
fun pEQMixerScreen(viewModel: AppViewModel) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val currentItem = viewModel.currentMediaItem.value
    val stems = viewModel.stemVolumes.keys.toList()

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent).padding(horizontal = 20.dp, vertical = 16.dp)) {
        // Console Header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    org.audiocular.peq.app.ui.theme.pEQStrings.NEURAL_MIXER,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp
                )
                Text("NEURAL STEM ENGINE v2.0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.startStemSplit() },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = { viewModel.currentTab.value = Tab.Dashboard }) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
        
        // --- MASTER MONITORING ---
        MasterMonitoringSection(viewModel, primaryColor)

        // --- STEM CONSOLE ---
        StemConsoleSection(viewModel, primaryColor, stems)

        // --- MASTER FADER & IO ---
        MasterFaderSection(viewModel, primaryColor)
    }
}

@Composable
private fun MasterFaderSection(viewModel: AppViewModel, primaryColor: Color) {
    MixerSectionCard("MASTER FADER & IO", icon = Icons.Default.Tune, info = "Global output gain and bit-perfect hardware status.") {
        Surface(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("MASTER GAIN", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryColor)
                    Text("HARDWARE BYPASS ACTIVE", fontSize = 7.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
                Spacer(Modifier.weight(1f))
                Text("${(viewModel.masterVolume.value * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MasterMonitoringSection(viewModel: AppViewModel, primaryColor: Color) {
    MixerSectionCard("MASTER MONITORING", icon = Icons.Default.Monitor, info = "Post-fader master bus RTA and peak level metrics.") {
        viewModel.activeJob.value?.let { job ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                color = primaryColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(job.name.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                        Text("${(job.progress * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                    }
                    LinearProgressIndicator(
                        progress = { job.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = primaryColor,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                    Text(job.status, fontSize = 8.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun StemConsoleSection(viewModel: AppViewModel, primaryColor: Color, stems: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(420.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeStems = stems.ifEmpty { listOf("Vocals", "Drums", "Bass", "Piano", "Guitar", "Other") }
            items(activeStems) { stem ->
                StemFaderColumn(
                    name = stem,
                    volume = viewModel.stemVolumes[stem.lowercase()] ?: 1.0f,
                    isMuted = viewModel.stemMutes[stem.lowercase()] ?: false,
                    isSolo = viewModel.stemSolos[stem.lowercase()] ?: false,
                    pan = viewModel.stemPans[stem.lowercase()] ?: 0f,
                    onVolumeChange = { viewModel.updateStemVolume(stem.lowercase(), it) },
                    onMuteToggle = { viewModel.toggleStemMute(stem) },
                    onSoloToggle = { viewModel.toggleStemSolo(stem.lowercase(), !(viewModel.stemSolos[stem.lowercase()] ?: false)) },
                    onPanChange = { viewModel.updateStemPan(stem, it) }
                )
            }
        }
    }
}

@Composable
fun StemFaderColumn(
    name: String, 
    volume: Float, 
    isMuted: Boolean,
    isSolo: Boolean,
    pan: Float,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onPanChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight().width(56.dp)
    ) {
        // Solo / Mute Controls
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                onClick = onSoloToggle,
                shape = RoundedCornerShape(4.dp),
                color = if (isSolo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("S", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isSolo) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface)
                }
            }
            Surface(
                onClick = onMuteToggle,
                shape = RoundedCornerShape(4.dp),
                color = if (isMuted) Color.Red else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("M", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isMuted) Color.Black else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Pan Knob (Simulated)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PAN", fontSize = 7.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Slider(
                value = pan,
                onValueChange = onPanChange,
                valueRange = -1f..1f,
                modifier = Modifier.width(48.dp).height(24.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Vertical Fader Hub
        Box(
            modifier = Modifier.weight(1f).width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // Track Groove
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Color.Black, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
            )

            // Tactile Slider
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier
                    .graphicsLayer { rotationZ = -90f }
                    .requiredWidth(320.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.onSurface,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stem Identity
        Surface(
            color = if (volume > 0.01f && !isMuted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                name.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (volume > 0.01f && !isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        
        Text(
            if (isMuted) "MUTED" else "${(volume * 100).toInt()}%", 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Medium,
            color = if (isMuted) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = if (volume > 0.01f) 1f else 0.4f)
        )

        Spacer(Modifier.height(8.dp))

        // Post-Fader FX Slot
        Surface(
            onClick = { /* Open FX Picker */ },
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            modifier = Modifier.size(width = 48.dp, height = 20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("FX", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
@Composable
private fun MixerSectionCard(
    title: String, 
    icon: ImageVector, 
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                
                if (info != null) {
                    IconButton(onClick = { showInfo = !showInfo }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }
            }
            
            if (showInfo && info != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(info, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}
