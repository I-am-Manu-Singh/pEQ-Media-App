package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.audiocular.peq.app.data.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeoutOrNull

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
    scope.launch {
        while (isActive) {
            if (isSequencerActive.value) {
                val bpm = masterBpm.floatValue.coerceIn(40f, 240f)
                val msPerStep = (60_000 / bpm / 4).toLong() // 16th notes
                
                val step = (currentStep.intValue + 1) % 16
                currentStep.intValue = step
                
                if (sequencerSteps[step].value) {
                    audioEngine.playDrumSample("KICK", step)
                }
                
                // Add variety based on tutorial if active
                selectedTutorial.value?.let { 
                    // Simulate tutorial logic
                }

                delay(msPerStep)
            } else if (isMetronomeActive.value) {
                val bpm = masterBpm.floatValue.coerceIn(40f, 240f)
                val msPerBeat = (60_000 / bpm).toLong()
                audioEngine.playDrumSample("METRONOME_TICK")
                delay(msPerBeat)
            } else {
                delay(100)
            }
        }
    }
}

fun AppViewModel.tapTempo() {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    tapTimes.add(now)
    if (tapTimes.size > 8) tapTimes.removeAt(0)
    
    if (tapTimes.size >= 2) {
        val totalDiff = tapTimes.last() - tapTimes.first()
        val averageDiff = totalDiff / (tapTimes.size - 1)
        if (averageDiff in 250..1500) { // 40 to 240 BPM
            val newBpm = 60000f / averageDiff
            updateMasterBpm(newBpm)
        }
    }
}

fun AppViewModel.toggleDrumRecording() {
    isRecordingDrumSession.value = !isRecordingDrumSession.value
}
