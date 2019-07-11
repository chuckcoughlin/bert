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
    private final static String UTTERANCE_ID = CLSS;
    /**
     * The Android facility for pronouncing text. Configuration
     * methods are not effective in the constructor
     */
    public Annunciator(Context context, OnInitListener listener) {
        super(context,listener);
    }


    /**
     * Convert error text to speach in the chosen language. We are not yet using the distance.
     * @param text the text to pronounce
     */
    public void speak(String text ){
        Log.i(CLSS,String.format("speak: %s",text));
        int result = 0;
        result = speak(text,TextToSpeech.QUEUE_FLUSH,null,UTTERANCE_ID);
        if( result!=SUCCESS ) {
            Log.w(CLSS,String.format("speak: %s pronouncing: %s",errorToText(result),text));
        }
    }

    private String errorToText(int err) {
        String error = "??";
        switch(err) {
            case TextToSpeech.ERROR_INVALID_REQUEST:
                error = "Invalid request";
                break;
            case TextToSpeech.ERROR_NETWORK:
                error = "Network error";
                break;
            case TextToSpeech.ERROR_NETWORK_TIMEOUT:
                error = "Network timeout";
                break;
            case TextToSpeech.ERROR_SYNTHESIS:
                error = "Failed to synthesize";
                break;
            default:
                error = String.format("Error %d", err);
                break;
        }
        return error;
    }
}
