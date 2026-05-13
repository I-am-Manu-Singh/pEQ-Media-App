package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.audiocular.peq.app.ui.components.ChordVisualizer
import org.audiocular.peq.app.ui.components.NowPlayingTelemetryCard
import org.audiocular.peq.app.ui.viewmodel.*

@Composable
fun pEQLearnScreen(
    viewModel: AppViewModel
) {

    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),

        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        ),

        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

        item {

            Column {

                Text(
                    text = "LEARN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "RHYTHM • TRAINING • ANALYSIS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        item {
            NowPlayingTelemetryCard(
                viewModel,
                primaryColor
            )
        }

        item {
            SpectralFluxOverlay(
                viewModel,
                primaryColor
            )
        }

        item {
            RhythmLabSection(
                viewModel,
                primaryColor
            )
        }

        item {
            PerformanceAcademySection(
                viewModel,
                primaryColor
            )
        }

        item {
            TuningMasterSection(
                viewModel,
                primaryColor
            )
        }


        item {
            Spacer(Modifier.height(42.dp))
        }
    }
}

/* ---------------------------------------------------------- */
/* SECTION CARD */
/* ---------------------------------------------------------- */

@Composable
private fun LearnSectionCard(
    title: String,
    icon: ImageVector,
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    var showInfo by remember {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(28.dp),

        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),

        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        ),

        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),

                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        icon,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "NEURAL PERFORMANCE MODULE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }

                if (info != null) {

                    IconButton(
                        onClick = {
                            showInfo = !showInfo
                        },

                        modifier = Modifier.size(28.dp)
                    ) {

                        Icon(
                            Icons.Default.Info,
                            null,

                            tint = MaterialTheme.colorScheme
                                .onSurface
                                .copy(alpha = 0.45f),

                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(showInfo) {

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),

                    shape = RoundedCornerShape(14.dp),

                    color = MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.08f)
                ) {

                    Text(
                        text = info.orEmpty(),

                        modifier = Modifier.padding(12.dp),

                        fontSize = 10.sp,

                        lineHeight = 16.sp,

                        fontWeight = FontWeight.Medium,

                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            content()
        }
    }
}

/* ---------------------------------------------------------- */
/* SPECTRAL */
/* ---------------------------------------------------------- */

