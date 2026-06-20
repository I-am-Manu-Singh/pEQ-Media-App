package org.audiocular.peq.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, queue.size) {
        if (queue.isNotEmpty() && currentIndex in queue.indices) {
            kotlinx.coroutines.delay(250)
            if (!listState.isScrollInProgress) {
                try {
                    listState.scrollToItem(currentIndex)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

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

            SectionHeader(
                title = "REAL-TIME QUEUE (${queue.size})",
                modifier = Modifier.weight(1f)
            )

            // SHUFFLE
            FilledTonalIconButton(
                onClick = {
                    viewModel.shuffleQueue()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = if (viewModel.isShuffleEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // PURGE ALL
            FilledTonalIconButton(
                onClick = {
                    viewModel.clearQueue()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (queue.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "QUEUE VACANT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme
                        .onSurfaceVariant
                        .copy(alpha = 0.5f)
                )
            }

        } else {

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {

                itemsIndexed(
                    queue,
                    key = { index, item ->
                        "${item.path}_${index}_${item.id}"
                    }
                ) { index, item ->

                    val isCurrent = index == currentIndex

                    var accumulatedDrag by remember(item.path, index) {
                        mutableFloatStateOf(0f)
                    }

                    val dismissState =
                        rememberSwipeToDismissBoxState(
                            positionalThreshold = { it * 0.22f },
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    if (isCurrent) {
                                        viewModel.showToast("Current playing cannot be removed")
                                        false
                                    } else {
                                        viewModel.removeFromQueue(item)
                                        true
                                    }
                                } else {
                                    false
                                }
                            }
                        )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !isCurrent,

                        backgroundContent = {
                            if (!isCurrent) {
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
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }
                    ) {

                        MediaItemCard(
                            item = item,
                            isActive = isCurrent,
                            showIndex = true,
                            index = index + 1,
                            onClick = {
                                viewModel.playMediaItemFromQueue(index)
                            },
                            trailing = if (isCurrent) null else {
                                {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            )
                                            .pointerInput(item.path, queue.size) {
                                                detectVerticalDragGestures(
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        val currentItemIndex = queue.indexOfFirst { it.path == item.path }
                                                        if (currentItemIndex == -1) {
                                                            return@detectVerticalDragGestures
                                                        }
                                                        val threshold = 65f
                                                        if (accumulatedDrag > threshold && currentItemIndex < queue.lastIndex) {
                                                            viewModel.reorderQueue(currentItemIndex, currentItemIndex + 1)
                                                            accumulatedDrag = 0f
                                                        } else if (accumulatedDrag < -threshold && currentItemIndex > 0) {
                                                            viewModel.reorderQueue(currentItemIndex, currentItemIndex - 1)
                                                            accumulatedDrag = 0f
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        accumulatedDrag = 0f
                                                    }
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DragHandle,
                                            contentDescription = "Reorder Queue",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}