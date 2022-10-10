/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.R
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import chuckcoughlin.bertspeak.bert.MessageType
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.db.DatabaseManager
import java.lang.IllegalArgumentException
import java.util.*

/**
 * This is a foreground service and may be turned on/off with a notifications interface.
 * The voice service manages connections between the robot as and speech/logging facilities.
 * It accepts voice commands from the socket connection from the robot and updates listeners with
 * the resulting text. The listeners handle text enunciation and logging.
 *
 * The service relies on a Bluetooth connection, socket communication and the
 * Android speech recognition classes.
 */
class DispatchService : Service(), BluetoothHandler {
    @Volatile
    private var notificationManager: NotificationManager? = null
    private var bluetoothConnection: BluetoothConnection? = null // Stays null when simulated
    private var bluetoothDevice: BluetoothDevice? = null
    private val binder: DispatchServiceBinder
    private var dbManager: DatabaseManager? = null
    private var statusManager: StatusManager? = null
    private var textManager: TextManager? = null
    private var isMuted = false
    private var simulatedConnectionMode: Boolean

    /**
     * Display a notification about us starting.  We put an icon in the status bar.
     * Initialize all the singletons.
     */
    override fun onCreate() {
        Log.i(CLSS, "onCreate: Starting foreground service ...")
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification()
        dbManager = DatabaseManager(applicationContext)
        val flag: String = dbManager.getSetting(BertConstants.BERT_SIMULATED_CONNECTION)
        if ("true".equals(flag, ignoreCase = true)) simulatedConnectionMode = true
        startForeground(DISPATCH_NOTIFICATION, notification)
        statusManager = StatusManager()
        textManager = TextManager()
    }

