package org.audiocular.peq.app.ui.components

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.components.pEQGlassCard
import org.audiocular.peq.app.ui.theme.pEQTheme
import org.audiocular.peq.app.ui.theme.pEQAccent
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Premium Settings components for the pEQ Workstation.
 */

@Composable
fun SettingsPanel(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SYSTEM PREFERENCES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "INDUSTRIAL WORKSTATION v2.0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = { viewModel.showSettings.value = false }, modifier = Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }

        // Content
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Theme Section
            SettingsSection("AESTHETICS & THEME", icon = Icons.Default.Palette) {
                ThemeSelectionRow(viewModel)
                Spacer(Modifier.height(16.dp))
                AccentSelectionRow(viewModel)
            }

            // Engine Section
            SettingsSection("AUDIO ENGINE", icon = Icons.Default.SettingsInputComponent) {
                TogglePreference("Neural Gapless Playback", "Enable smooth transitions between workstation tracks", viewModel.isGaplessEnabled.value) { viewModel.isGaplessEnabled.value = it }
                TogglePreference("Hardware Bit-Perfect Mode", "Bypass system mixer for high-resolution DAC output", viewModel.isBitPerfectEnabled.value) { viewModel.isBitPerfectEnabled.value = it }
                TogglePreference("Haptic Feedback Orchestration", "Synchronize tactile response with neural transients", viewModel.isHapticsEnabled.value) { viewModel.isHapticsEnabled.value = it }
                
                Spacer(Modifier.height(8.dp))
                ActionPreference("Reset Neural Parameters", "Restore EQ and Master gains to flat state", Icons.Default.RestartAlt) {
                    viewModel.resetAudioEngine()
                }
            }
            
            // Services Section
            SettingsSection("API SERVICES", icon = Icons.Default.Cloud) {
                InputPreference(
                    label = "OpenSubtitles API Key",
                    desc = "Required for cloud-based subtitle retrieval",
                    value = viewModel.osApiKey.value,
                    onValueChange = { viewModel.updateOsApiKey(it) }
                )
            }
            
            // Backup & Persistence
            SettingsSection("ARCHIVE MAINTENANCE", icon = Icons.Default.SdStorage) {
                TogglePreference("Auto-Scan Archives", "Automatically refresh library on system changes", viewModel.isAutoScanEnabled.value) { viewModel.isAutoScanEnabled.value = it }
                
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        ActionPreference("Refresh Index", "Scan all folders for new media", Icons.Default.Refresh) {
                            viewModel.refreshLibrary()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ActionPreference("Clear Archive", "Purge all indexed metadata", Icons.Default.DeleteSweep) {
                            viewModel.clearLibrary()
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                ActionPreference("Purge Workstation Cache", "Delete temporary thumbnails and lyrics", Icons.Default.Cached) {
                    viewModel.clearCache()
                }
            }
        }
    }
}

@Composable
private fun InputPreference(label: String, desc: String, value: String, onValueChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 13.sp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ActionPreference(label: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun ThemeSelectionRow(viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("STATION THEME", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pEQTheme.entries.forEach { theme ->
                ThemeChip(theme.name, viewModel.selectedTheme.value == theme) {
                    viewModel.updateTheme(theme)
                }
            }
        }
    }
}

@Composable
private fun AccentSelectionRow(viewModel: AppViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("INDUSTRIAL ACCENT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pEQAccent.entries.forEach { accent ->
                AccentCircle(accent, viewModel.selectedAccent.value == accent) {
                    viewModel.updateAccent(accent)
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AccentCircle(accent: pEQAccent, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(accent.color)
            .border(
                2.dp,
                if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.2f),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    pEQGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.2.sp)
            }
            content()
        }
    }
}

@Composable
private fun TogglePreference(label: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 14.sp)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
        )
    }
}
