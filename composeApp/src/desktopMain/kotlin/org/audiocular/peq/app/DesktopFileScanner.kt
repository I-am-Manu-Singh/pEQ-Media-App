package org.audiocular.peq.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.audiocular.peq.app.data.*
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.Meta
import java.io.File

class DesktopFileScanner : pEQFileScanner {

    private val scope =
        CoroutineScope(Dispatchers.IO)

    private val factory by lazy {

        MediaPlayerFactory(

            "--quiet",

            "--no-video-title-show",

            "--avcodec-hw=none"
        )
    }

    /*
     * STATE
     */
    private val _mediaFiles =
        MutableStateFlow<List<VideoFile>>(emptyList())

    override val mediaFiles:
            StateFlow<List<VideoFile>> =
        _mediaFiles

    private val _audioFiles =
        MutableStateFlow<List<AudioFile>>(emptyList())

    override val audioFiles:
            StateFlow<List<AudioFile>> =
        _audioFiles

    private val _scanProgress =
        MutableStateFlow(0f)

    override val scanProgress:
            StateFlow<Float> =
        _scanProgress

    private val _scanStatus =
        MutableStateFlow("Idle")

    override val scanStatus:
            StateFlow<String> =
        _scanStatus

    private var included =
        emptyList<String>()

    /*
     * CLEAN METADATA
     */
    private fun cleanMeta(
        value: String?
    ): String? {

        val cleaned =
            value
                ?.trim()
                ?.replace("\u0000", "")
                ?.replace("_", " ")

        return cleaned?.takeIf {

            it.isNotBlank() &&
            it.lowercase() != "unknown"
        }
    }

