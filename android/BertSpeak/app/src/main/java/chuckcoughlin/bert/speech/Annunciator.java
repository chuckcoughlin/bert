/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.speech;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;


/**
 * Pronounce a supplied phrase.
 */

public class Annunciator extends TextToSpeech {
    private final static String CLSS="Annunciator";
    private long id;
    /**
     * The Android facility for pronouncing text.
     */
    public Annunciator(Context context, OnInitListener listener) {
        super(context,listener);
        setLanguage(Locale.UK);
        setPitch(1.0f);       // Default = 1.0
        setSpeechRate(1.0f);  // Defailt = 1.0
        id = 0;
    }


    /**
     * Convert error text to speach in the chosen language. We are not yet using the distance.
     * @param text the text to pronounce
     */
    public void speak(String text ){
        int result = 0;
        id = id+1;
        result = speak(text,TextToSpeech.QUEUE_FLUSH,null,String.valueOf(id));
        if( result!=SUCCESS ) {
            Log.w(CLSS,String.format("speak: Error %d pronouncing: %s",result,text));
        }
    }
}
