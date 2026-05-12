package org.audiocular.peq.app.ai

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import kotlinx.coroutines.*
import org.audiocular.peq.app.ai.pEQCloudDownloader as IpEQCloudDownloader

/**
 * EXPERIMENTAL NEURAL BRIDGE: pEQ CLOUD DOWNLOADER
 * Uses YouTube-DL for high-fidelity stream extraction.
 */
class AndroidpEQCloudDownloader(private val context: Context) : IpEQCloudDownloader {

    override fun downloadAudio(url: String, onProgress: (Float, Long) -> Unit, onComplete: (String) -> Unit, onError: (String) -> Unit) {
        val downloadDir = File(context.getExternalFilesDir(null), "pEQ/Cache")
        if (!downloadDir.exists()) downloadDir.mkdirs()

        val request = YoutubeDLRequest(url).apply {
            addOption("--extract-audio")
            addOption("--audio-format", "wav")
            addOption("--output", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
        }

        try {
            val response = YoutubeDL.getInstance().execute(request) { progress, eta, _ ->
                onProgress(progress, eta)
            }
            
            val outputFile = File("${downloadDir.absolutePath}/${response.out.substringAfterLast("/")}")
            if (outputFile.exists()) {
                onComplete(outputFile.absolutePath)
            } else {
                onError("File not found after extraction")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Unknown Cloud Error")
        }
    }

    override suspend fun getStreamUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val info = YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url))
                info.url ?: info.formats.find { it.ext == "mp4" || it.ext == "m4a" }?.url
            } catch (e: Exception) {
                println("pEQControl: [CLOUD] Stream resolution failed: ${e.message}")
                null
            }
        }
    }
}
