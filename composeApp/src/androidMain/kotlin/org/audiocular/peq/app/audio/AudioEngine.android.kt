package org.audiocular.peq.app.audio

import org.audiocular.peq.app.pEQApplication

actual fun getAudioEngine(): AudioEngine {
    return AndroidAudioEngine(
        pEQApplication.instance
    )
}