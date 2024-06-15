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
import chuckcoughlin.bertspeak.common.NameValue
import chuckcoughlin.bertspeak.data.GeometryDataObserver
import chuckcoughlin.bertspeak.data.StatusDataObserver
import chuckcoughlin.bertspeak.data.TextDataObserver
import chuckcoughlin.bertspeak.db.DatabaseManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt


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
    lateinit var speechManager: SpeechManager
    lateinit var geometryManager: GeometryManager
    lateinit var hearingManager: HearingManager
    lateinit var socketManager: SocketManager
    lateinit var statusManager: StatusManager
    lateinit var textManager: TextManager

    /**
     * The order here is important
     */
    fun initialize() {
        Log.i(CLSS, "initialize: ... ");
        instance = this
        statusManager = StatusManager(this)
        textManager = TextManager(this)
        speechManager = SpeechManager(this)
        geometryManager = GeometryManager(this)
        socketManager = SocketManager(this)
        hearingManager = HearingManager(this)
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
            speechManager.start()
            hearingManager.start()
        }
        // Start those managers that run on a background thread (no UI)
        // This includes especially network handlers
        GlobalScope.launch(Dispatchers.IO) {
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
        speechManager.stop()
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


    fun reportManagerState(type: ManagerType, state: ManagerState) {
        statusManager.updateState(type, state)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startSpeech() {
        GlobalScope.launch(Dispatchers.Main) {
            hearingManager.startListening()
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    fun stopSpeech() {
        GlobalScope.launch(Dispatchers.Main) {
            hearingManager.stopListening()
        }
    }

    /**
     * The TCP socket reads a result from the robot. The text starts with a
     * MessageType header. We strip it and route the message accordingly.
     */
    fun receiveText(text: String) {
        var txt = text
        if(txt.length > 4) {
            Log.i(CLSS, String.format("receiveText: (%s)", txt))
            try {
                val hdr = txt.substring(0, BertConstants.HEADER_LENGTH)
                val type = MessageType.valueOf(hdr.uppercase(Locale.getDefault()))
                txt = txt.substring(BertConstants.HEADER_LENGTH + 1)
                textManager.processText(type, txt)
            }
            catch(iae: IllegalArgumentException) {
                Log.w(CLSS, String.format("receiveText: (%s) has unrecognized header", txt))
            }
        }
        else {
            Log.w(CLSS, String.format("receiveText: (%s) is too short", txt))
        }
    }

    /**
     * Presumeably the text originates from the speech recognizer on the tablet (or an error).
     * Send text to the robot for processing. Inform the text manager for dissemination
     * to any observers.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun processSpokenText(text: String) {
        GlobalScope.launch(Dispatchers.IO) {
            Log.i(CLSS, String.format("processSpokenText: %s", text))
            textManager.processText(MessageType.ANS, text)
            socketManager.receiveTextToSend(String.format("%s:%s", MessageType.ANS.name, text))
        }
    }


    /* ================================================================
     * The companion object contains methods callable in a static way
     * from components throughout the application. It is necessary
     * to set the instance before any components are initialkized.
     * ================================================================
     */
    companion object {
        lateinit var instance:DispatchService

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
        fun unregisterForLogs(obs: TextDataObserver) {
            instance.textManager.unregisterLogViewer(obs)
        }
        fun registerForTranscripts(obs: TextDataObserver) {
            instance.textManager.registerTranscriptViewer(obs)
        }
        fun unregisterForTranscripts(obs: TextDataObserver) {
            instance.textManager.unregisterTranscriptViewer(obs)
        }
        fun clear(type: MessageType) {
            instance.textManager.clear(type)
        }

        fun restoreAudio() {
            instance.speechManager.restoreAudio()
        }
        fun toggleSpeechState() {
            instance.hearingManager.toggleSpeechState()
        }
        fun suppressAudio() {
            instance.speechManager.suppressAudio()
        }
        fun updateManagerStatus(mgr:ManagerType,state:ManagerState) {
            instance.statusManager.updateState(mgr, state)
        }
    }
    val CLSS = "DispatchService"

    init {
        context = ctx
    }
}
