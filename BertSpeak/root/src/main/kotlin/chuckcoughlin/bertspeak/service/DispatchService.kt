/**
 * Copyright 2022-2023 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.MessageType.LOG
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.data.TextDataObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * This is the foreground service and may be turned on/off with a notifications interface.
 * The voice service manages connections between the robot as and speech/logging facilities.
 * It accepts voice commands from the socket connection from the robot and updates listeners
 * with the resulting text. The listeners handle text enunciation and logging.
 *
 * The service relies on a Bluetooth connection, socket communication and the
 * Android speech recognition classes.
 *
 * Use a delay the transition to the next step. When we find
 * an error, we need to avoid a hard loop.
 */
class DispatchService : Service(){
    val flag = false
    val annunciationManager: AnnunciationManager
    val geometryManager: GeometryManager
    val speechManager: SpeechManager
    val statusManager: StatusManager
    val textManager: TextManager


    // A client is binding to the service with bindService(). Not used.
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * The initial intent action is null. Otherwise we receive values when the user clicks on the
     * notification buttons.
     *  start speech analyzer and annunciator
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if( intent.action!=null ) {
            if(intent.action.equals(DispatchConstants.ACTION_START_SERVICE)) {
                Log.i(CLSS, "Received startup intent ");
                DispatchService.instance = this
                // Start those managers that run on the main (UI) thread
                statusManager.start()
                // Start those managers that run on a background thread (no UI)
                // This includes especially network handlers
                GlobalScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        annunciationManager.start()
                        geometryManager.start()
                    }
                }
            }
            else if(intent.action.equals(DispatchConstants.ACTION_STOP_SERVICE)) {
                Log.i(CLSS, String.format("Received shutdown intent ", intent.action!!))
                stopSelf();
            }
            else {
                Log.i(CLSS, String.format("Received unrecognized intent (%s)", intent.action!!))
                stopSelf();
            }
        }
        else {
            Log.e(CLSS, "Received null intent");
        }
        return START_STICKY;
    }


    /**
     * Shutdown the services and the singletons.
     */
    override fun onDestroy() {
        super.onDestroy()
        geometryManager.stop()
        statusManager.stop()
        textManager.stop()
        stopSelf()
    }

    /* ==============================================================================
     * Methods callable by the various managers which are given the service instance
     * ==============================================================================
     */
    // NOTE: This does not automatically set state to ERROR.
    fun logError(type:ManagerType,text:String) {
        textManager.processText(LOG,text)

    }
    fun reportManagerStatus(type:ManagerType, state:ManagerState) {
        statusManager.updateState(type,state)
    }
    fun startSpeech() {
        speechManager.activateSpeechAnalyzer()
    }
    fun stopSpeech() {
        speechManager.deactivateSpeechAnalyzer()
    }

    // Start the 3 stages in order
    /*
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

    override fun handleBluetoothError(reason: String) {
        reportConnectionState(ControllerType.BLUETOOTH, ControllerState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(ControllerType.BLUETOOTH, ERROR_CYCLE_DELAY)).start()
    }
     */v
    /**
     * The bluetooth connection request succeeded.

    override fun receiveBluetoothConnection() {
        reportConnectionState(ControllerType.BLUETOOTH, ControllerState.ACTIVE)
        determineNextAction(ControllerType.BLUETOOTH)
    }
     */
    /*
     * Update any observers with the current state. Additionally create a log entry.

    override fun reportConnectionState(fac: ControllerType, state: ControllerState) {
        Log.i(CLSS, String.format("reportConnectionState: %s %s", fac.name, state.name))
        val msg = String.format("Connection state: %s %s", fac.name, state.name)
        statusManager.reportState(fac, state)
        textManager.processText(MessageType.LOG, msg)
    }
*/
    /**
     * There was an error in the attempt to create/open sockets.
     * @param reason error description

    override fun handleSocketError(reason: String) {
        reportConnectionState(ControllerType.SOCKET, ControllerState.ERROR)
        receiveSpokenText(reason)
        Thread(ProcessDelay(ControllerType.SOCKET, ERROR_CYCLE_DELAY)).start()
    }
     */
    /**
     * The socket connection request succeeded.

    override fun receiveSocketConnection() {
        reportConnectionState(ControllerType.SOCKET, ControllerState.ACTIVE)
        determineNextAction(ControllerType.SOCKET)
    }
     */
    /**
     * The bluetooth reader recorded a result. The text starts with a
     * MessageType header.

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
     */
     */
    /**
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     * The text originates from the speech recognizer on the tablet (or an error).

    override fun receiveSpokenText(text: String) {
        Log.i(CLSS, String.format("receiveSpokenText: %s", text))
        textManager.processText(MessageType.MSG, text)
        if (bluetoothConnection != null) {
            bluetoothConnection!!.write(String.format("%s:%s", MessageType.MSG.name, text))
        }
    }

     */
    /* ==============================================================================
     * The companion object contains methods callable in a static way from components
       throughout the application.
     * ==============================================================================
     */
    companion object {
        var instance:DispatchService

        // Handle all the registrations
        fun registerForGeometry(obs: GeometryDataObserver) {
            instance.geometryManager.register(obs)
        }
        fun unregisterForGeometry(obs: GeometryDataObserver) {
            instance.geometryManager.unregister(obs)
        }
        fun registerForStatus(obs: StatusDataObserver) {
            instance.statusManager.register(obs)
        }
        fun unregisterForStatus(obs: StatusDataObserver) {
            instance.statusManager.unregister(obs)
        }
        fun registerForLogs(obs: TextDataObserver) {
            instance.textManager.registerLogViewer(obs)
        }
        fun unregisterForText(obs: TextDataObserver) {
            instance.textManager.unregisterLogViewer(obs)
        }
        fun registerForTranscripts(obs: TextDataObserver) {
            instance.textManager.registerTranscriptViewer(obs)
        }
        fun unregisterForLogs(obs: TextDataObserver) {
            instance.textManager.unregisterTranscriptViewer(obs)
        }
        fun clear(type: MessageType) {
            instance.textManager.clear(type)
        }
        fun restoreAudio() {
            //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
        }

        // Mute the beeps waiting for spoken input. At one point these methods were used to silence
        // annoying beeps with every onReadyForSpeech cycle. Currently they are not needed (??)
        fun suppressAudio() {
            //AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            //audio.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
        init {
            instance = DispatchService()
        }
    }
    val CLSS = "DispatchService"

    /*
     * Initially we start a rudimentary version of each controller
     */
    init {
        annunciationManager = AnnunciationManager(this)
        geometryManager = GeometryManager(this)
        speechManager = SpeechManager(this)
        statusManager = StatusManager(this)
        textManager = TextManager(this)
    }
}
