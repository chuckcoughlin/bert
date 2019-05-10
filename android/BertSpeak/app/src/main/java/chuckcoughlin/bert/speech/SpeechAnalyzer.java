/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

import chuckcoughlin.bert.MainActivity;
import chuckcoughlin.bert.service.VoiceServiceHandler;

/**
 *This class analyzes speech, converting it into text (lists of words).
 * These methods must be executeed on the main application thread (UI thread).
 */

public class SpeechAnalyzer implements  RecognitionListener  {
    private static final String CLSS = "SpeechAnalyzer";
    private final Context context;
    private SpeechRecognizer sr = null;
    private final VoiceServiceHandler handler;

    public SpeechAnalyzer(VoiceServiceHandler h,Context c ) {
        this.context = c;
        this.handler  = h;
    }

    public void start() {
        sr = SpeechRecognizer.createSpeechRecognizer(context);
        sr.setRecognitionListener(SpeechAnalyzer.this);
        listen();
    }
    public void shutdown() {
        if (sr != null) {
            sr.stopListening();
            sr.destroy();
        }
        sr = null;
    }

    // start or restart recognizer
    public void listen() {
        if(sr!=null) sr.cancel();
        String locale =  "us-UK";
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);  // Partials are always empty
        //Give a hint to the recognizer about what the user is going to say
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Max number of results. This is three attempts at deciphering, not a 3-word limit.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        sr.startListening(intent);
        Log.i(CLSS,"SpeechRecognizer: Listening for audio ...");
    }

    // ========================================= RecognitionListener ============================
    public void onReadyForSpeech(Bundle params)  {
        Log.i(CLSS, "onReadyForSpeech");
    }
    public void onBeginningOfSpeech(){
        Log.i(CLSS, "onBeginningOfSpeech");
    }
    // Background level changed ...
    public void onRmsChanged(float rmsdB){
    }
    public void onBufferReceived(byte[] buffer)  {
        Log.i(CLSS, "onBufferReceived");
    }
    public void onEndOfSpeech()  {
        Log.i(CLSS, "onEndofSpeech");
    }
    public void onError(int error)  {
        String reason = null;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                reason = String.format("Audio recording error");
                break;
                // On the Android device, settings, go to BertSpeak and enable the microphone
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                reason = String.format("INSUFFICIENT PERMISSION - Enable microphone in application");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - no word match. Enunciate!"));
                listen();  // Try again
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - no speech input"));
                listen();  // Try again
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                reason =  String.format("Network error");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                reason = String.format("Error - in client");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                reason = String.format("Error - in server");
                break;
            default:
                reason = String.format("ERROR (%d) ",error);
        }
        if( reason!=null) {
            Log.i(CLSS,  String.format("SpeechRecognition: Error - %s",reason));
            handler.handleVoiceError(reason);
        }
    }
    public void onResults(Bundle results) {
        //Log.i(CLSS, "onResults \n" + results);
        // Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        //display results. The zeroth result is usually the space-separated one.
        assert matches != null;
        for (int i = 0; i < matches.size(); i++) {
            Log.i(CLSS, "result " + matches.get(i));
        }
        handler.receiveText(matches.get(0));
    }
    public void onPartialResults(Bundle partialResults) {
        Log.i(CLSS, "onPartialResults");
    }
    public void onEvent(int eventType, Bundle params) {
        Log.i(CLSS, "onEvent " + eventType);
    }
}