@Composable
private fun SpectralFluxOverlay(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    LearnSectionCard(
        title = "SPECTRAL ANALYSIS",
        icon = Icons.Default.GraphicEq,
        info = "Real-time frequency response and harmonic analysis."
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            ChordVisualizer(
                chord = viewModel.detectedChord.value,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        primaryColor.copy(alpha = 0.08f),
                        RoundedCornerShape(22.dp)
                    )
            ) {

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    val data = viewModel.rtaData.value

                    if (data.isNotEmpty()) {

                        val width = size.width / data.size

                        data.forEachIndexed { i, value ->

                            drawRoundRect(

                                brush = Brush.verticalGradient(
                                    listOf(
                                        primaryColor,
                                        primaryColor.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                ),

                                topLeft =
                                    androidx.compose.ui.geometry.Offset(
                                        i * width,
                                        size.height * (1f - value)
                                    ),

                                size =
                                    androidx.compose.ui.geometry.Size(
                                        width - 3f,
                                        size.height * value
                                    ),

                                cornerRadius =
                                    androidx.compose.ui.geometry.CornerRadius(
                                        8f,
                                        8f
                                    ),

                                alpha = 0.8f
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {

                    Text(
                        text = "LIVE FREQUENCY FLUX",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryColor
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${viewModel.detectedPitch.value.toInt()} Hz",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------- */
/* RHYTHM LAB */
/* ---------------------------------------------------------- */

@Composable
private fun RhythmLabSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    LearnSectionCard(
        title = "RHYTHM LAB",
        icon = Icons.Default.SlowMotionVideo,
        info = "Step sequencing and rhythm synchronization."
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        if (viewModel.isSequencerActive.value)
                            viewModel.stopSequencer()
                        else
                            viewModel.startSequencer()
                    },

                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),

                    shape = RoundedCornerShape(16.dp)
                ) {

                    Icon(
                        if (viewModel.isSequencerActive.value)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        if (viewModel.isSequencerActive.value)
                            "STOP"
                        else
                            "START",

                        fontWeight = FontWeight.Black
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.tapTempo()
                    },

                    modifier = Modifier.height(52.dp),

                    shape = RoundedCornerShape(16.dp)
                ) {

                    Icon(Icons.Default.TouchApp, null)

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "TAP",
                        fontWeight = FontWeight.Black
                    )
                }
            }

            LazyRow(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                items(16) { step ->

                    val active =
                        viewModel.sequencerSteps[step].value

                    val current =
                        viewModel.currentStep.value == step

                    Box(

                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {

                                    current ->
                                        primaryColor

                                    active ->
                                        primaryColor.copy(alpha = 0.45f)

                                    else ->
                                        MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                1.dp,
                                if (current)
                                    primaryColor
                                else
                                    Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.toggleSequencerStep(step)
                            },

                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "${step + 1}",

                            fontSize = 9.sp,

                            fontWeight = FontWeight.Black,

                            color =
                                if (current)
                                    Color.Black
                                else
                                    MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------- */
/* PERFORMANCE */
/* ---------------------------------------------------------- */
@Composable
private fun PerformanceAcademySection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    LearnSectionCard(
        title = "PERFORMANCE ACADEMY",
        icon = Icons.Default.Piano,
        info = "Finger drumming and live response training."
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            /*
             * KIT SELECTOR
             */

            Column {

                Text(
                    text = "DRUM KITS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(viewModel.drumKits) { kit ->

                        val selected =
                            viewModel.selectedDrumKit.value == kit

                        Surface(

                            onClick = {
                                viewModel.switchDrumKit(kit)
                            },

                            shape = RoundedCornerShape(14.dp),

                            color =
                                if (selected)
                                    primaryColor.copy(alpha = 0.12f)
                                else
                                    MaterialTheme.colorScheme.surface,

                            border = BorderStroke(
                                1.dp,

                                if (selected)
                                    primaryColor.copy(alpha = 0.22f)
                                else
                                    MaterialTheme.colorScheme.outline
                                        .copy(alpha = 0.08f)
                            ),

                            tonalElevation = 2.dp
                        ) {

                            Row(

                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                                ),

                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected)
                                                primaryColor
                                            else
                                                MaterialTheme.colorScheme
                                                    .onSurface
                                                    .copy(alpha = 0.15f)
                                        )
                                )

                                Spacer(Modifier.width(10.dp))

                                Column {

                                    Text(
                                        text = kit.name.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color =
                                            if (selected)
                                                primaryColor
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = "${kit.labels.size} PADS",
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurface
                                                .copy(alpha = 0.42f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            /*
             * TRAINING MODULES
             */

            Column {

                Text(
                    text = "TRAINING MODULES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(viewModel.drumTutorials) { pattern ->

                        val selected =
                            viewModel.selectedTutorial.value == pattern

                        FilterChip(

                            selected = selected,

                            onClick = {
                                viewModel.startTutorial(pattern)
                            },

                            label = {

                                Text(
                                    text = pattern.name.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black
                                )
                            },

                            shape = RoundedCornerShape(16.dp),

                            colors =
                                FilterChipDefaults.filterChipColors(

                                    selectedContainerColor =
                                        primaryColor.copy(alpha = 0.14f),

                                    selectedLabelColor =
                                        primaryColor
                                )
                        )
                    }
                }
            }

            /*
             * PERFORMANCE TOOLBAR
             */

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(18.dp),

                color =
                    MaterialTheme.colorScheme.surface,

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
                ),

                tonalElevation = 2.dp
            ) {

                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "LIVE RECORD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryColor
                            )

                            Spacer(Modifier.width(8.dp))

                            IconButton(

                                onClick = {
                                    viewModel.toggleDrumRecording()
                                },

                                modifier = Modifier.size(28.dp)
                            ) {

                                Icon(

                                    if (
                                        viewModel.isRecordingDrumSession.value
                                    )
                                        Icons.Default.FiberManualRecord
                                    else
                                        Icons.Default.RadioButtonUnchecked,

                                    null,

                                    tint =
                                        if (
                                            viewModel.isRecordingDrumSession.value
                                        )
                                            Color.Red
                                        else
                                            primaryColor,

                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                viewModel.startKitBuilding()
                            }
                        ) {

                            Icon(
                                Icons.Default.Tune,
                                null,
                                modifier = Modifier.size(15.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                "KIT BUILDER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(14.dp)
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "REVERB",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface
                                        .copy(alpha = 0.5f)
                            )

                            Slider(
                                value = viewModel.drumReverb.value,
                                onValueChange = {
                                    viewModel.drumReverb.value = it
                                }
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = "DELAY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface
                                        .copy(alpha = 0.5f)
                            )

                            Slider(
                                value = viewModel.drumDelay.value,
                                onValueChange = {
                                    viewModel.drumDelay.value = it
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "MASTER",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.5f)
                        )

                        Spacer(Modifier.width(10.dp))

                        Slider(
                            value = viewModel.drumKitVolume.value,
                            onValueChange = {
                                viewModel.drumKitVolume.value = it
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            /*
             * DRUM RACK LABELS
             */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                DrumRackLabel(
                    "KICKS",
                    Color(0xFFFF4D6D),
                    Modifier.weight(1f)
                )

                DrumRackLabel(
                    "SNARES",
                    Color(0xFFFFC107),
                    Modifier.weight(1f)
                )

                DrumRackLabel(
                    "PERC",
                    Color(0xFFB388FF),
                    Modifier.weight(1f)
                )

                DrumRackLabel(
                    "LEADS",
                    Color(0xFF4ADE80),
                    Modifier.weight(1f)
                )
            }

            /*
             * DRUM GRID
             */

            val pads = viewModel.drumPadLabels

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(24.dp),

                color = MaterialTheme.colorScheme.surface,

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
                ),

                tonalElevation = 3.dp
            ) {

                Box(
                    modifier = Modifier.padding(10.dp)
                ) {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {

                        for (row in 0 until 4) {

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                for (col in 0 until 4) {

                                    val index =
                                        row * 4 + col

                                    val label =
                                        pads.getOrNull(index)
                                            ?: "PAD"

                                    val guided =
                                        viewModel.selectedTutorial.value
                                            ?.steps
                                            ?.getOrNull(
                                                viewModel.guideStep.value
                                            )
                                            ?.contains(
                                                label.substringAfter("-")
                                            ) == true

                                    val triggered =
                                        viewModel
                                            .sequencerTriggeredPads[label] == true

                                    DrumPadItem(
                                        label = label,
                                        index = index,
                                        primaryColor = primaryColor,
                                        modifier = Modifier.weight(1f),
                                        isGuided = guided || triggered,
                                        isActive = triggered,

                                        onClick = {

                                            viewModel.audioEngine
                                                .playDrumSample(
                                                    label,
                                                    index
                                                )
                                        },

                                        onLongClick = {

                                            viewModel.editingPadIndex.value =
                                                index

                                            viewModel.isKitBuilderActive.value =
                                                true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    /*
                     * KIT BUILDER OVERLAY
                     */

                    if (viewModel.isKitBuilderActive.value) {

                        KitBuilderOverlay(
                            viewModel,
                            primaryColor
                        )
                    }
                }
            }

            /*
             * LIVE STATS
             */

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                color = MaterialTheme.colorScheme.surface,

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.06f)
                )
            ) {

                Row(

                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(
                            text = "LATENCY",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.4f)
                        )

                        Text(
                            text =
                                "${viewModel.audioEngine.getProcessingLatencyMs()} ms",

                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "ACTIVE KIT",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.4f)
                        )

                        Text(
                            text =
                                viewModel.selectedDrumKit.value.name
                                    .uppercase(),

                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {

                        Text(
                            text = "SAMPLES",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.4f)
                        )

                        Text(
                            text =
                                "${viewModel.selectedDrumKit.value.labels.size}/16",

                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            HorizontalDivider(
                color = primaryColor.copy(alpha = 0.08f)
            )

            StrobeTunerSection(
                viewModel,
                primaryColor
            )
        }
    }
}

@Composable
private fun DrumRackLabel(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier = modifier,

        shape = RoundedCornerShape(10.dp),

        color = color.copy(alpha = 0.10f),

        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.22f)
        )
    ) {

        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = title,

                fontSize = 8.sp,

                fontWeight = FontWeight.Black,

                letterSpacing = 1.sp,

                color = color
            )
        }
    }
}

/* ---------------------------------------------------------- */
/* DRUM PAD */
/* ---------------------------------------------------------- */

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

    val cleanLabel =
        label.substringAfter("-")
            .uppercase()

    val padColor =
        when {

            cleanLabel.contains("KICK") ||
                    cleanLabel.contains("BD") ->
                Color(0xFFFF4D6D)

            cleanLabel.contains("SNARE") ||
                    cleanLabel.contains("SD") ||
                    cleanLabel.contains("RIM") ->
                Color(0xFFFFC107)

            cleanLabel.contains("HAT") ||
                    cleanLabel.contains("HH") ||
                    cleanLabel.contains("CYM") ->
                Color(0xFF00D4FF)

            cleanLabel.contains("CLAP") ||
                    cleanLabel.contains("PERC") ||
                    cleanLabel.contains("TOM") ->
                Color(0xFFB388FF)

            cleanLabel.contains("BASS") ||
                    cleanLabel.contains("808") ->
                Color(0xFF536DFE)

            else ->
                Color(0xFF4ADE80)
        }

    val padIcon =
        when {

            cleanLabel.contains("KICK") ->
                Icons.Default.RadioButtonChecked

            cleanLabel.contains("SNARE") ->
                Icons.Default.GraphicEq

            cleanLabel.contains("HAT") ->
                Icons.Default.AutoAwesome

            cleanLabel.contains("CLAP") ->
                Icons.Default.Waves

            cleanLabel.contains("BASS") ->
                Icons.Default.Speaker

            else ->
                Icons.Default.MusicNote
        }

    val scope = rememberCoroutineScope()

    var pressed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(

        targetValue =
            when {

                pressed -> 0.92f
                isActive -> 1.03f
                else -> 1f
            },

        animationSpec =
            spring(
                dampingRatio =
                    Spring.DampingRatioMediumBouncy
            ),

        label = ""
    )

    val glowAlpha by animateFloatAsState(

        targetValue =
            when {

                pressed -> 0.55f
                isGuided -> 0.28f
                isActive -> 0.22f
                else -> 0.08f
            },

        animationSpec = tween(220),

        label = ""
    )

    Surface(

        modifier = modifier
            .aspectRatio(1f)
            .heightIn(min = 88.dp)
            .scale(scale)
            .pointerInput(Unit) {

                detectTapGestures(

                    onLongPress = {
                        onLongClick()
                    },

                    onPress = {

                        pressed = true

                        onClick()

                        try {

                            awaitRelease()

                        } finally {

                            scope.launch {

                                delay(70)

                                pressed = false
                            }
                        }
                    }
                )
            },

        shape = RoundedCornerShape(22.dp),

        color =
            MaterialTheme.colorScheme.surface
                .copy(alpha = 0.92f),

        border = BorderStroke(
            width =
                if (pressed || isActive)
                    2.dp
                else
                    1.dp,

            color =
                if (pressed || isActive)
                    padColor
                else
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
        ),

        tonalElevation =
            if (pressed) 1.dp else 6.dp,

        shadowElevation =
            if (pressed) 2.dp else 8.dp
    ) {

        Box(
    modifier = Modifier.fillMaxSize()
) {

    /*
     * SOFT ACTIVE GLOW
     */

    if (pressed || isGuided || isActive) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            padColor.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
    }

    /*
     * PAD CONTENT
     */

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),

        verticalArrangement = Arrangement.SpaceBetween
    ) {

        /*
         * TOP BAR
         */

        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * PAD NUMBER
             */

            Surface(

                shape = RoundedCornerShape(6.dp),

                color = padColor.copy(alpha = 0.12f)
            ) {

                Text(

                    text =
                        (index + 1)
                            .toString()
                            .padStart(2, '0'),

                    modifier = Modifier.padding(
                        horizontal = 6.dp,
                        vertical = 2.dp
                    ),

                    fontSize = 7.sp,

                    fontWeight = FontWeight.Black,

                    color = padColor
                )
            }

            /*
             * SMALL SIDE ICON
             */

            Icon(
                imageVector = padIcon,
                contentDescription = null,

                tint =
                    if (pressed || isActive)
                        padColor
                    else
                        padColor.copy(alpha = 0.55f),

                modifier = Modifier.size(14.dp)
            )
        }

        /*
         * LABEL AREA
         */

        Column(
            modifier = Modifier.fillMaxWidth(),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            val displayLabel = remember(label) {

                label
                    .substringAfter("-")
                    .replace("_", " ")
                    .replace("-", " ")
                    .uppercase()
            }

            Text(
                text = displayLabel,

                modifier = Modifier.fillMaxWidth(),

                fontSize = 9.sp,

                lineHeight = 10.sp,

                fontWeight = FontWeight.Black,

                textAlign = TextAlign.Center,

                color = MaterialTheme.colorScheme.onSurface,

                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))

            /*
             * VELOCITY BAR
             */

            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        padColor.copy(
                            alpha =
                                if (pressed)
                                    1f
                                else
                                    0.40f
                        )
                    )
            )
        }
    }

    /*
     * PRESS OVERLAY
     */

    if (pressed) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.White.copy(alpha = 0.04f)
                )
        )
    }
}
    }
}