    /*
     * CLEAN FILENAME
     */
    private fun cleanFilename(
        file: File
    ): String {

        return file.nameWithoutExtension
            .replace("_", " ")
            .replace(".", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /*
     * SCAN
     */
    override fun scan(
        includePaths: List<String>,
        excludePaths: List<String>
    ) {

        included = includePaths

        scope.launch {

            _scanStatus.value =
                "Scanning Archive..."

            val videos =
                mutableListOf<VideoFile>()

            val audios =
                mutableListOf<AudioFile>()

            val supportedExtensions =
                setOf(

                    "mp3",
                    "wav",
                    "flac",
                    "m4a",
                    "ogg",
                    "aac",

                    "mp4",
                    "mkv",
                    "mov",
                    "avi",
                    "webm",
                    "3gp"
                )

            val excluded =
                excludePaths.map {
                    File(it).absolutePath
                }

            val allFiles =
                ArrayList<File>(5000)

            includePaths.forEach { path ->

                val root =
                    File(path)

                if (!root.exists()) {
                    return@forEach
                }

                if (root.isFile) {

                    allFiles.add(root)

                } else {

                    root.walkTopDown()
                        .onEnter { dir ->

                            excluded.none {

                                dir.absolutePath
                                    .startsWith(it)
                            }
                        }
                        .filter { file ->

                            file.isFile &&
                            file.extension.lowercase() in supportedExtensions
                        }
                        .forEach {

                            allFiles.add(it)
                        }
                }
            }

            val total =
                allFiles.size
                    .coerceAtLeast(1)

            allFiles.forEachIndexed { index, file ->

                try {

                    _scanProgress.value =
                        (index + 1).toFloat() /
                        total.toFloat()

                    _scanStatus.value =
                        "Scanning ${file.name}"

                    val extension =
                        file.extension.lowercase()

                    val isAudio = extension in setOf("mp3", "flac", "wav", "m4a", "ogg", "wma", "aac")
                    var title: String? = null
                    var artist: String? = null
                    var album: String? = null
                    var duration = 0L
                    var thumbnail: String? = null
                    var jaudiotaggerSuccess = false

                    if (isAudio) {
                        try {
                            val audioFile = org.jaudiotagger.audio.AudioFileIO.read(file)
                            val tag = audioFile.tag
                            val header = audioFile.audioHeader
                            duration = (header?.trackLength?.toLong() ?: 0L) * 1000L
                            
                            title = cleanMeta(tag?.getFirst(org.jaudiotagger.tag.FieldKey.TITLE))
                            artist = cleanMeta(tag?.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST))
                            album = cleanMeta(tag?.getFirst(org.jaudiotagger.tag.FieldKey.ALBUM))
                            
                            val artwork = tag?.firstArtwork
                            if (artwork != null) {
                                val binaryData = artwork.binaryData
                                if (binaryData != null) {
                                    val cacheDir = File(System.getProperty("java.io.tmpdir"))
                                    val cacheFile = File(cacheDir, "peq_art_${file.nameWithoutExtension.hashCode()}.jpg")
                                    cacheFile.writeBytes(binaryData)
                                    thumbnail = "file://${cacheFile.absolutePath}"
                                }
                            }
                            jaudiotaggerSuccess = true
                        } catch (e: Exception) {
                            println("pEQControl: jaudiotagger failed for ${file.name}: ${e.message}")
                        }
                    }

                    if (!jaudiotaggerSuccess) {
                        try {
                            val media =
                                factory.media()
                                    .newMedia(
                                        file.absolutePath
                                    )

                            try {
                                /*
                                 * SAFE PARSE
                                 */
                                try {
                                    media.parsing()
                                        .parse()

                                    Thread.sleep(100)
                                } catch (_: Exception) {
                                    println(
                                        "pEQControl: Metadata timeout -> ${file.name}"
                                    )
                                }

                                val meta =
                                    media.meta()

                                duration =
                                    media.info()
                                        ?.duration()
                                        ?: 0L

                                val parsedName =
                                    cleanFilename(file)

                                /*
                                 * TITLE
                                 */
                                title =
                                    cleanMeta(
                                        meta?.get(Meta.TITLE)
                                    )
                                        ?: parsedName

                                /*
                                 * ARTIST
                                 */
                                val filenameArtist =
                                    parsedName
                                        .substringBefore("-")
                                        .trim()
                                        .takeIf {
                                            parsedName.contains("-")
                                        }

                                artist =
                                    cleanMeta(
                                        meta?.get(Meta.ARTIST)
                                    )
                                        ?: filenameArtist
                                        ?: "Unknown Artist"

                                /*
                                 * ALBUM
                                 */
                                album =
                                    cleanMeta(
                                        meta?.get(Meta.ALBUM)
                                    )
                                        ?: file.parentFile
                                            ?.name
                                            ?.replace("_", " ")
                                            ?.replace(".", " ")
                                        ?: "Unknown Album"

                                /*
                                 * THUMBNAIL
                                 */
                                thumbnail =
                                    findThumbnail(file, media)
                                        ?.let {
                                            "file://$it"
                                        }

                            } catch (e: Exception) {
                                println(
                                    "pEQControl: Metadata parse failed -> ${file.name}"
                                )
                                e.printStackTrace()
                            } finally {
                                media.release()
                            }
                        } catch (e: Exception) {
                            println("pEQControl: VLC media creation failed -> ${e.message}")
                        }
                    }

                    if (thumbnail == null) {
                        thumbnail = findThumbnail(file)
                            ?.let {
                                "file://$it"
                            }
                    }

                    val parsedName = cleanFilename(file)
                    val finalTitle = title ?: parsedName
                    val finalArtist = artist ?: "Unknown Artist"
                    val finalAlbum = album ?: "Unknown Album"

                    try {
                        /*
                         * VIDEO
                         */
                        if (
                            extension in setOf(
                                "mp4",
                                "mkv",
                                "mov",
                                "avi",
                                "webm",
                                "3gp"
                            )
                        ) {
                            videos.add(
                                VideoFile(
                                    id = "v_${file.absolutePath.hashCode()}",
                                    name = file.name,
                                    title = finalTitle,
                                    path = file.absolutePath,
                                    duration = duration,
                                    sizeBytes = file.length(),
                                    resolution = "1080p", // Standard fallback resolution
                                    dateModified = file.lastModified(),
                                    thumbnail = thumbnail
                                )
                            )

                        } else {

                            /*
                             * AUDIO
                             */
                            audios.add(
                                AudioFile(
                                    id = "a_${file.absolutePath.hashCode()}",
                                    name = file.name,
                                    title = finalTitle,
                                    path = file.absolutePath,
                                    duration = duration,
                                    sizeBytes = file.length(),
                                    dateModified = file.lastModified(),
                                    artist = finalArtist,
                                    album = finalAlbum,
                                    format = extension.uppercase(),
                                    thumbnail = thumbnail
                                )
                            )
                        }

                    } catch (e: Exception) {

                        println(
                            "pEQControl: File mapping failed -> ${file.name}"
                        )

                        e.printStackTrace()
                    }

                } catch (e: Exception) {

                    println(
                        "pEQControl: [SCAN] Failed -> ${file.absolutePath}"
                    )

                    e.printStackTrace()
                }
            }

            /*
             * SORT
             */
            _audioFiles.value =
                audios.sortedBy {
                    it.title
                        .orEmpty()
                        .lowercase()
                }

            _mediaFiles.value =
                videos.sortedBy {
                    it.title
                        .orEmpty()
                        .lowercase()
                }

            _scanProgress.value = 1f

            _scanStatus.value =
                "Scan Complete (${audios.size + videos.size} files)"
        }
    }

    /*
     * RESOLUTION
     */
    private fun extractResolution(
        media: uk.co.caprica.vlcj.media.Media
    ): String {

        return try {

            val track =
                media.info()
                    ?.videoTracks()
                    ?.firstOrNull()

            val width =
                track?.width() ?: 0

            val height =
                track?.height() ?: 0

            if (
                width > 0 &&
                height > 0
            ) {

                "${width}x$height"

            } else {

                "Unknown"
            }

        } catch (_: Exception) {

            "Unknown"
        }
    }

    /*
     * THUMBNAILS
     */
    private fun findThumbnail(
        mediaFile: File,
        media: uk.co.caprica.vlcj.media.Media? = null
    ): String? {

        val folder =
            mediaFile.parentFile
                ?: return null

        val extensions =
            listOf(
                "jpg",
                "jpeg",
                "png",
                "webp"
            )

        /*
         * SAME NAME
         */
        extensions.forEach { ext ->

            val sameName =
                File(
                    folder,
                    "${mediaFile.nameWithoutExtension}.$ext"
                )

            if (
                sameName.exists() &&
                sameName.isFile
            ) {

                return sameName.absolutePath
            }
        }

        /*
         * VLC EMBEDDED ARTWORK
         */
        val vlcArt = media?.meta()?.get(Meta.ARTWORK_URL)
        if (!vlcArt.isNullOrBlank()) {
            val artPath = vlcArt.replace("file://", "")
            if (File(artPath).exists()) {
                return artPath
            }
        }

        /*
         * COMMON COVER NAMES
         */
        val patterns =
            listOf(
                "cover",
                "folder",
                "album",
                "artwork",
                "front",
                "poster",
                "thumb",
                "thumbnail"
            )

        folder.listFiles()
            ?.forEach { file ->

                if (!file.isFile) {
                    return@forEach
                }

                val name =
                    file.nameWithoutExtension
                        .lowercase()

                val ext =
                    file.extension
                        .lowercase()

                if (
                    (ext in extensions) &&
                    patterns.any {
                        name.contains(it)
                    }
                ) {

                    return file.absolutePath
                }
            }

        /*
         * FALLBACK FIRST IMAGE
         */
        return folder.listFiles()
            ?.firstOrNull {

                it.isFile &&
                it.extension.lowercase() in extensions
            }
            ?.absolutePath
    }

    /*
     * SETTINGS
     */
    override fun setExcludedFolders(
        folders: List<String>
    ) = Unit

    override fun setIncludedFolders(
        folders: List<String>
    ) {

        included = folders

        scan(
            folders,
            emptyList()
        )
    }

    override fun scanNetworkDrives(
        host: String
    ) = Unit

    override fun refresh() {

        scan(
            included,
            emptyList()
        )
    }

    /*
     * FILE OPS
     */
    override fun renameFile(
        file: VideoFile,
        newName: String
    ) = Unit

    override fun renameAudioFile(
        file: AudioFile,
        newName: String
    ) = Unit

    override fun deleteFile(
        file: VideoFile
    ) {

        File(file.path).delete()

        refresh()
    }

    override fun deleteAudioFile(
        file: AudioFile
    ) {

        File(file.path).delete()

        refresh()
    }

    override fun shareFile(
        file: VideoFile
    ) = Unit

    override fun shareAudioFile(
        file: AudioFile
    ) = Unit

    override fun exportPlaylist(
        name: String,
        tracks: List<AudioFile>
    ) = Unit

    /*
     * FILE ACCESS
     */
    override fun readFileContent(
        path: String
    ): String? {

        return try {

            File(path).readText()

        } catch (_: Exception) {

            null
        }
    }

    override fun readResourceContent(
        name: String
    ): String? {
        return try {
            this::class.java.classLoader.getResourceAsStream(name)?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    override fun listFiles(
        path: String
    ): List<String> {

        return try {

            File(path)
                .listFiles()
                ?.map {
                    it.absolutePath
                }
                ?: emptyList()

        } catch (_: Exception) {

            emptyList()
        }
    }

    /*
     * RELEASE
     */
    fun release() {

        factory.release()
    }
}