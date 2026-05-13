package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import org.audiocular.peq.app.ui.components.ChordVisualizer
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TuningScreen(viewModel: AppViewModel) {
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
            Text("TUNING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
            Text("AUDIO SUITE", fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        }

        // --- NEURAL SIGNAL TOPOLOGY ---
        SignalTopologySection(viewModel, primaryColor)

        // --- TUNING & MASTER CLOCK ---
        TuningMasterSection(viewModel, primaryColor)

        // --- DSP MASTERING CHAIN ---
        MasteringChainSection(viewModel, primaryColor)



        // --- AUTO-EQ PROFILES ---
        AutoEqSection(viewModel, primaryColor)

        Spacer(Modifier.height(80.dp))
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TuningMasterSection(viewModel: AppViewModel, primaryColor: Color) {
    TuningSectionCard("TUNING & MASTER CLOCK", Icons.Default.SettingsInputComponent, info = "Scale mapping and capo controls.") {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            TuningScaleSection(viewModel, primaryColor)
        }
    }
}

@Composable
private fun MasteringChainSection(viewModel: AppViewModel, primaryColor: Color) {
    TuningSectionCard("DSP MASTERING CHAIN", Icons.Default.AutoFixHigh, info = "10-band Parametric EQ, Convolution IR, and Dynamics.") {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            ParametricEqSection(viewModel, primaryColor)
            AdvancedMasteringSection(viewModel, primaryColor)
            ConvolutionDynamicsSection(viewModel, primaryColor)
        }
    }
}



