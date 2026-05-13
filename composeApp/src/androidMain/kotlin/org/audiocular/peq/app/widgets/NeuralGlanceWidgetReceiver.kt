package org.audiocular.peq.app.widgets

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NeuralGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NeuralGlanceWidget()

    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        
        val action = intent.action ?: return
        if (action.startsWith("org.audiocular.peq.app.ACTION_")) {
            val serviceIntent = android.content.Intent(context, org.audiocular.peq.app.services.pEQMediaSessionService::class.java).apply {
                this.action = action
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
