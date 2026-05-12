package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale
) {
    Box(modifier = modifier.background(Color.DarkGray), contentAlignment = Alignment.Center) {
        Text("VIDEO SURFACE (IOS STUB)", color = Color.White)
    }
}
