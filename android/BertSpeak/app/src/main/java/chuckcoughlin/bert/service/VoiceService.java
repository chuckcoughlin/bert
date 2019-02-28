/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *            (MIT License)
 */

package chuckcoughlin.bert.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import chuckcoughlin.bert.R;

/**
 * This is a foreground service and may be turned on/off with a notifications interface.
 * The voice service manages connections to the robot as well as voice-text communications. I
 * t listens to the voice commands and updates listeners with the resulting text. It also accepts text and renders
 * it on the speakers.
 *
 * The service relies on a Bluetooth connection, socket communication and the
 * Android speech recognition classes. Implement as a Singleton, to provide universal access.
 */
public class VoiceService extends Service  {
    private static final String CLSS = "VoiceService";
    private static volatile VoiceService instance = null;
    private volatile NotificationManager notificationManager;
    // Unique Identification Number for the Notification.
    private static final int VOICE_NOTIFICATION = R.string.notificationKey;

    public VoiceService() {
    }

    /**
     * Provide a means to access this service from anywhere.
     * @return the global Singleton instance.
     */
    public static synchronized VoiceService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Display a notification about us starting.  We put an icon in the status bar.
     */
    @Override
    public void onCreate() {
        Log.i(CLSS,"onCreate: Starting foreground service ...");
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = buildNotification();
        instance = this;
        startForeground(VOICE_NOTIFICATION, notification);
    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(VOICE_NOTIFICATION);
        instance = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(CLSS,String.format("onStartCommand: flags = %d, id = %d",flags,startId));
        return(START_NOT_STICKY);
    }

    /*
     * Build a notification with
     * --- start
     * ---- stop
     * ---- mute
     */
    private Notification buildNotification() {

        // Create notification default intent.
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // Create notification builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Make notification show big text.
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle("Music player implemented by foreground service.");
        bigTextStyle.bigText("Android foreground service is a android service which can run in foreground always, it can be controlled by user via notification.");
        // Set big text style.
        builder.setStyle(bigTextStyle);

        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rounded_button);
        builder.setLargeIcon(largeIconBitmap);
        // Make the notification max priority.
        builder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);
        // Make head-up notification.
        builder.setFullScreenIntent(pendingIntent, true);

        //  Start Button
        Intent startIntent = new Intent(this, VoiceService.class);
        startIntent.setAction(getString(R.string.notificationStart));
        PendingIntent pendingStartIntent = PendingIntent.getService(this, 0, startIntent, 0);
        NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, "Start", pendingStartIntent);
        builder.addAction(playAction);

        // Mute button
        Intent muteIntent = new Intent(this, VoiceService.class);
        muteIntent.setAction(getString(R.string.notificationMute));
        PendingIntent pendingMuteIntent = PendingIntent.getService(this, 0, muteIntent, 0);
        NotificationCompat.Action muteAction = new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Mute", pendingMuteIntent);
        builder.addAction(muteAction);

        // Stop button
        Intent stopntent = new Intent(this, VoiceService.class);
        stopntent.setAction(getString(R.string.notificationStop));
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(android.R.drawable.ic_lock_power_off, "Stop", pendingStopIntent);
        builder.addAction(stopAction);

        // Build the notification.
        return( builder.build());
    }

    private void stopForegroundService() {
        Log.i(CLSS, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }
}