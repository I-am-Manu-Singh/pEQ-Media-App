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
    private var mediaCommandReceiver: BroadcastReceiver? = null
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

        // Preload standard physical drum samples for learning drum pads
        listOf("Kick", "Snare", "Hihat", "Clap", "Crash", "Ride", "Tom", "Perc").forEach { type ->
            val sampleFile = java.io.File(filesDir, "samples/${type.lowercase()}.wav")
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
        ).apply {
            pEQViewModelProvider.viewModel = this
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
                val rawPath = it.path?.substringAfterLast(":") ?: it.toString()
                val realPath = if (rawPath.startsWith("/storage/") || rawPath.startsWith("/sdcard")) {
                    rawPath
                } else {
                    "/storage/emulated/0/$rawPath"
                }
                viewModel.addScanFolder(realPath)
            }
        }
        viewModel.onPickFolderRequested = {
            folderPickerLauncher.launch(null)
        }
        viewModel.onRestoreSamplesRequested = { restoreFactorySamples() }

        val lyricsPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                var displayName: String? = null
                if (it.scheme == "content") {
                    try {
                        contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    displayName = cursor.getString(nameIndex)
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
                val name = displayName ?: it.path?.substringAfterLast("/") ?: "Lyrics"
                contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    viewModel.loadExternalLyrics(content, name)
                }
            }
        }
        viewModel.onPickLyricsRequested = {
            lyricsPickerLauncher.launch(arrayOf(
                "text/plain", 
                "application/octet-stream", 
                "text/lrc",
                "application/x-lrc",
                "*/*"
            ))
        }

        val subtitlePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                var displayName: String? = null
                if (it.scheme == "content") {
                    try {
                        contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    displayName = cursor.getString(nameIndex)
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
                val path = it.toString()
                viewModel.loadExternalSubtitle(path, displayName)
            }
        }
        viewModel.onPickSubtitleRequested = {
            subtitlePickerLauncher.launch(arrayOf(
                "application/x-subrip", 
                "text/vtt", 
                "text/plain", 
                "application/octet-stream",
                "application/x-srt",
                "text/srt",
                "*/*"
            ))
        }

        val chaptersPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                viewModel.loadExternalChapters(it.toString())
            }
        }
        viewModel.onPickChaptersRequested = {
            chaptersPickerLauncher.launch(arrayOf(
                "text/plain",
                "application/json",
                "application/xml",
                "application/octet-stream",
                "*/*"
            ))
        }

        val trackPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                val path = it.toString()
                var displayName: String? = null
                var sizeBytes = 0L
                var mimeType: String? = null

                if (it.scheme == "content") {
                    mimeType = contentResolver.getType(it)
                    try {
                        contentResolver.query(it, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    displayName = cursor.getString(nameIndex)
                                }
                                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                if (sizeIndex != -1) {
                                    sizeBytes = cursor.getLong(sizeIndex)
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }

                val finalName = displayName ?: path.substringAfterLast("/")
                val extension = finalName.substringAfterLast(".", "").lowercase()
                val isVideo = if (mimeType != null) {
                    mimeType.startsWith("video/")
                } else {
                    listOf("mp4", "mkv", "webm", "mov", "avi", "3gp", "ts", "flv", "m4v").contains(extension) ||
                    path.contains("video", ignoreCase = true)
                }

                val item = viewModel.mediaLibrary.value.find { it.path == path } ?: pEQMediaItem(
                    id = path.hashCode().toString(),
                    title = finalName.substringBeforeLast(".", finalName),
                    subtitle = "External Source",
                    path = path,
                    type = if (isVideo) MediaType.VIDEO else MediaType.AUDIO,
                    format = if (extension.isNotEmpty()) extension.uppercase() else (if (isVideo) "VIDEO" else "AUDIO"),
                    sizeBytes = sizeBytes,
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
        registerMediaCommandReceiver()
        viewModel.start()
    }

    private fun registerMediaCommandReceiver() {
        val commandFilter = IntentFilter().apply {
            addAction("org.audiocular.peq.app.ACTION_SYSTEM_NEXT")
            addAction("org.audiocular.peq.app.ACTION_SYSTEM_PREV")
        }
        mediaCommandReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (::viewModel.isInitialized) {
                    when (intent?.action) {
                        "org.audiocular.peq.app.ACTION_SYSTEM_NEXT" -> viewModel.nextTrack()
                        "org.audiocular.peq.app.ACTION_SYSTEM_PREV" -> viewModel.previousTrack()
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mediaCommandReceiver, commandFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(mediaCommandReceiver, commandFilter)
        }
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
        if (intent == null || !::viewModel.isInitialized) return
        
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data
                if (uri != null) {
                    val path = when (uri.scheme) {
                        "file" -> uri.path
                        "content" -> uri.toString()
                        else -> uri.toString()
                    }
                    path?.let {
                        var displayName: String? = null
                        var sizeBytes: Long? = null
                        var mimeType: String? = null

                        if (uri.scheme == "content") {
                            mimeType = contentResolver.getType(uri)
                            try {
                                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (nameIndex != -1) {
                                            displayName = cursor.getString(nameIndex)
                                        }
                                        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                        if (sizeIndex != -1) {
                                            sizeBytes = cursor.getLong(sizeIndex)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                println("pEQControl: [SYSTEM] Failed to query content URI metadata: ${e.message}")
                            }
                        } else if (uri.scheme == "file") {
                            try {
                                val file = java.io.File(uri.path ?: "")
                                if (file.exists()) {
                                    displayName = file.name
                                    sizeBytes = file.length()
                                    mimeType = java.net.URLConnection.guessContentTypeFromName(file.name)
                                }
                            } catch (e: Exception) {
                                println("pEQControl: [SYSTEM] Failed to query file URI metadata: ${e.message}")
                            }
                        }

                        lifecycleScope.launch {
                            delay(600) // Engine stabilization
                            viewModel.playExternalFile(
                                path = it,
                                displayName = displayName,
                                mimeType = mimeType,
                                sizeBytes = sizeBytes
                            )
                        }
                    }
                }
            }
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                        viewModel.handleSharedUrl(sharedText)
                        println("pEQControl: [SYSTEM] Captured shared text: $sharedText")
                    }
                }
            }
            else -> {
                println("pEQControl: [SYSTEM] Activity re-synced via intent: ${intent.action}")
            }
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
        if (::viewModel.isInitialized) {
            viewModel.isAppInBackground.value = true
            // Only show overlay if playing and lyrics are enabled
            if (viewModel.isPlaying.value && viewModel.isLyricsVisible.value) {
                startService(Intent(this, org.audiocular.peq.app.services.pEQOverlayService::class.java))
            }
        }
        org.audiocular.peq.app.ui.components.releaseBrightness()
    }
    
    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.isAppInBackground.value = false
            stopService(Intent(this, org.audiocular.peq.app.services.pEQOverlayService::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        mediaCommandReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (granted) {
                println("pEQControl: [SYSTEM] Media permissions granted, refreshing scanner")
                if (::viewModel.isInitialized) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.scanner?.refresh()
                    }
                }
            }
        }
    }
}
