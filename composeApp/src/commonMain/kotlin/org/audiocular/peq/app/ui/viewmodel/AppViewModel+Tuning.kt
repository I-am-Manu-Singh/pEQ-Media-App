package org.audiocular.peq.app.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.audiocular.peq.app.data.*

/**
 * TUNING ENGINE EXTENSION: pEQ NEURAL WORKSTATION
 * Encapsulates DSP mastering, EQ profiles, and AutoEQ integration.
 */

fun AppViewModel.searchAutoEqRepository(query: String) {
    if (query.length < 2) {
        autoEqSearchResults.clear()
        return
    }
    
    viewModelScope.launch {
        isAutoEqLoading.value = true
        try {
            // Simulate neural lookup delay
            delay(400)
            // In a real app, this would query a database or API
            val mockResults = listOf(
                AutoEqProfile("WH-1000XM4", "Sony", "Over-ear"),
                AutoEqProfile("HD 600", "Sennheiser", "Open-back"),
                AutoEqProfile("AirPods Pro 2", "Apple", "In-ear"),
                AutoEqProfile("DT 990 Pro", "Beyerdynamic", "Over-ear")
            ).filter { it.name.contains(query, ignoreCase = true) }
            
            autoEqSearchResults.clear()
            autoEqSearchResults.addAll(mockResults)
        } finally {
            isAutoEqLoading.value = false
        }
    }
}

fun AppViewModel.applyAutoEqProfile(profile: AutoEqProfile) {
    selectedAutoEqProfile.value = profile
    viewModelScope.launch {
        showToast("Neural Profile: ${profile.name} Applied")
        // Simulate applying EQ curves
        for (i in 0 until 10) {
            val randomGain = (-3..3).random().toFloat()
            updateEqBand(i, randomGain)
            delay(50)
        }
    }
}

fun AppViewModel.importIr() {
    viewModelScope.launch {
        showToast("Ingesting Impulse Response...")
        delay(800)
        convolverIrName.value = "Custom Studio (WAV)"
        isConvolverActive.value = true
        convolverIrDryWet.floatValue = 0.5f
    }
}

fun AppViewModel.toggleSubHarmonicGenerator(enabled: Boolean) {
    isSubHarmonicActive.value = enabled
    audioEngine.setSubHarmonicGenerator(enabled)
}

fun AppViewModel.toggleNeuralSaturation(enabled: Boolean) {
    isNeuralSaturationEnabled.value = enabled
    audioEngine.setNeuralSaturation(enabled)
}

fun AppViewModel.onEQPointDragged(freq: Float, gain: Float) {
    val index = eqFrequencies.indices.minByOrNull { i ->
        val bandFreq = eqFrequencies[i].replace("k", "000").replace("Hz", "").trim().toFloat()
        kotlin.math.abs(bandFreq - freq)
    } ?: 0
    
    if (index in eqGains.indices) {
        eqGains[index] = gain
        audioEngine.updateEqBand(index, gain)
    }
}

fun AppViewModel.updateEqBand(index: Int, gain: Float) {
    if (index in eqGains.indices) {
        eqGains[index] = gain
        audioEngine.updateEqBand(index, gain)
    }
}

fun AppViewModel.updateEqQ(index: Int, q: Float) {
    audioEngine.updateEqQ(index, q)
}

fun AppViewModel.setCompressorParams(threshold: Float, ratio: Float, attack: Float, release: Float) {
    compressorThreshold.floatValue = threshold
    compressorRatio.value = ratio
    compressorAttackMs.floatValue = attack
    compressorReleaseMs.floatValue = release
    audioEngine.updateCompressor(threshold, ratio, attack, release)
}
