package org.audiocular.peq.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.theme.pEQTheme
import org.audiocular.peq.app.ui.theme.pEQAccent
import org.audiocular.peq.app.Tab

@Composable
fun SystemStatusTab(viewModel: AppViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        // 🆔 PROFILE SECTION
        ProfileSection(viewModel)

        // 🏗️ HEADER
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(
                "SYSTEM CONTROL CENTER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Industrial-grade orchestration for your neural workstation.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        // 📊 WORKSTATION TELEMETRY (Restored Previous Cards)
        SystemSectionBlock(title = "SYSTEM TELEMETRY", icon = Icons.Default.Analytics) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("CPU LOAD", "${(viewModel.dspProcessingLoad.value * 100).toInt()}%", Icons.Default.Memory, Modifier.weight(1f))
                StatCard("SIGNAL", "${viewModel.preDspLevel.value.toInt()}dB", Icons.Default.GraphicEq, Modifier.weight(1f))
            }
            
            // Output Device Card
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("ACTIVE NEURAL OUTPUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(viewModel.selectedOutputDevice.value.name, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // 🎨 USER INTERFACE & AESTHETICS
        SystemSectionBlock(title = "USER INTERFACE", icon = Icons.Default.Palette) {
            ThemePicker(viewModel)
            Spacer(Modifier.height(10.dp))
            AccentPicker(viewModel)
            Spacer(Modifier.height(10.dp))
            
            HardwareControlItem(
                label = "HAPTIC FEEDBACK",
                value = if (viewModel.isHapticsEnabled.value) "Enabled" else "Disabled",
                desc = "Vibrate on workstation interactions",
                enabled = viewModel.isHapticsEnabled.value,
                onToggle = { viewModel.isHapticsEnabled.value = it }
            )
            
            if (viewModel.isHapticsEnabled.value) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("VIBRATION INTENSITY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${(viewModel.hapticIntensity.value * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = viewModel.hapticIntensity.value,
                        onValueChange = { viewModel.hapticIntensity.value = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            HardwareControlItem(
                label = "DYNAMIC COLORS",
                value = if (viewModel.isDynamicColorsEnabled.value) "Adaptive" else "Static",
                desc = "Generate palette from workstation assets",
                enabled = viewModel.isDynamicColorsEnabled.value,
                onToggle = { viewModel.isDynamicColorsEnabled.value = it }
            )
        }

        // ⚡ AUDIO ENGINE & PLAYBACK
        SystemSectionBlock(title = "AUDIO ENGINE", icon = Icons.Default.SettingsInputComponent) {
            HardwareControlItem(
                label = "GAPLESS PLAYBACK",
                value = if (viewModel.isGaplessEnabled.value) "Enabled" else "Disabled",
                desc = "Remove playback latency between tracks",
                enabled = viewModel.isGaplessEnabled.value,
                onToggle = { viewModel.toggleGapless(it) }
            )

            HardwareControlItem(
                label = "NEURAL SATURATION",
                value = if (viewModel.isNeuralSaturationEnabled.value) "Active" else "Bypass",
                desc = "Industrial gain boost & harmonics",
                enabled = viewModel.isNeuralSaturationEnabled.value,
                onToggle = { viewModel.toggleNeuralSaturation(it) }
            )
            
            HardwareControlItem(
                label = "BIT-PERFECT MODE",
                value = if (viewModel.isBitPerfectEnabled.value) "Active" else "Standard",
                desc = "Direct hardware communication (USB/DAC)",
                enabled = viewModel.isBitPerfectEnabled.value,
                onToggle = { viewModel.isBitPerfectEnabled.value = it }
            )

            HardwareControlItem(
                label = "BUFFER PRIORITY",
                value = viewModel.bufferPriority.value,
                desc = "Ultra-low latency for pro hardware",
                enabled = viewModel.bufferPriority.value == "High",
                onToggle = { viewModel.bufferPriority.value = if (it) "High" else "Low" }
            )
        }

        // 🕒 WORKSTATION UTILITIES
        SystemSectionBlock(title = "WORKSTATION TOOLS", icon = Icons.Default.Timer) {
            HardwareControlItem(
                label = "SLEEP TIMER",
                value = if (viewModel.sleepTimerActive.value) "${viewModel.sleepTimerRemainingSec.longValue / 60}m remaining" else "Inactive",
                desc = "Automated system shutdown",
                enabled = viewModel.sleepTimerActive.value,
                onToggle = { viewModel.toggleSleepTimer(it) }
            )
            
            HardwareControlItem(
                label = "STATION LOCK",
                value = if (viewModel.isKidsLockActive.value) "Locked" else "Unsecured",
                desc = "Disable touch gestures in playback",
                enabled = viewModel.isKidsLockActive.value,
                onToggle = { viewModel.isKidsLockActive.value = it }
            )
        }

        // 📂 ARCHIVE SERVICES
        SystemSectionBlock(title = "ARCHIVE SERVICES", icon = Icons.Default.LibraryMusic) {
            HardwareControlItem(
                label = "AUTOMATIC SCAN",
                value = if (viewModel.isAutoScanEnabled.value) "Active" else "Manual Only",
                desc = "Refresh archives on workstation boot",
                enabled = viewModel.isAutoScanEnabled.value,
                onToggle = { viewModel.isAutoScanEnabled.value = it }
            )
        }


        


        Spacer(Modifier.height(110.dp))
    }
}

@Composable
fun ProfileSection(viewModel: AppViewModel) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("NEURAL IDENTITY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                Text("Workstation Admin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ThemePicker(viewModel: AppViewModel) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text("STATION THEME", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            pEQTheme.entries.forEach { theme ->
                val selected = viewModel.selectedTheme.value == theme
                Surface(
                    onClick = { viewModel.updateTheme(theme) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(theme.name, modifier = Modifier.padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AccentPicker(viewModel: AppViewModel) {
    Column(modifier = Modifier.padding(14.dp)) {
        Text("INDUSTRIAL ACCENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            pEQAccent.entries.forEach { accent ->
                val selected = viewModel.selectedAccent.value == accent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent.color, CircleShape)
                        .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                        .clickable { viewModel.updateAccent(accent) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SystemSectionBlock(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.58f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    }
}

@Composable
fun HardwareControlItem(label: String, value: String, desc: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
fun ModuleShortcut(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}
