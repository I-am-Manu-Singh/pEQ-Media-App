package org.audiocular.peq.app.receivers

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters

class pEQCommandWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): androidx.work.ListenableWorker.Result {

        val action = inputData.getString("action")
            ?: return androidx.work.ListenableWorker.Result.failure()

        val serviceIntent =
            Intent(applicationContext, org.audiocular.peq.app.services.pEQMediaSessionService::class.java).apply {
                this.action = action
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            applicationContext.startForegroundService(serviceIntent)

        } else {

            applicationContext.startService(serviceIntent)

        }

        return androidx.work.ListenableWorker.Result.success()
    }
}