package org.audiocular.peq.app.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.CastMediaControlIntent

/**
 * pEQ CAST OPTIONS PROVIDER: UNIVERSAL BROADCAST HUB
 * Required for Google Cast SDK initialization.
 */
class pEQCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(org.audiocular.peq.app.ui.theme.pEQStrings.DEFAULT_MEDIA_RECEIVER_ID)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
