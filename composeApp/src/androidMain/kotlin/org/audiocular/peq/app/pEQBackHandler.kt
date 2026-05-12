package org.audiocular.peq.app

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler as AndroidBackHandler

@Composable
actual fun pEQBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    AndroidBackHandler(
        enabled = enabled,
        onBack = onBack
    )
}