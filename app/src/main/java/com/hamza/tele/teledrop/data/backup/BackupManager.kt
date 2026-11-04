package com.hamza.tele.teledrop.data.backup

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.hamza.tele.TeleApplication
import com.hamza.tele.teledrop.data.api.buildTelegramUrl
import com.hamza.tele.teledrop.data.db.SyncFolderEntity
import com.hamza.tele.teledrop.data.db.UploadedMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val botToken: String? = null,
    val chatId: String? = null,
    val folders: List<SyncFolderEntity> = emptyList(),
    val uploadedMedia: List<UploadedMediaEntity> = emptyList()
)

object BackupManager {

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val app = TeleApplication.instance
        val db = app.database
        val settings = app.settingsManager

        val token = settings.botToken.first()
        val chatId = settings.chatId.first()
        val folders = db.syncFolderDao().getAllFolders()
        val media = db.uploadedMediaDao().getAllUploadedMedia()

        val backup = BackupData(
            botToken = token,
            chatId = chatId,
            folders = folders,
            uploadedMedia = media
        )

        Gson().toJson(backup)
    }

    suspend fun exportBackupToFile(context: Context, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json = createBackupJson(context)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackupFromUri(context: Context, sourceUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(sourceUri)?.bufferedReader().use { it?.readText() }
                ?: return@withContext Result.failure(Exception("Failed to read backup file"))

            val backup = Gson().fromJson(json, BackupData::class.java)
            val app = TeleApplication.instance
            val db = app.database
            val settings = app.settingsManager

            if (!backup.botToken.isNullOrBlank()) {
                settings.saveBotToken(backup.botToken)
            }
            if (!backup.chatId.isNullOrBlank()) {
                settings.saveChatId(backup.chatId)
            }

            for (folder in backup.folders) {
                db.syncFolderDao().insertFolder(folder)
            }

            var importedCount = 0
            for (item in backup.uploadedMedia) {
                db.uploadedMediaDao().insertUploaded(item)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backupToTelegram(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val app = TeleApplication.instance
            val settings = app.settingsManager
            val token = settings.botToken.first()
            val chatId = settings.chatId.first()

            if (token.isNullOrBlank() || chatId.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Telegram credentials not configured"))
            }

            val json = createBackupJson(context)
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempFile = File(context.cacheDir, "TeleIris_Backup_$timeStr.json")
            tempFile.writeText(json, Charsets.UTF_8)

            val reqFile = tempFile.asRequestBody("application/json".toMediaTypeOrNull())
            val docPart = MultipartBody.Part.createFormData("document", tempFile.name, reqFile)
            val chatIdPart = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionPart = "📦 TeleIris Database Backup\nCreated at: ${Date()}\nContains sync hashes & folder list.".toRequestBody("text/plain".toMediaTypeOrNull())

            val url = buildTelegramUrl(token, "sendDocument")
            val response = app.apiService.sendDocument(
                url = url,
                chatId = chatIdPart,
                document = docPart,
                caption = captionPart
            )

            tempFile.delete()

            if (response.isSuccessful) {
                Result.success("Backup uploaded to Telegram successfully!")
            } else {
                Result.failure(Exception("Telegram API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
