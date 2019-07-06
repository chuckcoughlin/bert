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
 * The spoken text manager is a repository of text messages destined to be
 * logged and/or annunciated. The messages are responses from the robot
 * connected via Bluetooth.
 */
public class SpokenTextManager {
    private final static String CLSS = "SpokenTextManager";
    private final FixedSizeList<TextMessage> textList;
    private final List<TextMessageObserver> observers;


    /**
     * There should only be one spoken text manager at a time
     * On start, create subscriptions to the applications.
     */
    public SpokenTextManager() {
        textList = new FixedSizeList<TextMessage>(BertConstants.NUM_LOG_MESSAGES);
        observers = new ArrayList<>();

    }



    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    public void stop() {
        clear();
        observers.clear();
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
            if (observer != null) {
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
}
