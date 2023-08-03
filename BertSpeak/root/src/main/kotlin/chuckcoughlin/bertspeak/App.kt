/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.os.StrictMode
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants


class App : Application() {
    fun App() {
        StrictMode.enableDefaults()  // Helps with dangling resource detection
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                BertConstants.NOTIFICATION_CHANNEL_ID, BertConstants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            serviceChannel.description = "Notification channel for Voice Service"
            serviceChannel.enableLights(true)
            serviceChannel.lightColor = Color.BLUE
            val manager = getSystemService(
                NotificationManager::class.java
            )!!
            manager.createNotificationChannel(serviceChannel)
            Log.i(
                CLSS, String.format(
                    "createNotificationChannel: %s (%s)",
                    BertConstants.NOTIFICATION_CHANNEL_NAME, BertConstants.NOTIFICATION_CHANNEL_ID
                )
            )
        }
    }

    companion object {
        private const val CLSS = "App"
    }
}
