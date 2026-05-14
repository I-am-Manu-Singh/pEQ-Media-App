package org.audiocular.peq.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.audiocular.peq.app.data.*
import java.io.File
import java.io.RandomAccessFile
import java.util.UUID

class DesktopFileScanner : pEQFileScanner {
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val factory = uk.co.caprica.vlcj.factory.MediaPlayerFactory()
    
    private val _mediaFiles = MutableStateFlow<List<VideoFile>>(emptyList())
    override val mediaFiles: StateFlow<List<VideoFile>> = _mediaFiles

    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    override val audioFiles: StateFlow<List<AudioFile>> = _audioFiles

    private val _scanProgress = MutableStateFlow(0f)
    override val scanProgress: StateFlow<Float> = _scanProgress

    private val _scanStatus = MutableStateFlow("Idle")
    override val scanStatus: StateFlow<String> = _scanStatus

    private var included = emptyList<String>()

    override fun scan(includePaths: List<String>, excludePaths: List<String>) {
        included = includePaths
        scope.launch {
            _scanStatus.value = "Scanning Archive..."
            val videos = mutableListOf<VideoFile>()
            val audios = mutableListOf<AudioFile>()
            
            includePaths.forEach { path ->
                val target = File(path)
                if (target.exists()) {
                    val filesToScan = if (target.isDirectory) {
                        target.walkTopDown().filter { it.isFile }.toList()
                    } else {
                        listOf(target)
                    }
                    
                    val total = filesToScan.size.toFloat()
                    var processed = 0
                    
                    filesToScan.forEach { file ->
                        processed++
                        if (total > 0) _scanProgress.value = processed / total
                        
                        val extension = file.extension.lowercase()
                        if (!listOf("mp3", "wav", "flac", "m4a", "ogg", "aac", "mp4", "mkv", "mov", "avi", "webm", "3gp").contains(extension)) return@forEach
                        
                        // Use VLC to probe metadata
                        val media = factory.media().newMedia(file.absolutePath)
                        val duration = media.info()?.duration() ?: 0L
                        
                        // We need to parse metadata
                        val meta = media.meta()
                        val title = meta?.get(uk.co.caprica.vlcj.media.Meta.TITLE) ?: file.nameWithoutExtension
                        val artist = meta?.get(uk.co.caprica.vlcj.media.Meta.ARTIST) ?: file.parentFile?.name ?: "Unknown Artist"
                        val album = meta?.get(uk.co.caprica.vlcj.media.Meta.ALBUM) ?: file.parentFile?.name ?: "Unknown Album"
                        
                        val thumbnailPath = findThumbnail(file) ?: extractEmbeddedArt(file)
                        
                        when (extension) {
                            "mp4", "mkv", "mov", "avi", "webm", "3gp" -> {
                                videos.add(VideoFile(
                                    id = "v_${file.absolutePath.hashCode()}",
                                    name = file.name,
                                    title = title,
                                    path = file.absolutePath,
                                    duration = duration,
                                    sizeBytes = file.length(),
                                    resolution = "HD", // Placeholder
                                    dateModified = file.lastModified(),
                                    thumbnail = thumbnailPath
                                ))
                            }
                            else -> {
                                audios.add(AudioFile(
                                    id = "a_${file.absolutePath.hashCode()}",
                                    name = file.name,
                                    title = title,
                                    path = file.absolutePath,
                                    duration = duration,
                                    sizeBytes = file.length(),
                                    dateModified = file.lastModified(),
                                    artist = artist,
                                    album = album,
                                    format = extension.uppercase(),
                                    thumbnail = thumbnailPath
                                ))
                            }
                        }
                        media.release()
                    }
                }
            }
            
            _mediaFiles.value = videos
            _audioFiles.value = audios
            _scanStatus.value = "Scan Complete"
            _scanProgress.value = 1f
        }
    }

    private fun extractEmbeddedArt(file: File): String? {
        // VLCJ doesn't easily extract art to a file without playing, 
        // but we can look for sidecar files as a fallback in findThumbnail.
        return null 
    }

    private fun findThumbnail(mediaFile: File): String? {
        val folder = mediaFile.parentFile ?: return null
        val patterns = listOf("cover", "folder", "album", "artwork", "front")
        val extensions = listOf("jpg", "jpeg", "png", "webp")
        
        // 1. Try to find an image with the same name as the media file
        extensions.forEach { ext ->
            val img = File(folder, "${mediaFile.nameWithoutExtension}.$ext")
            if (img.exists()) return img.absolutePath
        }
        
        // 2. Try common folder patterns
        folder.listFiles()?.forEach { file ->
            if (file.isFile) {
                val name = file.nameWithoutExtension.lowercase()
                val ext = file.extension.lowercase()
                if (name in patterns && ext in extensions) {
                    return file.absolutePath
                }
            }
        }
        
        return null
    }

    override fun setExcludedFolders(folders: List<String>) {}
    override fun setIncludedFolders(folders: List<String>) {
        included = folders
        scan(folders, emptyList())
    }
    override fun scanNetworkDrives(host: String) {}
    override fun refresh() {
        scan(included, emptyList())
    }
    override fun renameFile(file: VideoFile, newName: String) {}
    override fun renameAudioFile(file: AudioFile, newName: String) {}
    override fun deleteFile(file: VideoFile) {
        File(file.path).delete()
        refresh()
    }
    override fun deleteAudioFile(file: AudioFile) {
        File(file.path).delete()
        refresh()
    }
    override fun shareFile(file: VideoFile) {}
    override fun shareAudioFile(file: AudioFile) {}
    override fun exportPlaylist(name: String, tracks: List<AudioFile>) {}
    override fun readFileContent(path: String): String? {
        return try { File(path).readText() } catch (e: Exception) { null }
    }

    override fun listFiles(path: String): List<String> {
        return try {
            File(path).listFiles()?.map { it.absolutePath } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
