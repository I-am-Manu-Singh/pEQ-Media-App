package org.audiocular.peq.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import org.audiocular.peq.app.ui.viewmodel.*
import androidx.compose.ui.graphics.graphicsLayer
import java.awt.Taskbar
import java.awt.Toolkit
import javax.imageio.ImageIO
import java.io.File
import org.audiocular.peq.app.audio.DesktopAudioEngine

fun main(args: Array<String>) {
    // 🏷️ Fix macOS "Java" name in menu and dock
    System.setProperty("apple.awt.application.name", "pEQ Neural Workstation")
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "pEQ Neural Workstation")
    System.setProperty("apple.awt.UIElement", "false")
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    
    application {
        val viewModel = remember {
            AppViewModel(
                audioEngine = DesktopAudioEngine(),
                persistenceManager = DesktopPersistenceManager(),
                stemSplitter = null,
                cloudDownloader = null
            ).apply {
                scanner = DesktopFileScanner()
                platformStorageBase.value = System.getProperty("user.home")
                
                onPickFolderRequested = {
                    DesktopFilePicker
                        .pickFolder("Select Media Folder")
                        ?.let { folder ->
                            addScanFolder(folder)
                        }
                }
                
                onPickSubtitleRequested = {
                    DesktopFilePicker.pickFile("Select Subtitle File", listOf("srt", "vtt"))?.let { path ->
                        importSubtitleFile(path)
                    }
                }
                
                onPickChaptersRequested = {
                    DesktopFilePicker.pickFile("Select Chapter File", listOf("txt", "chapter", "lrc"))?.let { path ->
                        importChapterFile(path)
                    }
                }

                // Handle startup arguments
                if (args.isNotEmpty()) {
                    val path = args[0]
                    if (File(path).exists()) {
                        playExternalFile(path)
                    }
                }
            }
        }

        val icon = painterResource("app_icon.png")
        
        // 🚀 Ensure splash screen finishes after initialization
        LaunchedEffect(Unit) {
            delay(1500) // Give it a moment to stabilize
            viewModel.completeSplash()
        }
        
        // macOS Dock Icon Fix
        LaunchedEffect(Unit) {
            try {
                if (Taskbar.isTaskbarSupported()) {
                    val taskbar = Taskbar.getTaskbar()
                    if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                        val resource = Thread.currentThread().contextClassLoader.getResource("app_icon.png")
                        if (resource != null) {
                            val image = ImageIO.read(resource)
                            taskbar.iconImage = image
                        }
                    }
                }
            } catch (e: Exception) {
                println("pEQControl: [SYSTEM] Failed to set dock icon: ${e.message}")
            }
        }

        var isVisible by remember { mutableStateOf(true) }

        val windowState = rememberWindowState(
            width = if (viewModel.isPiPMode.value) 320.dp else 480.dp,
            height = if (viewModel.isPiPMode.value) 180.dp else 920.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )

        if (isVisible || viewModel.isPiPMode.value) {
            Window(
                onCloseRequest = { 
                    if (viewModel.isPiPMode.value) {
                        viewModel.isPiPMode.value = false
                    } else {
                        isVisible = false 
                    }
                },
                title = "pEQ Neural Workstation",
                state = windowState,
                icon = icon,
                visible = true,
                transparent = false,
                undecorated = viewModel.isPiPMode.value,
                alwaysOnTop = viewModel.isPiPMode.value,
                resizable = !viewModel.isPiPMode.value,
                onPreviewKeyEvent = { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.Spacebar -> {
                                viewModel.togglePlayback()
                                true
                            }
                            Key.K -> {
                                viewModel.togglePlayback()
                                true
                            }
                            Key.P -> {
                                viewModel.isPiPMode.value = !viewModel.isPiPMode.value
                                true
                            }
                            Key.F -> {
                                viewModel.isVideoFullScreen.value = !viewModel.isVideoFullScreen.value
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
                                viewModel.masterVolume.value = if (viewModel.masterVolume.value > 0f) 0f else 0.8f
                                true
                            }
                            Key.DirectionUp -> {
                                viewModel.masterVolume.value = (viewModel.masterVolume.value + 0.05f).coerceIn(0f, 1f)
                                true
                            }
                            Key.DirectionDown -> {
                                viewModel.masterVolume.value = (viewModel.masterVolume.value - 0.05f).coerceIn(0f, 1f)
                                true
                            }
                            Key.N -> {
                                viewModel.nextTrack()
                                true
                            }
                            Key.B -> {
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
                            Key.MediaNext -> {
                                viewModel.nextTrack()
                                true
                            }
                            Key.MediaPrevious -> {
                                viewModel.previousTrack()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
            ) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        CompositionLocalProvider(
                            LocalAppViewModel provides viewModel
                        ) {
                            App()
                        }
                    }
                }
            }
        }

        // 🔷 MINI PLAYER (WIDGET) WINDOW
        if (viewModel.showMiniPlayer.value) {
            Window(
                onCloseRequest = { viewModel.showMiniPlayer.value = false },
                state = rememberWindowState(width = 340.dp, height = 110.dp, position = WindowPosition.Aligned(Alignment.TopEnd)),
                title = "pEQ Remote",
                transparent = true,
                undecorated = true,
                alwaysOnTop = true,
                resizable = false
            ) {
                org.audiocular.peq.app.ui.widgets.pEQWidgetCommand(
                    item = viewModel.currentMediaItem.value,
                    isPlaying = viewModel.isPlaying.value,
                    onTogglePlayback = { viewModel.togglePlayback() },
                    onNext = { viewModel.nextTrack() },
                    onPrev = { viewModel.previousTrack() },
                    onLaunch = { isVisible = true; viewModel.showMiniPlayer.value = false }
                )
            }
        }

        Tray(
            icon = icon,
            tooltip = "pEQ Neural Workstation",
            menu = {
                val currentTrack = viewModel.currentMediaItem.value?.title ?: "pEQ Neural"
                val currentArtist = viewModel.currentMediaItem.value?.artist ?: "Neural Workstation"
                
                Item("Now Playing: $currentTrack", enabled = false, onClick = {})
                Item("by $currentArtist", enabled = false, onClick = {})
                Separator()
                Item("Restore Workstation", onClick = { isVisible = true })
                Item(if (viewModel.showMiniPlayer.value) "Hide Remote Hub" else "Show Remote Hub", onClick = { viewModel.showMiniPlayer.value = !viewModel.showMiniPlayer.value })
                Separator()
                Item(if (viewModel.isPlaying.value) "⏸ Pause" else "▶️ Play", onClick = { viewModel.togglePlayback() })
                Item("⏭ Next Track", onClick = { viewModel.nextTrack() })
                Item("⏮ Previous Track", onClick = { viewModel.previousTrack() })
                Separator()
                CheckboxItem(
                    "Open at Startup",
                    checked = viewModel.openAtStartup.value,
                    onCheckedChange = { checked ->
                        viewModel.openAtStartup.value = checked
                        toggleMacOSStartup(checked)
                    }
                )
                Separator()
                Item("Terminate (Force Exit)", onClick = ::exitApplication)
            }
        )
    }
}

private fun toggleMacOSStartup(enabled: Boolean) {
    try {
        val appPath = File(System.getProperty("user.dir")).absolutePath
        val script = if (enabled) {
            "tell application \"System Events\" to make login item at end with properties {path:\"$appPath\", hidden:false}"
        } else {
            "tell application \"System Events\" to delete login item \"pEQ Neural Workstation\""
        }
        Runtime.getRuntime().exec(arrayOf("osascript", "-e", script))
        println("pEQControl: [SYSTEM] Startup toggled: $enabled")
    } catch (e: Exception) {
        println("pEQControl: [SYSTEM] Failed to toggle startup: ${e.message}")
    }
}
