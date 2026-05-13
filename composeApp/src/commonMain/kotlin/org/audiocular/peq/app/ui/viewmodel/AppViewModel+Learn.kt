package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.audiocular.peq.app.data.*

/**
 * INDUSTRIAL LEARN ENGINE: pEQ NEURAL WORKSTATION
 * This extension manages neural drum kits, tuner telemetry, and harmonic analysis.
 */

fun AppViewModel.switchDrumKit(kit: DrumKit) {
    selectedDrumKit.value = kit
    drumPadLabels.clear()
    drumPadLabels.addAll(kit.labels)
}

fun AppViewModel.startTutorial(pattern: DrumTutorial) { 
    selectedTutorial.value = pattern 
}

fun AppViewModel.remapPad(index: Int, sample: String) {
    if (index in drumPadLabels.indices) {
        drumPadLabels[index] = sample
    }
}

fun AppViewModel.updateDrumPadVolume(index: Int, volume: Float) {
    drumPadVolumes[index] = volume
}

fun AppViewModel.toggleSequencerStep(step: Int) {
    if (step in sequencerSteps.indices) {
        sequencerSteps[step].value = !sequencerSteps[step].value
    }
}

fun AppViewModel.toggleSequencerStep(step: Int, type: String) {
    val list = stepSequencer.getOrPut(step) { mutableStateListOf() }
    if (list.contains(type)) list.remove(type) else list.add(type)
}

fun AppViewModel.startSequencer() { 
    isSequencerActive.value = true 
}

fun AppViewModel.stopSequencer() { 
    isSequencerActive.value = false 
}

// Tuner & Harmonic Analysis
fun AppViewModel.onTunerPitchDetected(pitch: Float, note: String, cents: Int) {
    detectedPitch.floatValue = pitch
    detectedNote.value = note
    centsDeviation.intValue = cents
}

fun AppViewModel.onChordDetected(chord: String) {
    detectedChord.value = chord
}

fun AppViewModel.toggleTuner(active: Boolean) {
    isTunerActive.value = active
    if (active) audioEngine.startTuner() else audioEngine.stopTuner()
}

fun AppViewModel.toggleMetronome(active: Boolean) {
    isMetronomeActive.value = active
    audioEngine.setMetronomeEnabled(active)
}

fun AppViewModel.updateMasterBpm(bpm: Float) {
    masterBpm.floatValue = bpm
    audioEngine.setBpm(bpm)
}

fun AppViewModel.startKitBuilding() {
    isKitBuilderActive.value = true
}

fun AppViewModel.observeMetronome() {
    viewModelScope.launch {
        while (true) {
            if (isMetronomeActive.value) {
                val bpm = masterBpm.floatValue.coerceIn(40f, 240f)
                val msPerBeat = (60_000 / bpm).toLong()
                
                audioEngine.playDrumSample("HIHAT")
                delay(msPerBeat)
            } else {
                delay(250)
            }
        }
    }
}

fun AppViewModel.tapTempo() {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    tapTimes.add(now)
    if (tapTimes.size > 4) tapTimes.removeAt(0)
    
    if (tapTimes.size >= 2) {
        val averageDiff = (tapTimes.last() - tapTimes.first()) / (tapTimes.size - 1)
        if (averageDiff > 0) {
            val newBpm = 60000f / averageDiff
            masterBpm.floatValue = newBpm.coerceIn(40f, 240f)
        }
    }
}