@Composable
private fun TuningScaleSection(viewModel: AppViewModel, primaryColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("TRANSPOSE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.transposeSemiTones.value-- }) { Icon(Icons.Default.Remove, null) }
                Text("${viewModel.transposeSemiTones.value}", fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.transposeSemiTones.value++ }) { Icon(Icons.Default.Add, null) }
            }
        }
        
        Column {
            Text("STEP SEQUENCER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(16) { step ->
                    val isActive = viewModel.sequencerSteps[step].value
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isActive) primaryColor else if (step % 4 == 0) primaryColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                                RoundedCornerShape(4.dp)
                            )
                            .border(1.dp, if (isActive) primaryColor else primaryColor.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleSequencerStep(step) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text((step + 1).toString(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        
        Column {
            Text("ACTIVE SCALE ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
            Spacer(Modifier.height(8.dp))
            val scales = listOf("C Major", "G Major", "D Major", "A Minor", "E Minor")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(scales) { scale ->
                    FilterChip(
                        selected = viewModel.selectedScale.value == scale,
                        onClick = { viewModel.selectedScale.value = scale },
                        label = { Text(text = scale, fontSize = 10.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConvolutionDynamicsSection(viewModel: AppViewModel, primaryColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Convolution Engine
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("CONVOLUTION REVERB (IR)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                Switch(checked = viewModel.isConvolverActive.value, onCheckedChange = { viewModel.isConvolverActive.value = it })
            }
            Text(viewModel.convolverIrName.value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = viewModel.convolverIrDryWet.value,
                onValueChange = { viewModel.convolverIrDryWet.value = it },
                valueRange = 0f..1f
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DRY", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("WET", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.importIr() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("IMPORT CUSTOM IR (.WAV)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Dynamics
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("COMP RATIO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                Text("${viewModel.compressorRatio.value.toInt()}:1", fontWeight = FontWeight.Bold)
                Slider(value = viewModel.compressorRatio.value, onValueChange = { viewModel.compressorRatio.value = it }, valueRange = 1f..20f)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("STEREO WIDTH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
                Text("${(viewModel.stereoWidth.value * 100).toInt()}%", fontWeight = FontWeight.Bold)
                Slider(value = viewModel.stereoWidth.value, onValueChange = { viewModel.stereoWidth.value = it }, valueRange = 0f..2f)
            }
        }
        
        // Environment
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("NEURAL ENVIRONMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("AI room simulation & acoustics", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Switch(checked = viewModel.isNeuralEnvActive.value, onCheckedChange = { viewModel.isNeuralEnvActive.value = it })
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedMasteringSection(viewModel: AppViewModel, primaryColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Neural Mastering Presets
        Column {
            Text("NEURAL MASTERING PRESET", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
            Spacer(Modifier.height(10.dp))
            val presets = listOf("FLAT", "STUDIO", "WARM", "AIRY", "CINEMATIC")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = false, // Add state to VM if needed
                        onClick = { viewModel.audioEngine.applyMasteringChain(preset) },
                        label = { Text(preset, fontSize = 10.sp) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Neural Toggles
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DspToggleItem(
                label = "VOCAL REMOVAL", 
                active = viewModel.isVocalRemovalActive.value, 
                onToggle = { viewModel.toggleVocalRemoval() },
                modifier = Modifier.weight(1f)
            )
            DspToggleItem(
                label = "NEURAL SAT", 
                active = viewModel.isNeuralSaturationEnabled.value, 
                onToggle = { viewModel.toggleNeuralSaturation(it) },
                modifier = Modifier.weight(1f)
            )
        }

        // Advanced Sliders
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DspSliderItem("NEURAL CLARITY+", 0f..1f, 0.5f, primaryColor) { viewModel.audioEngine.setClarityLevel(it) }
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    DspSliderItem("DEEP BASS ENGINE", 0f..1f, 0.5f, primaryColor) { viewModel.audioEngine.setDeepBassLevel(it) }
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SUB-GEN", fontSize = 8.sp, fontWeight = FontWeight.Black, color = primaryColor)
                    Switch(
                        checked = viewModel.isSubHarmonicActive.value,
                        onCheckedChange = { viewModel.toggleSubHarmonicGenerator(it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }

            DspSliderItem("COMP THRESHOLD", -60f..0f, viewModel.compressorThreshold.floatValue, primaryColor) { 
                viewModel.setCompressorParams(it, viewModel.compressorRatio.value, viewModel.compressorAttackMs.floatValue, viewModel.compressorReleaseMs.floatValue) 
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    DspSliderItem("ATTACK (ms)", 1f..100f, viewModel.compressorAttackMs.floatValue, primaryColor) { 
                        viewModel.setCompressorParams(viewModel.compressorThreshold.floatValue, viewModel.compressorRatio.value, it, viewModel.compressorReleaseMs.floatValue) 
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    DspSliderItem("RELEASE (ms)", 10f..1000f, viewModel.compressorReleaseMs.floatValue, primaryColor) { 
                        viewModel.setCompressorParams(viewModel.compressorThreshold.floatValue, viewModel.compressorRatio.value, viewModel.compressorAttackMs.floatValue, it) 
                    }
                }
            }
        }

        // Global Limiter
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("BRICKWALL LIMITER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Prevent digital clipping (0dB Peak)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Switch(
                checked = true, // Default to true for safety
                onCheckedChange = { viewModel.audioEngine.setLimiterEnabled(it) }
            )
        }
    }
}

@Composable
private fun DspToggleItem(label: String, active: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = { onToggle(!active) },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(if (active) "ACTIVE" else "BYPASS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DspSliderItem(label: String, range: ClosedFloatingPointRange<Float>, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    var localValue by remember { mutableFloatStateOf(value) }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color)
            Text("${localValue.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it; onValueChange(it) },
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
private fun pEQFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            label()
        }
    }
}

@Composable
private fun ParametricEqSection(viewModel: AppViewModel, primaryColor: Color) {
    val bands = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("10-BAND PRECISION KERNEL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = primaryColor)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bands.forEachIndexed { index, freq ->
                EqBandSlider(
                    label = freq,
                    value = viewModel.eqGains[index], 
                    qValue = viewModel.eqQs[index],
                    onValueChange = { viewModel.updateEqBand(index, it) },
                    onQChange = { viewModel.updateEqQ(index, it) },
                    modifier = Modifier.weight(1f),
                    primaryColor = primaryColor
                )
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { /* Reset EQ */ }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                Text("RESET FLAT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(onClick = { /* Auto-Optimize */ }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                Text("AUTO-OPTIMIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

 @Composable
private fun EqBandSlider(
    label: String, 
    value: Float, 
    qValue: Float,
    onValueChange: (Float) -> Unit, 
    onQChange: (Float) -> Unit,
    modifier: Modifier = Modifier, 
    primaryColor: Color
) {
    var localValue by remember { mutableFloatStateOf(value) }
    var localQ by remember { mutableFloatStateOf(qValue) }
    
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Q: ${localQ.toInt()}", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = primaryColor.copy(alpha = 0.6f))
        Slider(
            value = localQ,
            onValueChange = { localQ = it; onQChange(it) },
            valueRange = 0.1f..10f,
            modifier = Modifier.height(20.dp)
        )
        
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Slider(
                value = localValue,
                onValueChange = { localValue = it; onValueChange(it) },
                valueRange = -12f..12f,
                modifier = Modifier
                    .graphicsLayer { rotationZ = -90f }
                    .requiredWidth(320.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun AutoEqSection(viewModel: AppViewModel, primaryColor: Color) {
    var searchQuery by remember { mutableStateOf("") }
    
    TuningSectionCard("AUTO-EQ REPOSITORY", Icons.Default.CloudDownload, info = "Access 5,000+ headphone response curves.") {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.searchAutoEqRepository(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search 5,000+ Headphone Profiles...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(16.dp)
            )

            if (viewModel.isAutoEqLoading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }

            if (viewModel.autoEqSearchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(viewModel.autoEqSearchResults) { profile ->
                        Surface(
                            onClick = { viewModel.applyAutoEqProfile(profile) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewModel.selectedAutoEqProfile.value == profile) primaryColor.copy(alpha = 0.1f) else Color.Transparent
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Headset, null, modifier = Modifier.size(16.dp), tint = primaryColor)
                                Spacer(Modifier.width(12.dp))
                                Text(profile.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            viewModel.selectedAutoEqProfile.value?.let { active ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ACTIVE PROFILE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = primaryColor)
                            Text(active.name, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                        IconButton(onClick = { viewModel.selectedAutoEqProfile.value = null }) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TuningSectionCard(
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
private fun SignalTopologySection(viewModel: AppViewModel, primaryColor: Color) {
    TuningSectionCard("SIGNAL TOPOLOGY", icon = Icons.Default.Schema, info = "Real-time DSP path visualization.") {
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopologyNode("IN", true)
            TopologyLine(true)
            TopologyNode("EQ", true)
            TopologyLine(viewModel.isConvolverActive.value)
            TopologyNode("IR", viewModel.isConvolverActive.value)
            TopologyLine(true)
            TopologyNode("DYN", true)
            TopologyLine(true)
            TopologyNode("OUT", true)
        }
    }
}

@Composable
private fun TopologyNode(label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopologyLine(active: Boolean) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(2.dp)
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
    )
}
