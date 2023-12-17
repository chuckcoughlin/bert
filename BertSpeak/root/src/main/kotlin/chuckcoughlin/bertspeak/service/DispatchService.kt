/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import chuckcoughlin.bertspeak.R
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.db.DatabaseManager
import chuckcoughlin.bertspeak.service.DispatchService.Companion.DISPATCH_NOTIFICATION
import chuckcoughlin.bertspeak.service.DispatchService.Companion.ERROR_CYCLE_DELAY
import chuckcoughlin.bertspeak.service.DispatchService.Companion.bluetoothManager
import chuckcoughlin.bertspeak.service.DispatchService.Companion.notificationManager
import chuckcoughlin.bertspeak.service.DispatchService.isMuted
import chuckcoughlin.bertspeak.service.DispatchService.simulatedConnectionMode
import chuckcoughlin.bertspeak.service.DispatchService.statusManager
import chuckcoughlin.bertspeak.service.DispatchService.textManager
import java.util.Locale


/**
 * This is the foreground service and may be turned on/off with a notifications interface.
 * The voice service manages connections between the robot as and speech/logging facilities.
 * It accepts voice commands from the socket connection from the robot and updates listeners
 * with the resulting text. The listeners handle text enunciation and logging.
 *
 * The service relies on a Bluetooth connection, socket communication and the
 * Android speech recognition classes.
 */
object DispatchService : Service(), BluetoothHandler {
    val binder: DispatchServiceBinder
    val isMuted:Boolean
    var simulatedConnectionMode: Boolean
    val statusManager: StatusManager
    val textManager: TextManager


    private var bluetoothConnection: BluetoothConnection? = null // Stays null when simulated
    private var bluetoothDevice: BluetoothDevice? = null

    /**
     * Display a notification about us starting.  We put an icon in the status bar.
     * Initialize all the singletons.
     */
    override fun onCreate() {
        Log.i(CLSS, "onCreate: Starting foreground service ...")
        val notification = buildNotification()
        val flag: String? = DatabaseManager.getSetting(BertConstants.BERT_SIMULATED_CONNECTION)
        if ("true".equals(flag, ignoreCase = true)) simulatedConnectionMode = true
        startForeground(DISPATCH_NOTIFICATION, notification)
    }
    /**
     * The initial intent action is null. Otherwise we receive values when the user clicks on the
     * notification buttons.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if( intent!=null ) {
            val action: String? = intent.action
            Log.i(CLSS,String.format("onStartCommand: %s flags = %d, id = %d",
                    action, flags, startId))
            if (!simulatedConnectionMode) bluetoothConnection = BluetoothConnection(this)
            if (action == null) {
                if (simulatedConnectionMode) {
                    reportConnectionState(ControllerType.BLUETOOTH, ControllerState.ACTIVE)
                    reportConnectionState(ControllerType.SOCKET,ControllerState.ACTIVE) // Just to initialize
                    reportConnectionState(ControllerType.VOICE,ControllerState.OFF)    // Just to initialize
                    determineNextAction(ControllerType.VOICE)
                }
                else {
                    reportConnectionState(ControllerType.BLUETOOTH, ControllerState.OFF)
                    reportConnectionState(ControllerType.SOCKET, ControllerState.OFF)   // Just to initialize
                    reportConnectionState(ControllerType.VOICE,ControllerState.OFF)    // Just to initialize
                    determineNextAction(ControllerType.BLUETOOTH)
                }
            }
            else if (action.equals(getString(R.string.notificationMute), ignoreCase = true)) {
                toggleMute()
            }
            else if (action.equals(getString(R.string.notificationReset), ignoreCase = true)) {
                if (bluetoothConnection != null) bluetoothConnection!!.shutdown()
                statusManager.reportState(ControllerType.SOCKET, ControllerState.OFF)
                determineNextAction(ControllerType.BLUETOOTH)
            }
            else if (action.equals(getString(R.string.notificationStop), ignoreCase = true)) {
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    // A client is binding to the service with bindService().
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    /**
     * Shutdown the services and the singletons.
     */
    override fun onDestroy() {
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)  // Notification remains showing

        if(ActivityCompat.checkSelfPermission(this, permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothManager.adapter?.cancelDiscovery()
            if (bluetoothConnection != null) bluetoothConnection!!.shutdown()
        }
        notificationManager?.cancelAll()
        statusManager.stop()
        textManager.stop()
        stopSelf()
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
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Create notification builder.
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, BertConstants.NOTIFICATION_CHANNEL_ID)

        // Make notification show big text.
        val bigTextStyle: NotificationCompat.BigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(getString(R.string.notificationLabel))
        bigTextStyle.bigText(getString(R.string.notificationDescription))
        // Set big text style.
        builder.setStyle(bigTextStyle)
        builder.setWhen(System.currentTimeMillis())
        builder.setSmallIcon(R.mipmap.ic_launcher)
        val drawable = ResourcesCompat.getDrawable(resources,R.drawable.rounded_button,null)
        val largeIconBitmap: Bitmap = createBitmapFromDrawable(drawable!!)
        builder.setLargeIcon(largeIconBitmap)
        // Make the notification max priority.
        builder.priority = NotificationManager.IMPORTANCE_DEFAULT
        // Make head-up notification.
        builder.setFullScreenIntent(pendingIntent, true)

