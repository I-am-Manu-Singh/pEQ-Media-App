package org.audiocular.peq.app.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.pEQImage

@Composable
fun pEQWidgetCommand(
    item: pEQMediaItem?,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0.68f,
    isFavorite: Boolean = false,
    isShuffleEnabled: Boolean = false
) {

    // =========================================================
    // ANIMATION
    // =========================================================

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "widget_transition"
        )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.04f else 1f,
        animationSpec = tween(300),
        label = "play_scale"
    )

    // =========================================================
    // ROOT
    // =========================================================

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null
            ) {
                onLaunch()
            },

        shape = RoundedCornerShape(30.dp),

        color = Color(0xFF090909),

        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.06f)
        ),

        tonalElevation = 12.dp,

        shadowElevation = 18.dp
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // =====================================================
            // BACKGROUND
            // =====================================================

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF111111),
                                Color(0xFF070707)
                            )
                        )
                    )
            )

            // =====================================================
            // YELLOW GLOW
            // =====================================================

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(
                        x = 170.dp,
                        y = (-90).dp
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD600)
                                    .copy(alpha = glowAlpha),

                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // =====================================================
            // MAIN CONTENT
            // =====================================================

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 14.dp
                    ),

                verticalAlignment = Alignment.CenterVertically
            ) {

                // =================================================
                // ARTWORK
                // =================================================

                Box {

                    Surface(
                        modifier = Modifier.size(84.dp),

                        shape = RoundedCornerShape(24.dp),

                        color = Color(0xFF161616),

                        border = BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.04f)
                        )
                    ) {

                        if (item?.thumbnail != null) {

                            pEQImage(
                                data = item.thumbnail,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                mediaType = item.type
                            )

                        } else {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF1A1A1A),
                                                Color(0xFF101010)
                                            )
                                        )
                                    ),

                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.VolumeUp,

                                    contentDescription = null,

                                    tint = Color(0xFFFFD600),

                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    // =================================================
                    // PLAYING DOT
                    // =================================================

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(
                                x = (-3).dp,
                                y = 3.dp
                            )
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPlaying) {
                                    Color(0xFF00FF9D)
                                } else {
                                    Color.DarkGray
                                }
                            )
                    )
                }

                Spacer(
                    modifier = Modifier.width(16.dp)
                )

                // =================================================
                // RIGHT SIDE
                // =================================================

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {

                    // =============================================
                    // TITLE
                    // =============================================

                    Text(
                        text =
                            item?.title
                                ?: "pEQ Neural Engine",

                        color = Color.White,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.Bold,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    // =============================================
                    // ARTIST
                    // =============================================

                    Text(
                        text =
                            item?.artist
                                ?: "Industrial Audio Core",

                        color = Color(0xFFFFD600),

                        fontSize = 11.sp,

                        fontWeight = FontWeight.Medium,

                        maxLines = 1,

                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // =============================================
                    // CONTROLS
                    // =============================================

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        WidgetIconButton(
                            icon =
                                if (isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },

                            tint = Color(0xFFFF5A7A)
                        ) {
                        }

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        WidgetIconButton(
                            icon = Icons.Default.SkipPrevious
                        ) {
                            onPrev()
                        }

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        // =========================================
                        // PLAY BUTTON
                        // =========================================

                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = playScale
                                    scaleY = playScale
                                },

                            shape = CircleShape,

                            color = Color(0xFFFFD600),

                            shadowElevation = 10.dp,

                            onClick = onTogglePlayback
                        ) {

                            Box(
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        if (isPlaying) {
                                            Icons.Default.Pause
                                        } else {
                                            Icons.Default.PlayArrow
                                        },

                                    contentDescription = null,

                                    tint = Color.Black,

                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        WidgetIconButton(
                            icon = Icons.Default.SkipNext
                        ) {
                            onNext()
                        }

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        WidgetIconButton(
                            icon = Icons.Default.Shuffle,

                            tint =
                                if (isShuffleEnabled) {
                                    Color.White
                                } else {
                                    Color(0xFFFFD600)
                                }
                        ) {
                        }
                    }
                }
            }

            // =====================================================
            // PROGRESS
            // =====================================================

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Color.White.copy(alpha = 0.05f)
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                progress.coerceIn(
                                    0f,
                                    1f
                                )
                            )
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFFD600),
                                        Color(0xFFFFB300)
                                    )
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Color.White.copy(alpha = 0.03f)
                        )
                )
            }
        }
    }
}

@Composable
private fun WidgetIconButton(
    icon: ImageVector,
    tint: Color = Color.White,
    size: Dp = 40.dp,
    onClick: () -> Unit
) {

    Surface(
        modifier = Modifier.size(size),

        shape = CircleShape,

        color = Color.White.copy(alpha = 0.06f),

        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.05f)
        ),

        tonalElevation = 2.dp,

        onClick = onClick
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,

                contentDescription = null,

                tint = tint,

                modifier = Modifier.size(20.dp)
            )
        }
    }
}