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
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.viewmodel.toggleSubHarmonicGenerator
import org.audiocular.peq.app.ui.viewmodel.toggleNeuralSaturation
import org.audiocular.peq.app.ui.viewmodel.searchAutoEqRepository
import org.audiocular.peq.app.ui.viewmodel.applyAutoEqProfile
import org.audiocular.peq.app.ui.components.pEQGlassCard
import org.audiocular.peq.app.ui.components.ChordVisualizer
import kotlin.math.PI
import kotlin.math.sin
import org.audiocular.peq.app.ui.components.*
import androidx.compose.ui.text.font.FontFamily
import org.audiocular.peq.app.data.Preset
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TuningScreen(
    viewModel: AppViewModel
) {

    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.background,
                        colorScheme.surface.copy(alpha = 0.98f),
                        colorScheme.background
                    )
                )
            )
    ) {

        /*
         * BACKGROUND AMBIENT GLOW
         * Humans cannot resist glowing audio software.
         */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .offset(y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.10f),
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
                Arrangement.spacedBy(20.dp)
        ) {

            /*
             * HEADER
             */

            item {

                Column {

                    Text(
                        text = "TUNING",

                        fontSize = 12.sp,

                        fontWeight = FontWeight.Black,

                        letterSpacing = 3.sp,

                        color = primaryColor
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "AUDIO SUITE",

                        fontSize = 34.sp,

                        fontWeight = FontWeight.Black,

                        color = colorScheme.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text =
                            "PARAMETRIC DSP • AUTO-EQ • CONVOLUTION ENGINE",

                        fontSize = 10.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            colorScheme.onSurface
                                .copy(alpha = 0.42f),

                        letterSpacing = 1.sp
                    )
                }
            }

            /*
             * NOW PLAYING
             */

            item {

                NowPlayingTelemetryCard(
                    viewModel,
                    primaryColor
                )
            }

            /*
             * SIGNAL TOPOLOGY
             */

            item {

                SignalTopologySection(
                    viewModel,
                    primaryColor
                )
            }
             item {
            IndustrialDiagnosticsSection(
                viewModel,
                primaryColor
            )
        }

            /*
             * MASTERING CHAIN
             */

            item {

                MasteringChainSection(
                    viewModel,
                    primaryColor
                )
            }

            /*
             * AUTO EQ
             */

            item {

                AutoEqSection(
                    viewModel,
                    primaryColor
                )
            }

            item {

                Spacer(
                    modifier = Modifier.height(80.dp)
                )
            }
        }
    }
}

/*
 * IMPROVED SECTION CARD
 */

@Composable
private fun TuningSectionCard(
    title: String,
    icon: ImageVector,
    info: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {

    var showInfo by remember {
        mutableStateOf(false)
    }

    pEQGlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
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

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Black,

                        letterSpacing = 1.2.sp,

                        color =
                            MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "REALTIME DSP MODULE",

                        fontSize = 8.sp,

                        fontWeight = FontWeight.Bold,

                        color =
                            MaterialTheme.colorScheme
                                .onSurface
                                .copy(alpha = 0.32f)
                    )
                }

                if (info != null) {

                    FilledTonalIconButton(
                        onClick = {
                            showInfo = !showInfo
                        },

                        modifier = Modifier.size(34.dp),

                        colors =
                            IconButtonDefaults
                                .filledTonalIconButtonColors(
                                    containerColor =
                                        MaterialTheme.colorScheme
                                            .primary
                                            .copy(alpha = 0.12f)
                                )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Info,

                            contentDescription = null,

                            tint =
                                MaterialTheme.colorScheme.primary,

                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (showInfo && info != null) {

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),

                    color =
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.08f),

                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.12f)
                    )
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

            Spacer(Modifier.height(20.dp))

            content()
        }
    }
}

/*
 * FIXED EQ SECTION
 * Better spacing + cleaner vertical sliders.
 */

