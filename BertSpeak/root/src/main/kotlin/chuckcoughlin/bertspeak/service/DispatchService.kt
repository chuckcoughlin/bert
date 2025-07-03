/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.content.Context
import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.MessageType.LOG
import chuckcoughlin.bertspeak.data.JsonObserver
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.StatusObserver
import chuckcoughlin.bertspeak.data.LogDataObserver
import chuckcoughlin.bertspeak.data.LinkShapeObserver
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale



/**
 * The dispatcher handles communication between components and a set of
 * managers. Manager responsibilities include:
 * connections between the robot and speech/logging facilities;
 * processing voice commands from the socket connection from the robot
 * and updating listeners with the resulting text.
 *
 * The service relies on socket communication and the
 * Android speech recognition classes.
 */
class DispatchService(ctx: Context){
    var context:Context
    var screenHeight: Int   // For use by various managers
    var screenWidth: Int
    lateinit var facesManager: FacesManager
    lateinit var geometryManager: GeometryManager
    lateinit var hearingManager: HearingManager
    lateinit var socketManager: SocketManager
    lateinit var speechManager: SpeechManager
    lateinit var statusManager: StatusManager
    lateinit var textManager: TextManager
    private var ignoring: Boolean

    /**
     * The order here is important
     */
    fun initialize() {
        Log.i(CLSS, "initialize: ... ")
        instance = this
        ignoring = false
        facesManager   = FacesManager(this)
        hearingManager = HearingManager(this)
        statusManager = StatusManager(this)
        textManager   = TextManager(this)
        geometryManager = GeometryManager(this)
        socketManager = SocketManager(this)
        speechManager = SpeechManager(this)
    }
    /**
     * This instance is started by the Application in a background
     * thread.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun start() {
        Log.i(CLSS, String.format("start: starting Dispatcher and managers"))
        instance = this
        // Start those managers that run on the main (UI) thread
        GlobalScope.launch(Dispatchers.Main) {
            statusManager.start()
            hearingManager.start()
            speechManager.start()
        }
        // Start those managers that run on a background thread (no UI)
        // This includes especially network handlers
        GlobalScope.launch(Dispatchers.IO) {
            facesManager.start()
            geometryManager.start()
            socketManager.start()
        }
        Log.i(CLSS, String.format("start: complete"))
    }

    /**
     * Stop the sub-services.
     */
   @OptIn(DelicateCoroutinesApi::class)
    fun stop() {
        Log.i(CLSS, String.format("stop: stopping Dispatcher and managers"))
        speechManager.stop()
        facesManager.stop()
        geometryManager.stop()
        socketManager.stop()
        hearingManager.stop()
        statusManager.stop()
        textManager.stop()
    }

    /* =================================================================
     *  Methods callable by the various managers which are given the
     *  service instance.
     * ==================================================================
     */
    // NOTE: This does not automatically set state to ERROR.
    fun logError(type: ManagerType, text: String) {
        val msg = type.name + ":" + text
        textManager.processText(LOG, msg)
    }
    /* =================================================================
    *  Notify the hearing manager that spoken text has just ended
    *   (we attempt to prevent a feedback loop analyzing text
    *    originating from the robot).
    * ==================================================================
    */
    fun markEndOfSpeech() {
        hearingManager.markEndOfSpeech()
    }
    /**
     * Presumably the text originates from the speech recognizer on the tablet (or an error).
     * Determine if a local command to ignore or start paying attention. If not ignoring,
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun processSpokenText(text: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val isLocal = scanTextForLocalCommand(text)
            if( !isLocal ) {
                if (!ignoring) {
                    Log.i(CLSS, String.format("processSpokenText: %s", text))
                    textManager.processText(MessageType.MSG, text)
                    socketManager.receiveTextToSend(
                        String.format("%s:%s", MessageType.MSG.name, text)
                    )
                }
                else {
                    Log.i(CLSS, String.format("processSpokenText: IGNORING: %s", text))
                }
            }
            else {
                textManager.processText(MessageType.LOG, text)
                Log.i(CLSS, String.format("processSpokenText: LOCAL COMMAND: %s", text))
            }
        }
    }

    /**
     * The TCP socket has read a result from the robot. The text starts with a
     * MessageType header. We strip it and route the message accordingly.
     */
    fun receiveMessage(text: String) {
        var txt = text
        if(txt.length > 4) {
            Log.i(CLSS, String.format("receiveMessage: (%s)", txt))
            try {
                val hdr = txt.substring(0, BertConstants.HEADER_LENGTH)
                val type = MessageType.valueOf(hdr.uppercase(Locale.getDefault()))
                txt = txt.substring(BertConstants.HEADER_LENGTH + 1)
                // # delimiter between JSON type and JSON object
                if( type==MessageType.JSN) {
                    val index = txt.indexOf("#")
                    if( index>0 ) {
                        val tag = JsonType.valueOf(txt.substring(0,index))
                        var json = ""
                        if(txt.length>index+1) json = txt.substring(index+1)
                        textManager.processJson(tag,json)
                    }
                    else {
                        Log.w(CLSS, String.format("receiveMessage: Unable to process JSON (%s)",txt))
                    }
                }
                else {
                    textManager.processText(type, txt)
                }
            }
            catch(iae: IllegalArgumentException) {
                Log.w(CLSS, String.format("receiveMessage: (%s) has unrecognized header", txt))
            }
        }
        else {
            Log.w(CLSS, String.format("receiveMessage: (%s) is too short", txt))
        }
    }
    fun reportManagerState(type: ManagerType, state: ManagerState) {
        statusManager.updateState(type, state)
    }

