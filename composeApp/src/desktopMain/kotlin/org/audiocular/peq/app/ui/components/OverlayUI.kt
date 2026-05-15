package org.audiocular.peq.app.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OverlayUI() {

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Text(
            text = "pEQ Overlay",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp)
        )
    }
}