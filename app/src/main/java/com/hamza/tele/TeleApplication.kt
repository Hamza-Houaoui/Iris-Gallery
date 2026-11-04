package com.hamza.tele

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.decode.StaticImageDecoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.video.VideoFrameDecoder
import com.iris.gallery.decoder.AvifCoilDecoder
import com.hamza.tele.teledrop.data.api.ApiClient
import com.hamza.tele.teledrop.data.api.TelegramApiService
import com.hamza.tele.teledrop.data.db.AppDatabase
import com.hamza.tele.teledrop.data.prefs.SettingsManager
import com.hamza.tele.teledrop.data.repository.MediaRepository

class TeleApplication : Application(), SingletonImageLoader.Factory {

    lateinit var settingsManager: SettingsManager
    lateinit var database: AppDatabase
    lateinit var mediaRepository: MediaRepository
    lateinit var apiService: TelegramApiService

    companion object {
        lateinit var instance: TeleApplication
            private set
        const val UPLOAD_CHANNEL_ID = "teledrop_upload_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()

        settingsManager = SettingsManager(this)
        database = AppDatabase.getDatabase(this)
        mediaRepository = MediaRepository(this)
        apiService = ApiClient.instance
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPLOAD_CHANNEL_ID,
                "TeleIris Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows upload progress for photos and videos to Telegram"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(AvifCoilDecoder.Factory())
                add(coil3.svg.SvgDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                    add(StaticImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
