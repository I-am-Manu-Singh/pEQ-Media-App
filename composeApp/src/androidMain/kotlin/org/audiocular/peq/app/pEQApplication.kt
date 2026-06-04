package org.audiocular.peq.app

import android.app.Application
import com.yausername.ffmpeg.FFmpeg

class pEQApplication : Application() {

    companion object {

        lateinit var instance: pEQApplication
            private set
    }

    override fun onCreate() {

        super.onCreate()

        instance = this

        println("pEQ: [APP] Industrial Engine Initialized")

        // INITIALIZE FFMPEG
        try {

            FFmpeg.getInstance()
                .init(this)

            println(
                "pEQControl: [FFMPEG] Binary Initialized"
            )

            // INITIALIZE YOUTUBE-DL
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(this)
            println("pEQControl: [CLOUD] YouTube-DL Initialized")

        } catch (e: Exception) {

            println(
                "pEQControl: [FFMPEG] Init Failed: ${e.message}"
            )

            e.printStackTrace()
        }
    }

    override fun onTrimMemory(level: Int) {

        super.onTrimMemory(level)

        if (level >= TRIM_MEMORY_MODERATE) {

            println(
                "pEQControl: [MEMORY] Physically Purging Volatile Caches due to Moderate Pressure..."
            )

            cacheDir.deleteRecursively()
        }

        if (level >= TRIM_MEMORY_COMPLETE) {

            println(
                "pEQControl: [MEMORY] Critical Pressure! Physical release initiated."
            )
        }
    }

    override fun onLowMemory() {

        super.onLowMemory()

        println(
            "pEQControl: [MEMORY] LOW MEMORY WARNING!"
        )

        cacheDir.deleteRecursively()
    }
}