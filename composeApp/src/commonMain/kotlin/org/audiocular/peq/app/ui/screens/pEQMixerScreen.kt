package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*

/**
 * pEQ Neural Mixer
 * Apparently humans looked at studio mixers and thought:
 * "What if this glowed more and fit inside a rectangle."
 */

@Composable
fun pEQMixerScreen(
    viewModel: AppViewModel
) {

    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary

    val stems =
        remember(viewModel.stemVolumes) {
            viewModel.stemVolumes.keys.toList()
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.background,
                        colorScheme.surface.copy(alpha = 0.96f),
                        colorScheme.background
                    )
                )
            )
    ) {

        /*
         * BACKGROUND GLOW
         */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .offset(y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                horizontal = 18.dp,
                vertical = 18.dp
            ),

            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            /*
             * HEADER
             */

            item {

                MixerTopBar(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )
            }

            /*
             * NOW PLAYING STRIP
             */

            item {

                CompactTransportBanner(
                    viewModel = viewModel
                )
            }

            /*
             * MONITORING
             */

            item {

                MasterMonitoringSection(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )
            }

            /*
             * STEM CONSOLE
             */

            item {

                StemConsoleSection(
                    viewModel = viewModel,
                    primaryColor = primaryColor,
                    stems = stems
                )
            }

            /*
             * PIPELINE
             */

            item {

                NeuralStemPipeline(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )
            }

            /*
             * MASTER IO
             */

            item {

                MasterFaderSection(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )
            }

            item {
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun MixerTopBar(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column {

            Text(
                text = "pEQ NEURAL MIXER",

                fontSize = 13.sp,

                fontWeight = FontWeight.Black,

                color = primaryColor,

                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text =
                    "REALTIME STEM ENGINE • JNI DSP • ONNX",

                fontSize = 10.sp,

                fontWeight = FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme
                        .onSurface
                        .copy(alpha = 0.38f),

                letterSpacing = 1.sp
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            FilledTonalIconButton(
                onClick = {
                    viewModel.startStemSplit()
                },

                colors =
                    IconButtonDefaults
                        .filledTonalIconButtonColors(
                            containerColor =
                                primaryColor.copy(alpha = 0.12f)
                        )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.AutoGraph,

                    contentDescription = null,

                    tint = primaryColor
                )
            }

        }
    }
}

@Composable
private fun CompactTransportBanner(
    viewModel: AppViewModel
) {

    val item =
        viewModel.currentMediaItem.value

    val isPlaying =
        viewModel.isPlaying.value

    Surface(
        modifier = Modifier.fillMaxWidth(),

        shape = RoundedCornerShape(24.dp),

        color =
            MaterialTheme.colorScheme.surface
                .copy(alpha = 0.72f),

        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
                .copy(alpha = 0.08f)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.12f)
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.GraphicEq,

                    contentDescription = null,

                    tint =
                        MaterialTheme.colorScheme.primary,

                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        item?.title ?: "No Active Session",

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis,

                    fontWeight =
                        FontWeight.Black,

                    fontSize = 14.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text =
                        item?.artist
                            ?: "Awaiting signal input",

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis,

                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,

                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            FilledIconButton(
                onClick = {
                    viewModel.togglePlayback()
                }
            ) {

                Icon(
                    imageVector =
                        if (isPlaying)
                            Icons.Default.Pause
                        else
                            Icons.Default.PlayArrow,

                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun MasterFaderSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    MixerSectionCard(
        title = "MASTER BUS",
        icon = Icons.Default.Tune,
        info = "Global post-fader output stage."
    ) {

        Surface(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(20.dp),

            color =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.32f)
        ) {

            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column {

                        Text(
                            text = "MASTER OUTPUT",

                            fontSize = 10.sp,

                            fontWeight = FontWeight.Black,

                            color = primaryColor
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text =
                                "BIT PERFECT ENGINE",

                            fontSize = 9.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.42f)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        text =
                            "${(viewModel.masterVolume.value * 100).toInt()}%",

                        fontSize = 18.sp,

                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(Modifier.height(18.dp))

                Slider(
                    value = viewModel.masterVolume.value,

                    onValueChange = {
                        viewModel.masterVolume.value = it
                    },

                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor
                    )
                )
            }
        }
    }
}

@Composable
private fun MasterMonitoringSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    MixerSectionCard(
        title = "MASTER MONITORING",
        icon = Icons.Default.Monitor
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            NowPlayingTelemetryCard(
                viewModel,
                primaryColor
            )

            LiveSpectrumVisualizer(
                    viewModel = viewModel,
                    primaryColor = primaryColor
                )

            viewModel.activeJob.value?.let { job ->

                Surface(
                    shape = RoundedCornerShape(16.dp),

                    color =
                        primaryColor.copy(alpha = 0.06f),

                    border = BorderStroke(
                        1.dp,
                        primaryColor.copy(alpha = 0.08f)
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {

                            Text(
                                text = job.name,

                                fontSize = 10.sp,

                                fontWeight =
                                    FontWeight.Black,

                                color = primaryColor
                            )

                            Text(
                                text =
                                    "${(job.progress * 100).toInt()}%",

                                fontSize = 10.sp,

                                fontWeight =
                                    FontWeight.Black,

                                color = primaryColor
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { job.progress },

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),

                            color = primaryColor
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = job.status,

                            fontSize = 9.sp,

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
@Composable
private fun StemConsoleSection(
    viewModel: AppViewModel,
    primaryColor: Color,
    stems: List<String>
) {

    val activeStems =
        stems.ifEmpty {

            listOf(
                "vocals",
                "drums",
                "bass",
                "piano",
                "guitar",
                "other"
            )
        }

    MixerSectionCard(
        title = "STEM CONSOLE",
        icon = Icons.Default.Equalizer,
        info = "Realtime isolated neural stem balancing."
    ) {

        /*
         * FIXED GRID LAYOUT
         * No horizontal scrolling circus anymore.
         */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp),

            horizontalArrangement =
                Arrangement.SpaceEvenly,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            activeStems.take(6).forEach { stem ->

                StemFaderColumn(
                    name = stem,

                    icon = stemIcon(stem),

                    volume =
                        viewModel.stemVolumes[stem]
                            ?: 1f,

                    isMuted =
                        viewModel.stemMutes[stem]
                            ?: false,

                    isSolo =
                        viewModel.stemSolos[stem]
                            ?: false,

                    pan =
                        viewModel.stemPans[stem]
                            ?: 0f,

                    onVolumeChange = {
                        viewModel.updateStemVolume(
                            stem,
                            it
                        )
                    },

                    onMuteToggle = {
                        viewModel.toggleStemMute(stem)
                    },

                    onSoloToggle = {
                        viewModel.toggleStemSolo(stem)
                    },

                    onPanChange = {
                        viewModel.updateStemPan(
                            stem,
                            it
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun StemFaderColumn(
    name: String,
    icon: ImageVector,
    volume: Float,
    isMuted: Boolean,
    isSolo: Boolean,
    pan: Float,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onSoloToggle: () -> Unit,
    onPanChange: (Float) -> Unit
) {

    val primary =
        MaterialTheme.colorScheme.primary

    val active =
        volume > 0.01f && !isMuted

    Surface(
        modifier = Modifier
            .width(54.dp)
            .fillMaxHeight(),

        shape = RoundedCornerShape(22.dp),

        color =
            MaterialTheme.colorScheme.surface
                .copy(alpha = 0.62f),

        border = BorderStroke(
            1.dp,

            if (active)
                primary.copy(alpha = 0.14f)
            else
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.06f)
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 6.dp,
                    vertical = 10.dp
                ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            /*
             * STEM ICON
             */

            Surface(
                shape = CircleShape,

                color =
                    if (active)
                        primary.copy(alpha = 0.14f)
                    else
                        MaterialTheme.colorScheme
                            .surfaceVariant
                            .copy(alpha = 0.42f)
            ) {

                Box(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = icon,

                        contentDescription = null,

                        tint =
                            if (active)
                                primary
                            else
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,

                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            /*
             * SOLO / MUTE
             */

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                MiniMixerButton(
                    text = "S",

                    active = isSolo,

                    activeColor =
                        MaterialTheme.colorScheme.tertiary,

                    onClick = onSoloToggle
                )

                MiniMixerButton(
                    text = "M",

                    active = isMuted,

                    activeColor = Color.Red,

                    onClick = onMuteToggle
                )
            }

            Spacer(Modifier.height(12.dp))

            /*
             * PAN
             */

            Slider(
                value = pan,

                onValueChange = onPanChange,

                valueRange = -1f..1f,

                modifier = Modifier.width(42.dp),

                colors = SliderDefaults.colors(
                    thumbColor = primary,
                    activeTrackColor =
                        primary.copy(alpha = 0.3f)
                )
            )

            Spacer(Modifier.height(14.dp))

            /*
             * FADER
             */

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(40.dp),

                contentAlignment =
                    Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Color.Black.copy(alpha = 0.42f)
                        )
                )

                Slider(
                    value = volume,

                    onValueChange = onVolumeChange,

                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = -90f
                        }
                        .requiredWidth(230.dp),

                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            /*
             * LEVEL
             */

            Text(
                text =
                    if (isMuted)
                        "M"
                    else
                        "${(volume * 100).toInt()}",

                fontSize = 10.sp,

                fontWeight = FontWeight.Black,

                color =
                    if (isMuted)
                        Color.Red
                    else
                        MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/*
 * STEM ICON RESOLUTION
 * Tiny symbolic representation of organized audio suffering.
 */

private fun stemIcon(
    stem: String
): ImageVector {

    return when (stem.lowercase()) {

        "vocals" ->
            Icons.Default.Mic

        "drums" ->
            Icons.Default.GraphicEq

        "bass" ->
            Icons.Default.SurroundSound

        "piano" ->
            Icons.Default.Piano

        "guitar" ->
            Icons.Default.LibraryMusic

        else ->
            Icons.Default.AudioFile
    }
}

@Composable
private fun MiniMixerButton(
    text: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,

        modifier = Modifier.size(28.dp),

        shape = RoundedCornerShape(8.dp),

        color =
            if (active)
                activeColor
            else
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.45f)
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = text,

                fontSize = 10.sp,

                fontWeight = FontWeight.Black,

                color =
                    if (active)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurface
            )
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

    var expanded by remember {
        mutableStateOf(false)
    }

    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,

                    color =
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.12f)
                ) {

                    Box(
                        modifier = Modifier.padding(8.dp)
                    ) {

                        Icon(
                            imageVector = icon,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme.primary,

                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = title,

                    modifier = Modifier.weight(1f),

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Black,

                    color =
                        MaterialTheme.colorScheme.primary,

                    letterSpacing = 1.3.sp
                )

                if (info != null) {

                    IconButton(
                        onClick = {
                            expanded = !expanded
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Info,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.6f),

                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (expanded && info != null) {

                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),

                    color =
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.08f)
                ) {

                    Text(
                        text = info,

                        modifier = Modifier.padding(14.dp),

                        fontSize = 10.sp,

                        lineHeight = 15.sp,

                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            content()
        }
    }
}

@Composable
private fun NeuralStemPipeline(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    MixerSectionCard(
        title = "NEURAL STEM PIPELINE",
        icon = Icons.Default.AutoFixHigh
    ) {

        Column {

            Text(
                text =
                    "AI accelerated source separation using ONNX Runtime + FFmpeg.",

                fontSize = 11.sp,

                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {

                    viewModel.onPickTrackRequested?.invoke {
                            item: pEQMediaItem ->

                        viewModel.enqueueSplitJob(item)
                    }
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),

                shape = RoundedCornerShape(18.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Memory,

                    contentDescription = null
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "INITIATE STEM DECOUPLING",

                    fontWeight = FontWeight.Black,

                    fontSize = 12.sp
                )
            }

            viewModel.activeJob.value?.let {

                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { it.progress },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(100.dp)),

                    color = primaryColor
                )
            }
        }
    }
}