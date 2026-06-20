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
        downloadMedia(url, null, true, onProgress, onComplete, onError)
    }

    override fun downloadMedia(
        url: String,
        formatId: String?,
        isAudioOnly: Boolean,
        onProgress: (Float, Long) -> Unit,
        onComplete: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val downloadDir = File(context.getExternalFilesDir(null), "pEQ/Cache")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            // Try updating yt-dlp to latest scripts to prevent HTTP 403 / decryption issues
            try {
                YoutubeDL.getInstance().updateYoutubeDL(context)
            } catch (e: Exception) {
                println("pEQControl: [CLOUD] yt-dlp auto-update failed: ${e.message}")
            }

            val request = YoutubeDLRequest(url).apply {
                if (formatId != null) {
                    addOption("-f", formatId)
                } else if (isAudioOnly) {
                    addOption("--extract-audio")
                    addOption("--audio-format", "wav")
                }
                addOption("--output", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(request) { progress, eta, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            onProgress(progress, eta)
                        }
                    }
                }
                
                // Find file in directory
                val targetExt = if (formatId == null && isAudioOnly) "wav" else null
                val finalFile = downloadDir.listFiles()
                    ?.filter { targetExt == null || it.extension.lowercase() == targetExt }
                    ?.maxByOrNull { it.lastModified() }

                withContext(Dispatchers.Main) {
                    if (finalFile != null && finalFile.exists()) {
                        // Register file with MediaStore
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(finalFile.absolutePath),
                            null
                        ) { path, uri ->
                            println("pEQControl: [SCANNER] Indexed download file: $path -> $uri")
                        }
                        onComplete(finalFile.absolutePath)
                    } else {
                        onError("File not found after extraction")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown Cloud Error")
                }
            }
        }
    }

    override suspend fun getStreamUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val info = YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url))
                info.url ?: info.formats?.find { it.ext == "mp4" || it.ext == "m4a" }?.url
            } catch (e: Exception) {
                println("pEQControl: [CLOUD] Stream resolution failed: ${e.message}")
                null
            }
        }
    }

    override suspend fun getFormats(url: String): List<FormatInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val info = YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url))
                info.formats?.filter { f ->
                    val ext = f.ext?.lowercase() ?: ""
                    val note = (f.formatNote ?: f.format ?: "").lowercase()
                    val acodec = f.acodec?.lowercase() ?: ""
                    val vcodec = f.vcodec?.lowercase() ?: ""

                    ext != "mhtml" && !ext.startsWith("sb") && !note.contains("storyboard") && 
                    !(acodec == "none" && vcodec == "none") && !ext.contains("manifest")
                }?.map { f ->
                    FormatInfo(
                        formatId = f.formatId ?: "",
                        ext = f.ext ?: "",
                        note = f.formatNote ?: f.format ?: "",
                        acodec = f.acodec,
                        vcodec = f.vcodec,
                        isAudio = f.vcodec == null || f.vcodec == "none",
                        isVideo = f.vcodec != null && f.vcodec != "none",
                        sizeBytes = f.fileSize
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                println("pEQControl: [CLOUD] Failed to resolve formats: ${e.message}")
                emptyList()
            }
        }
    }
}
