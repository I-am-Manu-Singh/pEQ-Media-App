package org.audiocular.peq.app.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
     * ARM MOVES ONLY
     * ON REAL PLAYBACK
     */
  val armRotation by animateFloatAsState(
    targetValue =
        if (isPlaying) {
            0f
        } else {
            -42f
        },
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "arm_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight(1f)
            .aspectRatio(1f)
            .padding(horizontal = 18.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    isManualStop = !isManualStop
                }
            },
        contentAlignment = Alignment.Center
    ) {

        val vinylSize = 300.dp

        /*
         * VINYL
         */
        Box(
            modifier = Modifier
                .size(vinylSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {

            /*
             * STATIC DISC
             */
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                val radius =
                    size.minDimension / 2f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1B1B1B),
                            Color.Black
                        )
                    )
                )

                /*
                 * GROOVES
                 */
                for (i in 0 until 90 step 3) {

                    drawCircle(
                        color = Color.White.copy(
                            alpha =
                                if (i % 9 == 0) {
                                    0.045f
                                } else {
                                    0.018f
                                }
                        ),
                        radius =
                            radius * (i / 90f),
                        style = Stroke(
                            width = 0.6.dp.toPx()
                        )
                    )
                }

                /*
                 * OUTER RIM
                 */
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    style = Stroke(
                        width = 2.dp.toPx()
                    )
                )
            }

            /*
             * ROTATING CONTENT
             */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {

                        rotationZ =
                            if (actualPlaying) {
                                rotation
                            } else {
                                vinylRotation
                            }

                        compositingStrategy =
                            CompositingStrategy.Offscreen
                    },
                contentAlignment = Alignment.Center
            ) {

                /*
                 * CENTER LABEL
                 */
              /*
 * CENTER LABEL
 */
Box(
    modifier = Modifier.size(140.dp),
    contentAlignment = Alignment.Center
) {

    /*
     * ARTWORK
     */
    Box(
        modifier = Modifier
    .size(124.dp)
    .border(
        width = 1.dp,
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFFFF1A8),
                Color(0xFFE0B94B)
            )
        ),
        shape = CircleShape
    )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {

        /*
         * ALBUM ART
         */
        pEQImage(
            data = item.thumbnail,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        /*
         * GROOVE OVERLAY
         */
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val radius =
                size.minDimension / 2f

            /*
             * SUBTLE VINYL RINGS
             */
            for (i in 0 until 34 step 2) {

                drawCircle(
                    color = Color.White.copy(
                        alpha =
                            if (i % 8 == 0) {
                                0.055f
                            } else {
                                0.022f
                            }
                    ),
                    radius =
                        radius * (i / 34f),
                    style = Stroke(
                        width = 0.45.dp.toPx()
                    )
                )
            }

            /*
             * INNER SHADOW
             */
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

    /*
     * LABEL RING
     */
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                Color.Black.copy(alpha = 0.42f)
            )
    )

  /*
 * SPINDLE
 */
Box(
    modifier = Modifier.size(14.dp),
    contentAlignment = Alignment.Center
) {

    /*
     * SHADOW BASE
     */
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(
                Color.Black.copy(alpha = 0.22f)
            )
    )

    /*
     * METAL BODY
     */
    Box(
        modifier = Modifier
            .size(11.dp)
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

    /*
     * INNER CORE
     */
    Box(
        modifier = Modifier
            .size(5.dp)
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

    /*
     * SPECULAR HIGHLIGHT
     */
    Box(
        modifier = Modifier
            .offset(
                x = (-1.2).dp,
                y = (-1.2).dp
            )
            .size(1.8.dp)
            .clip(CircleShape)
            .background(
                Color.White.copy(alpha = 0.9f)
            )
    )
}
}
            }
        }

        /*
 * TONEARM
 */
