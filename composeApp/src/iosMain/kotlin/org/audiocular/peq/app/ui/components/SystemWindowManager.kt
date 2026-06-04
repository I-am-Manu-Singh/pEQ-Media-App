package org.audiocular.peq.app.ui.components

actual fun setScreenOrientation(orientation: Int) {
    println("pEQControl: [SYSTEM] Orientation Change (IOS STUB): $orientation")
}

actual fun setSystemBarsVisibility(visible: Boolean) {
    println("pEQControl: [SYSTEM] System Bars Visibility (IOS STUB): $visible")
}

actual fun setBrightness(brightness: Float) {
    println("pEQControl: [SYSTEM] Brightness (IOS STUB): $brightness")
}

actual fun setSystemVolume(volume: Float) {
    println("pEQControl: [SYSTEM] System Volume (IOS STUB): $volume")
}

actual fun getSystemVolume(): Float = 0.5f
