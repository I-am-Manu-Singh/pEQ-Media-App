package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment

/**
 * pEQ SPECTRUM ANALYZER: INDUSTRIAL FFT HUD Provides high-resolution frequency visualization for
 * signal monitoring.
 */
@Composable
fun SpectrumAnalyzer(
        magnitudes: FloatArray, // 0.0 to 1.0 magnitudes for frequency bins
        modifier: Modifier = Modifier
) {
        Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
                val width = size.width
                val height = size.height
                val barWidth = (width / magnitudes.size) * 0.8f
                val gap = (width / magnitudes.size) * 0.2f

                magnitudes.forEachIndexed { index, magnitude ->
                        val barHeight = magnitude * height
                        val x = index * (barWidth + gap)

                        // Industrial Gradient: Cyan to Deep Blue
                        val brush =
                                Brush.verticalGradient(
                                        colors =
                                                kotlin.collections.listOf(
                                                        Color(0, 229, 255),
                                                        Color(0, 176, 255).copy(alpha = 0.3f)
                                                ),
                                        startY = height - barHeight,
                                        endY = height
                                )

                        drawRoundRect(
                                brush = brush,
                                topLeft = Offset(x, height - barHeight),
                                size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx())
                        )
                }
        }
}

/**
 * pEQ CIRCULAR ANALYZER: NEURAL RING Provides a 360-degree frequency overview.
 */
@Composable
fun CircularSpectrumAnalyzer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(240.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val innerRadius = size.width * 0.25f
        val maxOuterRadius = size.width * 0.45f
        
        val angleStep = 360f / magnitudes.size
        
        magnitudes.forEachIndexed { index, magnitude ->
            val angle = index * angleStep
            val barHeight = magnitude * (maxOuterRadius - innerRadius)
            
            val start = Offset(
                x = center.x + innerRadius * kotlin.math.cos(angle * 3.14159f / 180f),
                y = center.y + innerRadius * kotlin.math.sin(angle * 3.14159f / 180f)
            )
            val end = Offset(
                x = center.x + (innerRadius + barHeight) * kotlin.math.cos(angle * 3.14159f / 180f),
                y = center.y + (innerRadius + barHeight) * kotlin.math.sin(angle * 3.14159f / 180f)
            )
            
            drawLine(
                color = Color(0, 229, 255).copy(alpha = 0.8f + (magnitude * 0.2f)),
                start = start,
                end = end,
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * pEQ SPECTROGRAM: TEMPORAL FREQUENCY MAP Displays frequency intensity over time.
 */
@Composable
fun SpectrogramAnalyzer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    // Placeholder for a real spectrogram which would require a history buffer
    Box(modifier = modifier.fillMaxWidth().height(120.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Spectrogram Mode (Placeholder)", color = Color.Gray.copy(alpha = 0.5f), fontSize = 10.sp)
    }
}