@Composable
private fun ParametricEqSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    val bands = listOf(
        "32", "64", "125", "250", "500",
        "1k", "2k", "4k", "8k", "16k"
    )

    val livePreset = remember {
        Preset("LIVE", emptyList())
    }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "10-BAND PRECISION KERNEL",

                fontSize = 10.sp,

                fontWeight = FontWeight.Black,

                color = primaryColor
            )

            Spacer(Modifier.weight(1f))

            Surface(
                shape = RoundedCornerShape(10.dp),

                color =
                    primaryColor.copy(alpha = 0.12f)
            ) {

                Text(
                    text = "LIVE",

                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    ),

                    fontSize = 8.sp,

                    fontWeight = FontWeight.Black,

                    color = primaryColor
                )
            }
        }

        /*
         * LIVE CURVE
         */

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),

            shape = RoundedCornerShape(20.dp),

            color =
                Color.Black.copy(alpha = 0.18f),

            border = BorderStroke(
                1.dp,
                primaryColor.copy(alpha = 0.08f)
            )
        ) {

            EQChart(
                preset = livePreset
            )
        }

        /*
         * SLIDERS
         */

        Surface(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(22.dp),

            color =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.52f),

            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.08f)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 14.dp
                    ),

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                bands.forEachIndexed { index, freq ->

                    ImprovedEqBandSlider(
                        label = freq,

                        value =
                            viewModel.eqGains[index],

                        qValue =
                            viewModel.eqQs[index],

                        onValueChange = {
                            viewModel.updateEqBand(
                                index,
                                it
                            )
                        },

                        onQChange = {
                            viewModel.updateEqQ(
                                index,
                                it
                            )
                        },

                        primaryColor = primaryColor
                    )
                }
            }
        }

        /*
         * ACTIONS
         */

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(
                onClick = { },

                modifier = Modifier.weight(1f),

                shape = RoundedCornerShape(16.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.RestartAlt,

                    contentDescription = null,

                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "RESET",

                    fontWeight = FontWeight.Black,

                    fontSize = 10.sp
                )
            }

            Button(
                onClick = { },

                modifier = Modifier.weight(1f),

                shape = RoundedCornerShape(16.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.AutoFixHigh,

                    contentDescription = null,

                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "AUTO OPTIMIZE",

                    fontWeight = FontWeight.Black,

                    fontSize = 10.sp
                )
            }
        }
    }
}

/*
 * CLEANER EQ BAND
 * Your previous one was visually compressed.
 */