    /**
     * The initial intent action is null. Otherwise we receive values when the user clicks on the
     * notification buttons.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var action: String? = null
        if (intent != null) action = intent.getAction()
        Log.i(CLSS, String.format("onStartCommand: %s flags = %d, id = %d", action, flags, startId))
        if (!simulatedConnectionMode) bluetoothConnection = BluetoothConnection(this)
        if (action == null) {
            if (simulatedConnectionMode) {
                reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.ACTIVE)
                reportConnectionState(
                    TieredFacility.SOCKET,
                    FacilityState.ACTIVE
                ) // Just to initialize
                reportConnectionState(
                    TieredFacility.VOICE,
                    FacilityState.IDLE
                ) // Just to initialize
                determineNextAction(TieredFacility.VOICE)
            } else {
                reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.IDLE)
                reportConnectionState(
                    TieredFacility.SOCKET,
                    FacilityState.IDLE
                ) // Just to initialize
                reportConnectionState(
                    TieredFacility.VOICE,
                    FacilityState.IDLE
                ) // Just to initialize
                determineNextAction(TieredFacility.BLUETOOTH)
            }
        } else if (action.equals(getString(R.string.notificationMute), ignoreCase = true)) {
            toggleMute()
        } else if (action.equals(getString(R.string.notificationReset), ignoreCase = true)) {
            if (bluetoothConnection != null) bluetoothConnection!!.shutdown()
            statusManager!!.reportState(TieredFacility.SOCKET, FacilityState.IDLE)
            determineNextAction(TieredFacility.BLUETOOTH)
        } else if (action.equals(getString(R.string.notificationStop), ignoreCase = true)) {
            stopSelf()
        }
        return START_STICKY
    }

    // A client is binding to the service with bindService(). This appears to
    // be called only once no matter how many clients.
    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return true
    }

    // A client has called bindService after calling unBind()
    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
    }

    /**
     * Shutdown the services and the singletons.
     */
    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancelAll()
        if (bluetoothConnection != null) bluetoothConnection!!.shutdown()
        statusManager!!.stop()
        textManager!!.stop()
        stopForegroundService()
    }

    fun isSimulatedConnectionMode(): Boolean {
        return simulatedConnectionMode
    }

    override fun setBluetoothDevice(device: BluetoothDevice?) {
        bluetoothDevice = device
    }

    /**
     * Build a notification with
     * --- reset
     * ---- stop
     * ---- mute
     */
    private fun buildNotification(): Notification {

        // Create notification default intent.
        val intent = Intent()
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // Create notification builder.
        val builder: NotificationCompat.Builder =
            Builder(this, BertConstants.NOTIFICATION_CHANNEL_ID)

        // Make notification show big text.
        val bigTextStyle: NotificationCompat.BigTextStyle = BigTextStyle()
        bigTextStyle.setBigContentTitle(getString(R.string.notificationLabel))
        bigTextStyle.bigText(getString(R.string.notificationDescription))
        // Set big text style.
        builder.setStyle(bigTextStyle)
        builder.setWhen(System.currentTimeMillis())
        builder.setSmallIcon(R.mipmap.ic_launcher)
        val largeIconBitmap: Bitmap =
            BitmapFactory.decodeResource(resources, R.drawable.rounded_button)
        builder.setLargeIcon(largeIconBitmap)
        // Make the notification max priority.
        builder.setPriority(NotificationManager.IMPORTANCE_DEFAULT)
        // Make head-up notification.
        builder.setFullScreenIntent(pendingIntent, true)

        //  Reset Button
        val startIntent = Intent(this, NotificationActionReceiver::class.java)
        var action = getString(R.string.notificationReset)
        startIntent.setAction(action)
        val pendingStartIntent: PendingIntent = PendingIntent.getBroadcast(this, 1, startIntent, 0)
        val resetAction: NotificationCompat.Action =
            Action(R.drawable.ic_media_play, action, pendingStartIntent)
        builder.addAction(resetAction)
        builder.setOngoing(true)

        // Mute button
        val muteIntent = Intent(this, NotificationActionReceiver::class.java)
        action = getString(R.string.notificationMute)
        muteIntent.setAction(action)
        val pendingMuteIntent: PendingIntent = PendingIntent.getBroadcast(this, 2, muteIntent, 0)
        val muteAction: NotificationCompat.Action =
            Action(R.drawable.ic_media_pause, action, pendingMuteIntent)
        builder.addAction(muteAction)
        builder.setOngoing(true)

        // Stop button
        val stopIntent = Intent(this, NotificationActionReceiver::class.java)
        action = getString(R.string.notificationStop)
        stopIntent.setAction(action)
        val pendingStopIntent: PendingIntent = PendingIntent.getBroadcast(this, 3, stopIntent, 0)
        val stopAction: NotificationCompat.Action =
            Action(R.drawable.ic_lock_power_off, action, pendingStopIntent)
        builder.addAction(stopAction)
        builder.setOngoing(true)

        // Build the notification.
        return builder.build()
    }

    // Start the 3 stages in order
    private fun determineNextAction(currentFacility: TieredFacility) {
        val currentState = statusManager!!.getStateForFacility(currentFacility)
        if (currentFacility == TieredFacility.BLUETOOTH) {
            if (currentState != FacilityState.ACTIVE) {
                var name: String = dbManager.getSetting(BertConstants.BERT_PAIRED_DEVICE)
                if (name == null) name = "UNKNOWN"
                val checker = BluetoothChecker(this, name)
                reportConnectionState(currentFacility, FacilityState.WAITING)
                checker.beginChecking(getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
            } else {
                reportConnectionState(TieredFacility.SOCKET, FacilityState.WAITING)
                if (bluetoothConnection != null) bluetoothConnection!!.openConnections(
                    bluetoothDevice
                )
            }
        } else if (currentFacility == TieredFacility.SOCKET) {
            if (currentState != FacilityState.ACTIVE) {
                reportConnectionState(currentFacility, FacilityState.WAITING)
                if (bluetoothConnection != null) bluetoothConnection!!.openConnections(
                    bluetoothDevice
                )
                Log.i(
                    CLSS,
                    String.format(
                        "%s: Set connection to %s (%s %s)",
                        CLSS,
                        bluetoothDevice.getName(),
                        bluetoothDevice.getType(),
                        bluetoothDevice.getAddress()
                    )
                )
            } else {
                val mainHandler = Handler(this.mainLooper)
                mainHandler.post {
                    if (bluetoothConnection != null) {
                        bluetoothConnection!!.readInThread()
                    } else {
                        try {
                            Thread.sleep(ERROR_CYCLE_DELAY)
                        } catch (ignore: InterruptedException) {
                        }
                    }
                }
                reportConnectionState(TieredFacility.VOICE, FacilityState.WAITING)
            }
        } else if (currentFacility == TieredFacility.VOICE) {
            if (isMuted) {
                reportConnectionState(currentFacility, FacilityState.WAITING)
            } else {
                reportConnectionState(currentFacility, FacilityState.ACTIVE)
            }
        }
    }

    private fun stopForegroundService() {
        Log.i(CLSS, "Stop foreground service.")

        // Stop foreground service and remove the notification.
        stopForeground(true)
        val bmgr: BluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        if (bmgr != null) {
            bmgr.getAdapter().cancelDiscovery()
        }
        // Stop the foreground service.
        stopSelf()
    }

    private fun toggleMute() {
        isMuted = !isMuted
        if (statusManager!!.getStateForFacility(TieredFacility.VOICE) != FacilityState.IDLE) {
            determineNextAction(TieredFacility.VOICE)
        }
    }
    //=================================== BluetoothHandler ==============================================
    /**
     * There was an error in the bluetooth connection attempt.
     * @param reason error description
     */
    override fun handleBluetoothError(reason: String) {
        reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(TieredFacility.BLUETOOTH, ERROR_CYCLE_DELAY)).start()
    }

    /**
     * The bluetooth connection request succeeded.
     */
    override fun receiveBluetoothConnection() {
        reportConnectionState(TieredFacility.BLUETOOTH, FacilityState.ACTIVE)
        determineNextAction(TieredFacility.BLUETOOTH)
    }

    /*
     * Update any observers with the current state. Additionally create a log entry.
     */
    override fun reportConnectionState(fac: TieredFacility, state: FacilityState) {
        Log.i(CLSS, String.format("reportConnectionState: %s %s", fac.name, state.name))
        val msg = String.format("Connection state: %s %s", fac.name, state.name)
        statusManager!!.reportState(fac, state)
        textManager!!.processText(MessageType.LOG, msg)
    }

    /**
     * There was an error in the attempt to create/open sockets.
     * @param reason error description
     */
    override fun handleSocketError(reason: String) {
        reportConnectionState(TieredFacility.SOCKET, FacilityState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(TieredFacility.SOCKET, ERROR_CYCLE_DELAY)).start()
    }

    /**
     * The socket connection request succeeded.
     */
    override fun receiveSocketConnection() {
        reportConnectionState(TieredFacility.SOCKET, FacilityState.ACTIVE)
        determineNextAction(TieredFacility.SOCKET)
    }

    /**
     * The bluetooth reader recorded a result. The text starts with a
     * MessageType header.
     */
    override fun receiveText(text: String) {
        var text = text
        if (text.length > 4) {
            Log.i(CLSS, String.format("receiveText: (%s)", text))
            try {
                val hdr = text.substring(0, BertConstants.HEADER_LENGTH)
                val type = MessageType.valueOf(hdr.uppercase(Locale.getDefault()))
                text = text.substring(BertConstants.HEADER_LENGTH + 1)
                textManager!!.processText(type, text)
            } catch (iae: IllegalArgumentException) {
                Log.w(CLSS, String.format("receiveText: (%s) has unrecognizedd header", text))
            }
        } else {
            Log.w(CLSS, String.format("receiveText: (%s) is too short", text))
        }
    }

    /**
     * The speech recognizer reported an error.
     * @param reason error description
     */
    override fun handleVoiceError(reason: String) {
        reportConnectionState(TieredFacility.VOICE, FacilityState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(TieredFacility.VOICE, ERROR_CYCLE_DELAY)).start()
    }

    /**
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     * The text originates from the speech recognizer on the tablet (or an error).
     */
    override fun receiveSpokenText(text: String) {
        Log.i(CLSS, String.format("receiveSpokenText: %s", text))
        textManager!!.processText(MessageType.MSG, text)
        if (bluetoothConnection != null) {
            bluetoothConnection!!.write(String.format("%s:%s", MessageType.MSG.name, text))
        }
    }
    //=================================== ProcessDelay ==============================================
    /**
     * Use this class to delay the transition to the next step. When we find
     * an error, we need to avoid a hard loop.
     */
    inner class ProcessDelay
    /**
     * Constructor:
     * @param delay millisecs to wait before going to the next state (or more
     * likely retrying the current).
     */(private val facility: TieredFacility, private val sleepInterval: Long) : Runnable {
        override fun run() {
            try {
                Thread.sleep(sleepInterval)
                determineNextAction(facility)
            } catch (ignore: InterruptedException) {
            }
        }
    }

    // ===================================== Methods Exposed thru Service Binder ====================================
    fun getStatusManager(): StatusManager? {
        return statusManager
    }

    fun getTextManager(): TextManager? {
        return textManager
    }

    companion object {
        private const val CLSS = "DispatchService"
        private const val ERROR_CYCLE_DELAY: Long = 15000 // Wait interval for retry after error
        private val DISPATCH_NOTIFICATION: Int =
            R.string.notificationKey // Unique id for the Notification.
    }

    //private static final boolean IS_EMULATOR = Build.HARDWARE.contains("goldfish");
    //private static final boolean IS_EMULATOR Build.IS_EMULATOR;
    init {
        binder = DispatchServiceBinder(this)
        simulatedConnectionMode = false
    }
}
