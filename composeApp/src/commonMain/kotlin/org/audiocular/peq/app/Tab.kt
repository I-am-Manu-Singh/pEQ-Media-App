package org.audiocular.peq.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import org.audiocular.peq.app.ui.theme.pEQStrings

enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("DASHBOARD", Icons.Default.Dashboard),
    Tuning(pEQStrings.TUNING_TAB, Icons.Default.Troubleshoot),
    Hardware(pEQStrings.HARDWARE_TAB, Icons.Default.SettingsInputComponent),
    Learn("LEARN", Icons.Default.School),
    Mixer(org.audiocular.peq.app.ui.theme.pEQStrings.NEURAL_MIXER, Icons.Default.Tune),
    Player("NOW PLAYING", Icons.Default.GraphicEq),
    // DrumLibrary("DRUM LIBRARY", Icons.Default.School)
}