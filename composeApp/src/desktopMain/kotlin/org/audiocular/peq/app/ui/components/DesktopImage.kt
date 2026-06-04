package org.audiocular.peq.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.audiocular.peq.app.data.MediaType
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

@Composable
actual fun pEQImage(
    data: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    alpha: Float,
    mediaType: MediaType?,
    fallbackPath: String?
) {

    var imageBitmap by remember(data, fallbackPath) {
        mutableStateOf<ImageBitmap?>(null)
    }

    var isLoading by remember(data, fallbackPath) {
        mutableStateOf(false)
    }

    LaunchedEffect(data, fallbackPath) {

        if (data.isNullOrBlank() && fallbackPath.isNullOrBlank()) {
            imageBitmap = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true

        imageBitmap = withContext(Dispatchers.IO) {
            try {
                // Helper to try reading a path as direct image
                fun tryLoadDirect(path: String?): ImageBitmap? {
                    if (path.isNullOrBlank()) return null
                    val cleaned = path.replace("file://", "")
                    if (path.startsWith("http")) {
                        return try {
                            ImageIO.read(URL(path))?.toComposeImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val file = File(cleaned)
                    if (file.exists() && file.isFile) {
                        try {
                            val img = ImageIO.read(file)
                            if (img != null) {
                                return img.toComposeImageBitmap()
                            }
                        } catch (_: Exception) {
                            // might not be a direct image format, could be audio
                        }
                    }
                    return null
                }

                // 1. Try loading primary data direct
                var bmp = tryLoadDirect(data)
                if (bmp != null) return@withContext bmp

                // 2. Try loading fallbackPath direct
                bmp = tryLoadDirect(fallbackPath)
                if (bmp != null) return@withContext bmp

                // Helper to extract embedded artwork
                fun tryLoadEmbedded(path: String?): ImageBitmap? {
                    if (path.isNullOrBlank()) return null
                    val cleaned = path.replace("file://", "")
                    val file = File(cleaned)
                    if (file.exists() && file.isFile) {
                        val extension = file.extension.lowercase()
                        if (extension in setOf("mp3", "flac", "wav", "m4a", "ogg", "wma", "aac")) {
                            return extractEmbeddedArt(file)
                        }
                    }
                    return null
                }

                // 3. Try scanning embedded art from data
                bmp = tryLoadEmbedded(data)
                if (bmp != null) return@withContext bmp

                // 4. Try scanning embedded art from fallbackPath
                bmp = tryLoadEmbedded(fallbackPath)
                if (bmp != null) return@withContext bmp

                null
            } catch (e: Exception) {
                println("pEQControl: Thumbnail load failed -> ${e.message}")
                null
            }
        }

        isLoading = false
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val bitmap = imageBitmap

        if (bitmap != null) {

            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alpha = alpha
            )

        } else if (!isLoading) {

            Icon(
                imageVector =
                    if (mediaType == MediaType.VIDEO) {
                        Icons.Default.VideoLibrary
                    } else {
                        Icons.Default.MusicNote
                    },

                contentDescription = null,

                tint = MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.25f),

                modifier = Modifier.fillMaxSize(0.35f)
            )
        }
    }
}

private fun extractEmbeddedArt(file: File): ImageBitmap? {
    try {
        if (!file.exists()) return null
        val bytes = file.inputStream().use { stream ->
            // Read at most 6MB to avoid out-of-memory
            val buffer = ByteArray(6 * 1024 * 1024)
            val bytesRead = stream.read(buffer)
            if (bytesRead <= 0) return null
            buffer.copyOf(bytesRead)
        }
        
        // PNG magic bytes
        val pngMagic = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte())
        val pngIndex = findSubarray(bytes, pngMagic)
        if (pngIndex != -1) {
            val bis = java.io.ByteArrayInputStream(bytes, pngIndex, bytes.size - pngIndex)
            ImageIO.read(bis)?.toComposeImageBitmap()?.let { return it }
        }

        // JPEG magic bytes
        val jpegMagic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        var startSearch = 0
        while (startSearch < bytes.size - 3) {
            val jpegIndex = findSubarray(bytes, jpegMagic, startSearch)
            if (jpegIndex == -1) break
            try {
                val bis = java.io.ByteArrayInputStream(bytes, jpegIndex, bytes.size - jpegIndex)
                val img = ImageIO.read(bis)
                if (img != null) {
                    return img.toComposeImageBitmap()
                }
            } catch (_: Exception) {}
            startSearch = jpegIndex + 1
        }
    } catch (e: Exception) {
        println("pEQControl: Failed to extract embedded art: ${e.message}")
    }
    return null
}

private fun findSubarray(array: ByteArray, sub: ByteArray, start: Int = 0): Int {
    if (sub.isEmpty() || array.size < sub.size) return -1
    for (i in start..array.size - sub.size) {
        var match = true
        for (j in sub.indices) {
            if (array[i + j] != sub[j]) {
                match = false
                break
            }
        }
        if (match) return i
    }
    return -1
}