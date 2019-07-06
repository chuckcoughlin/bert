/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */
package chuckcoughlin.bert.speech;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


/**
 * Pronounce a supplied phrase.
 */

public class Annunciator extends TextToSpeech {
    private final static String CLSS="Annunciator";
    private long id;
    /**
     * The Android facility for pronouncing text.
     */
    public Annunciator(Activity activity, OnInitListener listener) {
        super(activity,listener);
        setLanguage(Locale.UK);
        setPitch(1.0f);       // Default = 1.0
        setSpeechRate(1.0f);  // Default = 1.0
        Set<String> features = new HashSet<>();
        Voice voice = new Voice("en-GB-SMTm00",Locale.UK,Voice.QUALITY_HIGH,Voice.LATENCY_NORMAL,false,features);
        setVoice(voice);
        id = 0;
    }


    /**
     * Convert error text to speach in the chosen language. We are not yet using the distance.
     * @param text the text to pronounce
     */
    public void speak(String text ){
        Log.i(CLSS,String.format("speak: %s",text));
        int result = 0;
        id = id+1;
        result = speak(text,TextToSpeech.QUEUE_FLUSH,null,String.valueOf(id));
        if( result!=SUCCESS ) {
            Log.w(CLSS,String.format("speak: Error %d pronouncing: %s",result,text));
        }
    }
}
