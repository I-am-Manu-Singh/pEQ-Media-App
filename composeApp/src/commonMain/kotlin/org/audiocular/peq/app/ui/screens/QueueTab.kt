package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.ui.viewmodel.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueTab(viewModel: AppViewModel) {

    val queue = viewModel.playbackQueue
    val currentIndex = viewModel.currentQueueIndex.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

           Box(modifier = Modifier.weight(1f)) {
    SectionHeader("REAL-TIME QUEUE")
}

            // SHUFFLE ALL
            if (queue.isNotEmpty()) {

                FilledTonalButton(
                    onClick = {
                        viewModel.shuffleQueue()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor =
                            if (viewModel.isShuffleEnabled)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {

                    Icon(
                        Icons.Default.Shuffle,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint =
                            if (viewModel.isShuffleEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        "SHUFFLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color =
                            if (viewModel.isShuffleEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                    )
                }

                // PURGE ALL
                TextButton(
                    onClick = {
                        queue.clear()
                    }
                ) {

                    Icon(
                        Icons.Default.DeleteSweep,
                        null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "PURGE ALL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (queue.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    "QUEUE IS EMPTY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                        .copy(alpha = 0.5f)
                )
            }

        } else {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {

                itemsIndexed(
                    queue,
                    key = { _, item ->
                        item.path + item.id
                    }
                ) { index, item ->

                    val dismissState =
                        rememberSwipeToDismissBoxState(

                            confirmValueChange = { value ->

                                if (
                                    value ==
                                    SwipeToDismissBoxValue.EndToStart
                                ) {

                                    viewModel.removeFromQueue(item)
                                    true

                                } else {
                                    false
                                }
                            }
                        )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,

                        backgroundContent = {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        MaterialTheme.colorScheme.error
                                            .copy(alpha = 0.1f)
                                    )
                                    .padding(end = 16.dp),

                                contentAlignment = Alignment.CenterEnd
                            ) {

                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    ) {

                        MediaItemCard(
                            item = item,
                            isActive = index == currentIndex,
                            showIndex = true,
                            index = index + 1,
                            onClick = {
                                viewModel.playMediaItemFromQueue(index)
                            }
                        )
                    }
                }
            }
        }
    }
}