/* ---------------------------------------------------------- */
/* STROBE */
/* ---------------------------------------------------------- */

@Composable
private fun StrobeTunerSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    val cents =
        viewModel.centsDeviation.value

    val inTune =
        cents in -5..5

    val tunerColor =
        when {

            inTune ->
                Color(0xFF4CAF50)

            cents > 0 ->
                Color(0xFFFFB300)

            else ->
                Color(0xFF42A5F5)
        }

    val animatedRotation by animateFloatAsState(
        targetValue = cents * 3.6f,
        animationSpec = tween(180),
        label = ""
    )

    Surface(

        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(24.dp),

        color = MaterialTheme.colorScheme.surface,

        border = BorderStroke(
            1.dp,
            tunerColor.copy(alpha = 0.10f)
        ),

        tonalElevation = 2.dp
    ) {

        Column(

            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 20.dp
            ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            /*
             * HEADER
             */

            Text(
                text = "PRECISION STROBE TUNER",

                fontSize = 9.sp,

                fontWeight = FontWeight.Black,

                letterSpacing = 1.sp,

                color =
                    MaterialTheme.colorScheme
                        .onSurface
                        .copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(18.dp))

            /*
             * NOTE DISPLAY
             */

            Text(
                text =
                    viewModel.detectedNote.value
                        .ifBlank { "--" },

                fontSize = 52.sp,

                fontWeight = FontWeight.Black,

                color = tunerColor
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text =
                    if (inTune)
                        "IN TUNE"
                    else if (cents > 0)
                        "SHARP"
                    else
                        "FLAT",

                fontSize = 10.sp,

                fontWeight = FontWeight.Bold,

                letterSpacing = 1.sp,

                color =
                    tunerColor.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(22.dp))

            /*
             * STROBE DIAL
             */

            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {

                /*
                 * OUTER RING
                 */

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            2.dp,
                            tunerColor.copy(alpha = 0.12f),
                            CircleShape
                        )
                )

                /*
                 * STROBE CANVAS
                 */

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    rotate(animatedRotation) {

                        for (i in 0 until 48) {

                            rotate(i * 7.5f) {

                                val strong =
                                    i % 4 == 0

                                drawLine(

                                    color =
                                        tunerColor.copy(
                                            alpha =
                                                if (strong)
                                                    0.85f
                                                else
                                                    0.35f
                                        ),

                                    start =
                                        androidx.compose.ui.geometry.Offset(
                                            size.width / 2,
                                            12.dp.toPx()
                                        ),

                                    end =
                                        androidx.compose.ui.geometry.Offset(
                                            size.width / 2,
                                            28.dp.toPx()
                                        ),

                                    strokeWidth =
                                        if (strong)
                                            3.dp.toPx()
                                        else
                                            1.4.dp.toPx()
                                )
                            }
                        }
                    }
                }

                /*
                 * CENTER CORE
                 */

                Surface(

                    modifier = Modifier.size(74.dp),

                    shape = CircleShape,

                    color =
                        MaterialTheme.colorScheme.surface,

                    border = BorderStroke(
                        1.dp,
                        tunerColor.copy(alpha = 0.10f)
                    ),

                    tonalElevation = 4.dp
                ) {

                    Column(

                        modifier = Modifier.fillMaxSize(),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        Text(
                            text = "${cents.toInt()}¢",

                            fontSize = 16.sp,

                            fontWeight = FontWeight.Black,

                            color = tunerColor
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "OFFSET",

                            fontSize = 7.sp,

                            fontWeight = FontWeight.Black,

                            letterSpacing = 1.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            /*
             * DEVIATION BAR
             */

            Column {

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "-50",

                        fontSize = 8.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                                .copy(alpha = 0.35f)
                    )

                    Text(
                        text = "0",

                        fontSize = 8.sp,

                        fontWeight = FontWeight.Bold,

                        color = tunerColor
                    )

                    Text(
                        text = "+50",

                        fontSize = 8.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                                .copy(alpha = 0.35f)
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(

                    progress = {
                        ((cents + 50f) / 100f)
                            .coerceIn(0f, 1f)
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),

                    color = tunerColor,

                    trackColor =
                        MaterialTheme.colorScheme.surfaceVariant
                            .copy(alpha = 0.5f)
                )
            }
        }
    }
}
/* ---------------------------------------------------------- */
/* TUNING */
/* ---------------------------------------------------------- */

