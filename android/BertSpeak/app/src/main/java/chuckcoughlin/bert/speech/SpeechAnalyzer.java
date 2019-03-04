/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 * @See https://github.com/fcrisciani/android-speech-recognition/
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

/**
 * This fragment handles manual robot control. It publishes Twist messages
 * and listens to ObstacleDistance, not letting the robot crash into something in front of it.
 *
 * OFFLINE is a way of testing the widget and speech functions with the robot offline.
 */

public class SpeechAnalyzer implements  RecognitionListener  {
    private static final String CLSS = "SpeechAnalyzer";
    private final Activity activity;
    private SpeechRecognizer sr = null;

    public SpeechAnalyzer(Activity act ) {
        this.activity = act;
    }

    public void start() {
        sr = SpeechRecognizer.createSpeechRecognizer(activity);
        sr.setRecognitionListener(SpeechAnalyzer.this);
        startRecognizer();
    }
    public void shutdown() {
        if (sr != null) {
            sr.stopListening();
            sr.destroy();
        }
        sr = null;
    }



    // start or restart recognizer
    private void startRecognizer() {
        activity.runOnUiThread(new Runnable() {
            public void run() {
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
                Log.i(CLSS,"SpeechRecognizer: listening ...");
            }
        });
    }

    // ========================================= RecognitionListener ============================
    public void onReadyForSpeech(Bundle params)  {
        //Log.i(CLSS, "onReadyForSpeech");
    }
    public void onBeginningOfSpeech(){
        //Log.i(CLSS, "onBeginningOfSpeech");
    }
    // Background level changed ...
    public void onRmsChanged(float rmsdB){
    }
    public void onBufferReceived(byte[] buffer)  {
        Log.i(CLSS, "onBufferReceived");
    }
    public void onEndOfSpeech()  {
        //Log.i(CLSS, "onEndofSpeech");
    }
    public void onError(int error)  {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                Log.i(CLSS,  String.format("SpeechRecognition: Audio recording error"));
                break;
                // On the Android device, settings, go to SBAssistant and enable the microphone
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                Log.i(CLSS,  String.format("SpeechRecognition: INSUFFICIENT PERMISSION - Enable microphone in app"));
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - no word match. Enunciate!"));
                startRecognizer();  // Try again
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - no speech input"));
                startRecognizer();  // Try again
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                Log.i(CLSS,  String.format("SpeechRecognition: Error network"));
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - in client"));
                break;
            case SpeechRecognizer.ERROR_SERVER:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - in server"));
                break;
            default:
                Log.i(CLSS,  String.format("SpeechRecognition: ERROR (%d) ",error));
        }

    }
    public void onResults(Bundle results) {
        //Log.i(CLSS, "onResults \n" + results);
        // Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        //display results. The zeroth result is usually the space-separated one.
        for (int i = 0; i < matches.size(); i++) {
            Log.i(CLSS, "result " + matches.get(i));
            //if( interpreter.handleWordList(currentRequest,matches.get(i),language)) break;
        }
        startRecognizer();   // restart
    }
    public void onPartialResults(Bundle partialResults) {
        Log.i(CLSS, "onPartialResults");
    }
    public void onEvent(int eventType, Bundle params) {
        Log.i(CLSS, "onEvent " + eventType);
    }



}