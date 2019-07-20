package chuckcoughlin.bert.service;


import java.util.ArrayList;
import java.util.List;

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
    private final List<TextMessageObserver> logObservers;
    private final List<TextMessageObserver> tableObservers;
    private final List<TextMessageObserver> transcriptObservers;

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
        logObservers = new ArrayList<>();
        tableObservers = new ArrayList<>();
        transcriptObservers = new ArrayList<>();
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
    public void processText(MessageType type, String text) {
        switch(type) {
            case ANS:
                TextMessage msg = new TextMessage(type,text);
                transcriptList.addFirst(msg);
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
        logObservers.add(observer);
        observer.initialize(this);
    }
    public void registerTableViewer(TextMessageObserver observer) {
        tableObservers.add(observer);
        observer.initialize(this);
    }
    public void registerTranscriptViewer(TextMessageObserver observer) {
        transcriptObservers.add(observer);
        observer.initialize(this);
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
        for(TextMessageObserver observer:logObservers) {
            if (observer != null) {
                observer.initialize(this);
            }
        }
        for(TextMessageObserver observer:transcriptObservers) {
            if (observer != null) {
                observer.initialize(this);
            }
        }
    }
    private void initializeTableObservers(TextManager mgr) {
        for(TextMessageObserver observer:tableObservers) {
            if( observer!=null ) {
                observer.initialize(mgr);
            }
        }
    }
    /**
     * Notify log observers regarding receipt of a new message.
     */
    private void notifyLogObservers(TextMessage msg) {
        for(TextMessageObserver observer:logObservers) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }
    private void notifyTableObservers(TextMessage msg) {
        for(TextMessageObserver observer:tableObservers) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }
    private void notifyTranscriptObservers(TextMessage msg) {
        for(TextMessageObserver observer:transcriptObservers) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }
}
