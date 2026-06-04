package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.viewmodel.toggleNeuralSaturation
import org.audiocular.peq.app.ui.components.pEQGlassCard
import kotlinx.datetime.Clock
import org.audiocular.peq.app.ui.components.*

@Composable
fun HardwareScreen(viewModel: AppViewModel) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 900.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .verticalScroll(scrollState)
                .padding(if (isWide) 32.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- NOW PLAYING TELEMETRY ---
            NowPlayingTelemetryCard(viewModel, primaryColor)

            if (isWide) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        HardwareCoreSection(viewModel, primaryColor)
                        SystemEngineRegistry(viewModel, primaryColor)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        CalibrationMasterSection(viewModel, primaryColor)
                        AdvancedProtocolsSection(viewModel, primaryColor)
                    }
                }
            } else {
                // --- HARDWARE CORE & STATUS ---
                HardwareCoreSection(viewModel, primaryColor)

                // --- SYSTEM ENGINE REGISTRY ---
                SystemEngineRegistry(viewModel, primaryColor)

                // --- PROTOCOLS & CALIBRATION ---
                CalibrationMasterSection(viewModel, primaryColor)
                AdvancedProtocolsSection(viewModel, primaryColor)
            }
        
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun HardwareCoreSection(viewModel: AppViewModel, primaryColor: Color) {
    HardwareSectionCard("HARDWARE CORE & STATUS", icon = Icons.Default.Dns, info = "JNI signal tracing and hardware output registry.") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            HardwareRegistrySection(viewModel, primaryColor)
        }
    }
}

@Composable
private fun SystemEngineRegistry(viewModel: AppViewModel, primaryColor: Color) {
    HardwareSectionCard("SYSTEM ENGINE REGISTRY", icon = Icons.Default.Hub, info = "Low-level service and neural engine configurations.") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            NeuralEngineRegistry(viewModel)
            CoreAudioServiceRegistry(viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedProtocolsSection(viewModel: AppViewModel, primaryColor: Color) {
    HardwareSectionCard("ADVANCED ENGINE PROTOCOLS", icon = Icons.Default.Security) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HardwareControlItem(
                label = "EXCLUSIVE MODE", 
                value = "BYPASS", 
                desc = "Direct DMA access to hardware (No Mixing)", 
                enabled = false, 
                onToggle = { viewModel.audioEngine.setExclusiveMode(it) }
            )
            HardwareControlItem(
                label = "INTERFERENCE SHIELDING", 
                value = "ACTIVE", 
                desc = "Filter electromagnetic bus noise", 
                enabled = true, 
                onToggle = { viewModel.audioEngine.setInterferenceShielding(it) }
            )
            HardwareControlItem(
                label = "MIC PHASE ALIGNMENT", 
                value = "SYNCED", 
                desc = "Temporal sync for input/output", 
                enabled = true, 
                onToggle = { viewModel.audioEngine.alignMicPhase(it) }
            )
            HardwareControlItem(
                label = "ZERO LATENCY MONITORING", 
                value = "DISABLED", 
                desc = "Hardware loopback for recording", 
                enabled = false, 
                onToggle = { viewModel.audioEngine.enableZeroLatencyMonitoring(it) }
            )
            HardwareControlItem(
                label = "OVERFLOW PROTECTION", 
                value = "ACTIVE", 
                desc = "Neural buffer drift correction", 
                enabled = true, 
                onToggle = { viewModel.audioEngine.setOverflowProtection(it) }
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Bluetooth Codec Forcing
            Column {
                Text("BLUETOOTH CODEC FORCING", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                Spacer(Modifier.height(10.dp))
                val codecs = listOf("SBC", "AAC", "aptX", "LDAC")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    codecs.forEach { codec ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.audioEngine.forceBluetoothCodec(codec) },
                            label = { Text(codec, fontSize = 9.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            HardwareControlItem(
                label = "OSC BROADCASTING", 
                value = "OFF", 
                desc = "Remote workstation synchronization", 
                enabled = false, 
                onToggle = { viewModel.audioEngine.setOscBroadcasting(it) }
            )
        }
    }
}

@Composable
private fun CalibrationMasterSection(viewModel: AppViewModel, primaryColor: Color) {
    HardwareSectionCard("CALIBRATION & PHASE MASTER", icon = Icons.Default.CompassCalibration) {
        HardwareControlItem(label = "PHASE INVERSION", value = if (viewModel.phaseInverted.value) "INVERTED" else "NORMAL", desc = "Correct 180° polarity errors", enabled = viewModel.phaseInverted.value, onToggle = { viewModel.togglePhaseInversion(it) })
        Spacer(Modifier.height(20.dp))
        
        // Audio Balance
        Column {
            Text("STEREO IMAGE BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
            Slider(
                value = viewModel.channelBalance.value,
                onValueChange = { viewModel.updateChannelBalance(it) },
                valueRange = -1f..1f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LEFT", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("CENTER", fontSize = 9.sp, color = primaryColor)
                Text("RIGHT", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun HardwareSectionCard(
    title: String, 
    icon: ImageVector, 
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    
    pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), letterSpacing = 1.2.sp)
                
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
                    Text(info, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun HardwareControlItem(label: String, value: String, desc: String, enabled: Boolean, onToggle: ((Boolean) -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
        }
        if (onToggle != null) {
            Switch(
                checked = enabled, 
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )
            )
        } else {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                Text(value, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ArchiveCommandSection(viewModel: AppViewModel, primaryColor: Color) {
    val scanProgress by viewModel.scanProgress
    val scanStatus by viewModel.scanStatus

    HardwareSectionCard("ACTIVE ARCHIVES", icon = Icons.Default.FolderSpecial) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ARCHIVE REPOSITORY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${viewModel.scanFolders.size} paths registered", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
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
                LinearProgressIndicator(progress = { scanProgress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                Text(scanStatus, fontSize = 10.sp)
            }

            if (viewModel.scanFolders.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.scanFolders) { folder ->
                        AssistChip(
                            onClick = { viewModel.openFolder(folder) },
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
private fun HardwareRegistrySection(viewModel: AppViewModel, primaryColor: Color) {
    val registry = viewModel.audioEngine.getRegistry()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        registry.forEach { (key, value) ->
            HardwareControlItem(
                label = key,
                value = value,
                desc = "Active $key configuration from system bus.",
                enabled = true
            )
        }
    }
}

@Composable
private fun NeuralEngineRegistry(viewModel: AppViewModel) {
    val status = if (viewModel.isNeuralSaturationEnabled.value) "ACTIVE" else "BYPASS"
    HardwareControlItem(
        label = "NEURAL SATURATION", 
        value = status, 
        desc = "Harmonic generator via ${viewModel.audioEngine.getDecoderInfo()} pipeline", 
        enabled = viewModel.isNeuralSaturationEnabled.value, 
        onToggle = { viewModel.toggleNeuralSaturation(it) }
    )
}

@Composable
private fun CoreAudioServiceRegistry(viewModel: AppViewModel) {
    HardwareControlItem(label = "BACKGROUND SERVICE", value = if (viewModel.isBackgroundServiceRunning.value) "RUNNING" else "STOPPED", desc = "Persistent DSP engine life-cycle", enabled = viewModel.isBackgroundServiceRunning.value, onToggle = { viewModel.toggleBackgroundService(it) })
}
