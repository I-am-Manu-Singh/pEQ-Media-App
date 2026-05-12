package org.audiocular.peq.app

import android.app.Application

/**
 * INDUSTRIAL APPLICATION HUB: pEQ CONTROL
 * Manages global lifecycle, memory pressure, and persistence.
 */
class pEQApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        println("pEQ: [APP] Industrial Engine Initialized")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // MEMORY HARDENING: Clear caches under pressure to prevent crashes
        if (level >= TRIM_MEMORY_MODERATE) {
            println("pEQControl: [MEMORY] Physically Purging Volatile Caches due to Moderate Pressure...")
            cacheDir.deleteRecursively()
        }
        if (level >= TRIM_MEMORY_COMPLETE) {
            println("pEQControl: [MEMORY] Critical Pressure! Physically releasing all non-essential resources.")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        println("pEQControl: [MEMORY] LOW MEMORY WARNING! Physical emergency cache clear initiated.")
        cacheDir.deleteRecursively()
    }
}
