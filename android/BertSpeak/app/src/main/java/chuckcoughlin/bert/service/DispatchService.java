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
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import chuckcoughlin.bert.R;
import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.common.IntentObserver;
import chuckcoughlin.bert.common.MessageType;
import chuckcoughlin.bert.db.DatabaseManager;
import chuckcoughlin.bert.speech.TextMessageObserver;

/**
 * This is a foreground service and may be turned on/off with a notifications interface.
 * The voice service manages connections between the robot as and speech/logging facilities.
 * It accepts voice commands from the socket connection from the robot and updates listeners with
 * the resulting text. The listeners handle text enunciation and logging.
 *
 * The service relies on a Bluetooth connection, socket communication and the
 * Android speech recognition classes.
 */
public class DispatchService extends Service implements BluetoothHandler {
    private static final String CLSS = "DispatchService";
    private static final long ERROR_CYCLE_DELAY = 15000;   // Wait interval for retry after error
    private volatile NotificationManager notificationManager;
    private BluetoothConnection bluetoothConnection = null; // Stays null when simulated
    private BluetoothDevice bluetoothDevice = null;
    private final DispatchServiceBinder binder;
    private DatabaseManager dbManager = null;
    private StatusManager statusManager = null;
    private TextManager textManager = null;
    private boolean isMuted;
    private boolean simulatedConnectionMode;
    private static final int DISPATCH_NOTIFICATION = R.string.notificationKey; // Unique id for the Notification.

    //private static final boolean IS_EMULATOR = Build.HARDWARE.contains("goldfish");
    //private static final boolean IS_EMULATOR Build.IS_EMULATOR;

