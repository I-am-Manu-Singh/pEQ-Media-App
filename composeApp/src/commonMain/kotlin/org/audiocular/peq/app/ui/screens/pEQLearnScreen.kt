package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.data.pEQMediaItem
import androidx.compose.material3.ExperimentalMaterial3Api
import org.audiocular.peq.app.ui.components.ChordVisualizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun pEQLearnScreen(viewModel: AppViewModel) {
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
            Text("pEQ NEURAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("ACADEMY", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }

        // --- SPECTRAL ANALYSIS OVERLAY ---
        SpectralFluxOverlay(viewModel, primaryColor)

        // --- RHYTHM LAB & SEQUENCER ---
        RhythmLabSection(viewModel, primaryColor)

        // --- PERFORMANCE ACADEMY & TUNER ---
        PerformanceAcademySection(viewModel, primaryColor)

        // --- NEURAL STEM PIPELINE ---
        NeuralStemPipeline(viewModel, primaryColor)

        // --- INDUSTRIAL DIAGNOSTICS & METERS ---
        IndustrialDiagnosticsSection(viewModel, primaryColor)

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SpectralAnalysisSection(viewModel: AppViewModel, primaryColor: Color) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.2f))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val data = viewModel.rtaData.value
                if (data.isNotEmpty()) {
                    val barWidth = size.width / data.size
                    data.forEachIndexed { i, value ->
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, Color.Transparent)
                            ),
                            topLeft = androidx.compose.ui.geometry.Offset(i * barWidth, size.height * (1f - value)),
                            size = androidx.compose.ui.geometry.Size(barWidth - 2f, size.height * value),
                            alpha = 0.4f
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Text("SPECTRAL FLUX", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                Text("${viewModel.detectedPitch.value.toInt()} Hz", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
}

@Composable
private fun StepSequencerSection(viewModel: AppViewModel, primaryColor: Color) {
    Column {
        Text("16-Step industrial drum sequencer. Synchronized to master clock with pattern overlays.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { if (viewModel.isSequencerActive.value) viewModel.stopSequencer() else viewModel.startSequencer() },
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isSequencerActive.value) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (viewModel.isSequencerActive.value) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (viewModel.isSequencerActive.value) "STOP LOOP" else "START LOOP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            
            OutlinedButton(
                onClick = { viewModel.quantizeRecordedBeats() },
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("QUANTIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        val drumTypes = listOf("KICK", "SNARE", "HIHAT", "CLAP")
        drumTypes.forEach { type ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(type, modifier = Modifier.width(50.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 16) {
                        val isActive = viewModel.stepSequencer[i]?.contains(type) == true
                        val isCurrent = viewModel.currentStep.value == i
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isCurrent -> primaryColor
                                        isActive -> primaryColor.copy(alpha = 0.5f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    }
                                )
                                .clickable { viewModel.toggleSequencerStep(i, type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FingerdrummingSection(viewModel: AppViewModel, primaryColor: Color) {
    Column {
        Text("Master the grid. Choose a neural kit and follow the guidance glow.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        // Kit Selector
        Text("SELECT NEURAL KIT", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryColor.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(viewModel.drumKits) { kit ->
                val isSelected = viewModel.selectedDrumKit.value == kit
                Surface(
                    onClick = { viewModel.switchDrumKit(kit) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(kit.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Pattern Selector
        Text("TRAINING MODULES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryColor.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.drumTutorials) { pattern ->
                val isSelected = viewModel.selectedTutorial.value == pattern
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.startTutorial(pattern) },
                    label = { Text(pattern.name, fontSize = 10.sp) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                        selectedLabelColor = primaryColor
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // 4x4 Pad Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DRUM GRID", fontSize = 9.sp, fontWeight = FontWeight.Black, color = primaryColor.copy(alpha = 0.7f))
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = { viewModel.toggleDrumRecording() }, modifier = Modifier.size(24.dp)) {
                    Icon(if (viewModel.isRecordingDrumSession.value) Icons.Default.FiberManualRecord else Icons.Default.RadioButtonUnchecked, null, modifier = Modifier.size(16.dp), tint = if (viewModel.isRecordingDrumSession.value) Color.Red else primaryColor)
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MASTER", fontSize = 8.sp, color = Color.Gray)
                Slider(
                    value = viewModel.drumKitVolume.value,
                    onValueChange = { viewModel.drumKitVolume.value = it },
                    modifier = Modifier.width(60.dp).height(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { viewModel.startKitBuilding() }) {
                    Icon(Icons.Default.Tune, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CUSTOMIZE KIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("REVERB", fontSize = 7.sp, color = Color.Gray)
                Slider(value = viewModel.drumReverb.value, onValueChange = { viewModel.drumReverb.value = it }, modifier = Modifier.height(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("DELAY", fontSize = 7.sp, color = Color.Gray)
                Slider(value = viewModel.drumDelay.value, onValueChange = { viewModel.drumDelay.value = it }, modifier = Modifier.height(20.dp))
            }
        }
        
        // Drum Section Labels
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("KICKS", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFE91E63).copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("SNARES", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFC107).copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("PERC", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF9C27B0).copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("LEAD", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50).copy(alpha = 0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        
        Spacer(Modifier.height(8.dp))
        
        var activePad by remember { mutableStateOf(-1) }
        Box {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 3 downTo 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (col in 0 until 4) {
                            val index = row * 4 + col
                            val label = viewModel.drumPadLabels.getOrNull(index) ?: "PAD"
                            val currentStep = viewModel.selectedTutorial.value?.steps?.getOrNull(viewModel.guideStep.value)
                            val isGuided = currentStep?.contains(label.substringAfter("-")) == true
                            val isTriggered = viewModel.sequencerTriggeredPads[label] == true
                            
                            DrumPadItem(
                                label = label,
                                index = index,
                                primaryColor = primaryColor,
                                isGuided = isGuided || isTriggered,
                                isActive = activePad == index,
                                onClick = { 
                                    if (viewModel.isKitBuilderActive.value) {
                                        viewModel.editingPadIndex.value = index
                                    } else {
                                        activePad = index
                                        viewModel.audioEngine.playDrumSample(label, index)
                                    }
                                },
                                onLongClick = { viewModel.editingPadIndex.value = index; viewModel.isKitBuilderActive.value = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Kit Builder Overlay
            if (viewModel.isKitBuilderActive.value) {
                KitBuilderOverlay(viewModel, primaryColor)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Stats
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LATENCY: ${viewModel.audioEngine.getProcessingLatencyMs()}ms", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("SAMPLES: ${viewModel.selectedDrumKit.value.labels.size}/16", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}

@Composable
private fun StemSplitterSection(viewModel: AppViewModel, primaryColor: Color) {
    Column {
        Text("Asynchronous decoupling via ONNX Runtime + FFmpeg Chunk Pipeline.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { viewModel.onPickTrackRequested?.invoke { item: pEQMediaItem -> viewModel.enqueueSplitJob(item) } },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.DeveloperBoard, null)
            Spacer(Modifier.width(12.dp))
            Text("INITIATE JNI-ACCELERATED DECOUPLING", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        
        if (viewModel.activeJob.value != null) {
            Spacer(Modifier.height(16.dp))
            // Progress placeholder
            LinearProgressIndicator(
                progress = viewModel.activeJob.value!!.progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = primaryColor
            )
        }
    }
}

@Composable
private fun IndustrialDiagnosticsSection(viewModel: AppViewModel, primaryColor: Color) {
    LearnSectionCard("INDUSTRIAL DIAGNOSTICS", icon = Icons.Default.MonitorHeart, info = "Real-time hardware telemetry, latency monitoring, and phase analysis.") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Phase Correlation Meter
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PHASE CORRELATION", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                    Text(if (viewModel.audioEngine.getPhaseCorrelation() >= 0) "IN PHASE" else "OUT OF PHASE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (viewModel.audioEngine.getPhaseCorrelation() + 1f) / 2f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (viewModel.audioEngine.getPhaseCorrelation() >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }

            // Real-time Analytics
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DiagnosticMiniCard("JITTER", "${viewModel.audioEngine.getJitterBufferMs()}ms", modifier = Modifier.weight(1f))
                DiagnosticMiniCard("DROPOUTS", "${viewModel.audioEngine.getAudioDropouts()}", modifier = Modifier.weight(1f))
                DiagnosticMiniCard("LATENCY", "${viewModel.audioEngine.getProcessingLatencyMs()}ms", modifier = Modifier.weight(1f))
            }

            // Bitstream Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("BITSTREAM PROTOCOL", fontSize = 8.sp, fontWeight = FontWeight.Black, color = primaryColor)
                    Spacer(Modifier.height(4.dp))
                    Text("${viewModel.audioEngine.getCodec().uppercase()} | ${viewModel.audioEngine.getSampleRate()}Hz | ${viewModel.audioEngine.getBitDepth()}-BIT | ${viewModel.audioEngine.getBitrate() / 1000}kbps", fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun RhythmLabSection(viewModel: AppViewModel, primaryColor: Color) {
    LearnSectionCard("RHYTHM LAB & SEQUENCER", icon = Icons.Default.SlowMotionVideo, info = "16-step rhythmic sequencer and tap tempo engine.") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepSequencerSection(viewModel, primaryColor)
            
            Button(onClick = { viewModel.tapTempo() }, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("TAP TEMPO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PerformanceAcademySection(viewModel: AppViewModel, primaryColor: Color) {
    LearnSectionCard("PERFORMANCE ACADEMY", icon = Icons.Default.MusicNote, info = "Fingerdrumming practice, strobe tuner, and rhythmic training.") {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            FingerdrummingSection(viewModel, primaryColor)
            
            Divider(color = primaryColor.copy(alpha = 0.1f))
            
            Text("PRECISION STROBE TUNER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
            StrobeTunerSection(viewModel, primaryColor)
        }
    }
}

@Composable
private fun StrobeTunerSection(viewModel: AppViewModel, primaryColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = viewModel.detectedNote.value.ifBlank { "--" },
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = if (viewModel.centsDeviation.value in -5f..5f) Color(0xFF4CAF50) else primaryColor
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Strobe Wheel Simulation
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cents = viewModel.centsDeviation.value
                rotate(cents * 3.6f) {
                    for (i in 0 until 12) {
                        rotate(i * 30f) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.4f),
                                start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width / 2, 10.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }
            Text("${viewModel.centsDeviation.value.toInt()}¢", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NeuralStemPipeline(viewModel: AppViewModel, primaryColor: Color) {
    LearnSectionCard("NEURAL STEM PIPELINE", icon = Icons.Default.AutoFixHigh, info = "AI-powered source separation for track deconstruction.") {
        StemSplitterSection(viewModel, primaryColor)
    }
}

@Composable
private fun SpectralFluxOverlay(viewModel: AppViewModel, primaryColor: Color) {
    LearnSectionCard("SPECTRAL ANALYSIS OVERLAY", icon = Icons.Default.Visibility, info = "Neural chord detection and spectral frequency flux.") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ChordVisualizer(chord = viewModel.detectedChord.value, modifier = Modifier.fillMaxWidth())
            SpectralAnalysisSection(viewModel, primaryColor)
        }
    }
}

@Composable
private fun KitBuilderOverlay(viewModel: AppViewModel, primaryColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .clickable(enabled = false) { }
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("KIT BUILDER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                    Text(
                        if (viewModel.editingPadIndex.value == -1) "SELECT A PAD TO REMAP" 
                        else "REMAPPING PAD ${viewModel.editingPadIndex.value + 1}", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { viewModel.isKitBuilderActive.value = false; viewModel.editingPadIndex.value = -1 }) {
                    Icon(Icons.Default.Close, null)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (viewModel.editingPadIndex.value != -1) {
                Text("BROWSE SAMPLES", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                
                val categories = viewModel.sampleLibrary.keys.toList()
                var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontSize = 9.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val samples = viewModel.sampleLibrary[selectedCategory] ?: emptyList()
                    items(samples) { label ->
                        val padIndex = viewModel.editingPadIndex.value
                        val currentVolume = viewModel.drumPadVolumes[padIndex] ?: 1f
                        
                        Surface(
                            onClick = { 
                                viewModel.remapPad(padIndex, label)
                                viewModel.audioEngine.playDrumSample(label, -1)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewModel.drumPadLabels[padIndex] == label) primaryColor.copy(alpha = 0.1f) else Color.Transparent
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    if (viewModel.drumPadLabels[padIndex] == label) {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = primaryColor)
                                    }
                                }
                                if (viewModel.drumPadLabels[padIndex] == label) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                                        Slider(
                                            value = currentVolume,
                                            onValueChange = { viewModel.updateDrumPadVolume(padIndex, it) },
                                            modifier = Modifier.weight(1f).height(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.isKitBuilderActive.value = false; viewModel.editingPadIndex.value = -1 },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Tap any pad in the grid behind to start remapping.", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun LearnSectionCard(
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

@Composable
private fun DrumPadItem(
    label: String,
    index: Int,
    primaryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isGuided: Boolean = false,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cleanLabel = if (label.contains("-")) label.substringAfter("-").uppercase() else label.uppercase()
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    
    // Color Coding Logic
    val padColor = when {
        cleanLabel.contains("KICK") || cleanLabel.contains("BD") -> Color(0xFFE91E63)
        cleanLabel.contains("SNARE") || cleanLabel.contains("SD") || cleanLabel.contains("RIM") -> Color(0xFFFFC107)
        cleanLabel.contains("HAT") || cleanLabel.contains("CH") || cleanLabel.contains("OH") || cleanLabel.contains("CYM") -> Color(0xFF00BCD4)
        cleanLabel.contains("PERC") || cleanLabel.contains("TOM") || cleanLabel.contains("CLAP") -> Color(0xFF9C27B0)
        else -> Color(0xFF4CAF50) // Leads / Vocals / Others
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed -> padColor
            isActive -> padColor.copy(alpha = 0.8f)
            isGuided -> padColor.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        },
        animationSpec = if (isPressed) snap() else (tween(400))
    )

    // Motion & Pulse
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = if (isPressed) snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onPress = {
                        isPressed = true
                        onClick()
                        try {
                            awaitRelease()
                        } finally {
                            // Keep the flash for a minimum duration for visual impact
                            scope.launch {
                                delay(80)
                                isPressed = false
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        tonalElevation = if (isPressed) 0.dp else 8.dp,
        border = BorderStroke(
            if (isPressed || isGuided || isActive) 2.dp else 1.dp,
            if (isPressed || isGuided || isActive) padColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Neural Glow / Inner Shadow Simulation
            if (isPressed || isGuided || isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(padColor.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = cleanLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isPressed || isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
                
                if (!isPressed && !isActive) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp, 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(padColor.copy(alpha = 0.5f))
                    )
                }
            }
            
            // Channel Info
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = (if (isPressed || isActive) Color.White else Color.Gray).copy(alpha = 0.3f)
            )
        }
    }
}
