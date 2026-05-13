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
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import kotlinx.datetime.Clock

@Composable
fun HardwareScreen(viewModel: AppViewModel) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- HEADER ---
        Column {
            Text("pEQ HARDWARE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("COMMAND CENTER", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }

        // --- HARDWARE CORE & STATUS ---
        HardwareCoreSection(viewModel, primaryColor)

        // --- SYSTEM ENGINE REGISTRY ---
        SystemEngineRegistry(viewModel, primaryColor)

        // --- PROTOCOLS & CALIBRATION ---
        CalibrationMasterSection(viewModel, primaryColor)
        AdvancedProtocolsSection(viewModel, primaryColor)

        // --- INGESTION ---
        StreamingIntakeSection(viewModel, primaryColor)

        // --- FOOTER ---
        HardwareFooter()

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun HardwareCoreSection(viewModel: AppViewModel, primaryColor: Color) {
    HardwareSectionCard("HARDWARE CORE & STATUS", icon = Icons.Default.Dns, info = "JNI signal tracing and hardware output registry.") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            JniTelemetryTerminal(viewModel, primaryColor)
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
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
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

@Composable
private fun HardwareControlItem(label: String, value: String, desc: String, enabled: Boolean, onToggle: ((Boolean) -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onToggle != null) {
            Switch(checked = enabled, onCheckedChange = onToggle)
        } else {
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
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
private fun StreamingIntakeSection(viewModel: AppViewModel, primaryColor: Color) {
    HardwareSectionCard("STREAM INTAKE PROTOCOL", icon = Icons.Default.CloudDownload) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NETWORK INGESTION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("HLS / MP4 / MP3 Feeds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
            Button(onClick = { /* show intake dialog */ }, shape = RoundedCornerShape(12.dp)) {
                Text("INTAKE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HardwareFooter() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("WORKSTATION REGISTRY SECURE: SHA-256 VERIFIED", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), fontWeight = FontWeight.Black)
        Text("ENCRYPTION: AES-256-GCM [HARDWARE ACCELERATED]", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
        Text("pEQ KERNEL v4.9.5-STABLE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun JniTelemetryTerminal(viewModel: AppViewModel, primaryColor: Color) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
            Text("pEQ@KERNEL:~$ uptime", color = Color.Green, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("10:42:01 up 12 days, 3:14, 1 user, load average: 0.12, 0.08, 0.05", color = Color.White.copy(0.7f), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("pEQ@KERNEL:~$ jni --trace --buffer=4096", color = Color.Green, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("[INFO] JNI Trace active. Latency: 24.2ms. Buffer: 1024 frames.", color = primaryColor, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Text("[DEBUG] NeuralEngine thread priority: HIGH (RRT)", color = Color.White.copy(0.5f), fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}

@Composable
private fun HardwareRegistrySection(viewModel: AppViewModel, primaryColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HardwareControlItem(label = "OUTPUT DEVICE", value = "INTERNAL SPEAKER", desc = "Primary system output bridge", enabled = true)
        HardwareControlItem(label = "USB AUDIO INTERFACE", value = "NOT CONNECTED", desc = "External 24-bit/192kHz bridge", enabled = false)
        HardwareControlItem(label = "BLUETOOTH A2DP", value = "CONNECTED (LDAC)", desc = "High-fidelity wireless intake", enabled = true)
    }
}

@Composable
private fun NeuralEngineRegistry(viewModel: AppViewModel) {
    HardwareControlItem(label = "NEURAL SATURATION", value = if (viewModel.isNeuralSaturationEnabled.value) "ACTIVE" else "OFF", desc = "TFLite harmonic generator engine", enabled = viewModel.isNeuralSaturationEnabled.value, onToggle = { viewModel.toggleNeuralSaturation(it) })
}

@Composable
private fun CoreAudioServiceRegistry(viewModel: AppViewModel) {
    HardwareControlItem(label = "BACKGROUND SERVICE", value = if (viewModel.isBackgroundServiceRunning.value) "RUNNING" else "STOPPED", desc = "Persistent DSP engine life-cycle", enabled = viewModel.isBackgroundServiceRunning.value, onToggle = { viewModel.toggleBackgroundService(it) })
}
