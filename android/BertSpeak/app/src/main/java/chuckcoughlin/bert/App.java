package chuckcoughlin.bert;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import chuckcoughlin.bert.common.BertConstants;

public class App extends Application {
    private static final String CLSS = "App";
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    BertConstants.NOTIFICATION_CHANNEL_ID, BertConstants.NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription("Notification channel for Voice Service");
            serviceChannel.enableLights(true);
            serviceChannel.setLightColor(Color.BLUE);
            NotificationManager manager = getSystemService(NotificationManager.class);
            assert manager != null;
            manager.createNotificationChannel(serviceChannel);
            Log.i(CLSS,String.format("createNotificationChannel: %s (%s)",
                    BertConstants.NOTIFICATION_CHANNEL_NAME,BertConstants.NOTIFICATION_CHANNEL_ID));
        }
    }
}
