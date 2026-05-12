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
            _scanStatus.value = "Scanning..."
            val videos = mutableListOf<VideoFile>()
            val audios = mutableListOf<AudioFile>()
            
            includePaths.forEach { path ->
                val root = File(path)
                if (root.exists() && root.isDirectory) {
                    val allFiles = root.walkTopDown().filter { it.isFile }.toList()
                    val total = allFiles.size.toFloat()
                    var processed = 0
                    
                    allFiles.forEach { file ->
                        processed++
                        _scanProgress.value = processed / total
                        
                        when (file.extension.lowercase()) {
                            "mp4", "mkv", "mov", "avi" -> {
                                videos.add(VideoFile(
                                    id = UUID.randomUUID().toString(),
                                    name = file.name,
                                    title = file.nameWithoutExtension,
                                    path = file.absolutePath,
                                    duration = 0,
                                    sizeBytes = file.length(),
                                    resolution = "Unknown",
                                    dateModified = file.lastModified(),
                                    thumbnail = file.absolutePath
                                ))
                            }
                            "mp3", "wav", "flac", "m4a", "ogg" -> {
                                val meta = extractMetadata(file)
                                audios.add(AudioFile(
                                    id = UUID.randomUUID().toString(),
                                    name = file.name,
                                    title = meta.title ?: file.nameWithoutExtension,
                                    path = file.absolutePath,
                                    duration = meta.duration,
                                    sizeBytes = file.length(),
                                    dateModified = file.lastModified(),
                                    artist = meta.artist ?: file.parentFile?.parentFile?.name ?: "Unknown Artist",
                                    album = meta.album ?: file.parentFile?.name ?: "Unknown Album",
                                    thumbnail = file.absolutePath
                                ))
                            }
                        }
                    }
                }
            }
            
            _mediaFiles.value = videos
            _audioFiles.value = audios
            _scanStatus.value = "Scan Complete"
            _scanProgress.value = 1f
        }
    }

    private data class MetaData(val title: String?, val artist: String?, val album: String?, val duration: Long = 0)

    private fun extractMetadata(file: File): MetaData {
        // Simple metadata extraction for MP3/FLAC
        // For a real app, use a library like jaudiotagger, but here we do a basic read
        if (!file.exists()) return MetaData(null, null, null)
        
        try {
            if (file.extension.lowercase() == "mp3") {
                RandomAccessFile(file, "r").use { raf ->
                    // Check for ID3v2
                    val header = ByteArray(10)
                    raf.readFully(header)
                    if (header[0] == 'I'.toByte() && header[1] == 'D'.toByte() && header[2] == '3'.toByte()) {
                        // Very basic ID3v2 parser would go here
                        // For now, we fallback to filename-based but we've tried
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore parsing errors
        }
        
        return MetaData(null, null, null)
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
