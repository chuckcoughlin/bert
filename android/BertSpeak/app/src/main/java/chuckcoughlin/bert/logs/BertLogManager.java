package chuckcoughlin.bert.logs;


import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.bert.common.FixedSizeList;
import chuckcoughlin.bert.common.BertConstants;
import chuckcoughlin.bert.logs.LogMessage;
/**
 * The log manager subscribes to log messages whenever any application is active.
 * The instance is created and shutdown in the MainActivity. The instance must be
 * initialized as its first operation.
 */
public class BertLogManager  {
    private final static String CLSS = "BertLogManager";
    private static volatile BertLogManager instance = null;
    private final LogListener logListener;
    private final FixedSizeList<LogMessage> logList;
    private final List<LogListObserver> observers;
    private boolean frozen = false;

    /**
     * Constructor is private per Singleton pattern. This forces use of the single instance.
     * On start, create subscriptions to the applications.
     */
    private BertLogManager() {
        logList = new FixedSizeList<LogMessage>(BertConstants.NUM_LOG_MESSAGES);
        logListener = new LogListener();
        observers = new ArrayList<>();
    }

    /**
     * Use this method in the initial activity. We need to assign the context.
     * @return the Singleton instance
     */
    public static synchronized BertLogManager initialize() {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        if (instance == null) {
            instance = new BertLogManager();
        }
        else {
            android.util.Log.w(CLSS,String.format("initialize: Log manager exists, re-initialization ignored"));
        }
        return instance;
    }

    /**
     * Called when main activity is destroyed. Clean up any resources.
     * To use again requires re-initialization.
     */
    public static void destroy() {
        if (instance != null) {
            synchronized (BertLogManager.class) {
                instance.shutdown();
                instance = null;
            }
        }
    }
    /**
     * Use this method for all subsequent calls. We often don't have
     * a convenient context.
     * @return the Singleton instance.
     */
    public static synchronized BertLogManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Attempt to return uninitialized copy of SBLogManager");
        }
        return instance;
    }

    public FixedSizeList<LogMessage> getLogs() { return logList; }
    public LogMessage getLogAtPosition(int pos) {
        LogMessage result = null;
        if(pos>=0 && pos<logList.size()) { result = logList.get(pos); }
        return result;
    }

    private void shutdown() {
    }


    /**
     * Remove existing logs.
     */
    public void clear() {
        logList.clear();
        notifyObserversOfClearedLog();
    }
    public void freeze() {
        frozen = true;
    }
    public void resume() {
        frozen = false;
    }

    public boolean isFrozen() { return frozen; }

    public void addObserver(LogListObserver observer) {
        observers.add(observer);
    }
    public void removeObserver(LogListObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify observers regarding a log queue clearance.
     * The queue has been emptied before this call.
     */
    private void notifyObserversOfClearedLog() {
        for(LogListObserver observer:observers) {
            if( observer!=null ) {
                observer.notifyListCleared();
            }
            else {
                //android.util.Log.i(CLSS, String.format("WARNING: Attempt to notify null observer"));
            }
        }
    }
    //
    private void notifyObserversOfLogAddition() {
        for(LogListObserver observer:observers) {
            if( observer!=null ) {
                observer.notifyListAppended();
            }
            else {
                //android.util.Log.i(CLSS, String.format("WARNING: Attempt to notify null observer"));
            }
        }
    }

    // =================================== Message Listener ============================
    private class LogListener implements MessageListener {
        public LogListener() {

        }

        @Override
        public void onNewMessage(LogMessage msg) {

            if( !frozen ) {
                android.util.Log.i(CLSS, String.format("%s", msg.getMessage()));
                synchronized(logList) {
                    logList.add(msg);
                    notifyObserversOfLogAddition();
                }
            }
            else {
                android.util.Log.i(CLSS, String.format("%s", msg.getMessage()));
            }
        }
    }
}
