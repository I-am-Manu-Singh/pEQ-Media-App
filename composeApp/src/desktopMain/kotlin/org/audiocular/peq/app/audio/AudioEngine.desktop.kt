package org.audiocular.peq.app.audio

actual fun getAudioEngine(): AudioEngine {
    return DesktopAudioEngine()
}