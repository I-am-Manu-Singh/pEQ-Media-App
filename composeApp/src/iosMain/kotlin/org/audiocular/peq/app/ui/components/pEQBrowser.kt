package org.audiocular.peq.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun pEQBrowser(
    url: String,
    onClose: () -> Unit,
    onStreamCaptured: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Browser not supported on iOS yet")
    }
}
