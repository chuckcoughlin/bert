package chuckcoughlin.bert.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.common.FixedSizeList;
import chuckcoughlin.bert.common.MessageType;
import chuckcoughlin.bert.speech.TextMessage;
import chuckcoughlin.bert.speech.TextMessageObserver;

/**
 * The text manager is a repository of text messages destined to be
 * logged and/or annunciated. The messages both originate on the tablet
 * and are responses from the robot connected via Bluetooth.
 *
 * Only the most recent table that has been transmitted is displayable.
 * The column list must be sent before the rows as new columns clear out
 * existing rows. Column and row data consist of pipe-delimited strings.
 */
public class TextManager {
    private final static String CLSS = "TextManager";
    private final FixedSizeList<TextMessage> logList;
    private final List<String> columnList;   // Columns in the most recent table
    private final List<TextMessage> rowList; // Text is tab-delimited
    private final FixedSizeList<TextMessage> transcriptList;
    private final Map<String,TextMessageObserver> logObservers;
    private final Map<String,TextMessageObserver> tableObservers;
    private final Map<String,TextMessageObserver> transcriptObservers;

    /**
     * There should only be one text manager. owned by the dispatch service.
     * There are three queues:
     *  1) Spoken text, both requests and responses
     *  2) Logs
     *  3) Table (only the most recent)
     *  When a subscriber first registers, the current queue to-date
     *  is sent.
     */
    public TextManager() {
        logList = new FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES);
        columnList = new ArrayList<String>();
        rowList = new ArrayList<TextMessage>();
        transcriptList = new FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES);
        logObservers = new HashMap<>();
        tableObservers = new HashMap<>();
        transcriptObservers = new HashMap<>();
    }

    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    public void stop() {
        clear();
        logObservers.clear();
        tableObservers.clear();
        transcriptObservers.clear();
    }

    public FixedSizeList<TextMessage> getLogs() { return logList; }
    public List<String> getTableColumns() { return columnList; }
    public List<TextMessage> getTableRows() { return rowList; }
    public FixedSizeList<TextMessage> getTranscript() { return transcriptList; }

    /*
     * The text has any header needed for tablet-robot communication
     * already stripped off. Place into the proper queue.
     */
    public synchronized void processText(MessageType type, String text) {
        switch(type) {
            case ANS:
                TextMessage msg = new TextMessage(type,text);
                transcriptList.addFirst(msg);
                notifyTranscriptObservers(msg);
                break;
            case LOG:
                msg = new TextMessage(type,text);
                logList.addFirst(msg);
                notifyLogObservers(msg);
                break;
            case MSG:
                msg = new TextMessage(type,text);
                transcriptList.addFirst(msg);
                notifyTranscriptObservers(msg);
                break;
            case ROW:
                msg = new TextMessage(type,text);
                rowList.add(msg);
                notifyTableObservers(msg);
                break;
            case TBL:
                columnList.add(text);
                initializeTableObservers(this);
                break;
        }

    }
    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregister".
     * @param observer
     */
    public void registerLogViewer(TextMessageObserver observer) {
        logObservers.put(observer.getName(),observer);
        observer.initialize();
    }
    public void registerTableViewer(TextMessageObserver observer) {
        tableObservers.put(observer.getName(),observer);
        observer.initialize();
    }
    public void registerTranscriptViewer(TextMessageObserver observer) {
        transcriptObservers.put(observer.getName(),observer);
        observer.initialize();
    }
    public void unregisterLogViewer(TextMessageObserver observer) {
        logObservers.remove(observer);
    }
    public void unregisterTableViewer(TextMessageObserver observer) { tableObservers.remove(observer); }
    public void unregisterTranscriptViewer(TextMessageObserver observer) {transcriptObservers.remove(observer);}

    /**
     * Remove existing logs/transcript because the manager is being stopped.
     * Leave the table alone.
     */
    public void clear() {
        logList.clear();
        transcriptList.clear();
        for(TextMessageObserver observer:logObservers.values()) {
            if (observer != null) {
                observer.initialize();
            }
        }
        transcriptList.clear();
        for(TextMessageObserver observer:transcriptObservers.values()) {
            if (observer != null) {
                observer.initialize();
            }
        }
    }
    private void initializeTableObservers(TextManager mgr) {
        for(TextMessageObserver observer:tableObservers.values()) {
            if( observer!=null ) {
                observer.initialize();
            }
        }
    }
    /**
     * Notify log observers regarding receipt of a new message.
     */
    private void notifyLogObservers(TextMessage msg) {
        for(TextMessageObserver observer:logObservers.values()) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }
    private void notifyTableObservers(TextMessage msg) {
        for(TextMessageObserver observer:tableObservers.values()) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }
    private void notifyTranscriptObservers(TextMessage msg) {
        for(TextMessageObserver observer:transcriptObservers.values()) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }
}
