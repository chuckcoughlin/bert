package chuckcoughlin.bertspeak.service

import android.util.Log
import chuckcoughlin.bertspeak.common.BertConstants
import chuckcoughlin.bertspeak.common.FixedSizeList
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.data.TextData
import chuckcoughlin.bertspeak.data.TextDataObserver

/**
 * The text manager is a repository of text messages destined to be
 * logged and/or annunciated. The messages may originate on the tablet
 * be responses from the robot connected via Bluetooth.
 *
 * Only the most recent table that has been transmitted is displayable.
 * The column list must be sent before the rows as new columns clear out
 * existing rows. Column and row data consist of pipe-delimited strings.
 */
class TextManager (service:DispatchService): CommunicationManager {
    val dispatcher = service
    override val managerType = ManagerType.TEXT
    override var managerState = ManagerState.OFF
    private val logList: FixedSizeList<TextData>
    private val columnList : MutableList<String>   // Columns in the most recent table
    private val rowList: MutableList<TextData>  // Text is tab-delimited
    private val transcriptList: FixedSizeList<TextData>
    private val logObservers: MutableMap<String, TextDataObserver>
    private val tableObservers: MutableMap<String, TextDataObserver>
    private val transcriptObservers: MutableMap<String, TextDataObserver>

    override fun start() {
        clear(MessageType.ANS)
        clear(MessageType.LOG)
        clear(MessageType.MSG)
        clear(MessageType.TBL)
    }

    /**
     * Called when the application is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
        clear(MessageType.ANS)
        clear(MessageType.LOG)
        clear(MessageType.MSG)
        clear(MessageType.TBL)
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
            MessageType.ROW -> {
                rowList.clear()
                initializeTableObservers()
            }
            MessageType.TBL -> {
                columnList.clear()
                initializeTableObservers()
            }
        }
    }

    /*
     * The text has any header needed for tablet-robot communication
     * already stripped off. Place into the proper queue.
     */
    @Synchronized
    fun processText(type: MessageType, text: String) {
        Log.i(CLSS, String.format("processText (%s): %s", type.name, text))
        when (type) {
            MessageType.ANS -> {
                var msg = TextData(text,type)
                transcriptList.addFirst(msg)
                notifyTranscriptObservers(msg)
            }
            MessageType.LOG -> {
                var msg = TextData(text,type)
                logList.addFirst(msg)
                notifyLogObservers(msg)
            }
            MessageType.MSG -> {
                var msg = TextData(text,type)
                transcriptList.addFirst(msg)
                notifyTranscriptObservers(msg)
            }
            MessageType.ROW -> {
                var msg = TextData(text,type)
                rowList.add(msg)
                notifyTableObservers(msg)
            }
            MessageType.TBL -> {
                columnList.add(text)
                initializeTableObservers()
            }
        }
    }
    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregister".
     * @param observer
     */
    fun registerLogViewer(observer: TextDataObserver) {
        logObservers[observer.name] = observer
        observer.reset(logList)
    }

    fun registerTableViewer(observer: TextDataObserver) {
        tableObservers[observer.name] = observer
        observer.reset(rowList)
    }

    fun registerTranscriptViewer(observer: TextDataObserver) {
        transcriptObservers[observer.name] = observer
        observer.reset(transcriptList)
    }

    fun unregisterLogViewer(observer: TextDataObserver) {
        for( key in logObservers.keys ) {
            if( logObservers.get(key)!!.equals(observer) ) {
                logObservers.remove(key,observer)
            }
        }
    }

    fun unregisterTableViewer(observer: TextDataObserver) {
        for( key in tableObservers.keys ) {
            if( tableObservers.get(key)!!.equals(observer) ) {
                tableObservers.remove(key,observer)
            }
        }
    }

    fun unregisterTranscriptViewer(observer: TextDataObserver) {
        for( key in transcriptObservers.keys ) {
            if( transcriptObservers.get(key)!!.equals(observer) ) {
                transcriptObservers.remove(key,observer)
            }
        }
    }

    private fun initializeLogObservers() {
        for (observer in logObservers.values) {
            observer.reset(logList)
        }
    }
    private fun initializeTableObservers() {
        for (observer in tableObservers.values) {
            observer.reset(rowList)
        }
    }

    private fun initializeTranscriptObservers() {
        for (observer in transcriptObservers.values) {
            observer.reset(transcriptList)
        }
    }

    /**
     * Notify log observers regarding receipt of a new message.
     */
    private fun notifyLogObservers(msg: TextData) {
        Log.i(CLSS, String.format("notifyLogObservers: %s", msg.message))
        for (observer in logObservers.values) {
            observer.update(msg)
        }
    }

    private fun notifyTableObservers(msg: TextData) {
        for (observer in tableObservers.values) {
            observer.update(msg)
        }
    }

    private fun notifyTranscriptObservers(msg: TextData) {
        Log.i(CLSS, String.format("notifyTranscriptObservers: %s", msg.message))
        for (observer in transcriptObservers.values) {
            Log.i(CLSS, String.format("notifyTranscript: %s", msg.message))
            Log.i(CLSS, String.format("notifyTranscript: updating for %s", msg.message))
            observer.update(msg)
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
        logList = FixedSizeList(BertConstants.NUM_LOG_MESSAGES)
        columnList     = mutableListOf<String>()
        rowList        = mutableListOf<TextData>()
        transcriptList = FixedSizeList(BertConstants.NUM_LOG_MESSAGES)
        logObservers        = mutableMapOf<String, TextDataObserver>()
        tableObservers      = mutableMapOf<String, TextDataObserver>()
        transcriptObservers = mutableMapOf<String, TextDataObserver>()
    }
}