@Composable
private fun ImprovedEqBandSlider(
    label: String,
    value: Float,
    qValue: Float,
    onValueChange: (Float) -> Unit,
    onQChange: (Float) -> Unit,
    primaryColor: Color
) {

    var localValue by remember {
        mutableFloatStateOf(value)
    }

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally,

        modifier = Modifier.width(28.dp)
    ) {

        Text(
            text = label,

            fontSize = 8.sp,

            fontWeight = FontWeight.Black,

            color =
                MaterialTheme.colorScheme
                    .onSurface
                    .copy(alpha = 0.62f)
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .width(26.dp),

            contentAlignment =
                Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Color.Black.copy(alpha = 0.35f)
                    )
            )

            Slider(
                value = localValue,

                onValueChange = {
                    localValue = it
                    onValueChange(it)
                },

                valueRange = -12f..12f,

                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                    }
                    .requiredWidth(220.dp),

                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "${localValue.toInt()}",

            fontSize = 8.sp,

            fontWeight = FontWeight.Black,

            color = primaryColor
        )
    }
}
@Composable
private fun SignalTopologySection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    TuningSectionCard(
        title = "SIGNAL TOPOLOGY",
        icon = Icons.Default.Schema,
        info = "Realtime DSP signal routing and active processing chain."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            Surface(
                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(20.dp),

                color =
                    MaterialTheme.colorScheme.surface
                        .copy(alpha = 0.52f),

                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                        .copy(alpha = 0.08f)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 18.dp
                        ),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    TopologyNode("IN", true)

                    TopologyLine(true)

                    TopologyNode("EQ", true)

                    TopologyLine(
                        viewModel.isConvolverActive.value
                    )

                    TopologyNode(
                        "IR",
                        viewModel.isConvolverActive.value
                    )

                    TopologyLine(true)

                    TopologyNode("DYN", true)

                    TopologyLine(true)

                    TopologyNode("OUT", true)
                }
            }

            LiveSpectrumVisualizer(
                viewModel = viewModel,
                primaryColor = primaryColor
            )
        }
    }
}
@Composable
private fun MasteringChainSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    TuningSectionCard(
        title = "DSP MASTERING CHAIN",
        icon = Icons.Default.AutoFixHigh,
        info = "Realtime mastering processors and neural enhancement stack."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(26.dp)
        ) {

            ParametricEqSection(
                viewModel,
                primaryColor
            )

            AdvancedMasteringSection(
                viewModel,
                primaryColor
            )

            ConvolutionDynamicsSection(
                viewModel,
                primaryColor
            )
        }
    }
}
@Composable
private fun AutoEqSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    TuningSectionCard(
        title = "AUTO-EQ REPOSITORY",
        icon = Icons.Default.Headphones,
        info = "Headphone correction profiles and compensation targets."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = searchQuery,

                onValueChange = {

                    searchQuery = it

                    viewModel
                        .searchAutoEqRepository(it)
                },

                modifier = Modifier.fillMaxWidth(),

                placeholder = {

                    Text(
                        text = "Search headphone profiles..."
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription = null
                    )
                },

                shape = RoundedCornerShape(18.dp),

                singleLine = true
            )

            if (viewModel.isAutoEqLoading.value) {

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),

                    color = primaryColor
                )
            }

            if (viewModel.autoEqSearchResults.isNotEmpty()) {

                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        viewModel.autoEqSearchResults
                    ) { profile ->

                        Surface(
                            onClick = {
                                viewModel
                                    .applyAutoEqProfile(profile)
                            },

                            shape = RoundedCornerShape(16.dp),

                            color =
                                if (
                                    viewModel
                                        .selectedAutoEqProfile
                                        .value == profile
                                ) {

                                    primaryColor.copy(alpha = 0.12f)

                                } else {

                                    MaterialTheme.colorScheme
                                        .surfaceVariant
                                        .copy(alpha = 0.24f)
                                },

                            border = BorderStroke(
                                1.dp,

                                if (
                                    viewModel
                                        .selectedAutoEqProfile
                                        .value == profile
                                ) {

                                    primaryColor.copy(alpha = 0.24f)

                                } else {

                                    Color.Transparent
                                }
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape = CircleShape,

                                    color =
                                        primaryColor.copy(alpha = 0.12f)
                                ) {

                                    Box(
                                        modifier = Modifier.padding(8.dp)
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Headphones,

                                            contentDescription = null,

                                            tint = primaryColor,

                                            modifier =
                                                Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = profile.name,

                                        fontSize = 12.sp,

                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = "AutoEQ Compensation",

                                        fontSize = 9.sp,

                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurface
                                                .copy(alpha = 0.42f)
                                    )
                                }

                                if (
                                    viewModel
                                        .selectedAutoEqProfile
                                        .value == profile
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Default.CheckCircle,

                                        contentDescription = null,

                                        tint = primaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun TopologyNode(
    label: String,
    active: Boolean
) {

    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (active)
                        primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = 1.dp,

                    color =
                        if (active)
                            primary.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.outline
                                .copy(alpha = 0.08f),

                    shape = CircleShape
                ),

            contentAlignment = Alignment.Center
        ) {

            Text(
                text = label,

                fontSize = 9.sp,

                fontWeight = FontWeight.Black,

                color =
                    if (active)
                        Color.Black
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun TopologyLine(
    active: Boolean
) {

    Box(
        modifier = Modifier
            .width(24.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(
                if (active)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
    )
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedMasteringSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(22.dp)
    ) {

        Text(
            text = "NEURAL MASTERING",

            fontSize = 10.sp,

            fontWeight = FontWeight.Black,

            color = primaryColor
        )

        val presets = listOf(
            "FLAT",
            "WARM",
            "AIRY",
            "STUDIO",
            "CINEMATIC"
        )

        FlowRow(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            presets.forEach { preset ->

                FilterChip(
                    selected = false,

                    onClick = {
                        viewModel.audioEngine
                            .applyMasteringChain(preset)
                    },

                    label = {
                        Text(
                            preset,
                            fontSize = 10.sp
                        )
                    }
                )
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            DspToggleItem(
                label = "VOCAL REMOVAL",

                active =
                    viewModel.isVocalRemovalActive.value,

                onToggle = {
                    viewModel.toggleVocalRemoval()
                },

                modifier = Modifier.weight(1f)
            )

            DspToggleItem(
                label = "NEURAL SAT",

                active =
                    viewModel.isNeuralSaturationEnabled.value,

                onToggle = {
                    viewModel.toggleNeuralSaturation(it)
                },

                modifier = Modifier.weight(1f)
            )
        }

        DspSliderItem(
            label = "CLARITY",

            range = 0f..1f,

            value = 0.5f,

            color = primaryColor
        ) {

            viewModel.audioEngine
                .setClarityLevel(it)
        }

        DspSliderItem(
            label = "DEEP BASS",

            range = 0f..1f,

            value = 0.5f,

            color = primaryColor
        ) {

            viewModel.audioEngine
                .setDeepBassLevel(it)
        }
    }
}

@Composable
private fun ConvolutionDynamicsSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {

        Surface(
            shape = RoundedCornerShape(18.dp),

            color =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.24f)
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
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
                            text = "CONVOLUTION IR",

                            fontSize = 11.sp,

                            fontWeight = FontWeight.Black,

                            color = primaryColor
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text =
                                viewModel.convolverIrName.value,

                            fontSize = 9.sp,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.42f)
                        )
                    }

                    Switch(
                        checked =
                            viewModel.isConvolverActive.value,

                        onCheckedChange = {
                            viewModel.isConvolverActive.value =
                                it
                        }
                    )
                }

                Spacer(Modifier.height(18.dp))

                Slider(
                    value =
                        viewModel.convolverIrDryWet.value,

                    onValueChange = {
                        viewModel.convolverIrDryWet.value =
                            it
                    }
                )

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.importIr()
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Icon(
                        Icons.Default.FileOpen,
                        null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "IMPORT IR",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        DspSliderItem(
            label = "COMP RATIO",

            range = 1f..20f,

            value =
                viewModel.compressorRatio.value,

            color = primaryColor
        ) {

            viewModel.compressorRatio.value = it
        }

        DspSliderItem(
            label = "STEREO WIDTH",

            range = 0f..2f,

            value =
                viewModel.stereoWidth.value,

            color = primaryColor
        ) {

            viewModel.stereoWidth.value = it
        }
    }
}
@Composable
private fun DspToggleItem(
    label: String,
    active: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Surface(
        onClick = {
            onToggle(!active)
        },

        modifier = modifier.height(64.dp),

        shape = RoundedCornerShape(18.dp),

        color =
            if (active)
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.14f)
            else
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.22f),

        border = BorderStroke(
            1.dp,

            if (active)
                MaterialTheme.colorScheme.primary
                    .copy(alpha = 0.22f)
            else
                MaterialTheme.colorScheme.outline
                    .copy(alpha = 0.06f)
        )
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = label,

                fontSize = 9.sp,

                fontWeight = FontWeight.Black,

                letterSpacing = 0.8.sp,

                color =
                    if (active)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.55f)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text =
                    if (active)
                        "ACTIVE"
                    else
                        "BYPASS",

                fontSize = 11.sp,

                fontWeight = FontWeight.Black,

                color =
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
private fun DspSliderItem(
    label: String,
    range: ClosedFloatingPointRange<Float>,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {

    var localValue by remember {
        mutableFloatStateOf(value)
    }

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = label,

                fontSize = 10.sp,

                fontWeight = FontWeight.Black,

                color = color
            )

            Text(
                text = localValue.toInt().toString(),

                fontSize = 10.sp,

                fontWeight = FontWeight.Bold,

                color =
                    MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(8.dp))

        Slider(
            value = localValue,

            onValueChange = {

                localValue = it
                onValueChange(it)
            },

            valueRange = range,

            modifier = Modifier.fillMaxWidth(),

            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor =
                    color.copy(alpha = 0.15f)
            )
        )
    }
}

