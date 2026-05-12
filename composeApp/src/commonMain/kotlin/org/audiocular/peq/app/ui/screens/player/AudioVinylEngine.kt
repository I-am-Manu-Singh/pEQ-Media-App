package org.audiocular.peq.app.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.data.pEQMediaItem
import org.audiocular.peq.app.ui.components.pEQImage
import kotlinx.coroutines.isActive

@Composable
fun AudioVinylEngine(item: pEQMediaItem, isPlaying: Boolean) {
    var vinylRotation by remember { mutableStateOf(0f) }
    var artworkRotation by remember { mutableStateOf(0f) }
    var isManualStop by remember { mutableStateOf(false) }

    val actualPlaying = isPlaying && !isManualStop

    LaunchedEffect(actualPlaying) {
        if (actualPlaying) {
            var lastTime = withFrameMillis { it }
            while (isActive) {
                withFrameMillis { time ->
                    val delta = (time - lastTime) / 1000f
                    lastTime = time
                    vinylRotation = (vinylRotation + delta * 45f) % 360f
                    artworkRotation = (artworkRotation + delta * 45f) % 360f
                }
            }
        }
    }

    val armRotation by animateFloatAsState(
        targetValue = if (actualPlaying) 0f else -45f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "ArmRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight(0.75f) // Reduced height for better landscape fit
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures {
                    isManualStop = !isManualStop
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Reduced diameter to fit better in landscape
        val vinylDiameter = 280.dp 
        Box(modifier = Modifier.size(vinylDiameter).clip(CircleShape), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = Color(0xFF080808)) {}
            
            // Vinyl Grooves
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = vinylRotation }) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val maxRadius = size.minDimension / 2
                    for (i in 0 until 180 step 4) { // More optimized step
                        drawCircle(
                            color = Color.White.copy(alpha = if (i % 40 == 0) 0.05f else 0.02f),
                            radius = (i.toFloat() / 180f) * (maxRadius - 10.dp.toPx()),
                            style = Stroke(width = 0.4f)
                        )
                    }
                }
            }
            
            // Album Artwork
            Box(
                modifier = Modifier
                    .size(130.dp) // Reduced artwork size
                    .graphicsLayer { rotationZ = artworkRotation }
                    .clip(CircleShape)
                    .border(1.5.dp, Color.Black.copy(0.9f), CircleShape)
            ) {
                pEQImage(data = item.thumbnail, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        
        // Tonearm - Optimized pivot and length
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = armRotation
                    transformOrigin = TransformOrigin(0.9f, 0.18f) // Adjusted pivot
                    clip = false
                }
        ) {
            val pivot = Offset(size.width * 0.9f, size.height * 0.18f)
            val armEndPoint = Offset(size.width * 0.28f, size.height * 0.55f) // Reduced arm length
            val armPath = Path().apply {
                moveTo(pivot.x, pivot.y)
                cubicTo(
                    pivot.x - 70.dp.toPx(), pivot.y + 30.dp.toPx(),
                    pivot.x + 30.dp.toPx(), pivot.y + 120.dp.toPx(),
                    armEndPoint.x, armEndPoint.y
                )
            }
            
            drawPath(armPath, Color.Black.copy(0.2f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
            drawPath(
                brush = Brush.linearGradient(listOf(Color(0xFFDDDDDD), Color(0xFF777777))),
                path = armPath,
                style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
            )
            
            rotate(-20f, pivot = armEndPoint) {
                drawRoundRect(
                    color = Color(0xFF151515),
                    topLeft = Offset(armEndPoint.x - 10.dp.toPx(), armEndPoint.y),
                    size = Size(20.dp.toPx(), 30.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                val cantileverEnd = Offset(armEndPoint.x, armEndPoint.y + 35.dp.toPx())
                drawLine(
                    color = Color(0xFF444444),
                    start = Offset(armEndPoint.x, armEndPoint.y + 25.dp.toPx()),
                    end = cantileverEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val stylusTip = Offset(cantileverEnd.x, cantileverEnd.y + 2.dp.toPx())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFF6A5A00)),
                        center = stylusTip,
                        radius = 3.dp.toPx()
                    ),
                    radius = 2.5.dp.toPx(),
                    center = stylusTip
                )
            }
        }
    }
}
