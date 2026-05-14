package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.theme.*
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.data.Device

/**
 * High-fidelity Dialog components for the pEQ Workstation.
 */


@Composable
fun DeviceSwitcherDialog(viewModel: AppViewModel) {
    AlertDialog(
        onDismissRequest = { viewModel.showDeviceSwitcher.value = false },
        confirmButton = {},
        title = {
            Text("AUDIO OUTPUT ROUTING", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 2.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceItem(Device.INTERNAL, "System Default Output", viewModel)
                DeviceItem(Device.WIRELESS_ADAPTER, "Bluetooth A2DP (High Latency)", viewModel)
                DeviceItem(Device.GENERIC, "USB-C Bit-Perfect DAC", viewModel)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    )
}

@Composable
private fun DeviceItem(device: Device, desc: String, viewModel: AppViewModel) {
    val isSelected = viewModel.selectedOutputDevice.value == device
    Surface(
        onClick = { 
            viewModel.updateDevice(device)
            viewModel.showDeviceSwitcher.value = false
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                device.icon,
                null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(device.name, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
