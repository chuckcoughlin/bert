/**
 * Copyright 2022-2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.common.MessageType.JSN
import chuckcoughlin.bertspeak.common.MessageType.LOG
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.data.JsonDataObserver
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.data.LogDataObserver
import chuckcoughlin.bertspeak.data.TextObserver
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
    lateinit var facesManager: FacesManager
    lateinit var geometryManager: GeometryManager
    lateinit var hearingManager: HearingManager
    lateinit var socketManager: SocketManager
    lateinit var speechManager: SpeechManager
    lateinit var statusManager: StatusManager
    lateinit var textManager: TextManager

    /**
     * The order here is important
     */
    fun initialize() {
        Log.i(CLSS, "initialize: ... ")
        instance = this
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
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun processSpokenText(text: String) {
        GlobalScope.launch(Dispatchers.IO) {
            Log.i(CLSS, String.format("processSpokenText: %s", text))
            textManager.processText(MessageType.MSG, text)
            socketManager.receiveTextToSend(String.format("%s:%s",
                MessageType.MSG.name, text))
        }
    }

    /**
     * The TCP socket reads a result from the robot. The text starts with a
     * MessageType header. We strip it and route the message accordingly.
     */
    fun receiveMessage(text: String) {
        var txt = text
        if(txt.length > 4) {
            Log.i(CLSS, String.format("receiveText: (%s)", txt))
            try {
                val hdr = txt.substring(0, BertConstants.HEADER_LENGTH)
                val type = MessageType.valueOf(hdr.uppercase(Locale.getDefault()))
                txt = txt.substring(BertConstants.HEADER_LENGTH + 1)
                if( type==MessageType.JSN) {
                    val index = txt.indexOf(" ")
                    if( index>0 ) {
                        val tag = txt.substring(0,index)
                        txt = txt.substring(index+1)
                        textManager.processJson(tag,txt)
                    }
                    else {
                        Log.w(CLSS, String.format("receiveMessage: Unable to process tag (%s)", txt))
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
    fun reportJsonData(type: JsonType,json: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val msg = String.format("%s:%s %s",MessageType.JSN.name,type.name,json)
            Log.i(CLSS, String.format("reportJsonData: %s", msg))
            socketManager.receiveTextToSend(msg)
        }
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
        fun registerForData(key:String,obs: JsonDataObserver) {
            instance.textManager.registerDataViewer(key,obs)
        }

        fun unregisterForData(key:String,obs: JsonDataObserver) {
            instance.textManager.unregisterDataViewer(key, obs)
        }

        fun registerForGeometry(obs: GeometryDataObserver) {
            instance.geometryManager.register(obs)
        }

        fun unregisterForGeometry(obs: GeometryDataObserver) {
            instance.geometryManager.unregister(obs)
        }

        fun registerForLogs(obs: LogDataObserver) {
            instance.textManager.registerLogViewer(obs)
        }

        fun unregisterForLogs(obs: LogDataObserver) {
            instance.textManager.unregisterLogViewer(obs)
        }

        fun registerForStatus(obs: StatusDataObserver) {
            instance.statusManager.register(obs)
        }

        fun unregisterForStatus(obs: StatusDataObserver) {
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
        fun speak(msg:String) {
            Log.i(DispatchService.CLSS, String.format("speak: %s",msg))
            instance.speechManager.speak(msg)
        }
        val CLSS = "DispatchService,companion"
    }
    val CLSS = "DispatchService"

    init {
        context = ctx
    }
}
