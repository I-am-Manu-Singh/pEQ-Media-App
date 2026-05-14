package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.audio.DesktopAudioEngine
import org.audiocular.peq.app.ui.viewmodel.AppViewModel

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: ContentScale
) {
    val viewModel = LocalAppViewModel.current
    val engine = viewModel.audioEngine as? DesktopAudioEngine ?: return
    val bitmap by engine.videoBitmap

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            androidx.compose.foundation.Image(
                bitmap = it,
                contentDescription = "Video Surface",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } ?: run {
            // Placeholder when no video is playing or frame is not ready
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}