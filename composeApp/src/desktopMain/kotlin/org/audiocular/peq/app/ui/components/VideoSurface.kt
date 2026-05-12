package org.audiocular.peq.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.VlcAudioEngine

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: androidx.compose.ui.layout.ContentScale
) {

    val viewModel =
        LocalAppViewModel.current

    val engine =
        viewModel.audioEngine
            as? VlcAudioEngine
                ?: return

    SwingPanel(
        modifier = modifier,
        factory = {
            engine.playerComponent
        }
    )
}