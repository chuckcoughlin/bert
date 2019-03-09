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
import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.speech.MessageType;
import chuckcoughlin.bert.speech.SpokenTextManager;

/**
 * This is a foreground service and may be turned on/off with a notifications interface.
 * The voice service manages connections between the robot as and speech/logging facilities.
 * It accepts voice commands from the socket connection to the robot and updates listeners with
 * the resulting text. The listeners handle text enunciation and logging.
 *
 * The service relies on a Bluetooth connection, socket communication and the
 * Android speech recognition classes. Implement as a Singleton, to provide universal access.
 */
public class VoiceService extends Service implements VoiceServiceHandler {
    private static final String CLSS = "VoiceService";
    private static volatile VoiceService instance = null;
    private static final long ERROR_CYCLE_DELAY = 10000;   // Wait interval for retry after error
    private volatile NotificationManager notificationManager;
    private static final String NOTIFICATION_COMMAND_MUTE  = "Mute";
    private static final String NOTIFICATION_COMMAND_START = "Start";
    private static final String NOTIFICATION_COMMAND_STOP  = "Stop";
    private BluetoothSocket socketHandler = null;
    private boolean isMuted;
    private static final int VOICE_NOTIFICATION = R.string.notificationKey; // Unique id for the Notification.
    private FacilityState currentState;
    private TieredFacility currentAction;

    public VoiceService() {
        this.isMuted = false;
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
        socketHandler = new BluetoothSocket(this);
        startForeground(VOICE_NOTIFICATION, notification);
    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(VOICE_NOTIFICATION);
        instance = null;
        socketHandler.shutdown();
        stopForegroundService();
    }

    /**
     * The initial intent action is null. Otherwise we receive values when the user clicks on the
     * notification buttons.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(CLSS,String.format("onStartCommand: %s flags = %d, id = %d",intent.getAction(),flags,startId));
        if( intent.getAction()==null) {
            currentAction = TieredFacility.BLUETOOTH;
            currentState = FacilityState.IDLE;
            reportConnectionState(currentAction, currentState);
            reportConnectionState(TieredFacility.SOCKET, FacilityState.IDLE);  // Just to initialize
            reportConnectionState(TieredFacility.VOICE, FacilityState.IDLE);   // Just to initialize

            determineNextAction();
        }
        else if(intent.getAction().equalsIgnoreCase(NOTIFICATION_COMMAND_MUTE)) {
            isMuted = !isMuted;
        }
        else if(intent.getAction().equalsIgnoreCase(NOTIFICATION_COMMAND_START)) {
            // What do we do here?
        }
        else if(intent.getAction().equalsIgnoreCase(NOTIFICATION_COMMAND_STOP)) {
        }
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BertConstants.NOTIFICATION_CHANNEL_ID);

        // Make notification show big text.
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle(getString(R.string.notificationLabel));
        bigTextStyle.bigText(getString(R.string.notificationDescription));
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
        NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, NOTIFICATION_COMMAND_START, pendingStartIntent);
        builder.addAction(playAction);

        // Mute button
        Intent muteIntent = new Intent(this, VoiceService.class);
        muteIntent.setAction(getString(R.string.notificationMute));
        PendingIntent pendingMuteIntent = PendingIntent.getService(this, 0, muteIntent, 0);
        NotificationCompat.Action muteAction = new NotificationCompat.Action(android.R.drawable.ic_media_pause, NOTIFICATION_COMMAND_MUTE, pendingMuteIntent);
        builder.addAction(muteAction);

        // Stop button
        Intent stopIntent = new Intent(this, VoiceService.class);
        stopIntent.setAction(getString(R.string.notificationStop));
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(android.R.drawable.ic_lock_power_off, NOTIFICATION_COMMAND_STOP, pendingStopIntent);
        builder.addAction(stopAction);

        // Build the notification.
        return( builder.build());
    }

    // Start the 3 actions in order
    private void determineNextAction() {
        if( currentAction.equals(TieredFacility.BLUETOOTH)) {
            if( !currentState.equals(FacilityState.ACTIVE)) {
                BluetoothChecker checker = new BluetoothChecker(this);
                currentState = FacilityState.WAITING;
                reportConnectionState(currentAction,currentState);
                checker.beginChecking((BluetoothManager)getSystemService(BLUETOOTH_SERVICE));
            }
            // Start socket
            else {
                currentAction = TieredFacility.SOCKET;
                currentState = FacilityState.WAITING;
                reportConnectionState(currentAction,currentState);
                socketHandler.create();
            }
        }
    }

    // Update any observers with the current state
    private void reportConnectionState(TieredFacility fac, FacilityState state) {
        Log.i(CLSS,String.format("reportConnectionState: %s %s",fac.name(),state.name()));
        ServiceStatusManager.getInstance().reportState(fac,state);
    }

    // Update any observers with the latest text
    private void reportSpokenText(String text) {
        Log.i(CLSS,String.format("reportSpokenText: %s",text));
        SpokenTextManager.getInstance().processText(text, MessageType.REQUEST);
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

    //=================================== VoiceServiceHandler ==============================================
    /**
     * There was an error in the bluetooth connection attempt.
     * @param reason error description
     */
    public void handleBluetoothError(String reason) {
        currentAction = TieredFacility.BLUETOOTH;
        currentState = FacilityState.ERROR;
        reportConnectionState(currentAction,currentState);
        reportSpokenText(reason);
        new Thread(new ProcessDelay(ERROR_CYCLE_DELAY)).start();
    }
    /**
     * The bluetooth connection request succeeded.
     */
    public void receiveBluetoothConnection() {
        currentAction = TieredFacility.BLUETOOTH;
        currentState = FacilityState.ACTIVE;
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