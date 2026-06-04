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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
fun BottomNavigationBar(
    current: Tab,
    viewModel: AppViewModel,
    onSelect: (Tab) -> Unit
) {

    val tabs = listOf(
        Tab.Dashboard,
        Tab.Tuning,
        Tab.Mixer,
        Tab.Hardware,
        Tab.Learn
    )

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
                .height(72.dp)
                .padding(horizontal = 12.dp),

            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            tabs.forEach { tab ->

                val isSelected =
                    current == tab

                val iconColor =
                    if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.45f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {

                            // CLOSE DRUM SCREEN
                            viewModel.showDrumLibrary.value = false

                            // CHANGE TAB
                            onSelect(tab)
                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 10.dp
                        ),

                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Box(
                        contentAlignment = Alignment.Center
                    ) {

                        // ACTIVE BACKGROUND
                    if (isSelected) {

    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .size(4.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.primary
            )
    )
}

                        Icon(
                            imageVector = tab.icon,

                            contentDescription =
                                tab.label,

                            tint = iconColor,

                            modifier =
                                Modifier.size(
                                    if (isSelected)
                                        24.dp
                                    else
                                        21.dp
                                )
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = tab.label,

                        fontSize = 9.sp,

                        maxLines = 1,

                        fontWeight =
                            if (isSelected)
                                FontWeight.Bold
                            else
                                FontWeight.Medium,

                        color = iconColor
                    )

                    if (isSelected) {

                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.primary
                                )
                        )
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
    // Tab.DrumLibrary -> Icons.Default.MusicVideo
}
