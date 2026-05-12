package org.audiocular.peq.app

import androidx.compose.runtime.Composable

@Composable
expect fun pEQBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)