/* ---------------------------------------------------------- */
/* DIAGNOSTICS */
/* ---------------------------------------------------------- */

@Composable
private fun IndustrialDiagnosticsSection(
    viewModel: AppViewModel,
    primaryColor: Color
) {

    TuningSectionCard(
        title = "DIAGNOSTICS",
        icon = Icons.Default.Memory,
        info = "Real-time audio engine telemetry."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                DiagnosticMiniCard(
                    "LATENCY",
                    "${viewModel.audioEngine.getProcessingLatencyMs()}ms",
                    Modifier.weight(1f)
                )

                DiagnosticMiniCard(
                    "DROPS",
                    "${viewModel.audioEngine.getAudioDropouts()}",
                    Modifier.weight(1f)
                )

                DiagnosticMiniCard(
                    "JITTER",
                    "${viewModel.audioEngine.getJitterBufferMs()}ms",
                    Modifier.weight(1f)
                )
            }

            Surface(

                modifier = Modifier.fillMaxWidth(),

                shape = RoundedCornerShape(16.dp),

                color =
                    MaterialTheme.colorScheme
                        .surfaceVariant
                        .copy(alpha = 0.25f)
            ) {

                Column(
                    modifier = Modifier.padding(
                        vertical = 12.dp,
                        horizontal = 10.dp
                    )
                ) {

                    Text(
                        text = "BITSTREAM",

                        fontSize = 8.sp,

                        fontWeight = FontWeight.Black,

                        color = primaryColor
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text =
                            "${viewModel.audioEngine.getCodec()} • " +
                                    "${viewModel.audioEngine.getSampleRate()}Hz • " +
                                    "${viewModel.audioEngine.getBitDepth()} BIT",

                        fontSize = 10.sp,

                        fontFamily = FontFamily.Monospace,

                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMiniCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Surface(

        modifier = modifier,

        shape = RoundedCornerShape(14.dp),

        color =
            MaterialTheme.colorScheme.surfaceVariant
                .copy(alpha = 0.24f),

        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
                .copy(alpha = 0.08f)
        )
    ) {

        Column(

            modifier = Modifier.padding(
                vertical = 12.dp,
                horizontal = 8.dp
            ),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = label,

                fontSize = 8.sp,

                fontWeight = FontWeight.Black,

                color =
                    MaterialTheme.colorScheme
                        .onSurface
                        .copy(alpha = 0.45f)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = value,

                fontSize = 12.sp,

                fontWeight = FontWeight.Black
            )
        }
    }
}