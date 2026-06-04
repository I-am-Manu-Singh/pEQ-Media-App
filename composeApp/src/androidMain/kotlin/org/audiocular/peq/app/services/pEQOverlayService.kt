package org.audiocular.peq.app.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import org.audiocular.peq.app.ui.components.FloatingLyricsOverlay
import org.audiocular.peq.app.ui.viewmodel.AppViewModel
import org.audiocular.peq.app.ui.viewmodel.pEQViewModelProvider
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import org.audiocular.peq.app.audio.AudioEngineProvider

class pEQOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {
    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
            
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            
            format = android.graphics.PixelFormat.TRANSLUCENT
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            x = 0
            y = 100 // Positioning at the bottom with a 100px safety margin
        }

        overlayView = ComposeView(this).apply {
            // Set required owners for Compose in Service
            setViewTreeLifecycleOwner(this@pEQOverlayService)
            setViewTreeSavedStateRegistryOwner(this@pEQOverlayService)
            
            setContent {
                val viewModel = pEQViewModelProvider.viewModel
                if (viewModel != null) {
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(100f) }

                    val isPlaying by viewModel.isPlaying
                    val isVisible by viewModel.isLyricsVisible
                    val isBackground by viewModel.isAppInBackground

                    LaunchedEffect(isPlaying) {
                        if (!isPlaying) stopSelf()
                    }

                    // Dynamically hide the overlay view when not in background or lyrics are hidden
                    LaunchedEffect(isVisible, isBackground) {
                        overlayView?.visibility = if (isVisible && isBackground) android.view.View.VISIBLE else android.view.View.GONE
                    }

                    if (isVisible && isBackground) {
                        FloatingLyricsOverlay(
                            viewModel = viewModel,
                            modifier = Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY -= dragAmount.y // Subtract Y because gravity is BOTTOM
                                    
                                    params.x = offsetX.toInt()
                                    params.y = offsetY.toInt()
                                    windowManager.updateViewLayout(this@apply, params)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        windowManager.addView(overlayView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

// Extension to help with finding view tree owners in service
private fun View.setViewTreeLifecycleOwner(owner: LifecycleOwner) {
    setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, owner)
}
private fun View.setViewTreeSavedStateRegistryOwner(owner: SavedStateRegistryOwner) {
    setTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner, owner)
}
