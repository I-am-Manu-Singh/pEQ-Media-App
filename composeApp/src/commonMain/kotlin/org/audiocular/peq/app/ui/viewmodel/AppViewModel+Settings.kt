package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import org.audiocular.peq.app.ui.theme.*

/**
 * INDUSTRIAL SETTINGS ENGINE: pEQ NEURAL WORKSTATION
 * This extension handles workstation personalization, theme synchronization, and utility timers.
 */

fun AppViewModel.updateTheme(theme: pEQTheme) {
    setTheme(theme)
    persistSession()
}

fun AppViewModel.updateAccent(accent: pEQAccent) {
    setAccent(accent)
    persistSession()
}

fun AppViewModel.toggleSleepTimer(enabled: Boolean) { 
    sleepTimerActive.value = enabled
}

fun AppViewModel.setSleepTimer(minutes: Int) {
    sleepTimerMinutes.value = minutes
    sleepTimerActive.value = true
    sleepTimerRemainingSec.longValue = minutes * 60L
    
    cancelSleepTimerJob()
    val job = viewModelScope.launch {
        while (sleepTimerRemainingSec.longValue > 0 && sleepTimerActive.value) {
            delay(1000)
            sleepTimerRemainingSec.longValue -= 1
        }
        if (sleepTimerActive.value) {
            audioEngine.pause()
            isPlaying.value = false
            sleepTimerActive.value = false
            showToast("Sleep timer active: Playback stopped.")
        }
    }
    setSleepTimerJob(job)
}

fun AppViewModel.stopSleepTimer() {
    sleepTimerActive.value = false
    cancelSleepTimerJob()
}

fun AppViewModel.toggleBackgroundService(active: Boolean) {
    isBackgroundServiceRunning.value = active
}

fun AppViewModel.toggleMicMonitoring() {
    isMicMonitoringEnabled.value = !isMicMonitoringEnabled.value
    audioEngine.enableZeroLatencyMonitoring(isMicMonitoringEnabled.value)
}

fun AppViewModel.toggleDrumRecording() {
    isRecordingDrumSession.value = !isRecordingDrumSession.value
    if (isRecordingDrumSession.value) {
        showToast("Neural recording session initiated")
    } else {
        showToast("Session mastered and saved to /pEQ/Recordings")
    }
}

fun AppViewModel.toggleLockScreenMode() {
    isLockScreenMode.value = !isLockScreenMode.value
}

fun AppViewModel.toggleGapless(enabled: Boolean) {
    isGaplessEnabled.value = enabled
}

fun AppViewModel.toggleForceHiRes(enabled: Boolean) {
    isForceHiResEnabled.value = enabled
    audioEngine.setExclusiveMode(enabled)
}

fun AppViewModel.cycleRotation() {
    val current = videoRotationDegrees.value
    videoRotationDegrees.value = (current + 90) % 360
}

fun AppViewModel.resetRotation() {
    videoRotationDegrees.value = 0
}
