package org.audiocular.peq.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.pEQImage

@Composable
fun AudioVinylEngine(
    item: pEQMediaItem,
    isPlaying: Boolean
) {

    var isManualStop by remember {
        mutableStateOf(false)
    }

    val actualPlaying =
        isPlaying && !isManualStop

    /*
     * VINYL ROTATION
     */
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "vinyl_rotation"
        )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5200,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    /*
     * FREEZE POSITION
     */
    var vinylRotation by remember {
        mutableStateOf(0f)
    }

    LaunchedEffect(rotation, actualPlaying) {
        if (actualPlaying) {
            vinylRotation = rotation
        }
    }

    /*
     * TONEARM ANIMATION
     */
    val armRotation by animateFloatAsState(
        targetValue =
        if (isPlaying) {
            -4f
        } else {
            -36f
        },
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "arm_rotation"
    )

    var scratchOffset by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {

        val vinylContainerSize = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier
                .size(vinylContainerSize)
                .graphicsLayer {
                    rotationX = 22f
                    rotationY = -14f
                    cameraDistance = 16f
                },
            contentAlignment = Alignment.Center
        ) {

            /*
             * VINYL DISC
             */
            Box(
                modifier = Modifier
                    .fillMaxSize(0.82f)
                    .clip(CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { _ -> isManualStop = true },
                            onDragEnd = { 
                                isManualStop = false 
                                scratchOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scratchOffset += (dragAmount.x - dragAmount.y) * 0.4f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {

                Canvas(modifier = Modifier.fillMaxSize()) {

                    val radius = size.minDimension / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1B1B1B),
                                Color.Black
                            )
                        )
                    )

                    for (i in 0 until 90 step 3) {
                        drawCircle(
                            color = Color.White.copy(
                                alpha =
                                if (i % 9 == 0) 0.045f
                                else 0.018f
                            ),
                            radius = radius * (i / 90f),
                            style = Stroke(width = 0.6.dp.toPx())
                        )
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ =
                                (if (actualPlaying) rotation
                                else vinylRotation) + scratchOffset

                            compositingStrategy =
                                CompositingStrategy.Offscreen
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier.fillMaxSize(0.45f),
                        contentAlignment = Alignment.Center
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .border(
                                    1.dp,
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFFFFF1A8),
                                            Color(0xFFE0B94B)
                                        )
                                    ),
                                    CircleShape
                                )
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {

                            pEQImage(
                                data = item.thumbnail,
                                fallbackPath = item.path,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {

                                val radius =
                                    size.minDimension / 2f

                                for (i in 0 until 34 step 2) {
                                    drawCircle(
                                        color = Color.White.copy(
                                            alpha =
                                            if (i % 8 == 0) 0.055f
                                            else 0.022f
                                        ),
                                        radius = radius * (i / 34f),
                                        style = Stroke(
                                            width = 0.45.dp.toPx()
                                        )
                                    )
                                }

                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.18f)
                                        )
                                    )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.18f)
                                .clip(CircleShape)
                                .background(
                                    Color.Black.copy(alpha = 0.42f)
                                )
                        )

                        Box(
                            modifier = Modifier.fillMaxSize(0.1f),
                            contentAlignment = Alignment.Center
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        Color.Black.copy(alpha = 0.22f)
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.8f)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFF8F8F8),
                                                Color(0xFFCFCFCF),
                                                Color(0xFF707070)
                                            )
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.4f)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.White,
                                                Color(0xFF9A9A9A)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            /*
             * REALISTIC LONGER TONEARM
             *
             * Humans keep making turntable arms too short in UI mocks.
             * Apparently physics is optional now.
             */
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {

                        rotationZ = armRotation

                        /*
                         * Moved pivot back
                         * so hub doesn't overlap vinyl
                         */
                        transformOrigin = TransformOrigin(
                            0.94f,
                            0.08f
                        )

                        clip = false
                    }
            ) {

                val canvasSize = size

                /*
                 * Pivot moved further right/top
                 */
                val pivot = Offset(
                    canvasSize.width * 0.98f,
                    canvasSize.height * 0.04f
                )

                /*
                 * Needle now points near outer edge
                 * instead of center label like a confused DJ
                 */
                val stylusPoint = Offset(
                    canvasSize.width * 0.12f,
                    canvasSize.height * 0.48f
                )

                /*
                 * LONGER ARM PATH
                 */
                val armPath = Path().apply {

                    moveTo(pivot.x, pivot.y)

                    cubicTo(
                        pivot.x - canvasSize.width * 0.04f,
                        pivot.y + canvasSize.height * 0.10f,

                        stylusPoint.x + canvasSize.width * 0.16f,
                        stylusPoint.y - canvasSize.height * 0.14f,

                        stylusPoint.x,
                        stylusPoint.y
                    )
                }

                /*
                 * ARM SHADOW
                 */
                drawPath(
                    path = armPath,
                    color = Color.Black.copy(alpha = 0.16f),
                    style = Stroke(
                        width = 12.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                /*
                 * METAL ARM
                 */
                drawPath(
                    path = armPath,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFEAEAEA),
                            Color(0xFF747474),
                            Color(0xFFCFCFCF)
                        )
                    ),
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                /*
                 * COUNTERWEIGHT STEM
                 */
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFD8D8D8),
                            Color(0xFF505050)
                        )
                    ),
                    start = Offset(
                        pivot.x + canvasSize.width * 0.01f,
                        pivot.y - canvasSize.height * 0.004f
                    ),
                    end = Offset(
                        pivot.x + canvasSize.width * 0.10f,
                        pivot.y - canvasSize.height * 0.018f
                    ),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                /*
                 * COUNTERWEIGHT BLOCK
                 */
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFD6D6D6),
                            Color(0xFF464646)
                        )
                    ),
                    topLeft = Offset(
                        pivot.x + canvasSize.width * 0.08f,
                        pivot.y - canvasSize.height * 0.04f
                    ),
                    size = Size(
                        canvasSize.width * 0.075f,
                        canvasSize.height * 0.05f
                    ),
                    cornerRadius = CornerRadius(
                        8.dp.toPx()
                    )
                )

                /*
                 * HEADSHELL
                 */
                drawRoundRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(
                        stylusPoint.x - 13.dp.toPx(),
                        stylusPoint.y - 5.dp.toPx()
                    ),
                    size = Size(
                        26.dp.toPx(),
                        36.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(
                        4.dp.toPx()
                    )
                )

                /*
                 * NEEDLE
                 */
                drawLine(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFDADADA),
                            Color(0xFF8A8A8A)
                        )
                    ),
                    start = Offset(
                        stylusPoint.x,
                        stylusPoint.y + 18.dp.toPx()
                    ),
                    end = Offset(
                        stylusPoint.x,
                        stylusPoint.y + 34.dp.toPx()
                    ),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 3.5.dp.toPx(),
                    center = Offset(
                        stylusPoint.x,
                        stylusPoint.y + 36.dp.toPx()
                    )
                )
            }

            /*
             * PIVOT HUB
             */
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                val canvasSize = size

                val pivot = Offset(
                    canvasSize.width * 0.94f,
                    canvasSize.height * 0.08f
                )

                drawCircle(
                    color = Color.Black.copy(alpha = 0.22f),
                    radius = 18.dp.toPx(),
                    center = Offset(
                        pivot.x + 1.dp.toPx(),
                        pivot.y + 2.dp.toPx()
                    )
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE8E8E8),
                            Color(0xFF555555)
                        )
                    ),
                    radius = 15.dp.toPx(),
                    center = pivot
                )

                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = 10.dp.toPx(),
                    center = pivot
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFF9B9B9B)
                        )
                    ),
                    radius = 5.dp.toPx(),
                    center = pivot
                )
            }
        }

        AnimatedVisibility(
            visible = isManualStop,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(
                    alpha = 0.85f
                ),
                tonalElevation = 4.dp
            ) {

                Text(
                    text = "STATION PAUSED",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}