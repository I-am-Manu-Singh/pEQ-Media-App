package org.audiocular.peq.app.ai

/**
 * EXPERIMENTAL NEURAL BRIDGE: pEQ CLOUD DOWNLOADER
 * Interface for high-fidelity stream extraction.
 */
interface pEQCloudDownloader {
    fun downloadAudio(url: String, onProgress: (Float, Long) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit)
    suspend fun getStreamUrl(url: String): String?
}
