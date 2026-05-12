package org.audiocular.peq.app.ui.components

actual fun setScreenOrientation(orientation: Int) {
    println("pEQControl: [SYSTEM] Orientation Change (STUB): $orientation")
}

actual fun setSystemBarsVisibility(visible: Boolean) {
    println("pEQControl: [SYSTEM] System Bars Visibility (STUB): $visible")
}

actual fun setBrightness(brightness: Float) {
    println("pEQControl: [SYSTEM] Brightness (STUB): $brightness")
}

actual fun setSystemVolume(volume: Float) {
    try {
        val volInt = (volume * 100).toInt()
        Runtime.getRuntime().exec(arrayOf("osascript", "-e", "set volume output volume $volInt"))
    } catch (e: Exception) {
        println("pEQControl: [SYSTEM] Error setting volume: ${e.message}")
    }
}

actual fun getSystemVolume(): Float {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("osascript", "-e", "output volume of (get volume settings)"))
        val output = process.inputStream.bufferedReader().readText().trim()
        output.toFloat() / 100f
    } catch (e: Exception) {
        0.5f
    }
}