Canvas(
    modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {

            rotationZ = armRotation

            transformOrigin =
                TransformOrigin(
                    0.78f,
                    0.11f
                )

            clip = false
        }
) {

    /*
     * PIVOT
     */
    val pivot =
        Offset(
            size.width * 0.77f,
            size.height * 0.13f
        )

    /*
     * NEEDLE POSITION
     */
    val stylusPoint =
    Offset(
        size.width * 0.245f,
        size.height * 0.60f
    )

    /*
     * TRUE SUBTLE S-CURVE
     */
    val armPath = Path().apply {

        moveTo(
            pivot.x,
            pivot.y
        )

       cubicTo(
    pivot.x - 40.dp.toPx(),
    pivot.y - 4.dp.toPx(),

    pivot.x - 60.dp.toPx(),
    pivot.y + 90.dp.toPx(),

    stylusPoint.x,
    stylusPoint.y
)
    }

    /*
     * ARM SHADOW
     */
    drawPath(
        path = armPath,
        color = Color.Black.copy(alpha = 0.13f),
        style = Stroke(
            width = 10.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    /*
     * MAIN ARM
     */
    drawPath(
        path = armPath,
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFEAEAEA),
                Color(0xFF747474)
            )
        ),
        style = Stroke(
            width = 7.dp.toPx(),
            cap = StrokeCap.Round
        )
    )

    /*
     * REAR STEM
     */
    drawLine(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFD8D8D8),
                Color(0xFF707070)
            )
        ),
        start = Offset(
            pivot.x + 10.dp.toPx(),
            pivot.y - 1.dp.toPx()
        ),
        end = Offset(
            pivot.x + 64.dp.toPx(),
            pivot.y - 5.dp.toPx()
        ),
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round
    )

    /*
     * COUNTERWEIGHT BODY
     */
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFFD6D6D6),
                Color(0xFF565656)
            )
        ),
        topLeft = Offset(
            pivot.x + 54.dp.toPx(),
            pivot.y - 13.dp.toPx()
        ),
        size = Size(
            24.dp.toPx(),
            16.dp.toPx()
        ),
        cornerRadius = CornerRadius(
            8.dp.toPx()
        )
    )

    /*
     * HEADSHELL
     */
    drawRoundRect(
        color = Color(0xFF181818),
        topLeft = Offset(
            stylusPoint.x - 12.dp.toPx(),
            stylusPoint.y - 4.dp.toPx()
        ),
        size = Size(
            24.dp.toPx(),
            34.dp.toPx()
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
            Color(0xFF7A7A7A),
            Color(0xFFECECEC)
        )
    ),
    start = Offset(
        stylusPoint.x,
        stylusPoint.y + 18.dp.toPx()
    ),
    end = Offset(
        stylusPoint.x,
        stylusPoint.y + 36.dp.toPx()
    ),
    strokeWidth = 2.dp.toPx(),
    cap = StrokeCap.Round
)

    /*
     * STYLUS TIP
     */
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 3.dp.toPx(),
        center = Offset(
            stylusPoint.x,
            stylusPoint.y + 38.dp.toPx()
        )
    )
}

/*
 * STATIC PIVOT ASSEMBLY
 */
Canvas(
    modifier = Modifier.fillMaxSize()
) {

    val pivot =
        Offset(
            size.width * 0.77f,
            size.height * 0.13f
        )

    /*
     * SHADOW
     */
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = 15.dp.toPx(),
        center = Offset(
            pivot.x + 1.dp.toPx(),
            pivot.y + 2.dp.toPx()
        )
    )

    /*
     * OUTER HOUSING
     */
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8E8E8),
                Color(0xFF666666)
            )
        ),
        radius = 13.dp.toPx(),
        center = pivot
    )

    /*
     * INNER RING
     */
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = 8.dp.toPx(),
        center = pivot
    )

    /*
     * CENTER BEARING
     */
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFF9B9B9B)
            )
        ),
        radius = 4.dp.toPx(),
        center = pivot
    )

    /*
     * METAL HIGHLIGHT
     */
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = 1.3.dp.toPx(),
        center = Offset(
            pivot.x - 3.dp.toPx(),
            pivot.y - 3.dp.toPx()
        )
    )
}

            
        }

        /*
         * MANUAL STOP CHIP
         */
       AnimatedVisibility(
    visible = isManualStop,
    modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.BottomCenter)
) {

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.78f),
                tonalElevation = 2.dp
            ) {

                Text(
                    text = "Paused",
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
