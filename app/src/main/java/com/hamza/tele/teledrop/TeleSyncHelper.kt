package com.hamza.tele.teledrop

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hamza.tele.TeleApplication
import com.hamza.tele.teledrop.worker.UploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TeleSyncHelper {

    fun sendUrisToTelegram(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            val settings = TeleApplication.instance.settingsManager
            val token = settings.botToken.first()
            val chatId = settings.chatId.first()

            if (token.isNullOrBlank() || chatId.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Telegram Bot not configured! Open TeleDrop first.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val wifiOnly = settings.wifiOnly.first()
            val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .build()

            val uriStrings = uris.map { it.toString() }.toTypedArray()
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(workDataOf(
                    UploadWorker.KEY_URIS to uriStrings,
                    UploadWorker.KEY_IS_AUTO_SYNC to false
                ))
                .addTag("manual_upload")
                .build()

            WorkManager.getInstance(appContext).enqueue(request)

            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, "Sending ${uris.size} item(s) to Telegram...", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
