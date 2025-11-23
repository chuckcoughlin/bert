/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.common.ConfigurationConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.JsonObserver
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.LogData
import chuckcoughlin.bertspeak.data.LogDataObserver

/**
 * The text manager is a repository of text messages destined to be
 * logged and/or annunciated. The messages may originate on the tablet
 * or be responses from the robot connected via a TCP network.
 *
 * It also handles messages that are responses to requests for information
 * and carry a JSON payload. These messages are not meant for enunciation.
 *
 * The transcripts and logs are stored newest first.
 *
 * Only the most recent table that has been transmitted is displayable.
 * The column list must be sent before the rows as new columns clear out
 * existing rows. Column and row data consist of pipe-delimited strings.
 */
class TextManager (service:DispatchService): CommunicationManager {
    val dispatcher = service
    override val managerType = ManagerType.TEXT
    override var managerState = ManagerState.OFF
    private val dataMap: MutableMap<JsonType, String>
    private val logList: FixedSizeList<LogData>
    private val transcriptList: FixedSizeList<LogData>
    private val jsonObservers: MutableMap<String, JsonObserver>
    private val logObservers: MutableMap<String, LogDataObserver>
    private val transcriptObservers: MutableMap<String, LogDataObserver>

    override fun start() {
        clear(MessageType.ANS)
        clear(MessageType.JSN)
        clear(MessageType.LOG)
        clear(MessageType.MSG)  // redundant
    }

    /**
     * Called when the application is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
        clear(MessageType.ANS)
        clear(MessageType.JSN)
        clear(MessageType.LOG)
        clear(MessageType.MSG)    // redundant
    }

    /*
     * Clear a queue of the specified type
     */
    @Synchronized
    fun clear(type: MessageType) {
        Log.i(CLSS, String.format("clear (%s):", type.name))
        when (type) {
            MessageType.ANS -> {
                transcriptList.clear()
                initializeTranscriptObservers()
            }
            MessageType.LOG -> {
                logList.clear()
                initializeLogObservers()
            }
            MessageType.MSG -> {
                transcriptList.clear()
                initializeTranscriptObservers()
            }
            MessageType.JSN -> {
                dataMap.clear()
                initializeDataObservers()
            }
        }
    }
    /*
	 * The data element is a String representing a JSON object.
	 * We leave it to the subscriber to analyze. The string is
	 * not appropriate for display to the user.
	*/
    @Synchronized
    fun processJson(tag: JsonType, json: String) {
        Log.i(CLSS, String.format("processJson (%s): %s", tag.name, json))
        dataMap.put(tag,json)
        notifyJsonObservers(tag,json)
    }
    /*
     * The text has any header needed for tablet-robot communication
     * already stripped off. Place into the proper queue.
     */
    @Synchronized
    fun processText(type: MessageType, text: String) {
        Log.i(CLSS, String.format("processText (%s): %s", type.name, text))
        when (type) {
            MessageType.ANS -> {               // Response from the robot
                var msg = LogData(text,type)
                transcriptList.addFirst(msg)
                notifyTranscriptObservers(msg)
            }
            MessageType.LOG -> {
                var msg = LogData(text,type)
                logList.addFirst(msg)
                notifyLogObservers(msg)
            }
            MessageType.MSG -> {
                var msg = LogData(text,type)
                transcriptList.addFirst(msg)
                notifyTranscriptObservers(msg)
            }
            MessageType.JSN -> {
                val err = String.format("%s.processText: Unexpected JSN message (%s)",CLSS,text)
                var msg = LogData(err,type)
                logList.addFirst(msg)
                notifyLogObservers(msg)
            }
        }
    }
    @Synchronized
    fun registerJsonViewer(observer: JsonObserver) {
        jsonObservers[observer.name] = observer
    }
    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregisterSettingsObserver".
     * @param observer
     */
    @Synchronized
    fun registerLogViewer(observer: LogDataObserver) {
        logObservers[observer.name] = observer
        observer.resetText(logList)
    }

    @Synchronized
    fun registerTranscriptViewer(observer: LogDataObserver) {
        transcriptObservers[observer.name] = observer
        observer.resetText(transcriptList)
    }
    fun unregisterLogViewer(observer: LogDataObserver) {
        for( key in logObservers.keys ) {
            if( logObservers.get(key)!!.equals(observer) ) {
                logObservers.remove(key,observer)
                break
            }
        }
    }
    fun unregisterJsonViewer(observer: JsonObserver) {
        for( key in jsonObservers.keys ) {
            if( jsonObservers.get(key)!!.equals(observer) ) {
                jsonObservers.remove(key,observer)
                break
            }
        }
    }

    fun unregisterTranscriptViewer(observer: LogDataObserver) {
        for( key in transcriptObservers.keys ) {
            if( transcriptObservers.get(key)!!.equals(observer) ) {
                transcriptObservers.remove(key,observer)
                break
            }
        }
    }
    private fun initializeLogObservers() {
        for (observer in logObservers.values) {
            observer.resetText(logList)
        }
    }
    private fun initializeDataObservers() {
        for (observer in jsonObservers.values) {
            observer.resetItem(dataMap)
        }
    }

    private fun initializeTranscriptObservers() {
        for (observer in transcriptObservers.values) {
            observer.resetText(transcriptList)
        }
    }

    /**
     * Notify log observers regarding receipt of a new message.
     */
    private fun notifyLogObservers(msg: LogData) {
        //Log.i(CLSS, String.format("notifyLogObservers: %s", msg.message))
        for (observer in logObservers.values) {
            observer.updateText(msg)
        }
    }

    private fun notifyJsonObservers(tag: JsonType, json: String) {
        for (observer in jsonObservers.values) {
            observer.updateItem(tag,json)
        }
    }

    private fun notifyTranscriptObservers(msg: LogData) {
        //Log.i(CLSS, String.format("notifyTranscriptObservers: %s", msg.message))
        for (observer in transcriptObservers.values) {
            Log.i(CLSS, String.format("notifyTranscript: %s: %s", msg.type.name,msg.message))
            observer.updateText(msg)
        }
    }

    private val CLSS = "TextManager"

    /**
     * There should only be one text manager. owned by the dispatch service.
     * There are three queues:
     * 1) Spoken text, both requests and responses
     * 2) Logs
     * 3) Table (only the most recent)
     * When a subscriber first registers, the current queue to-date
     * is sent.
     */
    init {
        dataMap = mutableMapOf<JsonType, String>()
        logList = FixedSizeList(ConfigurationConstants.NUM_LOG_MESSAGES)
        transcriptList = FixedSizeList(ConfigurationConstants.NUM_LOG_MESSAGES)
        logObservers        = mutableMapOf<String, LogDataObserver>()
        jsonObservers       = mutableMapOf<String, JsonObserver>()
        transcriptObservers = mutableMapOf<String, LogDataObserver>()
    }
}
