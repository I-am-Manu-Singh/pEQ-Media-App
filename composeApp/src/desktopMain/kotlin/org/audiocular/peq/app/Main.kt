package org.audiocular.peq.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import org.audiocular.peq.app.audio.DesktopAudioEngine
import org.audiocular.peq.app.ui.viewmodel.*
import java.awt.Taskbar
import java.io.File
import org.audiocular.peq.app.ui.widgets.pEQWidgetCommand
import javax.imageio.ImageIO

fun main(args: Array<String>) {

    /*
     * macOS FIXES
     */
   System.setProperty(
    "sun.awt.disableMixing",
    "true"
)
   System.setProperty(
    "compose.interop.blending",
    "true"
)
    val isMacOs =
        System.getProperty("os.name")
            .lowercase()
            .contains("mac")

    System.setProperty(
        "skiko.renderApi",
        if (isMacOs) "METAL" else "OPENGL"
    )

    System.setProperty(
        "apple.awt.application.name",
        "pEQ Neural Workstation"
    )

    System.setProperty(
        "com.apple.mrj.application.apple.menu.about.name",
        "pEQ Neural Workstation"
    )

    System.setProperty(
        "apple.awt.UIElement",
        "false"
    )



    System.setProperty(
        "apple.laf.useScreenMenuBar",
        "true"
    )

    application {

        /*
         * SINGLE ENGINE INSTANCE
         */
        val audioEngine = remember {
            DesktopAudioEngine()
        }

        /*
         * VIEWMODEL
         */
        val viewModel = remember {
            AppViewModel(
                audioEngine = audioEngine,
                persistenceManager = DesktopPersistenceManager(),
                cloudDownloader = null
            ).apply {
                scanner = DesktopFileScanner()
                platformStorageBase.value = System.getProperty("user.home")
                
                // Dynamic macOS Open File Handler
                if (isMacOs) {
                    try {
                        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.APP_OPEN_FILE)) {
                            java.awt.Desktop.getDesktop().setOpenFileHandler { event ->
                                val files = event.files
                                if (!files.isNullOrEmpty()) {
                                    val path = files[0].absolutePath
                                    playExternalFile(path)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("pEQControl: [macOS] Failed to register OpenFilesHandler: ${e.message}")
                    }
                }

                /*
                 * FOLDER PICKER
                 */
                onPickFolderRequested = {

                    DesktopFilePicker
                        .pickFolder(
                            "Select Media Folder"
                        )
                        ?.let { folder ->

                            addScanFolder(folder)
                        }
                }

                /*
                 * SUBTITLE PICKER
                 */
                onPickSubtitleRequested = {

                    DesktopFilePicker
                        .pickFile(
                            "Select Subtitle File",
                            listOf("srt", "vtt")
                        )
                        ?.let { path ->

                            importSubtitleFile(path)
                        }
                }

                /*
                 * CHAPTER PICKER
                 */
                onPickChaptersRequested = {

                    DesktopFilePicker
                        .pickFile(
                            "Select Chapter File",
                            listOf("txt", "chapter", "lrc")
                        )
                        ?.let { path ->

                            importChapterFile(path)
                        }
                }

                /*
                 * STARTUP FILE
                 */
                if (args.isNotEmpty()) {

                    val path =
                        args[0]

                    if (File(path).exists()) {

                        playExternalFile(path)
                    }
                }
            }
        }

        /*
         * ICON
         */
        val icon =
            painterResource("app_icon.png")

        /*
         * SPLASH DELAY
         */
        LaunchedEffect(Unit) {

            delay(1500)

            viewModel.completeSplash()
        }

        /*
         * macOS DOCK ICON
         */
        LaunchedEffect(Unit) {

            try {

                if (Taskbar.isTaskbarSupported()) {

                    val taskbar =
                        Taskbar.getTaskbar()

                    if (
                        taskbar.isSupported(
                            Taskbar.Feature.ICON_IMAGE
                        )
                    ) {

                        val resource =
                            Thread.currentThread()
                                .contextClassLoader
                                .getResource(
                                    "app_icon.png"
                                )

                        if (resource != null) {

                            val image =
                                ImageIO.read(resource)

                            taskbar.iconImage =
                                image
                        }
                    }
                }

            } catch (e: Exception) {

                println(
                    "pEQControl: [SYSTEM] Failed to set dock icon -> ${e.message}"
                )
            }
        }

        /*
         * WINDOW VISIBILITY
         */
        var isVisible by remember {
            mutableStateOf(true)
        }

        /*
         * WINDOW STATE
         */
        val windowState =
            rememberWindowState(

                width =
                    if (viewModel.isPiPMode.value)
                        320.dp
                    else
                        480.dp,

                height =
                    if (viewModel.isPiPMode.value)
                        180.dp
                    else
                        920.dp,

                position =
                    WindowPosition.Aligned(
                        Alignment.Center
                    )
            )

        /*
         * MAIN WINDOW
         */
        if (
            isVisible ||
            viewModel.isPiPMode.value
        ) {

            Window(

                onCloseRequest = {

                    if (
                        viewModel.isPiPMode.value
                    ) {

                        viewModel.isPiPMode.value =
                            false

                    } else {

                        isVisible = false
                    }
                },

                title =
                    "pEQ Neural Workstation",

                state =
                    windowState,

                icon =
                    icon,
                focusable = true,
                visible = true,

                transparent = false,

                undecorated =
                    viewModel.isPiPMode.value,

                alwaysOnTop =
                    viewModel.isPiPMode.value,

                resizable =
                    !viewModel.isPiPMode.value,

                onPreviewKeyEvent = { keyEvent ->

                    if (
                        keyEvent.type ==
                        KeyEventType.KeyDown
                    ) {

                        when (keyEvent.key) {

                            Key.Spacebar,
                            Key.K -> {

                                viewModel.togglePlayback()

                                true
                            }

                            Key.P -> {

                                viewModel.isPiPMode.value =
                                    !viewModel.isPiPMode.value

                                true
                            }

                            Key.F -> {

                                viewModel.isVideoFullScreen.value =
                                    !viewModel.isVideoFullScreen.value

                                true
                            }

                            Key.DirectionRight -> {

                                viewModel.seekRelative(5000)

                                true
                            }

                            Key.L -> {

                                viewModel.seekRelative(10000)

                                true
                            }

                            Key.DirectionLeft -> {

                                viewModel.seekRelative(-5000)

                                true
                            }

                            Key.J -> {

                                viewModel.seekRelative(-10000)

                                true
                            }

                            Key.M -> {

                                viewModel.masterVolume.value =

                                    if (
                                        viewModel.masterVolume.value > 0f
                                    ) {
                                        0f
                                    } else {
                                        0.8f
                                    }

                                true
                            }

                            Key.DirectionUp -> {

                                viewModel.masterVolume.value =

                                    (
                                        viewModel.masterVolume.value + 0.05f
                                    ).coerceIn(0f, 1f)

                                true
                            }

                            Key.DirectionDown -> {

                                viewModel.masterVolume.value =

                                    (
                                        viewModel.masterVolume.value - 0.05f
                                    ).coerceIn(0f, 1f)

                                true
                            }

                            Key.N,
                            Key.MediaNext -> {

                                viewModel.nextTrack()

                                true
                            }

                            Key.B,
                            Key.MediaPrevious -> {

                                viewModel.previousTrack()

                                true
                            }

                            Key.S -> {

                                viewModel.stop()

                                true
                            }

                            Key.MediaPlayPause -> {

                                viewModel.togglePlayback()

                                true
                            }

                            else -> false
                        }

                    } else {

                        false
                    }
                }

            ) {

                MaterialTheme {

                    Surface(

                        modifier =
                            Modifier.fillMaxSize(),

                        color =
                            MaterialTheme
                                .colorScheme
                                .background

                    ) {

                        CompositionLocalProvider(

                            LocalAppViewModel provides
                                viewModel

                        ) {

                            App()
                        }
                    }
                }
            }
        }

        /*
         * MINI PLAYER
         */
        if (viewModel.showMiniPlayer.value) {

            Window(

                onCloseRequest = {

                    viewModel.showMiniPlayer.value =
                        false
                },

                state =
                    rememberWindowState(

                        width = 360.dp,

                        height = if (viewModel.playbackQueue.isNotEmpty()) 460.dp else 145.dp,

                        position =
                            WindowPosition.Aligned(
                                Alignment.TopEnd
                            )
                    ),

                title = "pEQ Remote",

                transparent = true,

                undecorated = true,

                alwaysOnTop = true,

                resizable = false

            ) {

                val curItem = viewModel.currentMediaItem.value
                pEQWidgetCommand(

                        item = curItem,

                        isPlaying =
                            viewModel.isPlaying.value,

                        onTogglePlayback = {

                            viewModel.togglePlayback()
                        },

                        onNext = {

                            viewModel.nextTrack()
                        },

                        onPrev = {

                            viewModel.previousTrack()
                        },

                        onLaunch = {

                            isVisible = true

                            viewModel.showMiniPlayer.value =
                                false
                        },
                        progress = if (viewModel.currentDuration.value > 0) viewModel.currentPosition.value.toFloat() / viewModel.currentDuration.value.toFloat() else 0f,
                        duration = viewModel.currentDuration.value,
                        currentPosition = viewModel.currentPosition.value,
                        isFavorite = if (curItem != null) viewModel.favoriteItems.contains(curItem) else false,
                        queue = viewModel.playbackQueue,
                        onQueueItemClick = { viewModel.playMediaItem(it) },
                        onFavoriteToggle = { curItem?.let { viewModel.toggleFavorite(it) } }
                    )
            }
        }

        /*
         * TRAY
         */
        Tray(

            icon = icon,

            tooltip =
                "pEQ Neural Workstation",

            menu = {

                val currentTrack =

                    viewModel.currentMediaItem.value
                        ?.title
                        ?: "pEQ Neural"

                val currentArtist =

                    viewModel.currentMediaItem.value
                        ?.artist
                        ?: "Neural Workstation"

                Item(
                    "Now Playing: $currentTrack",
                    enabled = false,
                    onClick = {}
                )

                Item(
                    "by $currentArtist",
                    enabled = false,
                    onClick = {}
                )

                Separator()

                Item(
                    "Restore Workstation"
                ) {

                    isVisible = true
                }

                Item(

                    if (
                        viewModel.showMiniPlayer.value
                    ) {
                        "Hide Remote Hub"
                    } else {
                        "Show Remote Hub"
                    }

                ) {

                    viewModel.showMiniPlayer.value =
                        !viewModel.showMiniPlayer.value
                }

                Separator()

                Item(

                    if (viewModel.isPlaying.value) {
                        "⏸ Pause"
                    } else {
                        "▶ Play"
                    }

                ) {

                    viewModel.togglePlayback()
                }

                Item(
                    "⏭ Next Track"
                ) {

                    viewModel.nextTrack()
                }

                Item(
                    "⏮ Previous Track"
                ) {

                    viewModel.previousTrack()
                }

                Separator()

                CheckboxItem(

                    text = "Open at Startup",

                    checked =
                        viewModel.openAtStartup.value,

                    onCheckedChange = { checked ->

                        viewModel.openAtStartup.value =
                            checked

                        toggleMacOSStartup(checked)
                    }
                )

                Separator()

                Item(
                    "Terminate (Force Exit)",
                    onClick = ::exitApplication
                )
            }
        )
    }
}

/*
 * STARTUP TOGGLE
 */
private fun toggleMacOSStartup(
    enabled: Boolean
) {

    try {

        val appPath =
            File(
                System.getProperty("user.dir")
            ).absolutePath

        val script =

            if (enabled) {

                """
                tell application "System Events"
                    make login item at end with properties {path:"$appPath", hidden:false}
                end tell
                """.trimIndent()

            } else {

                """
                tell application "System Events"
                    delete login item "pEQ Neural Workstation"
                end tell
                """.trimIndent()
            }

        Runtime.getRuntime().exec(

            arrayOf(
                "osascript",
                "-e",
                script
            )
        )

        println(
            "pEQControl: [SYSTEM] Startup toggled -> $enabled"
        )

    } catch (e: Exception) {

        println(
            "pEQControl: [SYSTEM] Failed to toggle startup -> ${e.message}"
        )
    }
}
