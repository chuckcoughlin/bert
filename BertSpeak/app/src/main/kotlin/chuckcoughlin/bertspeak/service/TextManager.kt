package chuckcoughlin.bertspeak.service

import android.text.method.TextKeyListener.clear
import chuckcoughlin.bertspeak.speech.TextMessageObserver
import chuckcoughlin.bertspeak.speech.TextMessage
import kotlin.jvm.Synchronized
import android.util.Log
import chuckcoughlin.bertspeak.common.*
import java.util.ArrayList
import java.util.HashMap

/**
 * The text manager is a repository of text messages destined to be
 * logged and/or annunciated. The messages both originate on the tablet
 * and are responses from the robot connected via Bluetooth.
 *
 * Only the most recent table that has been transmitted is displayable.
 * The column list must be sent before the rows as new columns clear out
 * existing rows. Column and row data consist of pipe-delimited strings.
 */
class TextManager {
    private val logList: FixedSizeList<TextMessage?>
    private val columnList : MutableList<String>   // Columns in the most recent table
    private val rowList: MutableList<TextMessage>  // Text is tab-delimited
    private val transcriptList: FixedSizeList<TextMessage?>
    private val logObservers: MutableMap<String, TextMessageObserver>
    private val tableObservers: MutableMap<String, TextMessageObserver>
    private val transcriptObservers: MutableMap<String, TextMessageObserver>

    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    fun stop() {
        clear()
        logObservers.clear()
        tableObservers.clear()
        transcriptObservers.clear()
    }

    fun getLogs(): FixedSizeList<TextMessage?> {
        return logList
    }

    fun getTableColumns(): List<String> {
        return columnList
    }

    fun getTableRows(): List<TextMessage> {
        return rowList
    }

    fun getTranscript(): FixedSizeList<TextMessage?> {
        return transcriptList
    }

    /*
     * The text has any header needed for tablet-robot communication
     * already stripped off. Place into the proper queue.
     */
    @Synchronized
    fun processText(type: MessageType, text: String) {
        var msg: TextMessage? = null
        Log.i(CLSS, String.format("processText: %s: %s", type.name, text))
        when (type) {
            MessageType.ANS -> {
                msg = TextMessage(text,type)
                transcriptList.addFirst(msg)
                notifyTranscriptObservers(msg)
            }
            MessageType.LOG -> {
                msg = TextMessage(text,type)
                logList.addFirst(msg)
                notifyLogObservers(msg)
            }
            MessageType.MSG -> {
                msg = TextMessage(text,type)
                transcriptList.addFirst(msg)
                notifyTranscriptObservers(msg)
            }
            MessageType.ROW -> {
                msg = TextMessage(text,type)
                rowList.add(msg)
                notifyTableObservers(msg)
            }
            MessageType.TBL -> {
                columnList.add(text)
                initializeTableObservers(this)
            }
        }
    }

    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregister".
     * @param observer
     */
    fun registerLogViewer(observer: TextMessageObserver) {
        logObservers[observer.getName()] = observer
        observer.initialize()
    }

    fun registerTableViewer(observer: TextMessageObserver) {
        tableObservers[observer.getName()] = observer
        observer.initialize()
    }

    fun registerTranscriptViewer(observer: TextMessageObserver) {
        transcriptObservers[observer.getName()] = observer
        observer.initialize()
    }

    fun unregisterLogViewer(observer: TextMessageObserver) {
        for( key in logObservers.keys ) {
            if( logObservers.get(key)!!.equals(observer) ) {
                logObservers.remove(key,observer)
            }
        }
    }

    fun unregisterTableViewer(observer: TextMessageObserver) {
        for( key in tableObservers.keys ) {
            if( tableObservers.get(key)!!.equals(observer) ) {
                tableObservers.remove(key,observer)
            }
        }
    }

    fun unregisterTranscriptViewer(observer: TextMessageObserver) {
        for( key in transcriptObservers.keys ) {
            if( transcriptObservers.get(key)!!.equals(observer) ) {
                transcriptObservers.remove(key,observer)
            }
        }
    }

    /**
     * Remove existing logs/transcript because the manager is being stopped.
     * Leave the table alone.
     */
    fun clear() {
        logList.clear()
        transcriptList.clear()
        for (observer in logObservers.values) {
            observer.initialize()
        }
        transcriptList.clear()
        for (observer in transcriptObservers.values) {
            observer.initialize()
        }
    }

    private fun initializeTableObservers(mgr: TextManager) {
        for (observer in tableObservers.values) {
            observer.initialize()
        }
    }

    /**
     * Notify log observers regarding receipt of a new message.
     */
    private fun notifyLogObservers(msg: TextMessage) {
        for (observer in logObservers.values) {
            observer.update(msg)
        }
    }

    private fun notifyTableObservers(msg: TextMessage) {
        for (observer in tableObservers.values) {
            observer.update(msg)
        }
    }

    private fun notifyTranscriptObservers(msg: TextMessage) {
        for (observer in transcriptObservers.values) {
            Log.i(CLSS, String.format("notifyTranscript: %s", msg.message))
            Log.i(CLSS, String.format("notifyTranscript: updating for %s", msg.message))
            observer.update(msg)
        }
    }

    companion object {
        private const val CLSS = "TextManager"
    }

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
        rowList        = mutableListOf<TextMessage>()
        transcriptList = FixedSizeList(BertConstants.NUM_LOG_MESSAGES)
        logObservers        = mutableMapOf<String, TextMessageObserver>()
        tableObservers      = mutableMapOf<String, TextMessageObserver>()
        transcriptObservers = mutableMapOf<String, TextMessageObserver>()
    }
}