    /**
     * Send a Json message to the robot
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun reportJsonData(type: JsonType, json: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val msg = String.format("%s:%s#%s",MessageType.JSN.name,type.name,json)
            Log.i(CLSS, String.format("reportJsonData: %s", msg))
            socketManager.receiveTextToSend(msg)
        }
    }

    /**
     * Send an empty JSON message to the robot. The expectation
     * is a populated response of the same JsonType.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun sendJsonRequest(type: JsonType) {
        GlobalScope.launch(Dispatchers.IO) {
            val msg = String.format("%s:%s#",MessageType.JSN.name,type.name)
            Log.i(CLSS, String.format("sendJsonRequest: %s", msg))
            socketManager.receiveTextToSend(msg)
        }
    }

    /**
     * Send a text message to the robot
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun sendRequest(text: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val msg = String.format("%s:%s",MessageType.MSG.name,text)
            Log.i(CLSS, String.format("sendRequest: %s", msg))
            socketManager.receiveTextToSend(msg)
        }
    }

    /**
     * Search the text for "ignore" or "attention" commands
     */
    private fun scanTextForLocalCommand(text:String):Boolean {
        var isLocal = false
        if( text.contains("bert ",true) ) {
            if(text.contains("ignore ")) {
                ignoring = true     // Bert ignore me
                isLocal = true
                Log.i(CLSS, String.format("scanTextToSetLocalState: IGNORING set TRUE "))
            }
            else if( text.contains(" attention")) {
                ignoring = false    // Bert pay attention
                isLocal = true
                Log.i(CLSS, String.format("scanTextToSetLocalState: IGNORING set FALSE "))
            }
        }
        return isLocal
    }

    /* ================================================================
     * The companion object contains methods callable in a static way
     * from components throughout the application. It is necessary
     * to set the instance before any components are initialized.
     * ================================================================
     */
    companion object {
        lateinit var instance: DispatchService

        // Handle all the registrations
        fun registerForJson(obs: JsonObserver) {
            instance.textManager.registerJsonViewer(obs)
        }

        fun unregisterForJson(obs: JsonObserver) {
            instance.textManager.unregisterJsonViewer(obs)
        }

        fun registerForLogs(obs: LogDataObserver) {
            instance.textManager.registerLogViewer(obs)
        }

        fun unregisterForLogs(obs: LogDataObserver) {
            instance.textManager.unregisterLogViewer(obs)
        }

        fun registerForShapes(obs: LinkShapeObserver) {
            instance.geometryManager.registerShapeViewer(obs)
        }

        fun unregisterForShapes(obs: LinkShapeObserver) {
            instance.geometryManager.unregisterShapeViewer(obs)
        }
        fun registerForStatus(obs: StatusObserver) {
            instance.statusManager.register(obs)
        }

        fun unregisterForStatus(obs: StatusObserver) {
            instance.statusManager.unregister(obs)
        }

        fun registerForTranscripts(obs: LogDataObserver) {
            instance.textManager.registerTranscriptViewer(obs)
        }

        fun unregisterForTranscripts(obs: LogDataObserver) {
            instance.textManager.unregisterTranscriptViewer(obs)
        }

        fun clear(type: MessageType) {
            instance.textManager.clear(type)
        }

        fun reportFaceDetected(face:Face) {
            instance.facesManager.reportFaceDetected(face)
        }

        fun sendJsonRequest(type:JsonType) {
            instance.sendJsonRequest(type)
        }
        fun sendRequest(text:String) {
            instance.sendRequest(text)
        }

        fun speak(msg:String) {
            Log.i(CLSS, String.format("speak: %s", msg))
            instance.speechManager.speak(msg)
            instance.markEndOfSpeech()
        }

        val CLSS = "DispatchService,companion"
    }
    val CLSS = "DispatchService"

    init {
        context = ctx
        ignoring = false
        screenHeight = 1000
        screenWidth  = 1000
    }
}
