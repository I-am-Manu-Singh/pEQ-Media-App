package org.audiocular.peq.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.view.WindowManager
import android.graphics.PixelFormat
import android.view.Gravity
import android.widget.TextView
import android.graphics.Color

class pEQLockscreenLyricsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    private var lyricsView: TextView? = null
    private var windowManager: WindowManager? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Technical HUD Design
        lyricsView = TextView(this).apply {
            text = "pEQ: [Spectral Lock Active]"
            setTextColor(android.graphics.Color.CYAN)
            textSize = 12f
            setBackgroundColor(android.graphics.Color.argb(180, 0, 0, 0))
            setPadding(24, 12, 24, 12)
            gravity = Gravity.CENTER
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 200 // Position above lockscreen clock
        }

        try {
            windowManager?.addView(lyricsView, params)
            println("pEQControl: [SERVICE] Lockscreen Lyrics HUD Injected.")
            
            // Grounding: Real-time Lyric Polling
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(object : Runnable {
                override fun run() {
                    val prefs = getSharedPreferences("pEQ_workstation_registry", Context.MODE_PRIVATE)
                    val currentLyric = prefs.getString("current_lyric", "pEQ: [Spectral Lock Active]")
                    lyricsView?.text = currentLyric
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 500)
                }
            }, 500)
        } catch (e: Exception) {
            println("pEQControl: [SERVICE] HUD Injection Failed: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lyricsView?.let { windowManager?.removeView(it) }
    }
}