@Composable
private fun TuningMasterSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    LearnSectionCard(
        title = "TUNING",
        icon = Icons.Default.Tune,
        info = "Pitch transpose and scale mapping."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "TRANSPOSE",

                    modifier = Modifier.weight(1f),

                    fontSize = 10.sp,

                    fontWeight = FontWeight.Black,

                    color = primaryColor
                )

                FilledIconButton(
                    onClick = {
                        viewModel.transposeSemiTones.value--
                    }
                ) {

                    Icon(Icons.Default.Remove, null)
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text =
                        "${viewModel.transposeSemiTones.value}",

                    fontSize = 18.sp,

                    fontWeight = FontWeight.Black
                )

                Spacer(Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        viewModel.transposeSemiTones.value++
                    }
                ) {

                    Icon(Icons.Default.Add, null)
                }
            }
        }
    }
}

@Composable
private fun KitBuilderOverlay(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            )
            .border(
                1.dp,
                primaryColor.copy(alpha = 0.10f),
                RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
    ) {

        Column {

            /*
             * HEADER
             */

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column {

                    Text(
                        text = "KIT BUILDER",

                        fontSize = 10.sp,

                        fontWeight = FontWeight.Black,

                        color = primaryColor,

                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text =
                            if (viewModel.editingPadIndex.value == -1)
                                "SELECT A PAD TO REMAP"
                            else
                                "EDITING PAD ${
                                    viewModel.editingPadIndex.value + 1
                                }",

                        fontSize = 13.sp,

                        fontWeight = FontWeight.Black
                    )
                }

                IconButton(
                    onClick = {

                        viewModel.isKitBuilderActive.value = false
                        viewModel.editingPadIndex.value = -1
                    }
                ) {

                    Icon(
                        Icons.Default.Close,
                        null
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            /*
             * EMPTY STATE
             */

            if (viewModel.editingPadIndex.value == -1) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text =
                            "LONG PRESS ANY PAD TO REMAP",

                        fontSize = 12.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                                .copy(alpha = 0.45f)
                    )
                }

            } else {

                /*
                 * CATEGORY SELECTOR
                 */

                val categories =
                    viewModel.sampleLibrary.keys.toList()

                var selectedCategory by remember {

                    mutableStateOf(
                        categories.firstOrNull().orEmpty()
                    )
                }

                Text(
                    text = "SAMPLE LIBRARY",

                    fontSize = 8.sp,

                    fontWeight = FontWeight.Black,

                    color =
                        MaterialTheme.colorScheme
                            .onSurface
                            .copy(alpha = 0.45f)
                )

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(categories) { category ->

                        val selected =
                            selectedCategory == category

                        FilterChip(

                            selected = selected,

                            onClick = {
                                selectedCategory = category
                            },

                            label = {

                                Text(
                                    text = category.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },

                            shape = RoundedCornerShape(14.dp),

                            colors =
                                FilterChipDefaults
                                    .filterChipColors(

                                        selectedContainerColor =
                                            primaryColor.copy(alpha = 0.18f),

                                        selectedLabelColor =
                                            primaryColor
                                    )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                /*
                 * SAMPLE LIST
                 */

                val samples =
                    viewModel.sampleLibrary[selectedCategory]
                        ?: emptyList()

                LazyColumn(
                    modifier = Modifier.weight(1f),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(samples) { label ->

                        val padIndex =
                            viewModel.editingPadIndex.value

                        val selected =
                            viewModel.drumPadLabels[padIndex] == label

                        val currentVolume =
                            viewModel.drumPadVolumes[padIndex]
                                ?: 1f

                        Surface(

                            onClick = {

                                viewModel.remapPad(
                                    padIndex,
                                    label
                                )

                                viewModel.audioEngine
                                    .playDrumSample(
                                        label,
                                        -1
                                    )
                            },

                            shape = RoundedCornerShape(16.dp),

                            color =
                                if (selected)
                                    primaryColor.copy(alpha = 0.10f)
                                else
                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                        .copy(alpha = 0.16f),

                            border = BorderStroke(
                                1.dp,

                                if (selected)
                                    primaryColor.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme
                                        .outline
                                        .copy(alpha = 0.06f)
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = label.uppercase(),

                                        fontSize = 10.sp,

                                        fontWeight = FontWeight.Black
                                    )

                                    if (selected) {

                                        Icon(
                                            Icons.Default.Check,
                                            null,

                                            tint = primaryColor,

                                            modifier =
                                                Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (selected) {

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Icon(
                                            Icons.Default.VolumeUp,
                                            null,

                                            tint = Color.Gray,

                                            modifier =
                                                Modifier.size(14.dp)
                                        )

                                        Spacer(Modifier.width(8.dp))

                                        Slider(

                                            value = currentVolume,

                                            onValueChange = {

                                                viewModel
                                                    .updateDrumPadVolume(
                                                        padIndex,
                                                        it
                                                    )
                                            },

                                            modifier =
                                                Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(

                    onClick = {

                        viewModel.isKitBuilderActive.value = false
                        viewModel.editingPadIndex.value = -1
                    },

                    modifier = Modifier.fillMaxWidth(),

                    shape = RoundedCornerShape(18.dp)
                ) {

                    Text(
                        text = "DONE",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}