    public DispatchService() {
        this.isMuted = false;
        this.binder = new DispatchServiceBinder(this);
        this.simulatedConnectionMode = false;
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
        dbManager = new DatabaseManager(getApplicationContext());
        String flag = dbManager.getSetting(BertConstants.BERT_SIMULATED_CONNECTION);
        if("true".equalsIgnoreCase(flag))simulatedConnectionMode = true;
        startForeground(DISPATCH_NOTIFICATION, notification);
        statusManager = new StatusManager();
        textManager   = new TextManager();
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
        if( !simulatedConnectionMode ) bluetoothConnection = new BluetoothConnection(this);

        if( action==null) {
            if( simulatedConnectionMode ) {
                reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.ACTIVE);
                reportConnectionState(TieredFacility.SOCKET, FacilityState.ACTIVE);  // Just to initialize
                reportConnectionState(TieredFacility.VOICE, FacilityState.IDLE);   // Just to initialize
                determineNextAction(TieredFacility.VOICE);
            }
            else {
                reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.IDLE);
                reportConnectionState(TieredFacility.SOCKET, FacilityState.IDLE);  // Just to initialize
                reportConnectionState(TieredFacility.VOICE, FacilityState.IDLE);   // Just to initialize
                determineNextAction(TieredFacility.BLUETOOTH);
            }
        }
        else if(action.equalsIgnoreCase(getString(R.string.notificationMute))) {
            toggleMute();
        }
        else if(action.equalsIgnoreCase(getString(R.string.notificationReset))) {
            if(bluetoothConnection !=null) bluetoothConnection.shutdown();
            statusManager.reportState(TieredFacility.SOCKET,FacilityState.IDLE);
            determineNextAction(TieredFacility.BLUETOOTH);
        }
        else if(action.equalsIgnoreCase(getString(R.string.notificationStop))) {
            stopSelf();
        }
        return(START_STICKY);
    }
    // A client is binding to the service with bindService(). This appears to
    // be called only once no matter how many clients.
    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {return true;}

     // A client has called bindService after calling unBind()
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    /**
     * Shutdown the services and the singletons.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancelAll();
        if(bluetoothConnection!=null) bluetoothConnection.shutdown();
        statusManager.stop();
        textManager.stop();
        stopForegroundService();
    }



    public boolean isSimulatedConnectionMode() { return this.simulatedConnectionMode; }
    public void setBluetoothDevice(BluetoothDevice device) { this.bluetoothDevice = device; }
    /**
     * Build a notification with
     * --- reset
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

    // Start the 3 stages in order
    private void determineNextAction(TieredFacility currentFacility) {
        FacilityState currentState = statusManager.getStateForFacility(currentFacility);
        if( currentFacility.equals(TieredFacility.BLUETOOTH)) {
            if( !currentState.equals(FacilityState.ACTIVE)) {
                String name = dbManager.getSetting(BertConstants.BERT_PAIRED_DEVICE);
                if(name==null ) name = "UNKNOWN";
                BluetoothChecker checker = new BluetoothChecker(this,name);
                reportConnectionState(currentFacility,FacilityState.WAITING);
                checker.beginChecking((BluetoothManager)getSystemService(BLUETOOTH_SERVICE));
            }
            // Start socket
            else {
                reportConnectionState(TieredFacility.SOCKET,FacilityState.WAITING);
                if( bluetoothConnection!=null ) bluetoothConnection.openConnections(bluetoothDevice);
            }
        }
        else if( currentFacility.equals(TieredFacility.SOCKET)) {
            if (!currentState.equals(FacilityState.ACTIVE)) {
                reportConnectionState(currentFacility, FacilityState.WAITING);
                if( bluetoothConnection!=null )bluetoothConnection.openConnections(bluetoothDevice);
                Log.i(CLSS, String.format("%s: Set connection to %s (%s %s)",CLSS, bluetoothDevice.getName(), bluetoothDevice.getType(), bluetoothDevice.getAddress()));
            }
            // Start socket
            else {
                Handler mainHandler = new Handler(this.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if( bluetoothConnection!=null ) {
                            bluetoothConnection.readInThread();
                        }
                        else {
                            try {
                                Thread.sleep(ERROR_CYCLE_DELAY);
                            }
                            catch(InterruptedException ignore) {}
                        }
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
        if(!statusManager.getStateForFacility(TieredFacility.VOICE).equals(FacilityState.IDLE) ) {
            determineNextAction(TieredFacility.VOICE);
        }

    }

    //=================================== BluetoothHandler ==============================================
    /**
     * There was an error in the bluetooth connection attempt.
     * @param reason error description
     */
    public void handleBluetoothError(String reason) {
        reportConnectionState(TieredFacility.BLUETOOTH,FacilityState.ERROR);
        receiveSpokenText(reason);
        new Thread(new ProcessDelay(TieredFacility.BLUETOOTH,ERROR_CYCLE_DELAY)).start();
    }
    /**
     * The bluetooth connection request succeeded.
     */
    public void receiveBluetoothConnection() {
        reportConnectionState(TieredFacility.BLUETOOTH,FacilityState.ACTIVE);
        determineNextAction(TieredFacility.BLUETOOTH);
    }
    /*
     * Update any observers with the current state. Additionally create a log entry.
     */
    public void reportConnectionState(TieredFacility fac, FacilityState state) {
        Log.i(CLSS,String.format("reportConnectionState: %s %s",fac.name(),state.name()));
        String msg = String.format("Connection state: %s %s",fac.name(),state.name());
        statusManager.reportState(fac,state);
        textManager.processText(MessageType.LOG,msg);
    }
    /**
     * There was an error in the attempt to create/open sockets.
     * @param reason error description
     */
    public void handleSocketError(String reason) {
        reportConnectionState(TieredFacility.SOCKET,FacilityState.ERROR);
        receiveSpokenText(reason);
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
     * The bluetooth reader recorded a result. The text starts with a
     * MessageType header.
     */
    public void receiveText(String text) {
        if( text.length() > 4) {
            Log.i(CLSS,String.format("receiveText: (%s)",text));
            try {
                String hdr = text.substring(0,BertConstants.HEADER_LENGTH);
                MessageType type = MessageType.valueOf(hdr.toUpperCase());
                text = text.substring(BertConstants.HEADER_LENGTH+1);
                textManager.processText(type, text);
            }
            catch(IllegalArgumentException iae) {
                Log.w(CLSS,String.format("receiveText: (%s) has unrecognizedd header",text));
            }
        }
        else {
            Log.w(CLSS,String.format("receiveText: (%s) is too short",text));
        }
    }

    /**
     * The speech recognizer reported an error.
     * @param reason error description
     */
    public void handleVoiceError(String reason) {
        reportConnectionState(TieredFacility.VOICE,FacilityState.ERROR);
        receiveSpokenText(reason);
        new Thread(new ProcessDelay(TieredFacility.VOICE,ERROR_CYCLE_DELAY)).start();
    }

    /**
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     * The text originates from the speech recognizer on the tablet (or an error).
     */
    public void receiveSpokenText(String text) {
        Log.i(CLSS,String.format("receiveSpokenText: %s",text));
        //textManager.processText(MessageType.MSG,text);
        if( bluetoothConnection!=null) {
            bluetoothConnection.write(String.format("%s:%s", MessageType.MSG.name(), text));
        }
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

    // ===================================== Methods Exposed thru Service Binder ====================================
    public void registerIntentObserver(IntentObserver observer)   { statusManager.register(observer); }
    public void unregisterIntentObserver(IntentObserver observer) { statusManager.unregister(observer);}

    public void registerLogViewer(TextMessageObserver observer) { textManager.registerLogViewer(observer);}
    public void registerTableViewer(TextMessageObserver observer) { textManager.registerTableViewer(observer);}
    public void registerTranscriptViewer(TextMessageObserver observer) {textManager.registerTranscriptViewer(observer);}
    public void unregisterLogViewer(TextMessageObserver observer) { textManager.unregisterLogViewer(observer);}
    public void unregisterTableViewer(TextMessageObserver observer) { textManager.unregisterTableViewer(observer);}
    public void unregisterTranscriptViewer(TextMessageObserver observer) {textManager.unregisterTranscriptViewer(observer);}
}