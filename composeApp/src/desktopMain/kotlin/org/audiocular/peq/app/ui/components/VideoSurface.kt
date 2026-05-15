package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import org.audiocular.peq.app.LocalAppViewModel
import org.audiocular.peq.app.audio.DesktopAudioEngine
import java.awt.BorderLayout
import javax.swing.JPanel

@Composable
actual fun VideoSurface(
    modifier: Modifier,
    contentScale: ContentScale
) {

    val viewModel =
        LocalAppViewModel.current

    val engine =
        viewModel.audioEngine
            as? DesktopAudioEngine
            ?: return

    val component = remember {

        println(
            "pEQControl: REMEMBER VIDEO COMPONENT"
        )

        engine.getVideoComponent()
    }

    val videoPanel = remember {

        JPanel(BorderLayout()).apply {

            isOpaque = true

            background =
                java.awt.Color.BLACK

            add(
                component,
                BorderLayout.CENTER
            )

            println(
                "pEQControl: SWING VIDEO SURFACE MOUNTED"
            )
        }
    }

    LaunchedEffect(Unit) {

        delay(500)

        videoPanel.revalidate()
        videoPanel.repaint()

        component.revalidate()
        component.repaint()

        engine.notifyVideoSurfaceReady()
    }

    SwingPanel(

    modifier = modifier.fillMaxSize(),

    factory = {

        videoPanel
    }
)
}