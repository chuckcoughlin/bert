/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *            (MIT License)
 */

package chuckcoughlin.bert.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.db.SettingsManager;
import chuckcoughlin.bert.speech.MessageType;
import chuckcoughlin.bert.speech.SpeechAnalyzer;
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
    private static final long ERROR_CYCLE_DELAY = 15000;   // Wait interval for retry after error
    private volatile NotificationManager notificationManager;
    private BluetoothConnection bluetoothConnection = null;
    private BluetoothDevice bluetoothDevice = null;
    private SpeechAnalyzer analyzer = null;
    private boolean isMuted;
    private static final int VOICE_NOTIFICATION = R.string.notificationKey; // Unique id for the Notification.

    //private static final boolean IS_EMULATOR = Build.HARDWARE.contains("goldfish");
    //private static final boolean IS_EMULATOR Build.IS_EMULATOR;

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
        throw new UnsupportedOperationException("VoiceService: onBind not yet implemented");
    }

    /**
     * Display a notification about us starting.  We put an icon in the status bar.
     * Initialize all the singletons.
     */
    @Override
    public void onCreate() {
        Log.i(CLSS,"onCreate: Starting foreground service ...");
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = buildNotification();
        instance = this;
        SettingsManager.initialize(this);
        SpokenTextManager.initialize(this);
        ServiceStatusManager.initialize();
        startForeground(VOICE_NOTIFICATION, notification);
    }

    /**
     * Shutdown the services and the singletons.
     */
    @Override
    public void onDestroy() {
        notificationManager.cancel(VOICE_NOTIFICATION);
        instance = null;
        if(bluetoothConnection!=null) bluetoothConnection.shutdown();
        if(analyzer!=null) analyzer.shutdown();
        ServiceStatusManager.stop();
        SettingsManager.stop();
        SpokenTextManager.stop();
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
        String action = null;
        if( intent!=null) action = intent.getAction();
        Log.i(CLSS,String.format("onStartCommand: %s flags = %d, id = %d",action,flags,startId));
        bluetoothConnection = new BluetoothConnection(this);
        analyzer = new SpeechAnalyzer(this,getApplicationContext());

        if( action==null) {
            reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.IDLE);
            reportConnectionState(TieredFacility.SOCKET, FacilityState.IDLE);  // Just to initialize
            reportConnectionState(TieredFacility.VOICE, FacilityState.IDLE);   // Just to initialize
            determineNextAction(TieredFacility.BLUETOOTH);
        }
        else if(action.equalsIgnoreCase(getString(R.string.notificationMute))) {
            toggleMute();
        }
        else if(action.equalsIgnoreCase(getString(R.string.notificationReset))) {
            if(bluetoothConnection !=null) bluetoothConnection.shutdown();
            ServiceStatusManager.getInstance().reportState(TieredFacility.SOCKET,FacilityState.IDLE);
            if( analyzer!=null) analyzer.shutdown();
            determineNextAction(TieredFacility.BLUETOOTH);
        }
        else if(action.equalsIgnoreCase(getString(R.string.notificationStop))) {
            if( analyzer!=null) analyzer.shutdown();
            stopSelf();
        }
        return(START_STICKY);
    }

    public void setBluetoothDevice(BluetoothDevice device) { this.bluetoothDevice = device; }
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

        //  Reset Button
        Intent startIntent = new Intent(this, NotificationActionReceiver.class);
        String action = getString(R.string.notificationReset);
        startIntent.setAction(action);
        PendingIntent pendingStartIntent = PendingIntent.getBroadcast(this, 1, startIntent, 0);
        NotificationCompat.Action resetAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, action, pendingStartIntent);
        builder.addAction(resetAction);
        builder.setOngoing(true);

        // Mute button
        Intent muteIntent = new Intent(this, NotificationActionReceiver.class);
        action = getString(R.string.notificationMute);
        muteIntent.setAction(action);
        PendingIntent pendingMuteIntent = PendingIntent.getBroadcast(this, 2, muteIntent, 0);
        NotificationCompat.Action muteAction = new NotificationCompat.Action(android.R.drawable.ic_media_pause, action, pendingMuteIntent);
        builder.addAction(muteAction);
        builder.setOngoing(true);

        // Stop button
        Intent stopIntent = new Intent(this, NotificationActionReceiver.class);
        action = getString(R.string.notificationStop);
        stopIntent.setAction(action);
        PendingIntent pendingStopIntent = PendingIntent.getBroadcast(this, 3, stopIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(android.R.drawable.ic_lock_power_off, action, pendingStopIntent);
        builder.addAction(stopAction);
        builder.setOngoing(true);

        // Build the notification.
        return( builder.build());
    }

    // Start the 3 actions in order
    private void determineNextAction(TieredFacility currentFacility) {
        FacilityState currentState = ServiceStatusManager.getInstance().getStateForFacility(currentFacility);
        if( currentFacility.equals(TieredFacility.BLUETOOTH)) {
            if( !currentState.equals(FacilityState.ACTIVE)) {
                BluetoothChecker checker = new BluetoothChecker(this);
                reportConnectionState(currentFacility,FacilityState.WAITING);
                checker.beginChecking((BluetoothManager)getSystemService(BLUETOOTH_SERVICE));
            }
            // Start socket
            else {
                reportConnectionState(TieredFacility.SOCKET,FacilityState.WAITING);
                bluetoothConnection.openConnections(bluetoothDevice);
            }
        }
        else if( currentFacility.equals(TieredFacility.SOCKET)) {
            if (!currentState.equals(FacilityState.ACTIVE)) {
                reportConnectionState(currentFacility, FacilityState.WAITING);
                bluetoothConnection.openConnections(bluetoothDevice);
                Log.i(CLSS, String.format("%s: Set connection to %s (%s %s)",CLSS, bluetoothDevice.getName(), bluetoothDevice.getType(), bluetoothDevice.getAddress()));
            }
            // Start socket
            else {
                Handler mainHandler = new Handler(this.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        analyzer.start();
                        analyzer.listen();
                        bluetoothConnection.readInThread();
                    }
                });
                reportConnectionState(TieredFacility.VOICE,FacilityState.WAITING);
            }
        }
        else if( currentFacility.equals(TieredFacility.VOICE)) {
            if( isMuted ) {
                reportConnectionState(currentFacility, FacilityState.WAITING);
            }
            else {
                reportConnectionState(currentFacility, FacilityState.ACTIVE);
            }
        }
    }

    // Update any observers with the current state
    private void reportConnectionState(TieredFacility fac, FacilityState state) {
        Log.i(CLSS,String.format("reportConnectionState: %s %s",fac.name(),state.name()));
        ServiceStatusManager.getInstance().reportState(fac,state);
    }

    // Update any observers with the latest text
    // Send text to the robot for processing.
    private void reportSpokenText(String text) {
        Log.i(CLSS,String.format("reportSpokenText: %s",text));
        SpokenTextManager.getInstance().processText(text, MessageType.REQUEST);
        bluetoothConnection.write(text);
    }

    private void stopForegroundService() {
        Log.i(CLSS, "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);
        BluetoothManager bmgr = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        assert bmgr != null;
        bmgr.getAdapter().cancelDiscovery();

        // Stop the foreground service.
        stopSelf();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if(!ServiceStatusManager.getInstance().getStateForFacility(TieredFacility.VOICE).equals(FacilityState.IDLE) ) {
            determineNextAction(TieredFacility.VOICE);
        }

    }

    //=================================== VoiceServiceHandler ==============================================
    /**
     * There was an error in the bluetooth connection attempt.
     * @param reason error description
     */
    public void handleBluetoothError(String reason) {
        reportConnectionState(TieredFacility.BLUETOOTH,FacilityState.ERROR);
        reportSpokenText(reason);
        new Thread(new ProcessDelay(TieredFacility.BLUETOOTH,ERROR_CYCLE_DELAY)).start();
    }
    /**
     * The bluetooth connection request succeeded.
     */
    public void receiveBluetoothConnection() {
        reportConnectionState(TieredFacility.BLUETOOTH,FacilityState.ACTIVE);
        determineNextAction(TieredFacility.BLUETOOTH);
    }
    /**
     * There was an error in the attempt to create/open sockets.
     * @param reason error description
     */
    public void handleSocketError(String reason) {
        reportConnectionState(TieredFacility.SOCKET,FacilityState.ERROR);
        reportSpokenText(reason);
        new Thread(new ProcessDelay(TieredFacility.SOCKET,ERROR_CYCLE_DELAY)).start();
    }
    /**
     * The socket connection request succeeded.
     */
    public void receiveSocketConnection() {
        reportConnectionState(TieredFacility.SOCKET,FacilityState.ACTIVE);
        determineNextAction(TieredFacility.SOCKET);
    }
    /**
     * The speech recognizer reported an error.
     * @param reason error description
     */
    public void handleVoiceError(String reason) {
        reportConnectionState(TieredFacility.VOICE,FacilityState.ERROR);
        reportSpokenText(reason);
        new Thread(new ProcessDelay(TieredFacility.VOICE,ERROR_CYCLE_DELAY)).start();
    }
    /**
     * The speech recognizer recorded a result.
     */
    public void receiveText(String text) {
        reportConnectionState(TieredFacility.VOICE,FacilityState.ACTIVE);
        reportSpokenText(text);
    }
    //=================================== ProcessDelay ==============================================
    /**
     * Use this class to delay the transition to the next step. When we find
     * an error, we need to avoid a hard loop.
     */
    public class ProcessDelay implements Runnable {
        private final TieredFacility facility;
        private final long sleepInterval;
        /**
         * Constructor:
         * @param delay millisecs to wait before going to the next state (or more
         *              likely retrying the current).
         */
        public ProcessDelay(TieredFacility fac,long delay) {
            this.facility = fac;
            this.sleepInterval = delay;
        }

        public void run() {
            try{
                Thread.currentThread().sleep(sleepInterval);
                determineNextAction(facility);
            }
            catch(InterruptedException ignore) {}
        }
    }

}