package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.audio.DesktopAudioEngine
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Size
import kotlin.math.roundToInt

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: ContentScale
) {
    val viewModel = LocalAppViewModel.current
    val engine = viewModel.audioEngine as? DesktopAudioEngine ?: return
    val bitmap by engine.videoBitmap
    val frameCount by engine.videoFrameCounter
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = viewModel.videoRotationDegrees.value.toFloat()
                    }
            ) {
                bitmap?.let { bmp ->
                    val imageBitmap = bmp.asComposeImageBitmap()
                    val srcSize = Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
                    val dstSize = size
                    val scaleFactor = contentScale.computeScaleFactor(srcSize, dstSize)
                    
                    val scaledWidth = srcSize.width * scaleFactor.scaleX
                    val scaledHeight = srcSize.height * scaleFactor.scaleY
                    
                    val xOffset = (dstSize.width - scaledWidth) / 2
                    val yOffset = (dstSize.height - scaledHeight) / 2
                    
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(xOffset.roundToInt(), yOffset.roundToInt()),
                        dstSize = IntSize(scaledWidth.roundToInt(), scaledHeight.roundToInt())
                    )
                }
            }
        } else {
            // Show a dark loading state instead of falling back to the blank AWT SwingPanel
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Initializing video decoder...", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
