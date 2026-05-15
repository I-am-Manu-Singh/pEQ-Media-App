package org.audiocular.peq.app.desktop

import androidx.compose.ui.awt.ComposePanel
import org.audiocular.peq.app.audio.DesktopAudioEngine
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JFrame
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.OverlayLayout
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

class DesktopPlayerWindow(
    private val engine: DesktopAudioEngine
) {

    private val frame =
        JFrame("pEQ")

    private val layeredPane =
        JLayeredPane()

    private val videoContainer =
        JPanel(BorderLayout())

    private val composePanel =
        ComposePanel()

    init {

        setupWindow()

        setupVideoLayer()

        setupComposeLayer()

        buildLayout()
    }

    /*
     * WINDOW
     */
    private fun setupWindow() {

        frame.defaultCloseOperation =
            JFrame.EXIT_ON_CLOSE

        frame.background =
            Color.BLACK

        frame.layout =
            BorderLayout()

        frame.setSize(1280, 720)

        frame.setLocationRelativeTo(null)
    }

    /*
     * VLC VIDEO
     */
    private fun setupVideoLayer() {

        val videoComponent =
            engine.getVideoComponent()
                as EmbeddedMediaPlayerComponent

        videoContainer.background =
            Color.BLACK

        videoContainer.add(
            videoComponent,
            BorderLayout.CENTER
        )
    }

    /*
     * COMPOSE OVERLAY
     */
    private fun setupComposeLayer() {

        composePanel.isOpaque = false

        composePanel.background =
            Color(0, 0, 0, 0)

        composePanel.setContent {

            /*
             * TEMP TEST OVERLAY
             */
            OverlayUI()
        }
    }

    /*
     * LAYOUT
     */
    private fun buildLayout() {

        layeredPane.layout =
            OverlayLayout(layeredPane)

        videoContainer.alignmentX = 0f
        videoContainer.alignmentY = 0f

        composePanel.alignmentX = 0f
        composePanel.alignmentY = 0f

        layeredPane.add(videoContainer, Integer.valueOf(0))

        layeredPane.add(composePanel, Integer.valueOf(1))

        frame.contentPane.add(
            layeredPane,
            BorderLayout.CENTER
        )
    }

    fun show() {

        frame.isVisible = true
    }
}