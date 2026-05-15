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
    mediaType: MediaType?
) {

    var imageBitmap by remember(data) {
        mutableStateOf<ImageBitmap?>(null)
    }

    var isLoading by remember(data) {
        mutableStateOf(false)
    }

    LaunchedEffect(data) {

        if (data.isNullOrBlank()) {

            imageBitmap = null
            isLoading = false

            return@LaunchedEffect
        }

        isLoading = true

        imageBitmap = withContext(Dispatchers.IO) {

            try {

                val cleaned =
                    data.replace("file://", "")

                /*
                 * REMOTE IMAGE
                 */
                if (data.startsWith("http")) {

                    return@withContext ImageIO
                        .read(URL(data))
                        ?.toComposeImageBitmap()
                }

                val file = File(cleaned)

                /*
                 * DIRECT IMAGE
                 */
                if (
                    file.exists() &&
                    file.isFile
                ) {

                    try {

                        ImageIO.read(file)
                            ?.toComposeImageBitmap()

                    } catch (_: Exception) {

                        null
                    }

                } else {

                    null
                }

            } catch (e: Exception) {

                println(
                    "pEQControl: Thumbnail load failed -> ${e.message}"
                )

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