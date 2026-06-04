package org.audiocular.peq.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import java.awt.Desktop
import java.net.URI

@Composable
actual fun pEQWebView(
    url: String,
    modifier: Modifier
) {
    // AWT Bridge for Desktop Lyrics
    SwingPanel(
        factory = {
            JScrollPane(JEditorPane().apply {
                isEditable = false
                contentType = "text/html"
                try {
                    setPage(url)
                } catch (e: Exception) {
                    text = "<html><body><h3>Neural Bridge: External Browser Required</h3><p>Unable to render high-fidelity web content in-engine. Please use the system browser for this source.</p><p>URL: $url</p></body></html>"
                }
            })
        },
        modifier = modifier
    )
}
