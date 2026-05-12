package org.audiocular.peq.app.ai

import kotlinx.coroutines.flow.StateFlow

/**
 * NEURAL STEM ENGINE: pEQ SPLITTER
 * The primary contract for high-fidelity audio decoupling (Vocals, Drums, Bass, etc).
 */
interface pEQStemSplitter {
    val isProcessing: StateFlow<Boolean>
    val progress: StateFlow<Float>
    
    suspend fun splitTrack(inputPath: String, outputDir: String): Map<String, String> // Returns Map of StemName to FilePath
    suspend fun splitBatch(inputPaths: List<String>, outputDir: String): Map<String, Map<String, String>>
    suspend fun classifyGenre(inputPath: String): String
    suspend fun upscaleAudio(inputPath: String): String
}
