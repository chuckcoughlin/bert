/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *            (MIT License)
 */

package chuckcoughlin.bert.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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
public class VoiceService extends Service implements VoiceConnectionHandler {
    private static final String CLSS = "VoiceService";
    private static volatile VoiceService instance = null;
    private static final long ERROR_CYCLE_DELAY = 10000;   // Wait interval for retry after error
    private volatile NotificationManager notificationManager;
    private static final int VOICE_NOTIFICATION = R.string.notificationKey; // Unique id for the Notification.
    private ActionState currentState;
    private OrderedAction currentAction;

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
        currentAction = OrderedAction.BLUETOOTH;
        currentState = ActionState.IDLE;
        reportConnectionState(currentAction,currentState);
        reportConnectionState(OrderedAction.SOCKET,ActionState.IDLE);  // Just to initialize
        reportConnectionState(OrderedAction.VOICE,ActionState.IDLE);   // Just to initialize

    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(VOICE_NOTIFICATION);
        instance = null;
        stopForegroundService();
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

    // Start the 3 actions in order
    private void determineNextAction() {
        if( currentAction.equals(OrderedAction.BLUETOOTH)) {
            if( !currentState.equals(ActionState.ACTIVE)) {
                BluetoothChecker checker = new BluetoothChecker(this);
                currentState = ActionState.WAITING;
                reportConnectionState(currentAction,currentState);
                checker.beginChecking((BluetoothManager)getSystemService(BLUETOOTH_SERVICE));
            }
            // Start socket
            else {
                currentAction = OrderedAction.SOCKET;
                currentState = ActionState.WAITING;
                reportConnectionState(currentAction,currentState);
            }
        }
    }

    // Update any receivers with the current state
    private void reportConnectionState(OrderedAction action,ActionState state) {
        Intent intent = new Intent(VoiceConstants.RECEIVER_SERVICE_STATE);
        intent.addCategory(VoiceConstants.CATEGORY_SERVICE_STATE);
        intent.putExtra(VoiceConstants.KEY_SERVICE_ACTION,action.name());
        intent.putExtra(VoiceConstants.KEY_SERVICE_STATE,state.name());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Update any receivers with the latest text
    private void reportSpokenText(String text) {
        Intent intent = new Intent(VoiceConstants.RECEIVER_SPOKEN_TEXT);
        intent.addCategory(VoiceConstants.CATEGORY_SPOKEN_TEXT);
        intent.putExtra(VoiceConstants.KEY_SPOKEN_TEXT,text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void stopForegroundService() {
        Log.i(CLSS, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);
        BluetoothManager bmgr = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        bmgr.getAdapter().cancelDiscovery();

        // Stop the foreground service.
        stopSelf();
    }

    //=================================== VoiceConnectionHandler ==============================================
    /**
     * There was an error in the bluetooth connection attempt.
     * @param reason error description
     */
    public void handleBluetoothError(String reason) {
        currentAction = OrderedAction.BLUETOOTH;
        currentState = ActionState.ERROR;
        reportConnectionState(currentAction,currentState);
        reportSpokenText(reason);
        new Thread(new ProcessDelay(ERROR_CYCLE_DELAY)).start();
    }
    /**
     * The bluetooth connection request succeeded.
     */
    public void receiveBluetoothConnection() {
        currentAction = OrderedAction.BLUETOOTH;
        currentState = ActionState.ACTIVE;
        reportConnectionState(currentAction,currentState);
        determineNextAction();
    }

    //=================================== ProcessDelay ==============================================
    /**
     * Use this class to delay the transition to the next step. When we find
     * an error, we need to avoid a hard loop.

     */
    public class ProcessDelay implements Runnable {
        private final long sleepInterval;
        /**
         * Constructor:
         * @param delay millisecs to wait before going to the next state (or more
         *              likely retrying the current).
         */
        public ProcessDelay(long delay) {
            this.sleepInterval = delay;
        }

        public void run() {
            try{
                Thread.currentThread().sleep(sleepInterval);
                determineNextAction();
            }
            catch(InterruptedException ignore) {}
        }
    }
}