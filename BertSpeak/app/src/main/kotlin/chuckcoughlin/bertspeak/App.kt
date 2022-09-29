package chuckcoughlin.bertspeak

import android.app.Application
import chuckcoughlin.bertspeak.common.BertConstants
import android.app.NotificationManager
import android.os.Build
import android.app.NotificationChannel
import android.graphics.Color
import android.util.Log

class App : Application() {
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