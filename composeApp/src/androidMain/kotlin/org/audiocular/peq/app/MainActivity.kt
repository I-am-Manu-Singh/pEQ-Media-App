package org.audiocular.peq.app

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.audiocular.peq.app.ai.AndroidpEQCloudDownloader
import org.audiocular.peq.app.ai.AndroidpEQStemSplitter
import org.audiocular.peq.app.audio.AndroidAudioEngine
import org.audiocular.peq.app.audio.AudioEngineProvider
import org.audiocular.peq.app.data.*
import org.audiocular.peq.app.services.pEQMediaSessionService
import org.audiocular.peq.app.ui.viewmodel.*
import org.audiocular.peq.app.ui.components.*
import org.audiocular.peq.app.security.initPermissionManager
import org.audiocular.peq.app.security.getPermissionManager
import org.audiocular.peq.app.data.initPersistenceManager
import org.audiocular.peq.app.data.getPersistenceManager
import androidx.lifecycle.Lifecycle
import androidx.media3.common.util.UnstableApi
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
class MainActivity : ComponentActivity() {
    private fun restoreFactorySamples() {
        val samplesDir = java.io.File("/storage/emulated/0/pEQ/Samples")
        if (!samplesDir.exists()) samplesDir.mkdirs()
        val drumTypes = listOf("KICK", "SNARE", "HIHAT", "TOM-H", "CRASH", "RIDE", "COWBELL", "SHAKER")
        drumTypes.forEach { type ->
            val sampleFile = java.io.File(samplesDir, "${type.lowercase()}.wav")
            if (!sampleFile.exists()) {
                try {
                    assets.open("samples/${type.lowercase()}.wav").use { input ->
                        sampleFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    println("pEQControl: [SAMPLES] Failed to restore $type: ${e.message}")
                }
            }
        }
    }
    private lateinit var viewModel: AppViewModel
    private var volumeReceiver: VolumeBroadcastReceiver? = null
    private var lastVolumeSync = 0L
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        enableEdgeToEdge()

        initPermissionManager(this)
        initPersistenceManager(applicationContext)
        org.audiocular.peq.app.ui.components.initSystemWindowManager(this)
        
        val permissions = getPermissionManager()
        permissions.requestMediaPermissions()
        permissions.requestMicrophonePermission()
        permissions.requestNotificationPermission()

        val audioEngine =
            AndroidAudioEngine(this).apply {
                initialize(0)
            }

        viewModel = AppViewModel(
            audioEngine = audioEngine,
            persistenceManager = getPersistenceManager(),
            stemSplitter = AndroidpEQStemSplitter(this),
            cloudDownloader = AndroidpEQCloudDownloader(this)
        )
        viewModel.apply {
            scanner = AndroidFileScanner(this@MainActivity)
            onEnterPiP = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    enterPictureInPictureMode(params)
                }
            }
            start()
        }

        lifecycleScope.launch {
            snapshotFlow { viewModel.isLockScreenMode.value }.collect { isLocked ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(isLocked)
                    setTurnScreenOn(isLocked)
                } else {
                    @Suppress("DEPRECATION")
                    if (isLocked) {
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                    }
                }
            }
        }

        val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val path = it.path?.substringAfterLast(":") ?: it.toString()
                val realPath = "/storage/emulated/0/$path" 
                viewModel.addScanFolder(realPath)
            }
        }
        viewModel.onPickFolderRequested = {
            folderPickerLauncher.launch(null)
        }
        viewModel.onRestoreSamplesRequested = { restoreFactorySamples() }

        val lyricsPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    viewModel.loadExternalLyrics(content, it.toString())
                }
            }
        }
        viewModel.onPickLyricsRequested = {
            lyricsPickerLauncher.launch(arrayOf("*/*"))
        }

        val subtitlePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // For subtitles, we might just pass the URI if the engine can handle it, 
                // but for now let's use the path if it's a local file
                val path = it.toString()
                viewModel.loadExternalSubtitle(path)
            }
        }
        viewModel.onPickSubtitleRequested = {
            subtitlePickerLauncher.launch(arrayOf("*/*"))
        }

        val chaptersPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                viewModel.loadExternalChapters(it.toString())
            }
        }
        viewModel.onPickChaptersRequested = {
            chaptersPickerLauncher.launch(arrayOf("*/*"))
        }

        val trackPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // Find the media item in the library or create a temp one
                val path = it.toString()
                val item = viewModel.mediaLibrary.value.find { it.path == path } ?: pEQMediaItem(
                    id = path.hashCode().toString(),
                    title = path.substringAfterLast("/"),
                    subtitle = "External Source",
                    path = path,
                    type = if (path.lowercase().endsWith(".mp3") || path.lowercase().endsWith(".wav") || path.lowercase().endsWith(".flac")) MediaType.AUDIO else MediaType.VIDEO,
                    format = path.substringAfterLast(".").uppercase(),
                    sizeBytes = 0L,
                    durationMs = 0L
                )
                viewModel.enqueueSplitJob(item)
            }
        }
        viewModel.onPickTrackRequested = {
            trackPickerLauncher.launch(arrayOf("audio/*", "video/*"))
        }

        handleIntent(intent)

        lifecycleScope.launch(Dispatchers.IO) { 
            if (permissions.hasMediaPermissions()) {
                viewModel.scanner?.refresh() 
            }
        }

        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: Exception) {
            println("pEQControl: [CLOUD] Neural Bridge Error: ${e.message}")
        }

        setContent { CompositionLocalProvider(LocalAppViewModel provides viewModel) { App() } }
        
        startHardwareMonitoring()
        startSystemSync()
        registerVolumeReceiver()
        // viewModel.start()
    }

    private fun registerVolumeReceiver() {
        volumeReceiver = VolumeBroadcastReceiver {
            val audioManager =
                getSystemService(Context.AUDIO_SERVICE)
                        as AudioManager

            val max =
                audioManager.getStreamMaxVolume(
                    AudioManager.STREAM_MUSIC
                )

            viewModel.setMasterVolume(
                it.toFloat() / max.toFloat()
            )
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                registerReceiver(
                    volumeReceiver,
                    filter,
                    RECEIVER_NOT_EXPORTED
                )

            } else {

                registerReceiver(
                    volumeReceiver,
                    filter
                )
            }
    }

    private inner class VolumeBroadcastReceiver(val onVolumeChanged: (Int) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val now = android.os.SystemClock.elapsedRealtime()

                if (now - lastVolumeSync > 80L) {

                    lastVolumeSync = now

                    onVolumeChanged(current)
                }
            }
        }
    }

    private fun startHardwareMonitoring() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    updateHardwareState()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    updateHardwareState()
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
        }
        updateHardwareState()
    }

    private fun updateHardwareState() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = mutableListOf(Device.INTERNAL)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val connectedDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            connectedDevices.forEach { dev ->
                when (dev.type) {
                    android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES, 
                    android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET -> devices.add(Device.HI_RES_DAC)
                    android.media.AudioDeviceInfo.TYPE_USB_DEVICE, 
                    android.media.AudioDeviceInfo.TYPE_USB_ACCESSORY,
                    android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> devices.add(Device.GENERIC)
                    android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> devices.add(Device.WIRELESS_ADAPTER)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.connectedDevices.clear()
            viewModel.connectedDevices.addAll(devices.distinct())
            
            val target = when {
                devices.contains(Device.HI_RES_DAC) -> Device.HI_RES_DAC
                devices.contains(Device.GENERIC) -> Device.GENERIC
                devices.contains(Device.WIRELESS_ADAPTER) -> Device.WIRELESS_ADAPTER
                else -> Device.INTERNAL
            }
            // Only auto-switch if not currently set to something active
            if (viewModel.currentDevice.value == Device.INTERNAL || !viewModel.connectedDevices.contains(viewModel.currentDevice.value)) {
                viewModel.updateDevice(target)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (::viewModel.isInitialized) {
            // Handle notification clicks or deep links
            println("pEQControl: [SYSTEM] Activity re-synced via intent.")
        }
    }

    private var mediaServiceStarted = false

    @OptIn(UnstableApi::class)
    private fun startSystemSync() {

        // SYSTEM UI MONITOR
       lifecycleScope.launch {

    snapshotFlow {

        viewModel.isVideoFullScreen.value ||
            viewModel.currentTab.value == Tab.Player

    }.collect { immersive ->

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        if (immersive) {

            controller.hide(
                WindowInsetsCompat.Type.systemBars()
            )

            controller.systemBarsBehavior =
                WindowInsetsControllerCompat
                    .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        } else {

            controller.show(
                WindowInsetsCompat.Type.systemBars()
            )
        }
    }
}

        // START MEDIA SERVICE ONLY ONCE
        if (!mediaServiceStarted) {

            mediaServiceStarted = true

            val serviceIntent =
                Intent(
                    this,
                    pEQMediaSessionService::class.java
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                startForegroundService(serviceIntent)

            } else {

                startService(serviceIntent)
            }
        }

        // WIDGET UPDATES
        lifecycleScope.launch {

            while (
                lifecycle.currentState.isAtLeast(
                    androidx.lifecycle.Lifecycle.State.STARTED
                )
            ) {

                val appWidgetManager =
                    AppWidgetManager.getInstance(
                        this@MainActivity
                    )

                val ids =
                    appWidgetManager.getAppWidgetIds(
                        ComponentName(
                            this@MainActivity,
                            org.audiocular.peq.app.widgets.NeuralGlanceWidgetReceiver::class.java
                        )
                    )

                if (ids.isNotEmpty()) {

                    val updateIntent =
                        Intent(
                            AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        ).apply {

                            putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                ids
                            )
                        }

                    sendBroadcast(updateIntent)
                }

                delay(15000)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::viewModel.isInitialized && viewModel.isPlaying.value && viewModel.currentMediaItem.value?.type == MediaType.VIDEO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::viewModel.isInitialized) {
            viewModel.isInPipMode.value = isInPictureInPictureMode
        }
    }

    override fun onPause() {
        super.onPause()
        org.audiocular.peq.app.ui.components.releaseBrightness()
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
    }
}
