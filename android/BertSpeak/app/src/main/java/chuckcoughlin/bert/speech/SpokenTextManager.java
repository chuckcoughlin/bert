package chuckcoughlin.bert.speech;


import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import chuckcoughlin.bert.common.FixedSizeList;
import chuckcoughlin.bert.common.BertConstants;

/**
 * The spoken text manager is a repository of text message destined to be
 * logged and/or enunciated. The messages come from various sources including
 * the socket reader and detected errors.
 */
public class SpokenTextManager implements TextToSpeech.OnInitListener {
    private final static String CLSS = "SpokenTextManager";
    private static volatile SpokenTextManager instance = null;
    private final FixedSizeList<TextMessage> textList;
    private final List<TextMessageObserver> observers;
    //private final Annunciator annunciator;

    /**
     * Constructor is private per Singleton pattern. This forces use of the single instance.
     * On start, create subscriptions to the applications.
     */
    private SpokenTextManager(Context ctx) {
        textList = new FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES);
        observers = new ArrayList<>();
        //annunciator = new Annunciator(ctx,this);
    }

    /**
     * Use this method in the initial activity.
     * @return the Singleton instance
     */
    public static synchronized SpokenTextManager initialize(Context ctx) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        if (instance == null) {
            instance = new SpokenTextManager(ctx);
        }
        else {
            Log.w(CLSS,String.format("initialize: Text manager exists, re-initialization ignored"));
        }
        return instance;
    }

    /**
     * Called when main activity is destroyed. Clean up any resources.
     * To use again requires re-initialization.
     */
    public static void stop() {
        if (instance != null) {
            synchronized (SpokenTextManager.class) {
                instance = null;
            }
        }
    }
    /**
     * Use this method for all except the initial access.
     * @return the Singleton instance.
     */
    public static synchronized SpokenTextManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Attempt to return uninitialized copy of SpokenTextManager");
        }
        return instance;
    }


    public FixedSizeList<TextMessage> getLogs() { return textList; }
    public TextMessage getLogAtPosition(int pos) {
        TextMessage result = null;
        if(pos>=0 && pos<textList.size()) { result = textList.get(pos); }
        return result;
    }
    public void processText(String text,MessageType type) {
        TextMessage msg = new TextMessage(text,type);
        notifyObservers(msg);
    }
    /**
     * When a new observer is registered, update with states of all
     * tiered facilities.
     * @param observer
     */
    public void register(TextMessageObserver observer) {
        observers.add(observer);
        List<TextMessage> list = new ArrayList<>();
        for(TextMessage msg:textList) {
            list.add(msg);
        }
        observer.initialize(list);
    }
    public void unregister(TextMessageObserver observer) {
        observers.remove(observer);
    }


    /**
     * Remove existing logs.
     */
    public void clear() {
        textList.clear();
        for(TextMessageObserver observer:observers) {
            if( observer!=null ) {
                observer.initialize(textList);
            }
        }
    }

    /**
     * Notify observers regarding receipt of a new message
     */
    private void notifyObservers(TextMessage msg) {
        for(TextMessageObserver observer:observers) {
            if( observer!=null ) {
                observer.update(msg);
            }
        }
    }

    // =================================== OnInitListener ===============================
    @Override
    public void onInit(int status) {
        Log.i(CLSS,String.format("onInit: SpeechToText status - %d",status));
        /*
            For when we need to select an appropriate speaker ... maybe one of these
            en-gb-x-rjs#male_2-local
            en-gb-x-fis#male_1-local
            en-gb-x-fis#male_3-local

        Set<Voice> voices = annunciator.getVoices();
        for( Voice v:voices) {
            Log.i(CLSS,String.format("oninit: voice = %s %d",v.getName(),v.describeContents()));
        }
          */
    }
}
