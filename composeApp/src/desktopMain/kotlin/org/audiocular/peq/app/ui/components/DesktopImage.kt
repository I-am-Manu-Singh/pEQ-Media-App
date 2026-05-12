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
    mediaType: MediaType?
) {
    var imageBitmap by remember(data) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var isLoading by remember(data) { mutableStateOf(data != null) }

    LaunchedEffect(data) {
        if (data == null) {
            isLoading = false
            imageBitmap = null
            return@LaunchedEffect
        }
        
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val cleanPath = data.replace("file://", "")
                val file = File(cleanPath)
                
                val bitmap = if (data.startsWith("http")) {
                    ImageIO.read(URL(data))?.toComposeImageBitmap()
                } else if (file.exists()) {
                    ImageIO.read(file)?.toComposeImageBitmap()
                } else {
                    // Fallback sidecar
                    val parent = file.parentFile
                    val sidecar = parent?.listFiles()?.find { f ->
                        val name = f.nameWithoutExtension.lowercase()
                        name in listOf("cover", "folder", "album", "artwork")
                    }
                    sidecar?.let { ImageIO.read(it)?.toComposeImageBitmap() }
                }
                imageBitmap = bitmap
            } catch (e: Exception) {
                // Silent fail for images
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alpha = alpha
            )
        } ?: run {
            if (!isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mediaType == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize(0.4f)
                    )
                }
            }
        }
    }
}
