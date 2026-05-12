package org.audiocular.peq.app.ui.components

actual fun setScreenOrientation(orientation: Int) {
    println("pEQControl: [SYSTEM] Orientation Change (IOS STUB): $orientation")
}

actual fun setSystemBarsVisibility(visible: Boolean) {
    println("pEQControl: [SYSTEM] System Bars Visibility (IOS STUB): $visible")
}
