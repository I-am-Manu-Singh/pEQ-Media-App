package org.audiocular.peq.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import org.audiocular.peq.app.data.MediaType
import android.media.MediaMetadataRetriever
import android.os.Build

@Composable
actual fun pEQImage(
    data: String?,
    modifier: Modifier,
    contentScale: ContentScale,
    alpha: Float,
    mediaType: MediaType?
) {
    if (data == null || data.isEmpty()) {
        DefaultPlaceholder(modifier, mediaType)
        return
    }

    val context = LocalContext.current
    var bitmap by remember(data) { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(data) {
        withContext(Dispatchers.IO) {
            try {
                if (data.startsWith("content://")) {
                    val uri = Uri.parse(data)
                    // Try direct stream first (for album art)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                    
                    // If failed and it's a video URI, try to load thumbnail
                    if (bitmap == null && data.contains("video")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            bitmap = context.contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
                        }
                    }
                } else {
                    val file = File(data)
                    if (file.exists()) {
                        val extension = file.extension.lowercase()
                        if (extension == "mp4" || extension == "mkv" || extension == "mov" || extension == "webm") {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(file.absolutePath)
                            bitmap = retriever.getFrameAtTime(1000000) // 1 second in
                            retriever.release()
                        } else if (extension == "mp3" || extension == "flac" || extension == "wav" || extension == "m4a") {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(file.absolutePath)
                                val art = retriever.embeddedPicture
                                if (art != null) {
                                    bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                                }
                            } finally {
                                retriever.release()
                            }
                        } else {
                            bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        }
                    }
                }
            } catch (e: Exception) {
                println("pEQControl: [IMAGE] Load Failed: ${e.message}")
            }
        }
    }
    
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alpha = alpha
        )
    } ?: run {
        DefaultPlaceholder(modifier, mediaType)
    }
}

@Composable
private fun DefaultPlaceholder(modifier: Modifier, mediaType: MediaType?) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (mediaType == MediaType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )
    }
}