        //  Reset Button
        val startIntent = Intent(this, NotificationActionReceiver::class.java)
        var action = getString(R.string.notificationReset)
        startIntent.action = action
        val pendingStartIntent: PendingIntent = PendingIntent.getBroadcast(this, 1, startIntent, PendingIntent.FLAG_IMMUTABLE)
        val resetAction: NotificationCompat.Action =
            NotificationCompat.Action(android.R.drawable.ic_media_play, action, pendingStartIntent)
        builder.addAction(resetAction)
        builder.setOngoing(true)

        // Mute button
        val muteIntent = Intent(this, NotificationActionReceiver::class.java)
        action = getString(R.string.notificationMute)
        muteIntent.action = action
        val pendingMuteIntent: PendingIntent = PendingIntent.getBroadcast(this, 2, muteIntent, PendingIntent.FLAG_IMMUTABLE)
        val muteAction: NotificationCompat.Action =
            NotificationCompat.Action(android.R.drawable.ic_media_pause, action, pendingMuteIntent)
        builder.addAction(muteAction)
        builder.setOngoing(true)

        // Stop button
        val stopIntent = Intent(this, NotificationActionReceiver::class.java)
        action = getString(R.string.notificationStop)
        stopIntent.action = action
        val pendingStopIntent: PendingIntent = PendingIntent.getBroadcast(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction: NotificationCompat.Action =
            NotificationCompat.Action(android.R.drawable.ic_lock_power_off, action, pendingStopIntent)
        builder.addAction(stopAction)
        builder.setOngoing(true)

        // Build the notification.
        return builder.build()
    }
    // With a vector image, work with the bitmap
    private fun createBitmapFromDrawable(drawable: Drawable) : Bitmap {
        //val bitmap = Bitmap.createBitmap(drawable.minimumWidth,drawable.minimumHeight,                                   Bitmap.Config.ARGB_8888)
        val bitmap = Bitmap.createBitmap(40,40,                                   Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    // Start the 3 stages in order
    @SuppressLint("MissingPermission")
    private fun determineNextAction(currentFacility: ControllerType) {
        val currentState = statusManager.getStateForController(currentFacility)
        if (currentFacility == ControllerType.BLUETOOTH) {
            if (currentState != ControllerState.ACTIVE) {
                var name: String? = DatabaseManager.getSetting(BertConstants.BERT_PAIRED_DEVICE)
                if (name == null) name = "UNKNOWN"
                val checker = BluetoothChecker(this, name)
                reportConnectionState(currentFacility, ControllerState.PENDING)
                checker.beginChecking(getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
            }
            else {
                reportConnectionState(ControllerType.SOCKET, ControllerState.PENDING)
                if (bluetoothConnection != null) bluetoothConnection!!.openConnections(
                    bluetoothDevice
                )
            }
        }
        else if (currentFacility == ControllerType.SOCKET) {
            if (currentState != ControllerState.ACTIVE) {
                reportConnectionState(currentFacility, ControllerState.PENDING)
                if (bluetoothConnection != null) {
                    bluetoothConnection!!.openConnections(bluetoothDevice)
                }
                Log.i(CLSS,String.format("%s: Set connection to %s (%s %s)", CLSS,
                        bluetoothDevice!!.name,
                        bluetoothDevice!!.type,
                        bluetoothDevice!!.address
                    )
                )
            }
            else {
                val mainHandler = Handler(this.mainLooper)
                mainHandler.post {
                    if (bluetoothConnection != null) {
                        bluetoothConnection!!.readInThread()
                    }
                    else {
                        try {
                            Thread.sleep(ERROR_CYCLE_DELAY)
                        }
                        catch (ignore: InterruptedException) {}
                    }
                }
                reportConnectionState(ControllerType.VOICE, ControllerState.PENDING)
            }
        }
        else if (currentFacility == ControllerType.VOICE) {
            if (isMuted) {
                reportConnectionState(currentFacility, ControllerState.PENDING)
            }
            else {
                reportConnectionState(currentFacility, ControllerState.ACTIVE)
            }
        }
    }



    private fun toggleMute() {
        isMuted = !isMuted
        if (statusManager.getStateForController(ControllerType.VOICE) != ControllerState.OFF) {
            determineNextAction(ControllerType.VOICE)
        }
    }
    //=========================== BluetoothHandler ===============================
    /**
     * There was an error in the bluetooth connection attempt.
     * @param reason error description
     */
    override fun handleBluetoothError(reason: String) {
        reportConnectionState(ControllerType.BLUETOOTH, ControllerState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(ControllerType.BLUETOOTH, ERROR_CYCLE_DELAY)).start()
    }

    /**
     * The bluetooth connection request succeeded.
     */
    override fun receiveBluetoothConnection() {
        reportConnectionState(ControllerType.BLUETOOTH, ControllerState.ACTIVE)
        determineNextAction(ControllerType.BLUETOOTH)
    }

    /*
     * Update any observers with the current state. Additionally create a log entry.
     */
    override fun reportConnectionState(fac: ControllerType, state: ControllerState) {
        Log.i(CLSS, String.format("reportConnectionState: %s %s", fac.name, state.name))
        val msg = String.format("Connection state: %s %s", fac.name, state.name)
        statusManager.reportState(fac, state)
        textManager.processText(MessageType.LOG, msg)
    }

    /**
     * There was an error in the attempt to create/open sockets.
     * @param reason error description
     */
    override fun handleSocketError(reason: String) {
        reportConnectionState(ControllerType.SOCKET, ControllerState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(ControllerType.SOCKET, ERROR_CYCLE_DELAY)).start()
    }

    /**
     * The socket connection request succeeded.
     */
    override fun receiveSocketConnection() {
        reportConnectionState(ControllerType.SOCKET, ControllerState.ACTIVE)
        determineNextAction(ControllerType.SOCKET)
    }

    /**
     * The bluetooth reader recorded a result. The text starts with a
     * MessageType header.
     */
    override fun receiveText(text: String) {
        var txt = text
        if (txt.length > 4) {
            Log.i(CLSS, String.format("receiveText: (%s)", txt))
            try {
                val hdr = txt.substring(0, BertConstants.HEADER_LENGTH)
                val type = MessageType.valueOf(hdr.uppercase(Locale.getDefault()))
                txt = txt.substring(BertConstants.HEADER_LENGTH + 1)
                textManager.processText(type, txt)
            }
            catch (iae: IllegalArgumentException) {
                Log.w(CLSS, String.format("receiveText: (%s) has unrecognized header", txt))
            }
        }
        else {
            Log.w(CLSS, String.format("receiveText: (%s) is too short", txt))
        }
    }

    /**
     * The speech recognizer reported an error.
     * @param reason error description
     */
    override fun handleVoiceError(reason: String) {
        reportConnectionState(ControllerType.VOICE, ControllerState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(ControllerType.VOICE, ERROR_CYCLE_DELAY)).start()
    }

    /**
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     * The text originates from the speech recognizer on the tablet (or an error).
     */
    override fun receiveSpokenText(text: String) {
        Log.i(CLSS, String.format("receiveSpokenText: %s", text))
        textManager.processText(MessageType.MSG, text)
        if (bluetoothConnection != null) {
            bluetoothConnection!!.write(String.format("%s:%s", MessageType.MSG.name, text))
        }
    }
    //================= ProcessDelay ==========================
    /**
     * Use this class to delay the transition to the next step. When we find
     * an error, we need to avoid a hard loop.
     */
    inner class ProcessDelay {
    /**
     * Constructor:
     * @param sleepInterval milliseconds to wait before going to the next state (or more
     * likely retrying the current).
     */
    private val facility: ControllerType, private val sleepInterval: Long) : Runnable {
        override fun run() {
            try {
                Thread.sleep(sleepInterval)
                determineNextAction(facility)
            }
            catch (ignore: InterruptedException) {
            }
        }
    }

    // ================= Methods Exposed thru Service Binder ==========================
    /*
    fun getStatusManager(): StatusManager? {
        return statusManager
    }


    fun getTextManager(): TextManager {
        return textManager
    }
    */

        private var bluetoothManager :BluetoothManager
        private var notificationManager:NotificationManager? = null
        // Start foreground service
        fun startForegroundService(context: Context) {
            bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val startIntent = Intent(context, DispatchService::class.java)
            val message: String = context.getString(R.string.dispatchStartMessage)
            startIntent.putExtra(context.getString(R.string.dispatchStartIntent), message)
            ContextCompat.startForegroundService(context, startIntent)
            val channel = NotificationChannel(BertConstants.NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.dispatchChannelName),
                NotificationManager.IMPORTANCE_HIGH)
            channel.description = context.getString(R.string.dispatchChannelDesc)
            notificationManager!!.createNotificationChannel(channel)
        }

        // Stop foreground service and remove the notification.
        fun stopForegroundService(context: Context) {
            Log.i(CLSS, "Stop foreground service.")
            val stopIntent = Intent(context, DispatchService::class.java)
            context.stopService(stopIntent)
        }


    private const val CLSS = "DispatchService"
    private const val ERROR_CYCLE_DELAY: Long = 15000 // Wait interval for retry after error
    private val DISPATCH_NOTIFICATION: Int = R.string.notificationKey // Unique id for the Notification.

    init {
        binder = DispatchServiceBinder(this)
        isMuted = false
        simulatedConnectionMode = false
        statusManager = StatusManager()
        textManager = TextManager()
    }
}
