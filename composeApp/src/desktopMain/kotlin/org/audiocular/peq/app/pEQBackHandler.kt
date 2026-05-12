package org.audiocular.peq.app

import androidx.compose.runtime.Composable

@Composable
actual fun pEQBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // No-op for Desktop
}
