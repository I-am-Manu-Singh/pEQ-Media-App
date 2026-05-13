package org.audiocular.peq.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.Tab
import org.audiocular.peq.app.ui.viewmodel.*

/**
 * High-fidelity Navigation components for the pEQ Workstation.
 */

@Composable
fun NavigationRailSection(viewModel: AppViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Tab.entries.filter { it != Tab.Player }.forEach { tab ->
            NavRailItem(
                tab = tab,
                isSelected = viewModel.currentTab.value == tab,
                onClick = { viewModel.currentTab.value = tab }
            )
        }
    }
}

@Composable
private fun NavRailItem(tab: Tab, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else Color.Transparent,
        animationSpec = tween(300)
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(300)
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = getTabIcon(tab),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                tab.name.uppercase(),
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = contentColor,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(current: Tab, onSelect: (Tab) -> Unit) {
    val tabs = listOf(Tab.Dashboard, Tab.Tuning, Tab.Player, Tab.Hardware, Tab.Mixer)
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = current == tab
                val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = getTabIcon(tab),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                    )
                    if (isSelected) {
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
                    }
                }
            }
        }
    }
}

private fun getTabIcon(tab: Tab): ImageVector = when(tab) {
    Tab.Dashboard -> Icons.Default.Dashboard
    Tab.Player -> Icons.Default.MusicNote
    Tab.Hardware -> Icons.Default.Memory
    Tab.Mixer -> Icons.Default.Tune
    Tab.Tuning -> Icons.Default.GraphicEq
    Tab.Learn -> Icons.Default.School
}
