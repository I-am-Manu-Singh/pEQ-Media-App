package org.audiocular.peq.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {

    val audioEngine = remember { org.audiocular.peq.app.audio.getAudioEngine() }
    val persistenceManager = remember { org.audiocular.peq.app.data.getPersistenceManager() }

    val viewModel = remember {
        AppViewModel(
            audioEngine = audioEngine,
            persistenceManager = persistenceManager
        )
    }
    LaunchedEffect(Unit) {
        viewModel.start()
    }
    CompositionLocalProvider(LocalAppViewModel provides viewModel) {
        App()
    }
}
