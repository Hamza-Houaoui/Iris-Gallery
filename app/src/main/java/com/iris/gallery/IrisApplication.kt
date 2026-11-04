package com.iris.gallery

import android.app.Application
import com.hamza.tele.TeleApplication

object IrisApplication {
    val instance: Application
        get() = TeleApplication.instance
}
